package io.github.ieswar23.forkly.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ₹249, ₹1,249 or ₹12.45 — paise are only shown when non-zero. */
fun formatRupees(paise: Long): String {
    val negative = paise < 0
    val abs = kotlin.math.abs(paise)
    val rupees = abs / 100
    val fraction = abs % 100
    val grouped = groupIndian(rupees)
    val body = if (fraction == 0L) grouped else "$grouped.${fraction.toString().padStart(2, '0')}"
    return (if (negative) "-₹" else "₹") + body
}

/** Indian digit grouping: 1,23,456. */
private fun groupIndian(value: Long): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$restGrouped,$last3"
}

fun formatRating(rating: Double): String = String.format(Locale.US, "%.1f", rating)

fun formatCount(count: Int): String = when {
    count >= 1000 -> String.format(Locale.US, "%.1fK+", count / 1000.0).replace(".0K", "K")
    else -> "$count+"
}

fun formatDistance(km: Double): String = String.format(Locale.US, "%.1f km", km)

/** 01:42 style countdown. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

fun formatOrderDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(epochMillis))

fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(epochMillis))
