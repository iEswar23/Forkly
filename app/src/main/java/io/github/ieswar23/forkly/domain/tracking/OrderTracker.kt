package io.github.ieswar23.forkly.domain.tracking

import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * How long each stage lasts. The default timeline is accelerated (~2 minutes end-to-end) so the
 * whole journey can be watched in a demo; a production build would get these from the backend.
 */
data class TrackerConfig(
    val placedMillis: Long = 12_000,
    val preparingMillis: Long = 45_000,
    val outForDeliveryMillis: Long = 60_000,
    val tickMillis: Long = 1_000,
) {
    val totalMillis: Long get() = placedMillis + preparingMillis + outForDeliveryMillis
}

data class TrackingSnapshot(
    val status: OrderStatus,
    val elapsedMillis: Long,
    val remainingMillis: Long,
    /** Overall journey progress, 0f..1f. */
    val progress: Float,
    /** Progress within the current stage, 0f..1f. */
    val stageProgress: Float,
    /** Elapsed time (from the start of the timeline) at which each reached stage started. */
    val stageStartedAt: Map<OrderStatus, Long>,
    /** For a scheduled order whose slot hasn't arrived yet: time until the timeline starts (0 once live). */
    val startsInMillis: Long = 0,
) {
    val isWaitingForSlot: Boolean get() = startsInMillis > 0
}

/**
 * Drives the live order timeline. State is a pure function of the time elapsed since the timeline
 * started, so tracking survives process death and can be resumed at any point.
 *
 * The timeline starts when the order is placed, or for a scheduled order when its delivery slot
 * begins. Until then the order stays [OrderStatus.PLACED] and snapshots report [TrackingSnapshot.startsInMillis].
 */
class OrderTracker(
    private val clock: Clock,
    private val config: TrackerConfig = TrackerConfig(),
) {
    val totalMillis: Long get() = config.totalMillis

    fun snapshotAt(elapsedMillis: Long): TrackingSnapshot {
        val elapsed = elapsedMillis.coerceIn(0, config.totalMillis)
        val preparingAt = config.placedMillis
        val outAt = preparingAt + config.preparingMillis
        val deliveredAt = config.totalMillis

        val (status, stageStart, stageLength) = when {
            elapsed < preparingAt -> Triple(OrderStatus.PLACED, 0L, config.placedMillis)
            elapsed < outAt -> Triple(OrderStatus.PREPARING, preparingAt, config.preparingMillis)
            elapsed < deliveredAt -> Triple(OrderStatus.OUT_FOR_DELIVERY, outAt, config.outForDeliveryMillis)
            else -> Triple(OrderStatus.DELIVERED, deliveredAt, 0L)
        }
        val starts = buildMap {
            put(OrderStatus.PLACED, 0L)
            if (status >= OrderStatus.PREPARING) put(OrderStatus.PREPARING, preparingAt)
            if (status >= OrderStatus.OUT_FOR_DELIVERY) put(OrderStatus.OUT_FOR_DELIVERY, outAt)
            if (status == OrderStatus.DELIVERED) put(OrderStatus.DELIVERED, deliveredAt)
        }
        return TrackingSnapshot(
            status = status,
            elapsedMillis = elapsed,
            remainingMillis = deliveredAt - elapsed,
            progress = elapsed.toFloat() / deliveredAt,
            stageProgress = if (stageLength == 0L) 1f else (elapsed - stageStart).toFloat() / stageLength,
            stageStartedAt = starts,
            startsInMillis = (-elapsedMillis).coerceAtLeast(0),
        )
    }

    // In the functions below `placedAt` is when the timeline starts: the placement time, or the
    // slot start for a scheduled order (see Order.trackingStartsAt).

    fun statusAt(placedAt: Long): OrderStatus = snapshotAt(clock.now() - placedAt).status

    /**
     * Emits a snapshot every tick until the order is delivered, then completes. Before a scheduled
     * order's slot it emits one waiting snapshot and sleeps until the slot instead of ticking.
     */
    fun track(placedAt: Long): Flow<TrackingSnapshot> = flow {
        while (true) {
            val snapshot = snapshotAt(clock.now() - placedAt)
            emit(snapshot)
            if (snapshot.status == OrderStatus.DELIVERED) break
            delay(if (snapshot.isWaitingForSlot) snapshot.startsInMillis else config.tickMillis)
        }
    }

    /** Only the stage transitions, useful for persisting status changes. */
    fun statusChanges(placedAt: Long): Flow<OrderStatus> = track(placedAt).map { it.status }.distinctUntilChanged()
}
