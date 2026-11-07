package com.routara.field.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.routara.field.R

val Pine = Color(0xFF0B3B3C)
val PineDeep = Color(0xFF062829)
val PineBright = Color(0xFF12706B)
val Teal = Color(0xFF0B7A75)
val Mint = Color(0xFF3CC4B8)
val Marigold = Color(0xFFF2A93B)
val MarigoldDeep = Color(0xFFE58A14)
val MarigoldInk = Color(0xFF3D2800)
val Paper = Color(0xFFF6F3EC)
val Ink = Color(0xFF142624)
val InkSoft = Color(0xFF4C5F5B)
val Success = Color(0xFF17845A)
val Danger = Color(0xFFC2384F)

@OptIn(ExperimentalTextApi::class)
private fun variable(resource: Int, vararg weights: Int) = FontFamily(
    weights.map { weight ->
        Font(
            resource,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

val Body = variable(R.font.figtree, 400, 500, 600, 700, 800)
val Display = variable(R.font.bricolage, 500, 600, 700, 800)

private val type = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
)

private val light = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Marigold,
    onSecondary = MarigoldInk,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEADF),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFE2DCCD),
    error = Danger,
)

private val dark = darkColorScheme(
    primary = Mint,
    onPrimary = PineDeep,
    secondary = Marigold,
    onSecondary = MarigoldInk,
    background = Color(0xFF0C1716),
    onBackground = Color(0xFFEDF3F1),
    surface = Color(0xFF142321),
    onSurface = Color(0xFFEDF3F1),
    surfaceVariant = Color(0xFF1C302D),
    onSurfaceVariant = Color(0xFFAEBDB9),
    outline = Color(0xFF28403D),
    error = Color(0xFFF0778B),
)

/** The deep brand greens are too dim on a dark background, so they lighten there. */
@Composable
fun Color.readable(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when {
        !dark -> this
        this == Teal -> Mint
        this == Success -> Color(0xFF4CC791)
        else -> this
    }
}

@Composable
fun RoutaraTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) com.routara.field.ui.theme.dark else light
    MaterialTheme(colorScheme = colors, typography = type) {
        // Text placed straight on the background follows the theme too.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
