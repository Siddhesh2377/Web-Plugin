package com.mp.web_automation.agent

import android.util.Log
import android.webkit.WebView
import com.mp.web_automation.analyzer.WebPageAnalyzer
import com.mp.web_automation.analyzer.WebPageContext
import com.mp.web_automation.models.ActionType
import com.mp.web_automation.models.Message
import com.mp.web_automation.models.Role
import com.mp.web_automation.models.WebAction
import com.mp.web_automation.network.ApiClient
import com.mp.web_automation.parser.AdvancedCommandParser
import kotlinx.coroutines.delay

object ContextAwareAgent {

    fun initialize(apiKey: String) {
        ApiClient.initialize(apiKey)
    }

    // Top-level function: process user request with repeated analyze -> act cycles
    suspend fun processUserRequestWithContext(
        userRequest: String,
        webView: WebView
    ): Result<String> {
        try {
            var pageContext = WebPageAnalyzer.analyzePage(webView) ?: return Result.failure(
                Exception("Failed to analyze webpage")
            )

            // Build initial prompt
            var prompt = createContextPrompt(userRequest, pageContext)
            prompt = getSystemPrompt() + "\n\n" + prompt

            // Send to AI
            val messages = listOf(
                Message(Role.SYSTEM.toString(), getSystemPrompt()),
                Message(Role.USER.toString(), prompt)
            )
            val result = ApiClient.sendChatMessage(messages)

            result.onFailure { return Result.failure(it) }
            val aiResp = result.getOrThrow()

            // Parse actions
            var actions = AdvancedCommandParser.parseCommands(aiResp, pageContext)

            // Execute in a loop. After each NAVIGATE or CLICK that causes navigation, re-analyze.
            val maxSteps = 20
            var step = 0
            while (actions.isNotEmpty() && step < maxSteps) {
                for (action in actions) {
                    Log.d("ContextAwareAgent", "Executing step ${step + 1}: $action")
                    when (action.type) {
                        ActionType.NAVIGATE -> executeNavigation(action, webView)
                        ActionType.CLICK -> executeClick(action, webView)
                        ActionType.TYPE -> executeType(action, webView)
                        ActionType.WAIT -> delay(action.value?.toLongOrNull() ?: 1000)
                        ActionType.SCROLL -> executeScroll(webView)
                        ActionType.SUBMIT_FORM -> executeFormSubmit(action, webView)
                        ActionType.BACK -> webView.evaluateJavascript("history.back();", null)
                    }

                    // After a navigation or submit, wait and re-analyze
                    if (action.type == ActionType.NAVIGATE || action.type == ActionType.SUBMIT_FORM || action.type == ActionType.CLICK) {
                        delay(1200)
                        pageContext = WebPageAnalyzer.analyzePage(webView) ?: pageContext
                    }

                    // small inter-action delay
                    delay(600)
                    step++
                    if (step >= maxSteps) break
                }

                // After executing current actions, re-prompt the AI with updated context and remaining goal
                val followUpPrompt = createContextPrompt(
                    userRequest,
                    pageContext
                ) + "\n\nPREVIOUS_ACTIONS:\n" + actions.joinToString("\n")
                val followMessages = listOf(
                    Message(Role.SYSTEM.toString(), getSystemPrompt()),
                    Message(Role.USER.toString(), followUpPrompt)
                )
                val followResult = ApiClient.sendChatMessage(followMessages)
                if (followResult.isFailure) break
                val followResp = followResult.getOrNull() ?: break
                actions = AdvancedCommandParser.parseCommands(followResp, pageContext)
            }

            return Result.success("Completed up to $step steps")
        } catch (e: Exception) {
            Log.e("ContextAwareAgent", "Error in context-aware processing", e)
            return Result.failure(e)
        }
    }

    private fun createContextPrompt(userRequest: String, context: WebPageContext): String {
        val prompt = StringBuilder()
        prompt.append("USER REQUEST: $userRequest\n\n")
        prompt.append("URL: ${context.url}\n")
        prompt.append("Title: ${context.title}\n\n")

        if (context.links.isNotEmpty()) {
            prompt.append("LINKS:\n")
            context.links.take(30).forEach { l ->
                prompt.append("- [${l.selector}] ${l.text ?: "(no text)"} ${l.href ?: ""}\n")
            }
            prompt.append("\n")
        }

        if (context.buttons.isNotEmpty()) {
            prompt.append("BUTTONS:\n")
            context.buttons.take(30)
                .forEach { b -> prompt.append("- [${b.selector}] ${b.text ?: "(no text)"}\n") }
            prompt.append("\n")
        }

        if (context.inputs.isNotEmpty()) {
            prompt.append("INPUTS:\n")
            context.inputs.take(30)
                .forEach { i -> prompt.append("- [${i.selector}] type:${i.type ?: ""} placeholder:${i.placeholder ?: ""}\n") }
            prompt.append("\n")
        }

        // Key text elements
        if (context.textElements.isNotEmpty()) {
            prompt.append("TEXT:\n")
            context.textElements.filter { it.text?.length ?: 0 > 5 }.take(40)
                .forEach { t -> prompt.append("- [${t.selector}] ${t.text}\n") }
            prompt.append("\n")
        }

        return prompt.toString()
    }

    private fun getSystemPrompt(): String {
        return """
            You are an advanced web automation assistant. You receive user requests plus a page context with selectors, texts, inputs and links.
            Provide step-by-step commands using these primitives:
            - navigate to [URL]
            - click element with selector '[selector]'
            - click element with text '[visible text]'
            - type '[text]' into element with selector '[selector]'
            - submit form with selector '[selector]'
            - wait [seconds]
            - scroll
            - back

            If exact selector is not available, prefer visible text matching. Try to minimize navigation steps.
        """.trimIndent()
    }

    // ------------------------- Executors -------------------------

    private suspend fun executeNavigation(action: WebAction, webView: WebView) {
        action.url?.let { url ->
            Log.d("ContextAwareAgent", "navigate: $url")
            webView.loadUrl(url)
        }
    }

    private suspend fun executeClick(action: WebAction, webView: WebView) {
        val jsCode = when {
            action.selector != null -> "document.querySelector(\"${escapeJsSelector(action.selector)}\")?.click();"
            action.value != null -> {
                // case-insensitive contains match among buttons/anchors
                """
                (function() {
                    const text = `${escapeJsString(action.value)}`.toLowerCase();
                    const elements = Array.from(document.querySelectorAll('button, a, [role="button"], input[type="submit"], input[type="button"], [onclick]'));
                    for (let el of elements) {
                        try {
                            const t = (el.innerText || el.textContent || '').toLowerCase();
                            if (t && t.includes(text)) { el.click(); return 'clicked'; }
                        } catch(e){}
                    }
                    return 'not_found';
                })();
                """.trimIndent()
            }

            else -> "(function(){const el = document.querySelector('button, a, [role=\'button\']'); if(el){el.click(); return 'clicked';} return 'not_found'; })();"
        }

        webView.evaluateJavascript(jsCode) { res ->
            Log.d(
                "ContextAwareAgent",
                "click result: $res"
            )
        }
    }

    private suspend fun executeType(action: WebAction, webView: WebView) {
        if (action.selector == null || action.value == null) return
        val jsCode = """
            (function(){
                const el = document.querySelector("${escapeJsSelector(action.selector)}");
                if(!el) return 'not_found';
                el.focus();
                el.value = `${escapeJsString(action.value)}`;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return 'typed';
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { res ->
            Log.d(
                "ContextAwareAgent",
                "type result: $res"
            )
        }
    }

    private suspend fun executeScroll(webView: WebView) {
        webView.evaluateJavascript("window.scrollBy(0, 400);", null)
    }

    private suspend fun executeFormSubmit(action: WebAction, webView: WebView) {
        val jsCode = if (action.selector != null) {
            "document.querySelector(\"${escapeJsSelector(action.selector)}\")?.submit();"
        } else {
            "document.forms[0]?.submit();"
        }
        webView.evaluateJavascript(jsCode) { res ->
            Log.d(
                "ContextAwareAgent",
                "submit result: $res"
            )
        }
    }

    // ------------------------- Helpers -------------------------

    private fun escapeJsString(s: String): String {
        return s.replace("`", "\\`").replace("$", "\\$")
            .replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun escapeJsSelector(sel: String): String {
        // naive escaping for single/double quote contexts
        return sel.replace("\"", "\\\"").replace("\\", "\\\\")
    }
}