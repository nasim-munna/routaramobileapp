package com.routara.field

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.routara.field.data.DaySummary
import com.routara.field.data.LocalOrder
import com.routara.field.data.Me
import com.routara.field.data.MonthProgress
import com.routara.field.data.Named
import com.routara.field.data.Outlet
import com.routara.field.data.Pending
import com.routara.field.data.Product
import com.routara.field.data.Summary
import com.routara.field.ui.screens.ArriveScreen
import com.routara.field.ui.screens.ArriveState
import com.routara.field.ui.screens.CatalogScreen
import com.routara.field.ui.screens.HomeScreen
import com.routara.field.ui.screens.HomeState
import com.routara.field.ui.screens.LoginScreen
import com.routara.field.ui.screens.NewOutletScreen
import com.routara.field.ui.screens.NoOrderScreen
import com.routara.field.ui.screens.OrderScreen
import com.routara.field.ui.screens.OrdersScreen
import com.routara.field.ui.screens.PhotoScreen
import com.routara.field.ui.screens.ReviewScreen
import com.routara.field.ui.screens.RouteScreen
import com.routara.field.ui.screens.RouteStop
import com.routara.field.ui.screens.SyncScreen
import com.routara.field.ui.screens.TargetScreen
import com.routara.field.ui.screens.VisitDoneScreen
import com.routara.field.ui.theme.RoutaraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each screen to a PNG, so the design can be reviewed with no phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val shop = Outlet(1, "OL000001", "Lake View Store", ownerName = "Sample Owner", phone = "01700000000", address = "Road 12, Gulshan", channelName = "Grocery", latitude = 23.79, longitude = 90.41)
    private val second = shop.copy(id = 2, code = "OL000002", name = "Corner Mart", ownerName = "Sample Partner")
    private val third = shop.copy(id = 3, code = "OL000003", name = "Green Grocery", address = "Block C, Banani")
    private val products = listOf(
        Product(1, "SN-001", "Salted Crackers 100g", "Snacks", "pack", 48, "18.00", "20.00"),
        Product(2, "SN-002", "Chocolate Biscuit 75g", "Snacks", "pack", 48, "27.00", "30.00"),
        Product(3, "DR-001", "Mango Juice 250ml", "Drinks", "bottle", 24, "32.00", "35.00"),
    )
    private val summary = Summary(
        DaySummary("2026-11-07", "Gulshan North", 12, 5, 4, "18450.00", "4612.50", false),
        MonthProgress("500000.00", "340000.00", "160000.00", 68.0, "6957.00"),
    )
    private val home = HomeState(Me(1, "Rafi", "Ahmed", "rafi@example.com", null, "EMPLOYEE", "Gulshan"), summary, 12, 5, 4, 18450.0, 3, true, shop, 240.0, false)

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { RoutaraTheme(dark = dark) { content() } }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1200)
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    private fun homeScreen(state: HomeState) = @Composable {
        HomeScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun login() = shot("01_login") { LoginScreen(false, null) { _, _ -> } }

    @Test fun homeWorking() = shot("02_home", content = homeScreen(home))

    @Test fun homeCheckIn() = shot("03_home_check_in", content = homeScreen(home.copy(checkedIn = false)))

    @Test fun homeDark() = shot("04_home_dark", dark = true, content = homeScreen(home))

    @Test fun route() = shot("05_route") {
        RouteScreen("Gulshan North", "Gulshan", listOf(RouteStop(shop, true, 40.0), RouteStop(second, false, 240.0), RouteStop(third, false, 1250.0)), null, {}, {}, {}, {})
    }

    @Test fun arrivedNear() = shot("06_arrive_ok") { ArriveScreen(ArriveState(shop, 42.0, 100, false, true), {}, {}, {}, {}) }

    @Test fun arrivedFar() = shot("07_arrive_far") { ArriveScreen(ArriveState(shop, 12931.0, 100, false, true), {}, {}, {}, {}) }

    @Test fun photo() = shot("08_photo") { PhotoScreen(shop, null, {}, {}, {}) }

    @Test fun order() = shot("09_order") { OrderScreen(shop, products, mapOf(1L to 24, 3L to 12), false, {}, { _, _ -> }, {}, {}) }

    @Test fun review() = shot("10_review") { ReviewScreen(shop, mapOf(products[0] to 24, products[2] to 12), false, {}, {}) }

    @Test fun noOrder() = shot("11_no_order") { NoOrderScreen(shop, listOf(Named(1, "Shop closed"), Named(2, "Owner not available"), Named(3, "Enough stock")), false, {}) { _, _ -> } }

    @Test fun done() = shot("12_done") { VisitDoneScreen(shop, 816.0, second, {}, {}) }

    @Test fun orders() = shot("13_orders") {
        OrdersScreen(
            listOf(LocalOrder("a", "2026-11-07", "Lake View Store", "REGULAR", 2, 816.0, "2026-11-07T05:30:00Z"), LocalOrder("b", "2026-11-07", "Corner Mart", "READY_SALE", 1, 432.0, "2026-11-07T06:10:00Z")),
            setOf("b"),
        )
    }

    @Test fun target() = shot("14_target") { TargetScreen(summary.month) {} }

    @Test fun catalog() = shot("15_catalog") { CatalogScreen(products) {} }

    @Test fun sync() = shot("16_sync") {
        SyncScreen(listOf(Pending("1", "ORDER", "{}", "Order for Corner Mart"), Pending("2", "VISIT", "{}", "Visit to City Bazar", error = "This day is already closed.")), false, null, {}, {})
    }

    @Test fun newOutlet() = shot("17_new_outlet") { NewOutletScreen(listOf(Named(1, "Grocery"), Named(2, "Pharmacy")), 23.79 to 90.41, false, false, {}, {}) {} }
}
