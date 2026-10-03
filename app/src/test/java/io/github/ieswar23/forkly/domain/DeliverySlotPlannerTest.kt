package io.github.ieswar23.forkly.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.domain.scheduling.DeliverySlotPlanner
import io.github.ieswar23.forkly.domain.scheduling.OpeningHours
import io.github.ieswar23.forkly.domain.scheduling.SlotDay
import io.github.ieswar23.forkly.util.Clock
import io.github.ieswar23.forkly.util.formatScheduledFor
import io.github.ieswar23.forkly.util.formatSlotWindow
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DeliverySlotPlannerTest {

    private val ist: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /** Friday 2 Oct 2026 in Hyderabad, plus [dayOffset] days, at [hour]:[minute]. */
    private fun at(hour: Int, minute: Int, dayOffset: Int = 0, second: Int = 0): Long =
        Calendar.getInstance(ist).apply {
            clear()
            set(2026, Calendar.OCTOBER, 2, hour, minute, second)
            add(Calendar.DAY_OF_MONTH, dayOffset)
        }.timeInMillis

    private fun plannerAt(now: Long) = DeliverySlotPlanner(clock = Clock { now }, timeZone = { ist })

    private val shahiHours = OpeningHours.parse("11:00 AM – 11:30 PM")!!

    // ---- Opening hours ----

    @Test
    fun `parses the restaurant hours used in the app`() {
        assertThat(OpeningHours.parse("11:00 AM – 11:30 PM")).isEqualTo(OpeningHours(11 * 60, 23 * 60 + 30))
        assertThat(OpeningHours.parse("6:30 AM – 10:30 PM")).isEqualTo(OpeningHours(6 * 60 + 30, 22 * 60 + 30))
        assertThat(OpeningHours.parse("12:00 PM – 12:00 AM")).isEqualTo(OpeningHours(12 * 60, 0))
        assertThat(OpeningHours.parse("5:00 PM – 3:00 AM")).isEqualTo(OpeningHours(17 * 60, 3 * 60))
        assertThat(OpeningHours.parse("11:00am-2:00 a.m.")).isEqualTo(OpeningHours(11 * 60, 2 * 60))
        assertThat(OpeningHours.parse("09:00 - 21:30")).isEqualTo(OpeningHours(9 * 60, 21 * 60 + 30))
    }

    @Test
    fun `unreadable or missing hours fall back to 11 AM to 11 PM`() {
        listOf(null, "", "  ", "Open all day", "11:00 AM", "13:00 PM – 11:00 PM", "11:00 AM – 25:00").forEach { text ->
            assertThat(OpeningHours.parse(text)).isNull()
            assertThat(OpeningHours.parseOrDefault(text)).isEqualTo(OpeningHours(11 * 60, 23 * 60))
        }
    }

    @Test
    fun `closing at or before opening means the next day`() {
        assertThat(OpeningHours(17 * 60, 3 * 60).closesNextDay).isTrue()
        assertThat(OpeningHours(17 * 60, 3 * 60).openMinutes).isEqualTo(10 * 60)
        assertThat(OpeningHours(0, 0).openMinutes).isEqualTo(24 * 60) // open around the clock
        assertThat(shahiHours.closesNextDay).isFalse()
    }

    // ---- Slots ----

    @Test
    fun `first slot is the next half hour at least 45 minutes away`() {
        assertThat(plannerAt(at(18, 10)).slots(shahiHours).first().startMillis).isEqualTo(at(19, 0))
        assertThat(plannerAt(at(17, 45)).slots(shahiHours).first().startMillis).isEqualTo(at(18, 30))
        assertThat(plannerAt(at(18, 0)).slots(shahiHours).first().startMillis).isEqualTo(at(19, 0))
    }

    @Test
    fun `a slot exactly 45 minutes away is still bookable but not a second less`() {
        assertThat(plannerAt(at(17, 45)).slots(shahiHours).first().startMillis).isEqualTo(at(18, 30))
        assertThat(plannerAt(at(17, 45, second = 1)).slots(shahiHours).first().startMillis).isEqualTo(at(19, 0))
    }

    @Test
    fun `slots are 30 minutes long and fit inside opening hours`() {
        val slots = plannerAt(at(18, 10)).slots(shahiHours)

        assertThat(slots.all { it.endMillis - it.startMillis == 30 * 60_000L }).isTrue()
        val today = slots.filter { it.day == SlotDay.TODAY }
        assertThat(today.first().startMillis).isEqualTo(at(19, 0))
        assertThat(today.last().startMillis).isEqualTo(at(23, 0)) // ends 11:30 PM, closing time
        assertThat(today).hasSize(9)
    }

    @Test
    fun `tomorrow offers the full day from opening to closing`() {
        val tomorrow = plannerAt(at(18, 10)).slots(shahiHours).filter { it.day == SlotDay.TOMORROW }

        assertThat(tomorrow.first().startMillis).isEqualTo(at(11, 0, dayOffset = 1))
        assertThat(tomorrow.last().endMillis).isEqualTo(at(23, 30, dayOffset = 1))
        assertThat(tomorrow).hasSize(25)
    }

    @Test
    fun `slots are sorted, unique and only cover today and tomorrow`() {
        val slots = plannerAt(at(9, 0)).slots(OpeningHours.parse("12:00 AM – 12:00 AM")!!)
        val starts = slots.map { it.startMillis }

        assertThat(starts).isInStrictOrder()
        assertThat(starts.first()).isEqualTo(at(10, 0))
        assertThat(slots.last().startMillis).isEqualTo(at(23, 30, dayOffset = 1))
        assertThat(slots.count { it.day == SlotDay.TOMORROW }).isEqualTo(48)
    }

    @Test
    fun `before opening, today's slots start when the restaurant opens`() {
        val today = plannerAt(at(8, 0)).slots(shahiHours).filter { it.day == SlotDay.TODAY }
        assertThat(today.first().startMillis).isEqualTo(at(11, 0))
        assertThat(today).hasSize(25)
    }

    @Test
    fun `after closing only tomorrow is offered`() {
        val slots = plannerAt(at(23, 5)).slots(null) // no hours: 11 AM – 11 PM

        assertThat(slots.map { it.day }.toSet()).containsExactly(SlotDay.TOMORROW)
        assertThat(slots.first().startMillis).isEqualTo(at(11, 0, dayOffset = 1))
        assertThat(slots.last().startMillis).isEqualTo(at(22, 30, dayOffset = 1))
        assertThat(slots).hasSize(24)
    }

    @Test
    fun `late-night hours carry over past midnight`() {
        val lateNight = OpeningHours.parse("5:00 PM – 3:00 AM")!!
        // 1 AM: still inside last night's window, which runs until 3 AM.
        val slots = plannerAt(at(1, 0)).slots(lateNight)
        val today = slots.filter { it.day == SlotDay.TODAY }.map { it.startMillis }
        val tomorrow = slots.filter { it.day == SlotDay.TOMORROW }.map { it.startMillis }

        assertThat(today.take(2)).containsExactly(at(2, 0), at(2, 30)).inOrder()
        assertThat(today[2]).isEqualTo(at(17, 0))
        assertThat(today.last()).isEqualTo(at(23, 30))
        // Tomorrow's early hours come from tonight's window.
        assertThat(tomorrow.first()).isEqualTo(at(0, 0, dayOffset = 1))
        assertThat(tomorrow).contains(at(2, 30, dayOffset = 1))
        assertThat(tomorrow).doesNotContain(at(3, 0, dayOffset = 1))
        assertThat(tomorrow.last()).isEqualTo(at(23, 30, dayOffset = 1))
    }

    @Test
    fun `odd opening minutes round up to the next slot boundary`() {
        val slots = plannerAt(at(6, 0)).slots(OpeningHours.parse("7:10 AM – 9:00 AM")!!)
        assertThat(slots.filter { it.day == SlotDay.TODAY }.map { it.startMillis })
            .containsExactly(at(7, 30), at(8, 0), at(8, 30)).inOrder()
    }

    @Test
    fun `slot time follows the injected clock`() {
        var now = at(18, 10)
        val planner = DeliverySlotPlanner(clock = { now }, timeZone = { ist })
        assertThat(planner.isBookable(at(19, 0), shahiHours)).isTrue()

        now = at(18, 20) // 19:00 is now only 40 minutes away
        assertThat(planner.isBookable(at(19, 0), shahiHours)).isFalse()
        assertThat(planner.isBookable(at(19, 30), shahiHours)).isTrue()
        assertThat(planner.isBookable(at(19, 15), shahiHours)).isFalse() // not a slot boundary
    }

    // ---- Formatting ----

    @Test
    fun `scheduled times read naturally`() {
        val now = at(18, 10)
        assertThat(formatScheduledFor(at(19, 30), now, ist)).isEqualTo("Today, 7:30 PM")
        assertThat(formatScheduledFor(at(11, 0, dayOffset = 1), now, ist)).isEqualTo("Tomorrow, 11:00 AM")
        assertThat(formatScheduledFor(at(11, 0, dayOffset = 3), now, ist)).isEqualTo("Mon, 5 Oct, 11:00 AM")
        assertThat(formatSlotWindow(at(19, 30), at(20, 0), ist)).isEqualTo("7:30 – 8:00 PM")
        assertThat(formatSlotWindow(at(11, 30), at(12, 0), ist)).isEqualTo("11:30 AM – 12:00 PM")
    }
}
