package io.github.ieswar23.forkly.domain.scheduling

import io.github.ieswar23.forkly.util.Clock
import java.util.Calendar
import java.util.TimeZone

/**
 * Daily opening hours as minutes after local midnight. When [closesAtMinute] is not after
 * [opensAtMinute] the restaurant closes the next day ("5:00 PM – 3:00 AM", "12:00 PM – 12:00 AM").
 */
data class OpeningHours(val opensAtMinute: Int, val closesAtMinute: Int) {

    init {
        require(opensAtMinute in 0 until MINUTES_PER_DAY) { "opensAtMinute out of range: $opensAtMinute" }
        require(closesAtMinute in 0 until MINUTES_PER_DAY) { "closesAtMinute out of range: $closesAtMinute" }
    }

    val closesNextDay: Boolean get() = closesAtMinute <= opensAtMinute

    /** Length of one opening window; equal open and close times mean open around the clock. */
    val openMinutes: Int get() = if (closesNextDay) closesAtMinute + MINUTES_PER_DAY - opensAtMinute else closesAtMinute - opensAtMinute

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60

        /** Used when a restaurant has no (readable) hours: 11:00 AM – 11:00 PM. */
        val DEFAULT = OpeningHours(opensAtMinute = 11 * 60, closesAtMinute = 23 * 60)

        private val TIME = Regex("""(\d{1,2}):(\d{2})\s*([AaPp])\.?\s*[Mm]?\.?""")
        private val TIME_24H = Regex("""\b(\d{1,2}):(\d{2})\b""")

        /**
         * Parses "11:00 AM – 11:30 PM" style text (any dash, optional spaces, also 24-hour "11:00-23:00").
         * Returns null for anything it can't read with confidence.
         */
        fun parse(text: String?): OpeningHours? {
            if (text.isNullOrBlank()) return null
            val twelveHour = TIME.findAll(text).toList()
            val minutes = if (twelveHour.size == 2) {
                twelveHour.map { match ->
                    val hour = match.groupValues[1].toInt()
                    val minute = match.groupValues[2].toInt()
                    if (hour !in 1..12 || minute !in 0..59) return null
                    val pm = match.groupValues[3].equals("p", ignoreCase = true)
                    (hour % 12 + if (pm) 12 else 0) * 60 + minute
                }
            } else {
                val plain = TIME_24H.findAll(text).toList()
                if (twelveHour.isNotEmpty() || plain.size != 2) return null
                plain.map { match ->
                    val hour = match.groupValues[1].toInt()
                    val minute = match.groupValues[2].toInt()
                    if (hour !in 0..23 || minute !in 0..59) return null
                    hour * 60 + minute
                }
            }
            return OpeningHours(minutes[0], minutes[1])
        }

        fun parseOrDefault(text: String?): OpeningHours = parse(text) ?: DEFAULT
    }
}

enum class SlotDay(val label: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
}

/** A delivery window, [startMillis, endMillis). */
data class DeliverySlot(
    val startMillis: Long,
    val endMillis: Long,
    val day: SlotDay,
)

/**
 * Builds the delivery slots a customer can schedule for: 30-minute windows on the half hour for the
 * rest of today and tomorrow that sit fully inside the restaurant's opening hours and start at least
 * [minLeadMinutes] from now.
 *
 * Pure apart from the injected [Clock]: the same clock, time zone and hours always give the same slots.
 */
class DeliverySlotPlanner(
    private val clock: Clock,
    /** Read on every call so a device time-zone change is picked up. */
    private val timeZone: () -> TimeZone = { TimeZone.getDefault() },
    private val slotMinutes: Int = SLOT_MINUTES,
    private val minLeadMinutes: Int = MIN_LEAD_MINUTES,
) {
    init {
        require(slotMinutes > 0 && (24 * 60) % slotMinutes == 0) { "slotMinutes must divide a day evenly" }
        require(minLeadMinutes >= 0) { "minLeadMinutes can't be negative" }
    }

    fun slots(openHoursText: String?): List<DeliverySlot> = slots(OpeningHours.parseOrDefault(openHoursText))

    fun slots(hours: OpeningHours): List<DeliverySlot> {
        val zone = timeZone()
        val now = clock.now()
        val earliestStart = now + minLeadMinutes * MILLIS_PER_MINUTE
        val todayMidnight = midnight(now, zone, dayOffset = 0)
        val dayAfterTomorrowMidnight = midnight(now, zone, dayOffset = 2)
        val tomorrowMidnight = midnight(now, zone, dayOffset = 1)

        val slots = sortedMapOf<Long, DeliverySlot>()
        // Yesterday's window can run past midnight into today (e.g. 5 PM – 3 AM).
        for (windowDay in -1..1) {
            val opensAt = atMinute(now, zone, dayOffset = windowDay, minuteOfDay = hours.opensAtMinute)
            val closesAt = addMinutes(opensAt, zone, hours.openMinutes)
            var start = firstBoundaryAtOrAfter(opensAt, zone)
            while (true) {
                val end = addMinutes(start, zone, slotMinutes)
                if (end > closesAt) break
                if (start >= earliestStart && start >= todayMidnight && start < dayAfterTomorrowMidnight) {
                    val day = if (start < tomorrowMidnight) SlotDay.TODAY else SlotDay.TOMORROW
                    slots[start] = DeliverySlot(start, end, day)
                }
                start = end
            }
        }
        return slots.values.toList()
    }

    /** Whether a previously offered slot can still be booked (it may have gone stale on screen). */
    fun isBookable(startMillis: Long, hours: OpeningHours): Boolean = slots(hours).any { it.startMillis == startMillis }

    private fun calendar(zone: TimeZone, millis: Long): Calendar =
        Calendar.getInstance(zone).apply { timeInMillis = millis }

    private fun midnight(now: Long, zone: TimeZone, dayOffset: Int): Long = atMinute(now, zone, dayOffset, 0)

    private fun atMinute(now: Long, zone: TimeZone, dayOffset: Int, minuteOfDay: Int): Long =
        calendar(zone, now).apply {
            add(Calendar.DAY_OF_MONTH, dayOffset)
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun addMinutes(millis: Long, zone: TimeZone, minutes: Int): Long =
        calendar(zone, millis).apply { add(Calendar.MINUTE, minutes) }.timeInMillis

    /** Rounds up to the next :00 / :30 (for 30-minute slots) local wall-clock boundary. */
    private fun firstBoundaryAtOrAfter(millis: Long, zone: TimeZone): Long {
        val cal = calendar(zone, millis)
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val hasSeconds = cal.get(Calendar.SECOND) > 0 || cal.get(Calendar.MILLISECOND) > 0
        val remainder = minuteOfDay % slotMinutes
        val roundUp = if (remainder == 0 && !hasSeconds) 0 else slotMinutes - remainder
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, roundUp)
        return cal.timeInMillis
    }

    companion object {
        const val SLOT_MINUTES = 30
        const val MIN_LEAD_MINUTES = 45
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
