package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.DishResult
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.MenuSection
import io.github.ieswar23.forkly.domain.model.Restaurant
import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {
    /** All restaurants (with favourite flags) from the Room cache. */
    val restaurants: Flow<List<Restaurant>>
    val banners: Flow<List<Banner>>
    val categories: Flow<List<Category>>
    val favorites: Flow<List<Restaurant>>
    val recentSearches: Flow<List<String>>

    fun observeRestaurant(id: String): Flow<Restaurant?>
    fun observeMenu(restaurantId: String): Flow<List<MenuSection>>
    fun searchDishes(query: String): Flow<List<DishResult>>

    /** Fetches restaurants, banners and categories from the API and caches them. */
    suspend fun refresh(): Result<Unit>

    /** Makes sure a restaurant's menu is cached, fetching it if needed. */
    suspend fun ensureMenu(restaurantId: String, forceRefresh: Boolean = false): Result<Unit>

    suspend fun getMenuItems(ids: List<String>): List<MenuItem>
    suspend fun toggleFavorite(restaurantId: String)
    suspend fun saveRecentSearch(query: String)
    suspend fun clearRecentSearches()
}
