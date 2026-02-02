package dev.abu.material3.player

import dev.abu.material3.data.model.Song
import dev.abu.material3.utils.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YouTubeSearchExtractor {
    private const val TAG = "YouTubeSearchExtractor"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Using standard WEB client for search as it is more stable
    private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    suspend fun search(query: String): List<Song> {
        try {
            Logger.logInfo(TAG, "Searching for: $query")
            
            val json = JSONObject().apply {
                put("query", query)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20211111")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?key=$API_KEY")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .apply {
                    dev.abu.material3.data.api.SocketManager.youtubeCookie.value?.let {
                        header("Cookie", it)
                    }
                }
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            
            if (!response.isSuccessful) {
                Logger.logError(TAG, "Search request failed: ${response.code}")
                return emptyList()
            }

            val searchResponse = JSONObject(body)
            return parseWebSearchResponse(searchResponse)
        } catch (e: Exception) {
            Logger.logError(TAG, "Search extraction error", e)
        }
        return emptyList()
    }

    private fun parseWebSearchResponse(response: JSONObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val contents = response.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val itemSection = contents.optJSONObject(i)?.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue
                
                for (j in 0 until items.length()) {
                    val videoRenderer = items.optJSONObject(j)?.optJSONObject("videoRenderer") ?: continue
                    val song = parseVideoRenderer(videoRenderer)
                    if (song != null) {
                        songs.add(song)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Error parsing search response", e)
        }
        return songs
    }

    private fun parseVideoRenderer(renderer: JSONObject): Song? {
        try {
            val videoId = renderer.optString("videoId") ?: return null

            val title = renderer.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: "Unknown Title"

            val artist = renderer.optJSONObject("longBylineText")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: renderer.optJSONObject("ownerText")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: "Unknown Artist"

            val durationText = renderer.optJSONObject("lengthText")?.optString("simpleText") ?: ""
            val duration = if (durationText.contains(":")) parseDuration(durationText) else 0

            val thumbnails = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val thumbnailUrl = thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url")

            return Song(
                id = videoId,
                title = title,
                artist = artist,
                duration = duration,
                coverUrl = thumbnailUrl
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDuration(time: String): Long {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                (parts[0].toLong() * 60 + parts[1].toLong()) * 1000
            } else if (parts.size == 3) {
                (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}