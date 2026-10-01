package io.github.ieswar23.forkly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.forkly.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items WHERE restaurantId = :restaurantId ORDER BY sectionOrder, position")
    fun observeMenu(restaurantId: String): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MenuItemEntity>

    @Query("SELECT COUNT(*) FROM menu_items WHERE restaurantId = :restaurantId")
    suspend fun countFor(restaurantId: String): Int

    @Query(
        """
        SELECT * FROM menu_items
        WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY isBestseller DESC, name
        LIMIT :limit
        """,
    )
    fun search(query: String, limit: Int): Flow<List<MenuItemEntity>>

    @Query("DELETE FROM menu_items WHERE restaurantId = :restaurantId")
    suspend fun deleteFor(restaurantId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MenuItemEntity>)

    @Transaction
    suspend fun replaceMenu(restaurantId: String, items: List<MenuItemEntity>) {
        deleteFor(restaurantId)
        insertAll(items)
    }
}
