package com.mp.web_automation.analyzer

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

data class WebPageElement(
    val tagName: String,
    val id: String?,
    val className: String?,
    val text: String?,
    val type: String?,
    val placeholder: String?,
    val href: String?,
    val value: String?,
    val selector: String,
    val position: Int
)

data class FormInfo(
    val formId: String?,
    val formClass: String?,
    val action: String?,
    val method: String?,
    val inputs: List<WebPageElement>
)

data class WebPageContext(
    val url: String,
    val title: String,
    val forms: List<FormInfo>,
    val buttons: List<WebPageElement>,
    val links: List<WebPageElement>,
    val inputs: List<WebPageElement>,
    val textElements: List<WebPageElement>,
    val allElements: List<WebPageElement>
)

object WebPageAnalyzer {

    suspend fun analyzePage(webView: WebView): WebPageContext? {
        return suspendCancellableCoroutine { continuation ->
            val jsCode = """
                (function() {
                    function getSelector(element) {
                        if (!element) return null;
                        if (element.id) return '#' + element.id;
                        if (element.className) {
                            const classes = (typeof element.className === 'string' ? element.className : '').split(/\s+/).filter(c => c.length > 0);
                            if (classes.length > 0) return '.' + classes[0];
                        }
                        if (element.tagName) return element.tagName.toLowerCase();
                        return null;
                    }

                    function safeText(element) {
                        try {
                            const text = (element.innerText || element.textContent || '').trim();
                            return text.substring(0, 200);
                        } catch(e) { return null; }
                    }

                    function getElementInfo(element, index) {
                        return {
                            tagName: element.tagName || null,
                            id: element.id || null,
                            className: element.className || null,
                            text: safeText(element) || null,
                            type: element.type || null,
                            placeholder: element.placeholder || null,
                            href: element.href || null,
                            value: element.value || null,
                            selector: getSelector(element) || null,
                            position: index
                        };
                    }

                    const result = { url: window.location.href, title: document.title, forms: [], buttons: [], links: [], inputs: [], textElements: [], allElements: [] };

                    // Forms
                    const forms = document.querySelectorAll('form');
                    forms.forEach((form, formIndex) => {
                        const formInputs = [];
                        const inputs = form.querySelectorAll('input, textarea, select');
                        inputs.forEach((input, inputIndex) => formInputs.push(getElementInfo(input, inputIndex)));
                        result.forms.push({ formId: form.id || null, formClass: form.className || null, action: form.action || null, method: form.method || null, inputs: formInputs });
                    });

                    // Buttons and clickable things
                    const buttons = document.querySelectorAll('button, input[type="button"], input[type="submit"], [role="button"], [onclick]');
                    buttons.forEach((button, index) => result.buttons.push(getElementInfo(button, index)));

                    // Links & nav links
                    const links = document.querySelectorAll('a[href]');
                    links.forEach((link, index) => result.links.push(getElementInfo(link, index)));

                    // All inputs
                    const allInputs = document.querySelectorAll('input, textarea, select');
                    allInputs.forEach((input, index) => result.inputs.push(getElementInfo(input, index)));

                    // Text elements (headings, labels, paragraphs)
                    const textEls = document.querySelectorAll('h1, h2, h3, h4, h5, h6, label, p, span, div');
                    textEls.forEach((el, index) => {
                        const txt = safeText(el);
                        if (txt && txt.length > 0) result.textElements.push(getElementInfo(el, index));
                    });

                    // All interactive
                    const allInteractive = document.querySelectorAll('button, a, input, textarea, select, [onclick], [role="button"]');
                    allInteractive.forEach((el, index) => result.allElements.push(getElementInfo(el, index)));

                    return JSON.stringify(result);
                })();
            """.trimIndent()

            webView.evaluateJavascript(jsCode) { result ->
                try {
                    if (result != null && result != "null") {
                        val jsonText = JSONTokener(result).nextValue().toString()
                        val jsonObject = JSONObject(jsonText)
                        val context = parseWebPageContext(jsonObject)
                        continuation.resume(context)
                    } else {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    Log.e("WebPageAnalyzer", "Error parsing page analysis", e)
                    continuation.resume(null)
                }
            }
        }
    }

    private fun parseWebPageContext(json: JSONObject): WebPageContext {
        return WebPageContext(
            url = json.optString("url", ""),
            title = json.optString("title", ""),
            forms = parseFormInfoList(json.optJSONArray("forms")),
            buttons = parseElementList(json.optJSONArray("buttons")),
            links = parseElementList(json.optJSONArray("links")),
            inputs = parseElementList(json.optJSONArray("inputs")),
            textElements = parseElementList(json.optJSONArray("textElements")),
            allElements = parseElementList(json.optJSONArray("allElements"))
        )
    }

    private fun parseElementList(jsonArray: JSONArray?): List<WebPageElement> {
        if (jsonArray == null) return emptyList()
        val elements = mutableListOf<WebPageElement>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            elements.add(
                WebPageElement(
                    tagName = obj.optString("tagName", "").ifEmpty { "" },
                    id = obj.optString("id").takeIf { it.isNotEmpty() },
                    className = obj.optString("className").takeIf { it.isNotEmpty() },
                    text = obj.optString("text").takeIf { it.isNotEmpty() },
                    type = obj.optString("type").takeIf { it.isNotEmpty() },
                    placeholder = obj.optString("placeholder").takeIf { it.isNotEmpty() },
                    href = obj.optString("href").takeIf { it.isNotEmpty() },
                    value = obj.optString("value").takeIf { it.isNotEmpty() },
                    selector = obj.optString("selector", "") ?: "",
                    position = obj.optInt("position", 0)
                )
            )
        }
        return elements
    }

    private fun parseFormInfoList(jsonArray: JSONArray?): List<FormInfo> {
        if (jsonArray == null) return emptyList()
        val forms = mutableListOf<FormInfo>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            forms.add(
                FormInfo(
                    formId = obj.optString("formId").takeIf { it.isNotEmpty() },
                    formClass = obj.optString("formClass").takeIf { it.isNotEmpty() },
                    action = obj.optString("action").takeIf { it.isNotEmpty() },
                    method = obj.optString("method").takeIf { it.isNotEmpty() },
                    inputs = parseElementList(obj.optJSONArray("inputs"))
                )
            )
        }
        return forms
    }
}