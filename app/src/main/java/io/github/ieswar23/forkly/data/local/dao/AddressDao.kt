package io.github.ieswar23.forkly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.ieswar23.forkly.data.local.entity.AddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses ORDER BY createdAt")
    fun observeAll(): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses WHERE id = :id")
    suspend fun getById(id: Long): AddressEntity?

    @Query("SELECT COUNT(*) FROM addresses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(address: AddressEntity): Long

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun delete(id: Long)
}
