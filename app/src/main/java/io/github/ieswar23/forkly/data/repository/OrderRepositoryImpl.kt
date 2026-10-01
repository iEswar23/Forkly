package io.github.ieswar23.forkly.data.repository

import io.github.ieswar23.forkly.data.local.dao.CartDao
import io.github.ieswar23.forkly.data.local.dao.OrderDao
import io.github.ieswar23.forkly.data.local.entity.OrderEntity
import io.github.ieswar23.forkly.data.local.entity.OrderItemEntity
import io.github.ieswar23.forkly.data.mapper.toDomain
import io.github.ieswar23.forkly.data.remote.ForklyApi
import io.github.ieswar23.forkly.data.remote.dto.OrderLineDto
import io.github.ieswar23.forkly.data.remote.dto.PlaceOrderRequestDto
import io.github.ieswar23.forkly.di.ApplicationScope
import io.github.ieswar23.forkly.di.IoDispatcher
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.OrderStatus
import io.github.ieswar23.forkly.domain.model.PaymentMethod
import io.github.ieswar23.forkly.domain.model.PlaceOrderRequest
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import io.github.ieswar23.forkly.domain.tracking.OrderTracker
import io.github.ieswar23.forkly.util.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: ForklyApi,
    private val orderDao: OrderDao,
    private val cartDao: CartDao,
    private val preferences: PreferencesRepository,
    private val tracker: OrderTracker,
    private val pricing: PricingCalculator,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) : OrderRepository {

    private val trackingJobs = ConcurrentHashMap<String, Job>()

    override val orders: Flow<List<Order>> =
        orderDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override fun observeOrder(id: String): Flow<Order?> =
        orderDao.observeById(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun placeOrder(request: PlaceOrderRequest): Result<String> = withContext(io) {
        val cart = request.cart
        val restaurant = cart.restaurant
        if (cart.isEmpty || restaurant == null) {
            return@withContext Result.failure(IllegalStateException("Your cart is empty"))
        }
        try {
            val response = api.placeOrder(
                PlaceOrderRequestDto(
                    restaurantId = restaurant.id,
                    items = cart.lines.map { OrderLineDto(it.menuItemId, it.quantity, it.selectedOptionIds) },
                    totalPaise = request.bill.totalPaise,
                    paymentMethod = request.paymentMethod.name,
                    couponCode = request.bill.appliedCoupon?.code,
                    addressId = request.address.id,
                ),
            )
            val placedAt = clock.now()
            val order = OrderEntity(
                id = response.orderId,
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                restaurantEmoji = restaurant.emoji,
                restaurantArea = restaurant.area,
                itemTotalPaise = request.bill.itemTotalPaise,
                packagingFeePaise = request.bill.packagingFeePaise,
                deliveryFeePaise = request.bill.deliveryFeePaise,
                discountPaise = request.bill.couponDiscountPaise,
                gstPaise = request.bill.gstPaise,
                tipPaise = request.bill.tipPaise,
                totalPaise = request.bill.totalPaise,
                couponCode = request.bill.appliedCoupon?.code,
                paymentMethod = request.paymentMethod.name,
                addressLabel = request.address.displayLabel,
                addressLine = request.address.fullLine,
                deliveryInstructions = request.deliveryInstructions,
                status = OrderStatus.PLACED.name,
                placedAt = placedAt,
                deliveredAt = null,
                riderName = response.rider.name,
                riderPhone = response.rider.phone,
                riderVehicle = response.rider.vehicle,
                riderRating = response.rider.rating,
                riderDeliveries = response.rider.deliveries,
                userRating = null,
            )
            orderDao.insert(order, cart.lines.map { it.toOrderItem(order.id) })
            cartDao.clearCart()
            startTracking(order.id, placedAt)
            Result.success(order.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rateOrder(id: String, rating: Int) = withContext(io) {
        orderDao.updateRating(id, rating.coerceIn(1, 5))
    }

    override suspend fun resumeActiveOrders() = withContext(io) {
        orderDao.getActive().forEach { startTracking(it.id, it.placedAt) }
    }

    private fun startTracking(orderId: String, placedAt: Long) {
        trackingJobs[orderId]?.takeIf { it.isActive }?.let { return }
        trackingJobs[orderId] = appScope.launch(io) {
            tracker.statusChanges(placedAt).collect { status ->
                val deliveredAt = if (status == OrderStatus.DELIVERED) placedAt + tracker.totalMillis else null
                orderDao.updateStatus(orderId, status.name, deliveredAt)
            }
            trackingJobs.remove(orderId)
        }
    }

    override suspend fun seedHistoryIfNeeded() = withContext(io) {
        if (preferences.isHistorySeeded()) return@withContext
        if (orderDao.count() == 0) {
            SeedOrders.build(clock.now(), pricing).forEach { (order, items) -> orderDao.insert(order, items) }
        }
        preferences.markHistorySeeded()
    }

    private fun CartLine.toOrderItem(orderId: String) = OrderItemEntity(
        orderId = orderId,
        menuItemId = menuItemId,
        name = name,
        emoji = emoji,
        isVeg = isVeg,
        quantity = quantity,
        unitPricePaise = unitPricePaise,
        customizationSummary = customizationSummary,
        selectedOptionIds = selectedOptionIds,
    )
}

/** Two realistic past orders, priced with the real [PricingCalculator] so the numbers add up. */
internal object SeedOrders {

    private data class SeedLine(
        val id: String,
        val name: String,
        val emoji: String,
        val isVeg: Boolean,
        val qty: Int,
        val unitRupees: Int,
        val summary: String = "",
        val options: List<String> = emptyList(),
    )

    fun build(now: Long, pricing: PricingCalculator): List<Pair<OrderEntity, List<OrderItemEntity>>> = listOf(
        order(
            id = "FK48213907",
            restaurantId = "shahi-dastarkhwan",
            restaurantName = "Shahi Dastarkhwan",
            emoji = "🍛",
            area = "Banjara Hills",
            distanceKm = 2.4,
            placedAt = now - TimeUnit.DAYS.toMillis(3) - TimeUnit.HOURS.toMillis(2),
            coupon = null,
            tipPaise = 20_00,
            payment = PaymentMethod.UPI,
            rating = 5,
            rider = arrayOf("Mohammed Imran", "+91 99086 45120", "TS 13 FA 7316"),
            lines = listOf(
                SeedLine("shahi-dastarkhwan-01", "Hyderabadi Chicken Dum Biryani", "🍛", false, 2, 359,
                    "Regular (serves 1), Extra raita", listOf("portion:portion_0", "biryani_extras:biryani_extras_0")),
                SeedLine("shahi-dastarkhwan-07", "Chicken 65", "🍗", false, 1, 269),
                SeedLine("shahi-dastarkhwan-16", "Double Ka Meetha", "🍮", true, 1, 119),
            ),
            pricing = pricing,
        ),
        order(
            id = "FK47120586",
            restaurantId = "udupi-tiffin-corner",
            restaurantName = "Udupi Tiffin Corner",
            emoji = "🥞",
            area = "Ameerpet",
            distanceKm = 1.6,
            placedAt = now - TimeUnit.DAYS.toMillis(9) - TimeUnit.HOURS.toMillis(5),
            coupon = "WELCOME50",
            tipPaise = 0,
            payment = PaymentMethod.CASH,
            rating = null,
            rider = arrayOf("Ravi Kumar", "+91 98490 21573", "TS 09 EK 4821"),
            lines = listOf(
                SeedLine("udupi-tiffin-corner-01", "Masala Dosa", "🥞", true, 2, 109,
                    "Extra butter", listOf("dosa_extras:dosa_extras_0")),
                SeedLine("udupi-tiffin-corner-06", "Idli (2 pcs)", "⚪", true, 1, 49),
                SeedLine("udupi-tiffin-corner-14", "Filter Coffee", "☕", true, 2, 39),
            ),
            pricing = pricing,
        ),
    )

    private fun order(
        id: String,
        restaurantId: String,
        restaurantName: String,
        emoji: String,
        area: String,
        distanceKm: Double,
        placedAt: Long,
        coupon: String?,
        tipPaise: Long,
        payment: PaymentMethod,
        rating: Int?,
        rider: Array<String>,
        lines: List<SeedLine>,
        pricing: PricingCalculator,
    ): Pair<OrderEntity, List<OrderItemEntity>> {
        val cartLines = lines.map {
            CartLine(
                lineId = CartLine.lineIdFor(it.id, it.options),
                restaurantId = restaurantId,
                menuItemId = it.id,
                name = it.name,
                emoji = it.emoji,
                isVeg = it.isVeg,
                unitPricePaise = it.unitRupees * 100L,
                quantity = it.qty,
                customizationSummary = it.summary,
                selectedOptionIds = it.options,
                addedAt = placedAt,
            )
        }
        val bill = pricing.calculate(cartLines, distanceKm, coupon, tipPaise)
        val entity = OrderEntity(
            id = id,
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            restaurantEmoji = emoji,
            restaurantArea = area,
            itemTotalPaise = bill.itemTotalPaise,
            packagingFeePaise = bill.packagingFeePaise,
            deliveryFeePaise = bill.deliveryFeePaise,
            discountPaise = bill.couponDiscountPaise,
            gstPaise = bill.gstPaise,
            tipPaise = bill.tipPaise,
            totalPaise = bill.totalPaise,
            couponCode = bill.appliedCoupon?.code,
            paymentMethod = payment.name,
            addressLabel = "Home",
            addressLine = "Flat 302, Lakeview Residency, Road No. 12, Banjara Hills, Hyderabad 500034",
            deliveryInstructions = "",
            status = OrderStatus.DELIVERED.name,
            placedAt = placedAt,
            deliveredAt = placedAt + TimeUnit.MINUTES.toMillis(34),
            riderName = rider[0],
            riderPhone = rider[1],
            riderVehicle = rider[2],
            riderRating = 4.8,
            riderDeliveries = 2400,
            userRating = rating,
        )
        val items = lines.map {
            OrderItemEntity(
                orderId = id,
                menuItemId = it.id,
                name = it.name,
                emoji = it.emoji,
                isVeg = it.isVeg,
                quantity = it.qty,
                unitPricePaise = it.unitRupees * 100L,
                customizationSummary = it.summary,
                selectedOptionIds = it.options,
            )
        }
        return entity to items
    }
}
