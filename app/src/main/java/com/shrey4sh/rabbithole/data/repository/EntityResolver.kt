package com.shrey4sh.rabbithole.data.repository

import com.shrey4sh.rabbithole.data.remote.WikipediaApi
import com.shrey4sh.rabbithole.domain.model.KnowledgeEntity
import com.shrey4sh.rabbithole.domain.model.EntityNormalizer

/**
 * Entity resolution stage.
 * Decides whether a query is unambiguous (resolve directly) or ambiguous
 * (return 3-5 clean candidate meanings for the disambiguation UI).
 * Disambiguation pages themselves are NEVER candidates for the root.
 */
class EntityResolver(private val api: WikipediaApi) {

    data class Resolution(
        val entity: KnowledgeEntity?,          // non-null when resolved directly
        val alternatives: List<KnowledgeEntity> = emptyList(), // non-empty when ambiguous
    )

    /** Heuristic: multiword/specific queries resolve directly without a network round-trip. */
    private fun looksSpecific(query: String): Boolean {
        val q = query.trim()
        if (q.split(Regex("\\s+")).size >= 2 && !q.lowercase().contains("game")) return true
        // digits in title = specific work ("Cyberpunk 2077", "F1 2010")
        if (q.any { it.isDigit() }) return true
        return false
    }

    suspend fun resolve(query: String): Resolution {
        val results = runCatching { api.search(query.trim(), limit = 8) }
            .getOrElse { return Resolution(null) }

        // Normalize + drop junk (disambiguation pages, raw IDs, stubs)
        val entities = results.mapNotNull { page ->
            val title = EntityNormalizer.cleanTitle(page.title) ?: return@mapNotNull null
            val isDisambiguation = page.description?.lowercase()?.contains("disambiguation") == true ||
                    title.endsWith("(disambiguation)")
            if (isDisambiguation) return@mapNotNull null
            KnowledgeEntity(
                id = "wiki:${page.pageid}",
                canonicalTitle = title,
                description = EntityNormalizer.cleanDescription(page.description),
                aliases = listOf(EntityNormalizer.dedupKey(title)),
            )
        }

        val exact = entities.firstOrNull {
            it.canonicalTitle.equals(query.trim(), ignoreCase = true)
        }

        // Unambiguous signals:
        //  - exactly one strong candidate
        //  - an exact-title match whose description confirms it's a real article
        //  - the query itself looks specific (multiword, has digits)
        val top = entities.firstOrNull()
        val ambiguous = when {
            entities.isEmpty() -> false
            entities.size == 1 -> false
            exact != null && looksSpecific(query) -> false
            looksSpecific(query) && exact != null -> false
            // multiple distinct titles and no confident exact match → ask the user
            entities.size >= 3 && exact == null -> true
            else -> false
        }

        if (ambiguous) {
            return Resolution(entity = null, alternatives = entities.take(5))
        }

        val chosen = exact ?: top ?: return Resolution(null)
        return Resolution(entity = enrich(chosen))
    }

    /** Fetch summary to give the root a proper description + thumbnail. */
    private suspend fun enrich(e: KnowledgeEntity): KnowledgeEntity {
        val s = runCatching { apiWikipedia().summary(e.canonicalTitle) }.getOrNull() ?: return e
        return e.copy(
            description = EntityNormalizer.cleanDescription(s.extract) ?: e.description,
            imageUrl = s.thumbnail?.source ?: e.imageUrl,
        )
    }

    private fun apiWikipedia() = WikipediaApi()
}
