package dev.abu.material3.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.abu.material3.data.api.SocketManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLoginClick: () -> Unit) {
    val youtubeCookie by SocketManager.youtubeCookie.collectAsState()
    val isLoggedIn = youtubeCookie != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("YouTube Login") },
                supportingContent = { 
                    Text(if (isLoggedIn) "Logged in successfully" else "Login to access your library and playlists")
                },
                trailingContent = {
                    if (isLoggedIn) {
                        IconButton(onClick = { SocketManager.setYoutubeCookie(null) }) {
                            Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = onLoginClick) {
                            Text("Login")
                        }
                    }
                }
            )
        }
    }
}
