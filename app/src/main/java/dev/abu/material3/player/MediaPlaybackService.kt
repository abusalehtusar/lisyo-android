package dev.abu.material3.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.abu.material3.MainActivity
import dev.abu.material3.data.api.SocketManager
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    
    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val NOTIFICATION_CHANNEL_ID = "lisyo_playback_channel"
        
        // Custom commands
        const val ACTION_PLAY_PAUSE = "lisyo.action.PLAY_PAUSE"
        const val ACTION_NEXT = "lisyo.action.NEXT"
        const val ACTION_PREVIOUS = "lisyo.action.PREVIOUS"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        initializePlayer()
        initializeMediaSession()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Lisyo Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
            
        val dataSourceFactory = createDataSourceFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "Playback state: $playbackState")
                        if (playbackState == Player.STATE_ENDED) {
                            SocketManager.next()
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}", error)
                    }
                })
            }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)

        return ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
            val mediaId = dataSpec.uri.toString()
            Log.d(TAG, "Resolving data source for: $mediaId")
            
            // If it's already a full URL (not just a videoId), return as is
            if (mediaId.startsWith("http") || mediaId.contains("://")) {
                Log.d(TAG, "Direct URL detected, skipping resolution")
                return@Factory dataSpec
            }
            
            // Resolve videoId to stream URL using NewPipe Extractor
            val resolvedUrl = runBlocking {
                Log.d(TAG, "Fetching stream URL for: $mediaId via NewPipe")
                getVideoStreamUrl(mediaId)
            }
            
            if (resolvedUrl != null) {
                Log.d(TAG, "Resolved to: $resolvedUrl")
                dataSpec.withUri(android.net.Uri.parse(resolvedUrl))
            } else {
                Log.e(TAG, "Failed to resolve stream URL for: $mediaId")
                dataSpec
            }
        }
    }

    private suspend fun getVideoStreamUrl(videoId: String): String? {
        if (videoId == "test") return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        if (videoId.isBlank()) return null
        
        try {
            // Use NewPipe Extractor to get the stream URL
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            
            // Get the best audio stream
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNotEmpty()) {
                // Return the URL of the first (usually best) audio stream
                return audioStreams[0].url
            }
        } catch (e: Exception) {
            Log.e(TAG, "NewPipe resolution failed for $videoId: ${e.message}")
        }
        
        // Fallback to backend API if NewPipe fails
        try {
            Log.d(TAG, "Trying backend fallback for $videoId")
            val response = SocketManager.getApiService().getStreamUrl(videoId)
            if (response.url.isNotBlank()) {
                return response.url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend fallback failed: ${e.message}")
        }
        
        return null
    }
    
    private fun initializeMediaSession() {
        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
    
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_PLAY_PAUSE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_NEXT, Bundle.EMPTY))
                .add(SessionCommand(ACTION_PREVIOUS, Bundle.EMPTY))
                .build()
            
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }
        
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_PLAY_PAUSE -> {
                    if (SocketManager.playerState.value.isPlaying) {
                        SocketManager.pause()
                    } else {
                        SocketManager.resume()
                    }
                }
                ACTION_NEXT -> SocketManager.next()
                ACTION_PREVIOUS -> SocketManager.previous()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(), 0, 0
                )
            )
        }
    }
}