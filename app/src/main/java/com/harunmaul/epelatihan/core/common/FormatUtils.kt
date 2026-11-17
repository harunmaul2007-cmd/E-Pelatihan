package com.harunmaul.epelatihan.core.common

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val INDONESIA_LOCALE = Locale.forLanguageTag("id-ID")

fun Long.toRupiahString(): String {
    val formatter = NumberFormat.getNumberInstance(INDONESIA_LOCALE)
    return "Rp ${formatter.format(this)}"
}

fun Instant.formatDate(zoneId: String = "Asia/Jakarta"): String {
    val zone = resolveZoneId(zoneId)
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", INDONESIA_LOCALE)
    return formatter.format(this.atZone(zone))
}

fun Instant.formatDateShort(zoneId: String = "Asia/Jakarta"): String {
    val zone = resolveZoneId(zoneId)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", INDONESIA_LOCALE)
    return formatter.format(this.atZone(zone))
}

fun Instant.formatTime(zoneId: String = "Asia/Jakarta"): String {
    val zone = resolveZoneId(zoneId)
    val formatter = DateTimeFormatter.ofPattern("HH:mm", INDONESIA_LOCALE)
    return formatter.format(this.atZone(zone))
}

fun Instant.formatDateTime(zoneId: String = "Asia/Jakarta"): String {
    val zone = resolveZoneId(zoneId)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", INDONESIA_LOCALE)
    return formatter.format(this.atZone(zone))
}

fun formatSchedule(startAt: Instant, endAt: Instant, zoneId: String = "Asia/Jakarta"): String {
    val zone = resolveZoneId(zoneId)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", INDONESIA_LOCALE)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", INDONESIA_LOCALE)

    val startDate = dateFormatter.format(startAt.atZone(zone))
    val startTime = timeFormatter.format(startAt.atZone(zone))
    val endTime = timeFormatter.format(endAt.atZone(zone))
    val zoneAbbr = getZoneAbbreviation(zoneId)

    return "$startDate | $startTime – $endTime $zoneAbbr"
}

private fun resolveZoneId(zoneId: String): ZoneId {
    return try {
        ZoneId.of(zoneId)
    } catch (_: Exception) {
        ZoneId.of("Asia/Jakarta")
    }
}

private fun getZoneAbbreviation(zoneId: String): String {
    return when (zoneId) {
        "Asia/Jakarta", "Asia/Pontianak" -> "WIB"
        "Asia/Makassar", "Asia/Denpasar" -> "WITA"
        "Asia/Jayapura" -> "WIT"
        else -> zoneId
    }
}
