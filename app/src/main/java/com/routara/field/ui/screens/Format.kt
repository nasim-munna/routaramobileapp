package com.routara.field.ui.screens

import com.routara.field.data.DHAKA
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val whole = DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.US))
private val cents = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.US))

fun taka(value: Double, exact: Boolean = false) = "৳ " + (if (exact) cents else whole).format(value)
fun taka(value: String?) = taka(value?.toDoubleOrNull() ?: 0.0)

fun distanceLabel(meters: Double) = if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(Locale.US, meters / 1000)

private val metres = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

fun distanceMetres(meters: Double) = "${metres.format(meters)} m"

fun greeting(): String {
    val hour = ZonedDateTime.now(DHAKA).hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun todayLabel(): String = ZonedDateTime.now(DHAKA).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US))

fun clock(iso: String): String = runCatching {
    Instant.parse(iso).atZone(DHAKA).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
}.getOrElse {
    runCatching {
        ZonedDateTime.parse(iso).withZoneSameInstant(DHAKA).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }.getOrDefault("")
}
