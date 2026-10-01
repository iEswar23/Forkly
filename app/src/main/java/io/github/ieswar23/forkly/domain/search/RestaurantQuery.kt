package io.github.ieswar23.forkly.domain.search

import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption

/** Pure filtering / sorting rules shared by Home and Search. */
object RestaurantQuery {

    const val FAST_DELIVERY_MINS = 30

    fun apply(
        restaurants: List<Restaurant>,
        filters: RestaurantFilters = RestaurantFilters(),
        sort: SortOption = SortOption.RELEVANCE,
        cuisine: String? = null,
        text: String = "",
        dishMatchRestaurantIds: Set<String> = emptySet(),
    ): List<Restaurant> {
        val needle = text.trim()
        val filtered = restaurants.asSequence()
            .filter { cuisine == null || it.servesCuisine(cuisine) }
            .filter { !filters.vegOnly || it.isPureVeg }
            .filter { !filters.rating4Plus || it.rating >= 4.0 }
            .filter { !filters.offersOnly || it.hasOffer }
            .filter { !filters.fastDelivery || it.deliveryTimeMins <= FAST_DELIVERY_MINS }
            .filter { needle.isEmpty() || matches(it, needle) || it.id in dishMatchRestaurantIds }
            .toList()
        return sort(filtered, sort)
    }

    fun matches(restaurant: Restaurant, needle: String): Boolean =
        restaurant.name.contains(needle, ignoreCase = true) ||
            restaurant.cuisines.any { it.contains(needle, ignoreCase = true) } ||
            restaurant.area.contains(needle, ignoreCase = true)

    fun sort(restaurants: List<Restaurant>, sort: SortOption): List<Restaurant> = when (sort) {
        // "Relevance" blends quality and speed, the way delivery apps rank their feed.
        SortOption.RELEVANCE -> restaurants.sortedByDescending { it.rating * 10 - it.deliveryTimeMins / 5.0 }
        SortOption.RATING -> restaurants.sortedWith(compareByDescending<Restaurant> { it.rating }.thenByDescending { it.ratingCount })
        SortOption.DELIVERY_TIME -> restaurants.sortedWith(compareBy<Restaurant> { it.deliveryTimeMins }.thenByDescending { it.rating })
        SortOption.COST_LOW_TO_HIGH -> restaurants.sortedWith(compareBy<Restaurant> { it.costForTwo }.thenByDescending { it.rating })
        SortOption.COST_HIGH_TO_LOW -> restaurants.sortedWith(compareByDescending<Restaurant> { it.costForTwo }.thenByDescending { it.rating })
    }

    /** "Top rated near you": highly rated places within a comfortable delivery radius. */
    fun topRated(restaurants: List<Restaurant>, limit: Int = 8): List<Restaurant> =
        restaurants.filter { it.rating >= 4.3 && it.distanceKm <= 5.0 }
            .sortedWith(compareByDescending<Restaurant> { it.rating }.thenBy { it.distanceKm })
            .take(limit)
}
