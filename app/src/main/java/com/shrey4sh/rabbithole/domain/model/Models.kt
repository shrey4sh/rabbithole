package com.shrey4sh.rabbithole.domain.model

import kotlinx.serialization.Serializable

enum class NodeType { PERSON, PLACE, EVENT, TECHNOLOGY, BOOK, MOVIE, GAME, MUSIC, ORGANIZATION, CONCEPT }

@Serializable
data class Node(
    val id: String,
    val title: String,
    val description: String = "",
    val type: NodeType,
    val imageUrl: String? = null,
    val sourceUrls: List<String> = emptyList(),
    val expanded: Boolean = false,
)

@Serializable
data class Edge(
    val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationship: String,     // e.g. CREATED_BY, INSPIRED_BY, RELATED_TO
    val confidence: Float = 1f,
    val sources: List<String> = emptyList(),
)

@Serializable
data class RabbitHole(
    val id: String,
    val rootNodeId: String,
    val nodes: List<Node>,
    val edges: List<Edge>,
    val explorationPath: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
