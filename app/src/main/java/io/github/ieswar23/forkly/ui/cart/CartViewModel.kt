package io.github.ieswar23.forkly.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.BillBreakdown
import io.github.ieswar23.forkly.domain.model.Cart
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.Coupon
import io.github.ieswar23.forkly.domain.model.CouponError
import io.github.ieswar23.forkly.domain.model.CouponValidation
import io.github.ieswar23.forkly.domain.model.CustomizationSelection
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.pricing.CouponCatalog
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.util.Clock
import io.github.ieswar23.forkly.util.formatRupees
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Waiting for the user to confirm discarding a cart from another restaurant. */
data class PendingReplacement(
    val currentRestaurantName: String,
    val newRestaurantName: String,
    val lines: List<CartLine>,
)

data class CouponOption(
    val coupon: Coupon,
    val isEligible: Boolean,
    val shortByPaise: Long,
    val savingsPaise: Long,
)

data class CartUiState(
    val isLoading: Boolean = true,
    val cart: Cart = Cart(),
    val bill: BillBreakdown = BillBreakdown.EMPTY,
    val coupons: List<CouponOption> = emptyList(),
    val pendingReplacement: PendingReplacement? = null,
    val deliveryAddress: Address? = null,
)

sealed interface CartEvent {
    data class CouponApplied(val code: String, val savingsPaise: Long) : CartEvent
    data class CouponRejected(val message: String) : CartEvent
    data class LineRemoved(val line: CartLine) : CartEvent
    data class ItemAdded(val name: String) : CartEvent
    data class Reordered(val itemsAdded: Int, val itemsUnavailable: Int) : CartEvent
}

/**
 * Owns every cart mutation in the app (menu, cart screen and reorder), including the
 * single-restaurant rule: adding from a different restaurant asks before replacing the cart.
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val restaurantRepository: RestaurantRepository,
    addressRepository: AddressRepository,
    private val pricing: PricingCalculator,
    private val clock: Clock,
) : ViewModel() {

    private val pendingReplacement = MutableStateFlow<PendingReplacement?>(null)
    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events: Flow<CartEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.cart,
        pendingReplacement,
        addressRepository.selectedAddress,
    ) { cart, pending, address ->
        val bill = billFor(cart)
        CartUiState(
            isLoading = false,
            cart = cart,
            bill = bill,
            coupons = couponOptions(cart, bill),
            pendingReplacement = pending,
            deliveryAddress = address,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    private fun billFor(cart: Cart): BillBreakdown =
        pricing.calculate(cart.lines, cart.restaurant?.distanceKm ?: 0.0, cart.couponCode, cart.tipPaise)

    private fun couponOptions(cart: Cart, bill: BillBreakdown): List<CouponOption> =
        CouponCatalog.all.map { coupon ->
            val validation = pricing.validateCoupon(coupon.code, bill.itemTotalPaise)
            val shortBy = ((validation as? CouponValidation.Invalid)?.error as? CouponError.MinOrderNotMet)?.shortByPaise ?: 0
            CouponOption(
                coupon = coupon,
                isEligible = validation is CouponValidation.Valid,
                shortByPaise = shortBy,
                savingsPaise = savingsFor(coupon, cart),
            )
        }.sortedWith(compareByDescending<CouponOption> { it.isEligible }.thenByDescending { it.savingsPaise })

    private fun savingsFor(coupon: Coupon, cart: Cart): Long {
        if (cart.isEmpty) return 0
        val withCoupon = pricing.calculate(cart.lines, cart.restaurant?.distanceKm ?: 0.0, coupon.code, 0)
        val without = pricing.calculate(cart.lines, cart.restaurant?.distanceKm ?: 0.0, null, 0)
        return (withCoupon.totalSavingsPaise - without.totalSavingsPaise).coerceAtLeast(0)
    }

    // ---- Adding ----

    fun addItem(restaurant: Restaurant, item: MenuItem, selection: CustomizationSelection? = null, quantity: Int = 1) {
        val line = CartLine.from(item, selection, quantity = quantity, now = clock.now())
        addLines(restaurant.id, restaurant.name, listOf(line))
    }

    private fun addLines(restaurantId: String, restaurantName: String, lines: List<CartLine>, announce: Boolean = true) {
        viewModelScope.launch {
            val cart = cartRepository.cart.first()
            val currentId = cart.restaurantId
            if (currentId != null && currentId != restaurantId) {
                pendingReplacement.value = PendingReplacement(
                    currentRestaurantName = cart.restaurant?.name ?: "another restaurant",
                    newRestaurantName = restaurantName,
                    lines = lines,
                )
            } else {
                lines.forEach { cartRepository.add(it) }
                if (announce && lines.size == 1) _events.send(CartEvent.ItemAdded(lines.single().name))
            }
        }
    }

    fun confirmReplacement() {
        val pending = pendingReplacement.value ?: return
        pendingReplacement.value = null
        viewModelScope.launch { cartRepository.replaceWith(pending.lines) }
    }

    fun dismissReplacement() {
        pendingReplacement.value = null
    }

    /** Rebuilds an old order's lines at today's menu prices, skipping dishes no longer on the menu. */
    fun reorder(order: Order) {
        viewModelScope.launch {
            restaurantRepository.ensureMenu(order.restaurantId)
            val menu = restaurantRepository.getMenuItems(order.items.map { it.menuItemId }).associateBy { it.id }
            val now = clock.now()
            val lines = order.items.mapNotNull { orderItem ->
                val item = menu[orderItem.menuItemId] ?: return@mapNotNull null
                val selection = selectionFromIds(item, orderItem.selectedOptionIds)
                CartLine.from(item, selection, quantity = orderItem.quantity, now = now)
            }
            if (lines.isEmpty()) {
                _events.send(CartEvent.Reordered(0, order.items.size))
                return@launch
            }
            addLines(order.restaurantId, order.restaurantName, lines, announce = false)
            _events.send(CartEvent.Reordered(lines.sumOf { it.quantity }, order.items.size - lines.size))
        }
    }

    private fun selectionFromIds(item: MenuItem, ids: List<String>): CustomizationSelection? {
        if (!item.isCustomizable) return null
        val selected = ids.mapNotNull { id ->
            val parts = id.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        val selection = CustomizationSelection(selected)
        return if (selection.isComplete(item.customizations)) selection else CustomizationSelection.defaultFor(item)
    }

    // ---- Quantity ----

    fun increment(lineId: String) {
        viewModelScope.launch {
            val line = cartRepository.cart.first().lines.firstOrNull { it.lineId == lineId } ?: return@launch
            cartRepository.updateQuantity(lineId, line.quantity + 1)
        }
    }

    fun decrement(lineId: String) {
        viewModelScope.launch {
            val line = cartRepository.cart.first().lines.firstOrNull { it.lineId == lineId } ?: return@launch
            cartRepository.updateQuantity(lineId, line.quantity - 1)
            if (line.quantity == 1) _events.send(CartEvent.LineRemoved(line))
        }
    }

    /** "+" on a menu item that isn't customizable: bumps its single line. */
    fun incrementItem(menuItemId: String) {
        viewModelScope.launch {
            val line = cartRepository.cart.first().lines.lastOrNull { it.menuItemId == menuItemId } ?: return@launch
            cartRepository.updateQuantity(line.lineId, line.quantity + 1)
        }
    }

    /** "−" on the menu: removes one unit from the most recently added variant of that dish. */
    fun decrementItem(menuItemId: String) {
        viewModelScope.launch {
            val line = cartRepository.cart.first().lines
                .filter { it.menuItemId == menuItemId }
                .maxByOrNull { it.addedAt } ?: return@launch
            cartRepository.updateQuantity(line.lineId, line.quantity - 1)
        }
    }

    fun undoRemove(line: CartLine) {
        viewModelScope.launch { cartRepository.restore(line) }
    }

    fun clearCart() {
        viewModelScope.launch { cartRepository.clear() }
    }

    // ---- Coupons & tip ----

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            val cart = cartRepository.cart.first()
            val itemTotal = cart.lines.sumOf { it.totalPaise }
            when (val result = pricing.validateCoupon(code, itemTotal)) {
                is CouponValidation.Valid -> {
                    cartRepository.setCoupon(result.coupon.code)
                    _events.send(CartEvent.CouponApplied(result.coupon.code, savingsFor(result.coupon, cart)))
                }
                is CouponValidation.Invalid -> _events.send(CartEvent.CouponRejected(couponErrorMessage(result.error)))
            }
        }
    }

    fun removeCoupon() {
        viewModelScope.launch { cartRepository.setCoupon(null) }
    }

    fun setTip(tipPaise: Long) {
        viewModelScope.launch {
            val current = cartRepository.cart.first().tipPaise
            // Tapping the selected tip again removes it.
            cartRepository.setTip(if (current == tipPaise) 0 else tipPaise)
        }
    }

    companion object {
        fun couponErrorMessage(error: CouponError): String = when (error) {
            is CouponError.NotFound -> "\"${error.code}\" isn't a valid coupon"
            is CouponError.MinOrderNotMet -> "Add items worth ${formatRupees(error.shortByPaise)} more to use ${error.coupon.code}"
            CouponError.EmptyCart -> "Add something to your cart first"
        }
    }
}
