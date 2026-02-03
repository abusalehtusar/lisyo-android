package dev.abu.material3.utils

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import dev.abu.material3.innertube.NewPipeUtils
import dev.abu.material3.innertube.YouTube
import dev.abu.material3.innertube.models.YouTubeClient
import dev.abu.material3.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import dev.abu.material3.innertube.models.YouTubeClient.Companion.IOS
import dev.abu.material3.innertube.models.YouTubeClient.Companion.WEB_REMIX
import dev.abu.material3.innertube.models.response.PlayerResponse
import dev.abu.material3.utils.potoken.PoTokenGenerator
import dev.abu.material3.utils.potoken.PoTokenResult
import okhttp3.OkHttpClient

object YTPlayerUtils {

    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_NO_AUTH

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IOS,
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Log.d(TAG, "Playback info requested: $videoId")

        val signatureTimestamp = getSignatureTimestampOrNull(videoId)

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                YouTube.dataSyncId
            } else {
                YouTube.visitorData
            }

        Log.d(TAG, "[$videoId] signatureTimestamp: $signatureTimestamp, isLoggedIn: $isLoggedIn")

        val (webPlayerPot, webStreamingPot) = getWebClientPoTokenOrNull(videoId, sessionId)?.let {
            Pair(it.playerRequestPoToken, it.streamingDataPoToken)
        } ?: Pair(null, null).also {
            Log.w(TAG, "[$videoId] No po token")
        }

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, webPlayerPot)
                .getOrThrow()

        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null

        var streamPlayerResponse: PlayerResponse? = null
        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            val client: YouTubeClient
            if (clientIndex == -1) {
                Log.d(TAG, "Trying client: ${MAIN_CLIENT.clientName}")
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                Log.d(TAG, "Trying fallback client: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                client = STREAM_FALLBACK_CLIENTS[clientIndex]

                if (client.loginRequired && !isLoggedIn) {
                    continue
                }

                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp, webPlayerPot)
                        .getOrNull()
            }

            Log.d(TAG, "[$videoId] stream client: ${client.clientName}, " +
                    "playabilityStatus: ${streamPlayerResponse?.playabilityStatus?.let {
                        it.status + (it.reason?.let { " - $it" } ?: "")
                    }}")

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format = findFormat(streamPlayerResponse) ?: continue
                streamUrl = findUrlOrNull(format, videoId) ?: continue
                streamExpiresInSeconds =
                    streamPlayerResponse.streamingData?.expiresInSeconds ?: continue

                if (client.useWebPoTokens && webStreamingPot != null) {
                    streamUrl += "&pot=$webStreamingPot";
                }

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    break
                }
                if (validateStatus(streamUrl)) {
                    Log.i(TAG, "[$videoId] [${client.clientName}] found working stream")
                    break
                } else {
                    Log.w(TAG, "[$videoId] [${client.clientName}] got bad http status code")
                }
            }
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("Missing stream expire time")
        }
        if (format == null) {
            throw Exception("Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("Could not find stream url")
        }

        Log.d(TAG, "[$videoId] stream url: $streamUrl")

        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = WEB_REMIX)

    private fun findFormat(
        playerResponse: PlayerResponse,
    ): PlayerResponse.StreamingData.Format? =
        playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
            }

    private fun validateStatus(url: String): Boolean {
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .getOrNull()
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .getOrNull()
    }

    private fun getWebClientPoTokenOrNull(videoId: String, sessionId: String?): PoTokenResult? {
        if (sessionId == null) {
            Log.d(TAG, "[$videoId] Session identifier is null")
            return null
        }
        try {
            return poTokenGenerator.getWebClientPoToken(videoId, sessionId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
