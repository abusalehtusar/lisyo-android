package dev.abu.material3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.material3.ui.theme.LisyoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

val jetbrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular)
)

val inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semi_bold, FontWeight.SemiBold),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_light, FontWeight.Light)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Public", "Create", "Join")
    val icons = listOf(
        Icons.Filled.Public,
        Icons.Filled.Add,
        Icons.AutoMirrored.Filled.Login
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.size(20.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "App Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(44.dp)
            )
            Text(
                text = "Lisyo",
                fontFamily = jetbrainsMono,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val shape = if (isSelected) {
                    RoundedCornerShape(50)
                } else {
                    when (index) {
                        0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 4.dp, bottomEnd = 4.dp)
                        options.size - 1 -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 50.dp, bottomEnd = 50.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                }
                
                SegmentedButton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    shape = shape,
                    onClick = { selectedIndex = index },
                    selected = isSelected,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer, // Use surfaceContainer for better contrast than pure surface
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        activeBorderColor = Color.Transparent,
                        inactiveBorderColor = Color.Transparent
                    ),
                    border = SegmentedButtonDefaults.borderStroke(
                        color = Color.Transparent,
                        width = 0.dp
                    ),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontFamily = inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    },
                    icon = {} 
                )
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (selectedIndex) {
                0 -> PublicScreen()
                1 -> CreateScreen()
                2 -> JoinScreen()
            }
        }
    }
}

data class Room(
    val id: Int,
    val countryFlag: String,
    val countryName: String,
    val username: String,
    val content: String,
    val flagColor: Color
)

val dummyRooms = listOf(
    Room(1, "🇺🇸", "USA", "dj_mike", "Listening to: Lofi Hip Hop Radio 24/7", Color(0xFFE3F2FD)),
    Room(2, "🇯🇵", "Japan", "sakura_beats", "Streaming: Tokyo City Pop Classics", Color(0xFFFFEBEE)),
    Room(3, "🇧🇷", "Brazil", "rio_vibes", "Playing: Bossa Nova Jazz", Color(0xFFE8F5E9)),
    Room(4, "🇬🇧", "UK", "brit_pop_fan", "Jamming to: 90s Britpop Essentials", Color(0xFFF3E5F5)),
    Room(5, "🇩🇪", "Germany", "techno_hans", "Live: Berlin Underground Techno", Color(0xFFFFF3E0)),
    Room(6, "🇰🇷", "Korea", "kpop_stan", "Listening to: NewJeans - Super Shy", Color(0xFFFCE4EC))
)

@Composable
fun PublicScreen() {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
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
                // Circular Logo Container
                Surface(
                    shape = CircleShape,
                    color = room.flagColor,
                    modifier = Modifier.size(48.dp),
                    contentColor = Color.Black
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = room.countryFlag, fontSize = 24.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${room.countryName} / ${room.username}",
                            fontFamily = inter,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // Content
            Text(
                text = room.content,
                fontFamily = inter,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                    modifier = Modifier.weight(1f)
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
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreateScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text("Create Room", fontFamily = jetbrainsMono, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun JoinScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Text("Join Room", fontFamily = jetbrainsMono, style = MaterialTheme.typography.titleLarge)
    }
}
