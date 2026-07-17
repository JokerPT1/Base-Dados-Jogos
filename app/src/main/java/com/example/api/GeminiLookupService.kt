package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiLookupService {
    private const val TAG = "GeminiLookupService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class GameMetadata(
        val name: String,
        val type: String, // "GAME" or "CONSOLE"
        val platform: String, // "PS1", "PS2", "PS3", "PS4", "PS5", "Retro", "Other"
        val releaseYear: String,
        val estimatedPriceBought: Double,
        val estimatedPriceSold: Double,
        val notes: String
    )

    suspend fun lookupItem(query: String, isBarcode: Boolean): GameMetadata? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is missing or is placeholder!")
            return@withContext null
        }

        val prompt = if (isBarcode) {
            """
            You are an expert retro game reselling assistant.
            A user has scanned a barcode with value: "$query".
            Please identify what video game or gaming console this barcode corresponds to.
            If you cannot find the exact match for this barcode, make your best educated guess or provide a fallback retro game template.
            
            Return the result ONLY as a raw JSON object with these fields, with no markdown formatting and no backticks:
            {
              "name": "Full Name of Game or Console",
              "type": "GAME" or "CONSOLE",
              "platform": "PS1", "PS2", "PS3", "PS4", "PS5", "Switch", "Retro", or "Other",
              "releaseYear": "YYYY",
              "estimatedPriceBought": 15.0,
              "estimatedPriceSold": 45.0,
              "notes": "Brief reseller trivia, demand level, or rarity notes."
            }
            """.trimIndent()
        } else {
            """
            You are an expert retro game reselling assistant.
            A user is searching for item metadata for: "$query".
            Identify the game or console, and find standard retro reselling pricing information.
            
            Return the result ONLY as a raw JSON object with these fields, with no markdown formatting and no backticks:
            {
              "name": "Full Name of Game or Console",
              "type": "GAME" or "CONSOLE",
              "platform": "PS1", "PS2", "PS3", "PS4", "PS5", "Switch", "Retro", or "Other",
              "releaseYear": "YYYY",
              "estimatedPriceBought": 15.0,
              "estimatedPriceSold": 45.0,
              "notes": "Brief reseller trivia, demand level, or rarity notes."
            }
            """.trimIndent()
        }

        val requestJson = JSONObject().apply {
            put("contents", org.json.JSONArray().put(
                JSONObject().put("parts", org.json.JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(responseBody)
                val textResponse = rootJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Parse the inner JSON returned by Gemini
                val innerJson = JSONObject(textResponse.trim())
                val name = innerJson.optString("name", query)
                val type = innerJson.optString("type", "GAME").uppercase()
                val platform = mapPlatform(innerJson.optString("platform", "Other"))
                val releaseYear = innerJson.optString("releaseYear", "Unknown")
                val estBought = innerJson.optDouble("estimatedPriceBought", 10.0)
                val estSold = innerJson.optDouble("estimatedPriceSold", 30.0)
                val notes = innerJson.optString("notes", "Scanned barcode: $query")

                return@withContext GameMetadata(
                    name = name,
                    type = type,
                    platform = platform,
                    releaseYear = releaseYear,
                    estimatedPriceBought = estBought,
                    estimatedPriceSold = estSold,
                    notes = notes
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing lookup: ${e.message}", e)
            return@withContext null
        }
    }

    private fun mapPlatform(platform: String): String {
        val upper = platform.uppercase()
        return when {
            upper.contains("PS1") || upper.contains("PLAYSTATION 1") -> "PS1"
            upper.contains("PS2") || upper.contains("PLAYSTATION 2") -> "PS2"
            upper.contains("PS3") || upper.contains("PLAYSTATION 3") -> "PS3"
            upper.contains("PS4") || upper.contains("PLAYSTATION 4") -> "PS4"
            upper.contains("PS5") || upper.contains("PLAYSTATION 5") -> "PS5"
            upper.contains("SWITCH") || upper.contains("NINTENDO") -> "Switch"
            upper.contains("XBOX") -> "Xbox"
            upper.contains("RETRO") || upper.contains("NES") || upper.contains("SNES") || upper.contains("GENESIS") -> "Retro"
            else -> "Other"
        }
    }
}
