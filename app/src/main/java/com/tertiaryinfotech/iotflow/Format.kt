package com.tertiaryinfotech.iotflow

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

/** Parse an ISO-8601 timestamp (with or without fractional seconds) to an Instant. */
fun parseInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return try {
        Instant.parse(raw)
    } catch (_: Exception) {
        try {
            // Handle offsets like +08:00 or missing 'Z'
            DateTimeFormatter.ISO_DATE_TIME.parse(raw, Instant::from)
        } catch (_: Exception) {
            null
        }
    }
}

/** Relative time like the iOS `style: .relative` ("5m ago", "in 2h"). */
fun relativeTime(raw: String?, now: Instant = Instant.now()): String {
    val instant = parseInstant(raw) ?: return "—"
    val deltaSec = instant.epochSecond - now.epochSecond
    val past = deltaSec <= 0
    val s = abs(deltaSec)
    val text = when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        s < 86400 -> "${s / 3600}h"
        s < 604800 -> "${s / 86400}d"
        else -> "${s / 604800}w"
    }
    return if (past) "$text ago" else "in $text"
}

/** Absolute date like "Jun 16, 2026". */
fun shortDate(raw: String?): String {
    val instant = parseInstant(raw) ?: return "—"
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
    return fmt.format(instant)
}

/** Format a telemetry value: whole numbers without decimals, else 2 dp. */
fun formatValue(v: Double?): String {
    if (v == null) return "—"
    return if (v == v.roundToLong().toDouble()) v.roundToLong().toString()
    else String.format("%.2f", v)
}
