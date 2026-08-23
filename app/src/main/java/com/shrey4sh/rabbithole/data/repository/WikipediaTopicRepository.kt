package com.shrey4sh.rabbithole.data.repository

import com.shrey4sh.rabbithole.data.mock.MockData
import com.shrey4sh.rabbithole.data.remote.AiRanker
import com.shrey4sh.rabbithole.data.remote.WikipediaApi
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.EntityNormalizer
import com.shrey4sh.rabbithole.domain.model.KnowledgeEntity
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
 * Curated knowledge pipeline:
 * query → EntityResolver → candidates → normalize → dedupe → AI rank → validate → graph.
 * Only entities with clean titles and validated relationships reach the UI.
 */
class WikipediaTopicRepository @Inject constructor(
    private val wiki: WikipediaApi,
    private val aiRanker: AiRanker,
) : TopicRepository {

    /** Disambiguation result surfaced to the UI before any graph is built. */
    data class Disambiguation(val query: String, val options: List<KnowledgeEntity>)

    override fun searchTopic(query: String): Flow<RabbitHole?> = flow {
        delay(500) // discovery animation stages rotate

        // --- Stage 1: entity resolution ---
        val resolver = EntityResolver(WikipediaApi())
        val resolution = resolver.resolve(query)
        val root = resolution.entity ?: run {
            emit(null); return@flow
        }

        val hole = runCatching { buildFromRoot(root) }.getOrNull() ?: MockData.search(query)
        if (hole != null) delay(700)
        emit(hole)
    }.flowOn(Dispatchers.IO)

    /** Resolve an explicitly chosen alternative (from the disambiguation sheet). */
    fun resolveChosen(entity: KnowledgeEntity): Flow<RabbitHole?> = flow {
        val hole = runCatching { buildFromRoot(entity) }.getOrNull()
        if (hole != null) delay(400)
        emit(hole)
    }.flowOn(Dispatchers.IO)

    override suspend fun randomTopic(): RabbitHole {
        val starters = listOf(
            "Black holes", "Cyberpunk 2077", "Delhi", "Enigma machine",
            "Joji", "Formula 1", "Byzantine Empire", "Marie Curie",
        )
        val topic = starters.random()
        val resolution = EntityResolver(WikipediaApi()).resolve(topic)
        return resolution.entity?.let { runCatching { buildFromRoot(it) }.getOrNull() }
            ?: MockData.random()
    }

    private suspend fun buildFromRoot(root: KnowledgeEntity): RabbitHole? = coroutineScope {
        val api = WikipediaApi()

        val summaryDeferred = async { runCatching { api.summary(root.canonicalTitle) }.getOrNull() }
        val leadDeferred = async { runCatching { api.leadLinks(root.canonicalTitle) }.getOrElse { emptyList() } }
        val allLinksDeferred = async { runCatching { api.allLinks(root.canonicalTitle) }.getOrElse { emptyList() } }
        val summary = summaryDeferred.await()

        // --- Stage 2: candidate normalization + curation ---
        val lead = leadDeferred.await()
        val rawPool = if (lead.size >= 15) lead else lead + allLinksDeferred.await()
        val linkTitles = curateCandidates(root.canonicalTitle, rawPool).take(24)

        // --- Stage 3: AI ranks the strongest relationships (evidence-bound) ---
        val ranked = aiRanker.rankConnections(root.canonicalTitle, summary?.extract, linkTitles)
            .take(10)

        val rootNode = Node(
            id = root.id,
            title = root.canonicalTitle,
            description = EntityNormalizer.cleanDescription(summary?.extract) ?: root.description ?: "",
            type = root.type,
            imageUrl = root.imageUrl ?: summary?.thumbnail?.source,
            sourceUrls = listOf(wikiUrl(root.canonicalTitle)),
        )

        // --- Stage 4: fetch summaries, normalize into clean entities ---
        val relatedNodes = ranked.map { r ->
            coroutineScope {
                async {
                    val title = EntityNormalizer.cleanTitle(r.title) ?: return@async null
                    if (title.equals(root.canonicalTitle, ignoreCase = true)) return@async null
                    val s = runCatching { api.summary(title) }.getOrNull()
                    Node(
                        id = "wiki:${title.hashCode()}",
                        title = title,
                        description = EntityNormalizer.cleanDescription(s?.extract) ?: r.reason,
                        type = guessType(title, s?.extract?.take(200)),
                        imageUrl = s?.thumbnail?.source,
                        sourceUrls = listOf(wikiUrl(title)),
                    )
                }
            }
        }.awaitAll().filterNotNull()

        // --- Stage 5: dedup by id AND normalized title; keep first (highest-ranked) ---
        val allRelated = relatedNodes
            .filter { it.id != rootNode.id }
            .distinctBy { it.id }
            .distinctBy { EntityNormalizer.dedupKey(it.title) }

        val edges = allRelated.map {
            Edge(
                id = "${rootNode.id}-${it.id}-RELATED_TO",
                sourceNodeId = rootNode.id,
                targetNodeId = it.id,
                relationship = ranked.find { r -> r.title.equals(it.title, true) }?.relationship ?: "RELATED_TO",
                sources = listOf(wikiUrl(root.canonicalTitle)),
            )
        }

        RabbitHole(
            id = root.canonicalTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
            rootNodeId = rootNode.id,
            nodes = listOf(rootNode) + allRelated,
            edges = edges,
            explorationPath = listOf(rootNode.title),
        )
    }

    private fun wikiUrl(title: String) =
        "https://en.wikipedia.org/wiki/${java.net.URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"

    /**
     * Heuristic relevance pre-scoring of raw wiki links — removes navigation/junk,
     * fragments and non-entity pages so the AI pool is already clean.
     */
    private fun curateCandidates(rootTitle: String, raw: List<String>): List<String> {
        val stop = setOf("a", "an", "the", "of", "in", "on", "and", "for", "to", "with", "by", "at")
        val rootTerms = rootTitle.lowercase().split(Regex("\\W+")).filter { it !in stop && it.length > 2 }.toSet()

        val disallow = listOf(
            "list of", "index of", "outline of", "timeline of", "category:", "template:",
            "portal:", "wikipedia:", "disambiguation", "(disambiguation)", "stub",
            "history of", "bibliography", "glossary", "appendix", "references", "external links",
        )

        data class Scored(val title: String, val score: Double)

        val scored = raw.asSequence()
            .distinct()
            .mapNotNull { EntityNormalizer.cleanTitle(it) }
            .filter { t -> disallow.none { t.lowercase().contains(it) } }
            .map { t ->
                val lower = t.lowercase()
                var s = 0.0
                val terms = lower.split(Regex("\\W+")).filter { it !in stop && it.length > 2 }.toSet()
                s += 2.0 * minOf((terms intersect rootTerms).size, 2)
                if (t.contains(' ')) s += 0.8
                if (t.firstOrNull()?.isDigit() == true) s -= 2.5
                if (t.length <= 6 && t == t.uppercase()) s -= 1.5
                if (t.count { it == '(' } > 1) s -= 1.0
                listOf("isbn", "identifier", "doi ", "pmid", "issn", "institute of",
                       "association", "international", "journal", "university", "press)",
                       "generation", "western", "opera")
                    .any { lower.contains(it) }.let { if (it) s -= 2.0 }
                listOf("fiction", "novel", "film", "game", "genre", "universe", "series",
                       "company", "studio", "director", "writer", "theory", "effect")
                    .any { lower.endsWith(it) || lower.endsWith(it + "s") }.let { if (it) s += 0.5 }
                Scored(t, s)
            }
            .filter { it.score > -1.0 }
            .sortedByDescending { it.score }
            .map { it.title }
            .toList()
        return scored
    }

    private fun guessType(title: String, description: String? = null): NodeType {
        val text = (title + " " + (description ?: "")).lowercase()
        return when {
            listOf("singer", "actor", "actress", "writer", "author", "musician", "footballer",
                   "politician", "scientist", "physicist", "composer", "director", "producer",
                   "artist", "poet", "novelist", "engineer who", "philosopher", "emperor",
                   "king of", "queen", "president", "prime minister", "born 19", "born 18").any { it in text } -> NodeType.PERSON
            listOf("city", "town", "village", "district", "country", "state in india",
                   "river", "mountain", "park", "airport", "fort", "temple", "stadium",
                   "capital", "province", "region", "island", "neighbourhood").any { it in text } -> NodeType.PLACE
            listOf("war", "battle", "treaty", "revolution", "massacre", "uprising",
                   "tournament", "championship", "ceremony", "protest", "attack", "expedition").any { it in text } -> NodeType.EVENT
            listOf("video game", "game developed", "rpg", "action-adventure game",
                   "platform game", "shooter game", "gaming").any { it in text } -> NodeType.GAME
            listOf("film", "movie", "directed by", "cinematic").any { it in text } -> NodeType.MOVIE
            listOf("song", "album", "single by", "band", "singer-songwriter", "record producer",
                   "soundtrack").any { it in text } -> NodeType.MUSIC
            listOf("company", "studio", "corporation", "organization", "agency", "label",
                   "founded in", "developer of").any { it in text } -> NodeType.ORGANIZATION
            listOf("software", "operating system", "network", "internet", "computer",
                   "machine", "engine", "application", "programming", "artificial intelligence",
                   "algorithm", "cryptocurrency", "website").any { it in text } -> NodeType.TECHNOLOGY
            listOf("novel", "book", "trilogy", "memoir", "written by").any { it in text } -> NodeType.BOOK
            else -> NodeType.CONCEPT
        }
    }
}

private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitAll(): List<T> =
    coroutineScope { map { it.await() } }
