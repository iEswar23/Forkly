package io.github.ieswar23.forkly.ui.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.tracking.OrderTracker
import io.github.ieswar23.forkly.domain.tracking.TrackingSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackingUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val snapshot: TrackingSnapshot? = null,
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val tracker: OrderTracker,
) : ViewModel() {

    val orderId: String = checkNotNull(savedStateHandle[ARG_ORDER_ID]) { "orderId is required" }

    /**
     * The persisted order drives the screen, and while it's active the tracker ticks every second
     * to animate the ETA countdown and rider position between status changes.
     */
    val uiState: StateFlow<TrackingUiState> = orderRepository.observeOrder(orderId)
        .flatMapLatest { order ->
            when {
                order == null -> flowOf(TrackingUiState(isLoading = false))
                order.status.isActive -> tracker.track(order.trackingStartsAt).map { TrackingUiState(false, order, it) }
                else -> flowOf(TrackingUiState(false, order, tracker.snapshotAt(tracker.totalMillis)))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackingUiState())

    fun rate(stars: Int) {
        viewModelScope.launch { orderRepository.rateOrder(orderId, stars) }
    }

    companion object {
        const val ARG_ORDER_ID = "orderId"
    }
}
