package com.mp.web_searching.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mp.web_searching.models.SearchItem
import com.mp.web_searching.models.UiState
import com.mp.web_searching.viewModels.ToolScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun ToolingScreen(viewModel: ToolScreenViewModel = viewModel(), scope: CoroutineScope) {
    val state by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    var url by remember { mutableStateOf("https://en.wikipedia.org/wiki/Brain") }


    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusChip(state)


            Button(onClick = {
                viewModel.setLoading("Searching…")
                scope.launch {
                    try {
                        val json =
                            withTimeout(15_000L) { viewModel.searchWeb("Brain", 5) }
                        Log.d("ToolingScreen", "searchWeb returned: $json")
                        viewModel.searchAdapter.fromJson(json)
                            ?.let { viewModel.setSearchSuccess(it) }
                            ?: viewModel.setError("Parse error")
                    } catch (t: Throwable) {
                        val msg = "searchWeb failed: ${t.message ?: t::class.java.simpleName}"
                        Log.w("ToolingScreen", msg, t)
                        viewModel.setError(msg)
                    }
                }
            }) {
                Text(text = "Search Web")
            }


            Button(onClick = {
                viewModel.setLoading("Fetching page…")
                scope.launch {
                    try {
                        val res = withTimeout(15_000L) { viewModel.fetchAndSummarize(url) }
                        viewModel.pageAdapter.fromJson(res)?.let { viewModel.setFetchSuccess(it) }
                            ?: viewModel.setError("Parse error")
                    } catch (t: Throwable) {
                        val msg = "fetchPage failed: ${t.message ?: t::class.java.simpleName}"
                        Log.w("ToolingScreen", msg, t)
                        viewModel.setError(msg)
                    }
                }
            }) {
                Text(text = "Summarize Web")
            }


            when (val s = state) {
                UiState.Idle -> AssistCard(
                    "Ready", "Call a tool (searchWeb / fetchPage) to populate results here."
                )


                is UiState.Loading -> LoadingCard(s.label)


                is UiState.SearchSuccess -> {
                    ResultMetaCard(
                        "Search complete",
                        "“${s.data.query}” • ${s.data.results.size} results • ${s.data.elapsed_ms} ms"
                    )
                    if (s.data.results.isNotEmpty()) {
                        url = s.data.results[0].url
                    }
                    SearchResultsList(
                        results = s.data.results,
                        onOpen = { link -> viewModel.openLink(ctx, link) })
                }


                is UiState.FetchSuccess -> {
                    ResultMetaCard("Page fetched", s.data.url)
                    PageSummaryCard(s.data.summary)
                }


                is UiState.Error -> ErrorCard("Error", s.message)
            }
        }
    }
}

@Composable
private fun StatusChip(state: UiState) {
    val (label, tone) = when (state) {
        UiState.Idle -> "Idle" to ChipTone.Neutral
        is UiState.Loading -> "Searching…" to ChipTone.Info
        is UiState.SearchSuccess -> "Search complete" to ChipTone.Success
        is UiState.FetchSuccess -> "Fetch complete" to ChipTone.Success
        is UiState.Error -> "Error" to ChipTone.Error
    }
    Surface(
        color = when (tone) {
            ChipTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
            ChipTone.Info -> MaterialTheme.colorScheme.primaryContainer
            ChipTone.Success -> MaterialTheme.colorScheme.tertiaryContainer
            ChipTone.Error -> MaterialTheme.colorScheme.errorContainer
        },
        contentColor = when (tone) {
            ChipTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            ChipTone.Info -> MaterialTheme.colorScheme.onPrimaryContainer
            ChipTone.Success -> MaterialTheme.colorScheme.onTertiaryContainer
            ChipTone.Error -> MaterialTheme.colorScheme.onErrorContainer
        },
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private enum class ChipTone { Neutral, Info, Success, Error }

@Composable
private fun AssistCard(title: String, body: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(8.dp))
            Text(body, maxLines = 10, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoadingCard(label: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label)
        }
    }
}

@Composable
private fun ResultMetaCard(title: String, subtitle: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultsList(results: List<SearchItem>, onOpen: (String) -> Unit) {
    if (results.isEmpty()) {
        AssistCard("No results", "Try a different query.")
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        results.forEach { item ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(item.url) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        item.title.ifBlank { item.url },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    if (item.snippet.isNotBlank()) {
                        Text(
                            item.snippet,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        item.url.hostOrSelf(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PageSummaryCard(summary: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Text(
            text = summary,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorCard(title: String, body: String) = AssistCard(title, body)


private fun String.hostOrSelf(): String = try {
    val u = this.toUri()
    (u.host ?: this).removePrefix("www.")
} catch (_: Exception) {
    this
}