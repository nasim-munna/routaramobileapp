package com.routara.field.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.routara.field.data.LocalOrder
import com.routara.field.data.Me
import com.routara.field.data.Outlet
import com.routara.field.data.Summary
import com.routara.field.data.distanceMeters
import com.routara.field.ui.components.Chip
import com.routara.field.ui.components.EmptyNote
import com.routara.field.ui.components.Field
import com.routara.field.ui.components.GhostButton
import com.routara.field.ui.components.GoldButton
import com.routara.field.ui.components.IconBadge
import com.routara.field.ui.components.ProgressRing
import com.routara.field.ui.components.RouteBackdrop
import com.routara.field.ui.components.ScreenBar
import com.routara.field.ui.components.SoftCard
import com.routara.field.ui.components.Stat
import com.routara.field.ui.theme.Danger
import com.routara.field.ui.theme.Marigold
import com.routara.field.ui.theme.MarigoldDeep
import com.routara.field.ui.theme.Mint
import com.routara.field.ui.theme.Success
import com.routara.field.ui.theme.Teal

@Composable
fun LoginScreen(busy: Boolean, error: String?, onSubmit: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    RouteBackdrop(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Spacer(Modifier.height(56.dp))
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Marigold), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Route, null, tint = com.routara.field.ui.theme.Pine, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Routara", style = MaterialTheme.typography.displayLarge, color = Color.White)
            Text("Every outlet. Every order. Every day.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(44.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sign in", style = MaterialTheme.typography.headlineMedium)
                    error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                    Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
                    Field("Password", password, { password = it }, secure = true, keyboard = KeyboardType.Password)
                    Spacer(Modifier.height(4.dp))
                    GoldButton("Sign in", Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotBlank(), busy = busy) {
                        onSubmit(email.trim(), password)
                    }
                }
            }
        }
    }
}

data class HomeState(
    val me: Me?,
    val summary: Summary?,
    val planned: Int,
    val visited: Int,
    val memos: Int,
    val value: Double,
    val pending: Int,
    val checkedIn: Boolean,
    val nextStop: Outlet?,
    val nextDistance: Double?,
    val dayClosed: Boolean,
)

@Composable
fun HomeScreen(
    state: HomeState,
    onCheck: () -> Unit,
    onStartVisit: (Outlet) -> Unit,
    onOpenRoute: () -> Unit,
    onSync: () -> Unit,
    onNewOutlet: () -> Unit,
    onReadySale: () -> Unit,
    onTarget: () -> Unit,
    onReports: () -> Unit,
    onCatalog: () -> Unit,
) {
    val month = state.summary?.month
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 112.dp)) {
        item {
            RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))) {
                Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 64.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(todayLabel(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                "${greeting()},\n${state.me?.firstName ?: "there"}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                            )
                        }
                        SyncPill(state.pending, onSync)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing((month?.percent ?: 0.0).toFloat()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(month?.percent ?: 0.0).toInt()}%", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                                Text("of target", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat("Achieved this month", taka(month?.achieved), onDark = true)
                            Stat("Needed per day", taka(month?.requiredPerDay), onDark = true)
                        }
                    }
                }
            }
        }

        item {
            SoftCard(Modifier.padding(horizontal = 18.dp).offset(y = (-40).dp).fillMaxWidth()) {
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TodayStat("Visited", "${state.visited}/${state.planned}")
                    TodayStat("Memos", "${state.memos}")
                    TodayStat("Value", taka(state.value))
                }
            }
        }

        item {
            Column(Modifier.offset(y = (-26).dp).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    state.dayClosed -> DoneCard()
                    !state.checkedIn -> CheckInCard(onCheck)
                    state.nextStop != null -> NextStopCard(state.nextStop, state.nextDistance, onStartVisit, onOpenRoute)
                    else -> RouteDoneCard(onOpenRoute)
                }

                Text("Quick actions", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile("New outlet", "Tag a shop", Icons.Rounded.AddBusiness, Teal, Modifier.weight(1f), onNewOutlet)
                    QuickTile("Ready sale", "Sell on the spot", Icons.Rounded.Bolt, MarigoldDeep, Modifier.weight(1f), onReadySale)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile("Target", "Track your month", Icons.Rounded.TrackChanges, Danger, Modifier.weight(1f), onTarget)
                    QuickTile("Reports", "Sales and visits", Icons.Rounded.BarChart, Color(0xFF6C5B9E), Modifier.weight(1f), onReports)
                }
                MenuRow("Product catalogue", "Prices and margins", Icons.Rounded.Inventory2, Teal, onCatalog)
            }
        }
    }
}

@Composable
private fun SyncPill(pending: Int, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (pending == 0) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload, null, tint = if (pending == 0) Mint else Marigold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (pending == 0) "Synced" else "$pending to sync", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun TodayStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CheckInCard(onCheck: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Ready to start?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Check in to begin your route. Your location is shared only while you're checked in.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            GoldButton("Check in", Modifier.fillMaxWidth(), icon = Icons.Rounded.Today, onClick = onCheck)
        }
    }
}

@Composable
private fun NextStopCard(outlet: Outlet, distance: Double?, onStart: (Outlet) -> Unit, onRoute: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next stop", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                distance?.let { Chip(distanceLabel(it), Teal, icon = Icons.Rounded.NearMe) }
            }
            Spacer(Modifier.height(6.dp))
            Text(outlet.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(outlet.channelName, outlet.address).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GoldButton("Start visit", Modifier.weight(1f), icon = Icons.Rounded.Storefront) { onStart(outlet) }
                GhostButton("Route", icon = Icons.Rounded.Route, onClick = onRoute)
            }
        }
    }
}

@Composable
private fun RouteDoneCard(onRoute: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onRoute) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Flag, Success)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Route complete", style = MaterialTheme.typography.titleLarge)
                Text("Every planned outlet is visited.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DoneCard() {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.CheckCircle, Success)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Day closed", style = MaterialTheme.typography.titleLarge)
                Text("Well done. See you tomorrow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon, tint)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MenuRow(title: String, subtitle: String, icon: ImageVector, tint: Color, onClick: () -> Unit, trailing: String? = null) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.let { Chip(it, MarigoldDeep) }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class RouteStop(val outlet: Outlet, val visited: Boolean, val distance: Double?)

/** Filtering the outlet list is shared by every screen that picks an outlet. */
fun List<RouteStop>.matching(term: String) = if (term.isBlank()) this else filter {
    val text = term.trim().lowercase()
    it.outlet.name.lowercase().contains(text) || it.outlet.code.orEmpty().lowercase().contains(text) || it.outlet.ownerName.lowercase().contains(text)
}

@Composable
fun RouteScreen(
    title: String,
    subtitle: String,
    stops: List<RouteStop>,
    onBack: (() -> Unit)?,
    onOpen: (Outlet) -> Unit,
    onCall: (Outlet) -> Unit,
    onDirections: (Outlet) -> Unit,
    onEdit: (Outlet) -> Unit,
) {
    var term by remember { mutableStateOf("") }
    val shown = stops.matching(term)
    val done = stops.count { it.visited }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (onBack != null) {
            ScreenBar(title, subtitle, onBack)
        } else {
            RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
                Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text("$subtitle · $done of ${stops.size} visited", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        OutlinedTextField(
            value = term,
            onValueChange = { term = it },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth(),
            placeholder = { Text("Search outlet, code or owner") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        if (shown.isEmpty()) {
            EmptyNote("No outlets here", "Sync to get your latest route.", Icons.Rounded.Storefront)
        }
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 112.dp)) {
            items(shown, key = { it.outlet.id }) { stop ->
                val index = stops.indexOf(stop)
                StopRow(stop, index + 1, index == stops.lastIndex, onOpen, onCall, onDirections, onEdit)
            }
        }
    }
}

/** A stop on the route line: the rail joins each outlet to the next. */
@Composable
private fun StopRow(stop: RouteStop, number: Int, last: Boolean, onOpen: (Outlet) -> Unit, onCall: (Outlet) -> Unit, onDirections: (Outlet) -> Unit, onEdit: (Outlet) -> Unit) {
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Column(Modifier.width(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (stop.visited) Success else Marigold),
                contentAlignment = Alignment.Center,
            ) {
                if (stop.visited) Icon(Icons.Rounded.Check, "Visited", tint = Color.White, modifier = Modifier.size(18.dp))
                else Text("$number", style = MaterialTheme.typography.labelMedium, color = com.routara.field.ui.theme.MarigoldInk)
            }
            if (!last) Box(Modifier.width(3.dp).weight(1f).background(MaterialTheme.colorScheme.outline))
        }
        SoftCard(Modifier.weight(1f).padding(bottom = 12.dp), onClick = { onOpen(stop.outlet) }) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stop.outlet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    stop.distance?.let { Chip(distanceLabel(it), Teal, icon = Icons.Rounded.NearMe) }
                }
                Text(
                    listOf(stop.outlet.code.orEmpty(), stop.outlet.channelName).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (stop.outlet.address.isNotBlank() || stop.outlet.ownerName.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOf(stop.outlet.ownerName, stop.outlet.address).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stop.outlet.phone.isNotBlank()) GhostButton("Call", Modifier.weight(1f), Icons.Rounded.Call) { onCall(stop.outlet) }
                    if (stop.outlet.latitude != null) GhostButton("Directions", Modifier.weight(1.5f), Icons.Rounded.Directions) { onDirections(stop.outlet) }
                    GhostButton("Edit", Modifier.weight(1f), Icons.Rounded.EditLocationAlt) { onEdit(stop.outlet) }
                }
            }
        }
    }
}

fun routeStops(outlets: List<Outlet>, visited: Collection<Long>, here: Pair<Double, Double>?) = outlets.map { outlet ->
    val distance = if (here != null && outlet.latitude != null && outlet.longitude != null) {
        distanceMeters(here.first, here.second, outlet.latitude, outlet.longitude)
    } else {
        null
    }
    RouteStop(outlet, outlet.id in visited, distance)
}

@Composable
fun OrdersScreen(orders: List<LocalOrder>, pendingIds: Set<String>) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
            Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp)) {
                Text("Today's orders", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("${orders.size} ${if (orders.size == 1) "memo" else "memos"} · ${taka(orders.sumOf { it.total })}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }
        if (orders.isEmpty()) EmptyNote("No orders yet today", "Orders appear here as you complete visits.", Icons.Rounded.Receipt)
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(orders, key = { it.clientId }) { order ->
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(if (order.kind == "READY_SALE") Icons.Rounded.Bolt else Icons.Rounded.Receipt, if (order.kind == "READY_SALE") MarigoldDeep else Teal)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(order.outletName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${clock(order.capturedAt)} · ${order.lines} ${if (order.lines == 1) "line" else "lines"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(taka(order.total), style = MaterialTheme.typography.titleMedium)
                            if (order.clientId in pendingIds) Chip("Waiting to sync", MarigoldDeep) else Chip("Synced", Success)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(me: Me?, territory: String?, checkedIn: Boolean, dayClosed: Boolean, pending: Int, onCheckOut: () -> Unit, onSync: () -> Unit, onDayEnd: () -> Unit, onSignOut: () -> Unit, onOpen: (String) -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
            Row(Modifier.statusBarsPadding().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Marigold), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = com.routara.field.ui.theme.Pine, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(me?.fullName ?: "", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(territory ?: "No territory yet", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuRow("My orders", "See what was delivered", Icons.Rounded.LocalShipping, Teal, { onOpen("my-orders") })
            MenuRow("Outlet dues", "What your outlets still owe", Icons.Rounded.Payments, Danger, { onOpen("dues") })
            MenuRow("Messages", "Talk to your manager", Icons.Rounded.Forum, Teal, { onOpen("messages") })
            MenuRow("Offers", "What's running now", Icons.Rounded.LocalOffer, MarigoldDeep, { onOpen("offers") })
            MenuRow("Leave", "Ask for time off", Icons.Rounded.EventBusy, Success, { onOpen("leave") })
            MenuRow("Expenses", "Claim what you spent", Icons.Rounded.Receipt, MarigoldDeep, { onOpen("expenses") })
            MenuRow("Sync", if (pending == 0) "Everything is up to date" else "Send your saved work", Icons.Rounded.CloudUpload, Teal, onSync, trailing = if (pending == 0) null else "$pending")
            if (checkedIn) MenuRow("Check out", "Stop sharing your location", Icons.Rounded.Today, MarigoldDeep, onCheckOut)
            if (!dayClosed) MenuRow("Day end", "Close today's field work", Icons.Rounded.Flag, Success, onDayEnd)
            MenuRow("Sign out", "Leave this device", Icons.AutoMirrored.Rounded.Logout, Danger, onSignOut)
            Spacer(Modifier.height(8.dp))
            Text("Routara 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
