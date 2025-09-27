package com.mp.web_searching.models

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
@Serializable
@JsonClass(generateAdapter = true)
data class SearchItem(
    val title: String,
    val url: String,
    val snippet: String
)

@Serializable
@JsonClass(generateAdapter = true)
data class SearchResponse(
    val query: String,
    val results: List<SearchItem>,
    val source: String = "duckduckgo_html",
    val elapsed_ms: Long
)
@Serializable
@JsonClass(generateAdapter = true)
data class PageSummary(
    val url: String,
    val summary: String
)