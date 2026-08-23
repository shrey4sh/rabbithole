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

        // 2. parallel: summary + lead-section wikilinks (high-signal) + full link pool (backup)
        val summaryDeferred = async { runCatching { api.summary(rootPage.title) }.getOrNull() }
        val leadDeferred = async { runCatching { api.leadLinks(rootPage.title) }.getOrElse { emptyList() } }
        val allLinksDeferred = async { runCatching { api.allLinks(rootPage.title) }.getOrElse { emptyList() } }
        val summary = summaryDeferred.await()

        // 3. Curator stage A: normalize + filter weak/irrelevant candidates BEFORE any AI call.
        //    Lead links are definitional; the full pool fills in if the intro is sparse.
        val lead = leadDeferred.await()
        val rawPool = if (lead.size >= 15) lead else lead + allLinksDeferred.await()
        val linkTitles = curateCandidates(rootPage.title, rawPool)
            .take(24)

        // 4. AI ranks + labels the connections (evidence: only candidate titles allowed)
        val ranked = aiRanker.rankConnections(rootPage.title, summary?.extract, linkTitles)
            .take(10) // depth-1 graph: ~10 strong nodes, never a wall of search results

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
                    // never render a raw identifier as a title; drop candidates without one
                    val safeTitle = r.title.trim()
                    if (safeTitle.isEmpty() || safeTitle.all { it.isDigit() }) return@async null
                    Node(
                        id = "wiki:${safeTitle.hashCode()}",
                        title = safeTitle,
                        description = s?.extract?.take(160) ?: r.reason,
                        type = guessType(safeTitle, s?.extract?.take(200)),
                        imageUrl = s?.thumbnail?.source,
                        sourceUrls = listOf(wikiUrl(safeTitle)),
                    ) as Node?
                }
            }
        }.awaitAll().filterNotNull()

        // dedupe by id AND by normalized title (Flicker / flicker / Flicker (light) collapse
        // to one node unless Wikipedia summaries prove they're distinct pages)
        val allRelated = relatedNodes
            .filter { it.id != rootNode.id }
            .distinctBy { it.id }
            .distinctBy { it.title.lowercase().replace(Regex("\\s*\\(.*?\\)"), "").trim() }
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

    /**
     * Curator stage A — normalize + heuristic relevance pre-scoring of raw wiki links.
     * Removes navigation/junk pages, penalizes obscure fragments, boosts candidates that
     * share a meaningful term with the root or are strong entity titles. The AI then
     * ranks the surviving pool; this stage just guarantees the pool isn't alphabetical junk.
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
            .filter { t -> t.length in 4..60 }
            .filter { t -> disallow.none { t.lowercase().contains(it) } }
            .map { t ->
                val lower = t.lowercase()
                var s = 0.0
                // shared terms with root: signal, but capped — otherwise every
                // "... in Cyberpunk" page outranks William Gibson / Blade Runner
                val terms = lower.split(Regex("\\W+")).filter { it !in stop && it.length > 2 }.toSet()
                s += 2.0 * minOf((terms intersect rootTerms).size, 2)
                // proper multiword entity titles tend to be real subjects
                if (t.contains(' ')) s += 0.8
                // penalize leading numerals / fragments like "1. Outside", "3D film"
                if (t.firstOrNull()?.isDigit() == true) s -= 2.5
                // penalize all-caps acronyms and single obscure words
                if (t.length <= 6 && t == t.uppercase()) s -= 1.5
                // penalize parenthetical-heavy titles (disambiguation leftovers)
                if (t.count { it == '(' } > 1) s -= 1.0
                // penalize generic / non-entity titles that make poor graph nodes
                listOf("isbn", "identifier", "doi ", "pmid", "issn", "institute of",
                       "association", "international", "journal", "university", "press)",
                       "generation", "western", "opera")
                    .any { lower.contains(it) }.let { if (it) s -= 2.0 }
                // small boost for genre-defining keywords relative to any root
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
