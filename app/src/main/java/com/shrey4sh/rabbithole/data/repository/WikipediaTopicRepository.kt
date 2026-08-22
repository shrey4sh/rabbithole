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
class WikipediaTopicRepository @Inject constructor(
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
            description = rootPage.description ?: "",
            type = guessType(rootPage.title, summary?.extract?.take(400) ?: rootPage.description),
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
                        type = guessType(r.title, s?.extract?.take(200)),
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

    private fun guessType(title: String, description: String? = null): NodeType {
        val text = (title + " " + (description ?: "")).lowercase()
        return when {
            // people: occupations & roles
            listOf("singer", "actor", "actress", "writer", "author", "musician", "footballer",
                   "politician", "scientist", "physicist", "composer", "director", "producer",
                   "artist", "poet", "novelist", "engineer who", "philosopher", "emperor",
                   "king of", "queen", "president", "prime minister", "born 19", "born 18").any { it in text } -> NodeType.PERSON
            // places
            listOf("city", "town", "village", "district", "country", "state in india",
                   "river", "mountain", "park", "airport", "fort", "temple", "stadium",
                   "capital", "province", "region", "island", "neighbourhood").any { it in text } -> NodeType.PLACE
            // events
            listOf("war", "battle", "treaty", "revolution", "massacre", "uprising",
                   "tournament", "championship", "ceremony", "protest", "attack", "expedition").any { it in text } -> NodeType.EVENT
            // games
            listOf("video game", "game developed", "rpg", "action-adventure game",
                   "platform game", "shooter game", "gaming").any { it in text } -> NodeType.GAME
            // movies
            listOf("film", "movie", "directed by", "cinematic").any { it in text } -> NodeType.MOVIE
            // music
            listOf("song", "album", "single by", "band", "singer-songwriter", "record producer",
                   "soundtrack").any { it in text } -> NodeType.MUSIC
            // organizations
            listOf("company", "studio", "corporation", "organization", "agency", "label",
                   "founded in", "developer of").any { it in text } -> NodeType.ORGANIZATION
            // tech
            listOf("software", "operating system", "network", "internet", "computer",
                   "machine", "engine", "application", "programming", "artificial intelligence",
                   "algorithm", "cryptocurrency", "website").any { it in text } -> NodeType.TECHNOLOGY
            // books
            listOf("novel", "book", "trilogy", "memoir", "written by").any { it in text } -> NodeType.BOOK
            else -> NodeType.CONCEPT
        }
    }
}

private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitAll(): List<T> =
    coroutineScope { map { it.await() } }
