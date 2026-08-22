package com.shrey4sh.rabbithole.data.repository

import android.content.Context
import com.shrey4sh.rabbithole.data.local.RabbitHoleDatabase
import com.shrey4sh.rabbithole.data.local.RabbitHoleEntity
import com.shrey4sh.rabbithole.data.local.SavedItemEntity
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class LocalStorageRepository(context: Context) {

    private val db = RabbitHoleDatabase.get(context)
    private val holeDao = db.rabbitHoleDao()
    private val savedDao = db.savedDao()

    // ---- rabbit holes ----

    suspend fun saveHole(hole: RabbitHole) {
        holeDao.upsert(RabbitHoleEntity(
            id = hole.id,
            rootNodeId = hole.rootNodeId,
            nodesJson = json.encodeToString(hole.nodes),
            edgesJson = json.encodeToString(hole.edges),
            explorationPathJson = json.encodeToString(hole.explorationPath),
            nodeCount = hole.nodes.size,
            edgeCount = hole.edges.size,
            createdAt = hole.createdAt,
            updatedAt = System.currentTimeMillis(),
        ))
    }

    fun allHoles(): Flow<List<RabbitHoleEntity>> = holeDao.all()

    suspend fun restoreHole(id: String): RabbitHole? {
        val e = holeDao.byId(id) ?: return null
        return RabbitHole(
            id = e.id, rootNodeId = e.rootNodeId,
            nodes = json.decodeFromString<List<Node>>(e.nodesJson),
            edges = json.decodeFromString<List<Edge>>(e.edgesJson),
            explorationPath = json.decodeFromString(e.explorationPathJson),
            createdAt = e.createdAt, updatedAt = e.updatedAt,
        )
    }

    // ---- saved items ----

    fun savedItems(): Flow<List<SavedItemEntity>> = savedDao.all()

    fun isSaved(id: String): Flow<Boolean> = savedDao.isSaved(id)

    suspend fun saveNode(node: Node) {
        savedDao.upsert(SavedItemEntity(
            id = "node:${node.id}", kind = "NODE",
            title = node.title,
            subtitle = node.description.take(80),
            type = node.type.name,
            savedAt = System.currentTimeMillis()))
    }

    suspend fun unsave(id: String) = savedDao.delete(id)
}
