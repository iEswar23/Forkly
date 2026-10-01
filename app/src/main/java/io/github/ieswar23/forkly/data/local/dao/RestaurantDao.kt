package io.github.ieswar23.forkly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.ieswar23.forkly.data.local.entity.BannerEntity
import io.github.ieswar23.forkly.data.local.entity.CategoryEntity
import io.github.ieswar23.forkly.data.local.entity.FavoriteEntity
import io.github.ieswar23.forkly.data.local.entity.RecentSearchEntity
import io.github.ieswar23.forkly.data.local.entity.RestaurantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM restaurants ORDER BY position")
    fun observeAll(): Flow<List<RestaurantEntity>>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    fun observeById(id: String): Flow<RestaurantEntity?>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getById(id: String): RestaurantEntity?

    @Query("SELECT id FROM restaurants ORDER BY position")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RestaurantEntity>)

    @Query("DELETE FROM restaurants WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)

    @Transaction
    suspend fun replaceAll(items: List<RestaurantEntity>) {
        upsertAll(items)
        deleteAllExcept(items.map { it.id })
    }

    // Banners & categories
    @Query("SELECT * FROM banners ORDER BY position")
    fun observeBanners(): Flow<List<BannerEntity>>

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(items: List<BannerEntity>)

    @Transaction
    suspend fun replaceBanners(items: List<BannerEntity>) {
        clearBanners()
        insertBanners(items)
    }

    @Query("SELECT * FROM categories ORDER BY position")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<CategoryEntity>)

    @Transaction
    suspend fun replaceCategories(items: List<CategoryEntity>) {
        clearCategories()
        insertCategories(items)
    }

    // Favorites
    @Query("SELECT restaurantId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE restaurantId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE restaurantId = :id")
    suspend fun removeFavorite(id: String)

    // Recent searches
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecentSearches(limit: Int): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentSearch(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
