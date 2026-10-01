package io.github.ieswar23.forkly.ui.restaurant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.MenuSection
import io.github.ieswar23.forkly.domain.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantUiState(
    val restaurant: Restaurant? = null,
    val sections: List<MenuSection> = emptyList(),
    val totalItems: Int = 0,
    val isMenuLoading: Boolean = true,
    val menuError: String? = null,
    val vegOnly: Boolean = false,
    val bestsellersOnly: Boolean = false,
)

private data class MenuFilter(val vegOnly: Boolean = false, val bestsellersOnly: Boolean = false)

private data class MenuLoad(val loading: Boolean = true, val error: String? = null)

@HiltViewModel
class RestaurantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RestaurantRepository,
) : ViewModel() {

    val restaurantId: String = checkNotNull(savedStateHandle[ARG_RESTAURANT_ID]) { "restaurantId is required" }

    private val filter = MutableStateFlow(MenuFilter())
    private val load = MutableStateFlow(MenuLoad())

    val uiState: StateFlow<RestaurantUiState> = combine(
        repository.observeRestaurant(restaurantId),
        repository.observeMenu(restaurantId),
        filter,
        load,
    ) { restaurant, menu, filter, load ->
        val sections = menu.mapNotNull { section ->
            val items = section.items.filter { item ->
                (!filter.vegOnly || item.isVeg) && (!filter.bestsellersOnly || item.isBestseller)
            }
            if (items.isEmpty()) null else section.copy(items = items)
        }
        RestaurantUiState(
            restaurant = restaurant,
            sections = sections,
            totalItems = menu.sumOf { it.items.size },
            isMenuLoading = load.loading && menu.isEmpty(),
            menuError = load.error.takeIf { menu.isEmpty() },
            vegOnly = filter.vegOnly,
            bestsellersOnly = filter.bestsellersOnly,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RestaurantUiState())

    init {
        loadMenu()
    }

    fun loadMenu() {
        viewModelScope.launch {
            load.value = MenuLoad(loading = true)
            val result = repository.ensureMenu(restaurantId)
            load.value = MenuLoad(loading = false, error = result.exceptionOrNull()?.let { "Couldn't load the menu. Please try again." })
        }
    }

    fun toggleVegOnly() = filter.update { it.copy(vegOnly = !it.vegOnly) }

    fun toggleBestsellers() = filter.update { it.copy(bestsellersOnly = !it.bestsellersOnly) }

    fun toggleFavorite() {
        viewModelScope.launch { repository.toggleFavorite(restaurantId) }
    }

    companion object {
        const val ARG_RESTAURANT_ID = "restaurantId"
    }
}
