package io.github.ieswar23.forkly.ui.tracking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.tracking.TrackingSnapshot
import io.github.ieswar23.forkly.ui.cart.BillDetailsCard
import io.github.ieswar23.forkly.ui.cart.toBill
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.InitialsAvatar
import io.github.ieswar23.forkly.ui.common.ShimmerBlock
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatCountdown
import io.github.ieswar23.forkly.util.formatOrderDate
import io.github.ieswar23.forkly.util.formatRating
import io.github.ieswar23.forkly.util.formatRupees
import io.github.ieswar23.forkly.util.formatScheduledFor
import io.github.ieswar23.forkly.util.formatTime

@Composable
fun TrackingRoute(
    onBack: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackingScreen(state = state, onBack = onBack, onRate = viewModel::rate)
}

@Composable
fun TrackingScreen(state: TrackingUiState, onBack: () -> Unit, onRate: (Int) -> Unit) {
    val order = state.order
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.restaurantName ?: "Your order", style = MaterialTheme.typography.titleMedium)
                        if (order != null) {
                            Text(
                                "Order #${order.id} • ${formatOrderDate(order.placedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> Column(Modifier.padding(padding).padding(16.dp)) {
                ShimmerBlock(Modifier.fillMaxWidth().height(160.dp), MaterialTheme.shapes.large)
                Spacer(Modifier.height(16.dp))
                ShimmerBlock(Modifier.fillMaxWidth().height(200.dp), MaterialTheme.shapes.large)
            }
            order == null -> EmptyState(
                emoji = "🧐",
                title = "Order not found",
                message = "We couldn't find this order. It may have been removed.",
                actionLabel = "Go back",
                onAction = onBack,
                modifier = Modifier.padding(padding),
            )
            else -> {
                val snapshot = state.snapshot
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { StatusHero(order, snapshot) }
                    if (snapshot != null) {
                        // Nothing moves on the map until a scheduled order's slot begins.
                        if (!snapshot.isWaitingForSlot) {
                            item { DeliveryMap(snapshot.status, snapshot.stageProgress, order.restaurantEmoji) }
                        }
                        item { Timeline(order, snapshot) }
                    }
                    item { RiderCard(order) }
                    if (order.status == OrderStatus.DELIVERED) {
                        item { RatingCard(order.userRating, onRate) }
                    }
                    item { OrderItemsCard(order) }
                    item { BillDetailsCard(order.toBill(), couponCode = order.couponCode, title = "Bill summary") }
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Delivery details", style = MaterialTheme.typography.titleMedium)
                                order.scheduledFor?.let { slot ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Scheduled for ${formatScheduledFor(slot)}", style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(order.addressLabel, style = MaterialTheme.typography.titleSmall)
                                        Text(order.addressLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (order.deliveryInstructions.isNotBlank()) {
                                    Text(
                                        "Instructions: ${order.deliveryInstructions}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "Paid via ${order.paymentMethod.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHero(order: Order, snapshot: TrackingSnapshot?) {
    val status = snapshot?.status ?: order.status
    val delivered = status == OrderStatus.DELIVERED
    val waiting = snapshot?.isWaitingForSlot == true
    val start by animateColorAsState(if (delivered) Color(0xFF15803D) else MaterialTheme.colorScheme.primary, label = "heroStart")
    val end by animateColorAsState(if (delivered) Color(0xFF4ADE80) else MaterialTheme.colorScheme.secondary, label = "heroEnd")
    val progress by animateFloatAsState(snapshot?.progress ?: 1f, tween(800), label = "heroProgress")

    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(start, end)), MaterialTheme.shapes.large)
            .padding(20.dp),
    ) {
        Column {
            AnimatedContent(
                targetState = if (waiting) null else status,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                label = "statusTitle",
            ) { current ->
                Column {
                    Text(current?.title ?: "Order scheduled", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        current?.subtitle ?: "The restaurant will start on it when your slot begins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (waiting && order.scheduledFor != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Scheduled for ${formatScheduledFor(order.scheduledFor)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
            } else if (!delivered && snapshot != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Arriving in", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatCountdown(snapshot.remainingMillis),
                        style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                    )
                }
            } else {
                Text(
                    order.deliveredAt?.let { "Delivered at ${formatTime(it)}" } ?: "Delivered",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun Timeline(order: Order, snapshot: TrackingSnapshot) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Order status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            // Live orders follow the tracker's clock; older orders are scaled to their real delivery time.
            // A scheduled order's timeline starts with its slot rather than when it was placed.
            val start = order.trackingStartsAt
            val trackedTotal = snapshot.stageStartedAt[OrderStatus.DELIVERED]
            val scale = if (order.deliveredAt != null && trackedTotal != null && trackedTotal > 0) {
                (order.deliveredAt - start).toDouble() / trackedTotal
            } else {
                1.0
            }
            OrderStatus.entries.forEachIndexed { index, step ->
                val reachedAt = snapshot.stageStartedAt[step]?.let { (it * scale).toLong() }
                TimelineStep(
                    step = step,
                    isDone = snapshot.status > step || snapshot.status == OrderStatus.DELIVERED,
                    isCurrent = snapshot.status == step && step != OrderStatus.DELIVERED,
                    time = when {
                        step == OrderStatus.PLACED -> formatTime(order.placedAt)
                        else -> reachedAt?.let { formatTime(start + it) }
                    },
                    isLast = index == OrderStatus.entries.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun TimelineStep(step: OrderStatus, isDone: Boolean, isCurrent: Boolean, time: String?, isLast: Boolean) {
    val active = isDone || isCurrent
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .scale(if (isCurrent) pulseScale else 1f)
                    .background(if (isDone) color else MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                } else if (isCurrent) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                }
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(if (isDone) color else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 20.dp)) {
            Text(
                step.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(step.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = time != null) {
            Text(time.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RiderCard(order: Order) {
    val context = LocalContext.current
    val rider = order.rider
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(
                name = rider.name,
                background = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 52.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (order.status == OrderStatus.DELIVERED) "Delivered by ${rider.name}" else "${rider.name} is your delivery partner",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = ForklyTheme.extraColors.rating, modifier = Modifier.size(14.dp))
                    Text(
                        " ${formatRating(rider.rating)} • ${rider.deliveries}+ deliveries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    rider.vehicle,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (order.status.isActive) {
                FilledTonalIconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${rider.phone.filter { it.isDigit() || it == '+' }}")))
                }) {
                    Icon(Icons.Rounded.Call, contentDescription = "Call rider")
                }
            }
        }
    }
}

@Composable
private fun RatingCard(current: Int?, onRate: (Int) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (current == null) "How was your food?" else "Thanks for rating!",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                (1..5).forEach { star ->
                    val filled = current != null && star <= current
                    val scale by animateFloatAsState(if (filled) 1.15f else 1f, label = "star$star")
                    IconButton(onClick = { onRate(star) }) {
                        Icon(
                            if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "$star star${if (star > 1) "s" else ""}",
                            tint = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp).scale(scale),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemsCard(order: Order) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${order.restaurantEmoji} ${order.restaurantName}", style = MaterialTheme.typography.titleMedium)
            Text(order.restaurantArea, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    VegIndicator(item.isVeg, size = 14.dp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${item.quantity} × ${item.name}", style = MaterialTheme.typography.bodyMedium)
                        if (item.customizationSummary.isNotBlank()) {
                            Text(item.customizationSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(formatRupees(item.totalPaise), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
