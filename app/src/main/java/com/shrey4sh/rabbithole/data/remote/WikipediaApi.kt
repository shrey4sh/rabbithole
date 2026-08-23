package com.shrey4sh.rabbithole.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wikipedia client: search + summary + links.
 * No Retrofit needed for these two simple GET endpoints; uses OkHttp directly
 * (Retrofit stays in the stack for future structured APIs like Wikidata SPARQL).
 */
class WikipediaApi(
    private val client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "RabbitHole/1.0 (Android; knowledge exploration)")
                    .build())
        }
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val BASE = "https://en.wikipedia.org"
        private const val API = "$BASE/w/api.php"
    }

    /** Full-text search returning candidate pages with descriptions + thumbnails. */
    fun search(query: String, limit: Int = 5): List<WikiPage> {
        val url = "$API?action=query&format=json&generator=search" +
                "&gsrsearch=${enc(query)}&gsrlimit=$limit" +
                "&prop=pageimages|description&piprop=thumbnail&pithumbsize=300"
        return parsePages(get(url))
    }

    /** Links from a page — candidates for related nodes. Fetches ALL link batches via continuation. */
    fun allLinks(title: String): List<String> {
        val out = mutableListOf<String>()
        var cont: String? = null
        repeat(4) { // up to ~2000 links
            val url = buildString {
                append("$API?action=query&format=json&titles=${enc(title)}" +
                        "&prop=links&pllimit=500&plnamespace=0")
                if (cont != null) append("&plcontinue=${enc(cont!!)}")
            }
            val body = runCatching { get(url) }.getOrElse { return out }
            val root = json.parseToJsonElement(body).jsonObject
            val pages = (root["query"] as? JsonObject)?.get("pages") as? JsonObject ?: return out
            out += pages.values.flatMap { p ->
                (p as? JsonObject)?.get("links")?.jsonArray?.mapNotNull {
                    (it as? JsonObject)?.get("title")?.jsonPrimitive?.content
                } ?: emptyList()
            }
            cont = ((root["continue"] as? JsonObject)?.get("plcontinue")
                as? kotlinx.serialization.json.JsonPrimitive)?.content
            if (cont == null) return out
        }
        return out
    }

    /**
     * Lead-section wikilinks — the highest-signal candidates.
     * Links appearing in the article intro are the ones Wikipedia editors consider
     * definitional ("Cyberpunk" → William Gibson, Neuromancer, Akira, Philip K. Dick),
     * unlike the alphabetical full-link dump where junk like "A.D. Vision" dominates.
     */
    fun leadLinks(title: String): List<String> {
        val body = runCatching {
            get("$API?action=query&format=json&prop=revisions&titles=${enc(title)}" +
                    "&rvprop=content&rvslots=main&rvsection=0")
        }.getOrElse { return emptyList() }
        val pages = (json.parseToJsonElement(body).jsonObject["query"]
            as? JsonObject)?.get("pages") as? JsonObject ?: return emptyList()
        return pages.values.firstOrNull()?.let { p ->
            ((p as? JsonObject)?.get("revisions")?.jsonArray?.firstOrNull() as? JsonObject)
                ?.get("slots")?.jsonObject?.get("main")?.jsonObject
                ?.get("*")?.jsonPrimitive?.content
        }?.let { wikitext ->
            Regex("\\[\\[([^\\]|#]+)").findAll(wikitext).mapNotNull { m ->
                val l = m.groupValues[1].trim()
                val skipPrefixes = listOf("File:", "Image:", "Category:", "wikt:",
                                          "Wiktionary:", "s:", "m:")
                if (l.isBlank() || skipPrefixes.any { l.startsWith(it) }) null else l
            }.toList().take(80)
        } ?: emptyList()
    }

    /** Links from a page — single batch. */
    fun links(title: String, limit: Int = 500): List<String> {
        val url = "$API?action=query&format=json&titles=${enc(title)}" +
                "&prop=links&pllimit=$limit&plnamespace=0"
        val body = get(url)
        val root = json.parseToJsonElement(body).jsonObject
        val pages = (root["query"] as? JsonObject)?.get("pages") as? JsonObject
            ?: return emptyList()
        return pages.values.flatMap { p ->
            (p as? JsonObject)?.get("links")?.jsonArray?.mapNotNull {
                (it as? JsonObject)?.get("title")?.jsonPrimitive?.content
            } ?: emptyList()
        }
    }

    /** REST summary: extract + thumbnail + page URL. */
    fun summary(title: String): WikiSummaryResponse? {
        val body = runCatching { get("$BASE/api/rest_v1/page/summary/${enc(title)}") }
            .getOrElse { return null }
        return json.decodeFromString<WikiSummaryResponse>(body)
    }

    private fun get(url: String): String {
        val req = okhttp3.Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }

    private fun parsePages(body: String): List<WikiPage> =
        json.decodeFromString<WikiSearchResponse>(body).query
            ?.pages?.values.orEmpty().sortedBy { it.index ?: 999 }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
