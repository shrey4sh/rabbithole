package com.shrey4sh.rabbithole.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val kind: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val savedAt: Long,
)
