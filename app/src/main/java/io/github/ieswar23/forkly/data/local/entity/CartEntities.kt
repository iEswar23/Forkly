package io.github.ieswar23.forkly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val lineId: String,
    val restaurantId: String,
    val menuItemId: String,
    val name: String,
    val emoji: String,
    val isVeg: Boolean,
    val unitPricePaise: Long,
    val quantity: Int,
    val customizationSummary: String,
    val selectedOptionIds: List<String>,
    val addedAt: Long,
)

/** Single-row table holding cart-level choices (coupon, tip). */
@Entity(tableName = "cart_meta")
data class CartMetaEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val couponCode: String?,
    val tipPaise: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
