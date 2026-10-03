package io.github.ieswar23.forkly.ui

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.forkly.MainDispatcherRule
import io.github.ieswar23.forkly.domain.model.CouponError
import io.github.ieswar23.forkly.domain.model.CustomizationSelection
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderItem
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.model.Rider
import io.github.ieswar23.forkly.domain.pricing.BillSplitter
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.fakes.FakeAddressRepository
import io.github.ieswar23.forkly.fakes.FakeCartRepository
import io.github.ieswar23.forkly.fakes.FakeRestaurantRepository
import io.github.ieswar23.forkly.fakes.TestData
import io.github.ieswar23.forkly.ui.cart.CartEvent
import io.github.ieswar23.forkly.ui.cart.CartViewModel
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val shahi = TestData.restaurant("shahi", name = "Shahi Dastarkhwan", distanceKm = 2.0)
    private val napoli = TestData.restaurant("napoli", name = "Napoli Woodfire", distanceKm = 2.0)

    private val biryani = TestData.menuItem("shahi-01", "shahi", 329_00, isVeg = false)
    private val kebab = TestData.menuItem("shahi-02", "shahi", 120_00)
    private val pizza = TestData.menuItem(
        "napoli-01",
        "napoli",
        299_00,
        customizations = listOf(TestData.sizeGroup, TestData.toppingsGroup),
    )

    private var now = 1_000L
    private val cartRepository = FakeCartRepository(mapOf("shahi" to shahi, "napoli" to napoli))
    private val restaurantRepository = FakeRestaurantRepository(menuItems = listOf(biryani, kebab, pizza))

    private fun TestScope.createViewModel(): Pair<CartViewModel, MutableList<CartEvent>> {
        val viewModel = CartViewModel(
            cartRepository = cartRepository,
            restaurantRepository = restaurantRepository,
            addressRepository = FakeAddressRepository(),
            pricing = PricingCalculator(),
            billSplitter = BillSplitter(),
            clock = Clock { now++ },
        )
        val events = mutableListOf<CartEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel to events
    }

    @Test
    fun `adding the same dish twice merges into one line`() = runTest {
        val (vm, events) = createViewModel()

        vm.addItem(shahi, biryani)
        vm.addItem(shahi, biryani)

        val lines = vm.uiState.value.cart.lines
        assertThat(lines).hasSize(1)
        assertThat(lines.single().quantity).isEqualTo(2)
        assertThat(vm.uiState.value.cart.itemCount).isEqualTo(2)
        assertThat(events.filterIsInstance<CartEvent.ItemAdded>()).hasSize(2)
    }

    @Test
    fun `different customizations of a dish become separate priced lines`() = runTest {
        val (vm, _) = createViewModel()
        val regular = CustomizationSelection.defaultFor(pizza)
        val largeWithCheese = regular.toggle(TestData.sizeGroup, "large").toggle(TestData.toppingsGroup, "cheese")

        vm.addItem(napoli, pizza, regular)
        vm.addItem(napoli, pizza, largeWithCheese)

        val lines = vm.uiState.value.cart.lines
        assertThat(lines).hasSize(2)
        assertThat(lines.map { it.unitPricePaise }).containsExactly(299_00L, 509_00L)
        assertThat(lines[1].customizationSummary).isEqualTo("Large, Cheese")
        assertThat(vm.uiState.value.cart.quantityOf(pizza.id)).isEqualTo(2)
    }

    @Test
    fun `adding from another restaurant asks before replacing the cart`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)

        vm.addItem(napoli, pizza, CustomizationSelection.defaultFor(pizza))

        val pending = vm.uiState.value.pendingReplacement
        assertThat(pending).isNotNull()
        assertThat(pending!!.currentRestaurantName).isEqualTo("Shahi Dastarkhwan")
        assertThat(pending.newRestaurantName).isEqualTo("Napoli Woodfire")
        // Cart is untouched until the user confirms.
        assertThat(cartRepository.currentLines.map { it.restaurantId }).containsExactly("shahi")
    }

    @Test
    fun `confirming replacement swaps the cart and drops the old coupon`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.applyCoupon("WELCOME50")
        vm.addItem(napoli, pizza, CustomizationSelection.defaultFor(pizza))

        vm.confirmReplacement()

        val state = vm.uiState.value
        assertThat(state.pendingReplacement).isNull()
        assertThat(state.cart.lines.map { it.menuItemId }).containsExactly("napoli-01")
        assertThat(state.cart.restaurant?.name).isEqualTo("Napoli Woodfire")
        assertThat(state.cart.couponCode).isNull()
    }

    @Test
    fun `dismissing replacement keeps the existing cart`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.addItem(napoli, pizza, CustomizationSelection.defaultFor(pizza))

        vm.dismissReplacement()

        assertThat(vm.uiState.value.pendingReplacement).isNull()
        assertThat(vm.uiState.value.cart.lines.map { it.menuItemId }).containsExactly("shahi-01")
    }

    @Test
    fun `decrementing the last unit removes the line and offers undo`() = runTest {
        val (vm, events) = createViewModel()
        vm.addItem(shahi, kebab)
        val lineId = vm.uiState.value.cart.lines.single().lineId

        vm.decrement(lineId)

        assertThat(vm.uiState.value.cart.isEmpty).isTrue()
        val removed = events.filterIsInstance<CartEvent.LineRemoved>().single()
        vm.undoRemove(removed.line)
        assertThat(vm.uiState.value.cart.lines.single().quantity).isEqualTo(1)
    }

    @Test
    fun `increment and decrement adjust quantity and totals`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, kebab)
        val lineId = vm.uiState.value.cart.lines.single().lineId

        vm.increment(lineId)
        vm.increment(lineId)
        vm.decrement(lineId)

        assertThat(vm.uiState.value.cart.lines.single().quantity).isEqualTo(2)
        assertThat(vm.uiState.value.bill.itemTotalPaise).isEqualTo(240_00)
    }

    @Test
    fun `menu minus removes from the most recently added variant`() = runTest {
        val (vm, _) = createViewModel()
        val regular = CustomizationSelection.defaultFor(pizza)
        val large = regular.toggle(TestData.sizeGroup, "large")
        vm.addItem(napoli, pizza, regular)
        vm.addItem(napoli, pizza, large)

        vm.decrementItem(pizza.id)

        assertThat(vm.uiState.value.cart.lines.single().unitPricePaise).isEqualTo(299_00)
    }

    @Test
    fun `valid coupon is applied and reflected in the bill`() = runTest {
        val (vm, events) = createViewModel()
        vm.addItem(shahi, biryani) // ₹329

        vm.applyCoupon("welcome50")

        val state = vm.uiState.value
        assertThat(cartRepository.currentCoupon).isEqualTo("WELCOME50")
        assertThat(state.bill.couponDiscountPaise).isEqualTo(100_00) // 50% capped at ₹100
        val applied = events.filterIsInstance<CartEvent.CouponApplied>().single()
        assertThat(applied.savingsPaise).isEqualTo(100_00)
    }

    @Test
    fun `coupon below minimum order is rejected with a helpful message`() = runTest {
        val (vm, events) = createViewModel()
        vm.addItem(shahi, kebab) // ₹120

        vm.applyCoupon("FEAST120")

        assertThat(cartRepository.currentCoupon).isNull()
        val rejected = events.filterIsInstance<CartEvent.CouponRejected>().single()
        assertThat(rejected.message).isEqualTo("Add items worth ₹479 more to use FEAST120")
    }

    @Test
    fun `unknown coupon is rejected`() = runTest {
        val (vm, events) = createViewModel()
        vm.addItem(shahi, kebab)

        vm.applyCoupon("FREEFOOD")

        assertThat(events.filterIsInstance<CartEvent.CouponRejected>().single().message)
            .isEqualTo("\"FREEFOOD\" isn't a valid coupon")
    }

    @Test
    fun `applied coupon stops discounting when the cart drops below its minimum`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, kebab)
        vm.addItem(shahi, kebab) // ₹240
        vm.applyCoupon("WELCOME50") // min order ₹149
        assertThat(vm.uiState.value.bill.couponDiscountPaise).isEqualTo(100_00)
        val lineId = vm.uiState.value.cart.lines.single().lineId

        vm.decrement(lineId) // ₹120 now

        val bill = vm.uiState.value.bill
        assertThat(bill.couponDiscountPaise).isEqualTo(0)
        assertThat(bill.couponError).isInstanceOf(CouponError.MinOrderNotMet::class.java)
    }

    @Test
    fun `removing a coupon clears the discount`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.applyCoupon("WELCOME50")

        vm.removeCoupon()

        assertThat(vm.uiState.value.bill.couponDiscountPaise).isEqualTo(0)
        assertThat(vm.uiState.value.cart.couponCode).isNull()
    }

    @Test
    fun `eligible coupons are listed first`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani) // ₹329: WELCOME50, FREEDEL, SWEET20 eligible; FEAST120 not

        val coupons = vm.uiState.value.coupons
        assertThat(coupons.last().coupon.code).isEqualTo("FEAST120")
        assertThat(coupons.last().isEligible).isFalse()
        assertThat(coupons.last().shortByPaise).isEqualTo(270_00)
        assertThat(coupons.first().coupon.code).isEqualTo("WELCOME50")
    }

    @Test
    fun `tapping the selected tip again removes it`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)

        vm.setTip(30_00)
        assertThat(vm.uiState.value.bill.tipPaise).isEqualTo(30_00)

        vm.setTip(30_00)
        assertThat(vm.uiState.value.bill.tipPaise).isEqualTo(0)
    }

    @Test
    fun `reorder rebuilds lines at current menu prices and reports unavailable dishes`() = runTest {
        val (vm, events) = createViewModel()
        val order = pastOrder(
            OrderItem("shahi-01", "Biryani", "🍛", false, 2, 299_00, "", emptyList()),
            OrderItem("shahi-99", "Discontinued", "🍲", true, 1, 99_00, "", emptyList()),
        )

        vm.reorder(order)

        val line = vm.uiState.value.cart.lines.single()
        assertThat(line.quantity).isEqualTo(2)
        assertThat(line.unitPricePaise).isEqualTo(329_00) // today's price, not the old ₹299
        assertThat(events.filterIsInstance<CartEvent.Reordered>().single())
            .isEqualTo(CartEvent.Reordered(itemsAdded = 2, itemsUnavailable = 1))
    }

    // ---- Split bill ----

    @Test
    fun `split is unavailable while the cart is empty`() = runTest {
        val (vm, _) = createViewModel()
        assertThat(vm.uiState.value.split).isNull()
    }

    @Test
    fun `split defaults to two people sharing the whole bill including tip`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.setTip(30_00)

        val state = vm.uiState.value
        val split = state.split!!
        assertThat(split.people).isEqualTo(2)
        assertThat(split.splitTip).isTrue()
        assertThat(split.totalPaise).isEqualTo(state.bill.totalPaise)
        assertThat(split.shares.sumOf { it.amountPaise }).isEqualTo(state.bill.totalPaise)
    }

    @Test
    fun `people stepper is clamped to 2 through 10`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)

        vm.removeSplitPerson()
        assertThat(vm.uiState.value.split!!.people).isEqualTo(2)

        repeat(12) { vm.addSplitPerson() }
        assertThat(vm.uiState.value.split!!.people).isEqualTo(10)
        assertThat(vm.uiState.value.split!!.shares).hasSize(10)

        vm.setSplitPeople(0)
        assertThat(vm.uiState.value.split!!.people).isEqualTo(2)
        vm.setSplitPeople(4)
        assertThat(vm.uiState.value.split!!.people).isEqualTo(4)
    }

    @Test
    fun `orderer covers the tip when it is not split`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.setTip(50_00)
        vm.setSplitPeople(3)
        val tipSplit = vm.uiState.value.split!!

        vm.setSplitTip(false)

        val split = vm.uiState.value.split!!
        val total = vm.uiState.value.bill.totalPaise
        assertThat(split.splitTip).isFalse()
        assertThat(split.shares.first().tipCoveredPaise).isEqualTo(50_00)
        assertThat(split.shares.drop(1).map { it.tipCoveredPaise }).containsExactly(0L, 0L)
        assertThat(split.shares.last().amountPaise).isLessThan(tipSplit.shares.last().amountPaise)
        assertThat(split.shares.sumOf { it.amountPaise }).isEqualTo(total)
    }

    @Test
    fun `split follows the bill as the cart and coupon change`() = runTest {
        val (vm, _) = createViewModel()
        vm.addItem(shahi, biryani)
        vm.setSplitPeople(4)
        val before = vm.uiState.value.split!!.totalPaise

        vm.addItem(shahi, kebab)
        vm.applyCoupon("WELCOME50")

        val state = vm.uiState.value
        assertThat(state.split!!.people).isEqualTo(4) // settings survive cart changes
        assertThat(state.split!!.totalPaise).isEqualTo(state.bill.totalPaise)
        assertThat(state.split!!.totalPaise).isNotEqualTo(before)
        assertThat(state.split!!.shares.sumOf { it.amountPaise }).isEqualTo(state.bill.totalPaise)
    }

    private fun pastOrder(vararg items: OrderItem) = Order(
        id = "FK1", restaurantId = "shahi", restaurantName = "Shahi Dastarkhwan", restaurantEmoji = "🍛",
        restaurantArea = "Banjara Hills", items = items.toList(), itemTotalPaise = 0, packagingFeePaise = 0,
        deliveryFeePaise = 0, discountPaise = 0, gstPaise = 0, tipPaise = 0, totalPaise = 0, couponCode = null,
        paymentMethod = PaymentMethod.UPI, addressLabel = "Home", addressLine = "", deliveryInstructions = "",
        status = OrderStatus.DELIVERED, placedAt = 0, deliveredAt = 0,
        rider = Rider("Ravi", "", "", 4.8, 100), userRating = null,
    )
}
