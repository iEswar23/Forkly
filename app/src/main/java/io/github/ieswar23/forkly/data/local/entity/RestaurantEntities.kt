package io.github.ieswar23.forkly.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cuisines: List<String>,
    val area: String,
    val rating: Double,
    val ratingCount: Int,
    val deliveryTimeMins: Int,
    val costForTwo: Int,
    val distanceKm: Double,
    val isPureVeg: Boolean,
    val offerText: String?,
    val offerCode: String?,
    val emoji: String,
    val gradientStart: String,
    val gradientEnd: String,
    val tagline: String,
    val openHours: String,
    val position: Int,
)

@Entity(
    tableName = "menu_items",
    indices = [Index("restaurantId"), Index("name")],
)
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val section: String,
    val sectionOrder: Int,
    val position: Int,
    val name: String,
    val description: String,
    val pricePaise: Long,
    val isVeg: Boolean,
    val isBestseller: Boolean,
    val emoji: String,
    /** Customization groups serialized as JSON; they're always read together with the item. */
    val customizationsJson: String,
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val gradientStart: String,
    val gradientEnd: String,
    val couponCode: String?,
    val category: String?,
    val position: Int,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val position: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val restaurantId: String,
    val addedAt: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)
