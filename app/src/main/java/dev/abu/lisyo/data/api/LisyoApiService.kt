package dev.abu.lisyo.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LisyoApiService {
    @GET("api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("api/rooms")
    suspend fun getRooms(): List<RoomResponse>
    
    @GET("api/stream/{videoId}")
    suspend fun getStreamUrl(@Path("videoId") videoId: String): StreamResponse
    
    @GET("api/generate-names")
    suspend fun generateNames(): GenerateNamesResponse
    
    @POST("api/rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): CreateRoomResponse
    
    @GET("api/location")
    suspend fun getLocation(): LocationResponse
}

data class SearchResponse(
    val content: List<SearchResultItem>
)

data class SearchResultItem(
    val type: String,
    val name: String,
    val artist: ArtistObj?,
    val videoId: String,
    val duration: Long?,
    val thumbnails: List<ThumbnailObj>?
)

data class ArtistObj(val name: String)
data class ThumbnailObj(val url: String)

data class RoomResponse(
    val id: String,
    val name: String,
    val userCount: Int,
    val isPlaying: Boolean,
    val vibe: String = "Chill",
    val countryFlag: String = "🌐",
    val currentSong: CurrentSongResponse? = null,
    val queuePreview: List<QueueSongPreview>? = null,
    val totalSongs: Int = 0
)

data class CurrentSongResponse(
    val title: String,
    val artist: String
)

data class QueueSongPreview(
    val title: String,
    val artist: String
)

data class StreamResponse(
    val url: String,
    val duration: Long = 0
)

data class GenerateNamesResponse(
    val roomName: String,
    val username: String
)

data class CreateRoomRequest(
    val name: String,
    val vibe: String,
    val isPrivate: Boolean,
    val hostUsername: String,
    val countryFlag: String = "🌐"
)

data class CreateRoomResponse(
    val roomId: String,
    val name: String,
    val vibe: String,
    val isPrivate: Boolean
)

data class LocationResponse(
    val countryCode: String,
    val countryFlag: String
)
