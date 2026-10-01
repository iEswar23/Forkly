package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.data.local.dao.CartDao
import io.github.ieswar23.forkly.data.local.dao.RestaurantDao
import io.github.ieswar23.forkly.data.local.entity.CartMetaEntity
import io.github.ieswar23.forkly.data.mapper.toDomain
import io.github.ieswar23.forkly.data.mapper.toEntity
import io.github.ieswar23.forkly.di.IoDispatcher
import io.github.ieswar23.forkly.domain.model.Cart
import io.github.ieswar23.forkly.domain.model.CartLine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao,
    private val restaurantDao: RestaurantDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CartRepository {

    override val cart: Flow<Cart> = combine(cartDao.observeItems(), cartDao.observeMeta()) { items, meta ->
        Cart(
            lines = items.map { it.toDomain() },
            couponCode = meta?.couponCode,
            tipPaise = meta?.tipPaise ?: 0,
        )
    }.flatMapLatest { cart ->
        val restaurantId = cart.restaurantId ?: return@flatMapLatest flowOf(cart)
        restaurantDao.observeById(restaurantId).map { entity -> cart.copy(restaurant = entity?.toDomain()) }
    }.distinctUntilChanged().flowOn(io)

    override suspend fun add(line: CartLine) = withContext(io) {
        val existing = cartDao.getLine(line.lineId)
        if (existing != null) {
            cartDao.updateQuantity(line.lineId, existing.quantity + line.quantity)
        } else {
            cartDao.upsert(line.toEntity())
        }
    }

    override suspend fun replaceWith(lines: List<CartLine>) = withContext(io) {
        cartDao.replaceCart(lines.map { it.toEntity() })
    }

    override suspend fun updateQuantity(lineId: String, quantity: Int) = withContext(io) {
        if (quantity <= 0) cartDao.delete(lineId) else cartDao.updateQuantity(lineId, quantity)
    }

    override suspend fun restore(line: CartLine) = withContext(io) { cartDao.upsert(line.toEntity()) }

    override suspend fun clear() = withContext(io) { cartDao.clearCart() }

    override suspend fun setCoupon(code: String?) = withContext(io) {
        val meta = cartDao.getMeta()
        cartDao.upsertMeta(CartMetaEntity(couponCode = code, tipPaise = meta?.tipPaise ?: 0))
    }

    override suspend fun setTip(tipPaise: Long) = withContext(io) {
        val meta = cartDao.getMeta()
        cartDao.upsertMeta(CartMetaEntity(couponCode = meta?.couponCode, tipPaise = tipPaise))
    }
}
