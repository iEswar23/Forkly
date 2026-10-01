package io.github.ieswar23.forkly.ui.checkout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.ui.cart.BillDetailsCard
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.common.VegIndicator
import io.github.ieswar23.forkly.util.formatRupees

/** Wrapper so "add new" and "edit existing" share one sheet state. */
private data class AddressEditRequest(val address: Address?)

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AddressEditRequest?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CheckoutEvent.OrderPlaced -> onOrderPlaced(event.orderId)
            }
        }
    }
    // Swallow back presses while a (mock) payment is in flight.
    BackHandler(enabled = state.form.isPlacing) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.form.isPlacing) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!state.cart.isEmpty || state.form.isPlacing) {
                PlaceOrderBar(
                    totalPaise = state.bill.totalPaise,
                    method = state.form.paymentMethod,
                    enabled = state.canPlaceOrder,
                    onPlaceOrder = viewModel::placeOrder,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.cart.isEmpty && !state.form.isPlacing) {
                EmptyState(
                    emoji = "🧾",
                    title = "Nothing to check out",
                    message = "Your cart is empty. Add a few dishes and come back here to place your order.",
                    actionLabel = "Go back",
                    onAction = onBack,
                )
            } else {
                CheckoutContent(
                    state = state,
                    onSelectAddress = viewModel::selectAddress,
                    onEditAddress = { editing = AddressEditRequest(it) },
                    onAddAddress = { editing = AddressEditRequest(null) },
                    onToggleInstruction = viewModel::toggleInstruction,
                    onSelectPayment = viewModel::selectPayment,
                    onUpiChange = viewModel::updateUpiId,
                )
            }
            AnimatedVisibility(visible = state.form.isPlacing, enter = fadeIn(), exit = fadeOut()) {
                PlacingOverlay(state.form.placingMessage)
            }
        }
    }

    editing?.let { request ->
        AddressEditorSheet(
            initial = request.address,
            onSave = {
                viewModel.saveAddress(it)
                editing = null
            },
            onDelete = if (state.addresses.size > 1) {
                { id ->
                    viewModel.deleteAddress(id)
                    editing = null
                }
            } else {
                null
            },
            onDismiss = { editing = null },
        )
    }

    state.form.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Couldn't place order") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutUiState,
    onSelectAddress: (Long) -> Unit,
    onEditAddress: (Address) -> Unit,
    onAddAddress: () -> Unit,
    onToggleInstruction: (String) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onUpiChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StepTitle("1", "Delivery address") }
        items(state.addresses, key = { it.id }) { address ->
            AddressOptionCard(
                address = address,
                selected = address.id == state.selectedAddress?.id,
                onSelect = { onSelectAddress(address.id) },
                onEdit = { onEditAddress(address) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            OutlinedButton(onClick = onAddAddress, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add new address")
            }
        }
        item {
            Text("Delivery instructions", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryInstructionOptions.forEach { option ->
                    FilterChip(
                        selected = option in state.form.instructions,
                        onClick = { onToggleInstruction(option) },
                        label = { Text(option) },
                    )
                }
            }
        }
        item { StepTitle("2", "Payment method") }
        items(PaymentMethod.entries, key = { it.name }) { method ->
            PaymentOptionCard(
                method = method,
                selected = state.form.paymentMethod == method,
                upiId = state.form.upiId,
                upiValid = state.form.isUpiValid,
                onSelect = { onSelectPayment(method) },
                onUpiChange = onUpiChange,
            )
        }
        item { StepTitle("3", "Order summary") }
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${state.cart.restaurant?.emoji.orEmpty()} ${state.cart.restaurant?.name.orEmpty()}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    state.cart.lines.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VegIndicator(line.isVeg, size = 12.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${line.quantity} × ${line.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(formatRupees(line.totalPaise), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item { BillDetailsCard(state.bill) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Demo checkout — payments are simulated and no money is charged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepTitle(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AddressOptionCard(
    address: Address,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (address.label) {
        AddressLabel.HOME -> Icons.Rounded.Home
        AddressLabel.WORK -> Icons.Rounded.Apartment
        AddressLabel.OTHER -> Icons.Rounded.Place
    }
    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f).padding(top = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(address.displayLabel, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(address.fullLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${address.receiverName} • ${address.receiverPhone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit address") }
        }
    }
}

@Composable
private fun PaymentOptionCard(
    method: PaymentMethod,
    selected: Boolean,
    upiId: String,
    upiValid: Boolean,
    onSelect: () -> Unit,
    onUpiChange: (String) -> Unit,
) {
    val icon = when (method) {
        PaymentMethod.UPI -> Icons.Rounded.QrCode2
        PaymentMethod.CARD -> Icons.Rounded.CreditCard
        PaymentMethod.CASH -> Icons.Rounded.Payments
    }
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(method.title, style = MaterialTheme.typography.titleSmall)
                    Text(method.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RadioButton(selected = selected, onClick = onSelect)
            }
            AnimatedVisibility(
                visible = selected && method == PaymentMethod.UPI,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                OutlinedTextField(
                    value = upiId,
                    onValueChange = onUpiChange,
                    label = { Text("UPI ID") },
                    singleLine = true,
                    isError = !upiValid,
                    supportingText = { Text(if (upiValid) "A collect request will be sent to this ID" else "Enter a valid UPI ID, e.g. name@okbank") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
            AnimatedVisibility(
                visible = selected && method == PaymentMethod.CARD,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SavedCardPreview()
            }
        }
    }
}

@Composable
private fun SavedCardPreview() {
    Box(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(120.dp)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF455A64))),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Text("VISA", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.TopEnd))
        Icon(Icons.Rounded.Lock, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.TopStart).size(18.dp))
        Column(Modifier.align(Alignment.BottomStart)) {
            Text("••••  ••••  ••••  4242", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("AARAV REDDY  ·  08/29", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PlaceOrderBar(totalPaise: Long, method: PaymentMethod, enabled: Boolean, onPlaceOrder: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(targetState = totalPaise, label = "checkoutTotal") { total ->
                    Text(formatRupees(total), style = MaterialTheme.typography.titleLarge)
                }
                Text("via ${method.title}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onPlaceOrder,
                enabled = enabled,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (method == PaymentMethod.CASH) "Place order" else "Pay ${formatRupees(totalPaise)}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun PlacingOverlay(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(strokeWidth = 4.dp)
                Spacer(Modifier.height(16.dp))
                AnimatedContent(targetState = message, label = "placingMessage") {
                    Text(it, style = MaterialTheme.typography.titleSmall)
                }
                Text("Please don't press back", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
