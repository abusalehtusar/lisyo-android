package dev.abu.lisyo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.lisyo.data.api.SocketManager
import dev.abu.lisyo.ui.theme.inter
import dev.abu.lisyo.ui.theme.jetbrainsMono
import kotlinx.coroutines.launch

@Composable
fun CreateScreen(onJoin: (String, String) -> Unit) {
    val username by SocketManager.currentUsername.collectAsState()
    var roomName by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Lofi") }
    var isPrivate by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(true) }
    var countryFlag by remember { mutableStateOf("🌐") }
    
    val scope = rememberCoroutineScope()
    val genres = listOf("Lofi", "Pop", "Jazz", "Rock", "Techno", "K-Pop", "Classical")

    // Generate random room name and get country flag on first load
    LaunchedEffect(Unit) {
        isGenerating = true
        val (generatedRoom, _) = SocketManager.generateNames()
        roomName = generatedRoom
        countryFlag = SocketManager.getCountryFlag()
        isGenerating = false
    }

    fun regenerateRoomName() {
        scope.launch {
            isGenerating = true
            val (generatedRoom, _) = SocketManager.generateNames()
            roomName = generatedRoom
            isGenerating = false
        }
    }

    val myRooms by SocketManager.myRooms.collectAsState()
    val existingRoomId = myRooms.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (existingRoomId != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "You have an active room",
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Room ID: $existingRoomId",
                        fontFamily = inter,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { 
                             scope.launch {
                                 SocketManager.establishConnection()
                                 onJoin(existingRoomId, username)
                             }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Rejoin Session", fontFamily = inter, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "To create a new room, you must first rejoin and terminate your active session from the session tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontFamily = inter,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header with country flag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = countryFlag,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Host a Session",
                            fontFamily = jetbrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.size(24.dp))

                    // Room Name Input
                    Text(
                        text = "Room Details",
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("Room Name", fontFamily = inter) },
                        placeholder = { Text("e.g. Neon Vibes", fontFamily = inter) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        trailingIcon = {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { regenerateRoomName() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Randomize")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )

                    Spacer(modifier = Modifier.size(24.dp))

                    // Vibe / Genre Selection
                    Text(
                        text = "Select Vibe",
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(genres) { genre ->
                            val isSelected = genre == selectedGenre
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedGenre = genre },
                                label = { 
                                    Text(
                                        text = genre, 
                                        fontFamily = inter,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ) 
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(24.dp))

                    // Privacy Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isPrivate) "Private Room" else "Public Room",
                                    fontFamily = jetbrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPrivate) "Invite only" else "Anyone can join",
                                    fontFamily = inter,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it },
                            thumbContent = if (isPrivate) {
                                { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.size(32.dp))

                    // Action Button
                    Button(
                        onClick = { 
                            if (username.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    SocketManager.establishConnection()
                                    val roomId = SocketManager.createRoom(
                                        name = roomName.ifBlank { "My Room" },
                                        vibe = selectedGenre,
                                        isPrivate = isPrivate,
                                        hostUsername = username,
                                        countryFlag = countryFlag
                                    )
                                    
                                    if (roomId != null) {
                                        onJoin(roomId, username)
                                    } else {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = username.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Listening",
                                fontFamily = inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
