package dev.abu.material3.data.api

import dev.abu.material3.data.model.Song
import retrofit2.http.GET
import retrofit2.http.Query

interface LisyoApiService {
    @GET("/api/search")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("/api/rooms")
    suspend fun getRooms(): List<RoomResponse>
}

data class SearchResponse(
    val content: List<SearchResultItem>
)

data class SearchResultItem(
    val type: String,
    val name: String,
    val artist: ArtistObj?,
    val videoId: String,
    val duration: Long?, // might be in seconds or formatted string, need to check API
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
    val countryFlag: String = "🇺🇳"
)
