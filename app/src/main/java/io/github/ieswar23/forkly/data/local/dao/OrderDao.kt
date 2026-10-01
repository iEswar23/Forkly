package io.github.ieswar23.forkly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.forkly.data.local.entity.OrderEntity
import io.github.ieswar23.forkly.data.local.entity.OrderItemEntity
import io.github.ieswar23.forkly.data.local.entity.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY placedAt DESC")
    fun observeAll(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeById(id: String): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderWithItems?

    @Query("SELECT * FROM orders WHERE status != 'DELIVERED'")
    suspend fun getActive(): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun insert(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertItems(items)
    }

    @Query("UPDATE orders SET status = :status, deliveredAt = :deliveredAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, deliveredAt: Long?)

    @Query("UPDATE orders SET userRating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: Int)
}
