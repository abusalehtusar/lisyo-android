package dev.abu.material3.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayer(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    
    // We can expose local playback state if needed
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun initialize() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })
            }
        }
    }

    fun play(url: String, startPositionMs: Long = 0, playWhenReady: Boolean = true) {
        CoroutineScope(Dispatchers.Main).launch {
            val player = exoPlayer ?: return@launch
            
            // Check if we are already playing this item to avoid reload
            val currentItem = player.currentMediaItem
            if (currentItem?.mediaId == url) {
                if (kotlin.math.abs(player.currentPosition - startPositionMs) > 2000) {
                     player.seekTo(startPositionMs)
                }
                if (player.isPlaying != playWhenReady) {
                    if (playWhenReady) player.play() else player.pause()
                }
                return@launch
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId(url)
                .setUri(url)
                .build()

            player.setMediaItem(mediaItem)
            player.seekTo(startPositionMs)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    fun pause() {
        CoroutineScope(Dispatchers.Main).launch {
            exoPlayer?.pause()
        }
    }

    fun resume() {
        CoroutineScope(Dispatchers.Main).launch {
            exoPlayer?.play()
        }
    }

    fun seekTo(positionMs: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            exoPlayer?.seekTo(positionMs)
        }
    }

    fun release() {
        CoroutineScope(Dispatchers.Main).launch {
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
