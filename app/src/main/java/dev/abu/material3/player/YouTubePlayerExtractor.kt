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
        // IOS: Modern version - Most reliable currently
        Triple("IOS", "19.29.1", "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc"),
        // ANDROID_MUSIC: Modern version
        Triple("ANDROID_MUSIC", "5.44.54", "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI"),
        // TVHTML5: Standard
        Triple("TVHTML5", "7.20230405.08.01", "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8"),
        // WEB_REMIX: YT Music Web
        Triple("WEB_REMIX", "1.20220606.03.00", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30")
    )

    private const val USER_AGENT_ANDROID = "com.google.android.apps.youtube.music/5.44.54 (Linux; U; Android 14; en_US; Pixel 7; Build/AP1A.240305.019; Cronet/122.0.6261.119)"
    private const val USER_AGENT_IOS = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"

    suspend fun getAudioUrl(videoId: String): String? {
        for (clientInfo in clients) {
            val (clientName, clientVersion, apiKey) = clientInfo
            try {
                Logger.logInfo(TAG, "Attempting extraction for $videoId using $clientName")
                
                val userAgent = when (clientName) {
                    "IOS" -> USER_AGENT_IOS
                    "ANDROID_MUSIC" -> USER_AGENT_ANDROID
                    else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
                }

                val json = JSONObject().apply {
                    put("videoId", videoId)
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", clientName)
                            put("clientVersion", clientVersion)
                            put("hl", "en")
                            put("gl", "US")
                            if (clientName == "IOS") {
                                put("osVersion", "17.5.1.21F90")
                            }
                        })
                    })
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                }

                val request = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player?key=$apiKey")
                    .header("User-Agent", userAgent)
                    .header("Content-Type", "application/json")
                    .apply {
                        dev.abu.material3.data.api.SocketManager.youtubeCookie.value?.let {
                            header("Cookie", it)
                        }
                    }
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
