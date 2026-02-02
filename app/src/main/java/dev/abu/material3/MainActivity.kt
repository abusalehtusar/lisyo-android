package dev.abu.material3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.abu.material3.data.api.SocketManager
import dev.abu.material3.player.MediaPlaybackService
import dev.abu.material3.ui.MainScreen
import dev.abu.material3.ui.theme.LisyoTheme
import dev.abu.material3.utils.Logger

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        Logger.init(this)
        Logger.logInfo(TAG, "App started, Logger initialized")
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.logError(TAG, "Uncaught exception in thread ${thread.name}", throwable)
        }
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
        
        // Start media playback service
        val serviceIntent = Intent(this, MediaPlaybackService::class.java)
        startService(serviceIntent)
        
        SocketManager.init(this)
        SocketManager.establishConnection()
        
        setContent {
            LisyoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
