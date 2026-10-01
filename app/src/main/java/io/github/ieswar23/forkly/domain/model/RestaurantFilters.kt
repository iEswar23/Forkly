package io.github.ieswar23.forkly.domain.model

enum class SortOption(val title: String) {
    RELEVANCE("Relevance"),
    RATING("Rating: high to low"),
    DELIVERY_TIME("Delivery time: fastest"),
    COST_LOW_TO_HIGH("Cost: low to high"),
    COST_HIGH_TO_LOW("Cost: high to low"),
}

data class RestaurantFilters(
    val vegOnly: Boolean = false,
    val rating4Plus: Boolean = false,
    val offersOnly: Boolean = false,
    val fastDelivery: Boolean = false,
) {
    val activeCount: Int get() = listOf(vegOnly, rating4Plus, offersOnly, fastDelivery).count { it }
    val isEmpty: Boolean get() = activeCount == 0
}
