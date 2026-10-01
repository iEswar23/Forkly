package io.github.ieswar23.forkly.data.remote.dto

data class RestaurantDto(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val area: String,
    val rating: Double,
    val ratingCount: Int,
    val deliveryTimeMins: Int,
    val costForTwo: Int,
    val distanceKm: Double,
    val isPureVeg: Boolean,
    val offerText: String?,
    val offerCode: String?,
    val emoji: String,
    val gradientStart: String,
    val gradientEnd: String,
    val tagline: String,
    val openHours: String,
)

data class MenuDto(
    val restaurantId: String,
    val sections: List<MenuSectionDto>,
)

data class MenuSectionDto(
    val name: String,
    val items: List<MenuItemDto>,
)

data class MenuItemDto(
    val id: String,
    val name: String,
    val description: String,
    /** Whole rupees, as served by the API. */
    val price: Int,
    val isVeg: Boolean,
    val isBestseller: Boolean,
    val emoji: String,
    val customizations: List<CustomizationGroupDto>?,
)

data class CustomizationGroupDto(
    val id: String,
    val title: String,
    val type: String,
    val required: Boolean,
    val maxSelections: Int,
    val options: List<CustomizationOptionDto>,
)

data class CustomizationOptionDto(
    val id: String,
    val name: String,
    /** Whole rupees. */
    val priceDelta: Int,
)

data class BannerDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val gradientStart: String,
    val gradientEnd: String,
    val couponCode: String?,
    val category: String?,
)

data class CategoryDto(
    val id: String,
    val name: String,
    val emoji: String,
)

data class RiderDto(
    val name: String,
    val phone: String,
    val vehicle: String,
    val rating: Double,
    val deliveries: Int,
)

data class PlaceOrderRequestDto(
    val restaurantId: String,
    val items: List<OrderLineDto>,
    val totalPaise: Long,
    val paymentMethod: String,
    val couponCode: String?,
    val addressId: Long,
)

data class OrderLineDto(
    val menuItemId: String,
    val quantity: Int,
    val options: List<String>,
)

data class PlaceOrderResponseDto(
    val orderId: String,
    val placedAt: Long,
    val rider: RiderDto,
)
