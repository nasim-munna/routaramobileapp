package com.routara.field.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timeline
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.routara.field.data.Kpi
import com.routara.field.data.MonthProgress
import com.routara.field.data.Named
import com.routara.field.data.NewOutletPayload
import com.routara.field.data.Outlet
import com.routara.field.data.OutletRow
import com.routara.field.data.Pending
import com.routara.field.data.Product
import com.routara.field.data.SkuRow
import com.routara.field.data.TimelineRow
import com.routara.field.ui.components.Chip
import com.routara.field.ui.components.EmptyNote
import com.routara.field.ui.components.Field
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
import com.routara.field.ui.theme.Success
import com.routara.field.ui.theme.Teal
import java.util.UUID

@Composable
fun TargetScreen(month: MonthProgress?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        ScreenBar("Target", "This month's progress", onBack)
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing((month?.percent ?: 0.0).toFloat(), diameter = 190.dp, stroke = 18.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(month?.percent ?: 0.0).toInt()}%", style = MaterialTheme.typography.displayLarge, color = Color.White)
                            Text("achieved", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TargetTile("Target", taka(month?.target), Teal, Modifier.weight(1f))
                TargetTile("Achieved", taka(month?.achieved), Success, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TargetTile("Remaining", taka(month?.remaining), MarigoldDeep, Modifier.weight(1f))
                TargetTile("Needed per day", taka(month?.requiredPerDay), Danger, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TargetTile(label: String, value: String, tint: Color, modifier: Modifier) {
    SoftCard(modifier) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(38.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(10.dp))
            Stat(label, value)
        }
    }
}

enum class ReportTab(val title: String) { SKU("Products"), OUTLET("Outlets"), KPI("Month"), TIMELINE("Timeline") }

@Composable
fun ReportsScreen(
    tab: ReportTab,
    skus: List<SkuRow>,
    outlets: List<OutletRow>,
    kpi: Kpi?,
    timeline: List<TimelineRow>,
    error: String?,
    onTab: (ReportTab) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBar("Reports", "Your sales and visits", onBack)
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
        ) {
            ReportTab.entries.forEach { item ->
                val active = item == tab
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent).clickable { onTab(item) }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.title, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let { Text(it, Modifier.padding(horizontal = 18.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tab) {
                ReportTab.SKU -> {
                    if (skus.isEmpty()) item { EmptyNote("No orders today yet", "Products appear here after your first synced order.", Icons.Rounded.Inventory2) }
                    items(skus, key = { it.productId }) { ReportRow(it.name, "${it.quantity} units · ${it.cases} cases · ${it.memos} memos", taka(it.value)) }
                }
                ReportTab.OUTLET -> {
                    if (outlets.isEmpty()) item { EmptyNote("No outlet orders today", "Data appears here after your first synced order.") }
                    items(outlets, key = { it.outletId }) { ReportRow(it.name, "${it.memos} memos · ${it.lines} lines", taka(it.value)) }
                }
                ReportTab.KPI -> {
                    if (kpi == null || kpi.calls == 0) {
                        item { EmptyNote("No calls this month yet") }
                    } else {
                        item {
                            SoftCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Stat("Calls", "${kpi.calls}", Modifier.weight(1f))
                                    Stat("Productive", "${kpi.productivePercent}%", Modifier.weight(1f))
                                    Stat("Sales", taka(kpi.value), Modifier.weight(1.3f))
                                }
                            }
                        }
                        items(kpi.days.reversed(), key = { it.date }) { ReportRow(it.date, "${it.calls} calls · ${it.productive} productive · ${it.memos} memos", taka(it.value)) }
                    }
                }
                ReportTab.TIMELINE -> {
                    if (timeline.isEmpty()) item { EmptyNote("No visits today yet", "Your day appears here as you work.", Icons.Rounded.Timeline) }
                    items(timeline, key = { it.time + it.outlet }) { row ->
                        val detail = listOfNotNull(
                            clock(row.time),
                            row.reason,
                            row.distanceM?.let { distanceLabel(it) },
                        ).joinToString(" · ")
                        ReportRow(row.outlet, detail, row.value?.let { taka(it) } ?: "No order")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(title: String, subtitle: String, value: String) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CatalogScreen(products: List<Product>, onBack: () -> Unit) {
    var term by remember { mutableStateOf("") }
    val shown = products.filter { term.isBlank() || it.name.contains(term.trim(), true) || it.sku.contains(term.trim(), true) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBar("Product catalogue", "${products.size} products", onBack)
        OutlinedTextField(
            value = term,
            onValueChange = { term = it },
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            placeholder = { Text("Search product or SKU") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) item { EmptyNote("No products match", icon = Icons.Rounded.Inventory2) }
            items(shown, key = { it.id }) { ProductCard(it, null, null) }
        }
    }
}

@Composable
fun SyncScreen(items: List<Pending>, syncing: Boolean, message: String?, onBack: () -> Unit, onSync: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        ScreenBar("Sync", if (items.isEmpty()) "All up to date" else "${items.size} waiting", onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) item { EmptyNote("Everything is synced", "Your work is safely on the server.", Icons.Rounded.CloudDone) }
            items(items, key = { it.clientId }) { item ->
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(if (item.error == null) Icons.Rounded.CloudUpload else Icons.Rounded.ErrorOutline, if (item.error == null) MarigoldDeep else Danger, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.error ?: "Waiting to send", style = MaterialTheme.typography.bodySmall, color = if (item.error == null) MaterialTheme.colorScheme.onSurfaceVariant else Danger)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(18.dp)) {
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            GoldButton("Sync now", Modifier.fillMaxWidth(), busy = syncing, icon = Icons.Rounded.CloudUpload, onClick = onSync)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun LocationCard(location: Pair<Double, Double>?, locating: Boolean, onLocate: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), onClick = onLocate) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (location != null) Icons.Rounded.CheckCircle else Icons.Rounded.GpsFixed, if (location != null) Success else Teal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (location != null) "Location captured" else "Capture GPS location", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        locating -> "Finding you…"
                        location != null -> "%.6f, %.6f".format(java.util.Locale.US, location.first, location.second)
                        else -> "Stand at the shop, then tap here."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun NewOutletScreen(
    channels: List<Named>,
    location: Pair<Double, Double>?,
    locating: Boolean,
    saving: Boolean,
    onLocate: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (NewOutletPayload) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nameBn by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var addressBn by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar("New outlet", "Your manager checks it before it goes live", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard("Shop") {
                Field("Outlet name", name, { name = it })
                Field("দোকানের নাম (বাংলা)", nameBn, { nameBn = it })
            }
            SectionCard("Owner") {
                Field("Owner name", owner, { owner = it })
                Field("Mobile number", phone, { phone = it }, keyboard = KeyboardType.Phone)
            }
            SectionCard("Address") {
                Field("Address", address, { address = it })
                Field("ঠিকানা (বাংলা)", addressBn, { addressBn = it })
            }
            SectionCard("Channel") {
                channels.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (channel == item.id) Marigold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { channel = item.id }.padding(14.dp),
                    ) { Text(item.name, style = MaterialTheme.typography.titleMedium) }
                }
                if (channels.isEmpty()) Text("Sync to load channels.", style = MaterialTheme.typography.bodyMedium)
            }
            LocationCard(location, locating, onLocate)
        }
        GoldButton(
            "Save outlet",
            Modifier.navigationBarsPadding().padding(18.dp).fillMaxWidth(),
            enabled = name.isNotBlank() && channel != null && location != null,
            busy = saving,
        ) {
            onSubmit(NewOutletPayload(UUID.randomUUID().toString(), name.trim(), nameBn.trim(), owner.trim(), phone.trim(), address.trim(), addressBn.trim(), channel!!, location?.first, location?.second))
        }
    }
}

@Composable
fun EditOutletScreen(
    outlet: Outlet,
    location: Pair<Double, Double>?,
    locating: Boolean,
    saving: Boolean,
    onLocate: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (nameBn: String, owner: String, phone: String, address: String, addressBn: String) -> Unit,
) {
    var nameBn by remember { mutableStateOf(outlet.nameBn) }
    var owner by remember { mutableStateOf(outlet.ownerName) }
    var phone by remember { mutableStateOf(outlet.phone) }
    var address by remember { mutableStateOf(outlet.address) }
    var addressBn by remember { mutableStateOf(outlet.addressBn) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar("Edit outlet", outlet.name, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard("Details") {
                Field("দোকানের নাম (বাংলা)", nameBn, { nameBn = it })
                Field("Owner name", owner, { owner = it })
                Field("Mobile number", phone, { phone = it }, keyboard = KeyboardType.Phone)
                Field("Address", address, { address = it })
                Field("ঠিকানা (বাংলা)", addressBn, { addressBn = it })
            }
            if (outlet.latitude == null) {
                LocationCard(location, locating, onLocate)
            } else {
                Chip("Location is set", Success, icon = Icons.Rounded.CheckCircle)
            }
        }
        GoldButton("Save changes", Modifier.navigationBarsPadding().padding(18.dp).fillMaxWidth(), busy = saving) {
            onSubmit(nameBn.trim(), owner.trim(), phone.trim(), address.trim(), addressBn.trim())
        }
    }
}
