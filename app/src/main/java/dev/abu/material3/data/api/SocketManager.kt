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
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.ui.graphics.Color

object SocketManager {
    private var mSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private const val BASE_URL = "https://lisyo-backend-production-1acf.up.railway.app"
    
    // API Services
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiService = retrofit.create(LisyoApiService::class.java)

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
    
    // Loading States
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms = _isLoadingRooms.asStateFlow()
    
    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream = _isLoadingStream.asStateFlow()
    
    // Shuffle and Repeat
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()
    
    private val _repeatMode = MutableStateFlow("off") // "off", "all", "one"
    val repeatMode = _repeatMode.asStateFlow()
    
    // Current username
    private var _currentUsername = MutableStateFlow("")
    val currentUsername = _currentUsername.asStateFlow()

    private var timeOffset: Long = 0L

    @Synchronized
    fun getSocket(): Socket? {
        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.forceNew = true
                opts.reconnection = true
                mSocket = IO.socket(BASE_URL, opts)
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
        
        socket.on("room:state") { args ->
             if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                updatePlayerState(data)
                _shuffleEnabled.value = data.optBoolean("shuffleEnabled", false)
                _repeatMode.value = data.optString("repeatMode", "off")
             }
        }

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
                val message = parseMessage(data)
                _messages.value = _messages.value + message
            }
        }
        
        socket.on("chat:history") { args ->
            if (args.isNotEmpty()) {
                val jsonArray = args[0] as JSONArray
                val newMessages = ArrayList<ChatMessage>()
                for (i in 0 until jsonArray.length()) {
                    newMessages.add(parseMessage(jsonArray.getJSONObject(i)))
                }
                _messages.value = newMessages
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
        
        socket.on("player:shuffle:update") { args ->
            if (args.isNotEmpty()) {
                _shuffleEnabled.value = args[0] as Boolean
            }
        }
        
        socket.on("player:repeat:update") { args ->
            if (args.isNotEmpty()) {
                _repeatMode.value = args[0] as String
            }
        }
        
        socket.on("ntp:pong") { args ->
             if (args.isNotEmpty()) {
                 val data = args[0] as JSONObject
                 val t1 = data.optLong("t1")
                 val t2 = data.optLong("t2")
                 val t0 = data.optLong("t0")
                 val t3 = System.currentTimeMillis()
                 timeOffset = ((t1 - t0) + (t2 - t3)) / 2
             }
        }
    }
    
    private fun updatePlayerState(data: JSONObject) {
        val isPlaying = data.optBoolean("isPlaying", false)
        val startTime = data.optLong("startTime", 0L)
        val songJson = data.optJSONObject("currentSong")
        
        val now = System.currentTimeMillis()
        val serverNow = now - timeOffset
        var position = if (isPlaying && startTime > 0) {
            serverNow - startTime
        } else {
            data.optLong("position", 0L)
        }
        
        if (position < 0) position = 0

        val song = if (songJson != null) parseSong(songJson) else null
        
        _playerState.value = PlayerState(
            currentSong = song,
            isPlaying = isPlaying,
            currentPosition = position,
            lastSyncTime = now
        )
        
        scope.launch {
            if (song != null) {
                _isLoadingStream.value = true
                val videoId = song.id
                val streamUrl = getVideoStreamUrl(videoId)
                _isLoadingStream.value = false
                
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
    
    // Use Piped API for YouTube audio extraction
    private suspend fun getVideoStreamUrl(videoId: String): String {
        if (videoId == "test") return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        
        try {
            val response = apiService.getStreamUrl(videoId)
            return response.url
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to direct Piped API call
            try {
                val url = java.net.URL("https://pipedapi.kavin.rocks/streams/$videoId")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                
                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                val json = JSONObject(response)
                val audioStreams = json.optJSONArray("audioStreams")
                if (audioStreams != null && audioStreams.length() > 0) {
                    // Get highest bitrate audio
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
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    private fun parseMessage(data: JSONObject): ChatMessage {
        return ChatMessage(
            id = data.optString("id", UUID.randomUUID().toString()),
            senderName = data.optString("senderName", "Unknown"),
            text = data.optString("text", ""),
            timestamp = data.optLong("timestamp", System.currentTimeMillis())
        )
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
    
    fun setUsername(username: String) {
        _currentUsername.value = username
    }

    // Actions
    fun joinRoom(roomName: String, username: String) {
        _currentUsername.value = username
        _messages.value = emptyList() // Clear previous room's messages
        _queue.value = emptyList()
        _users.value = emptyList()
        
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
    
    fun seekTo(positionMs: Long) {
        mSocket?.emit("player:seek", positionMs)
        audioPlayer?.seekTo(positionMs)
    }
    
    fun toggleShuffle() {
        val newValue = !_shuffleEnabled.value
        _shuffleEnabled.value = newValue
        mSocket?.emit("player:shuffle", newValue)
    }
    
    fun cycleRepeatMode() {
        val modes = listOf("off", "all", "one")
        val currentIndex = modes.indexOf(_repeatMode.value)
        val newMode = modes[(currentIndex + 1) % modes.size]
        _repeatMode.value = newMode
        mSocket?.emit("player:repeat", newMode)
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
        data.put("coverUrl", song.coverUrl)
        mSocket?.emit("queue:add", data)
    }
    
    fun removeFromQueue(index: Int) {
        mSocket?.emit("queue:remove", index)
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
            _isSearching.value = true
            try {
                val response = apiService.search(query)
                val songs = response.content.map { item ->
                    Song(
                        id = item.videoId,
                        title = item.name,
                        artist = item.artist?.name ?: "Unknown",
                        duration = (item.duration ?: 0) * 1000L, // Convert seconds to ms
                        coverUrl = item.thumbnails?.lastOrNull()?.url
                    )
                }
                _searchResults.value = songs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
    fun refreshRooms() {
        scope.launch {
            _isLoadingRooms.value = true
            try {
                val response = apiService.getRooms()
                val rooms = response.mapIndexed { index, item ->
                    Room(
                        id = index,
                        roomId = item.id,
                        countryFlag = item.countryFlag,
                        vibe = item.vibe,
                        username = "Host",
                        roomName = item.name,
                        songs = if (item.currentSong != null) listOf("${item.currentSong.title} - ${item.currentSong.artist}") else emptyList(),
                        totalSongs = 0,
                        userCount = item.userCount,
                        flagColor = Color(0xFFE3F2FD)
                    )
                }
                _publicRooms.value = rooms
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingRooms.value = false
            }
        }
    }
    
    suspend fun generateNames(): Pair<String, String> {
        return try {
            val response = apiService.generateNames()
            Pair(response.roomName, response.username)
        } catch (e: Exception) {
            // Fallback local generation
            val adj = listOf("Cool", "Happy", "Lazy", "Silent", "Neon", "Cyber", "Retro", "Zen")
            val noun = listOf("Cat", "Fox", "Panda", "Wolf", "Ghost", "Surfer", "Pilot", "Ninja")
            val roomAdj = listOf("Neon", "Cosmic", "Chill", "Electric", "Midnight", "Golden")
            val roomNoun = listOf("Vibes", "Dreams", "Beats", "Waves", "Lounge", "Station")
            
            val username = "${adj.random()}-${noun.random()}"
            val roomName = "${roomAdj.random()} ${roomNoun.random()}"
            Pair(roomName, username)
        }
    }
    
    suspend fun createRoom(name: String, vibe: String, isPrivate: Boolean, hostUsername: String): String? {
        return try {
            val response = apiService.createRoom(CreateRoomRequest(name, vibe, isPrivate, hostUsername))
            response.roomId
        } catch (e: Exception) {
            e.printStackTrace()
            // Generate local 6-digit ID as fallback
            String.format("%06d", (100000..999999).random())
        }
    }
}
