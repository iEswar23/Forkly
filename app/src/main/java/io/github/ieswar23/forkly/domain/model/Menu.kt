package io.github.ieswar23.forkly.domain.model

data class MenuSection(
    val name: String,
    val items: List<MenuItem>,
)

data class MenuItem(
    val id: String,
    val restaurantId: String,
    val section: String,
    val name: String,
    val description: String,
    val pricePaise: Long,
    val isVeg: Boolean,
    val isBestseller: Boolean,
    val emoji: String,
    val customizations: List<CustomizationGroup>,
) {
    val isCustomizable: Boolean get() = customizations.isNotEmpty()
}

enum class SelectionType { SINGLE, MULTIPLE }

data class CustomizationGroup(
    val id: String,
    val title: String,
    val type: SelectionType,
    val required: Boolean,
    val maxSelections: Int,
    val options: List<CustomizationOption>,
)

data class CustomizationOption(
    val id: String,
    val name: String,
    val priceDeltaPaise: Long,
)

/** A dish that matched a search query, along with the restaurant that serves it. */
data class DishResult(
    val item: MenuItem,
    val restaurantName: String,
    val restaurantEmoji: String,
)
