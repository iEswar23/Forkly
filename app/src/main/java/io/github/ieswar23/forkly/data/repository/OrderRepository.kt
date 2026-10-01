package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.PlaceOrderRequest
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    val orders: Flow<List<Order>>

    fun observeOrder(id: String): Flow<Order?>

    /** Submits the order, persists it, clears the cart and starts live tracking. Returns the order id. */
    suspend fun placeOrder(request: PlaceOrderRequest): Result<String>

    suspend fun rateOrder(id: String, rating: Int)

    /** Re-attaches status tracking for orders that were still active when the app was closed. */
    suspend fun resumeActiveOrders()

    /** Adds a couple of past orders on first launch so history and reorder can be explored. */
    suspend fun seedHistoryIfNeeded()
}
