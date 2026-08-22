package com.shrey4sh.rabbithole.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "rabbit_holes")
data class RabbitHoleEntity(
    @PrimaryKey val id: String,
    val rootNodeId: String,
    val nodesJson: String,
    val edgesJson: String,
    val explorationPathJson: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "saved_items")
data class SavedItemEntity(
    @PrimaryKey val id: String,
    val kind: String,          // NODE or HOLE
    val title: String,
    val subtitle: String,
    val type: String,
    val savedAt: Long,
)

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
