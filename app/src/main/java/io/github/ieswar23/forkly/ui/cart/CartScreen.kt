package io.github.ieswar23.forkly.ui.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.pricing.BillSplit
import io.github.ieswar23.forkly.ui.common.EmojiCircle
import io.github.ieswar23.forkly.ui.common.EmojiTile
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.QuantityStepper
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import io.github.ieswar23.forkly.util.formatRupees

private val tipOptions = listOf(20_00L, 30_00L, 50_00L)

@Composable
fun CartRoute(
    onBrowse: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onCheckout: () -> Unit,
    onChangeAddress: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCoupons by remember { mutableStateOf(false) }
    var showSplit by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<CartEvent.CouponApplied?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.CouponApplied -> {
                    showCoupons = false
                    celebration = event
                }
                is CartEvent.CouponRejected -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
                is CartEvent.LineRemoved -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar("${event.line.name} removed", actionLabel = "UNDO")
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(event.line)
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cart", style = MaterialTheme.typography.titleLarge)
                        state.cart.restaurant?.let {
                            Text(
                                "${it.name} • ${it.deliveryTimeMins}–${it.deliveryTimeMins + 5} mins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (!state.cart.isEmpty) TextButton(onClick = { confirmClear = true }) { Text("Clear") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = !state.cart.isEmpty) {
                CheckoutBar(totalPaise = state.bill.totalPaise, onCheckout = onCheckout)
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.cart.isEmpty) {
            if (!state.isLoading) {
                EmptyState(
                    emoji = "🛒",
                    title = "Your cart is empty",
                    message = "Good food is always cooking! Browse restaurants near you and add something delicious.",
                    actionLabel = "Browse restaurants",
                    onAction = onBrowse,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
            }
        } else {
            CartContent(
                state = state,
                contentPadding = padding,
                onIncrement = viewModel::increment,
                onDecrement = viewModel::decrement,
                onAddMore = { state.cart.restaurantId?.let(onRestaurantClick) },
                onOpenCoupons = { showCoupons = true },
                onRemoveCoupon = viewModel::removeCoupon,
                onTip = viewModel::setTip,
                onOpenSplit = { showSplit = true },
                onChangeAddress = onChangeAddress,
            )
        }
    }

    val split = state.split
    if (showSplit && split != null) {
        SplitBillSheet(
            split = split,
            onAddPerson = viewModel::addSplitPerson,
            onRemovePerson = viewModel::removeSplitPerson,
            onSplitTipChange = viewModel::setSplitTip,
            onDismiss = { showSplit = false },
        )
    }

    if (showCoupons) {
        CouponSheet(
            coupons = state.coupons,
            appliedCode = state.bill.appliedCoupon?.code,
            onApply = viewModel::applyCoupon,
            onDismiss = { showCoupons = false },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear cart?") },
            text = { Text("All items from ${state.cart.restaurant?.name ?: "this restaurant"} will be removed.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearCart()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }

    celebration?.let { applied ->
        AlertDialog(
            onDismissRequest = { celebration = null },
            icon = { Text("🎉", style = TextStyle(fontSize = 44.sp)) },
            title = { Text("'${applied.code}' applied", textAlign = TextAlign.Center) },
            text = {
                Text(
                    if (applied.savingsPaise > 0) "You saved ${formatRupees(applied.savingsPaise)} with this coupon"
                    else "Coupon applied to your order",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { celebration = null }) { Text("YAY!") } },
        )
    }
}

@Composable
private fun CartContent(
    state: CartUiState,
    contentPadding: PaddingValues,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onAddMore: () -> Unit,
    onOpenCoupons: () -> Unit,
    onRemoveCoupon: () -> Unit,
    onTip: (Long) -> Unit,
    onOpenSplit: () -> Unit,
    onChangeAddress: () -> Unit,
) {
    val bill = state.bill
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "items") {
            CartCard {
                state.cart.restaurant?.let { restaurant ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        EmojiTile(
                            emoji = restaurant.emoji,
                            gradientStart = restaurant.gradientStart,
                            gradientEnd = restaurant.gradientEnd,
                            shape = RoundedCornerShape(12.dp),
                            emojiSize = 26.sp,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(restaurant.name, style = MaterialTheme.typography.titleMedium)
                            Text(restaurant.area, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Column(Modifier.animateContentSize()) {
                    state.cart.lines.forEach { line ->
                        CartLineRow(line, onIncrement = { onIncrement(line.lineId) }, onDecrement = { onDecrement(line.lineId) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onAddMore).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add more items", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item(key = "freeDelivery") { FreeDeliveryProgress(bill.amountToFreeDeliveryPaise, bill.itemTotalPaise) }
        item(key = "coupon") {
            CouponRow(state = state, onOpen = onOpenCoupons, onRemove = onRemoveCoupon)
        }
        item(key = "tip") { TipCard(selected = state.cart.tipPaise, onTip = onTip) }
        item(key = "bill") { BillDetailsCard(bill) }
        state.split?.let { split ->
            item(key = "split") { SplitBillRow(split, onOpen = onOpenSplit) }
        }
        if (bill.totalSavingsPaise > 0) {
            item(key = "savings") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ForklyTheme.extraColors.success.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Savings, null, tint = ForklyTheme.extraColors.success)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "You're saving ${formatRupees(bill.totalSavingsPaise)} on this order",
                            style = MaterialTheme.typography.titleSmall,
                            color = ForklyTheme.extraColors.success,
                        )
                    }
                }
            }
        }
        item(key = "address") {
            CartCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.deliveryAddress?.let { "Delivering to ${it.displayLabel}" } ?: "Add a delivery address",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        state.deliveryAddress?.let {
                            Text(
                                it.fullLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    TextButton(onClick = onChangeAddress) { Text("Change") }
                }
            }
        }
        item(key = "policy") {
            Text(
                "Review your order and address details to avoid cancellations. Orders once prepared can't be cancelled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun CartCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun CartLineRow(line: CartLine, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VegIndicator(line.isVeg, size = 14.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (line.customizationSummary.isNotBlank()) {
                Text(
                    line.customizationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatRupees(line.unitPricePaise),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            QuantityStepper(quantity = line.quantity, onIncrement = onIncrement, onDecrement = onDecrement, compact = true)
            Spacer(Modifier.height(4.dp))
            Text(formatRupees(line.totalPaise), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FreeDeliveryProgress(remainingPaise: Long, itemTotalPaise: Long) {
    val threshold = remainingPaise + itemTotalPaise
    val progress by animateFloatAsState(
        targetValue = if (threshold == 0L) 1f else (itemTotalPaise.toFloat() / threshold).coerceIn(0f, 1f),
        label = "freeDeliveryProgress",
    )
    CartCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DeliveryDining, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (remainingPaise > 0) "Add ${formatRupees(remainingPaise)} more for FREE delivery"
                    else "Yay! You've unlocked FREE delivery",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (remainingPaise > 0) MaterialTheme.colorScheme.primary else ForklyTheme.extraColors.success,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun CouponRow(state: CartUiState, onOpen: () -> Unit, onRemove: () -> Unit) {
    val applied = state.bill.appliedCoupon
    val pendingCode = state.cart.couponCode
    CartCard {
        when {
            applied != null -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = ForklyTheme.extraColors.success)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("'${applied.code}' applied", style = MaterialTheme.typography.titleSmall)
                    // Only what the coupon itself saves: a delivery fee already waived by the
                    // free-delivery threshold must not be credited to the coupon.
                    val savings = state.coupons.firstOrNull { it.coupon.code == applied.code }?.savingsPaise
                        ?: state.bill.couponDiscountPaise
                    Text(
                        if (savings > 0) "You saved ${formatRupees(savings)}" else applied.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForklyTheme.extraColors.success,
                    )
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            pendingCode != null && state.bill.couponError != null -> Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("'$pendingCode' not applicable", style = MaterialTheme.typography.titleSmall)
                    Text(
                        CartViewModel.couponErrorMessage(state.bill.couponError),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            else -> Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmojiCircle("🏷️", MaterialTheme.colorScheme.primaryContainer, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Apply coupon", style = MaterialTheme.typography.titleSmall)
                    val best = state.coupons.firstOrNull { it.isEligible && it.savingsPaise > 0 }
                    Text(
                        best?.let { "Save ${formatRupees(it.savingsPaise)} with ${it.coupon.code}" } ?: "View all coupons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
            }
        }
    }
}

@Composable
private fun SplitBillRow(split: BillSplit, onOpen: () -> Unit) {
    CartCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiCircle("👥", MaterialTheme.colorScheme.secondaryContainer, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Split bill", style = MaterialTheme.typography.titleSmall)
                val friendShare = split.shares.last().amountPaise
                val allEqual = split.shares.all { it.amountPaise == friendShare }
                Text(
                    "${split.people} people • ${if (allEqual) "" else "from "}${formatRupees(friendShare)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun TipCard(selected: Long, onTip: (Long) -> Unit) {
    CartCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Percent, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Tip your delivery partner", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "100% of the tip goes to your rider. You can tap again to remove it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tipOptions.forEach { amount ->
                    FilterChip(
                        selected = selected == amount,
                        onClick = { onTip(amount) },
                        label = { Text(formatRupees(amount)) },
                        leadingIcon = { Text(if (amount == 30_00L) "😍" else if (amount == 50_00L) "🤩" else "🙂") },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutBar(totalPaise: Long, onCheckout: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(formatRupees(totalPaise), style = MaterialTheme.typography.titleLarge)
                Text("TOTAL • incl. taxes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onCheckout,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Proceed to checkout", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

