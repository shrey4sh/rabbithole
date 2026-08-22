package com.shrey4sh.rabbithole.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RabbitHoleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hole: RabbitHoleEntity)

    @Query("SELECT * FROM rabbit_holes ORDER BY updatedAt DESC")
    fun all(): Flow<List<RabbitHoleEntity>>

    @Query("SELECT * FROM rabbit_holes WHERE id = :id")
    suspend fun byId(id: String): RabbitHoleEntity?

    @Query("DELETE FROM rabbit_holes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SavedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SavedItemEntity)

    @Query("SELECT * FROM saved_items ORDER BY savedAt DESC")
    fun all(): Flow<List<SavedItemEntity>>

    @Query("DELETE FROM saved_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_items WHERE id = :id)")
    fun isSaved(id: String): Flow<Boolean>
}
