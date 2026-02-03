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
import dev.abu.material3.utils.Logger
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val urlCache = ConcurrentHashMap<String, Pair<String, Long>>() // videoId -> (url, expiry)
    private val CACHE_DURATION = 30 * 60 * 1000 // 30 minutes
    
    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val NOTIFICATION_CHANNEL_ID = "lisyo_playback_channel"
        
        const val ACTION_PLAY_PAUSE = "lisyo.action.PLAY_PAUSE"
        const val ACTION_NEXT = "lisyo.action.NEXT"
        const val ACTION_PREVIOUS = "lisyo.action.PREVIOUS"
    }
    
    override fun onCreate() {
        super.onCreate()
        Logger.logInfo(TAG, "onCreate")
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
                        Logger.logInfo(TAG, "Playback state changed: $playbackState")
                        if (playbackState == Player.STATE_ENDED) {
                            SocketManager.next()
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Logger.logError(TAG, "Player error: ${error.message}", error)
                    }
                })
            }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        return ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
            val mediaId = dataSpec.uri.toString()
            
            if (mediaId.startsWith("http") || mediaId.contains("://")) {
                return@Factory dataSpec
            }
            
            // Check cache
            val cached = urlCache[mediaId]
            if (cached != null && System.currentTimeMillis() < cached.second) {
                Logger.logInfo(TAG, "Using cached URL for $mediaId")
                return@Factory dataSpec.withUri(android.net.Uri.parse(cached.first))
            }
            
            // Resolve videoId to stream URL
            val resolvedData = try {
                runBlocking {
                    Logger.logInfo(TAG, "Resolving: $mediaId")
                    dev.abu.material3.utils.YTPlayerUtils.playerResponseForPlayback(
                        mediaId,
                        connectivityManager = connectivityManager
                    ).getOrNull()
                }
            } catch (e: Exception) {
                Logger.logError(TAG, "runBlocking failed for $mediaId", e)
                null
            }
            
            val resolvedUrl = resolvedData?.streamUrl
            val expiry = resolvedData?.streamExpiresInSeconds?.toLong()?.times(1000) ?: CACHE_DURATION.toLong()
            
            if (resolvedUrl != null) {
                Logger.logInfo(TAG, "Resolved $mediaId to: ${resolvedUrl.take(50)}...")
                urlCache[mediaId] = Pair(resolvedUrl, System.currentTimeMillis() + (expiry - 60000)) // Buffer 1 min
                dataSpec.withUri(android.net.Uri.parse(resolvedUrl))
            } else {
                Logger.logError(TAG, "CRITICAL: Could not resolve stream for $mediaId.")
                dataSpec.withUri(android.net.Uri.parse("https://localhost/error_resolving_$mediaId"))
            }
        }
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
        Logger.logInfo(TAG, "onDestroy")
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
