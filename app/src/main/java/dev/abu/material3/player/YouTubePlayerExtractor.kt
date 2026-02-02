package dev.abu.material3.player

import dev.abu.material3.utils.Logger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YouTubePlayerExtractor {
    private const val TAG = "YouTubePlayerExtractor"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val TV_CLIENT_NAME = "TVHTML5_SIMPLY_EMBEDDED_PLAYER"
    private const val TV_CLIENT_VERSION = "2.0"
    private const val TV_API_KEY = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8"
    private const val TV_USER_AGENT = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"

    suspend fun getAudioUrl(videoId: String): String? {
        try {
            Logger.logInfo(TAG, "Attempting TVHTML5 extraction for $videoId")
            
            val json = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", TV_CLIENT_NAME)
                        put("clientVersion", TV_CLIENT_VERSION)
                        put("hl", "en")
                        put("gl", "US")
                    })
                    put("thirdParty", JSONObject().apply {
                        put("embedUrl", "https://www.youtube.com/watch?v=$videoId")
                    })
                })
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?key=$TV_API_KEY")
                .header("User-Agent", TV_USER_AGENT)
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Format-Version", "2")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            
            if (!response.isSuccessful) {
                Logger.logError(TAG, "TVHTML5 request failed: ${response.code}. Body: ${body.take(200)}")
                return null
            }

            val playerResponse = JSONObject(body)
            val playabilityStatus = playerResponse.optJSONObject("playabilityStatus")
            if (playabilityStatus?.optString("status") != "OK") {
                Logger.logError(TAG, "Video $videoId not playable: ${playabilityStatus?.optString("reason")}")
                return null
            }

            val streamingData = playerResponse.optJSONObject("streamingData") ?: return null
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: return null
            
            var bestUrl: String? = null
            var bestBitrate = 0
            
            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val mimeType = format.optString("mimeType", "")
                if (mimeType.contains("audio/")) {
                    val bitrate = format.optInt("bitrate", 0)
                    val url = format.optString("url")
                    if (url.isNotBlank() && bitrate > bestBitrate) {
                        bestBitrate = bitrate
                        bestUrl = url
                    }
                }
            }
            
            if (bestUrl != null) {
                Logger.logInfo(TAG, "TVHTML5 extraction successful for $videoId")
                return bestUrl
            }
            
            Logger.logError(TAG, "No audio stream found in TVHTML5 response for $videoId")
        } catch (e: Exception) {
            Logger.logError(TAG, "TVHTML5 extraction error for $videoId", e)
        }
        return null
    }
}
