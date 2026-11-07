package com.routara.field.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Envelope<T>(val success: Boolean, val message: String?, val data: T?)

data class LoginRequest(val email: String, val password: String)
data class LoginData(val accessToken: String, val refreshToken: String, val role: String)
data class RefreshRequest(val refresh: String)
data class RefreshData(val access: String, val refresh: String?)

data class Me(
    val id: Long,
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val phone: String?,
    val role: String,
    val territoryName: String?,
) {
    val fullName get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email }
}

data class Outlet(
    val id: Long,
    val code: String?,
    val name: String,
    val nameBn: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val addressBn: String = "",
    val channel: Long = 0,
    val channelName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "APPROVED",
)

data class Product(
    val id: Long,
    val sku: String,
    val name: String,
    val category: String,
    val unitName: String,
    val unitsPerCase: Int,
    val tradePrice: String,
    val mrp: String,
) {
    val trade get() = tradePrice.toDoubleOrNull() ?: 0.0
    val retail get() = mrp.toDoubleOrNull() ?: 0.0
    val margin get() = retail - trade
}

data class Beat(val id: Long, val name: String, val outletIds: List<Long>)
data class Named(val id: Long, val name: String)

data class Bootstrap(
    val date: String,
    val visitRadiusM: Int,
    val territory: String?,
    val todayBeat: Long?,
    val beats: List<Beat>,
    val outlets: List<Outlet>,
    val products: List<Product>,
    val reasons: List<Named>,
    val channels: List<Named>,
    val visitedOutletIds: List<Long>,
)

data class DaySummary(
    val date: String,
    val beat: String?,
    val planned: Int,
    val visited: Int,
    val memos: Int,
    val value: String,
    val valuePerMemo: String,
    val dayClosed: Boolean,
)

data class MonthProgress(
    val target: String,
    val achieved: String,
    val remaining: String,
    val percent: Double,
    val requiredPerDay: String,
)

data class Summary(val today: DaySummary, val month: MonthProgress)

data class VisitPayload(
    val clientId: String,
    val outletId: Long,
    val capturedAt: String,
    val latitude: Double?,
    val longitude: Double?,
    val outcome: String,
    val beatId: Long?,
    val reasonId: Long? = null,
    val note: String = "",
)

data class OrderLinePayload(val productId: Long, val quantity: Int)

data class OrderPayload(
    val clientId: String,
    val visitClientId: String?,
    val outletId: Long,
    val capturedAt: String,
    val kind: String,
    val lines: List<OrderLinePayload>,
)

data class NewOutletPayload(
    val clientId: String,
    val name: String,
    val nameBn: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val addressBn: String,
    val channelId: Long,
    val latitude: Double?,
    val longitude: Double?,
)

data class OutletUpdatePayload(
    val clientId: String,
    val outletId: Long,
    val nameBn: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val addressBn: String,
    val latitude: Double?,
    val longitude: Double?,
)

data class SyncBatch(
    val newOutlets: List<NewOutletPayload>,
    val outletUpdates: List<OutletUpdatePayload>,
    val visits: List<VisitPayload>,
    val orders: List<OrderPayload>,
)

data class SyncResult(val clientId: String, val ok: Boolean, val serverId: Long?, val error: String?)

data class SyncResponse(
    val newOutlets: List<SyncResult>,
    val outletUpdates: List<SyncResult>,
    val visits: List<SyncResult>,
    val orders: List<SyncResult>,
)

data class CheckInOut(val lat: Double, val lng: Double)
data class AttendanceBlock(val checkInTime: String?, val checkOutTime: String?)
data class DailyAttendance(val blocks: List<AttendanceBlock>, val totalHours: Double)

data class SkuRow(val productId: Long, val name: String, val sku: String, val quantity: Int, val cases: Double, val value: String, val memos: Int)
data class OutletRow(val outletId: Long, val name: String, val code: String?, val value: String, val memos: Int, val lines: Int)
data class KpiDay(val date: String, val calls: Int, val productive: Int, val memos: Int, val value: String)
data class Kpi(val calls: Int, val productive: Int, val productivePercent: Double, val memos: Int, val value: String, val valuePerMemo: String, val days: List<KpiDay>)
data class TimelineRow(val time: String, val outlet: String, val outcome: String, val reason: String?, val withinRange: Boolean?, val distanceM: Double?, val value: String?)

/** Straight-line distance in metres. */
fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val radius = 6_371_000.0
    fun rad(degrees: Double) = Math.toRadians(degrees)
    val a = sin((rad(lat2) - rad(lat1)) / 2).pow(2) +
        cos(rad(lat1)) * cos(rad(lat2)) * sin((rad(lng2) - rad(lng1)) / 2).pow(2)
    return 2 * radius * asin(sqrt(a))
}
