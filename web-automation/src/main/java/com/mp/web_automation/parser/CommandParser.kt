package com.mp.web_automation.parser

import android.util.Log
import com.mp.web_automation.models.WebAction
import com.mp.web_automation.models.ActionType

object CommandParser {

    fun parseCommands(aiResponse: String): List<WebAction> {
        val actions = mutableListOf<WebAction>()
        val lines = aiResponse.lines()

        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase()

            Log.d("CommandParser", "Parsing line: $trimmed")

            when {
                // Navigate commands - look for URLs
                lower.contains("navigate to") -> {
                    val url = extractUrl(trimmed)
                    if (url != null) {
                        actions.add(WebAction(ActionType.NAVIGATE, url = url))
                        Log.d("CommandParser", "Added NAVIGATE action: $url")
                    }
                }

                // Click commands with ID
                lower.contains("click on") && lower.contains("element with id") -> {
                    val id = extractQuotedValue(trimmed)
                    if (id != null) {
                        actions.add(WebAction(ActionType.CLICK, selector = "#$id"))
                        Log.d("CommandParser", "Added CLICK action: #$id")
                    }
                }

                // Click commands with class
                lower.contains("click on") && lower.contains("element with class") -> {
                    val className = extractQuotedValue(trimmed)
                    if (className != null) {
                        actions.add(WebAction(ActionType.CLICK, selector = ".$className"))
                        Log.d("CommandParser", "Added CLICK action: .$className")
                    }
                }

                // Simple click commands (Sign In button)
                lower.contains("click on sign in") || lower.contains("click on 'sign in'") -> {
                    actions.add(WebAction(ActionType.CLICK, selector = "SIGN_IN_BUTTON"))
                    Log.d("CommandParser", "Added CLICK action for Sign In button")
                }

                // Type commands
                lower.contains("type") && lower.contains("into element with id") -> {
                    val textMatch = Regex("type\\s+'([^']+)'").find(trimmed)
                    val idMatch = Regex("element with id\\s+'([^']+)'").find(trimmed)

                    if (textMatch != null && idMatch != null) {
                        val text = textMatch.groupValues[1]
                        val id = idMatch.groupValues[1]
                        actions.add(WebAction(ActionType.TYPE, selector = "#$id", value = text))
                        Log.d("CommandParser", "Added TYPE action: '$text' into #$id")
                    }
                }

                // Wait commands
                lower.contains("wait") && lower.contains("second") -> {
                    val seconds = extractNumber(trimmed) ?: 2
                    actions.add(WebAction(ActionType.WAIT, value = "${seconds * 1000}"))
                    Log.d("CommandParser", "Added WAIT action: ${seconds}s")
                }

                // Scroll commands
                lower.contains("scroll") -> {
                    actions.add(WebAction(ActionType.SCROLL))
                    Log.d("CommandParser", "Added SCROLL action")
                }
            }
        }

        Log.d("CommandParser", "Total actions parsed: ${actions.size}")
        return actions
    }

    private fun extractUrl(text: String): String? {
        // Extract URL from "navigate to https://example.com"
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }

    private fun extractQuotedValue(text: String): String? {
        // Extract text between quotes
        val quotedPattern = Regex("\"([^\"]+)\"")
        return quotedPattern.find(text)?.groupValues?.get(1)
    }

    private fun extractNumber(text: String): Int? {
        // Extract numbers from text
        val numberPattern = Regex("\\d+")
        return numberPattern.find(text)?.value?.toIntOrNull()
    }

    private fun convertXpathToCSS(xpath: String): String {
        // Simple XPath to CSS conversion
        return when {
            xpath.contains("contains(text()") -> {
                val text = Regex("'([^']+)'").find(xpath)?.groupValues?.get(1) ?: ""
                "button:contains('$text'), a:contains('$text'), *:contains('$text')"
            }
            xpath.contains("@id=") -> {
                val id = Regex("'([^']+)'").find(xpath)?.groupValues?.get(1) ?: ""
                "#$id"
            }
            xpath.contains("@class=") -> {
                val className = Regex("'([^']+)'").find(xpath)?.groupValues?.get(1) ?: ""
                ".$className"
            }
            else -> xpath // Fallback to original
        }
    }
}
