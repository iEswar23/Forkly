package io.github.ieswar23.forkly.data

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.github.ieswar23.forkly.data.mapper.toDomain
import io.github.ieswar23.forkly.data.mapper.toEntities
import io.github.ieswar23.forkly.data.remote.ForklyApi
import io.github.ieswar23.forkly.data.remote.MockInterceptor
import io.github.ieswar23.forkly.data.remote.dto.OrderLineDto
import io.github.ieswar23.forkly.data.remote.dto.PlaceOrderRequestDto
import io.github.ieswar23.forkly.data.repository.SeedOrders
import io.github.ieswar23.forkly.domain.pricing.CouponCatalog
import io.github.ieswar23.forkly.domain.pricing.PricingCalculator
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

/**
 * Exercises the real Retrofit + Gson + MockInterceptor stack against the bundled JSON,
 * and checks the seed data is internally consistent (coupons, categories, menu ids).
 */
class MockApiContractTest {

    private val gson = Gson()
    private val api: ForklyApi = Retrofit.Builder()
        .baseUrl(ForklyApi.BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    MockInterceptor(
                        openAsset = { path -> File("src/main/assets/api/$path").inputStream() },
                        gson = gson,
                        minDelayMs = 0,
                        maxDelayMs = 0,
                    ),
                )
                .build(),
        )
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ForklyApi::class.java)

    @Test
    fun `restaurants endpoint serves twelve complete restaurants`() = runBlocking {
        val restaurants = api.getRestaurants()

        assertThat(restaurants).hasSize(12)
        assertThat(restaurants.map { it.id }.toSet()).hasSize(12)
        restaurants.forEach {
            assertThat(it.rating).isIn(com.google.common.collect.Range.closed(3.5, 5.0))
            assertThat(it.cuisines).isNotEmpty()
            assertThat(it.gradientStart).matches("#[0-9A-Fa-f]{6}")
        }
    }

    @Test
    fun `every menu has 10 to 18 dishes and parses customizations`() = runBlocking {
        api.getRestaurants().forEach { restaurant ->
            val menu = api.getMenu(restaurant.id)
            val items = menu.toEntities().map { it.toDomain() }

            assertThat(items.size).isIn(com.google.common.collect.Range.closed(10, 18))
            assertThat(items.map { it.id }.toSet()).hasSize(items.size)
            items.forEach { item ->
                assertThat(item.pricePaise).isGreaterThan(0L)
                assertThat(item.restaurantId).isEqualTo(restaurant.id)
                item.customizations.forEach { group -> assertThat(group.options).isNotEmpty() }
            }
        }
        assertThat(api.getMenu("napoli-woodfire").toEntities().map { it.toDomain() }.count { it.isCustomizable }).isAtLeast(6)
    }

    @Test
    fun `restaurant offer codes and banner coupons exist in the coupon catalog`() = runBlocking {
        val codes = CouponCatalog.all.map { it.code }
        api.getRestaurants().mapNotNull { it.offerCode }.forEach { assertThat(codes).contains(it) }
        api.getBanners().mapNotNull { it.couponCode }.forEach { assertThat(codes).contains(it) }
    }

    @Test
    fun `every category matches at least one restaurant cuisine`() = runBlocking {
        val cuisines = api.getRestaurants().flatMap { it.cuisines }.toSet()
        val categories = api.getCategories()
        assertThat(categories).isNotEmpty()
        categories.forEach { assertThat(cuisines).contains(it.name) }
        api.getBanners().mapNotNull { it.category }.forEach { assertThat(cuisines).contains(it) }
    }

    @Test
    fun `placing an order returns an id and an assigned rider`() = runBlocking {
        val response = api.placeOrder(
            PlaceOrderRequestDto("shahi-dastarkhwan", listOf(OrderLineDto("shahi-dastarkhwan-01", 1, emptyList())), 400_00, "UPI", null, 1),
        )
        assertThat(response.orderId).matches("FK\\d{8}")
        assertThat(response.rider.name).isNotEmpty()
    }

    @Test
    fun `unknown endpoints return 404`() {
        assertThrows(HttpException::class.java) { runBlocking { api.getMenu("does-not-exist") } }
    }

    @Test
    fun `seeded order history references real dishes and options`() = runBlocking {
        val seeds = SeedOrders.build(now = 1_700_000_000_000, pricing = PricingCalculator())
        assertThat(seeds).hasSize(2)
        seeds.forEach { (order, items) ->
            val menu = api.getMenu(order.restaurantId).toEntities().map { it.toDomain() }.associateBy { it.id }
            items.forEach { item ->
                val dish = menu[item.menuItemId]
                assertThat(dish).isNotNull()
                assertThat(dish!!.name).isEqualTo(item.name)
                val optionIds = dish.customizations.flatMap { g -> g.options.map { "${g.id}:${it.id}" } }
                assertThat(optionIds).containsAtLeastElementsIn(item.selectedOptionIds)
            }
            assertThat(order.totalPaise).isGreaterThan(order.itemTotalPaise - order.discountPaise)
        }
    }
}
