package com.routara.field.data

import android.content.Context
import com.google.gson.Gson
import com.routara.field.BuildConfig
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RoutaraApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<LoginData>

    @GET("user/me")
    suspend fun me(): Envelope<Me>

    @GET("field/bootstrap")
    suspend fun bootstrap(): Envelope<Bootstrap>

    @GET("field/summary")
    suspend fun summary(): Envelope<Summary>

    @POST("field/sync")
    suspend fun sync(@Body batch: SyncBatch): Envelope<SyncResponse>

    @Multipart
    @POST("field/visits/{clientId}/photo")
    suspend fun uploadPhoto(@Path("clientId") clientId: String, @Part photo: MultipartBody.Part): Envelope<Any>

    @POST("field/day-end")
    suspend fun dayEnd(): Envelope<Any>

    @POST("attendance/check-in")
    suspend fun checkIn(@Body body: CheckInOut): Envelope<Any>

    @POST("attendance/check-out")
    suspend fun checkOut(@Body body: CheckInOut): Envelope<Any>

    @GET("attendance/user/daily/{id}")
    suspend fun attendance(@Path("id") id: Long, @Query("date") date: String): Envelope<DailyAttendance>

    @GET("field/reports/skus")
    suspend fun skuReport(): Envelope<List<SkuRow>>

    @GET("field/reports/outlets")
    suspend fun outletReport(): Envelope<List<OutletRow>>

    @GET("field/reports/kpi")
    suspend fun kpiReport(): Envelope<Kpi>

    @GET("field/reports/timeline")
    suspend fun timeline(): Envelope<List<TimelineRow>>

    @GET("field/orders")
    suspend fun myOrders(): Envelope<List<MyOrder>>

    @GET("field/dues")
    suspend fun dues(): Envelope<List<DueRow>>

    @GET("offers?size=100")
    suspend fun offers(): Envelope<Page<OfferRow>>

    @GET("leave-requests?size=100")
    suspend fun leave(): Envelope<Page<LeaveRow>>

    @POST("leave-requests")
    suspend fun addLeave(@Body body: LeaveBody): Envelope<LeaveRow>

    @POST("leave-requests/{id}/cancel")
    suspend fun cancelLeave(@Path("id") id: Long): Envelope<LeaveRow>

    @GET("expenses?size=100")
    suspend fun expenses(): Envelope<Page<ExpenseRow>>

    @POST("expenses")
    suspend fun addExpense(@Body body: ExpenseBody): Envelope<ExpenseRow>

    @POST("expenses/{id}/cancel")
    suspend fun cancelExpense(@Path("id") id: Long): Envelope<ExpenseRow>

    @GET("conversation?size=100")
    suspend fun chats(): Envelope<Page<ChatRow>>

    @GET("user/others?size=100")
    suspend fun contacts(): Envelope<Page<Contact>>

    @POST("conversation/start/{id}")
    suspend fun startChat(@Path("id") id: Long): Envelope<ChatRow>

    @GET("conversation/{id}/messages?latest=true&size=100")
    suspend fun messages(@Path("id") id: String): Envelope<Page<ChatMessage>>

    @POST("conversation/{id}/messages")
    suspend fun send(@Path("id") id: String, @Body body: MessageBody): Envelope<ChatMessage>

    @POST("conversation/{id}/seen")
    suspend fun seen(@Path("id") id: String): Envelope<Any>
}

/** Tokens live in this app's private storage, cleared on sign-out. */
class Session(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var access: String?
        get() = prefs.getString("access", null)
        set(value) = prefs.edit().putString("access", value).apply()

    var refresh: String?
        get() = prefs.getString("refresh", null)
        set(value) = prefs.edit().putString("refresh", value).apply()

    val signedIn get() = refresh != null

    fun clear() = prefs.edit().clear().apply()
}

/**
 * Swaps an expired access token for a new one and retries once. Refresh
 * tokens rotate on every use, so this runs one at a time: a second request
 * failing at the same moment picks up the token the first one just got.
 */
private class TokenRefresher(private val session: Session, private val gson: Gson) : Authenticator {
    private val client = OkHttpClient()
    private val json = "application/json".toMediaType()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null

        synchronized(this) {
            val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
            val current = session.access
            if (current != null && current != sent) return withToken(response, current)

            val refresh = session.refresh ?: return null
            val request = Request.Builder()
                .url(BuildConfig.API_URL + "auth/token/refresh")
                .post(gson.toJson(RefreshRequest(refresh)).toRequestBody(json))
                .build()

            return client.newCall(request).execute().use { result ->
                if (!result.isSuccessful) {
                    // An outage isn't a rejected session, so only a real
                    // refusal signs the rep out.
                    if (result.code == 401) session.clear()
                    return@use null
                }
                val tokens = gson.fromJson(result.body.string(), RefreshData::class.java)
                session.access = tokens.access
                tokens.refresh?.let { session.refresh = it }
                withToken(response, tokens.access)
            }
        }
    }

    private fun withToken(response: Response, token: String) =
        response.request.newBuilder().header("Authorization", "Bearer $token").build()
}

fun buildApi(session: Session, gson: Gson): RoutaraApi {
    val authenticated = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = session.access
            val request = chain.request().newBuilder()
            if (token != null) request.header("Authorization", "Bearer $token")
            chain.proceed(request.build())
        }
        .authenticator(TokenRefresher(session, gson))
        .build()
    return retrofit(BuildConfig.API_URL, authenticated, gson)
}

fun retrofit(baseUrl: String, client: OkHttpClient, gson: Gson): RoutaraApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()
    .create(RoutaraApi::class.java)

/** A readable message from a failed call, for showing to the rep. */
fun Throwable.friendly(gson: Gson): String = when (this) {
    is retrofit2.HttpException -> runCatching {
        gson.fromJson(response()?.errorBody()?.string(), Envelope::class.java)?.message
    }.getOrNull() ?: "The server couldn't do that. Please try again."
    is java.io.IOException -> "No connection. Your work is saved and will sync later."
    else -> message ?: "Something went wrong."
}
