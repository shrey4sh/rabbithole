package com.shrey4sh.rabbithole.data.repository

import com.shrey4sh.rabbithole.data.mock.MockData
import com.shrey4sh.rabbithole.data.remote.AiRanker
import com.shrey4sh.rabbithole.data.remote.WikipediaApi
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.NodeType
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Phase 5+6: Wikipedia retrieval + AI ranking, with graceful mock fallback offline.
 * AI reasons over retrieved candidates only — never invents facts.
 */
class WikipediaTopicRepository constructor(
    private val wiki: WikipediaApi,
    private val aiRanker: AiRanker,
) : TopicRepository {

    override fun searchTopic(query: String): Flow<RabbitHole?> = flow {
        delay(500) // discovery animation stages rotate
        val hole = runCatching { buildFromWikipedia(query) }.getOrNull()
            ?: MockData.search(query)
        if (hole != null) delay(700)
        emit(hole)
    }.flowOn(Dispatchers.IO)

    override suspend fun randomTopic(): RabbitHole {
        val starters = listOf(
            "Black holes", "Cyberpunk 2077", "Delhi", "Enigma machine",
            "Joji", "Formula 1", "Byzantine Empire", "Marie Curie",
        )
        val topic = starters.random()
        return runCatching { buildFromWikipedia(topic) }.getOrNull()
            ?: MockData.random()
    }

    private suspend fun buildFromWikipedia(query: String): RabbitHole? = coroutineScope {
        val api = WikipediaApi()

        // 1. find root article
        val candidates = async { api.search(query, limit = 5) }
        val rootPage = candidates.await().firstOrNull() ?: return@coroutineScope null
        val rootId = "wiki:${rootPage.pageid}"

        // 2. parallel: summary + links + AI ranking of links
        val summaryDeferred = async { runCatching { api.summary(rootPage.title) }.getOrNull() }
        val linksDeferred = async { runCatching { api.links(rootPage.title, limit = 40) }.getOrElse { emptyList() } }
        val summary = summaryDeferred.await()
        val linkTitles = linksDeferred.await()
            .filter { it.length in 4..40 && !it.contains("list", true) }
            .distinct()
            .take(20)

        // 3. AI ranks + labels the connections (evidence: only candidate titles allowed)
        val ranked = aiRanker.rankConnections(rootPage.title, summary?.extract, linkTitles)

        val rootNode = Node(
            id = rootId,
            title = rootPage.title,
            description = rootPage.description ?: summary?.extract?.take(200) ?: "",
            type = NodeType.CONCEPT,
            imageUrl = rootPage.thumbnail?.source,
            sourceUrls = listOf(wikiUrl(rootPage.title)),
        )

        // 4. fetch summaries for ranked nodes (parallel, capped)
        val relatedNodes = ranked.map { r ->
            coroutineScope {
                async {
                    val s = runCatching { api.summary(r.title) }.getOrNull()
                    Node(
                        id = "wiki:${r.title.hashCode()}",
                        title = r.title,
                        description = s?.extract?.take(160) ?: r.reason,
                        type = guessType(r.title),
                        imageUrl = s?.thumbnail?.source,
                        sourceUrls = listOf(wikiUrl(r.title)),
                    )
                }
            }
        }.awaitAll()

        // dedupe by id, drop self
        val allRelated = relatedNodes.filter { it.id != rootNode.id }.distinctBy { it.id }
        val edges = allRelated.map {
            Edge(
                id = "${rootNode.id}-${it.id}-RELATED_TO",
                sourceNodeId = rootNode.id,
                targetNodeId = it.id,
                relationship = ranked.find { r -> r.title == it.title }?.relationship ?: "RELATED_TO",
                sources = listOf(wikiUrl(rootPage.title)),
            )
        }

        RabbitHole(
            id = query.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            rootNodeId = rootNode.id,
            nodes = listOf(rootNode) + allRelated,
            edges = edges,
            explorationPath = listOf(rootNode.title),
        )
    }

    private fun wikiUrl(title: String) =
        "https://en.wikipedia.org/wiki/${java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"

    private fun guessType(title: String): NodeType {
        val t = title.lowercase()
        return when {
            listOf("university", "city", "river", "mountain", "park", "airport", "fort", "temple").any { t.contains(it) } -> NodeType.PLACE
            listOf("war", "treaty", "revolution", "empire", "battle", "cup").any { t.contains(it) } -> NodeType.EVENT
            listOf("software", "engine", "system", "network", "telescope", "machine").any { t.contains(it) } -> NodeType.TECHNOLOGY
            listOf("band", "singer", "album").any { t.contains(it) } -> NodeType.MUSIC
            else -> NodeType.CONCEPT
        }
    }
}

private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitAll(): List<T> =
    coroutineScope { map { it.await() } }
