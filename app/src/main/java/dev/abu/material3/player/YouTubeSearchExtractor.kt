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

    private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    suspend fun search(query: String): List<Song> {
        try {
            Logger.logInfo(TAG, "Searching for: $query")
            
            val json = JSONObject().apply {
                put("query", query)
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA==") // Filter for Songs
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20220606.03.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search?key=$API_KEY")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Format-Version", "1")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            
            if (!response.isSuccessful) {
                Logger.logError(TAG, "Search request failed: ${response.code}")
                return emptyList()
            }

            val searchResponse = JSONObject(body)
            return parseSearchResponse(searchResponse)
        } catch (e: Exception) {
            Logger.logError(TAG, "Search extraction error", e)
        }
        return emptyList()
    }

    private fun parseSearchResponse(response: JSONObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val contents = response.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val shelf = contents.optJSONObject(i)?.optJSONObject("musicShelfRenderer") ?: continue
                val items = shelf.optJSONArray("contents") ?: continue
                
                for (j in 0 until items.length()) {
                    val renderer = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val song = parseRenderer(renderer)
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

    private fun parseRenderer(renderer: JSONObject): Song? {
        try {
            val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")
                ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                ?: return null

            val flexColumns = renderer.optJSONArray("flexColumns") ?: return null
            
            // Title is usually in the first column
            val title = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: "Unknown Title"

            // Artist and other info are in the second column
            val secondColumnRuns = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            
            val artist = secondColumnRuns?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"

            // Duration is usually the last run in the second column if it exists
            var duration: Long = 0
            if (secondColumnRuns != null && secondColumnRuns.length() > 0) {
                val lastRunText = secondColumnRuns.optJSONObject(secondColumnRuns.length() - 1)?.optString("text") ?: ""
                if (lastRunText.contains(":")) {
                    duration = parseDuration(lastRunText)
                }
            }

            // Thumbnail
            val thumbnails = renderer.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
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
