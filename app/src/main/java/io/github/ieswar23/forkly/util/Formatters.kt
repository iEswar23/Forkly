package io.github.ieswar23.forkly.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

/** "7:30 – 8:00 PM", or "11:30 AM – 12:00 PM" when the window crosses noon or midnight. */
fun formatSlotWindow(startMillis: Long, endMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val clock = SimpleDateFormat("h:mm", Locale.ENGLISH).apply { this.timeZone = timeZone }
    val full = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { this.timeZone = timeZone }
    val marker = SimpleDateFormat("a", Locale.ENGLISH).apply { this.timeZone = timeZone }
    val sameHalf = marker.format(Date(startMillis)) == marker.format(Date(endMillis))
    val start = if (sameHalf) clock.format(Date(startMillis)) else full.format(Date(startMillis))
    return "$start – ${full.format(Date(endMillis))}"
}

/** "Today, 7:30 PM", "Tomorrow, 9:00 AM" or "Sat, 3 Oct, 7:30 PM", relative to [now]. */
fun formatScheduledFor(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val time = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { this.timeZone = timeZone }.format(Date(epochMillis))
    val day = when (daysBetween(now, epochMillis, timeZone)) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        else -> SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).apply { this.timeZone = timeZone }.format(Date(epochMillis))
    }
    return "$day, $time"
}

/** Calendar days from [from] to [to] in [timeZone] (not 24-hour periods). */
private fun daysBetween(from: Long, to: Long, timeZone: TimeZone): Int {
    fun dayNumber(millis: Long): Long {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis / 86_400_000L
    }
    return (dayNumber(to) - dayNumber(from)).toInt()
}
