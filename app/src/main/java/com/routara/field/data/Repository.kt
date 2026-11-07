package com.routara.field.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")

fun todayIso(): String = LocalDate.now(DHAKA).toString()

enum class PendingType { NEW_OUTLET, OUTLET_UPDATE, VISIT, ORDER, PHOTO }

/** Work saved on the phone, waiting to reach the server. */
@Entity(tableName = "pending")
data class Pending(
    @PrimaryKey val clientId: String,
    val type: String,
    val json: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null,
)

/** A local copy of each order, so today's list works with no signal. */
@Entity(tableName = "orders")
data class LocalOrder(
    @PrimaryKey val clientId: String,
    val day: String,
    val outletName: String,
    val kind: String,
    val lines: Int,
    val total: Double,
    val capturedAt: String,
)

@Dao
interface FieldDao {
    @Query("SELECT * FROM pending ORDER BY createdAt")
    fun pending(): Flow<List<Pending>>

    @Query("SELECT * FROM pending WHERE type = :type ORDER BY createdAt")
    suspend fun pendingOf(type: String): List<Pending>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun add(item: Pending)

    @Query("DELETE FROM pending WHERE clientId = :clientId")
    suspend fun remove(clientId: String)

    @Query("UPDATE pending SET error = :error WHERE clientId = :clientId")
    suspend fun fail(clientId: String, error: String)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun addOrder(order: LocalOrder)

    @Query("SELECT * FROM orders WHERE day = :day ORDER BY capturedAt DESC")
    fun ordersOn(day: String): Flow<List<LocalOrder>>

    @Query("DELETE FROM orders WHERE day < :day")
    suspend fun forgetBefore(day: String)

    @Query("DELETE FROM pending")
    suspend fun clearPending()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()
}

@Database(entities = [Pending::class, LocalOrder::class], version = 1, exportSchema = false)
abstract class FieldDb : RoomDatabase() {
    abstract fun dao(): FieldDao
}

class Repository(private val context: Context) {
    val gson = Gson()
    val session = Session(context)
    val api = buildApi(session, gson)
    private val dao = Room.databaseBuilder(context, FieldDb::class.java, "field.db").build().dao()
    private val cacheFile = File(context.filesDir, "bootstrap.json")
    private val summaryFile = File(context.filesDir, "summary.json")
    private val syncLock = Mutex()

    private val _bootstrap = MutableStateFlow(read(cacheFile, Bootstrap::class.java))
    val bootstrap: StateFlow<Bootstrap?> = _bootstrap.asStateFlow()

    private val _summary = MutableStateFlow(read(summaryFile, Summary::class.java))
    val summary: StateFlow<Summary?> = _summary.asStateFlow()

    val pending: Flow<List<Pending>> = dao.pending()
    fun ordersToday(): Flow<List<LocalOrder>> = dao.ordersOn(todayIso())

    private fun <T> read(file: File, type: Class<T>): T? =
        runCatching { if (file.exists()) gson.fromJson(file.readText(), type) else null }.getOrNull()

    suspend fun signIn(email: String, password: String): Me {
        val login = api.login(LoginRequest(email, password)).data ?: error("Could not sign in.")
        check(login.role == "EMPLOYEE" || login.role == "MANAGER") {
            "This app is for field staff. Please use the web panel."
        }
        session.access = login.accessToken
        session.refresh = login.refreshToken
        return api.me().data ?: error("Could not load your profile.")
    }

    suspend fun signOut() {
        dao.clearPending()
        dao.clearOrders()
        cacheFile.delete()
        summaryFile.delete()
        _bootstrap.value = null
        _summary.value = null
        session.clear()
    }

    /** Sends saved work first, so the fresh copy already includes it. */
    suspend fun refresh() {
        flush()
        val fresh = api.bootstrap().data ?: return
        cacheFile.writeText(gson.toJson(fresh))
        _bootstrap.value = fresh
        val summary = api.summary().data ?: return
        summaryFile.writeText(gson.toJson(summary))
        _summary.value = summary
        dao.forgetBefore(todayIso())
    }

    private suspend fun save(type: PendingType, clientId: String, payload: Any, label: String) {
        dao.add(Pending(clientId, type.name, gson.toJson(payload), label))
        requestSync()
    }

    fun requestSync() {
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, work)
    }

    /** Saves a finished visit, its order and its photo, all on the phone. */
    suspend fun saveVisit(
        outlet: Outlet,
        kind: String,
        location: Pair<Double, Double>?,
        cart: Map<Product, Int>,
        reasonId: Long?,
        note: String,
        photoPath: String?,
    ) {
        val now = Instant.now().toString()
        val visitId = UUID.randomUUID().toString()
        val beat = _bootstrap.value?.todayBeat
        val productive = cart.isNotEmpty()

        val visit = VisitPayload(
            clientId = visitId,
            outletId = outlet.id,
            capturedAt = now,
            latitude = location?.first,
            longitude = location?.second,
            outcome = if (productive) "PRODUCTIVE" else "NON_PRODUCTIVE",
            beatId = beat,
            reasonId = reasonId,
            note = note,
        )
        save(PendingType.VISIT, visitId, visit, "Visit to ${outlet.name}")

        if (productive) {
            val orderId = UUID.randomUUID().toString()
            val order = OrderPayload(
                clientId = orderId,
                visitClientId = visitId,
                outletId = outlet.id,
                capturedAt = now,
                kind = kind,
                lines = cart.map { (product, quantity) -> OrderLinePayload(product.id, quantity) },
            )
            dao.addOrder(
                LocalOrder(
                    clientId = orderId,
                    day = todayIso(),
                    outletName = outlet.name,
                    kind = kind,
                    lines = cart.size,
                    total = cart.entries.sumOf { (product, quantity) -> product.trade * quantity },
                    capturedAt = now,
                ),
            )
            save(PendingType.ORDER, orderId, order, "Order for ${outlet.name}")
        }

        photoPath?.let { save(PendingType.PHOTO, "photo-$visitId", PhotoJob(visitId, it), "Photo of ${outlet.name}") }

        _bootstrap.value?.let { current ->
            val updated = current.copy(visitedOutletIds = (current.visitedOutletIds + outlet.id).distinct())
            _bootstrap.value = updated
            cacheFile.writeText(gson.toJson(updated))
        }
    }

    suspend fun saveNewOutlet(payload: NewOutletPayload) =
        save(PendingType.NEW_OUTLET, payload.clientId, payload, "New outlet ${payload.name}")

    suspend fun saveOutletUpdate(payload: OutletUpdatePayload) {
        save(PendingType.OUTLET_UPDATE, payload.clientId, payload, "Changes to outlet")
        _bootstrap.value?.let { current ->
            val updated = current.copy(
                outlets = current.outlets.map { outlet ->
                    if (outlet.id != payload.outletId) outlet else outlet.copy(
                        nameBn = payload.nameBn,
                        ownerName = payload.ownerName,
                        phone = payload.phone,
                        address = payload.address,
                        addressBn = payload.addressBn,
                        latitude = outlet.latitude ?: payload.latitude,
                        longitude = outlet.longitude ?: payload.longitude,
                    )
                },
            )
            _bootstrap.value = updated
            cacheFile.writeText(gson.toJson(updated))
        }
    }

    private inline fun <reified T> List<Pending>.decode() = map { gson.fromJson(it.json, T::class.java) }

    /**
     * Sends everything waiting. Items only leave the queue once the server
     * has them, and the server ignores repeats, so a dropped connection part
     * way through can't lose or double anything.
     */
    suspend fun flush() = syncLock.withLock {
        val newOutlets = dao.pendingOf(PendingType.NEW_OUTLET.name)
        val updates = dao.pendingOf(PendingType.OUTLET_UPDATE.name)
        val visits = dao.pendingOf(PendingType.VISIT.name)
        val orders = dao.pendingOf(PendingType.ORDER.name)

        if (listOf(newOutlets, updates, visits, orders).any { it.isNotEmpty() }) {
            val response = api.sync(
                SyncBatch(newOutlets.decode(), updates.decode(), visits.decode(), orders.decode()),
            ).data ?: error("The server sent an empty reply.")

            settle(newOutlets, response.newOutlets)
            settle(updates, response.outletUpdates)
            settle(visits, response.visits)
            settle(orders, response.orders)
        }

        for (item in dao.pendingOf(PendingType.PHOTO.name)) {
            val job = gson.fromJson(item.json, PhotoJob::class.java)
            val file = File(job.path)
            if (!file.exists()) {
                dao.remove(item.clientId)
                continue
            }
            // The visit must reach the server before its photo can.
            if (dao.pendingOf(PendingType.VISIT.name).any { it.clientId == job.visitId }) continue
            val part = MultipartBody.Part.createFormData("photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            try {
                api.uploadPhoto(job.visitId, part)
                dao.remove(item.clientId)
                file.delete()
            } catch (refused: retrofit2.HttpException) {
                // One refused photo mustn't hold up the rest of the queue.
                if (refused.code() !in 400..499) throw refused
                dao.fail(item.clientId, refused.friendly(gson))
            }
        }
    }

    private suspend fun settle(items: List<Pending>, results: List<SyncResult>) {
        items.zip(results).forEach { (item, result) ->
            if (result.ok) dao.remove(item.clientId) else dao.fail(item.clientId, result.error ?: "The server refused this.")
        }
    }
}

data class PhotoJob(val visitId: String, val path: String)

private const val PHOTO_MAX_SIDE = 1600
private const val PHOTO_QUALITY = 80

/**
 * Shrinks a camera photo in place, to spare reps' mobile data and stay under
 * the server's size limit. The pixels are turned upright, because saving
 * drops the rotation tag cameras use. If anything fails, the original stays.
 */
fun shrinkPhoto(path: String) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })

        val matrix = Matrix()
        val scale = PHOTO_MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        if (scale < 1f) matrix.postScale(scale, scale)
        matrix.postRotate(ExifInterface(path).rotationDegrees.toFloat())

        val finished = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        File(path).outputStream().use { finished.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, it) }
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as RepositoryHolder).repository
        if (!repository.session.signedIn) return Result.success()
        // A full refresh sends saved work, then brings home's totals up to date.
        return runCatching { repository.refresh() }.fold({ Result.success() }, { Result.retry() })
    }
}

interface RepositoryHolder {
    val repository: Repository
}
