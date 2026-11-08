package com.noted.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiRepository(private val context: Context) {

    suspend fun generateItems(text: String?, bitmap: Bitmap?): List<String> = withContext(Dispatchers.IO) {
        val store = ApiKeyStore(context)
        if (!store.isEnabled()) throw IllegalStateException("AI is disabled — enable it in Settings")

        val apiKey = store.getKey()
            ?: throw IllegalStateException("No API key — add one in Settings")

        when (store.getProvider()) {
            AiProvider.ANTHROPIC -> generateWithAnthropic(apiKey, text, bitmap)
            AiProvider.DEEPSEEK -> generateWithDeepSeek(apiKey, text, bitmap)
        }
    }

    private fun generateWithAnthropic(apiKey: String, text: String?, bitmap: Bitmap?): List<String> {
        val contentArray = JSONArray()

        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            contentArray.put(
                JSONObject().apply {
                    put("type", "image")
                    put(
                        "source",
                        JSONObject().apply {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", b64)
                        }
                    )
                }
            )
        }

        contentArray.put(
            JSONObject().apply {
                put("type", "text")
                put("text", text ?: "Extract a checklist from this image.")
            }
        )

        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 1024)
            put("system", SYSTEM_PROMPT)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", contentArray)
                    }
                )
            )
        }

        val responseText =
            postJson(
                "https://api.anthropic.com/v1/messages",
                mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01"),
                body
            )

        val rawText = JSONObject(responseText)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()

        return parseItems(rawText)
    }

    private fun generateWithDeepSeek(apiKey: String, text: String?, bitmap: Bitmap?): List<String> {
        if (bitmap != null) {
            throw IllegalStateException(
                "DeepSeek can't read screenshots — paste the text instead, or use Anthropic in Settings"
            )
        }

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 1024)
            put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        }
                    )
                    .put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", text ?: "")
                        }
                    )
            )
        }

        val responseText =
            postJson(
                "https://api.deepseek.com/v1/chat/completions",
                mapOf("Authorization" to "Bearer $apiKey"),
                body
            )

        val rawText = JSONObject(responseText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        return parseItems(rawText)
    }

    private fun postJson(url: String, headers: Map<String, String>, body: JSONObject): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            setRequestProperty("content-type", "application/json")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        return if (conn.responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("API error ${conn.responseCode}: $err")
        }
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "Extract actionable items from the provided content and return them as a JSON array " +
                "of strings. Return only the JSON array, no other text."

        /**
         * Extracts a list of strings from an AI response that should be a JSON array.
         * Handles markdown fences and leading prose that the model adds despite the prompt.
         */
        internal fun parseItems(rawText: String): List<String> {
            val stripped = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .trimStart()
                .let { s -> if (s.endsWith("```")) s.dropLast(3).trimEnd() else s }

            val start = stripped.indexOf('[')
            val end = stripped.lastIndexOf(']')
            val jsonText =
                if (start != -1 && end > start) {
                    stripped.substring(start, end + 1)
                } else {
                    throw Exception("Unexpected response format: $stripped")
                }

            val arr = JSONArray(jsonText)
            return (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        }
    }
}
