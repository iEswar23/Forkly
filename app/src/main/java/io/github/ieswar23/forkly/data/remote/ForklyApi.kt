package io.github.ieswar23.forkly.data.remote

import io.github.ieswar23.forkly.data.remote.dto.BannerDto
import io.github.ieswar23.forkly.data.remote.dto.CategoryDto
import io.github.ieswar23.forkly.data.remote.dto.MenuDto
import io.github.ieswar23.forkly.data.remote.dto.PlaceOrderRequestDto
import io.github.ieswar23.forkly.data.remote.dto.PlaceOrderResponseDto
import io.github.ieswar23.forkly.data.remote.dto.RestaurantDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ForklyApi {
    @GET("restaurants")
    suspend fun getRestaurants(): List<RestaurantDto>

    @GET("restaurants/{id}/menu")
    suspend fun getMenu(@Path("id") restaurantId: String): MenuDto

    @GET("banners")
    suspend fun getBanners(): List<BannerDto>

    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @POST("orders")
    suspend fun placeOrder(@Body request: PlaceOrderRequestDto): PlaceOrderResponseDto

    companion object {
        const val BASE_URL = "https://api.forkly.app/v1/"
    }
}
