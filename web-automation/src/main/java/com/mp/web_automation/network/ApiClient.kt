package com.mp.web_automation.network

import android.util.Log
import com.mp.web_automation.models.ChatRequest
import com.mp.web_automation.models.Message
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var apiKey: String = ""

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun initialize(key: String) {
        apiKey = key
    }

    suspend fun sendChatMessage(
        messages: List<Message>,
        stream: Boolean = false,
        onTokenReceived: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                model = "sarvam-m",
                messages = messages.map {
                    it.copy(role = it.role.lowercase()) // ensure "system", "user"
                },
                stream = stream // omit if not streaming
            )
            val json = moshi.adapter(ChatRequest::class.java).toJson(request)

            val body = json.toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url("https://api.sarvam.ai/v1/chat/completions")
                .post(body)
                .build()

            if (stream && onTokenReceived != null) {
                handleStreamingResponse(httpRequest, onTokenReceived)
            } else {
                handleNonStreamingResponse(httpRequest)
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Error sending message", e)
            Result.failure(e)
        }
    }

    private suspend fun handleNonStreamingResponse(request: Request): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("HTTP ${response.code}: ${response.message}")
                        )
                    }

                    val bodyString = response.body.string()
                    val jsonResponse = JSONObject(bodyString)
                    val content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    Result.success(content)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun handleStreamingResponse(
        request: Request,
        onTokenReceived: (String) -> Unit
    ): Result<String> {
        // Simplified streaming - you can implement this if needed
        return handleNonStreamingResponse(request)
    }
}