package dev.abu.material3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.material3.data.api.SocketManager
import dev.abu.material3.data.model.ChatMessage
import dev.abu.material3.data.model.Song
import dev.abu.material3.ui.theme.inter
import dev.abu.material3.ui.theme.jetbrainsMono
import java.util.UUID

@Composable
fun PlayerScreen(
    roomName: String,
    onLeave: () -> Unit
) {
    val playerState by SocketManager.playerState.collectAsState()
    val queue by SocketManager.queue.collectAsState()
    val messages by SocketManager.messages.collectAsState()
    val users by SocketManager.users.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Chat", "Session")
    val icons = listOf(Icons.Default.MusicNote, Icons.Default.Chat, Icons.Default.Group)

    LaunchedEffect(roomName) {
        SocketManager.establishConnection()
        SocketManager.joinRoom(roomName, "User-${(100..999).random()}") // Random username for now
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLeave) {
                        Icon(Icons.Default.KeyboardArrowDown, "Leave")
                    }
                    Text(
                        text = roomName,
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(48.dp)) // Balance
                }
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontFamily = inter) },
                            icon = { Icon(icons[index], null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> SongsTab(playerState.isPlaying, playerState.currentSong, queue)
                1 -> ChatTab(messages)
                2 -> SessionTab(users, roomName)
            }
        }
    }
}

@Composable
fun SongsTab(isPlaying: Boolean, currentSong: Song?, queue: List<Song>) {
    var searchQuery by remember { mutableStateOf("") }
    var currentProgress by remember { mutableStateOf(0L) }
    val searchResults by SocketManager.searchResults.collectAsState()
    
    // Local timer to update progress bar smoothly
    LaunchedEffect(isPlaying, currentSong) {
        if (isPlaying && currentSong != null) {
            val startTime = System.currentTimeMillis()
            val startPos = SocketManager.getCurrentPosition()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                currentProgress = startPos + elapsed
                kotlinx.coroutines.delay(1000) // Update every second
            }
        } else if (!isPlaying) {
             currentProgress = SocketManager.getCurrentPosition()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it 
                if (it.length > 2) {
                     SocketManager.search(it)
                } else if (it.isEmpty()) {
                     // Clear results if needed
                }
            },
            placeholder = { Text("Search songs...", fontFamily = inter) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(Modifier.height(16.dp))

        // Content: Search Results OR Queue
        if (searchQuery.isNotEmpty()) {
            Text("Search Results", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { song ->
                    SongItem(song, onClick = { 
                        SocketManager.playSong(song)
                        searchQuery = "" // Clear search after playing
                    })
                }
            }
        } else {
             Text("Queue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
             Spacer(Modifier.height(8.dp))
             // Queue
             LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue) { song ->
                    SongItem(song, onClick = { /* Maybe remove or vote skip? */ })
                }
                if (queue.isEmpty()) {
                    item {
                        Text(
                            "Queue is empty",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            fontFamily = jetbrainsMono,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        
        // Player Controls
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = currentSong?.title ?: "No Song Playing",
                    fontFamily = jetbrainsMono,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontFamily = inter,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(Modifier.height(12.dp))
                
                val duration = currentSong?.duration ?: 1L
                val progressFraction = (currentProgress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { SocketManager.previous() }) {
                        Icon(Icons.Default.SkipPrevious, "Prev")
                    }
                    IconButton(
                        onClick = { if (isPlaying) SocketManager.pause() else SocketManager.resume() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { SocketManager.next() }) {
                        Icon(Icons.Default.SkipNext, "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(song.title, fontFamily = jetbrainsMono, fontWeight = FontWeight.SemiBold)
            Text(song.artist, fontFamily = inter, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ChatTab(messages: List<ChatMessage>) {
    var text by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(msg.text, fontFamily = inter)
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Say something...") },
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = { 
                if (text.isNotBlank()) {
                    SocketManager.sendMessage(text, "Me")
                    text = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
    }
}

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun SessionTab(users: List<dev.abu.material3.data.model.SessionUser>, roomName: String) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Room Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Room ID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(roomName, fontFamily = jetbrainsMono, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                IconButton(onClick = { 
                    clipboardManager.setText(AnnotatedString(roomName)) 
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy ID", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        
        Text("Active Users (${users.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(users) { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(user.username, fontFamily = jetbrainsMono, fontWeight = FontWeight.Bold)
                        if (user.isHost) {
                            Text("HOST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

