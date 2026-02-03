package dev.abu.material3.utils

import android.content.Context
import android.util.Log

object Logger {
    private var isInitialized = false

    fun init(context: Context) {
        isInitialized = true
    }

    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }
}