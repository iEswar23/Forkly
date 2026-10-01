package io.github.ieswar23.forkly.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val restaurantEmoji: String,
    val restaurantArea: String,
    val itemTotalPaise: Long,
    val packagingFeePaise: Long,
    val deliveryFeePaise: Long,
    val discountPaise: Long,
    val gstPaise: Long,
    val tipPaise: Long,
    val totalPaise: Long,
    val couponCode: String?,
    val paymentMethod: String,
    val addressLabel: String,
    val addressLine: String,
    val deliveryInstructions: String,
    val status: String,
    val placedAt: Long,
    val deliveredAt: Long?,
    val riderName: String,
    val riderPhone: String,
    val riderVehicle: String,
    val riderRating: Double,
    val riderDeliveries: Int,
    val userRating: Int?,
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val orderId: String,
    val menuItemId: String,
    val name: String,
    val emoji: String,
    val isVeg: Boolean,
    val quantity: Int,
    val unitPricePaise: Long,
    val customizationSummary: String,
    val selectedOptionIds: List<String>,
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val items: List<OrderItemEntity>,
)
