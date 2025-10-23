package com.mp.web_plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.mp.web_plugin.ui.theme.WebPluginTheme
import com.mp.web_searching.models.SearchItem
import com.mp.web_searching.models.SearchResponse
import com.mp.web_searching.models.PageSummary
import com.mp.web_searching.screens.ToolingScreen
import kotlinx.serialization.json.Json

class ToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Option 1: Dummy search data ---
        val dummySearchResponse = SearchResponse(
            query = "Neural automation systems",
            elapsed_ms = 142,
            results = listOf(
                SearchItem(
                    title = "Neuron-1: A New Kind of Symbolic AI",
                    snippet = "Neuron-1 introduces structured symbolic reasoning inspired by neural graphs.",
                    url = "https://neuroverse.ai/blog/neuron-1"
                ),
                SearchItem(
                    title = "How modular automation redefines Android AI tools",
                    snippet = "Learn how modular plugins enable on-device smart automation without cloud models.",
                    url = "https://medium.com/@neuroverse/modular-ai-android"
                ),
                SearchItem(
                    title = "Symbolic reasoning vs deep learning: the hybrid future",
                    snippet = "An exploration of how dynamic symbolic systems like NeuronTree evolve AI reasoning.",
                    url = "https://towardsdatascience.com/symbolic-ai-vs-dl"
                )
            )
        )

        // --- Option 2: Dummy page summary ---
        val dummyPageSummary = PageSummary(
            url = "https://neuroverse.ai/about",
            summary = "NeuronVerse is an offline-capable AI ecosystem where modular plugins interact through a shared reasoning engine. " +
                    "The system supports symbolic logic, low-latency context management, and multi-model inference."
        )

        // Choose one to preview:
        val jsonData = Json.encodeToString(dummySearchResponse)
        // val jsonData = Json.encodeToString(dummyPageSummary)

        setContent {
            WebPluginTheme {
                Scaffold { inner ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(inner)
                    ) {
                        ToolingScreen(data = jsonData)
                    }
                }
            }
        }
    }
}
