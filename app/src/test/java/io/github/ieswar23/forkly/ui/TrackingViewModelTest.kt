package io.github.ieswar23.forkly.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.MainDispatcherRule
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderItem
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.model.Rider
import io.github.ieswar23.forkly.domain.tracking.OrderTracker
import io.github.ieswar23.forkly.domain.tracking.TrackerConfig
import io.github.ieswar23.forkly.fakes.FakeOrderRepository
import io.github.ieswar23.forkly.ui.tracking.TrackingViewModel
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TrackingViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val config = TrackerConfig(placedMillis = 12_000, preparingMillis = 45_000, outForDeliveryMillis = 60_000, tickMillis = 1_000)

    private fun TestScope.createViewModel(order: Order): TrackingViewModel {
        val viewModel = TrackingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackingViewModel.ARG_ORDER_ID to order.id)),
            orderRepository = FakeOrderRepository(listOf(order)),
            tracker = OrderTracker(Clock { testScheduler.currentTime }, config),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    @Test
    fun `an order placed now is tracked from its placement`() = runTest {
        val vm = createViewModel(order(placedAt = 0, scheduledFor = null))
        advanceTimeBy(20_000)
        runCurrent()

        val snapshot = vm.uiState.value.snapshot!!
        assertThat(snapshot.isWaitingForSlot).isFalse()
        assertThat(snapshot.status).isEqualTo(OrderStatus.PREPARING)
    }

    @Test
    fun `a scheduled order waits for its slot before the live timeline starts`() = runTest {
        val slot = 30 * 60_000L
        val vm = createViewModel(order(placedAt = 0, scheduledFor = slot))
        runCurrent()

        val waiting = vm.uiState.value
        assertThat(waiting.order!!.isScheduled).isTrue()
        assertThat(waiting.snapshot!!.isWaitingForSlot).isTrue()
        assertThat(waiting.snapshot!!.startsInMillis).isEqualTo(slot)
        assertThat(waiting.snapshot!!.status).isEqualTo(OrderStatus.PLACED)

        advanceTimeBy(slot + 20_000)
        runCurrent()

        val live = vm.uiState.value.snapshot!!
        assertThat(live.isWaitingForSlot).isFalse()
        assertThat(live.status).isEqualTo(OrderStatus.PREPARING)
        assertThat(live.elapsedMillis).isEqualTo(20_000)
    }

    private fun order(placedAt: Long, scheduledFor: Long?) = Order(
        id = "FK1", restaurantId = "shahi", restaurantName = "Shahi Dastarkhwan", restaurantEmoji = "🍛",
        restaurantArea = "Banjara Hills",
        items = listOf(OrderItem("shahi-01", "Biryani", "🍛", false, 1, 329_00, "", emptyList())),
        itemTotalPaise = 329_00, packagingFeePaise = 0, deliveryFeePaise = 0, discountPaise = 0, gstPaise = 0,
        tipPaise = 0, totalPaise = 329_00, couponCode = null, paymentMethod = PaymentMethod.UPI, addressLabel = "Home",
        addressLine = "", deliveryInstructions = "", status = OrderStatus.PLACED, placedAt = placedAt, deliveredAt = null,
        rider = Rider("Ravi", "", "", 4.8, 100), userRating = null, scheduledFor = scheduledFor,
    )
}
