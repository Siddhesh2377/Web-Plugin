package com.mp.web_automation.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mp.web_automation.BuildConfig
import com.mp.web_automation.agent.ContextAwareAgent
import com.mp.web_automation.analyzer.WebPageAnalyzer
import com.mp.web_automation.models.Message
import kotlinx.coroutines.launch

@Composable
fun ContextAwareWebAutomationScreen() {
    val coroutineScope = rememberCoroutineScope()

    var userInput by remember { mutableStateOf("") }
    var directCommand by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var statusMessage by remember { mutableStateOf("Ready for automation") }

    LaunchedEffect(Unit) {
        ContextAwareAgent.initialize(BuildConfig.api)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = statusMessage,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Context-Aware Chat Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🧠 Context-Aware AI Assistant", style = MaterialTheme.typography.headlineSmall)
                Text("Analyzes webpage and performs intelligent automation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Describe what you want to do...") },
                    enabled = !isLoading,
                    placeholder = { Text("e.g., 'Fill out this form with my contact info'") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (userInput.isNotBlank() && webView != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    statusMessage = "Analyzing webpage and processing request..."

                                    try {
                                        val result = ContextAwareAgent.processUserRequestWithContext(userInput, webView!!)
                                        result.onSuccess {
                                            statusMessage = "Automation completed successfully!"
                                        }.onFailure { error ->
                                            statusMessage = "Error: ${error.message}"
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = "Error: ${e.message}"
                                    }

                                    isLoading = false
                                    userInput = ""
                                }
                            }
                        },
                        enabled = !isLoading && userInput.isNotBlank() && webView != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("🚀 Execute Smart Automation")
                        }
                    }
                }
            }
        }

        // Quick Test Actions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Test Actions", style = MaterialTheme.typography.titleMedium)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Button(onClick = {
                            webView?.loadUrl("https://docs.google.com/forms/d/e/1FAIpQLScY0_gE-XkiFUjHtzDr1UszfWm5VuXu2y3zRa_NycE0G503-w/viewform")
                            statusMessage = "Loading Google Form..."
                        }) {
                            Text("📝 Load Form")
                        }
                    }

                    item {
                        Button(onClick = {
                            webView?.loadUrl("https://github.com/login")
                            statusMessage = "Loading GitHub login..."
                        }) {
                            Text("🔐 GitHub Login")
                        }
                    }

                    item {
                        Button(onClick = {
                            if (webView != null) {
                                coroutineScope.launch {
                                    statusMessage = "Analyzing current page..."
                                    val context = WebPageAnalyzer.analyzePage(webView!!)
                                    statusMessage = "Found: ${context?.forms?.size ?: 0} forms, ${context?.buttons?.size ?: 0} buttons"
                                }
                            }
                        }) {
                            Text("🔍 Analyze Page")
                        }
                    }
                }
            }
        }

        // WebView Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .padding(8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        webViewClient = WebViewClient()
                        webView = this
                        loadUrl("https://docs.google.com/forms/d/e/1FAIpQLScY0_gE-XkiFUjHtzDr1UszfWm5VuXu2y3zRa_NycE0G503-w/viewform")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.role == "user")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = message.role.uppercase(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}