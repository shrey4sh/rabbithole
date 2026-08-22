package com.shrey4sh.rabbithole.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * AI layer (Phase 6). Reasons OVER retrieved Wikipedia data — never invents facts.
 * Ranks candidate links by interestingness, labels relationships.
 * All output must reference candidates provided in the prompt.
 */
class AiRanker(
    private val apiKey: String,
    private val model: String = "stealth/ox-alpha",
) {
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(20))
        .readTimeout(java.time.Duration.ofSeconds(60))
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class RankedLink(val title: String, val relationship: String, val reason: String)

    companion object {
        private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val SYSTEM =
            "You are a knowledge-graph curator. Rank connections between topics. " +
            "Use only candidate titles given; never invent new entities. " +
            "Respond with ONLY a JSON array."
    }

    /**
     * Given root topic + candidate titles (retrieved from Wikipedia), returns ranked
     * links with semantic relationship labels + one-line reasons.
     * Falls back to heuristic ranking on any failure so the app works offline/no-key.
     */
    suspend fun rankConnections(
        rootTitle: String,
        rootSummary: String?,
        candidates: List<String>,
    ): List<RankedLink> {
        if (candidates.isEmpty() || apiKey.isBlank()) return fallbackRank(candidates)

        val numbered = candidates.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n")
        val summaryLine = rootSummary?.let { "SUMMARY: ${it.take(400)}\n" } ?: ""
        val userPrompt = "ROOT TOPIC: $rootTitle\n${summaryLine}\n" +
                "CANDIDATES (use ONLY these):\n$numbered\n\n" +
                "For each candidate classify the relationship to the root using ONE of: " +
                "CREATED_BY, INSPIRED_BY, LOCATED_IN, MEMBER_OF, INFLUENCED, SAME_GENRE, " +
                "OCCURRED_IN, WORKED_ON, BASED_ON, RELATED_TO.\n" +
                "Pick the 8 most interesting and diverse. Respond ONLY with JSON array: " +
                """[{"title":"<exact title>","relationship":"<LABEL>","reason":"<max 15 words>"}]"""

        return runCatching {
            withContext(Dispatchers.IO) {
                val payload = buildString {
                    append("{\"model\":\"").append(model).append("\",\"temperature\":0.3,\"messages\":[")
                    append("{\"role\":\"system\",\"content\":").append(SYSTEM.toJsonStr()).append("},")
                    append("{\"role\":\"user\",\"content\":").append(userPrompt.toJsonStr()).append("}]}")
                }
                val req = okhttp3.Request.Builder()
                    .url(ENDPOINT)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody())
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("AI HTTP ${resp.code}")
                    val text = resp.body?.string() ?: throw RuntimeException("empty response")
                    val content = json.parseToJsonElement(text).jsonObject["choices"]
                        ?.jsonArray?.firstOrNull()?.jsonObject?.get("message")
                        ?.jsonObject?.get("content")?.jsonPrimitive?.content
                        ?: throw RuntimeException("no AI content")
                    parseRanked(content, candidates)
                }
            }
        }.getOrElse { fallbackRank(candidates) }
    }

    private fun parseRanked(content: String, candidates: List<String>): List<RankedLink> {
        val cleaned = content.trim().removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val arr = json.parseToJsonElement(cleaned).jsonArray
        return arr.mapNotNull { el ->
            val o = el.jsonObject
            val title = o["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val match = candidates.find { it.equals(title, ignoreCase = true) } ?: return@mapNotNull null
            RankedLink(
                title = match,
                relationship = o["relationship"]?.jsonPrimitive?.content ?: "RELATED_TO",
                reason = o["reason"]?.jsonPrimitive?.content ?: "",
            )
        }
    }

    /** Deterministic fallback when AI is unavailable — keeps app functional. */
    private fun fallbackRank(candidates: List<String>): List<RankedLink> =
        candidates.take(8).map { RankedLink(it, "RELATED_TO", "") }

    private fun String.toJsonStr(): String {
        val sb = StringBuilder("\"")
        for (ch in this) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> {}
                '\t' -> sb.append("\\t")
                else -> if (ch.code >= 0x20) sb.append(ch)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun String.toRequestBody(): okhttp3.RequestBody =
        okhttp3.RequestBody.create(null, this)
}
