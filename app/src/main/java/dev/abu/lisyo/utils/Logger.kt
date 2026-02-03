package dev.abu.lisyo.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val TAG = "LisyoLogger"
    private var logFile: File? = null

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null)
        if (dir != null) {
            logFile = File(dir, "errors.log")
            if (!logFile!!.exists()) {
                logFile!!.createNewFile()
            }
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeToLogFile("ERROR", tag, message, throwable)
    }

    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
        writeToLogFile("INFO", tag, message, null)
    }

    fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
        // Optionally log debug to file as well
        // writeToLogFile("DEBUG", tag, message, null)
    }

    private fun writeToLogFile(level: String, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val writer = FileWriter(file, true)
            writer.append("$timestamp [$level] $tag: $message\n")
            throwable?.let {
                writer.append(Log.getStackTraceString(it))
                writer.append("\n")
            }
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
}
