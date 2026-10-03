package io.github.ieswar23.forkly.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.MainDispatcherRule
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.domain.scheduling.DeliverySlotPlanner
import io.github.ieswar23.forkly.domain.scheduling.SlotDay
import io.github.ieswar23.forkly.fakes.FakeAddressRepository
import io.github.ieswar23.forkly.fakes.FakeCartRepository
import io.github.ieswar23.forkly.fakes.FakeOrderRepository
import io.github.ieswar23.forkly.fakes.TestData
import io.github.ieswar23.forkly.ui.checkout.CheckoutEvent
import io.github.ieswar23.forkly.ui.checkout.CheckoutViewModel
import io.github.ieswar23.forkly.ui.checkout.DeliveryTiming
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CheckoutViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val ist: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val shahi = TestData.restaurant("shahi", name = "Shahi Dastarkhwan") // 11:00 AM – 11:00 PM

    private fun at(hour: Int, minute: Int, dayOffset: Int = 0): Long =
        Calendar.getInstance(ist).apply {
            clear()
            set(2026, Calendar.OCTOBER, 2, hour, minute)
            add(Calendar.DAY_OF_MONTH, dayOffset)
        }.timeInMillis

    private var now = at(18, 10)
    private val cartRepository = FakeCartRepository(mapOf("shahi" to shahi))
    private val orderRepository = FakeOrderRepository()

    private suspend fun TestScope.createViewModel(
        withCart: Boolean = true,
    ): Pair<CheckoutViewModel, MutableList<CheckoutEvent>> {
        if (withCart) cartRepository.add(TestData.line("shahi-01", 329_00, quantity = 2, restaurantId = "shahi"))
        val viewModel = CheckoutViewModel(
            cartRepository = cartRepository,
            addressRepository = FakeAddressRepository(),
            orderRepository = orderRepository,
            pricing = PricingCalculator(),
            slotPlanner = DeliverySlotPlanner(clock = Clock { now }, timeZone = { ist }),
        )
        val events = mutableListOf<CheckoutEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel to events
    }

    @Test
    fun `delivers now by default and offers slots within the restaurant's hours`() = runTest {
        val (vm, _) = createViewModel()
        val state = vm.uiState.value

        assertThat(state.form.deliveryTiming).isEqualTo(DeliveryTiming.NOW)
        assertThat(state.selectedSlot).isNull()
        assertThat(state.canSchedule).isTrue()
        assertThat(state.slots.first().startMillis).isEqualTo(at(19, 0))
        assertThat(state.slots.last().startMillis).isEqualTo(at(22, 30, dayOffset = 1))
        assertThat(state.slots.map { it.day }.toSet()).containsExactly(SlotDay.TODAY, SlotDay.TOMORROW)
        assertThat(state.canPlaceOrder).isTrue()
    }

    @Test
    fun `choosing schedule preselects the earliest slot`() = runTest {
        val (vm, _) = createViewModel()

        vm.schedule()

        val state = vm.uiState.value
        assertThat(state.form.deliveryTiming).isEqualTo(DeliveryTiming.SCHEDULED)
        assertThat(state.selectedSlot?.startMillis).isEqualTo(at(19, 0))
        assertThat(state.canPlaceOrder).isTrue()
    }

    @Test
    fun `a picked slot is kept when switching to deliver now and back`() = runTest {
        val (vm, _) = createViewModel()
        vm.selectSlot(at(12, 30, dayOffset = 1))

        vm.deliverNow()
        assertThat(vm.uiState.value.selectedSlot).isNull()

        vm.schedule()
        assertThat(vm.uiState.value.selectedSlot?.startMillis).isEqualTo(at(12, 30, dayOffset = 1))
        assertThat(vm.uiState.value.selectedSlot?.day).isEqualTo(SlotDay.TOMORROW)
    }

    @Test
    fun `scheduled order is placed with the slot start`() = runTest {
        val (vm, events) = createViewModel()
        vm.selectPayment(PaymentMethod.CASH)
        vm.selectSlot(at(20, 30))

        vm.placeOrder()
        advanceUntilIdle()

        assertThat(orderRepository.placed.single().scheduledFor).isEqualTo(at(20, 30))
        assertThat(events.single()).isInstanceOf(CheckoutEvent.OrderPlaced::class.java)
    }

    @Test
    fun `deliver now places an order without a slot`() = runTest {
        val (vm, _) = createViewModel()
        vm.selectSlot(at(20, 30))
        vm.deliverNow()

        vm.placeOrder()
        advanceUntilIdle()

        assertThat(orderRepository.placed.single().scheduledFor).isNull()
    }

    @Test
    fun `a slot that is not on offer cannot be placed`() = runTest {
        val (vm, _) = createViewModel()

        vm.selectSlot(at(18, 30)) // only 20 minutes away

        assertThat(vm.uiState.value.selectedSlot).isNull()
        assertThat(vm.uiState.value.canPlaceOrder).isFalse()
        vm.placeOrder()
        advanceUntilIdle()
        assertThat(orderRepository.placed).isEmpty()
    }

    @Test
    fun `a slot that expires while the screen is open is rejected and refreshed`() = runTest {
        val (vm, events) = createViewModel()
        vm.selectPayment(PaymentMethod.CASH)
        vm.schedule() // 7:00 PM
        now = at(18, 20) // 7:00 PM is now only 40 minutes away

        vm.placeOrder()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(orderRepository.placed).isEmpty()
        assertThat(events).isEmpty()
        assertThat(state.form.error).isEqualTo(CheckoutViewModel.SLOT_EXPIRED)
        assertThat(state.selectedSlot).isNull()
        assertThat(state.slots.first().startMillis).isEqualTo(at(19, 30))
        assertThat(state.canPlaceOrder).isFalse()
    }

    @Test
    fun `restaurant hours bound the slots and late at night only tomorrow is offered`() = runTest {
        now = at(22, 50)
        val (vm, _) = createViewModel()

        val slots = vm.uiState.value.slots
        assertThat(slots.map { it.day }.toSet()).containsExactly(SlotDay.TOMORROW)
        assertThat(slots.first().startMillis).isEqualTo(at(11, 0, dayOffset = 1))
    }

    @Test
    fun `an empty cart cannot be placed even with a slot`() = runTest {
        val (vm, _) = createViewModel(withCart = false)

        // No restaurant: default hours still apply, but nothing can be ordered.
        vm.schedule()
        assertThat(vm.uiState.value.canPlaceOrder).isFalse()
    }
}
