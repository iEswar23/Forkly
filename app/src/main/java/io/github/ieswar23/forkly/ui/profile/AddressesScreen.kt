package io.github.ieswar23.forkly.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.AddressLabel
import io.github.ieswar23.forkly.ui.checkout.AddressEditorSheet
import io.github.ieswar23.forkly.ui.common.EmptyState
import io.github.ieswar23.forkly.ui.theme.ForklyTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressesUiState(val addresses: List<Address> = emptyList(), val selectedId: Long? = null)

@HiltViewModel
class AddressesViewModel @Inject constructor(
    private val repository: AddressRepository,
) : ViewModel() {

    val uiState: StateFlow<AddressesUiState> = combine(repository.addresses, repository.selectedAddress) { list, selected ->
        AddressesUiState(list, selected?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddressesUiState())

    fun select(id: Long) {
        viewModelScope.launch { repository.select(id) }
    }

    fun save(address: Address) {
        viewModelScope.launch { repository.save(address) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

private data class EditRequest(val address: Address?)

@Composable
fun AddressesRoute(onBack: () -> Unit, viewModel: AddressesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<EditRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved addresses") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = EditRequest(null) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Add address") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.addresses.isEmpty()) {
            EmptyState(
                emoji = "📍",
                title = "No saved addresses",
                message = "Add your home or work address for faster checkout.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Tap an address to deliver there by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.addresses, key = { it.id }) { address ->
                    val selected = address.id == state.selectedId
                    Card(
                        onClick = { viewModel.select(address.id) },
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (address.label) {
                                    AddressLabel.HOME -> Icons.Rounded.Home
                                    AddressLabel.WORK -> Icons.Rounded.Apartment
                                    AddressLabel.OTHER -> Icons.Rounded.Place
                                },
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(address.displayLabel, style = MaterialTheme.typography.titleSmall)
                                    if (selected) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Rounded.CheckCircle, "Selected", tint = ForklyTheme.extraColors.success, modifier = Modifier.padding(top = 1.dp))
                                    }
                                }
                                Text(address.fullLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${address.receiverName} • ${address.receiverPhone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editing = EditRequest(address) }) { Icon(Icons.Rounded.Edit, "Edit address") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { request ->
        AddressEditorSheet(
            initial = request.address,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = { id ->
                viewModel.delete(id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
