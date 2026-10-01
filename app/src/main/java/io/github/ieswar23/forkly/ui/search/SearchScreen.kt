package io.github.ieswar23.forkly.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.DishResult
import io.github.ieswar23.forkly.ui.common.EmojiCircle
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.FilterSortSheet
import io.github.ieswar23.forkly.ui.common.RestaurantCard
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.util.formatRupees

@Composable
fun SearchRoute(
    onRestaurantClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        OutlinedTextField(
            value = viewModel.queryText,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Restaurants, dishes or cuisines") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (viewModel.queryText.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearQuery) { Icon(Icons.Rounded.Close, "Clear search") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.onSearchSubmitted()
                focusManager.clearFocus()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val active = state.filters.activeCount + if (state.sort != io.github.ieswar23.forkly.domain.model.SortOption.RELEVANCE) 1 else 0
            FilterChip(
                selected = active > 0,
                onClick = { showFilters = true },
                label = { Text(if (active > 0) "Sort & filter · $active" else "Sort & filter") },
                leadingIcon = { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) },
            )
            FilterChip(selected = state.filters.vegOnly, onClick = viewModel::toggleVegOnly, label = { Text("Pure veg") })
            if (state.sort != io.github.ieswar23.forkly.domain.model.SortOption.RELEVANCE) {
                Text(
                    state.sort.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Crossfade(targetState = state.isIdle, label = "searchContent") { idle ->
            if (idle) {
                SearchSuggestions(
                    state = state,
                    onSuggestion = {
                        viewModel.onSuggestionClick(it)
                        focusManager.clearFocus()
                    },
                    onClearRecent = viewModel::clearRecentSearches,
                )
            } else {
                SearchResults(
                    state = state,
                    onTabSelected = viewModel::selectTab,
                    onRestaurantClick = { id ->
                        viewModel.onSearchSubmitted()
                        onRestaurantClick(id)
                    },
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }

    if (showFilters) {
        FilterSortSheet(
            filters = state.filters,
            sort = state.sort,
            onApply = { f, s ->
                viewModel.applyFiltersAndSort(f, s)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

private val popularDishes = listOf("Biryani", "Masala Dosa", "Margherita", "Butter Chicken", "Cold Brew", "Brownie", "Shawarma", "Paneer Tikka")

@Composable
private fun SearchSuggestions(state: SearchUiState, onSuggestion: (String) -> Unit, onClearRecent: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        if (state.recentSearches.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearRecent) { Text("Clear") }
                }
            }
            items(state.recentSearches, key = { "recent-$it" }) { recent ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestion(recent) }
                        .padding(vertical = 12.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(recent, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Popular right now", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                popularDishes.forEach { dish -> AssistChip(onClick = { onSuggestion(dish) }, label = { Text(dish) }) }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (state.categories.isNotEmpty()) {
            item {
                Text("Explore cuisines", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEach { category ->
                        AssistChip(
                            onClick = { onSuggestion(category.name) },
                            label = { Text("${category.emoji}  ${category.name}") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: SearchUiState,
    onTabSelected: (SearchTab) -> Unit,
    onRestaurantClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SearchTab.entries.forEach { tab ->
                val count = if (tab == SearchTab.RESTAURANTS) state.restaurants.size else state.dishes.size
                Tab(
                    selected = state.tab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text("${tab.title} ($count)") },
                )
            }
        }
        AnimatedVisibility(visible = state.hasNoResults) {
            EmptyState(
                emoji = "🤔",
                title = "No matches for \"${state.query}\"",
                message = "Check the spelling or try a cuisine like Biryani, Pizza or Desserts.",
            )
        }
        if (!state.hasNoResults) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                when (state.tab) {
                    SearchTab.RESTAURANTS -> {
                        if (state.restaurants.isEmpty()) {
                            item { NoTabResults("No restaurants found — check the Dishes tab.") }
                        }
                        items(state.restaurants, key = { it.id }) { restaurant ->
                            RestaurantCard(
                                restaurant = restaurant,
                                onClick = { onRestaurantClick(restaurant.id) },
                                onToggleFavorite = { onToggleFavorite(restaurant.id) },
                                modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    SearchTab.DISHES -> {
                        if (state.dishes.isEmpty()) {
                            item { NoTabResults("Type at least 2 letters of a dish name to see matching dishes.") }
                        }
                        items(state.dishes, key = { it.item.id }) { dish ->
                            DishResultRow(dish, onClick = { onRestaurantClick(dish.item.restaurantId) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoTabResults(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun DishResultRow(dish: DishResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiCircle(dish.item.emoji, MaterialTheme.colorScheme.secondaryContainer, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VegIndicator(dish.item.isVeg, size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(dish.item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${dish.restaurantEmoji} ${dish.restaurantName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Text(formatRupees(dish.item.pricePaise), style = MaterialTheme.typography.labelLarge)
            }
            Text("View ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
