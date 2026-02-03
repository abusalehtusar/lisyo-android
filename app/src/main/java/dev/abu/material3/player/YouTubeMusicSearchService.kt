package dev.abu.material3.player

import com.liskovsoft.youtubeapi.search.SearchService
import dev.abu.material3.data.model.Song
import dev.abu.material3.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeMusicSearchService {
    private const val TAG = "YouTubeMusicSearchService"
    private val searchService = SearchService()
    
    // Music search options (Songs filter)
    private const val MUSIC_SONG_FILTER = 1 shl 2 // 0x04
    
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            Logger.logInfo(TAG, "Searching for: $query")
            
            // Use SearchService with music filter
            val searchResult = searchService.getSearch(query, MUSIC_SONG_FILTER)
            
            if (searchResult == null) {
                Logger.logError(TAG, "Search result is null")
                return@withContext emptyList()
            }
            
            val songs = mutableListOf<Song>()
            
            // Get search results from sections
            searchResult.sections?.forEach { section ->
                section.musicItems?.forEach { musicItem ->
                    try {
                        val videoId = musicItem.videoId ?: return@forEach
                        val title = musicItem.title ?: "Unknown Title"
                        val artist = musicItem.userName ?: "Unknown Artist"
                        val duration = parseDuration(musicItem.lengthText ?: "0:00")
                        
                        // Get best quality thumbnail
                        val thumbnailUrl = musicItem.thumbnails
                            ?.maxByOrNull { it.width ?: 0 }
                            ?.url
                        
                        songs.add(
                            Song(
                                id = videoId,
                                title = title,
                                artist = artist,
                                duration = duration,
                                coverUrl = thumbnailUrl
                            )
                        )
                        
                        Logger.logInfo(TAG, "Found song: $title by $artist (${musicItem.lengthText})")
                    } catch (e: Exception) {
                        Logger.logError(TAG, "Error parsing music item", e)
                    }
                }
                
                // Also check for video items (fallback)
                section.videoItems?.forEach { videoItem ->
                    try {
                        val videoId = videoItem.videoId ?: return@forEach
                        val title = videoItem.title ?: "Unknown Title"
                        val artist = videoItem.userName ?: "Unknown Artist"
                        val duration = parseDuration(videoItem.lengthText ?: "0:00")
                        
                        val thumbnailUrl = videoItem.thumbnails
                            ?.maxByOrNull { it.width ?: 0 }
                            ?.url
                        
                        songs.add(
                            Song(
                                id = videoId,
                                title = title,
                                artist = artist,
                                duration = duration,
                                coverUrl = thumbnailUrl
                            )
                        )
                        
                        Logger.logInfo(TAG, "Found video: $title by $artist")
                    } catch (e: Exception) {
                        Logger.logError(TAG, "Error parsing video item", e)
                    }
                }
            }
            
            Logger.logInfo(TAG, "Search completed. Found ${songs.size} songs")
            songs
        } catch (e: Exception) {
            Logger.logError(TAG, "Search error", e)
            emptyList()
        }
    }
    
    private fun parseDuration(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":")
            when (parts.size) {
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
