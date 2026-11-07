package com.routara.field.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.routara.field.data.Named
import com.routara.field.data.afterDiscount
import com.routara.field.data.Outlet
import com.routara.field.data.Product
import com.routara.field.ui.components.EmptyNote
import com.routara.field.ui.components.Field
import com.routara.field.ui.components.GhostButton
import com.routara.field.ui.components.GoldButton
import com.routara.field.ui.components.IconBadge
import com.routara.field.ui.components.RouteBackdrop
import com.routara.field.ui.components.ScreenBar
import com.routara.field.ui.components.SoftCard
import com.routara.field.ui.theme.Danger
import com.routara.field.ui.theme.Marigold
import com.routara.field.ui.theme.MarigoldDeep
import com.routara.field.ui.theme.MarigoldInk
import com.routara.field.ui.theme.Mint
import com.routara.field.ui.theme.Pine
import com.routara.field.ui.theme.Success
import com.routara.field.ui.theme.Teal
import com.routara.field.ui.theme.readable
import java.io.File

val VisitSteps = listOf("Arrive", "Photo", "Order", "Review")

/** The visit's progress, drawn as a short route of waypoints. */
@Composable
fun StepRoute(current: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
        VisitSteps.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(
                        when {
                            index < current -> Success
                            index == current -> Marigold
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < current) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = if (index == current) MarigoldInk else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = if (index == current) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (index < VisitSteps.lastIndex) {
                Box(Modifier.weight(1f).padding(top = 12.dp).height(3.dp).background(if (index < current) Success else MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Composable
private fun Pulse(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val wave by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    Canvas(Modifier.size(210.dp)) {
        val radius = size.minDimension / 2
        drawCircle(color.copy(alpha = 0.10f), radius)
        drawCircle(color.copy(alpha = 0.16f), radius * 0.68f)
        drawCircle(color.copy(alpha = (1f - wave) * 0.7f), radius * wave, style = Stroke(5f))
    }
}

data class ArriveState(val outlet: Outlet, val distance: Double?, val radius: Int, val locating: Boolean, val hasOutletLocation: Boolean) {
    val allowed get() = hasOutletLocation && distance != null && distance <= radius
}

@Composable
fun ArriveScreen(state: ArriveState, onBack: () -> Unit, onRetry: () -> Unit, onContinue: () -> Unit, onDirections: () -> Unit) {
    val color = if (state.allowed) Success else if (state.locating) MarigoldDeep else Danger

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        ScreenBar(state.outlet.name, state.outlet.code, onBack)
        StepRoute(0)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Box(contentAlignment = Alignment.Center) {
                Pulse(color)
                Box(Modifier.size(86.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    Icon(if (state.locating) Icons.Rounded.GpsFixed else if (state.allowed) Icons.Rounded.Storefront else Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    state.locating -> "Finding you…"
                    !state.hasOutletLocation -> "Store has no location yet"
                    state.distance == null -> "Can't get your location"
                    state.allowed -> "You've arrived"
                    else -> "You are ${distanceMetres(state.distance)} from the store"
                },
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    state.locating -> "Checking how far you are from this outlet."
                    !state.hasOutletLocation -> "Stand at the shop and tag its location using Edit on your route."
                    state.distance == null -> "Check that location is on, then try again."
                    state.allowed -> "${distanceLabel(state.distance)} from the shop."
                    else -> "Go within ${state.radius} m of the store to take an order."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(state.outlet.ownerName.ifBlank { state.outlet.channelName }, style = MaterialTheme.typography.titleMedium)
                    if (state.outlet.address.isNotBlank()) Text(state.outlet.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.outlet.addressBn.isNotBlank()) Text(state.outlet.addressBn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.allowed) {
                GoldButton("Continue", Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward, onClick = onContinue)
            } else {
                if (state.hasOutletLocation) GoldButton("Check again", Modifier.fillMaxWidth(), busy = state.locating, icon = Icons.Rounded.GpsOff, onClick = onRetry)
                if (state.hasOutletLocation) GhostButton("Get directions", Modifier.fillMaxWidth(), onClick = onDirections)
            }
        }
    }
}

@Composable
fun PhotoScreen(outlet: Outlet, photo: String?, required: Boolean, onBack: () -> Unit, onTake: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        ScreenBar(outlet.name, if (required) "Shop front photo" else "Shop front photo (optional)", onBack)
        StepRoute(1)
        Column(Modifier.weight(1f).padding(horizontal = 22.dp)) {
            SoftCard(Modifier.fillMaxWidth().aspectRatio(0.95f), onClick = onTake) {
                if (photo != null) {
                    AsyncImage(File(photo), "Photo of ${outlet.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        IconBadge(Icons.Rounded.CameraAlt, Teal, size = 72.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Take a photo of the shop", style = MaterialTheme.typography.titleLarge)
                        Text("It proves the visit and helps your manager.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (photo != null) GhostButton("Retake photo", Modifier.fillMaxWidth(), Icons.Rounded.CameraAlt, onTake)
            if (photo == null && !required) GhostButton("Skip photo", Modifier.fillMaxWidth(), onClick = onContinue)
            GoldButton(if (photo != null) "Continue" else "Take photo", Modifier.fillMaxWidth(), icon = if (photo != null) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.CameraAlt, onClick = if (photo != null) onContinue else onTake)
        }
    }
}

@Composable
private fun PriceTag(label: String, value: String, tint: Color, modifier: Modifier) {
    val color = tint.readable()
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Stepper(quantity: Int, onChange: (Int) -> Unit) {
    if (quantity == 0) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Marigold).clickable { onChange(1) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Add", tint = MarigoldInk) }
        return
    }
    Row(Modifier.clip(CircleShape).background(Pine), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable { onChange(quantity - 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Remove, "Remove one", tint = Color.White)
        }
        Text("$quantity", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
        Box(Modifier.size(44.dp).clickable { onChange(quantity + 1) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "Add one", tint = Marigold)
        }
    }
}

@Composable
fun ProductCard(product: Product, quantity: Int?, onChange: ((Int) -> Unit)?) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${product.sku} · ${product.unitsPerCase} ${product.unitName} per case", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (quantity != null && onChange != null) {
                    Spacer(Modifier.width(8.dp))
                    Stepper(quantity, onChange)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriceTag("MRP", taka(product.retail, exact = true), Teal, Modifier.weight(1f))
                PriceTag("Trade", taka(product.trade, exact = true), Color(0xFF6C5B9E), Modifier.weight(1f))
                PriceTag("Margin", taka(product.margin, exact = true), MarigoldDeep, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryChips(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf<String?>(null) + categories).forEach { category ->
            val active = selected == category
            Box(
                Modifier.clip(CircleShape)
                    .background(if (active) Pine else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (active) Pine else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(category ?: "All", style = MaterialTheme.typography.labelMedium, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun OrderScreen(
    outlet: Outlet,
    products: List<Product>,
    cart: Map<Long, Int>,
    readySale: Boolean,
    onBack: () -> Unit,
    onQuantity: (Product, Int) -> Unit,
    onNoOrder: () -> Unit,
    onReview: () -> Unit,
) {
    var term by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val shown = products.filter {
        (category == null || it.category == category) &&
            (term.isBlank() || it.name.contains(term.trim(), true) || it.sku.contains(term.trim(), true))
    }
    val total = products.sumOf { it.trade * (cart[it.id] ?: 0) }
    val lines = cart.values.count { it > 0 }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ScreenBar(outlet.name, if (readySale) "Ready sale" else "New order", onBack)
        StepRoute(2)
        OutlinedTextField(
            value = term,
            onValueChange = { term = it },
            modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
            placeholder = { Text("Search product or SKU") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(10.dp))
        CategoryChips(products.map { it.category }.distinct(), category) { category = it }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) item { EmptyNote("No products match", icon = Icons.Rounded.ShoppingBag) }
            items(shown, key = { it.id }) { product ->
                ProductCard(product, cart[product.id] ?: 0) { onQuantity(product, it) }
            }
        }
        RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))) {
            Row(Modifier.navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(taka(total), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("$lines ${if (lines == 1) "product" else "products"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                }
                if (lines == 0 && !readySale) {
                    GoldButton("No order", Modifier.width(150.dp), onClick = onNoOrder)
                } else {
                    GoldButton("Review", Modifier.width(150.dp), enabled = lines > 0, icon = Icons.AutoMirrored.Rounded.ArrowForward, onClick = onReview)
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(
    outlet: Outlet,
    cart: Map<Product, Int>,
    saving: Boolean,
    maxDiscount: Double,
    onBack: () -> Unit,
    onSubmit: (discountPercent: Double) -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    val percent = entered.toDoubleOrNull() ?: 0.0
    val invalid = (entered.isNotBlank() && entered.toDoubleOrNull() == null) || percent < 0 || percent > maxDiscount
    val gross = cart.entries.sumOf { (product, quantity) -> product.trade * quantity }
    val total = afterDiscount(gross, percent)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        ScreenBar(outlet.name, "Check before you submit", onBack)
        StepRoute(3)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cart.entries.toList(), key = { it.key.id }) { (product, quantity) ->
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$quantity × ${taka(product.trade, exact = true)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(taka(product.trade * quantity, exact = true), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        SoftCard(Modifier.padding(18.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (maxDiscount > 0) {
                    Field(
                        "Discount % (optional)",
                        entered,
                        { entered = it.take(6) },
                        hint = if (invalid) "Enter 0 to ${percentLabel(maxDiscount)}%" else "Up to ${percentLabel(maxDiscount)}%",
                        keyboard = KeyboardType.Decimal,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (percent > 0 && !invalid) {
                        AmountRow("Subtotal", taka(gross, exact = true))
                        AmountRow("Discount (${percentLabel(percent)}%)", "− " + taka(gross - total, exact = true))
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Row {
                    Text("Order total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(taka(total, exact = true), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(12.dp))
                GoldButton("Submit order", Modifier.fillMaxWidth(), enabled = !invalid, busy = saving, icon = Icons.Rounded.Check) { onSubmit(percent) }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun percentLabel(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

@Composable
fun NoOrderScreen(outlet: Outlet, reasons: List<Named>, saving: Boolean, onBack: () -> Unit, onSubmit: (Long, String) -> Unit) {
    var reason by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        ScreenBar(outlet.name, "No order today", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Why no order?", style = MaterialTheme.typography.headlineMedium)
            if (reasons.isEmpty()) EmptyNote("No reasons set up yet", "Sync, or ask your admin to add them.")
            reasons.forEach { item ->
                SoftCard(Modifier.fillMaxWidth(), onClick = { reason = item.id }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (reason == item.id) Icon(Icons.Rounded.Check, "Selected", tint = Success)
                    }
                }
            }
            OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, shape = RoundedCornerShape(16.dp))
        }
        GoldButton("Save visit", Modifier.padding(18.dp).fillMaxWidth(), enabled = reason != null, busy = saving) { onSubmit(reason!!, note) }
    }
}

@Composable
fun VisitDoneScreen(outlet: Outlet, total: Double?, nextStop: Outlet?, onNext: (Outlet) -> Unit, onHome: () -> Unit) {
    RouteBackdrop(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center) {
                Pulse(Mint)
                Box(Modifier.size(92.dp).clip(CircleShape).background(Marigold), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Pine, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(if (total != null) "Order saved" else "Visit saved", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(outlet.name, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
            total?.let {
                Spacer(Modifier.height(10.dp))
                Text(taka(it), style = MaterialTheme.typography.displayLarge, color = Marigold)
            }
            Spacer(Modifier.height(36.dp))
            nextStop?.let {
                GoldButton("Next: ${it.name}", Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward) { onNext(it) }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                "Back to home",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onHome).padding(14.dp),
            )
        }
    }
}
