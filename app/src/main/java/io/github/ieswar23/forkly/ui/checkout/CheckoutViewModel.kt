package io.github.ieswar23.forkly.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.BillBreakdown
import io.github.ieswar23.forkly.domain.model.Cart
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.model.PlaceOrderRequest
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val DeliveryInstructionOptions = listOf("Leave at the door", "Avoid calling", "Don't ring the bell", "Leave with security")

data class CheckoutForm(
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val upiId: String = "aarav.reddy@okaxis",
    val instructions: Set<String> = emptySet(),
    val isPlacing: Boolean = false,
    val placingMessage: String = "",
    val error: String? = null,
) {
    val isUpiValid: Boolean get() = upiId.trim().matches(UPI_REGEX)

    companion object {
        val UPI_REGEX = Regex("^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z]{2,64}$")
    }
}

data class CheckoutUiState(
    val cart: Cart = Cart(),
    val bill: BillBreakdown = BillBreakdown.EMPTY,
    val addresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val form: CheckoutForm = CheckoutForm(),
) {
    val canPlaceOrder: Boolean
        get() = !cart.isEmpty && selectedAddress != null && !form.isPlacing &&
            (form.paymentMethod != PaymentMethod.UPI || form.isUpiValid)
}

sealed interface CheckoutEvent {
    data class OrderPlaced(val orderId: String) : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val orderRepository: OrderRepository,
    private val pricing: PricingCalculator,
) : ViewModel() {

    private val form = MutableStateFlow(CheckoutForm())
    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events: Flow<CheckoutEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<CheckoutUiState> = combine(
        cartRepository.cart,
        addressRepository.addresses,
        addressRepository.selectedAddress,
        form,
    ) { cart, addresses, selected, form ->
        CheckoutUiState(
            cart = cart,
            bill = pricing.calculate(cart.lines, cart.restaurant?.distanceKm ?: 0.0, cart.couponCode, cart.tipPaise),
            addresses = addresses,
            selectedAddress = selected,
            form = form,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    fun selectPayment(method: PaymentMethod) = form.update { it.copy(paymentMethod = method, error = null) }

    fun updateUpiId(value: String) = form.update { it.copy(upiId = value.trim(), error = null) }

    fun toggleInstruction(value: String) = form.update {
        it.copy(instructions = if (value in it.instructions) it.instructions - value else it.instructions + value)
    }

    fun selectAddress(id: Long) {
        viewModelScope.launch { addressRepository.select(id) }
    }

    fun saveAddress(address: Address) {
        viewModelScope.launch {
            val id = addressRepository.save(address)
            addressRepository.select(id)
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch { addressRepository.delete(id) }
    }

    fun dismissError() = form.update { it.copy(error = null) }

    fun placeOrder() {
        val state = uiState.value
        val address = state.selectedAddress ?: return
        if (!state.canPlaceOrder) return
        viewModelScope.launch {
            val method = state.form.paymentMethod
            form.update {
                it.copy(
                    isPlacing = true,
                    error = null,
                    placingMessage = when (method) {
                        PaymentMethod.UPI -> "Waiting for UPI confirmation…"
                        PaymentMethod.CARD -> "Securely processing your card…"
                        PaymentMethod.CASH -> "Confirming with the restaurant…"
                    },
                )
            }
            // Mock payment step: no real payment is ever made.
            if (method != PaymentMethod.CASH) delay(MOCK_PAYMENT_MS)
            form.update { it.copy(placingMessage = "Placing your order…") }
            val result = orderRepository.placeOrder(
                PlaceOrderRequest(
                    cart = state.cart,
                    bill = state.bill,
                    address = address,
                    paymentMethod = method,
                    deliveryInstructions = state.form.instructions.joinToString(", "),
                ),
            )
            result.fold(
                onSuccess = { orderId ->
                    // Keep the progress overlay up until navigation replaces this screen.
                    _events.send(CheckoutEvent.OrderPlaced(orderId))
                },
                onFailure = { error ->
                    form.update { it.copy(isPlacing = false, error = error.message ?: "Something went wrong. Please try again.") }
                },
            )
        }
    }

    private companion object {
        const val MOCK_PAYMENT_MS = 1_200L
    }
}
