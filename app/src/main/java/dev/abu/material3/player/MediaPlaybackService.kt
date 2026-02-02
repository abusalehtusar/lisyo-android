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
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {
    
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val urlCache = ConcurrentHashMap<String, Pair<String, Long>>() // videoId -> (url, expiry)
    private val CACHE_DURATION = 30 * 60 * 1000 // 30 minutes
    
    // List of Piped instances to try as fallback
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.private.coffee",
        "https://piped-api.lunar.icu",
        "https://pipedapi.rivo.lol",
        "https://pipedapi.astartes.nl"
    )
    
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
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)

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
            val resolvedUrl = try {
                runBlocking {
                    Logger.logInfo(TAG, "Resolving: $mediaId")
                    getVideoStreamUrl(mediaId)
                }
            } catch (e: Exception) {
                Logger.logError(TAG, "runBlocking failed for $mediaId", e)
                null
            }
            
            if (resolvedUrl != null) {
                Logger.logInfo(TAG, "Resolved $mediaId to: ${resolvedUrl.take(50)}...")
                urlCache[mediaId] = Pair(resolvedUrl, System.currentTimeMillis() + CACHE_DURATION)
                dataSpec.withUri(android.net.Uri.parse(resolvedUrl))
            } else {
                Logger.logError(TAG, "CRITICAL: Could not resolve stream for $mediaId.")
                dataSpec.withUri(android.net.Uri.parse("https://localhost/error_resolving_$mediaId"))
            }
        }
    }

    private suspend fun getVideoStreamUrl(videoId: String): String? {
        if (videoId == "test") return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        if (videoId.isBlank()) return null
        
        // 1. Try Direct TVHTML5 Extraction (Most reliable currently)
        try {
            val url = YouTubePlayerExtractor.getAudioUrl(videoId)
            if (url != null) {
                Logger.logInfo(TAG, "TVHTML5 extraction successful for $videoId")
                return url
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "TVHTML5 extraction failed for $videoId", e)
        }

        // 2. Try NewPipe Extractor
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNotEmpty()) {
                val bestStream = audioStreams.sortedByDescending { it.bitrate }.first()
                Logger.logInfo(TAG, "NewPipe success for $videoId")
                return bestStream.url
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "NewPipe failed for $videoId: ${e.message}")
        }
        
        // 3. Try Backend Fallback
        try {
            Logger.logInfo(TAG, "Trying backend fallback for $videoId")
            val response = SocketManager.getApiService().getStreamUrl(videoId)
            if (response.url.isNotBlank()) {
                Logger.logInfo(TAG, "Backend success for $videoId")
                return response.url
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Backend fallback failed for $videoId")
        }
        
        // 4. Try Piped API Instances directly
        for (instance in pipedInstances) {
            try {
                Logger.logInfo(TAG, "Trying Piped instance $instance for $videoId")
                val streamUrl = fetchFromPiped(instance, videoId)
                if (streamUrl != null) {
                    Logger.logInfo(TAG, "Piped instance success: $instance")
                    return streamUrl
                }
            } catch (e: Exception) {
                Logger.logError(TAG, "Piped instance $instance failed: ${e.message}")
            }
        }
        
        return null
    }
    
    private suspend fun fetchFromPiped(instance: String, videoId: String): String? {
        val url = java.net.URL("$instance/streams/$videoId")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        if (connection.responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val audioStreams = json.optJSONArray("audioStreams")
            if (audioStreams != null && audioStreams.length() > 0) {
                var bestUrl = ""
                var bestBitrate = 0
                for (i in 0 until audioStreams.length()) {
                    val stream = audioStreams.getJSONObject(i)
                    val bitrate = stream.optInt("bitrate", 0)
                    if (bitrate > bestBitrate) {
                        bestBitrate = bitrate
                        bestUrl = stream.optString("url", "")
                    }
                }
                if (bestUrl.isNotEmpty()) return bestUrl
            }
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