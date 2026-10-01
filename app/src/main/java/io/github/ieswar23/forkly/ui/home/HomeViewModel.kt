package io.github.ieswar23.forkly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.PreferencesRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption
import io.github.ieswar23.forkly.domain.search.RestaurantQuery
import io.github.ieswar23.forkly.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeFeed(
    val banners: List<Banner>,
    val categories: List<Category>,
    val topRated: List<Restaurant>,
    /** Restaurants after category, filters and sort are applied. */
    val restaurants: List<Restaurant>,
    val totalRestaurants: Int,
)

data class HomeUiState(
    val feed: UiState<HomeFeed> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val address: Address? = null,
    val userName: String = "",
    val selectedCategory: String? = null,
    val filters: RestaurantFilters = RestaurantFilters(),
    val sort: SortOption = SortOption.RELEVANCE,
) {
    val hasActiveRefinements: Boolean get() = !filters.isEmpty || sort != SortOption.RELEVANCE || selectedCategory != null
}

private sealed interface RefreshState {
    data object Idle : RefreshState
    data object Refreshing : RefreshState
    data class Failed(val message: String) : RefreshState
}

private data class FeedQuery(val category: String?, val filters: RestaurantFilters, val sort: SortOption)

private data class FeedData(val restaurants: List<Restaurant>, val banners: List<Banner>, val categories: List<Category>)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    addressRepository: AddressRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(RestaurantFilters())
    private val sort = MutableStateFlow(SortOption.RELEVANCE)
    private val refreshState = MutableStateFlow<RefreshState>(RefreshState.Refreshing)

    private val data = combine(
        restaurantRepository.restaurants,
        restaurantRepository.banners,
        restaurantRepository.categories,
    ) { restaurants, banners, categories -> FeedData(restaurants, banners, categories) }

    private val query = combine(selectedCategory, filters, sort) { category, f, s -> FeedQuery(category, f, s) }

    private val header = combine(addressRepository.selectedAddress, preferencesRepository.preferences) { address, prefs ->
        address to prefs.userName
    }

    val uiState: StateFlow<HomeUiState> = combine(data, query, refreshState, header) { data, query, refresh, header ->
        val feed: UiState<HomeFeed> = when {
            data.restaurants.isNotEmpty() -> UiState.Success(
                HomeFeed(
                    banners = data.banners,
                    categories = data.categories,
                    topRated = RestaurantQuery.topRated(data.restaurants),
                    restaurants = RestaurantQuery.apply(
                        restaurants = data.restaurants,
                        filters = query.filters,
                        sort = query.sort,
                        cuisine = query.category,
                    ),
                    totalRestaurants = data.restaurants.size,
                ),
            )
            refresh is RefreshState.Failed -> UiState.Error(refresh.message)
            refresh is RefreshState.Refreshing -> UiState.Loading
            else -> UiState.Empty
        }
        HomeUiState(
            feed = feed,
            isRefreshing = refresh is RefreshState.Refreshing && data.restaurants.isNotEmpty(),
            address = header.first,
            userName = header.second,
            selectedCategory = query.category,
            filters = query.filters,
            sort = query.sort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = RefreshState.Refreshing
            refreshState.value = restaurantRepository.refresh().fold(
                onSuccess = { RefreshState.Idle },
                onFailure = { RefreshState.Failed(it.message ?: "Couldn't reach Forkly. Check your connection.") },
            )
        }
    }

    /** Tapping the selected category again clears it. */
    fun selectCategory(name: String?) {
        selectedCategory.update { current -> if (current == name) null else name }
    }

    fun toggleVegOnly() = filters.update { it.copy(vegOnly = !it.vegOnly) }

    fun toggleRating4Plus() = filters.update { it.copy(rating4Plus = !it.rating4Plus) }

    fun toggleOffersOnly() = filters.update { it.copy(offersOnly = !it.offersOnly) }

    fun toggleFastDelivery() = filters.update { it.copy(fastDelivery = !it.fastDelivery) }

    fun applyFiltersAndSort(newFilters: RestaurantFilters, newSort: SortOption) {
        filters.value = newFilters
        sort.value = newSort
    }

    fun clearRefinements() {
        filters.value = RestaurantFilters()
        sort.value = SortOption.RELEVANCE
        selectedCategory.value = null
    }

    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch { restaurantRepository.toggleFavorite(restaurantId) }
    }
}
