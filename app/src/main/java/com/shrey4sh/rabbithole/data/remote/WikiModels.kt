package com.shrey4sh.rabbithole.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class WikiSearchResponse(
    val query: WikiQuery? = null,
)

@Serializable
data class WikiQuery(
    val pages: Map<String, WikiPage>? = null,
)

@Serializable
data class WikiPage(
    val pageid: Long,
    val title: String,
    val index: Int? = null,
    val description: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val links: List<WikiLink>? = null,
)

@Serializable
data class WikiThumbnail(
    val source: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class WikiLink(
    val ns: Int,
    val title: String,
)

@Serializable
data class WikiSummaryResponse(
    val title: String? = null,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val content_urls: WikiContentUrls? = null,
)

@Serializable
data class WikiContentUrls(
    val page: WikiPageUrl? = null,
)

@Serializable
data class WikiPageUrl(
    val page: String? = null,
)
