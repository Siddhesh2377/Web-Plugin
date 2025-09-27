package com.mp.web_automation.parser

import android.util.Log
import com.mp.web_automation.analyzer.WebPageContext
import com.mp.web_automation.analyzer.WebPageElement
import com.mp.web_automation.models.WebAction
import com.mp.web_automation.models.ActionType

object AdvancedCommandParser {
    fun parseCommands(aiResponse: String, context: WebPageContext): List<WebAction> {
        val actions = mutableListOf<WebAction>()
        val lines = aiResponse.lines()

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val lower = line.lowercase()

            when {
                lower.startsWith("navigate to") -> {
                    extractUrl(line)?.let { actions.add(WebAction(ActionType.NAVIGATE, url = it)) }
                }

                lower.contains("type") && lower.contains("into") -> {
                    val text = extractQuotedValue(line, "type\\s+'([^']+)'")
                    val targetId = extractQuotedValue(line, "into.*id\\s+'([^']+)'")
                    val targetSel = extractQuotedValue(line, "into.*selector\\s+'([^']+)'")
                    when {
                        targetSel != null && text != null -> actions.add(WebAction(ActionType.TYPE, selector = targetSel, value = text))
                        targetId != null && text != null -> actions.add(WebAction(ActionType.TYPE, selector = "#${targetId}", value = text))
                        text != null -> {
                            // Try find an input by placeholder or label
                            val input = findInputByText(context, text)
                            input?.let { actions.add(WebAction(ActionType.TYPE, selector = it.selector, value = text)) }
                        }
                    }
                }

                lower.contains("click on") || lower.startsWith("click") -> {
                    val id = extractQuotedValue(line, "element with id\\s+'([^']+)'")
                    val sel = extractQuotedValue(line, "selector\\s+'([^']+)'")
                    val text = extractQuotedValue(line, "with text\\s+'([^']+)'")
                    when {
                        id != null -> actions.add(WebAction(ActionType.CLICK, selector = "#${id}"))
                        sel != null -> actions.add(WebAction(ActionType.CLICK, selector = sel))
                        text != null -> {
                            // resolve by matching context
                            val resolved = resolveByText(context, text)
                            if (resolved != null) actions.add(WebAction(ActionType.CLICK, selector = resolved.selector, value = text))
                            else actions.add(WebAction(ActionType.CLICK, value = text)) // leave unresolved, executor will fallback
                        }
                        else -> {
                            // generic click fallback: try to click the first prominent button
                            val firstBtn = context.buttons.firstOrNull() ?: context.links.firstOrNull()
                            firstBtn?.let { actions.add(WebAction(ActionType.CLICK, selector = it.selector)) }
                        }
                    }
                }

                lower.contains("submit form") -> {
                    val formId = extractQuotedValue(line, "form with id\\s+'([^']+)'")
                    if (formId != null) actions.add(WebAction(ActionType.SUBMIT_FORM, selector = "#${formId}"))
                    else actions.add(WebAction(ActionType.SUBMIT_FORM))
                }

                lower.contains("wait") && lower.contains("second") -> {
                    val seconds = extractNumber(line) ?: 2
                    actions.add(WebAction(ActionType.WAIT, value = (seconds * 1000).toString()))
                }

                lower.contains("scroll") -> actions.add(WebAction(ActionType.SCROLL))

                lower.startsWith("back") -> actions.add(WebAction(ActionType.BACK))

                else -> {
                    // Unknown sentence: attempt to interpret as "click 'text'" or navigate
                    val url = extractUrl(line)
                    if (url != null) actions.add(WebAction(ActionType.NAVIGATE, url = url))
                    else {
                        // try implicit click by quoted text
                        val quoted = extractQuotedValue(line, "'([^']+)'")
                        if (quoted != null) {
                            val resolved = resolveByText(context, quoted)
                            if (resolved != null) actions.add(WebAction(ActionType.CLICK, selector = resolved.selector, value = quoted))
                        }
                    }
                }
            }
        }

        Log.d("AdvancedCommandParser", "Parsed ${actions.size} actions from AI response")
        return actions
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }

    private fun extractQuotedValue(text: String, pattern: String): String? {
        val regex = Regex(pattern)
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractNumber(text: String): Int? {
        val numberPattern = Regex("\\d+")
        return numberPattern.find(text)?.value?.toIntOrNull()
    }

    private fun resolveByText(context: WebPageContext, text: String): WebPageElement? {
        // Prefer buttons, then links, then any element. Use contains (case-insensitive).
        val byButton = context.buttons.firstOrNull { it.text?.contains(text, ignoreCase = true) == true }
        if (byButton != null) return byButton
        val byLink = context.links.firstOrNull { it.text?.contains(text, ignoreCase = true) == true }
        if (byLink != null) return byLink
        val byAny = context.allElements.firstOrNull { it.text?.contains(text, ignoreCase = true) == true }
        return byAny
    }

    private fun findInputByText(context: WebPageContext, text: String): WebPageElement? {
        // Try placeholders, labels and types like 'search'
        val byPlaceholder = context.inputs.firstOrNull { it.placeholder?.contains(text, ignoreCase = true) == true }
        if (byPlaceholder != null) return byPlaceholder
        val byLabel = context.textElements.firstOrNull { it.text?.contains(text, ignoreCase = true) == true }
        // if label found, try nearby input by position heuristic
        if (byLabel != null) {
            val near = context.inputs.minByOrNull { kotlin.math.abs(it.position - byLabel.position) }
            if (near != null) return near
        }
        // fallback: any input of type search or text
        val searchInput = context.inputs.firstOrNull { it.type?.contains("search", ignoreCase = true) == true }
        if (searchInput != null) return searchInput
        return context.inputs.firstOrNull()
    }
}