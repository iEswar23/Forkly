package io.github.ieswar23.forkly.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.DishResult
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption
import io.github.ieswar23.forkly.domain.search.RestaurantQuery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab(val title: String) { RESTAURANTS("Restaurants"), DISHES("Dishes") }

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.RESTAURANTS,
    val restaurants: List<Restaurant> = emptyList(),
    val dishes: List<DishResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filters: RestaurantFilters = RestaurantFilters(),
    val sort: SortOption = SortOption.RELEVANCE,
) {
    val isIdle: Boolean get() = query.isBlank() && filters.isEmpty && sort == SortOption.RELEVANCE
    val hasNoResults: Boolean get() = !isIdle && restaurants.isEmpty() && dishes.isEmpty()
}

private data class Refinements(val filters: RestaurantFilters, val sort: SortOption, val tab: SearchTab)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: RestaurantRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /**
     * The text field's value, held as Compose state and updated synchronously on every keystroke.
     * Feeding the field from the async [uiState] pipeline instead would let a stale value land
     * mid-typing and reset the cursor to the start.
     */
    var queryText by mutableStateOf("")
        private set
    private val filters = MutableStateFlow(RestaurantFilters())
    private val sort = MutableStateFlow(SortOption.RELEVANCE)
    private val tab = MutableStateFlow(SearchTab.RESTAURANTS)

    private val debouncedQuery = query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged()

    private val dishResults = debouncedQuery.flatMapLatest { repository.searchDishes(it) }

    private val refinements = combine(filters, sort, tab) { f, s, t -> Refinements(f, s, t) }

    private val results = combine(repository.restaurants, debouncedQuery, dishResults, refinements) { all, q, dishes, ref ->
        val vegAwareDishes = dishes.filter { !ref.filters.vegOnly || it.item.isVeg }
        val restaurants = if (q.isBlank() && ref.filters.isEmpty && ref.sort == SortOption.RELEVANCE) {
            emptyList()
        } else {
            RestaurantQuery.apply(
                restaurants = all,
                filters = ref.filters,
                sort = ref.sort,
                text = q,
                dishMatchRestaurantIds = vegAwareDishes.map { it.item.restaurantId }.toSet(),
            )
        }
        restaurants to vegAwareDishes
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        results,
        refinements,
        repository.recentSearches,
        repository.categories,
    ) { q, (restaurants, dishes), ref, recent, categories ->
        SearchUiState(
            query = q,
            tab = ref.tab,
            restaurants = restaurants,
            dishes = dishes,
            recentSearches = recent,
            categories = categories,
            filters = ref.filters,
            sort = ref.sort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        queryText = value
        query.value = value
    }

    fun onSearchSubmitted() {
        val current = query.value
        viewModelScope.launch { repository.saveRecentSearch(current) }
    }

    fun onSuggestionClick(value: String) {
        onQueryChange(value)
        onSearchSubmitted()
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun selectTab(value: SearchTab) {
        tab.value = value
    }

    fun toggleVegOnly() = filters.update { it.copy(vegOnly = !it.vegOnly) }

    fun applyFiltersAndSort(newFilters: RestaurantFilters, newSort: SortOption) {
        filters.value = newFilters
        sort.value = newSort
    }

    fun clearRecentSearches() {
        viewModelScope.launch { repository.clearRecentSearches() }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
