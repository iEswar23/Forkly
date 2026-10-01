package io.github.ieswar23.forkly.domain.model

/**
 * A restaurant listed on Forkly. Monetary values are whole rupees here because they are only
 * used for display ("₹500 for two"); everything that flows into billing uses paise.
 */
data class Restaurant(
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
    val isFavorite: Boolean = false,
) {
    val cuisineLine: String get() = cuisines.joinToString(", ")
    val hasOffer: Boolean get() = !offerText.isNullOrBlank()

    fun servesCuisine(cuisine: String): Boolean = cuisines.any { it.equals(cuisine, ignoreCase = true) }
}

data class Banner(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val gradientStart: String,
    val gradientEnd: String,
    val couponCode: String?,
    val category: String?,
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
)
