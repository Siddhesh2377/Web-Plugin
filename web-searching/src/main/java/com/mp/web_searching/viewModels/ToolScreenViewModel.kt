package com.mp.web_searching.viewModels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.mp.web_searching.Common.MAX_HTML_CHARS
import com.mp.web_searching.Common.TAG
import com.mp.web_searching.Common.UA
import com.mp.web_searching.models.PageSummary
import com.mp.web_searching.models.SearchItem
import com.mp.web_searching.models.SearchResponse
import com.mp.web_searching.models.UiState
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class ToolScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    val searchAdapter: JsonAdapter<SearchResponse> by lazy {
        moshi.adapter(SearchResponse::class.java)
    }

    val pageAdapter: JsonAdapter<PageSummary> by lazy {
        moshi.adapter(PageSummary::class.java)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS).followRedirects(true).followSslRedirects(true)
            .build()
    }

    // ------------------ State Updaters ------------------

    fun setLoading(label: String) = _uiState.update { UiState.Loading(label) }
    fun setSearchSuccess(data: SearchResponse) = _uiState.update { UiState.SearchSuccess(data) }
    fun setFetchSuccess(data: PageSummary) = _uiState.update { UiState.FetchSuccess(data) }
    fun setError(msg: String) = _uiState.update { UiState.Error(msg) }

    // ------------------ Networking ------------------

    suspend fun searchWeb(query: String, limit: Int = 5): String = withContext(Dispatchers.IO) {
        require(query.isNotBlank()) { "Query must not be blank" }

        val started = System.currentTimeMillis()
        Log.d(TAG, "searchWeb() q='$query', limit=$limit")

        val url =
            "https://duckduckgo.com/html/".toUri().buildUpon().appendQueryParameter("q", query)
                .appendQueryParameter("kl", "in-en").appendQueryParameter("ia", "web").build()
                .toString()

        val req = Request.Builder().url(url).header("User-Agent", UA)
            .header("Accept-Language", "en-US,en;q=0.9").get().build()

        val body = client.newCall(req).execute().use { resp ->
            ensureSuccess(resp)
            resp.body.string().take(MAX_HTML_CHARS)
        }

        val doc: Document = Jsoup.parse(body)
        val resultEls =
            doc.select("div.result, div.results_links, div.result__body, article[data-nrn]")

        val items = resultEls.mapNotNull { el ->
            if (el == null) return@mapNotNull null

            val titleEl =
                el.selectFirst("a.result__a, a.result__title, a[data-testid=ResultTitle], h2 a")
                    ?: return@mapNotNull null
            val rawHref = titleEl.attr("href").trim()
            val title = titleEl.text().trim()
            val snippet = el.selectFirst(
                "a.result__snippet, div.result__snippet, div.result__snippet.js-result-snippet, .result__snippet, p"
            )?.text()?.trim().orEmpty()

            val cleanedUrl = cleanDuckDuckGoUrl(rawHref)
            if (title.isNotEmpty() && cleanedUrl.isNotEmpty()) {
                SearchItem(
                    title = title, url = cleanedUrl, snippet = snippet.take(400)
                )
            } else null
        }.take(limit)

        val elapsed = System.currentTimeMillis() - started
        searchAdapter.toJson(SearchResponse(query = query, results = items, elapsed_ms = elapsed))
    }

    suspend fun fetchAndSummarize(url: String, maxChars: Int = 1200): String =
        withContext(Dispatchers.IO) {
            //require(url.startsWith("http")) { "URL must start with http/https" }

            val req = Request.Builder().url(url).header("User-Agent", UA)
                .header("Accept-Language", "en-US,en;q=0.9").get().build()

            val html = client.newCall(req).execute().use { resp ->
                ensureSuccess(resp)
                resp.body.string().orEmpty().take(MAX_HTML_CHARS)
            }

            val doc = Jsoup.parse(html)
            val main = doc.selectFirst("article, main") ?: doc.body()
            val text = main.text().trim()
            val compact = text.replace(Regex("\\s+"), " ").take(maxChars)

            pageAdapter.toJson(PageSummary(url = url, summary = compact))
        }

    // ------------------ Helpers ------------------

    fun cleanDuckDuckGoUrl(raw: String): String = try {
        val uri = raw.toUri()
        if (uri.path?.startsWith("/l/") == true) {
            val uddg = uri.getQueryParameter("uddg")
            if (!uddg.isNullOrBlank()) URLDecoder.decode(uddg, "UTF-8") else raw
        } else {
            when {
                raw.startsWith("http") -> raw
                raw.startsWith("//") -> "https:$raw"
                else -> raw
            }
        }
    } catch (_: Exception) {
        raw
    }

    private fun ensureSuccess(resp: Response) {
        if (!resp.isSuccessful) {
            throw IllegalStateException("HTTP ${resp.code}: ${resp.message}")
        }
    }

    fun errorJson(tool: String, message: String): String =
        """{"tool":"${tool.replace("\"", "\\\"")}","error":"${message.replace("\"", "\\\"")}"}"""

    fun openLink(ctx: Context, url: String) {
        try {
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW, url.toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
            // ignored
        }
    }

    fun runTool(
        ioScope: CoroutineScope, toolName: String, args: JSONObject, callback: (result: Any) -> Unit
    ) {
        when (toolName) {
            "searchWeb" -> {
                val query = args.optString("query").orEmpty()
                val limit = args.optInt("limit", 5).coerceIn(1, 25)
                Log.d(TAG, "runTool(searchWeb): q='$query', limit=$limit")

                setLoading("Searching…")
                ioScope.launch {
                    try {
                        val json = withTimeout(15_000L) { searchWeb(query, limit) }
                        // parse → UI state shows “Search complete”
                        searchAdapter.fromJson(json)?.let { setSearchSuccess(it) }
                            ?: run { setError("Parse error") }
                        callback(JSONObject().apply {
                            put("name", "Web-Searching")
                            put("type", "text")
                            put("output", json)
                        }) // keep raw json for caller
                    } catch (t: Throwable) {
                        val msg = "searchWeb failed: ${t.message ?: t::class.java.simpleName}"
                        Log.w(TAG, msg, t)
                        setError(msg)
                        callback(errorJson("searchWeb", msg))
                    }
                }
            }

            "fetchPage" -> {
                val url = args.optString("url").orEmpty()
                Log.d(TAG, "runTool(fetchPage): url='$url'")

                setLoading("Fetching page…")
                ioScope.launch {
                    try {
                        val res = withTimeout(15_000L) { fetchAndSummarize(url) }
                        pageAdapter.fromJson(res)?.let { setFetchSuccess(it) }
                            ?: run { setError("Parse error") }
                        callback(JSONObject().apply {
                            put("name", "Web-Searching")
                            put("type", "text")
                            put("output", res)
                        })
                    } catch (t: Throwable) {
                        val msg = "fetchPage failed: ${t.message ?: t::class.java.simpleName}"
                        Log.w(TAG, msg, t)
                        setError(msg)
                        callback(errorJson("fetchPage", msg))
                    }
                }
            }

            else -> {
                val msg = "Unknown tool: $toolName"
                Log.w(TAG, msg)
                setError(msg)
                callback(errorJson(toolName, msg))
            }
        }
    }
}