package com.shrey4sh.rabbithole.data.repository

import com.shrey4sh.rabbithole.data.mock.MockData
import com.shrey4sh.rabbithole.data.remote.WikipediaApi
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Phase 5: real Wikipedia-backed repository with graceful mock fallback.
 * Search → root summary + related links → graph. Falls back to mocks offline.
 */
class WikipediaTopicRepository @Inject constructor(
    private val wiki: WikipediaApi,
) : TopicRepository {

    override fun searchTopic(query: String): Flow<RabbitHole?> = flow {
        delay(500) // let the discovery animation breathe (stages rotate)
        val hole = runCatching { buildFromWikipedia(query) }.getOrNull()
            ?: MockData.search(query)
        if (hole != null) delay(700) // remaining discovery stages
        emit(hole)
    }.flowOn(Dispatchers.IO)

    override suspend fun randomTopic(): RabbitHole {
        // pick from curated interesting starters, then real data
        val starters = listOf(
            "An abandoned city", "Black holes", "Cyberpunk 2077", "Delhi",
            "Enigma machine", "Joji", "Formula 1",
        )
        val topic = starters.random()
        return runCatching { buildFromWikipedia(topic) }.getOrNull()
            ?: MockData.random()
    }

    private suspend fun buildFromWikipedia(query: String): RabbitHole? {
        val api = WikipediaApi()
        val candidates = Dispatchers.IO.run { api.search(query, limit = 5) }
        val rootPage = candidates.firstOrNull() ?: return null

        val rootNode = Node(
            id = "wiki:${rootPage.pageid}",
            title = rootPage.title,
            description = rootPage.description ?: "",
            type = NodeType.CONCEPT,
            imageUrl = rootPage.thumbnail?.source,
            sourceUrls = listOf(wikiUrl(rootPage.title)),
        )

        // summary for richer description
        val summary = runCatching { api.summary(rootPage.title) }.getOrNull()

        // related nodes: page links, filtered to meaningful ones, take top N
        val linkTitles = runCatching { api.links(rootPage.title, limit = 30) }
            .getOrElse { emptyList() }
            .filter { it.length in 4..40 && !it.contains("list", true) }
            .distinct()
            .take(9)

        val relatedNodes = linkTitles.mapIndexed { i, title ->
            val s = runCatching { api.summary(title) }.getOrNull()
            Node(
                id = "wiki:${title.hashCode()}",
                title = title,
                description = s?.extract?.take(160) ?: "",
                type = guessType(title),
                imageUrl = s?.thumbnail?.source,
                sourceUrls = listOf(wikiUrl(title)),
            )
        }

        val allNodes = listOf(rootNode) + relatedNodes
        val edges = relatedNodes.map {
            com.shrey4sh.rabbithole.domain.model.Edge(
                id = "${rootNode.id}-${it.id}-RELATED_TO",
                sourceNodeId = rootNode.id,
                targetNodeId = it.id,
                relationship = "RELATED_TO",
                sources = listOf(wikiUrl(rootPage.title)),
            )
        }

        return RabbitHole(
            id = query.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            rootNodeId = rootNode.id,
            nodes = allNodes,
            edges = edges,
            explorationPath = listOf(rootNode.title),
        )
    }

    private fun wikiUrl(title: String) =
        "https://en.wikipedia.org/wiki/${java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"

    /** Lightweight heuristic type inference until Wikidata instance-of data is wired. */
    private fun guessType(title: String): NodeType {
        val t = title.lowercase()
        return when {
            listOf("university", "city", "river", "mountain", "park", "airport").any { t.contains(it) } -> NodeType.PLACE
            listOf("war", "treaty", "revolution", "empire", "battle").any { t.contains(it) } -> NodeType.EVENT
            listOf("software", "engine", "system", "network", "api").any { t.contains(it) } -> NodeType.TECHNOLOGY
            else -> NodeType.CONCEPT
        }
    }
}
