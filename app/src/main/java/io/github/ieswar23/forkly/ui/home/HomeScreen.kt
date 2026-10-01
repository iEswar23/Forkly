package io.github.ieswar23.forkly.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.RestaurantFilters
import io.github.ieswar23.forkly.domain.model.SortOption
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.FilterSortSheet
import io.github.ieswar23.forkly.ui.common.InitialsAvatar
import io.github.ieswar23.forkly.ui.common.RestaurantCard
import io.github.ieswar23.forkly.ui.common.RestaurantCardPlaceholder
import io.github.ieswar23.forkly.ui.common.RestaurantMiniCard
import io.github.ieswar23.forkly.ui.common.SectionHeader
import io.github.ieswar23.forkly.ui.common.ShimmerBlock
import io.github.ieswar23.forkly.util.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onRestaurantClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddressClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onRestaurantClick = onRestaurantClick,
        onSearchClick = onSearchClick,
        onProfileClick = onProfileClick,
        onAddressClick = onAddressClick,
        onRefresh = viewModel::refresh,
        onCategoryClick = viewModel::selectCategory,
        onToggleVeg = viewModel::toggleVegOnly,
        onToggleRating = viewModel::toggleRating4Plus,
        onToggleOffers = viewModel::toggleOffersOnly,
        onToggleFast = viewModel::toggleFastDelivery,
        onApplyFilters = viewModel::applyFiltersAndSort,
        onClearRefinements = viewModel::clearRefinements,
        onToggleFavorite = viewModel::toggleFavorite,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRestaurantClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddressClick: () -> Unit,
    onRefresh: () -> Unit,
    onCategoryClick: (String?) -> Unit,
    onToggleVeg: () -> Unit,
    onToggleRating: () -> Unit,
    onToggleOffers: () -> Unit,
    onToggleFast: () -> Unit,
    onApplyFilters: (RestaurantFilters, SortOption) -> Unit,
    onClearRefinements: () -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item(key = "header") {
                    HomeHeader(
                        state = state,
                        onAddressClick = onAddressClick,
                        onProfileClick = onProfileClick,
                        onSearchClick = onSearchClick,
                    )
                }
                when (val feed = state.feed) {
                    UiState.Loading -> item(key = "loading") { HomeSkeleton() }
                    is UiState.Error -> item(key = "error") {
                        EmptyState(
                            emoji = "📡",
                            title = "We couldn't load restaurants",
                            message = feed.message,
                            actionLabel = "Try again",
                            onAction = onRefresh,
                        )
                    }
                    UiState.Empty -> item(key = "empty") {
                        EmptyState(
                            emoji = "🍽️",
                            title = "No restaurants yet",
                            message = "We're not delivering to this area right now. Pull down to refresh.",
                        )
                    }
                    is UiState.Success -> {
                        val data = feed.data
                        item(key = "banners") {
                            PromoCarousel(
                                banners = data.banners,
                                onBannerClick = { banner ->
                                    if (banner.category != null) {
                                        onCategoryClick(banner.category)
                                    }
                                    if (banner.couponCode != null) {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("Use code ${banner.couponCode} in your cart to save")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        item(key = "categories") {
                            CategoryRail(
                                categories = data.categories,
                                selected = state.selectedCategory,
                                onCategoryClick = onCategoryClick,
                            )
                        }
                        if (data.topRated.isNotEmpty() && state.selectedCategory == null) {
                            item(key = "topRatedHeader") {
                                SectionHeader(
                                    title = "Top rated near you",
                                    subtitle = "Loved by foodies in your neighbourhood",
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                                )
                            }
                            item(key = "topRated") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(data.topRated, key = { it.id }) { restaurant ->
                                        RestaurantMiniCard(restaurant, onClick = { onRestaurantClick(restaurant.id) })
                                    }
                                }
                            }
                        }
                        item(key = "allHeader") {
                            SectionHeader(
                                title = state.selectedCategory?.let { "$it near you" } ?: "All restaurants",
                                subtitle = "${data.restaurants.size} of ${data.totalRestaurants} restaurants delivering to you",
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                            )
                        }
                        stickyHeader(key = "filters") {
                            QuickFilterRow(
                                state = state,
                                onOpenSheet = { showFilterSheet = true },
                                onToggleVeg = onToggleVeg,
                                onToggleRating = onToggleRating,
                                onToggleOffers = onToggleOffers,
                                onToggleFast = onToggleFast,
                            )
                        }
                        if (data.restaurants.isEmpty()) {
                            item(key = "noResults") {
                                EmptyState(
                                    emoji = "🔍",
                                    title = "No restaurants match",
                                    message = "Try removing a filter or picking another cuisine.",
                                    actionLabel = "Clear filters",
                                    onAction = onClearRefinements,
                                )
                            }
                        } else {
                            items(data.restaurants, key = { it.id }) { restaurant ->
                                RestaurantCard(
                                    restaurant = restaurant,
                                    onClick = { onRestaurantClick(restaurant.id) },
                                    onToggleFavorite = { onToggleFavorite(restaurant.id) },
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSortSheet(
            filters = state.filters,
            sort = state.sort,
            onApply = { filters, sort ->
                onApplyFilters(filters, sort)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onAddressClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddressClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.address?.let { "${it.displayLabel} · ${it.locality}" } ?: "Set delivery location",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Change address")
                    }
                    Text(
                        text = state.address?.let { "Deliver to ${it.houseDetails}" } ?: "Tap to add an address",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            InitialsAvatar(
                name = state.userName,
                background = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 40.dp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onProfileClick),
            )
        }
        Spacer(Modifier.height(14.dp))
        val firstName = state.userName.substringBefore(" ").ifBlank { "there" }
        Text(
            "Hungry, $firstName?",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Order from the best spots in Hyderabad",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        FakeSearchBar(onClick = onSearchClick)
    }
}

private val searchHints = listOf("biryani", "masala dosa", "pizza", "cold coffee", "ice cream", "shawarma")

@Composable
private fun FakeSearchBar(onClick: () -> Unit) {
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_200)
            hintIndex = (hintIndex + 1) % searchHints.size
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(
            "Search for ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedContent(
            targetState = searchHints[hintIndex],
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "searchHint",
        ) { hint ->
            Text(
                "\"$hint\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryRail(categories: List<Category>, selected: String?, onCategoryClick: (String?) -> Unit) {
    if (categories.isEmpty()) return
    Column(Modifier.padding(top = 20.dp)) {
        SectionHeader(
            title = "What's on your mind?",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                val isSelected = category.name == selected
                val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, label = "categoryScale")
                Column(
                    Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCategoryClick(category.name) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(category.emoji, style = TextStyle(fontSize = 28.sp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickFilterRow(
    state: HomeUiState,
    onOpenSheet: () -> Unit,
    onToggleVeg: () -> Unit,
    onToggleRating: () -> Unit,
    onToggleOffers: () -> Unit,
    onToggleFast: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val count = state.filters.activeCount + if (state.sort != SortOption.RELEVANCE) 1 else 0
            FilterChip(
                selected = count > 0,
                onClick = onOpenSheet,
                label = { Text(if (count > 0) "Filters · $count" else "Sort & filter") },
                leadingIcon = {
                    if (count > 0) {
                        BadgedBox(badge = { Badge() }) { Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp)) }
                    } else {
                        Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp))
                    }
                },
            )
        }
        item { QuickChip("Pure veg", state.filters.vegOnly, onToggleVeg) }
        item { QuickChip("Rating 4.0+", state.filters.rating4Plus, onToggleRating) }
        item { QuickChip("Offers", state.filters.offersOnly, onToggleOffers) }
        item { QuickChip("Under 30 mins", state.filters.fastDelivery, onToggleFast) }
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.padding(16.dp)) {
        ShimmerBlock(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(22.dp))
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { ShimmerBlock(Modifier.size(60.dp), CircleShape) }
        }
        Spacer(Modifier.height(24.dp))
        ShimmerBlock(Modifier.width(180.dp).height(20.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(16.dp))
        repeat(2) {
            RestaurantCardPlaceholder()
            Spacer(Modifier.height(20.dp))
        }
    }
}
