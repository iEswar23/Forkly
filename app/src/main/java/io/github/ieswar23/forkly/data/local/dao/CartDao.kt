package io.github.ieswar23.forkly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.forkly.data.local.entity.CartItemEntity
import io.github.ieswar23.forkly.data.local.entity.CartMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAt")
    fun observeItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY addedAt")
    suspend fun getItems(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE lineId = :lineId")
    suspend fun getLine(lineId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CartItemEntity>)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE lineId = :lineId")
    suspend fun updateQuantity(lineId: String, quantity: Int)

    @Query("DELETE FROM cart_items WHERE lineId = :lineId")
    suspend fun delete(lineId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearItems()

    @Query("SELECT * FROM cart_meta WHERE id = 0")
    fun observeMeta(): Flow<CartMetaEntity?>

    @Query("SELECT * FROM cart_meta WHERE id = 0")
    suspend fun getMeta(): CartMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: CartMetaEntity)

    @Query("DELETE FROM cart_meta")
    suspend fun clearMeta()

    @Transaction
    suspend fun clearCart() {
        clearItems()
        clearMeta()
    }

    @Transaction
    suspend fun replaceCart(items: List<CartItemEntity>) {
        clearItems()
        clearMeta()
        upsertAll(items)
    }
}
