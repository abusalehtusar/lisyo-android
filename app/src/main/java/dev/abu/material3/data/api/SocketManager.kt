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

object SocketManager {
    private var mSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // State Flows
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState = _playerState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()

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

    private fun initListeners() {
        val socket = mSocket ?: return

        socket.on(Socket.EVENT_CONNECT) {
            syncTime()
        }

        socket.on("player:update") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val isPlaying = data.optBoolean("isPlaying", false)
                val position = data.optLong("position", 0L)
                val songJson = data.optJSONObject("currentSong")
                val timestamp = data.optLong("timestamp", System.currentTimeMillis())

                val song = if (songJson != null) parseSong(songJson) else null
                
                // Adjust position based on network time
                val estimatedPosition = if (isPlaying) {
                    val now = System.currentTimeMillis() + timeOffset
                    position + (now - timestamp)
                } else {
                    position
                }

                _playerState.value = PlayerState(
                    currentSong = song,
                    isPlaying = isPlaying,
                    currentPosition = estimatedPosition,
                    lastSyncTime = System.currentTimeMillis()
                )
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
}
