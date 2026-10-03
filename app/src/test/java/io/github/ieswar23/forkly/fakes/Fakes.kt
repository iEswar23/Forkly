package io.github.ieswar23.forkly.fakes

import io.github.ieswar23.forkly.data.repository.AddressRepository
import io.github.ieswar23.forkly.data.repository.CartRepository
import io.github.ieswar23.forkly.data.repository.OrderRepository
import io.github.ieswar23.forkly.data.repository.PreferencesRepository
import io.github.ieswar23.forkly.data.repository.RestaurantRepository
import io.github.ieswar23.forkly.domain.model.Address
import io.github.ieswar23.forkly.domain.model.Banner
import io.github.ieswar23.forkly.domain.model.Cart
import io.github.ieswar23.forkly.domain.model.CartLine
import io.github.ieswar23.forkly.domain.model.Category
import io.github.ieswar23.forkly.domain.model.DishResult
import io.github.ieswar23.forkly.domain.model.MenuItem
import io.github.ieswar23.forkly.domain.model.MenuSection
import io.github.ieswar23.forkly.domain.model.Order
import io.github.ieswar23.forkly.domain.model.PlaceOrderRequest
import io.github.ieswar23.forkly.domain.model.Restaurant
import io.github.ieswar23.forkly.domain.model.ThemeMode
import io.github.ieswar23.forkly.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRestaurantRepository(
    initialRestaurants: List<Restaurant> = emptyList(),
    private val menuItems: List<MenuItem> = emptyList(),
) : RestaurantRepository {
    /** What the next refresh() "downloads". */
    var remoteRestaurants: List<Restaurant> = initialRestaurants
    var refreshError: Exception? = null
    var refreshCount = 0

    private val cache = MutableStateFlow<List<Restaurant>>(emptyList())
    private val favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    fun seedCache(list: List<Restaurant>) {
        cache.value = list
    }

    override val restaurants: Flow<List<Restaurant>> =
        combine(cache, favoriteIds) { list, favs -> list.map { it.copy(isFavorite = it.id in favs) } }
    override val banners: Flow<List<Banner>> = flowOf(emptyList())
    override val categories: Flow<List<Category>> = flowOf(listOf(Category("biryani", "Biryani", "🍛")))
    override val favorites: Flow<List<Restaurant>> = restaurants.map { list -> list.filter { it.isFavorite } }
    override val recentSearches: Flow<List<String>> = flowOf(emptyList())

    override fun observeRestaurant(id: String): Flow<Restaurant?> = restaurants.map { list -> list.firstOrNull { it.id == id } }
    override fun observeMenu(restaurantId: String): Flow<List<MenuSection>> =
        flowOf(listOf(MenuSection("Mains", menuItems.filter { it.restaurantId == restaurantId })))
    override fun searchDishes(query: String): Flow<List<DishResult>> = flowOf(emptyList())

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        refreshError?.let { return Result.failure(it) }
        cache.value = remoteRestaurants
        return Result.success(Unit)
    }

    override suspend fun ensureMenu(restaurantId: String, forceRefresh: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun getMenuItems(ids: List<String>): List<MenuItem> = menuItems.filter { it.id in ids }
    override suspend fun toggleFavorite(restaurantId: String) {
        favoriteIds.update { if (restaurantId in it) it - restaurantId else it + restaurantId }
    }
    override suspend fun saveRecentSearch(query: String) = Unit
    override suspend fun clearRecentSearches() = Unit
}

/** In-memory cart that mirrors the Room implementation's merge semantics. */
class FakeCartRepository(private val restaurants: Map<String, Restaurant> = emptyMap()) : CartRepository {
    private val lines = MutableStateFlow<List<CartLine>>(emptyList())
    private val coupon = MutableStateFlow<String?>(null)
    private val tip = MutableStateFlow(0L)

    val currentLines: List<CartLine> get() = lines.value
    val currentCoupon: String? get() = coupon.value

    override val cart: Flow<Cart> = combine(lines, coupon, tip) { l, c, t ->
        Cart(restaurant = l.firstOrNull()?.let { restaurants[it.restaurantId] }, lines = l, couponCode = c, tipPaise = t)
    }

    override suspend fun add(line: CartLine) {
        lines.update { current ->
            val existing = current.firstOrNull { it.lineId == line.lineId }
            if (existing != null) {
                current.map { if (it.lineId == line.lineId) it.copy(quantity = it.quantity + line.quantity) else it }
            } else {
                current + line
            }
        }
    }

    override suspend fun replaceWith(lines: List<CartLine>) {
        this.lines.value = lines
        coupon.value = null
        tip.value = 0
    }

    override suspend fun updateQuantity(lineId: String, quantity: Int) {
        lines.update { current ->
            if (quantity <= 0) current.filterNot { it.lineId == lineId }
            else current.map { if (it.lineId == lineId) it.copy(quantity = quantity) else it }
        }
    }

    override suspend fun restore(line: CartLine) = add(line)

    override suspend fun clear() {
        lines.value = emptyList()
        coupon.value = null
        tip.value = 0
    }

    override suspend fun setCoupon(code: String?) {
        coupon.value = code
    }

    override suspend fun setTip(tipPaise: Long) {
        tip.value = tipPaise
    }
}

class FakeAddressRepository(initial: List<Address> = listOf(TestData.homeAddress)) : AddressRepository {
    private val list = MutableStateFlow(initial)
    private val selectedId = MutableStateFlow<Long?>(initial.firstOrNull()?.id)

    override val addresses: Flow<List<Address>> = list
    override val selectedAddress: Flow<Address?> =
        combine(list, selectedId) { l, id -> l.firstOrNull { it.id == id } ?: l.firstOrNull() }

    override suspend fun save(address: Address): Long {
        val id = if (address.id == 0L) (list.value.maxOfOrNull { it.id } ?: 0) + 1 else address.id
        list.update { current -> current.filterNot { it.id == id } + address.copy(id = id) }
        return id
    }

    override suspend fun delete(id: Long) = list.update { current -> current.filterNot { it.id == id } }
    override suspend fun select(id: Long) {
        selectedId.value = id
    }
    override suspend fun seedDefaultsIfEmpty() = Unit
}

class FakePreferencesRepository : PreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = prefs
    override suspend fun setThemeMode(mode: ThemeMode) = prefs.update { it.copy(themeMode = mode) }
    override suspend fun updateProfile(name: String, phone: String, email: String) =
        prefs.update { it.copy(userName = name, phone = phone, email = email) }
    override suspend fun setSelectedAddressId(id: Long?) = prefs.update { it.copy(selectedAddressId = id) }
    override suspend fun setOrderUpdates(enabled: Boolean) = prefs.update { it.copy(orderUpdates = enabled) }
    override suspend fun setOffersAndPromos(enabled: Boolean) = prefs.update { it.copy(offersAndPromos = enabled) }
    override suspend fun isHistorySeeded(): Boolean = true
    override suspend fun markHistorySeeded() = Unit
}

/** Records placed orders and serves whatever orders a test puts in [orderList]. */
class FakeOrderRepository(initial: List<Order> = emptyList()) : OrderRepository {
    val orderList = MutableStateFlow(initial)
    val placed = mutableListOf<PlaceOrderRequest>()
    var placeError: Exception? = null

    override val orders: Flow<List<Order>> = orderList
    override fun observeOrder(id: String): Flow<Order?> = orderList.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun placeOrder(request: PlaceOrderRequest): Result<String> {
        placeError?.let { return Result.failure(it) }
        placed += request
        return Result.success("FK${10_000_000 + placed.size}")
    }

    override suspend fun rateOrder(id: String, rating: Int) =
        orderList.update { list -> list.map { if (it.id == id) it.copy(userRating = rating) else it } }

    override suspend fun resumeActiveOrders() = Unit
    override suspend fun seedHistoryIfNeeded() = Unit
}
