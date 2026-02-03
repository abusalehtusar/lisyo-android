package dev.abu.material3.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.material3.data.api.SocketManager
import dev.abu.material3.data.model.ChatMessage
import dev.abu.material3.data.model.Song
import dev.abu.material3.ui.theme.inter
import dev.abu.material3.ui.theme.jetbrainsMono
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    roomId: String,
    username: String,
    onLeave: () -> Unit
) {
    val playerState by SocketManager.playerState.collectAsState()
    val queue by SocketManager.queue.collectAsState()
    val messages by SocketManager.messages.collectAsState()
    val users by SocketManager.users.collectAsState()
    val shuffleEnabled by SocketManager.shuffleEnabled.collectAsState()
    val repeatMode by SocketManager.repeatMode.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    BackHandler {
        onLeave()
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Chat", "Session")
    val icons = listOf(Icons.Default.MusicNote, Icons.Default.Chat, Icons.Default.Group)

    LaunchedEffect(roomId, username) {
        SocketManager.establishConnection()
        SocketManager.joinRoom(roomId, username)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = roomId,
                            fontFamily = jetbrainsMono,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(48.dp))
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
                0 -> SongsTab(
                    playerState = playerState,
                    queue = queue,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode
                )
                1 -> ChatTab(messages = messages, username = username)
                2 -> SessionTab(users = users, roomId = roomId)
            }
        }
    }
}

@Composable
fun SongsTab(
    playerState: dev.abu.material3.data.model.PlayerState,
    queue: List<Song>,
    shuffleEnabled: Boolean,
    repeatMode: String
) {
    val isPlaying = playerState.isPlaying
    val currentSong = playerState.currentSong
    var searchQuery by remember { mutableStateOf("") }
    var currentProgress by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val searchResults by SocketManager.searchResults.collectAsState()
    val isSearching by SocketManager.isSearching.collectAsState()
    
    // Update progress every 500ms
    LaunchedEffect(isPlaying, currentSong) {
        while (true) {
            if (!isSeeking) {
                currentProgress = SocketManager.getCurrentPosition()
            }
            delay(500)
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
                    SocketManager.clearSearchResults()
                }
            },
            placeholder = { Text("Search songs...", fontFamily = inter) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
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
                    SongItem(
                        song = song,
                        onClick = { 
                            // Click adds to queue and clears search
                            SocketManager.addToQueue(song)
                            searchQuery = ""
                            SocketManager.clearSearchResults()
                        },
                        onAddToQueue = {
                            SocketManager.addToQueue(song)
                        }
                    )
                }
                if (searchResults.isEmpty() && !isSearching && searchQuery.length > 2) {
                    item {
                        Text(
                            "No results found",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            fontFamily = inter,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Queue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("${queue.size} songs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue.size) { index ->
                    val song = queue[index]
                    val isCurrentSong = currentSong?.id == song.id
                    QueueItem(
                        song = song,
                        index = index,
                        isCurrentSong = isCurrentSong,
                        onClick = { SocketManager.playFromQueue(index) },
                        onRemove = { SocketManager.removeFromQueue(index) }
                    )
                }
                if (queue.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Queue is empty",
                                fontFamily = jetbrainsMono,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Search and add songs!",
                                fontFamily = inter,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = currentSong?.artist ?: "Unknown Artist",
                    fontFamily = inter,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                
                Spacer(Modifier.height(12.dp))
                
                val duration = (currentSong?.duration ?: 0L).coerceAtLeast(1L)
                val progressFraction = if (isSeeking) {
                    seekPosition
                } else {
                    val fraction = currentProgress.toFloat() / duration.toFloat()
                    if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
                }
                
                // Seekable Progress Slider
                Box(contentAlignment = Alignment.Center) {
                    Slider(
                        value = progressFraction,
                        onValueChange = { 
                            isSeeking = true
                            seekPosition = it
                        },
                        onValueChangeFinished = {
                            val newPosition = (seekPosition * duration).toLong()
                            SocketManager.seekTo(newPosition)
                            currentProgress = newPosition
                            isSeeking = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    if (playerState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(if (isSeeking) (seekPosition * duration).toLong() else currentProgress),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = jetbrainsMono
                    )
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = jetbrainsMono
                    )
                }
                
                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { SocketManager.toggleShuffle() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Shuffle, "Shuffle")
                    }
                    
                    // Previous
                    IconButton(onClick = { SocketManager.previous() }) {
                        Icon(Icons.Default.SkipPrevious, "Previous")
                    }
                    
                    // Play/Pause
                    IconButton(
                        onClick = { if (isPlaying) SocketManager.pause() else SocketManager.resume() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Next
                    IconButton(onClick = { SocketManager.next() }) {
                        Icon(Icons.Default.SkipNext, "Next")
                    }
                    
                    // Repeat
                    IconButton(
                        onClick = { SocketManager.cycleRepeatMode() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (repeatMode != "off") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            when (repeatMode) {
                                "one" -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            "Repeat: $repeatMode"
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun SongItem(song: Song, onClick: () -> Unit, onAddToQueue: () -> Unit) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, fontFamily = jetbrainsMono, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(song.artist, fontFamily = inter, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconButton(onClick = onAddToQueue) {
            Icon(Icons.Default.Add, "Add to queue", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun QueueItem(song: Song, index: Int, isCurrentSong: Boolean, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceContainer, 
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${index + 1}",
            fontFamily = jetbrainsMono,
            color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title, 
                fontFamily = jetbrainsMono, 
                fontWeight = FontWeight.SemiBold, 
                maxLines = 1,
                color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                song.artist, 
                fontFamily = inter, 
                style = MaterialTheme.typography.bodySmall, 
                maxLines = 1,
                color = if (isCurrentSong) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isCurrentSong) {
            Icon(
                Icons.Default.MusicNote,
                null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChatTab(messages: List<ChatMessage>, username: String) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isMe = msg.senderName == username
                ChatBubble(message = msg, isMe = isMe)
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Say something...", fontFamily = inter) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    if (text.isNotBlank()) {
                        SocketManager.sendMessage(text, username)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Username label above bubble
        Text(
            text = message.senderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = jetbrainsMono,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )
        
        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                fontFamily = inter,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionTab(users: List<dev.abu.material3.data.model.SessionUser>, roomId: String) {
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
                    Text(
                        roomId,
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        letterSpacing = 4.sp
                    )
                }
                IconButton(onClick = { 
                    clipboardManager.setText(AnnotatedString(roomId)) 
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy ID", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        
        Text("Active Users (${users.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(users) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.username.firstOrNull()?.uppercase() ?: "?",
                            fontFamily = jetbrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.username, fontFamily = jetbrainsMono, fontWeight = FontWeight.Bold)
                        if (user.isHost) {
                            Text(
                                "HOST",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontFamily = jetbrainsMono
                            )
                        }
                    }
                }
            }
        }
    }
}