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

    /** Links from a page — candidates for related nodes. */
    fun links(title: String, limit: Int = 20): List<String> {
        val url = "$API?action=query&format=json&titles=${enc(title)}" +
                "&prop=links&pllimit=$limit&plnamespace=0"
        val body = get(url)
        val pages = json.parseToJsonElement(body).jsonObject["query"]
            ?.jsonObject?.get("pages")?.jsonObject ?: return emptyList()
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
        json.decodeFromString<WikiSearchResponse>(body).query?.pages.values.orEmpty()
            .sortedBy { it.index ?: 999 }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
