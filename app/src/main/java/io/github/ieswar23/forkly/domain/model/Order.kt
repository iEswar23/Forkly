package io.github.ieswar23.forkly.domain.model

enum class OrderStatus(val title: String, val subtitle: String) {
    PLACED("Order placed", "The restaurant has received your order"),
    PREPARING("Preparing your food", "The chef is cooking your order fresh"),
    OUT_FOR_DELIVERY("Out for delivery", "Your rider is on the way"),
    DELIVERED("Delivered", "Enjoy your meal!"),
    ;

    val isActive: Boolean get() = this != DELIVERED
}

enum class PaymentMethod(val title: String, val subtitle: String) {
    UPI("UPI", "Pay instantly with any UPI app"),
    CARD("Credit / Debit card", "Visa ending 4242"),
    CASH("Cash on delivery", "Pay with cash or UPI at your door"),
}

data class Rider(
    val name: String,
    val phone: String,
    val vehicle: String,
    val rating: Double,
    val deliveries: Int,
)

data class OrderItem(
    val menuItemId: String,
    val name: String,
    val emoji: String,
    val isVeg: Boolean,
    val quantity: Int,
    val unitPricePaise: Long,
    val customizationSummary: String,
    val selectedOptionIds: List<String>,
) {
    val totalPaise: Long get() = unitPricePaise * quantity
}

data class Order(
    val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val restaurantEmoji: String,
    val restaurantArea: String,
    val items: List<OrderItem>,
    val itemTotalPaise: Long,
    val packagingFeePaise: Long,
    val deliveryFeePaise: Long,
    val discountPaise: Long,
    val gstPaise: Long,
    val tipPaise: Long,
    val totalPaise: Long,
    val couponCode: String?,
    val paymentMethod: PaymentMethod,
    val addressLabel: String,
    val addressLine: String,
    val deliveryInstructions: String,
    val status: OrderStatus,
    val placedAt: Long,
    val deliveredAt: Long?,
    val rider: Rider,
    val userRating: Int?,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val itemsSummary: String get() = items.joinToString(", ") { "${it.quantity} × ${it.name}" }
}

/** Everything checkout needs to hand over to the order repository. */
data class PlaceOrderRequest(
    val cart: Cart,
    val bill: BillBreakdown,
    val address: Address,
    val paymentMethod: PaymentMethod,
    val deliveryInstructions: String,
)
