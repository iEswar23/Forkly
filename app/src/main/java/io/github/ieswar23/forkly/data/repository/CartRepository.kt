package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.domain.model.Cart
import io.github.ieswar23.forkly.domain.model.CartLine
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    val cart: Flow<Cart>

    /** Adds a line, merging quantities when an identical line (same item + options) exists. */
    suspend fun add(line: CartLine)

    /** Empties the cart (and its coupon / tip) and fills it with [lines] atomically. */
    suspend fun replaceWith(lines: List<CartLine>)

    /** Sets a line's quantity; zero or less removes it. */
    suspend fun updateQuantity(lineId: String, quantity: Int)

    /** Puts a previously removed line back exactly as it was (used by "Undo"). */
    suspend fun restore(line: CartLine)

    suspend fun clear()
    suspend fun setCoupon(code: String?)
    suspend fun setTip(tipPaise: Long)
}
