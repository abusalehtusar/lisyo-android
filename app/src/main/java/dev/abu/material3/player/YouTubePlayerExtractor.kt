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

    private val clients = listOf(
        // ANDROID_TESTSUITE: Very reliable for audio
        Triple("ANDROID_TESTSUITE", "1.9", "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI"),
        // WEB_EMBEDDED_PLAYER: Good for restricted videos
        Triple("WEB_EMBEDDED_PLAYER", "1.20230626.01.00", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3"),
        // ANDROID_VR: Sometimes bypasses bot detection
        Triple("ANDROID_VR", "1.50.2", "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI")
    )

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    suspend fun getAudioUrl(videoId: String): String? {
        for (clientInfo in clients) {
            val (clientName, clientVersion, apiKey) = clientInfo
            try {
                Logger.logInfo(TAG, "Attempting extraction for $videoId using $clientName")
                
                val json = JSONObject().apply {
                    put("videoId", videoId)
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", clientName)
                            put("clientVersion", clientVersion)
                            put("hl", "en")
                            put("gl", "US")
                        })
                        if (clientName == "WEB_EMBEDDED_PLAYER") {
                            put("thirdParty", JSONObject().apply {
                                put("embedUrl", "https://www.youtube.com/watch?v=$videoId")
                            })
                        }
                    })
                }

                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player?key=$apiKey")
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue
                
                if (!response.isSuccessful) {
                    Logger.logError(TAG, "$clientName request failed: ${response.code}")
                    continue
                }

                val playerResponse = JSONObject(body)
                val playabilityStatus = playerResponse.optJSONObject("playabilityStatus")
                if (playabilityStatus?.optString("status") != "OK") {
                    Logger.logError(TAG, "$clientName status: ${playabilityStatus?.optString("status")}, reason: ${playabilityStatus?.optString("reason")}")
                    continue
                }

                val streamingData = playerResponse.optJSONObject("streamingData") ?: continue
                val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: continue
                
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
                    Logger.logInfo(TAG, "$clientName extraction successful for $videoId")
                    return bestUrl
                }
            } catch (e: Exception) {
                Logger.logError(TAG, "$clientName extraction error for $videoId", e)
            }
        }
        return null
    }
}