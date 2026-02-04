package dev.abu.lisyo.data.api

import dev.abu.lisyo.data.model.ChatMessage
import dev.abu.lisyo.data.model.PlayerState
import dev.abu.lisyo.data.model.SessionUser
import dev.abu.lisyo.data.model.Song
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

import dev.abu.lisyo.ui.screens.Room
import dev.abu.lisyo.ui.screens.getVibeColor
import dev.abu.lisyo.utils.Logger
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.ui.graphics.Color

object SocketManager {
    private var mSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private const val DEFAULT_BASE_URL = "https://lisyo-backend-production-1acf.up.railway.app/"
    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl = _baseUrl.asStateFlow()
    
    // API Services
    private var retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private var apiService = retrofit.create(LisyoApiService::class.java)
    
    fun getApiService(): LisyoApiService = apiService

    private fun rebuildApiService(newUrl: String) {
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(if (newUrl.endsWith("/")) newUrl else "$newUrl/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(LisyoApiService::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    // My Hosted Rooms (fetched from backend)
    private val _myHostedRooms = MutableStateFlow<List<Room>>(emptyList())
    val myHostedRooms = _myHostedRooms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _users = MutableStateFlow<List<SessionUser>>(emptyList())
    val users = _users.asStateFlow()
    
    // Loading States
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms = _isLoadingRooms.asStateFlow()
    
    // Shuffle and Repeat
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()
    
    private val _repeatMode = MutableStateFlow("off") // "off", "all", "one"
    val repeatMode = _repeatMode.asStateFlow()
    
    // Current username
    private var _currentUsername = MutableStateFlow("")
    val currentUsername = _currentUsername.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }
    
    // Join History
    private val _joinHistory = MutableStateFlow<List<RoomHistoryItem>>(emptyList())
    val joinHistory = _joinHistory.asStateFlow()
    
    // YouTube Login Cookies
    private val _youtubeCookie = MutableStateFlow<String?>(null)
    val youtubeCookie = _youtubeCookie.asStateFlow()
    
    private var prefs: android.content.SharedPreferences? = null

    fun setYoutubeCookie(cookie: String?) {
        _youtubeCookie.value = cookie
        prefs?.edit()?.putString("yt_cookie", cookie)?.apply()
        dev.abu.lisyo.innertube.YouTube.cookie = cookie
    }
    
    // Track current playing song ID to avoid redundant updates
    private var currentVideoId: String? = null

    private var timeOffset: Long = 0L

    @Synchronized
    fun getSocket(): Socket? {
        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.forceNew = true
                opts.reconnection = true
                mSocket = IO.socket(_baseUrl.value, opts)
                initListeners()
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
        return mSocket
    }

    private var audioPlayer: dev.abu.lisyo.player.AudioPlayer? = null

    // My Created Rooms
    private val _myRooms = MutableStateFlow<List<String>>(emptyList())
    val myRooms = _myRooms.asStateFlow()

    fun init(context: android.content.Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("lisyo_prefs", android.content.Context.MODE_PRIVATE)
            
            // Load custom base URL
            val savedUrl = prefs?.getString("base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
            _baseUrl.value = savedUrl
            rebuildApiService(savedUrl)

            val cookie = prefs?.getString("yt_cookie", null)
            _youtubeCookie.value = cookie
            dev.abu.lisyo.innertube.YouTube.cookie = cookie
            
            // Load persistent username
            val savedUsername = prefs?.getString("username", "") ?: ""
            _currentUsername.value = savedUsername

            // Load join history
            val historyJson = prefs?.getString("join_history", "[]") ?: "[]"
            try {
                val array = JSONArray(historyJson)
                val history = mutableListOf<RoomHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    history.add(RoomHistoryItem(obj.getString("roomId"), obj.getLong("timestamp")))
                }
                _joinHistory.value = history
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Load my rooms
            val myRoomsJson = prefs?.getString("my_rooms", "[]") ?: "[]"
            try {
                val array = JSONArray(myRoomsJson)
                val rooms = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    rooms.add(array.getString(i))
                }
                _myRooms.value = rooms
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (audioPlayer == null) {
            audioPlayer = dev.abu.lisyo.player.AudioPlayer(context.applicationContext)
            audioPlayer?.initialize()
            
            scope.launch {
                audioPlayer?.isLoading?.collect { loading ->
                    val currentPos = getCurrentPosition()
                    _playerState.value = _playerState.value.copy(
                        isLoading = loading,
                        currentPosition = currentPos,
                        lastSyncTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun initListeners() {
        val socket = mSocket ?: return

        socket.on(Socket.EVENT_CONNECT) {
            Logger.logInfo("SocketManager", "Socket connected")
            syncTime()
            
            // Re-join room if we have one in state
            val currentRoomId = _playerState.value.roomId
            val username = _currentUsername.value
            if (currentRoomId.isNotEmpty() && username.isNotEmpty()) {
                Logger.logInfo("SocketManager", "Auto-joining room on connect: $currentRoomId")
                val data = JSONObject()
                data.put("room", currentRoomId)
                data.put("username", username)
                socket.emit("join:room", data)
            }
        }
        
        socket.on("room:state") { args ->
             if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val serverRoomId = data.optString("id", "")
                _playerState.value = _playerState.value.copy(roomId = serverRoomId)
                _shuffleEnabled.value = data.optBoolean("shuffleEnabled", false)
                _repeatMode.value = data.optString("repeatMode", "off")
                updatePlayerState(data)
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
                val currentQueueMap = _queue.value.associateBy { it.id }
                
                for (i in 0 until jsonArray.length()) {
                    var s = parseSong(jsonArray.getJSONObject(i))
                    if (s.duration == 0L) {
                        currentQueueMap[s.id]?.duration?.let {
                            if (it > 0L) s = s.copy(duration = it)
                        }
                    }
                    newQueue.add(s)
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

        socket.on("room:terminated") {
            // Room was terminated by host, leave it
            _playerState.value = _playerState.value.copy(roomId = "")
            _queue.value = emptyList()
            _messages.value = emptyList()
            // We could trigger a navigation back to home here if we had a reference
        }

        socket.on("room:error") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val rawMessage = data.optString("message", "Error joining room")
                val message = if (rawMessage == "Room not found") "Room not found, Leave!" else rawMessage
                Logger.logError("SocketManager", "Join error: $message")
                _errorMessage.value = message
                
                // If rejoining my own room failed, remove it from my rooms list
                val currentId = _playerState.value.roomId.ifEmpty { 
                    // This is tricky because we cleared roomId in joinRoom. 
                    // We might need to track the 'pending' roomId.
                    "" 
                }
                
                // Let's just refresh all rooms to be safe
                refreshRooms()

                // Clear room ID so UI navigates back
                _playerState.value = _playerState.value.copy(roomId = "")
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
        val serverNow = now + timeOffset
        var position = if (isPlaying && startTime > 0) {
            serverNow - startTime
        } else {
            data.optLong("position", 0L)
        }
        
        if (position < 0) position = 0

        var song = if (songJson != null) parseSong(songJson) else null
        
        // Preserve duration if already known
        if (song != null && song.duration == 0L) {
            val current = _playerState.value.currentSong
            if (current?.id == song.id && current.duration > 0L) {
                song = song.copy(duration = current.duration)
            }
        }

        val wasPlaying = _playerState.value.isPlaying
        
        _playerState.value = _playerState.value.copy(
            currentSong = song,
            isPlaying = isPlaying,
            currentPosition = position,
            lastSyncTime = now
        )
        
        scope.launch {
            if (song != null) {
                val videoId = song.id
                
                if (videoId != currentVideoId || (isPlaying && !wasPlaying)) {
                    currentVideoId = videoId
                    withContext(Dispatchers.Main) {
                        audioPlayer?.play(videoId, position, isPlaying)
                        audioPlayer?.updateMetadata(song.title, song.artist)
                    }
                } else {
                    // Same song, just update play state
                    withContext(Dispatchers.Main) {
                        when {
                            isPlaying && !wasPlaying -> audioPlayer?.resume()
                            !isPlaying && wasPlaying -> audioPlayer?.pause()
                            else -> Unit
                        }
                    }
                }
            } else {
                currentVideoId = null
                withContext(Dispatchers.Main) {
                    audioPlayer?.pause()
                }
            }
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
        prefs?.edit()?.putString("username", username)?.apply()
    }

    fun setBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        _baseUrl.value = sanitized
        prefs?.edit()?.putString("base_url", sanitized)?.apply()
        rebuildApiService(sanitized)
        
        // Reset socket
        mSocket?.disconnect()
        mSocket = null
        establishConnection()
    }

    // Actions
    fun joinRoom(roomName: String, username: String) {
        _currentUsername.value = username
        _messages.value = emptyList() // Clear previous room's messages
        _queue.value = emptyList()
        _users.value = emptyList()
        _playerState.value = _playerState.value.copy(roomId = "") // Clear ID, wait for server

        // Update Join History
        val currentHistory = _joinHistory.value.toMutableList()
        currentHistory.removeAll { it.roomId == roomName }
        currentHistory.add(0, RoomHistoryItem(roomName, System.currentTimeMillis()))
        if (currentHistory.size > 10) {
            val trimmed = currentHistory.take(10)
            _joinHistory.value = trimmed
        } else {
            _joinHistory.value = currentHistory
        }
        
        // Persist History
        scope.launch {
            val array = JSONArray()
            _joinHistory.value.forEach { 
                val obj = JSONObject()
                obj.put("roomId", it.roomId)
                obj.put("timestamp", it.timestamp)
                array.put(obj)
            }
            prefs?.edit()?.putString("join_history", array.toString())?.apply()
        }
        
        val data = JSONObject()
        data.put("room", roomName)
        data.put("username", username)
        mSocket?.emit("join:room", data)
    }

    fun leaveRoom() {
        val currentRoomId = _playerState.value.roomId
        if (currentRoomId.isNotEmpty()) {
            val data = JSONObject()
            data.put("room", currentRoomId)
            data.put("username", _currentUsername.value)
            mSocket?.emit("leave:room", data)
        }
        
        // Clear local state
        _playerState.value = PlayerState()
        _queue.value = emptyList()
        _messages.value = emptyList()
        _users.value = emptyList()
        currentVideoId = null
        
        scope.launch {
            withContext(Dispatchers.Main) {
                audioPlayer?.pause()
            }
        }
    }

    fun playSong(song: Song) {
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("id", song.id)
        data.put("title", song.title)
        data.put("artist", song.artist)
        data.put("duration", song.duration)
        data.put("coverUrl", song.coverUrl)
        mSocket?.emit("player:play", data)
    }

    // Debounce play/pause to prevent rapid clicks causing state issues
    private var lastPlayPauseTime = 0L
    private const val PLAY_PAUSE_DEBOUNCE_MS = 300L
    
    fun pause() {
        val now = System.currentTimeMillis()
        if (now - lastPlayPauseTime < PLAY_PAUSE_DEBOUNCE_MS) return
        lastPlayPauseTime = now
        
        if (!_playerState.value.isPlaying) return // Already paused
        
        val currentPos = getCurrentPosition()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            currentPosition = currentPos,
            lastSyncTime = now
        )
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        mSocket?.emit("player:pause", data)
        audioPlayer?.pause()
    }

    fun resume() {
        val now = System.currentTimeMillis()
        if (now - lastPlayPauseTime < PLAY_PAUSE_DEBOUNCE_MS) return
        lastPlayPauseTime = now
        
        if (_playerState.value.currentSong == null) return
        if (_playerState.value.isPlaying) return // Already playing
        
        _playerState.value = _playerState.value.copy(
            isPlaying = true,
            lastSyncTime = now
        )
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        mSocket?.emit("player:resume", data)
        audioPlayer?.resume()
    }
    
    fun next() {
        if (_queue.value.isEmpty()) return
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        mSocket?.emit("player:next", data)
    }
    
    fun previous() {
        if (_queue.value.isEmpty()) return
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        mSocket?.emit("player:previous", data)
    }
    
    fun seekTo(positionMs: Long) {
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("position", positionMs)
        mSocket?.emit("player:seek", data)
        _playerState.value = _playerState.value.copy(
            currentPosition = positionMs,
            lastSyncTime = System.currentTimeMillis()
        )
        audioPlayer?.seekTo(positionMs)
    }
    
    fun toggleShuffle() {
        val newValue = !_shuffleEnabled.value
        _shuffleEnabled.value = newValue
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("enabled", newValue)
        mSocket?.emit("player:shuffle", data)
    }
    
    fun cycleRepeatMode() {
        val modes = listOf("off", "all", "one")
        val currentIndex = modes.indexOf(_repeatMode.value)
        val newMode = modes[(currentIndex + 1) % modes.size]
        _repeatMode.value = newMode
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("mode", newMode)
        mSocket?.emit("player:repeat", data)
    }
    
    fun playFromQueue(index: Int) {
        val queueList = _queue.value
        if (index < 0 || index >= queueList.size) return
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("index", index)
        mSocket?.emit("queue:play", data)
    }

    fun sendMessage(text: String, username: String) {
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("text", text)
        data.put("senderName", username)
        data.put("timestamp", System.currentTimeMillis())
        mSocket?.emit("chat:send", data)
    }
    
    fun addToQueue(song: Song) {
        if (mSocket?.connected() != true) return
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("id", song.id)
        data.put("title", song.title)
        data.put("artist", song.artist)
        data.put("duration", song.duration)
        data.put("coverUrl", song.coverUrl)
        mSocket?.emit("queue:add", data)
    }
    
    fun removeFromQueue(index: Int) {
        val data = JSONObject()
        data.put("roomId", _playerState.value.roomId)
        data.put("index", index)
        mSocket?.emit("queue:remove", data)
    }

    fun updateSongDuration(videoId: String, durationMs: Long) {
        val state = _playerState.value
        var changed = false
        if (state.currentSong?.id == videoId && state.currentSong.duration == 0L) {
            _playerState.value = state.copy(
                currentSong = state.currentSong.copy(duration = durationMs)
            )
            changed = true
        }
        
        // Also update in queue if present
        val currentQueue = _queue.value.toMutableList()
        var updatedInQueue = false
        for (i in currentQueue.indices) {
            if (currentQueue[i].id == videoId && currentQueue[i].duration == 0L) {
                currentQueue[i] = currentQueue[i].copy(duration = durationMs)
                updatedInQueue = true
            }
        }
        if (updatedInQueue) {
            _queue.value = currentQueue
            changed = true
        }

        if (changed) {
            val data = JSONObject()
            data.put("videoId", videoId)
            data.put("duration", durationMs)
            mSocket?.emit("player:duration", data)
        }
    }

    suspend fun ping(): Boolean {
        return try {
            apiService.getRooms()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun syncTime() {
        val t0 = System.currentTimeMillis()
        val data = JSONObject()
        data.put("t0", t0)
        mSocket?.emit("ntp:ping", data)
    }
    
    fun getCurrentPosition(): Long {
        val state = _playerState.value
        if (!state.isPlaying || state.isLoading) return state.currentPosition
        
        val elapsed = System.currentTimeMillis() - state.lastSyncTime
        return state.currentPosition + elapsed
    }
    
    // API Actions
    fun search(query: String) {
        scope.launch {
            _isSearching.value = true
            try {
                val songs = dev.abu.lisyo.player.YouTubeMusicSearchService.search(query)
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
                _publicRooms.value = response.mapIndexed { index, item -> 
                    mapRoomResponse(index, item) 
                }
                
                // Also refresh my hosted rooms if username is available
                val username = _currentUsername.value
                if (username.isNotBlank()) {
                    refreshMyRooms(username)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingRooms.value = false
            }
        }
    }

    private fun refreshMyRooms(username: String) {
        scope.launch {
            try {
                val response = apiService.getMyRooms(username)
                val rooms = response.mapIndexed { index, item ->
                    mapRoomResponse(index, item)
                }
                _myHostedRooms.value = rooms
                
                // Sync the local IDs list with server reality
                val serverIds = response.map { it.id }
                _myRooms.value = serverIds
                prefs?.edit()?.putString("my_rooms", JSONArray(serverIds).toString())?.apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mapRoomResponse(index: Int, item: RoomResponse): Room {
        val queueSongs = mutableListOf<String>()
        item.currentSong?.let { 
            queueSongs.add("${it.title} - ${it.artist}")
        }
        item.queuePreview?.forEach { song ->
            queueSongs.add("${song.title} - ${song.artist}")
        }
        return Room(
            id = index,
            roomId = item.id,
            countryFlag = item.countryFlag,
            vibe = item.vibe,
            username = "Host",
            roomName = item.name,
            songs = queueSongs.take(5),
            totalSongs = item.totalSongs,
            userCount = item.userCount,
            flagColor = getVibeColor(item.vibe)
        )
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
    
    suspend fun createRoom(name: String, vibe: String, isPrivate: Boolean, hostUsername: String, countryFlag: String = "🌐"): String? {
        return try {
            val response = apiService.createRoom(CreateRoomRequest(name, vibe, isPrivate, hostUsername, countryFlag))
            val roomId = response.roomId
            
            // Save to my rooms
            val updated = _myRooms.value.toMutableList()
            if (!updated.contains(roomId)) {
                updated.add(0, roomId)
                _myRooms.value = updated
                prefs?.edit()?.putString("my_rooms", JSONArray(updated).toString())?.apply()
            }
            
            roomId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun terminateRoom(roomId: String) {
        val data = JSONObject()
        data.put("roomId", roomId)
        mSocket?.emit("room:terminate", data)
        
        // Remove from my rooms locally
        val updated = _myRooms.value.toMutableList()
        updated.remove(roomId)
        _myRooms.value = updated
        prefs?.edit()?.putString("my_rooms", JSONArray(updated).toString())?.apply()
    }

    suspend fun getCountryFlag(): String {
        return try {
            val response = apiService.getLocation()
            response.countryFlag
        } catch (e: Exception) {
            "🌐"
        }
    }
}data class RoomHistoryItem(val roomId: String, val timestamp: Long)
