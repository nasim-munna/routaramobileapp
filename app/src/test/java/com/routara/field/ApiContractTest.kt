package com.routara.field

import com.google.gson.Gson
import com.routara.field.data.CheckInOut
import com.routara.field.data.ExpenseBody
import com.routara.field.data.LeaveBody
import com.routara.field.data.LoginRequest
import com.routara.field.data.MessageBody
import com.routara.field.data.NewOutletPayload
import com.routara.field.data.OrderLinePayload
import com.routara.field.data.OrderPayload
import com.routara.field.data.SyncBatch
import com.routara.field.data.VisitPayload
import com.routara.field.data.afterDiscount
import com.routara.field.data.friendly
import com.routara.field.data.retrofit
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Runs the app's real network code against a live backend, so the two can't
 * drift apart unnoticed. Skipped unless these are set:
 *
 *   ROUTARA_TEST_API      for example http://127.0.0.1:8000/api/v1/
 *   ROUTARA_TEST_EMAIL    a field account with a territory and a beat today
 *   ROUTARA_TEST_PASSWORD
 */
class ApiContractTest {
    private val gson = Gson()
    private val url: String? = System.getenv("ROUTARA_TEST_API")
    private var token: String? = null

    private val api by lazy {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder()
            token?.let { request.header("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }.build()
        retrofit(url!!, client, gson)
    }

    @Test
    fun aFullFieldDayWorks() = runBlocking {
        assumeTrue("No test backend configured", url != null)

        val login = api.login(LoginRequest(System.getenv("ROUTARA_TEST_EMAIL"), System.getenv("ROUTARA_TEST_PASSWORD"))).data!!
        token = login.accessToken
        assertEquals("EMPLOYEE", login.role)

        val me = api.me().data!!
        assertNotNull(me.territoryName)

        val day = api.bootstrap().data!!
        assertTrue(day.products.isNotEmpty())
        assertTrue(day.reasons.isNotEmpty())
        assertTrue(day.channels.isNotEmpty())
        assertTrue(day.visitRadiusM > 0)
        assertEquals(15.0, day.maxDiscountPercent, 0.0)
        val beat = day.beats.first { it.id == day.todayBeat }
        val outlet = day.outlets.first { it.id == beat.outletIds.first() }
        val closed = day.outlets.first { it.id == beat.outletIds.last() }
        val product = day.products.first()
        assertTrue(product.trade > 0 && product.margin >= 0)

        // Far from the zone is refused, with the distance in the message.
        val refused = runCatching { api.checkIn(CheckInOut(24.5, 91.5)) }.exceptionOrNull()
        assertTrue(refused?.friendly(gson).orEmpty().contains("from your zone"))
        assertEquals(true, api.checkIn(CheckInOut(outlet.latitude!!, outlet.longitude!!)).success)

        val tooFar = VisitPayload(UUID.randomUUID().toString(), outlet.id, Instant.now().toString(), 24.5, 91.5, "PRODUCTIVE", beat.id)
        val farResult = api.sync(SyncBatch(emptyList(), emptyList(), listOf(tooFar), emptyList())).data!!.visits.single()
        assertFalse(farResult.ok)
        assertTrue(farResult.error.orEmpty().endsWith("m from the store."))

        val now = Instant.now().toString()
        val visit = VisitPayload(UUID.randomUUID().toString(), outlet.id, now, outlet.latitude, outlet.longitude, "PRODUCTIVE", beat.id)
        val noOrder = VisitPayload(UUID.randomUUID().toString(), closed.id, now, closed.latitude, closed.longitude, "NON_PRODUCTIVE", beat.id, day.reasons.first().id, "Closed for lunch")
        val order = OrderPayload(UUID.randomUUID().toString(), visit.clientId, outlet.id, now, "REGULAR", listOf(OrderLinePayload(product.id, 3)), discountPercent = 10.0)
        val shop = NewOutletPayload(UUID.randomUUID().toString(), "Contract Test Shop", "পরীক্ষা দোকান", "Owner", "017", "Road 1", "রোড ১", day.channels.first().id, 23.8, 90.4)
        val batch = SyncBatch(listOf(shop), emptyList(), listOf(visit, noOrder), listOf(order))

        val first = api.sync(batch).data!!
        assertTrue(first.toString(), (first.visits + first.orders + first.newOutlets).all { it.ok })

        val repeat = api.sync(batch).data!!
        assertEquals(first.orders.single().serverId, repeat.orders.single().serverId)

        val jpeg = Base64.getDecoder().decode(TINY_JPEG)
        val part = MultipartBody.Part.createFormData("photo", "shop.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        api.uploadPhoto(visit.clientId, part)

        val summary = api.summary().data!!
        assertTrue(summary.today.memos >= 1)
        assertEquals(afterDiscount(product.trade * 3, 10.0), summary.today.value.toDouble(), 0.01)
        assertEquals(beat.name, summary.today.beat)
        assertFalse(summary.today.dayClosed)

        assertTrue(api.skuReport().data!!.any { it.productId == product.id })
        assertTrue(api.outletReport().data!!.any { it.outletId == outlet.id })
        assertTrue(api.kpiReport().data!!.calls >= 2)
        val timeline = api.timeline().data!!
        assertTrue(timeline.any { it.reason != null })
        assertTrue(timeline.all { it.time.isNotBlank() })

        assertEquals(true, api.dayEnd().success)
        assertTrue(api.summary().data!!.today.dayClosed)
    }

    @Test
    fun teamScreensReadTheRealApi() = runBlocking {
        assumeTrue("No test backend configured", url != null)
        token = api.login(LoginRequest(System.getenv("ROUTARA_TEST_EMAIL"), System.getenv("ROUTARA_TEST_PASSWORD"))).data!!.accessToken

        api.myOrders().data!!.forEach { assertTrue(it.deliveryStatus.isNotBlank()) }
        api.dues().data!!.forEach { assertTrue(it.due.toDouble() > 0) }
        api.offers().data!!

        val start = java.time.LocalDate.now().plusDays(200 + (0..3000).random().toLong()).toString()
        val leave = api.addLeave(LeaveBody("CASUAL", start, start, "Contract test")).data!!
        assertEquals(1, leave.days)
        assertTrue(api.leave().data!!.results.any { it.id == leave.id })
        assertEquals("CANCELLED", api.cancelLeave(leave.id).data!!.status)

        val expense = api.addExpense(ExpenseBody(java.time.LocalDate.now().minusDays(1).toString(), "TRANSPORT", "55.50", "Bus")).data!!
        assertEquals("PENDING", expense.status)
        assertTrue(api.expenses().data!!.results.any { it.id == expense.id })
        assertEquals("CANCELLED", api.cancelExpense(expense.id).data!!.status)

        val person = api.contacts().data!!.results.first()
        val chat = api.startChat(person.id).data!!
        val sent = api.send(chat.id, MessageBody("Contract test")).data!!
        assertEquals(sent.id, api.messages(chat.id).data!!.results.first().id)
        assertEquals(true, api.seen(chat.id).success)
        assertEquals("Contract test", api.chats().data!!.results.first { it.id == chat.id }.lastMessage?.message)
    }

    private companion object {
        const val TINY_JPEG =
            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////" +
                "wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAAA//EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AN//Z"
    }
}
