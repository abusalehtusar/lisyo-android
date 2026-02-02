package dev.abu.material3.data.api

import dev.abu.material3.data.model.ChatMessage
import dev.abu.material3.data.model.PlayerState
import dev.abu.material3.data.model.SessionUser
import dev.abu.material3.data.model.Song
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.UUID

import dev.abu.material3.ui.screens.Room
import dev.abu.material3.ui.screens.dummyRooms // Keep for fallback if needed, or remove
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.ui.graphics.Color

object SocketManager {
    private var mSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // API Services
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://lisyo-backend-production-1acf.up.railway.app")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiService = retrofit.create(LisyoApiService::class.java)

    private val youtubeRetrofit = Retrofit.Builder()
        .baseUrl("https://youtubei.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val youtubeService = youtubeRetrofit.create(YouTubeApiService::class.java)

    // State Flows
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()


    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()
    
    // Search Results
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults = _searchResults.asStateFlow()
    
    // Public Rooms
    private val _publicRooms = MutableStateFlow<List<Room>>(emptyList())
    val publicRooms = _publicRooms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()


    private val _users = MutableStateFlow<List<SessionUser>>(emptyList())
    val users = _users.asStateFlow()

    private var timeOffset: Long = 0L

    @Synchronized
    fun getSocket(): Socket? {
        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.forceNew = true
                opts.reconnection = true
                mSocket = IO.socket("https://lisyo-backend-production-1acf.up.railway.app", opts)
                initListeners()
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
        return mSocket
    }

    private var audioPlayer: dev.abu.material3.player.AudioPlayer? = null

    fun init(context: android.content.Context) {
        if (audioPlayer == null) {
            audioPlayer = dev.abu.material3.player.AudioPlayer(context.applicationContext)
            audioPlayer?.initialize()
        }
    }

    private fun initListeners() {
        val socket = mSocket ?: return

        socket.on(Socket.EVENT_CONNECT) {
            syncTime()
        }
        
        // Backend emits 'room:state' on join
        socket.on("room:state") { args ->
             if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                updatePlayerState(data)
             }
        }

        // Backend emits 'player:sync' for updates
        socket.on("player:sync") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                updatePlayerState(data)
            }
        }
        
        socket.on("queue:update") { args ->
            if (args.isNotEmpty()) {
                val jsonArray = args[0] as JSONArray
                val newQueue = ArrayList<Song>()
                for (i in 0 until jsonArray.length()) {
                    newQueue.add(parseSong(jsonArray.getJSONObject(i)))
                }
                _queue.value = newQueue
            }
        }

        socket.on("chat:new") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = ChatMessage(
                    id = data.optString("id", UUID.randomUUID().toString()),
                    senderName = data.optString("senderName", "Unknown"),
                    text = data.optString("text", ""),
                    timestamp = data.optLong("timestamp", System.currentTimeMillis())
                )
                _messages.value = _messages.value + message
            }
        }

        socket.on("room:users") { args ->
            if (args.isNotEmpty()) {
                val jsonArray = args[0] as JSONArray
                val newUsers = ArrayList<SessionUser>()
                for (i in 0 until jsonArray.length()) {
                    val u = jsonArray.getJSONObject(i)
                    newUsers.add(SessionUser(
                        id = u.optString("id"),
                        username = u.optString("username"),
                        isHost = u.optBoolean("isHost")
                    ))
                }
                _users.value = newUsers
            }
        }
        
        socket.on("ntp:pong") { args ->
             if (args.isNotEmpty()) {
                 val data = args[0] as JSONObject
                 val t1 = data.optLong("t1") // Server receive
                 val t2 = data.optLong("t2") // Server send
                 val t0 = data.optLong("t0") // Client send
                 val t3 = System.currentTimeMillis()
                 
                 // Offset = ((t1 - t0) + (t2 - t3)) / 2
                 timeOffset = ((t1 - t0) + (t2 - t3)) / 2
             }
        }
    }
    
    private fun updatePlayerState(data: JSONObject) {
        val isPlaying = data.optBoolean("isPlaying", false)
        val startTime = data.optLong("startTime", 0L)
        val songJson = data.optJSONObject("currentSong")
        
        // Calculate current position
        // Server Time = Client Time - Offset
        // Position = (ClientTime - Offset) - StartTime
        
        val now = System.currentTimeMillis()
        val serverNow = now - timeOffset
        var position = if (isPlaying && startTime > 0) {
            serverNow - startTime
        } else {
            data.optLong("position", 0L)
        }
        
        if (position < 0) position = 0 // Future start or drift

        val song = if (songJson != null) parseSong(songJson) else null
        
        // Update State Flow
        _playerState.value = PlayerState(
            currentSong = song,
            isPlaying = isPlaying,
            currentPosition = position,
            lastSyncTime = now
        )
        
        // Trigger Audio Player
        scope.launch {
            if (song != null) {
                // Check if already playing this exact song to avoid re-fetching
                if (audioPlayer?.isPlaying?.value == true && _playerState.value.currentSong?.id == song.id && isPlaying) {
                     // Just seek if drift is large? AudioPlayer handles this check locally too.
                     // But we need to verify if the URL is expired or valid.
                }
                
                val videoId = song.id
                val streamUrl = getVideoStreamUrl(videoId)
                
                withContext(Dispatchers.Main) {
                     audioPlayer?.play(streamUrl, position, isPlaying)
                }
            } else {
                withContext(Dispatchers.Main) {
                    audioPlayer?.pause()
                }
            }
        }
    }
    
    // Real YouTube Stream Extractor (Basic)
    private suspend fun getVideoStreamUrl(videoId: String): String {
        // Fallback for testing
        if (videoId == "test") return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        
        try {
            val request = PlayerRequest(
                videoId = videoId,
                context = ContextData(
                    client = ClientData(
                        clientName = "ANDROID_TESTSUITE"
                    )
                )
            )
            val response = youtubeService.getPlayerResponse(request)
            
            // Look for audio-only stream in adaptiveFormats
            val audioFormat = response.streamingData?.adaptiveFormats?.find { 
                it.mimeType?.startsWith("audio") == true 
            }
            
            // Fallback to formats if adaptive not found
            val format = audioFormat ?: response.streamingData?.formats?.find {
                it.mimeType?.startsWith("audio") == true
            }
            
            return format?.url ?: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" // Fallback if extraction fails
        } catch (e: Exception) {
            e.printStackTrace()
            return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    private fun parseSong(json: JSONObject): Song {
        return Song(
            id = json.optString("id"),
            title = json.optString("title"),
            artist = json.optString("artist"),
            duration = json.optLong("duration"),
            coverUrl = json.optString("coverUrl", null)
        )
    }

    fun establishConnection() {
        val socket = getSocket()
        if (socket?.connected() == false) {
            socket.connect()
        }
    }

    fun closeConnection() {
        mSocket?.disconnect()
    }

    // Actions
    fun joinRoom(roomName: String, username: String) {
        val data = JSONObject()
        data.put("room", roomName)
        data.put("username", username)
        mSocket?.emit("join:room", data)
    }

    fun playSong(song: Song) {
        val data = JSONObject()
        data.put("id", song.id)
        data.put("title", song.title)
        data.put("artist", song.artist)
        data.put("duration", song.duration)
        data.put("coverUrl", song.coverUrl)
        mSocket?.emit("player:play", data)
    }

    fun pause() {
        mSocket?.emit("player:pause")
    }

    fun resume() {
        mSocket?.emit("player:resume")
    }
    
    fun next() {
        mSocket?.emit("player:next")
    }
    
    fun previous() {
        mSocket?.emit("player:previous")
    }

    fun sendMessage(text: String, username: String) {
        val data = JSONObject()
        data.put("text", text)
        data.put("senderName", username)
        data.put("timestamp", System.currentTimeMillis())
        mSocket?.emit("chat:send", data)
    }
    
    fun addToQueue(song: Song) {
        val data = JSONObject()
        data.put("id", song.id)
        data.put("title", song.title)
        data.put("artist", song.artist)
        data.put("duration", song.duration)
        mSocket?.emit("queue:add", data)
    }

    private fun syncTime() {
        val t0 = System.currentTimeMillis()
        val data = JSONObject()
        data.put("t0", t0)
        mSocket?.emit("ntp:ping", data)
    }
    
    fun getCurrentPosition(): Long {
        val state = _playerState.value
        if (!state.isPlaying) return state.currentPosition
        
        val elapsed = System.currentTimeMillis() - state.lastSyncTime
        return state.currentPosition + elapsed
    }
    
    // API Actions
    fun search(query: String) {
        scope.launch {
            try {
                val response = apiService.search(query)
                val songs = response.content.map { item ->
                    Song(
                        id = item.videoId,
                        title = item.name,
                        artist = item.artist?.name ?: "Unknown",
                        duration = item.duration ?: 0,
                        coverUrl = item.thumbnails?.lastOrNull()?.url
                    )
                }
                _searchResults.value = songs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun refreshRooms() {
        scope.launch {
            try {
                val response = apiService.getRooms()
                val rooms = response.mapIndexed { index, item ->
                    Room(
                        id = index, // UI uses Int id
                        countryFlag = item.countryFlag,
                        vibe = item.vibe,
                        username = "Host",
                        roomName = item.name,
                        songs = emptyList(), // Not returned by list API
                        totalSongs = 0,
                        userCount = item.userCount,
                        flagColor = Color(0xFFE3F2FD) // Default color
                    )
                }
                _publicRooms.value = rooms
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to dummy if empty or error? No, just show empty or error state.
            }
        }
    }
}
