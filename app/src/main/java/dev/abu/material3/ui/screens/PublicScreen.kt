package dev.abu.material3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.material3.ui.theme.inter
import dev.abu.material3.ui.theme.jetbrainsMono

import androidx.compose.material.icons.filled.GraphicEq

data class Room(
    val id: Int,
    val countryFlag: String,
    val vibe: String,
    val username: String,
    val roomName: String,
    val songs: List<String>,
    val totalSongs: Int,
    val userCount: Int,
    val flagColor: Color
)

val dummyRooms = listOf(
    Room(
        1, "🇺🇸", "Lofi", "dj_mike", "Chill Study Session",
        listOf("Blinding Lights - The Weeknd", "As It Was - Harry Styles", "Stay - Justin Bieber"),
        42, 128, Color(0xFFE3F2FD)
    ),
    Room(
        2, "🇯🇵", "City Pop", "sakura_beats", "Tokyo Night Drive",
        listOf("Plastic Love - Mariya Takeuchi", "Stay With Me - Miki Matsubara", "Mayonaka no Door - Miki Matsubara"),
        24, 85, Color(0xFFFFEBEE)
    ),
    Room(
        3, "🇧🇷", "Bossa Nova", "rio_vibes", "Copacabana Sunset",
        listOf("Garota de Ipanema - Tom Jobim", "Mas Que Nada - Jorge Ben Jor", "Águas de Março - Elis Regina"),
        30, 64, Color(0xFFE8F5E9)
    ),
    Room(
        4, "🇬🇧", "Britpop", "brit_pop_fan", "90s Anthems",
        listOf("Wonderwall - Oasis", "Bitter Sweet Symphony - The Verve", "Don't Look Back in Anger - Oasis"),
        55, 210, Color(0xFFF3E5F5)
    ),
    Room(
        5, "🇩🇪", "Techno", "techno_hans", "Berlin Underground",
        listOf("The Model - Kraftwerk", "Das Boot - U96", "Sonne - Rammstein"),
        18, 45, Color(0xFFFFF3E0)
    ),
    Room(
        6, "🇰🇷", "K-Pop", "kpop_stan", "Seoul Vibe",
        listOf("Super Shy - NewJeans", "Dynamite - BTS", "Fancy - TWICE"),
        60, 350, Color(0xFFFCE4EC)
    )
)

@Composable
fun PublicScreen() {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(dummyRooms) { room ->
            RoomCard(room)
        }
    }
}

@Composable
fun RoomCard(room: Room) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Logo + Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Logo Container (Reduced size: 40.dp)
                Surface(
                    shape = CircleShape,
                    color = room.flagColor,
                    modifier = Modifier.size(40.dp),
                    contentColor = Color.Black
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = room.countryFlag, fontSize = 20.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${room.username} / ${room.roomName}",
                            fontFamily = jetbrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Song List (Timeline Style)
            Column {
                room.songs.take(3).forEachIndexed { index, song ->
                    Row(verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (index < 2) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(14.dp) // Connected with vertical line, not full connected
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = song,
                            fontFamily = jetbrainsMono, // JetBrains font for contents
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (index < 2) {
                         Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Stats Row (Songs, Users, Vibe)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center, // Centered
                modifier = Modifier.fillMaxWidth()
            ) {
                // Total Songs
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Total Songs",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${room.totalSongs} Songs",
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(Modifier.width(20.dp))

                // Total Users
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Users",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${room.userCount}",
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(Modifier.width(20.dp))

                // Vibe
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Vibe",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = room.vibe,
                        fontFamily = jetbrainsMono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { /* Join action */ },
                    shape = RoundedCornerShape(50), // Capsule
                    modifier = Modifier
                        .weight(1f)
                        .height(ButtonDefaults.MinHeight)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Join", fontFamily = inter, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                FilledIconButton(
                    onClick = { /* Report action */ },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.size(ButtonDefaults.MinHeight) // Match Join button height
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
