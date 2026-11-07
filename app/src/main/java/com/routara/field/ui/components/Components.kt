package com.routara.field.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.routara.field.ui.theme.Marigold
import com.routara.field.ui.theme.MarigoldDeep
import com.routara.field.ui.theme.MarigoldInk
import com.routara.field.ui.theme.Mint
import com.routara.field.ui.theme.Pine
import com.routara.field.ui.theme.PineBright
import com.routara.field.ui.theme.PineDeep
import com.routara.field.ui.theme.readable

val HeroGradient = Brush.linearGradient(listOf(PineDeep, Pine, PineBright))
val GoldGradient = Brush.horizontalGradient(listOf(Marigold, MarigoldDeep))

/** The brand backdrop: a faint dotted route, kept quiet so text reads over it. */
@Composable
fun RouteBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(HeroGradient)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val route = Path().apply {
                moveTo(-20f, h * 0.86f)
                cubicTo(w * 0.28f, h * 0.98f, w * 0.34f, h * 0.42f, w * 0.60f, h * 0.56f)
                cubicTo(w * 0.86f, h * 0.70f, w * 0.80f, h * 0.10f, w + 20f, h * 0.06f)
            }
            drawPath(
                route,
                Marigold.copy(alpha = 0.32f),
                style = Stroke(width = 5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 20f))),
            )
            drawCircle(Mint.copy(alpha = 0.08f), w * 0.34f, Offset(w * 0.98f, h * 0.02f))
        }
        content()
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 3.dp,
    ) { content() }
}

/** Target progress: a ring that sweeps up to its value when it appears. */
@Composable
fun ProgressRing(
    percent: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp,
    stroke: Dp = 13.dp,
    track: Color = Color.White.copy(alpha = 0.14f),
    center: @Composable BoxScope.() -> Unit,
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        sweep.animateTo(percent.coerceIn(0f, 100f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            drawArc(track, -90f, 360f, false, style = Stroke(width, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(Marigold, MarigoldDeep, Marigold)),
                -90f,
                360f * sweep.value / 100f,
                false,
                style = Stroke(width, cap = StrokeCap.Round),
            )
        }
        center()
    }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, busy: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    val active = enabled && !busy
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) GoldGradient else Brush.horizontalGradient(listOf(Color(0xFFD9D3C4), Color(0xFFD9D3C4))))
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MarigoldInk, strokeWidth = 3.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon?.let { Icon(it, null, tint = MarigoldInk, modifier = Modifier.size(22.dp)) }
                Text(text, style = MaterialTheme.typography.labelLarge, color = MarigoldInk)
            }
        }
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, tint: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val color = tint.readable()
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(13.dp), tint = color)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val color = tint.readable()
    Box(
        modifier.size(size).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun ScreenBar(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    RouteBackdrop(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Row(
            Modifier.statusBarsPadding().padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1) }
            }
            actions()
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, hint: String? = null, secure: Boolean = false, keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (secure) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (onDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun EmptyNote(title: String, hint: String? = null, icon: ImageVector? = null) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        icon?.let {
            IconBadge(it, MaterialTheme.colorScheme.primary, size = 64.dp)
            Spacer(Modifier.height(14.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        hint?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
