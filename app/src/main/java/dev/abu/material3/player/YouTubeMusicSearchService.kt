package dev.abu.material3.player

import dev.abu.material3.data.model.Song
import dev.abu.material3.innertube.YouTube
import dev.abu.material3.innertube.models.SongItem
import dev.abu.material3.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeMusicSearchService {
    private const val TAG = "YouTubeMusicSearchService"
    
    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            Logger.logInfo(TAG, "Searching for: $query")
            
            // Set cookie if available
            dev.abu.material3.data.api.SocketManager.youtubeCookie.value?.let {
                YouTube.cookie = it
            }

            val result = YouTube.searchSummary(query).getOrNull()
            
            if (result == null) {
                Logger.logError(TAG, "Search result is null")
                return@withContext emptyList()
            }
            
            val songs = mutableListOf<Song>()
            
            result.summaries.forEach { summary ->
                summary.items.forEach { item ->
                    if (item is SongItem) {
                        if (item.duration == null || item.duration == 0) {
                            Logger.logInfo(TAG, "Song without duration: ${item.title}")
                        }
                        songs.add(
                            Song(
                                id = item.id,
                                title = item.title,
                                artist = item.artists.joinToString { it.name },
                                duration = (item.duration ?: 0).toLong() * 1000,
                                coverUrl = item.thumbnail
                            )
                        )
                    }
                }
            }
            
            // If no songs in summary, try explicit song search
            if (songs.isEmpty()) {
                val songResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                songResult?.items?.forEach { item ->
                    if (item is SongItem) {
                        if (item.duration == null || item.duration == 0) {
                            Logger.logInfo(TAG, "Song without duration: ${item.title}")
                        }
                        songs.add(
                            Song(
                                id = item.id,
                                title = item.title,
                                artist = item.artists.joinToString { it.name },
                                duration = (item.duration ?: 0).toLong() * 1000,
                                coverUrl = item.thumbnail
                            )
                        )
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
}
