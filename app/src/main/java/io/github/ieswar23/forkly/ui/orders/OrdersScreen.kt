package io.github.ieswar23.forkly.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.ui.cart.CartEvent
import io.github.ieswar23.forkly.ui.cart.CartViewModel
import io.github.ieswar23.forkly.ui.common.EmojiCircle
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.ReplaceCartDialog
import io.github.ieswar23.forkly.ui.common.ShimmerBlock
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.UiState
import io.github.ieswar23.forkly.util.formatOrderDate
import io.github.ieswar23.forkly.util.formatRupees

@Composable
fun OrdersRoute(
    onOrderClick: (String) -> Unit,
    onBrowse: () -> Unit,
    onViewCart: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(cartViewModel) {
        cartViewModel.events.collect { event ->
            if (event is CartEvent.Reordered) {
                snackbarHostState.currentSnackbarData?.dismiss()
                val message = when {
                    event.itemsAdded == 0 -> "Sorry, these dishes are no longer available"
                    event.itemsUnavailable > 0 -> "${event.itemsAdded} items added • ${event.itemsUnavailable} unavailable"
                    else -> "${event.itemsAdded} item${if (event.itemsAdded == 1) "" else "s"} added to your cart"
                }
                val result = snackbarHostState.showSnackbar(message, actionLabel = if (event.itemsAdded > 0) "VIEW CART" else null)
                if (result == SnackbarResult.ActionPerformed) onViewCart()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your orders", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(3) { ShimmerBlock(Modifier.fillMaxWidth().height(150.dp), MaterialTheme.shapes.large) }
            }
            UiState.Empty, is UiState.Error -> EmptyState(
                emoji = "🧾",
                title = "No orders yet",
                message = "Once you place an order, you can track it and reorder your favourites from here.",
                actionLabel = "Order now",
                onAction = onBrowse,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (s.data.active.isNotEmpty()) {
                    item(key = "activeHeader") { ListHeader("Active orders") }
                    items(s.data.active, key = { it.id }) { order ->
                        OrderCard(order, onClick = { onOrderClick(order.id) }, onReorder = null, modifier = Modifier.animateItem())
                    }
                }
                if (s.data.past.isNotEmpty()) {
                    item(key = "pastHeader") { ListHeader("Past orders") }
                    items(s.data.past, key = { it.id }) { order ->
                        OrderCard(
                            order,
                            onClick = { onOrderClick(order.id) },
                            onReorder = { cartViewModel.reorder(order) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
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
private fun ListHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit, onReorder: (() -> Unit)?, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiCircle(order.restaurantEmoji, MaterialTheme.colorScheme.secondaryContainer, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(order.restaurantName, style = MaterialTheme.typography.titleMedium)
                    Text(order.restaurantArea, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(order.status)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatOrderDate(order.placedAt)} • ${formatRupees(order.totalPaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                order.userRating?.let { rating ->
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Text(" You rated $rating", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (onReorder != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) { Text("View details") }
                    Button(onClick = onReorder, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Replay, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reorder")
                    }
                }
            } else {
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeliveryDining, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Track order")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val (bg, fg) = when (status) {
        OrderStatus.DELIVERED -> ForklyTheme.extraColors.success.copy(alpha = 0.14f) to ForklyTheme.extraColors.success
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Text(
        status.title,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
