package dev.abu.material3.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long, // in milliseconds
    val coverUrl: String? = null
)

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isSystem: Boolean = false
)

data class SessionUser(
    val id: String,
    val username: String,
    val isHost: Boolean,
    val isOnline: Boolean = true
)

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false, // Loading state for buffering/loading
    val currentPosition: Long = 0L,
    val lastSyncTime: Long = 0L // System time when position was updated
)
