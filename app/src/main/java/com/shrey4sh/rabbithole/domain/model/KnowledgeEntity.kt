package com.shrey4sh.rabbithole.domain.model

/**
 * Canonical internal entity — the ONLY thing the graph pipeline consumes.
 * Raw API objects never reach the UI; they are normalized into this first.
 */
data class KnowledgeEntity(
    val id: String,
    val canonicalTitle: String,
    val description: String? = null,
    val type: NodeType = NodeType.CONCEPT,
    val imageUrl: String? = null,
    val aliases: List<String> = emptyList(),
    val sourceUrls: List<String> = emptyList(),
)

/** A validated relationship between two canonical entities. */
data class KnowledgeRelationship(
    val sourceId: String,
    val targetId: String,
    val relationshipType: String,
    val description: String? = null,
    val confidence: Float = 1f,
    val sourceUrls: List<String> = emptyList(),
)

object EntityNormalizer {

    private val ID_PATTERNS = listOf(
        Regex("""^wiki:\d+$""", RegexOption.IGNORE_CASE),
        Regex("""^Q\d+$"""),
        Regex("""^P\d+$"""),
        Regex("""^\d+$"""),
        Regex("""^(page|entity|item)[-_]?\d+$""", RegexOption.IGNORE_CASE),
    )

    private val JUNK_TITLES = listOf(
        "disambiguation", "list of", "index of", "outline of", "category:",
        "template:", "portal:", "wikipedia:", "stub", "(identifier)",
    )

    /** Returns a clean human-readable title, or null if the entity must be discarded. */
    fun cleanTitle(raw: String?): String? {
        var t = raw?.trim() ?: return null
        // strip HTML tags & markup artifacts
        t = t.replace(Regex("<[^>]*>"), "")
        t = t.replace(Regex("""\s+"""), " ")
        // collapse duplicate punctuation
        t = t.replace(Regex("""([.,;:!?)])\1+"""), "$1")
        // hard-reject anything that is or looks like an internal identifier
        if (ID_PATTERNS.any { it.containsMatchIn(t) }) return null
        if (t.length < 2 || t.length > 80) return null
        if (!t.any { it.isLetter() }) return null
        // reject junk pages outright
        val lower = t.lowercase()
        if (JUNK_TITLES.any { lower.contains(it) }) return null
        return t
    }

    /** Human-readable 1-2 sentence description with citations/markup stripped. */
    fun cleanDescription(raw: String?): String? {
        var d = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        d = d.replace(Regex("<[^>]*>"), "")
        d = d.replace(Regex("""\[[0-9]+\]"""), "")      // [1][2] citations
        d = d.replace(Regex("""\[(citation|note \d+|e)\]"""), "")
        d = d.replace(Regex("""\s*\^\s*[a-zA-Z]{2,}\b.*$"""), "") // trailing metadata
        d = d.replace(Regex("""\s+"""), " ")
        return d.takeIf { it.length >= 20 }?.take(220)
    }

    /**
     * Normalize a raw title into a dedup key: lowercase, punctuation stripped,
     * parenthetical qualifiers removed. "Flicker (light)" and "flicker" collide.
     */
    fun dedupKey(title: String): String =
        title.lowercase()
            .replace(Regex("""\s*\(.*?\)"""), "")
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()

    /** Disambiguation suffix preserved for display, e.g. "Game (1997 film)". */
    fun disambiguator(title: String): String? =
        Regex("""\((.+?)\)$""").find(title)?.groupValues?.get(1)
}
