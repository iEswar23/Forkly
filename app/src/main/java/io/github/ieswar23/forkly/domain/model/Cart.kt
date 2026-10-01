package io.github.ieswar23.forkly.domain.model

data class CartLine(
    val lineId: String,
    val restaurantId: String,
    val menuItemId: String,
    val name: String,
    val emoji: String,
    val isVeg: Boolean,
    val unitPricePaise: Long,
    val quantity: Int,
    val customizationSummary: String,
    val selectedOptionIds: List<String>,
    val addedAt: Long,
) {
    val totalPaise: Long get() = unitPricePaise * quantity

    companion object {
        /** Lines are unique per item + exact customization, so "Large + cheese" and "Regular" stack separately. */
        fun lineIdFor(menuItemId: String, optionIds: List<String>): String =
            if (optionIds.isEmpty()) menuItemId else "$menuItemId|${optionIds.sorted().joinToString(",")}"

        fun from(
            item: MenuItem,
            selection: CustomizationSelection?,
            quantity: Int = 1,
            now: Long = System.currentTimeMillis(),
        ): CartLine {
            val optionIds = selection?.optionIds(item.customizations).orEmpty()
            return CartLine(
                lineId = lineIdFor(item.id, optionIds),
                restaurantId = item.restaurantId,
                menuItemId = item.id,
                name = item.name,
                emoji = item.emoji,
                isVeg = item.isVeg,
                unitPricePaise = selection?.unitPricePaise(item) ?: item.pricePaise,
                quantity = quantity,
                customizationSummary = selection?.summary(item.customizations).orEmpty(),
                selectedOptionIds = optionIds,
                addedAt = now,
            )
        }
    }
}

data class Cart(
    val restaurant: Restaurant? = null,
    val lines: List<CartLine> = emptyList(),
    val couponCode: String? = null,
    val tipPaise: Long = 0,
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val isEmpty: Boolean get() = lines.isEmpty()
    val restaurantId: String? get() = lines.firstOrNull()?.restaurantId

    fun quantityOf(menuItemId: String): Int = lines.filter { it.menuItemId == menuItemId }.sumOf { it.quantity }
}
