package io.github.ieswar23.forkly.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.util.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OrdersContent(val active: List<Order>, val past: List<Order>)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    orderRepository: OrderRepository,
) : ViewModel() {

    val uiState: StateFlow<UiState<OrdersContent>> = orderRepository.orders
        .map { orders ->
            if (orders.isEmpty()) {
                UiState.Empty
            } else {
                val (active, past) = orders.partition { it.status.isActive }
                UiState.Success(OrdersContent(active, past))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
