package com.mp.web_automation.models


data class ChatRequest(
    val model: String = "sarvam-m",
    val messages: List<Message>,
    val stream: Boolean? = null
)

data class Message(
    val role: String,
    val content: String
)


enum class Role(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant")
}

enum class ActionType { NAVIGATE, CLICK, TYPE, WAIT, SCROLL, SUBMIT_FORM, BACK }


data class WebAction(
    val type: ActionType,
    val selector: String? = null, // CSS selector when resolved
    val value: String? = null, // text to type or to match
    val url: String? = null // for NAVIGATE
)