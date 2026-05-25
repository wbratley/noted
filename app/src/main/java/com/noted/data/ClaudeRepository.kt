package com.noted.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ClaudeRepository(private val context: Context) {

    suspend fun generateItems(text: String?, bitmap: Bitmap?): List<String> = withContext(Dispatchers.IO) {
        val store = ApiKeyStore(context)
        if (!store.isEnabled()) throw IllegalStateException("Claude is disabled — enable it in Settings")

        val apiKey = store.getKey()
            ?: throw IllegalStateException("No API key — add one in Settings")

        val contentArray = JSONArray()

        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            contentArray.put(JSONObject().apply {
                put("type", "image")
                put("source", JSONObject().apply {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", b64)
                })
            })
        }

        contentArray.put(JSONObject().apply {
            put("type", "text")
            put("text", text ?: "Extract a checklist from this image.")
        })

        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 1024)
            put("system", "Extract actionable items from the provided content and return them as a JSON array of strings. Return only the JSON array, no other text.")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            }))
        }

        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            setRequestProperty("content-type", "application/json")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val responseCode = conn.responseCode
        val responseText = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("API error $responseCode: $err")
        }

        val rawText = JSONObject(responseText)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()

        // Strip markdown fences Claude sometimes adds despite the prompt
        val stripped = rawText
            .removePrefix("```json").removePrefix("```")
            .trimStart()
            .let { s -> if (s.endsWith("```")) s.dropLast(3).trimEnd() else s }

        // Find the outermost [...] in case there's any surrounding prose
        val start = stripped.indexOf('[')
        val end = stripped.lastIndexOf(']')
        val jsonText = if (start != -1 && end > start) stripped.substring(start, end + 1)
                       else throw Exception("Unexpected response: $stripped")

        val arr = JSONArray(jsonText)
        (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
    }
}
