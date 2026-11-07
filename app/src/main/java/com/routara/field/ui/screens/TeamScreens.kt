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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.routara.field.data.ChatMessage
import com.routara.field.data.ChatRow
import com.routara.field.data.Contact
import com.routara.field.data.DueRow
import com.routara.field.data.ExpenseBody
import com.routara.field.data.ExpenseRow
import com.routara.field.data.LeaveBody
import com.routara.field.data.LeaveRow
import com.routara.field.data.MessageBody
import com.routara.field.data.MyOrder
import com.routara.field.data.OfferRow
import com.routara.field.data.Repository
import com.routara.field.data.todayIso
import com.routara.field.ui.components.Chip
import com.routara.field.ui.components.EmptyNote
import com.routara.field.ui.components.Field
import com.routara.field.ui.components.GhostButton
import com.routara.field.ui.components.GoldButton
import com.routara.field.ui.components.IconBadge
import com.routara.field.ui.components.ScreenBar
import com.routara.field.ui.components.SoftCard
import com.routara.field.ui.theme.Danger
import com.routara.field.ui.theme.Marigold
import com.routara.field.ui.theme.MarigoldDeep
import com.routara.field.ui.theme.Pine
import com.routara.field.ui.theme.Success
import com.routara.field.ui.theme.Teal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** These screens work live, so unlike visits they don't promise to save offline. */
private fun Throwable.onlineMessage(gson: Gson): String = when (this) {
    is IOException -> "No connection. Try again when you're online."
    is retrofit2.HttpException -> runCatching {
        gson.fromJson(response()?.errorBody()?.string(), com.routara.field.data.Envelope::class.java)?.message
    }.getOrNull() ?: "The server couldn't do that. Please try again."
    else -> message ?: "Something went wrong."
}

private class Loader<T>(val rows: T?, val error: String?, val reload: () -> Unit)

/** Fetches once on opening, and again whenever `reload` is called. */
@Composable
private fun <T> rememberLoader(repository: Repository, load: suspend () -> T): Loader<T> {
    var tick by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<T?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tick) {
        runCatching { load() }
            .onSuccess { rows = it; error = null }
            .onFailure { error = it.onlineMessage(repository.gson) }
    }
    return Loader(rows, error) { tick++ }
}

/** Dues and claims are settled to the paisa, so they aren't rounded for display. */
private fun exactTaka(value: String) = taka(value.toDoubleOrNull() ?: 0.0, exact = true)

private fun shortDate(iso: String): String = runCatching {
    LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}.getOrDefault(iso)

@Composable
private fun StatusChip(status: String) {
    val (label, tint) = when (status) {
        "APPROVED" -> "Approved" to Success
        "REJECTED" -> "Rejected" to Danger
        "CANCELLED" -> "Cancelled" to Color.Gray
        else -> "Waiting" to MarigoldDeep
    }
    Chip(label, tint)
}

@Composable
private fun ListScaffold(title: String, subtitle: String?, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBar(title, subtitle, onBack)
        content()
    }
}

@Composable
private fun ErrorLine(text: String?) {
    text?.let { Text(it, Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun Waiting() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
}

@Composable
private fun InfoCard(title: String, subtitle: String, icon: ImageVector, tint: Color, trailing: @Composable () -> Unit = {}) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

// ── My orders ─────────────────────────────────────────────────────────────

@Composable
fun MyOrdersList(rows: List<MyOrder>?, error: String?, onBack: () -> Unit) {
    ListScaffold("My orders", "Last 30 days", onBack) {
        ErrorLine(error)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows == null && error == null) item { Waiting() }
            if (rows?.isEmpty() == true) item { EmptyNote("No orders yet", "Synced orders appear here with their delivery.", Icons.Rounded.LocalShipping) }
            items(rows.orEmpty(), key = { it.id }) { order ->
                val (label, tint) = when (order.deliveryStatus) {
                    "DELIVERED" -> "Delivered" to Success
                    "PARTIAL" -> "Part delivered" to MarigoldDeep
                    "CANCELLED" -> "Cancelled" to Danger
                    else -> "Waiting" to Teal
                }
                val detail = buildString {
                    append(shortDate(order.date)).append(" · ").append(taka(order.total))
                    order.deliveredTotal?.takeIf { order.deliveryStatus == "PARTIAL" }?.let { append(" · ").append(taka(it)).append(" sent") }
                }
                InfoCard(order.outlet, detail, Icons.Rounded.LocalShipping, tint) { Chip(label, tint) }
            }
        }
    }
}

@Composable
fun MyOrdersRoute(repository: Repository, onBack: () -> Unit) {
    val data = rememberLoader(repository) { repository.api.myOrders().data.orEmpty() }
    MyOrdersList(data.rows, data.error, onBack)
}

// ── Outlet dues ───────────────────────────────────────────────────────────

@Composable
fun DuesList(rows: List<DueRow>?, error: String?, onBack: () -> Unit) {
    val total = rows.orEmpty().sumOf { it.due.toDoubleOrNull() ?: 0.0 }
    ListScaffold("Outlet dues", if (rows == null) "Your territory" else "${taka(total, exact = true)} to collect", onBack) {
        ErrorLine(error)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows == null && error == null) item { Waiting() }
            if (rows?.isEmpty() == true) item { EmptyNote("Nothing owed", "No outlet in your territory has a balance.", Icons.Rounded.Payments) }
            items(rows.orEmpty(), key = { it.outletId }) { row ->
                InfoCard(row.name, "Still to pay", Icons.Rounded.Storefront, Danger) {
                    Text(exactTaka(row.due), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun DuesRoute(repository: Repository, onBack: () -> Unit) {
    val data = rememberLoader(repository) { repository.api.dues().data.orEmpty() }
    DuesList(data.rows, data.error, onBack)
}

// ── Offers ────────────────────────────────────────────────────────────────

@Composable
fun OffersList(rows: List<OfferRow>?, error: String?, onBack: () -> Unit) {
    ListScaffold("Offers", "Running now", onBack) {
        ErrorLine(error)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows == null && error == null) item { Waiting() }
            if (rows?.isEmpty() == true) item { EmptyNote("No offers right now", icon = Icons.Rounded.LocalOffer) }
            items(rows.orEmpty(), key = { it.id }) { offer ->
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        IconBadge(Icons.Rounded.LocalOffer, MarigoldDeep, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(offer.title, style = MaterialTheme.typography.titleMedium)
                            if (offer.details.isNotBlank()) Text(offer.details, style = MaterialTheme.typography.bodyMedium)
                            Text("Until ${shortDate(offer.endDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OffersRoute(repository: Repository, onBack: () -> Unit) {
    val data = rememberLoader(repository) { repository.api.offers().data?.results.orEmpty() }
    OffersList(data.rows, data.error, onBack)
}

// ── Shared form pieces ────────────────────────────────────────────────────

@Composable
private fun ChoiceRow(options: List<Pair<String, String>>, chosen: String, onChoose: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (chosen == value) Marigold.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChoose(value) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { open = true }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = Teal)
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(shortDate(value), style = MaterialTheme.typography.titleMedium)
    }
    if (open) {
        val state = rememberDatePickerState(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

// ── Leave ─────────────────────────────────────────────────────────────────

private val leaveKinds = listOf("CASUAL" to "Casual", "SICK" to "Sick", "ANNUAL" to "Annual", "OTHER" to "Other")

@Composable
fun LeaveList(rows: List<LeaveRow>?, error: String?, onBack: () -> Unit, onNew: () -> Unit, onCancel: (LeaveRow) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        ScreenBar("Leave", "Your requests", onBack)
        ErrorLine(error)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows == null && error == null) item { Waiting() }
            if (rows?.isEmpty() == true) item { EmptyNote("No leave requests", icon = Icons.Rounded.EventBusy) }
            items(rows.orEmpty(), key = { it.id }) { row ->
                val kind = leaveKinds.firstOrNull { it.first == row.kind }?.second ?: row.kind
                val dates = if (row.startDate == row.endDate) shortDate(row.startDate) else "${shortDate(row.startDate)} to ${shortDate(row.endDate)}"
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$kind · ${row.days} ${if (row.days == 1) "day" else "days"}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            StatusChip(row.status)
                        }
                        Text(dates, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (row.rejectionReason.isNotBlank()) Text(row.rejectionReason, style = MaterialTheme.typography.bodySmall, color = Danger)
                        if (row.status == "PENDING") GhostButton("Cancel request", Modifier.fillMaxWidth()) { onCancel(row) }
                    }
                }
            }
        }
        GoldButton("Ask for leave", Modifier.padding(18.dp).fillMaxWidth(), icon = Icons.Rounded.Add, onClick = onNew)
    }
}

@Composable
fun LeaveForm(saving: Boolean, error: String?, onBack: () -> Unit, onSubmit: (LeaveBody) -> Unit) {
    var kind by remember { mutableStateOf("CASUAL") }
    var start by remember { mutableStateOf(todayIso()) }
    var end by remember { mutableStateOf(todayIso()) }
    var reason by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar("Ask for leave", "Your manager decides", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ChoiceRow(leaveKinds, kind) { kind = it }
            DateRow("From", start) {
                start = it
                if (end < it) end = it
            }
            DateRow("To", end) { end = it }
            Field("Reason", reason, { reason = it })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        GoldButton("Send request", Modifier.navigationBarsPadding().padding(18.dp).fillMaxWidth(), busy = saving) {
            onSubmit(LeaveBody(kind, start, end, reason.trim()))
        }
    }
}

@Composable
fun LeaveRoute(repository: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val data = rememberLoader(repository) { repository.api.leave().data?.results.orEmpty() }
    var adding by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (adding) {
        LeaveForm(saving, error, { adding = false; error = null }) { body ->
            saving = true
            scope.launch {
                runCatching { repository.api.addLeave(body) }
                    .onSuccess { adding = false; error = null; data.reload() }
                    .onFailure { error = it.onlineMessage(repository.gson) }
                saving = false
            }
        }
    } else {
        LeaveList(data.rows, data.error ?: error, onBack, { adding = true }) { row ->
            scope.launch {
                runCatching { repository.api.cancelLeave(row.id) }
                    .onSuccess { data.reload() }
                    .onFailure { error = it.onlineMessage(repository.gson) }
            }
        }
    }
}

// ── Expenses ──────────────────────────────────────────────────────────────

private val expenseKinds = listOf("TRANSPORT" to "Transport", "FOOD" to "Food", "PHONE" to "Phone", "OTHER" to "Other")

@Composable
fun ExpenseList(rows: List<ExpenseRow>?, error: String?, onBack: () -> Unit, onNew: () -> Unit, onCancel: (ExpenseRow) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        ScreenBar("Expenses", "Your claims", onBack)
        ErrorLine(error)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (rows == null && error == null) item { Waiting() }
            if (rows?.isEmpty() == true) item { EmptyNote("No expenses yet", icon = Icons.Rounded.Receipt) }
            items(rows.orEmpty(), key = { it.id }) { row ->
                val kind = expenseKinds.firstOrNull { it.first == row.category }?.second ?: row.category
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exactTaka(row.amount), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            StatusChip(row.status)
                        }
                        Text(listOf(kind, shortDate(row.date), row.note).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (row.rejectionReason.isNotBlank()) Text(row.rejectionReason, style = MaterialTheme.typography.bodySmall, color = Danger)
                        if (row.status == "PENDING") GhostButton("Cancel claim", Modifier.fillMaxWidth()) { onCancel(row) }
                    }
                }
            }
        }
        GoldButton("Add expense", Modifier.padding(18.dp).fillMaxWidth(), icon = Icons.Rounded.Add, onClick = onNew)
    }
}

@Composable
fun ExpenseForm(saving: Boolean, error: String?, onBack: () -> Unit, onSubmit: (ExpenseBody) -> Unit) {
    var category by remember { mutableStateOf("TRANSPORT") }
    var date by remember { mutableStateOf(todayIso()) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar("Add expense", "Your manager decides", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ChoiceRow(expenseKinds, category) { category = it }
            DateRow("Date", date) { date = it }
            Field("Amount (৳)", amount, { amount = it }, keyboard = KeyboardType.Decimal)
            Field("Note", note, { note = it })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        GoldButton("Send claim", Modifier.navigationBarsPadding().padding(18.dp).fillMaxWidth(), enabled = value != null && value > 0, busy = saving) {
            onSubmit(ExpenseBody(date, category, amount.trim(), note.trim()))
        }
    }
}

@Composable
fun ExpenseRoute(repository: Repository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val data = rememberLoader(repository) { repository.api.expenses().data?.results.orEmpty() }
    var adding by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (adding) {
        ExpenseForm(saving, error, { adding = false; error = null }) { body ->
            saving = true
            scope.launch {
                runCatching { repository.api.addExpense(body) }
                    .onSuccess { adding = false; error = null; data.reload() }
                    .onFailure { error = it.onlineMessage(repository.gson) }
                saving = false
            }
        }
    } else {
        ExpenseList(data.rows, data.error ?: error, onBack, { adding = true }) { row ->
            scope.launch {
                runCatching { repository.api.cancelExpense(row.id) }
                    .onSuccess { data.reload() }
                    .onFailure { error = it.onlineMessage(repository.gson) }
            }
        }
    }
}

// ── Messages ──────────────────────────────────────────────────────────────

@Composable
fun ChatsList(chats: List<ChatRow>?, contacts: List<Contact>, error: String?, onBack: () -> Unit, onOpen: (ChatRow) -> Unit, onStart: (Contact) -> Unit) {
    val talking = chats.orEmpty().mapNotNull { it.otherParticipant?.id }.toSet()
    val others = contacts.filter { it.id !in talking }

    ListScaffold("Messages", "Your manager and admins", onBack) {
        ErrorLine(error)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (chats == null && error == null) item { Waiting() }
            if (chats?.isEmpty() == true && others.isEmpty()) item { EmptyNote("No one to message yet", icon = Icons.Rounded.Forum) }
            items(chats.orEmpty(), key = { it.id }) { chat ->
                SoftCard(Modifier.fillMaxWidth(), onClick = { onOpen(chat) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Forum, Teal, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(chat.lastMessage?.message ?: "No messages yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        chat.lastMessage?.let { Text(clock(it.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (others.isNotEmpty()) item { Text("Start a chat", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium) }
            items(others, key = { "c${it.id}" }) { person ->
                SoftCard(Modifier.fillMaxWidth(), onClick = { onStart(person) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Person, MarigoldDeep, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(person.fullName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatsRoute(repository: Repository, onBack: () -> Unit, onChat: (id: String, name: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val chats = rememberLoader(repository) { repository.api.chats().data?.results.orEmpty() }
    val contacts = rememberLoader(repository) { repository.api.contacts().data?.results.orEmpty() }
    var error by remember { mutableStateOf<String?>(null) }

    ChatsList(
        chats.rows,
        contacts.rows.orEmpty(),
        chats.error ?: error,
        onBack,
        onOpen = { onChat(it.id, it.title) },
        onStart = { person ->
            scope.launch {
                runCatching { repository.api.startChat(person.id).data!! }
                    .onSuccess { onChat(it.id, person.fullName) }
                    .onFailure { error = it.onlineMessage(repository.gson) }
            }
        },
    )
}

/** `messages` come newest first, which is what a bottom-up list wants. */
@Composable
fun ChatThread(name: String, myId: Long, messages: List<ChatMessage>?, error: String?, sending: Boolean, onBack: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar(name, null, onBack)
        ErrorLine(error)
        LazyColumn(Modifier.weight(1f), reverseLayout = true, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages.orEmpty(), key = { it.id }) { row ->
                val mine = row.sender == myId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (mine) Pine else MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(row.message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface)
                        Text(clock(row.createdAt), style = MaterialTheme.typography.labelSmall, color = if (mine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.navigationBarsPadding().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a message") },
                shape = RoundedCornerShape(18.dp),
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onSend(text.trim())
                    text = ""
                },
                enabled = text.isNotBlank() && !sending,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(if (text.isNotBlank()) Marigold else MaterialTheme.colorScheme.surfaceVariant),
            ) { Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = Pine) }
        }
    }
}

@Composable
fun ChatRoute(repository: Repository, conversationId: String, name: String, myId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    suspend fun load() {
        runCatching { repository.api.messages(conversationId).data?.results.orEmpty() }
            .onSuccess { fresh ->
                if (fresh.firstOrNull()?.id != messages?.firstOrNull()?.id) runCatching { repository.api.seen(conversationId) }
                messages = fresh
                error = null
            }
            .onFailure { error = it.onlineMessage(repository.gson) }
    }

    // New messages arrive by checking every few seconds while the chat is open.
    LaunchedEffect(conversationId) {
        while (true) {
            load()
            delay(5000)
        }
    }

    ChatThread(name, myId, messages, error, sending, onBack) { text ->
        sending = true
        scope.launch {
            runCatching { repository.api.send(conversationId, MessageBody(text)) }
                .onSuccess { load() }
                .onFailure { error = it.onlineMessage(repository.gson) }
            sending = false
        }
    }
}
