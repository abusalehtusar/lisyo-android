package dev.abu.lisyo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abu.lisyo.data.api.SocketManager
import dev.abu.lisyo.player.MediaPlaybackService
import dev.abu.lisyo.ui.MainScreen
import dev.abu.lisyo.ui.theme.LisyoTheme
import dev.abu.lisyo.ui.theme.inter
import dev.abu.lisyo.ui.theme.jetbrainsMono
import dev.abu.lisyo.utils.Logger

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
                val username by SocketManager.currentUsername.collectAsState()
                var showUsernameDialog by remember { mutableStateOf(username.isEmpty()) }
                var tempUsername by remember { mutableStateOf("") }

                if (showUsernameDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Don't allow dismiss if no username */ },
                        title = {
                            Text(
                                "Welcome to Lisyo",
                                fontFamily = jetbrainsMono,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    "Please set a username to start listening together.",
                                    fontFamily = inter,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                OutlinedTextField(
                                    value = tempUsername,
                                    onValueChange = { tempUsername = it },
                                    label = { Text("Username", fontFamily = inter) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (tempUsername.isNotBlank()) {
                                        SocketManager.setUsername(tempUsername)
                                        showUsernameDialog = false
                                    }
                                },
                                enabled = tempUsername.isNotBlank()
                            ) {
                                Text("Save", fontFamily = inter)
                            }
                        }
                    )
                }

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
