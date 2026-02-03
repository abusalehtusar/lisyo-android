package dev.abu.lisyo.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface YouTubeApiService {
    @POST("youtubei/v1/player")
    suspend fun getPlayerResponse(@Body body: PlayerRequest): PlayerResponse
}

data class PlayerRequest(
    val videoId: String,
    val context: ContextData
)

data class ContextData(
    val client: ClientData
)

data class ClientData(
    val clientName: String = "ANDROID_TESTSUITE",
    val clientVersion: String = "1.9",
    val androidSdkVersion: Int = 30,
    val hl: String = "en",
    val gl: String = "US",
    val utcOffsetMinutes: Int = 0
)

data class PlayerResponse(
    val streamingData: StreamingData?
)

data class StreamingData(
    val formats: List<Format>?,
    val adaptiveFormats: List<Format>?
)

data class Format(
    val itag: Int,
    val url: String?,
    val mimeType: String?,
    val contentLength: String?,
    @SerializedName("cipher") val cipher: String?,
    @SerializedName("signatureCipher") val signatureCipher: String?
)
