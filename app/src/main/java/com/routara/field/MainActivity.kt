package com.routara.field

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.routara.field.data.Kpi
import com.routara.field.data.Me
import com.routara.field.data.Outlet
import com.routara.field.data.OutletRow
import com.routara.field.data.OutletUpdatePayload
import com.routara.field.data.Product
import com.routara.field.data.Repository
import com.routara.field.data.afterDiscount
import com.routara.field.data.RepositoryHolder
import com.routara.field.data.SkuRow
import com.routara.field.data.TimelineRow
import com.routara.field.data.TrackingService
import com.routara.field.data.CheckInOut
import com.routara.field.data.currentLocation
import com.routara.field.data.distanceMeters
import com.routara.field.data.friendly
import com.routara.field.data.hasLocationPermission
import com.routara.field.data.shrinkPhoto
import com.routara.field.data.todayIso
import com.routara.field.ui.screens.AccountScreen
import com.routara.field.ui.screens.ArriveScreen
import com.routara.field.ui.screens.ArriveState
import com.routara.field.ui.screens.ChatRoute
import com.routara.field.ui.screens.ChatsRoute
import com.routara.field.ui.screens.DuesRoute
import com.routara.field.ui.screens.ExpenseRoute
import com.routara.field.ui.screens.LeaveRoute
import com.routara.field.ui.screens.MyOrdersRoute
import com.routara.field.ui.screens.OffersRoute
import com.routara.field.ui.screens.CatalogScreen
import com.routara.field.ui.screens.EditOutletScreen
import com.routara.field.ui.screens.HomeScreen
import com.routara.field.ui.screens.HomeState
import com.routara.field.ui.screens.LoginScreen
import com.routara.field.ui.screens.NewOutletScreen
import com.routara.field.ui.screens.NoOrderScreen
import com.routara.field.ui.screens.OrderScreen
import com.routara.field.ui.screens.OrdersScreen
import com.routara.field.ui.screens.PhotoScreen
import com.routara.field.ui.screens.ReportTab
import com.routara.field.ui.screens.ReportsScreen
import com.routara.field.ui.screens.ReviewScreen
import com.routara.field.ui.screens.RouteScreen
import com.routara.field.ui.screens.SyncScreen
import com.routara.field.ui.screens.TargetScreen
import com.routara.field.ui.screens.VisitDoneScreen
import com.routara.field.ui.screens.routeStops
import com.routara.field.ui.theme.Marigold
import com.routara.field.ui.theme.Pine
import com.routara.field.ui.theme.RoutaraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RoutaraApp : Application(), RepositoryHolder {
    override val repository by lazy { Repository(this) }
}

/** The visit in progress. Nothing here is saved until the rep submits. */
data class VisitDraft(
    val outlet: Outlet,
    val kind: String,
    val location: Pair<Double, Double>? = null,
    val locating: Boolean = true,
    val photo: String? = null,
    val cart: Map<Long, Int> = emptyMap(),
    val saving: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as RoutaraApp).repository
    private val context: Context get() = getApplication()

    val me = MutableStateFlow<Me?>(null)
    val signedIn = MutableStateFlow(repository.session.signedIn)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val notice = MutableStateFlow<String?>(null)
    val checkedIn = MutableStateFlow(false)
    val here = MutableStateFlow<Pair<Double, Double>?>(null)
    val locating = MutableStateFlow(false)
    val syncing = MutableStateFlow(false)
    val dayClosed = MutableStateFlow(false)

    private val _visit = MutableStateFlow<VisitDraft?>(null)
    val visit = _visit.asStateFlow()

    val skus = MutableStateFlow<List<SkuRow>>(emptyList())
    val outletRows = MutableStateFlow<List<OutletRow>>(emptyList())
    val kpi = MutableStateFlow<Kpi?>(null)
    val timeline = MutableStateFlow<List<TimelineRow>>(emptyList())
    val reportError = MutableStateFlow<String?>(null)

    init {
        if (signedIn.value) refreshAll()
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        busy.value = true
        error.value = null
        runCatching { repository.signIn(email, password) }
            .onSuccess {
                me.value = it
                signedIn.value = true
                refreshAll()
            }
            .onFailure { error.value = it.friendly(repository.gson) }
        busy.value = false
    }

    fun refreshAll() = viewModelScope.launch {
        locate()
        runCatching {
            val profile = repository.api.me().data
            me.value = profile
            repository.refresh()
            dayClosed.value = repository.summary.value?.today?.dayClosed == true
            profile?.let { user ->
                val blocks = repository.api.attendance(user.id, todayIso()).data?.blocks.orEmpty()
                checkedIn.value = blocks.lastOrNull()?.let { it.checkInTime != null && it.checkOutTime == null } == true
                // Android can kill the app mid-shift, so pick tracking back up.
                if (checkedIn.value) TrackingService.start(context)
            }
        }.onFailure {
            if (!repository.session.signedIn) signedIn.value = false
        }
    }

    suspend fun locate() {
        locating.value = true
        currentLocation(context)?.let { here.value = it }
        locating.value = false
    }

    fun locateNow() = viewModelScope.launch { locate() }

    fun toggleCheck() = viewModelScope.launch {
        val point = currentLocation(context)
        if (point == null) {
            notice.value = "Turn on location to check in."
            return@launch
        }
        here.value = point
        val going = !checkedIn.value
        runCatching {
            val body = CheckInOut(point.first, point.second)
            if (going) repository.api.checkIn(body) else repository.api.checkOut(body)
        }.onSuccess {
            checkedIn.value = going
            if (going) TrackingService.start(context) else TrackingService.stop(context)
            notice.value = if (going) "You're checked in." else "You're checked out."
        }.onFailure { notice.value = it.friendly(repository.gson) }
    }

    fun startVisit(outlet: Outlet, kind: String) {
        _visit.value = VisitDraft(outlet, kind)
        recheck()
    }

    fun recheck() = viewModelScope.launch {
        _visit.update { it?.copy(locating = true) }
        val point = currentLocation(context)
        if (point != null) here.value = point
        _visit.update { it?.copy(location = point, locating = false) }
    }

    fun newPhotoFile(): File = File(File(context.filesDir, "photos").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")

    fun setPhoto(path: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { shrinkPhoto(path) }
        _visit.update { it?.copy(photo = path) }
    }

    fun setQuantity(product: Product, quantity: Int) = _visit.update { draft ->
        draft?.copy(cart = if (quantity <= 0) draft.cart - product.id else draft.cart + (product.id to quantity))
    }

    fun cartProducts(): Map<Product, Int> {
        val draft = _visit.value ?: return emptyMap()
        val products = repository.bootstrap.value?.products.orEmpty()
        return products.filter { (draft.cart[it.id] ?: 0) > 0 }.associateWith { draft.cart.getValue(it.id) }
    }

    fun submitVisit(reasonId: Long?, note: String, discountPercent: Double = 0.0, done: () -> Unit) = viewModelScope.launch {
        val draft = _visit.value ?: return@launch
        _visit.update { it?.copy(saving = true) }
        repository.saveVisit(draft.outlet, draft.kind, draft.location, cartProducts(), reasonId, note, draft.photo, discountPercent)
        _visit.update { it?.copy(saving = false) }
        done()
    }

    fun sync() = viewModelScope.launch {
        syncing.value = true
        runCatching { repository.refresh() }
            .onSuccess { notice.value = "Synced." }
            .onFailure { notice.value = it.friendly(repository.gson) }
        syncing.value = false
    }

    fun endDay() = viewModelScope.launch {
        runCatching {
            repository.flush()
            repository.api.dayEnd()
        }.onSuccess {
            dayClosed.value = true
            if (checkedIn.value) {
                currentLocation(context)?.let { point -> runCatching { repository.api.checkOut(CheckInOut(point.first, point.second)) } }
            }
            checkedIn.value = false
            TrackingService.stop(context)
            runCatching { repository.refresh() }
            notice.value = "Day closed. Well done."
        }.onFailure { notice.value = it.friendly(repository.gson) }
    }

    fun signOut() = viewModelScope.launch {
        TrackingService.stop(context)
        repository.signOut()
        me.value = null
        signedIn.value = false
    }

    fun loadReport(tab: ReportTab) = viewModelScope.launch {
        reportError.value = null
        runCatching {
            when (tab) {
                ReportTab.SKU -> skus.value = repository.api.skuReport().data.orEmpty()
                ReportTab.OUTLET -> outletRows.value = repository.api.outletReport().data.orEmpty()
                ReportTab.KPI -> kpi.value = repository.api.kpiReport().data
                ReportTab.TIMELINE -> timeline.value = repository.api.timeline().data.orEmpty()
            }
        }.onFailure { reportError.value = it.friendly(repository.gson) }
    }

    fun distanceTo(outlet: Outlet, point: Pair<Double, Double>?): Double? =
        if (point != null && outlet.latitude != null && outlet.longitude != null) {
            distanceMeters(point.first, point.second, outlet.latitude, outlet.longitude)
        } else {
            null
        }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RoutaraTheme {
                val model: AppViewModel = viewModel()
                val signedIn by model.signedIn.collectAsStateWithLifecycle()
                if (signedIn) {
                    Permissions(model)
                    Signedin(model)
                } else {
                    val busy by model.busy.collectAsStateWithLifecycle()
                    val error by model.error.collectAsStateWithLifecycle()
                    LoginScreen(busy, error, model::signIn)
                }
            }
        }
    }
}

@Composable
private fun Permissions(model: AppViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission(context)) model.locateNow()
    }
    LaunchedEffect(Unit) {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(wanted.toTypedArray())
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Today", Icons.Rounded.Home),
    ROUTE("Route", Icons.Rounded.Route),
    ORDERS("Orders", Icons.Rounded.Receipt),
    ME("Me", Icons.Rounded.Person),
}

@Composable
private fun FloatingTabs(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier) {
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Pine).padding(6.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == current
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (active) Marigold else Color.Transparent).clickable { onSelect(tab) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, null, tint = if (active) Pine else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                Text(tab.label, style = MaterialTheme.typography.labelMedium, color = if (active) Pine else Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

private fun openDial(context: Context, phone: String) = context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

private fun openDirections(context: Context, outlet: Outlet) {
    val uri = Uri.parse("google.navigation:q=${outlet.latitude},${outlet.longitude}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${outlet.latitude},${outlet.longitude}"))) }
}

@Composable
private fun Signedin(model: AppViewModel) {
    val nav = rememberNavController()
    val notice by model.notice.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = "main") {
            composable("main") { MainTabs(model, nav) }
            composable("arrive") { ArriveRoute(model, nav) }
            composable("photo") { PhotoRoute(model, nav) }
            composable("order") { OrderRoute(model, nav) }
            composable("review") { ReviewRoute(model, nav) }
            composable("noorder") { NoOrderRoute(model, nav) }
            composable("done/{total}", listOf(navArgument("total") { type = NavType.StringType })) { entry ->
                DoneRoute(model, nav, entry.arguments?.getString("total")?.toDoubleOrNull())
            }
            composable("sync") { SyncRoute(model, nav) }
            composable("target") {
                val summary by model.repository.summary.collectAsStateWithLifecycle()
                TargetScreen(summary?.month) { nav.popBackStack() }
            }
            composable("reports") { ReportsRoute(model, nav) }
            composable("catalog") {
                val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
                CatalogScreen(bootstrap?.products.orEmpty()) { nav.popBackStack() }
            }
            composable("new-outlet") { NewOutletRoute(model, nav) }
            composable("edit/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                EditOutletRoute(model, nav, entry.arguments?.getLong("id") ?: 0)
            }
            composable("pick-sale") { PickSaleRoute(model, nav) }
            composable("my-orders") { MyOrdersRoute(model.repository) { nav.popBackStack() } }
            composable("dues") { DuesRoute(model.repository) { nav.popBackStack() } }
            composable("offers") { OffersRoute(model.repository) { nav.popBackStack() } }
            composable("leave") { LeaveRoute(model.repository) { nav.popBackStack() } }
            composable("expenses") { ExpenseRoute(model.repository) { nav.popBackStack() } }
            composable("messages") {
                ChatsRoute(model.repository, { nav.popBackStack() }) { id, name -> nav.navigate("chat/$id/${Uri.encode(name)}") }
            }
            composable("chat/{id}/{name}") { entry ->
                val me by model.me.collectAsStateWithLifecycle()
                ChatRoute(model.repository, entry.arguments?.getString("id").orEmpty(), entry.arguments?.getString("name").orEmpty(), me?.id ?: 0) { nav.popBackStack() }
            }
        }
        notice?.let { text ->
            LaunchedEffect(text) {
                kotlinx.coroutines.delay(2800)
                model.notice.value = null
            }
            Snackbar(Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)) { Text(text) }
        }
    }
}

@Composable
private fun MainTabs(model: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.HOME) }
    var confirmDayEnd by remember { mutableStateOf(false) }
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val summary by model.repository.summary.collectAsStateWithLifecycle()
    val me by model.me.collectAsStateWithLifecycle()
    val here by model.here.collectAsStateWithLifecycle()
    val checkedIn by model.checkedIn.collectAsStateWithLifecycle()
    val dayClosed by model.dayClosed.collectAsStateWithLifecycle()
    val pending by model.repository.pending.collectAsStateWithLifecycle(emptyList())
    val orders by model.repository.ordersToday().collectAsStateWithLifecycle(emptyList())

    val data = bootstrap
    val beat = data?.beats?.firstOrNull { it.id == data.todayBeat }
    val byId = data?.outlets.orEmpty().associateBy { it.id }
    val planned = beat?.outletIds?.mapNotNull(byId::get).orEmpty()
    val routeOutlets = planned.ifEmpty { data?.outlets.orEmpty().filter { it.status == "APPROVED" } }
    val visited = data?.visitedOutletIds.orEmpty()
    val stops = routeStops(routeOutlets, visited, here)
    val next = stops.firstOrNull { !it.visited }

    Box(Modifier.fillMaxSize()) {
        when (tab) {
            Tab.HOME -> HomeScreen(
                state = HomeState(
                    me = me,
                    summary = summary,
                    planned = stops.size,
                    visited = stops.count { it.visited },
                    memos = orders.size,
                    value = orders.sumOf { it.total },
                    pending = pending.size,
                    checkedIn = checkedIn,
                    nextStop = next?.outlet,
                    nextDistance = next?.distance,
                    dayClosed = dayClosed,
                ),
                onCheck = model::toggleCheck,
                onStartVisit = { model.startVisit(it, "REGULAR"); nav.navigate("arrive") },
                onOpenRoute = { tab = Tab.ROUTE },
                onSync = { nav.navigate("sync") },
                onNewOutlet = { nav.navigate("new-outlet") },
                onReadySale = { nav.navigate("pick-sale") },
                onTarget = { nav.navigate("target") },
                onReports = { nav.navigate("reports") },
                onCatalog = { nav.navigate("catalog") },
            )
            Tab.ROUTE -> RouteScreen(
                title = beat?.name ?: "All outlets",
                subtitle = data?.territory ?: "Your route",
                stops = stops,
                onBack = null,
                onOpen = { model.startVisit(it, "REGULAR"); nav.navigate("arrive") },
                onCall = { openDial(context, it.phone) },
                onDirections = { openDirections(context, it) },
                onEdit = { nav.navigate("edit/${it.id}") },
            )
            Tab.ORDERS -> OrdersScreen(orders, pending.map { it.clientId }.toSet())
            Tab.ME -> AccountScreen(
                me = me,
                territory = data?.territory,
                checkedIn = checkedIn,
                dayClosed = dayClosed,
                pending = pending.size,
                onCheckOut = model::toggleCheck,
                onSync = { nav.navigate("sync") },
                onDayEnd = { confirmDayEnd = true },
                onSignOut = model::signOut,
                onOpen = { nav.navigate(it) },
            )
        }
        FloatingTabs(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmDayEnd) {
        AlertDialog(
            onDismissRequest = { confirmDayEnd = false },
            title = { Text("Close today?") },
            text = { Text("You can't add more visits or orders for today after this.") },
            confirmButton = {
                TextButton(onClick = { confirmDayEnd = false; model.endDay() }) { Text("Close day") }
            },
            dismissButton = { TextButton(onClick = { confirmDayEnd = false }) { Text("Not yet") } },
        )
    }
}

@Composable
private fun ArriveRoute(model: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    ArriveScreen(
        state = ArriveState(
            outlet = visit.outlet,
            distance = model.distanceTo(visit.outlet, visit.location),
            radius = bootstrap?.visitRadiusM ?: 100,
            locating = visit.locating,
            hasOutletLocation = visit.outlet.latitude != null,
        ),
        onBack = { nav.popBackStack() },
        onRetry = model::recheck,
        onContinue = { nav.navigate("photo") },
        onDirections = { openDirections(context, visit.outlet) },
    )
}

@Composable
private fun PhotoRoute(model: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    var target by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) target?.let { model.setPhoto(it.path) }
    }
    PhotoScreen(
        outlet = visit.outlet,
        photo = visit.photo,
        required = bootstrap?.photoRequired == true,
        onBack = { nav.popBackStack() },
        onTake = {
            val file = model.newPhotoFile()
            target = file
            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
        },
        onContinue = { nav.navigate("order") },
    )
}

@Composable
private fun OrderRoute(model: AppViewModel, nav: NavHostController) {
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    OrderScreen(
        outlet = visit.outlet,
        products = bootstrap?.products.orEmpty(),
        cart = visit.cart,
        readySale = visit.kind == "READY_SALE",
        onBack = { nav.popBackStack() },
        onQuantity = model::setQuantity,
        onNoOrder = { nav.navigate("noorder") },
        onReview = { nav.navigate("review") },
    )
}

private fun finish(nav: NavHostController, total: Double?) = nav.navigate("done/${total ?: "none"}") {
    popUpTo("main")
}

@Composable
private fun ReviewRoute(model: AppViewModel, nav: NavHostController) {
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    val cart = model.cartProducts()
    ReviewScreen(visit.outlet, cart, visit.saving, bootstrap?.maxDiscountPercent ?: 0.0, { nav.popBackStack() }) { percent ->
        val gross = cart.entries.sumOf { (product, quantity) -> product.trade * quantity }
        model.submitVisit(null, "", percent) { finish(nav, afterDiscount(gross, percent)) }
    }
}

@Composable
private fun NoOrderRoute(model: AppViewModel, nav: NavHostController) {
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    NoOrderScreen(visit.outlet, bootstrap?.reasons.orEmpty(), visit.saving, { nav.popBackStack() }) { reason, note ->
        model.submitVisit(reason, note) { finish(nav, null) }
    }
}

@Composable
private fun DoneRoute(model: AppViewModel, nav: NavHostController, total: Double?) {
    val draft by model.visit.collectAsStateWithLifecycle()
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val visit = draft ?: return
    val data = bootstrap
    val byId = data?.outlets.orEmpty().associateBy { it.id }
    val next = data?.beats?.firstOrNull { it.id == data.todayBeat }?.outletIds?.mapNotNull(byId::get)
        ?.firstOrNull { it.id !in data.visitedOutletIds }
    VisitDoneScreen(
        outlet = visit.outlet,
        total = total,
        nextStop = next,
        onNext = {
            model.startVisit(it, "REGULAR")
            nav.navigate("arrive") { popUpTo("main") }
        },
        onHome = { nav.popBackStack("main", false) },
    )
}

@Composable
private fun SyncRoute(model: AppViewModel, nav: NavHostController) {
    val pending by model.repository.pending.collectAsStateWithLifecycle(emptyList())
    val syncing by model.syncing.collectAsStateWithLifecycle()
    SyncScreen(pending, syncing, null, { nav.popBackStack() }, model::sync)
}

@Composable
private fun ReportsRoute(model: AppViewModel, nav: NavHostController) {
    var tab by remember { mutableStateOf(ReportTab.SKU) }
    val skus by model.skus.collectAsStateWithLifecycle()
    val outlets by model.outletRows.collectAsStateWithLifecycle()
    val kpi by model.kpi.collectAsStateWithLifecycle()
    val timeline by model.timeline.collectAsStateWithLifecycle()
    val error by model.reportError.collectAsStateWithLifecycle()
    LaunchedEffect(tab) { model.loadReport(tab) }
    ReportsScreen(tab, skus, outlets, kpi, timeline, error, { tab = it }, { nav.popBackStack() })
}

@Composable
private fun NewOutletRoute(model: AppViewModel, nav: NavHostController) {
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val here by model.here.collectAsStateWithLifecycle()
    val locating by model.locating.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    NewOutletScreen(bootstrap?.channels.orEmpty(), here, locating, saving, model::locateNow, { nav.popBackStack() }) { payload ->
        saving = true
        scope.launch {
            model.repository.saveNewOutlet(payload)
            model.notice.value = "Outlet saved. Your manager will check it."
            nav.popBackStack()
        }
    }
}

@Composable
private fun EditOutletRoute(model: AppViewModel, nav: NavHostController, id: Long) {
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val here by model.here.collectAsStateWithLifecycle()
    val locating by model.locating.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val outlet = bootstrap?.outlets?.firstOrNull { it.id == id } ?: return
    EditOutletScreen(outlet, here, locating, saving, model::locateNow, { nav.popBackStack() }) { nameBn, owner, phone, address, addressBn ->
        saving = true
        scope.launch {
            model.repository.saveOutletUpdate(
                OutletUpdatePayload(UUID.randomUUID().toString(), outlet.id, nameBn, owner, phone, address, addressBn, here?.first, here?.second),
            )
            model.notice.value = "Changes saved."
            nav.popBackStack()
        }
    }
}

@Composable
private fun PickSaleRoute(model: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    val bootstrap by model.repository.bootstrap.collectAsStateWithLifecycle()
    val here by model.here.collectAsStateWithLifecycle()
    val outlets = bootstrap?.outlets.orEmpty().filter { it.status == "APPROVED" }
    RouteScreen(
        title = "Ready sale",
        subtitle = "Choose an outlet",
        stops = routeStops(outlets, emptyList(), here).sortedBy { it.distance ?: Double.MAX_VALUE },
        onBack = { nav.popBackStack() },
        onOpen = {
            model.startVisit(it, "READY_SALE")
            nav.navigate("arrive") { popUpTo("main") }
        },
        onCall = { openDial(context, it.phone) },
        onDirections = { openDirections(context, it) },
        onEdit = { nav.navigate("edit/${it.id}") },
    )
}
