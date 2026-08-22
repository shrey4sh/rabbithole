package com.shrey4sh.rabbithole.data.mock

import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType

/**
 * Deeper-layer content for "Take Me Deeper" expansion.
 * Maps node title -> list of new nodes + edges to grow the graph organically.
 */
object DeeperLayers {

    data class DeepLayer(val newNodeIds: List<String>, val newEdges: List<Triple<String, String, String>>)

    private val layers = mutableMapOf(
        "gibson" to DeepLayer(
            newNodeIds = listOf("neuromancer", "sprawl", "cyberspace"),
            newEdges = listOf(
                Triple("gibson", "neuromancer", "WORKED_ON"),
                Triple("neuromancer", "cyberspace", "CREATED_BY"),
                Triple("neuromancer", "sprawl", "BASED_ON"),
            ),
        ),
        "neuromancer" to DeepLayer(
            newNodeIds = listOf("hugo", "matrix"),
            newEdges = listOf(
                Triple("neuromancer", "hugo", "RELATED_TO"),
                Triple("neuromancer", "matrix", "INFLUENCED"),
            ),
        ),
        "turing" to DeepLayer(
            newNodeIds = listOf("bletchley", "enigma2"),
            newEdges = listOf(
                Triple("turing", "bletchley", "LOCATED_IN"),
                Triple("turing", "enigma2", "WORKED_ON"),
            ),
        ),
        "ai" to DeepLayer(
            newNodeIds = listOf("singularity2", "agi", "ethics"),
            newEdges = listOf(
                Triple("ai", "agi", "BASED_ON"),
                Triple("ai", "ethics", "RELATED_TO"),
                Triple("agi", "singularity2", "RELATED_TO"),
            ),
        ),
        "cp2077" to DeepLayer(
            newNodeIds = listOf("johnny", "samurai"),
            newEdges = listOf(
                Triple("cp2077", "johnny", "MEMBER_OF"),
                Triple("johnny", "samurai", "MEMBER_OF"),
            ),
        ),
        "keanu" to DeepLayer(
            newNodeIds = listOf("matrixmovie", "johnwick"),
            newEdges = listOf(
                Triple("keanu", "matrixmovie", "WORKED_ON"),
                Triple("keanu", "johnwick", "WORKED_ON"),
            ),
        ),
        "delhi" to DeepLayer(
            newNodeIds = listOf("qutub", "lotustemple"),
            newEdges = listOf(
                Triple("delhi", "qutub", "LOCATED_IN"),
                Triple("delhi", "lotustemple", "LOCATED_IN"),
            ),
        ),
        "bh" to DeepLayer(
            newNodeIds = listOf("wormholes", "spacetime"),
            newEdges = listOf(
                Triple("bh", "wormholes", "RELATED_TO"),
                Triple("bh", "spacetime", "BASED_ON"),
            ),
        ),
    )

    /** Returns a deep layer for a node; generates a generic one if none authored. */
    fun get(nodeId: String, nodeTitle: String): DeepLayer {
        layers[nodeId]?.let { return it }
        // generic fallback: 2 concept nodes branching from this node
        val idA = "${nodeId}_deep_a"
        val idB = "${nodeId}_deep_b"
        return DeepLayer(
            newNodeIds = listOf(idA, idB),
            newEdges = listOf(
                Triple(nodeId, idA, "RELATED_TO"),
                Triple(nodeId, idB, "INFLUENCED"),
            ),
        )
    }

    /** Build actual Node objects from ids (title-cased placeholder with type inference). */
    fun nodesFor(ids: List<String>): List<Node> =
        ids.map { id ->
            Node(
                id = id,
                title = id.removePrefix("${id}_").split("_").joinToString(" ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                }.let { prettyTitle(it) },
                type = inferType(id),
                description = "Discovered while going deeper down the rabbit hole.",
            )
        }

    private fun prettyTitle(raw: String): String = when (raw.lowercase()) {
        "neuromancer" -> "Neuromancer"
        "sprawl" -> "The Sprawl Trilogy"
        "cyberspace" -> "Cyberspace"
        "hugo" -> "Hugo Award"
        "matrix" -> "The Matrix"
        "matrixmovie" -> "The Matrix"
        "johnwick" -> "John Wick"
        "bletchley" -> "Bletchley Park"
        "enigma2" -> "Enigma Codebreaking"
        "singularity2" -> "Technological Singularity"
        "agi" -> "Artificial General Intelligence"
        "ethics" -> "AI Ethics"
        "johnny" -> "Johnny Silverhand"
        "samurai" -> "Samurai (Band)"
        "qutub" -> "Qutub Minar"
        "lotustemple" -> "Lotus Temple"
        "wormholes" -> "Wormholes"
        "spacetime" -> "Spacetime Curvature"
        else -> raw.split("_").joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun inferType(id: String): NodeType = when {
        listOf("neuromancer", "hugo").any { id.contains(it) } -> NodeType.BOOK
        listOf("matrixmovie", "johnwick").any { id.contains(it) } -> NodeType.MOVIE
        listOf("johnny", "samurai").any { id.contains(it) } -> NodeType.PERSON
        listOf("bletchley").any { id.contains(it) } -> NodeType.PLACE
        listOf("qutub", "lotustemple").any { id.contains(it) } -> NodeType.PLACE
        listOf("enigma2").any { id.contains(it) } -> NodeType.EVENT
        listOf("agi", "ethics", "wormholes", "spacetime", "singularity2", "cyberspace",
               "sprawl", "matrix").any { id.contains(it) } -> NodeType.CONCEPT
        else -> NodeType.CONCEPT
    }
}
