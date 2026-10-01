package io.github.ieswar23.forkly.ui.restaurant

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.MenuSection
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.ui.cart.CartEvent
import io.github.ieswar23.forkly.ui.cart.CartViewModel
import io.github.ieswar23.forkly.ui.common.AddToCartButton
import io.github.ieswar23.forkly.ui.common.BestsellerTag
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.FavoriteButton
import io.github.ieswar23.forkly.ui.common.PureVegBadge
import io.github.ieswar23.forkly.ui.common.RatingPill
import io.github.ieswar23.forkly.ui.common.ReplaceCartDialog
import io.github.ieswar23.forkly.ui.common.ShimmerBlock
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.ui.common.ViewCartBar
import io.github.ieswar23.forkly.ui.common.rememberGradient
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatCount
import io.github.ieswar23.forkly.util.formatDistance
import io.github.ieswar23.forkly.util.formatRupees
import kotlinx.coroutines.launch

private val HeaderMaxHeight = 248.dp
private val ToolbarHeight = 64.dp

@Composable
fun RestaurantRoute(
    onBack: () -> Unit,
    onViewCart: () -> Unit,
    viewModel: RestaurantViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var customizing by remember { mutableStateOf<MenuItem?>(null) }

    LaunchedEffect(cartViewModel) {
        cartViewModel.events.collect { event ->
            if (event is CartEvent.ItemAdded) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("${event.name} added to cart", duration = SnackbarDuration.Short)
            }
        }
    }

    val restaurant = state.restaurant
    val cart = cartState.cart
    val cartIsHere = cart.restaurantId == viewModel.restaurantId

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = !cart.isEmpty,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ViewCartBar(
                    itemCount = cart.itemCount,
                    totalPaise = cartState.bill.itemTotalPaise,
                    restaurantName = if (cartIsHere) null else cart.restaurant?.name,
                    onClick = onViewCart,
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { padding ->
        if (restaurant == null) {
            RestaurantSkeleton(onBack)
        } else {
            RestaurantContent(
                state = state,
                restaurant = restaurant,
                quantityOf = { if (cartIsHere) cart.quantityOf(it) else 0 },
                bottomPadding = padding.calculateBottomPadding(),
                onBack = onBack,
                onToggleFavorite = viewModel::toggleFavorite,
                onToggleVeg = viewModel::toggleVegOnly,
                onToggleBestsellers = viewModel::toggleBestsellers,
                onRetry = viewModel::loadMenu,
                onAdd = { item ->
                    if (item.isCustomizable) customizing = item else cartViewModel.addItem(restaurant, item)
                },
                onIncrement = { item ->
                    if (item.isCustomizable) customizing = item else cartViewModel.incrementItem(item.id)
                },
                onDecrement = { item -> cartViewModel.decrementItem(item.id) },
            )
        }
    }

    customizing?.let { item ->
        CustomizationSheet(
            item = item,
            onDismiss = { customizing = null },
            onAddToCart = { selection, qty ->
                restaurant?.let { cartViewModel.addItem(it, item, selection, qty) }
                customizing = null
            },
        )
    }

    cartState.pendingReplacement?.let { pending ->
        ReplaceCartDialog(
            currentRestaurant = pending.currentRestaurantName,
            newRestaurant = pending.newRestaurantName,
            onConfirm = cartViewModel::confirmReplacement,
            onDismiss = cartViewModel::dismissReplacement,
        )
    }
}

@Composable
private fun RestaurantContent(
    state: RestaurantUiState,
    restaurant: Restaurant,
    quantityOf: (String) -> Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleVeg: () -> Unit,
    onToggleBestsellers: () -> Unit,
    onRetry: () -> Unit,
    onAdd: (MenuItem) -> Unit,
    onIncrement: (MenuItem) -> Unit,
    onDecrement: (MenuItem) -> Unit,
) {
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val maxPx = with(density) { HeaderMaxHeight.toPx() } + statusBarPx
    val minPx = with(density) { ToolbarHeight.toPx() } + statusBarPx
    val range = maxPx - minPx
    var headerOffset by remember { mutableFloatStateOf(0f) }
    val collapse = if (range > 0) (-headerOffset / range).coerceIn(0f, 1f) else 1f

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Collapsing header: the header shrinks before the list scrolls, and expands after the list hits the top.
    val connection = remember(range) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta >= 0) return Offset.Zero
                val newOffset = (headerOffset + delta).coerceIn(-range, 0f)
                val consumed = newOffset - headerOffset
                headerOffset = newOffset
                return Offset(0f, consumed)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta <= 0) return Offset.Zero
                val newOffset = (headerOffset + delta).coerceIn(-range, 0f)
                val used = newOffset - headerOffset
                headerOffset = newOffset
                return Offset(0f, used)
            }
        }
    }

    val sectionIndices = remember(state.sections) { sectionStartIndices(state.sections) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().nestedScroll(connection)) {
            CollapsingHeader(
                restaurant = restaurant,
                heightPx = maxPx + headerOffset,
                collapse = collapse,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 88.dp),
            ) {
                item(key = "info") { RestaurantInfoCard(restaurant) }
                item(key = "menuControls") {
                    MenuControls(state, onToggleVeg, onToggleBestsellers)
                }
                when {
                    state.isMenuLoading -> item(key = "menuLoading") { MenuSkeleton() }
                    state.menuError != null -> item(key = "menuError") {
                        EmptyState(
                            emoji = "🍳",
                            title = "Menu unavailable",
                            message = state.menuError,
                            actionLabel = "Retry",
                            onAction = onRetry,
                        )
                    }
                    state.sections.isEmpty() -> item(key = "menuEmpty") {
                        EmptyState(
                            emoji = "🥦",
                            title = "Nothing matches",
                            message = "No dishes match these menu filters. Try turning one off.",
                        )
                    }
                    else -> menuSections(state.sections, quantityOf, onAdd, onIncrement, onDecrement)
                }
            }
        }

        if (state.sections.size > 1) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .then(if (bottomPadding == 0.dp) Modifier.navigationBarsPadding() else Modifier)
                    .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { menuOpen = true },
                    icon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null) },
                    text = { Text("Menu") },
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    state.sections.forEachIndexed { index, section ->
                        DropdownMenuItem(
                            text = {
                                Row(Modifier.width(220.dp)) {
                                    Text(section.name, modifier = Modifier.weight(1f))
                                    Text("${section.items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                menuOpen = false
                                headerOffset = -range
                                scope.launch { listState.animateScrollToItem(sectionIndices[index]) }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Index of each section's sticky header in the LazyColumn (2 leading items: info + controls). */
private fun sectionStartIndices(sections: List<MenuSection>): List<Int> {
    var index = 2
    return sections.map { section ->
        val start = index
        index += 1 + section.items.size
        start
    }
}

@Composable
private fun CollapsingHeader(
    restaurant: Restaurant,
    heightPx: Float,
    collapse: Float,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(with(density) { heightPx.toDp() })
            .background(rememberGradient(restaurant.gradientStart, restaurant.gradientEnd)),
    ) {
        // Big hero emoji that shrinks and fades as the header collapses.
        Text(
            restaurant.emoji,
            style = TextStyle(fontSize = 108.sp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 8.dp)
                .graphicsLayer {
                    val scale = 1f - 0.5f * collapse
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - collapse
                    rotationZ = -8f * (1f - collapse)
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.35f)))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 18.dp, end = 140.dp)
                .graphicsLayer { alpha = (1f - collapse * 1.6f).coerceIn(0f, 1f) },
        ) {
            Text(restaurant.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(restaurant.tagline, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(ToolbarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.25f * (1f - collapse)), CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                restaurant.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        alpha = ((collapse - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        translationY = (1f - collapse) * 24f
                    },
            )
            IconButton(
                onClick = {
                    val text = "Craving ${restaurant.cuisines.first()}? Try ${restaurant.name} in ${restaurant.area} on Forkly."
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                            "Share restaurant",
                        ),
                    )
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.25f * (1f - collapse)), CircleShape),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            FavoriteButton(isFavorite = restaurant.isFavorite, onToggle = onToggleFavorite)
        }
    }
}

@Composable
private fun RestaurantInfoCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(restaurant.cuisineLine, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            " ${restaurant.area} • ${formatDistance(restaurant.distanceKm)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    RatingPill(restaurant.rating)
                    Text(
                        "${formatCount(restaurant.ratingCount)} ratings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                InfoStat(Icons.Rounded.AccessTime, "${restaurant.deliveryTimeMins}–${restaurant.deliveryTimeMins + 5} mins", "Delivery")
                InfoStat(Icons.Rounded.CurrencyRupee, "₹${restaurant.costForTwo}", "For two")
                InfoStat(Icons.Rounded.AccessTime, restaurant.openHours.substringBefore(" –"), "Opens")
            }
            if (restaurant.isPureVeg) {
                Spacer(Modifier.height(12.dp))
                PureVegBadge()
            }
            if (restaurant.hasOffer) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(ForklyTheme.extraColors.offer.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.LocalOffer, null, tint = ForklyTheme.extraColors.offer, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(restaurant.offerText.orEmpty(), style = MaterialTheme.typography.titleSmall, color = ForklyTheme.extraColors.offer)
                        restaurant.offerCode?.let {
                            Text("Use code $it in your cart", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MenuControls(state: RestaurantUiState, onToggleVeg: () -> Unit, onToggleBestsellers: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("MENU", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        VegIndicator(isVeg = true, size = 14.dp)
        Spacer(Modifier.width(6.dp))
        Text("Veg only", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = state.vegOnly,
            onCheckedChange = { onToggleVeg() },
            colors = SwitchDefaults.colors(checkedTrackColor = ForklyTheme.extraColors.veg),
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = state.bestsellersOnly, onClick = onToggleBestsellers, label = { Text("Bestseller") })
    }
}

private fun LazyListScope.menuSections(
    sections: List<MenuSection>,
    quantityOf: (String) -> Int,
    onAdd: (MenuItem) -> Unit,
    onIncrement: (MenuItem) -> Unit,
    onDecrement: (MenuItem) -> Unit,
) {
    sections.forEach { section ->
        stickyHeader(key = "section-${section.name}") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(section.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "${section.items.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(section.items, key = { it.id }) { item ->
            MenuItemRow(
                item = item,
                quantity = quantityOf(item.id),
                onAdd = { onAdd(item) },
                onIncrement = { onIncrement(item) },
                onDecrement = { onDecrement(item) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .animateContentSize(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VegIndicator(item.isVeg)
                    if (item.isBestseller) {
                        Spacer(Modifier.width(8.dp))
                        BestsellerTag()
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(formatRupees(item.pricePaise), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
            Box(Modifier.width(128.dp), contentAlignment = Alignment.TopCenter) {
                val tint = if (item.isVeg) ForklyTheme.extraColors.veg else MaterialTheme.colorScheme.primary
                Box(
                    Modifier
                        .size(128.dp, 112.dp)
                        .background(
                            Brush.linearGradient(listOf(tint.copy(alpha = 0.10f), tint.copy(alpha = 0.24f))),
                            RoundedCornerShape(18.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji, style = TextStyle(fontSize = 52.sp))
                }
                AddToCartButton(
                    quantity = quantity,
                    onAdd = onAdd,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    customizable = item.isCustomizable,
                    modifier = Modifier.padding(top = 92.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MenuSkeleton() {
    Column(Modifier.padding(16.dp)) {
        repeat(4) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    ShimmerBlock(Modifier.width(140.dp).height(16.dp), RoundedCornerShape(6.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(Modifier.width(60.dp).height(14.dp), RoundedCornerShape(6.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(Modifier.fillMaxWidth(0.9f).height(12.dp), RoundedCornerShape(6.dp))
                }
                ShimmerBlock(Modifier.size(112.dp), RoundedCornerShape(18.dp))
            }
        }
    }
}

@Composable
private fun RestaurantSkeleton(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().height(HeaderMaxHeight)) {
            ShimmerBlock(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
        MenuSkeleton()
    }
}

