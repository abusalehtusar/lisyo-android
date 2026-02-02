package dev.abu.material3.data.api

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketManager {
    private var mSocket: Socket? = null

    @Synchronized
    fun getSocket(): Socket? {
        if (mSocket == null) {
            try {
                // Production URL from Railway
                val opts = IO.Options()
                opts.forceNew = true
                opts.reconnection = true
                
                // Using the provided URL (Standard HTTPS port 443)
                mSocket = IO.socket("https://lisyo-backend-production.up.railway.app", opts)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }
        return mSocket
    }

    fun establishConnection() {
        val socket = getSocket()
        socket?.connect()
    }

    fun closeConnection() {
        val socket = getSocket()
        socket?.disconnect()
    }
}
