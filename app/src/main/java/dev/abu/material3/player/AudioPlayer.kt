package dev.abu.material3.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AudioPlayer(private val context: Context) {
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    // We can expose local playback state if needed
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private var pendingMediaId: String? = null
    private var pendingPosition: Long = 0L
    private var pendingPlayWhenReady: Boolean = true

    fun initialize() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaPlaybackService::class.java)
        )
        
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isLoading.value = playbackState == Player.STATE_BUFFERING
                    }
                })
                
                // Play pending mediaId if any
                pendingMediaId?.let { mediaId ->
                    play(mediaId, pendingPosition, pendingPlayWhenReady)
                    pendingMediaId = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun play(mediaId: String, startPositionMs: Long = 0, playWhenReady: Boolean = true) {
        val controller = mediaController
        if (controller == null) {
            // Save for later when controller is ready
            pendingMediaId = mediaId
            pendingPosition = startPositionMs
            pendingPlayWhenReady = playWhenReady
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            // Check if we are already playing this item to avoid reload
            val currentItem = controller.currentMediaItem
            if (currentItem?.mediaId == mediaId) {
                if (kotlin.math.abs(controller.currentPosition - startPositionMs) > 2000) {
                    controller.seekTo(startPositionMs)
                }
                if (controller.isPlaying != playWhenReady) {
                    if (playWhenReady) controller.play() else controller.pause()
                }
                return@launch
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(mediaId)
                .build()

            controller.setMediaItem(mediaItem)
            controller.seekTo(startPositionMs)
            controller.prepare()
            controller.playWhenReady = playWhenReady
        }
    }

    fun pause() {
        CoroutineScope(Dispatchers.Main).launch {
            mediaController?.pause()
        }
    }

    fun resume() {
        CoroutineScope(Dispatchers.Main).launch {
            mediaController?.play()
        }
    }

    fun seekTo(positionMs: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            mediaController?.seekTo(positionMs)
        }
    }
    
    fun updateMetadata(title: String, artist: String) {
        CoroutineScope(Dispatchers.Main).launch {
            mediaController?.let { controller ->
                val currentItem = controller.currentMediaItem ?: return@launch
                val newMetadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setDisplayTitle(title)
                    .build()
                val updatedItem = currentItem.buildUpon()
                    .setMediaMetadata(newMetadata)
                    .build()
                controller.replaceMediaItem(0, updatedItem)
            }
        }
    }

    fun release() {
        mediaControllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        mediaControllerFuture = null
    }
}