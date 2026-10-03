package io.github.ieswar23.forkly.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.tracking.OrderTracker
import io.github.ieswar23.forkly.domain.tracking.TrackerConfig
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OrderTrackerTest {

    private val config = TrackerConfig(placedMillis = 12_000, preparingMillis = 45_000, outForDeliveryMillis = 60_000, tickMillis = 1_000)

    private fun TestScope.tracker() = OrderTracker(Clock { testScheduler.currentTime }, config)

    @Test
    fun `snapshot status follows the configured stage boundaries`() {
        val tracker = OrderTracker(Clock { 0 }, config)
        assertThat(tracker.snapshotAt(0).status).isEqualTo(OrderStatus.PLACED)
        assertThat(tracker.snapshotAt(11_999).status).isEqualTo(OrderStatus.PLACED)
        assertThat(tracker.snapshotAt(12_000).status).isEqualTo(OrderStatus.PREPARING)
        assertThat(tracker.snapshotAt(56_999).status).isEqualTo(OrderStatus.PREPARING)
        assertThat(tracker.snapshotAt(57_000).status).isEqualTo(OrderStatus.OUT_FOR_DELIVERY)
        assertThat(tracker.snapshotAt(117_000).status).isEqualTo(OrderStatus.DELIVERED)
        assertThat(tracker.snapshotAt(10_000_000).status).isEqualTo(OrderStatus.DELIVERED)
    }

    @Test
    fun `snapshot exposes remaining time, progress and reached stages`() {
        val tracker = OrderTracker(Clock { 0 }, config)
        val snapshot = tracker.snapshotAt(87_000) // halfway through delivery

        assertThat(snapshot.remainingMillis).isEqualTo(30_000)
        assertThat(snapshot.stageProgress).isWithin(0.001f).of(0.5f)
        assertThat(snapshot.progress).isWithin(0.001f).of(87f / 117f)
        assertThat(snapshot.stageStartedAt.keys)
            .containsExactly(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY)
        assertThat(snapshot.stageStartedAt[OrderStatus.OUT_FOR_DELIVERY]).isEqualTo(57_000)
    }

    @Test
    fun `negative elapsed time is clamped to the start`() {
        val tracker = OrderTracker(Clock { 0 }, config)
        val snapshot = tracker.snapshotAt(-5_000)
        assertThat(snapshot.status).isEqualTo(OrderStatus.PLACED)
        assertThat(snapshot.remainingMillis).isEqualTo(117_000)
    }

    @Test
    fun `status flow advances through every stage on virtual time and completes`() = runTest {
        val tracker = tracker()
        val statuses = tracker.statusChanges(placedAt = 0).toList()

        assertThat(statuses).containsExactly(
            OrderStatus.PLACED,
            OrderStatus.PREPARING,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED,
        ).inOrder()
        assertThat(testScheduler.currentTime).isEqualTo(117_000)
    }

    @Test
    fun `tracker ticks once per second until delivered`() = runTest {
        val snapshots = tracker().track(placedAt = 0).toList()

        assertThat(snapshots).hasSize(118) // t = 0s … 117s
        assertThat(snapshots.first().remainingMillis).isEqualTo(117_000)
        assertThat(snapshots.last().status).isEqualTo(OrderStatus.DELIVERED)
        assertThat(snapshots.last().remainingMillis).isEqualTo(0)
    }

    @Test
    fun `status is observable mid-journey`() = runTest {
        val tracker = tracker()
        val seen = mutableListOf<OrderStatus>()
        val job = launch { tracker.statusChanges(placedAt = 0).collect { seen += it } }

        runCurrent()
        assertThat(seen).containsExactly(OrderStatus.PLACED)

        advanceTimeBy(12_001)
        assertThat(seen.last()).isEqualTo(OrderStatus.PREPARING)

        advanceTimeBy(45_000)
        assertThat(seen.last()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY)
        job.cancel()
    }

    @Test
    fun `tracking resumes from elapsed time after process death`() = runTest {
        advanceTimeBy(100_000)
        val first = tracker().track(placedAt = 0).toList().first()
        assertThat(first.status).isEqualTo(OrderStatus.OUT_FOR_DELIVERY)
        assertThat(first.remainingMillis).isEqualTo(17_000)
    }

    @Test
    fun `already delivered order emits a single delivered snapshot`() = runTest {
        advanceTimeBy(500_000)
        val snapshots = tracker().track(placedAt = 0).toList()
        assertThat(snapshots.map { it.status }).containsExactly(OrderStatus.DELIVERED)
    }

    // ---- Scheduled orders ----

    @Test
    fun `before the slot the order waits as placed with no progress`() {
        val tracker = OrderTracker(Clock { 0 }, config)
        val snapshot = tracker.snapshotAt(-30 * 60_000L) // slot starts in 30 minutes

        assertThat(snapshot.isWaitingForSlot).isTrue()
        assertThat(snapshot.startsInMillis).isEqualTo(30 * 60_000L)
        assertThat(snapshot.status).isEqualTo(OrderStatus.PLACED)
        assertThat(snapshot.progress).isEqualTo(0f)
        assertThat(snapshot.remainingMillis).isEqualTo(117_000)
        assertThat(tracker.snapshotAt(0).isWaitingForSlot).isFalse()
    }

    @Test
    fun `scheduled order sleeps until its slot instead of ticking, then runs the normal timeline`() = runTest {
        val slotStart = 20 * 60_000L
        val snapshots = tracker().track(placedAt = slotStart).toList()

        // One waiting snapshot, then 118 live ticks exactly as for an order placed at the slot.
        assertThat(snapshots).hasSize(1 + 118)
        assertThat(snapshots.first().isWaitingForSlot).isTrue()
        assertThat(snapshots.first().startsInMillis).isEqualTo(slotStart)
        assertThat(snapshots.drop(1).none { it.isWaitingForSlot }).isTrue()
        assertThat(snapshots.last().status).isEqualTo(OrderStatus.DELIVERED)
        assertThat(testScheduler.currentTime).isEqualTo(slotStart + 117_000)
    }

    @Test
    fun `scheduled order status stays placed until the slot begins`() = runTest {
        val slotStart = 60 * 60_000L
        val tracker = tracker()
        val seen = mutableListOf<OrderStatus>()
        val job = launch { tracker.statusChanges(placedAt = slotStart).collect { seen += it } }

        advanceTimeBy(slotStart + 11_000) // 11 s into the slot: the restaurant hasn't started yet
        assertThat(seen).containsExactly(OrderStatus.PLACED)
        assertThat(tracker.statusAt(slotStart)).isEqualTo(OrderStatus.PLACED)

        advanceTimeBy(2_000)
        assertThat(seen.last()).isEqualTo(OrderStatus.PREPARING)
        job.cancel()
    }
}
