package dev.abu.lisyo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.abu.lisyo.ui.screens.CreateScreen
import dev.abu.lisyo.ui.screens.JoinScreen
import dev.abu.lisyo.ui.screens.LoginScreen
import dev.abu.lisyo.ui.screens.PlayerScreen
import dev.abu.lisyo.ui.screens.PublicScreen
import dev.abu.lisyo.ui.screens.SettingsScreen
import dev.abu.lisyo.ui.theme.inter
import dev.abu.lisyo.ui.theme.jetbrainsMono

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Player : Screen("player/{roomId}/{username}") {
        fun createRoute(roomId: String, username: String) = "player/$roomId/$username"
    }
    object Settings : Screen("settings")
    object Login : Screen("login")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onJoin = { roomId, username ->
                    navController.navigate(Screen.Player.createRoute(roomId, username))
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Player.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val username = backStackEntry.arguments?.getString("username") ?: ""
            PlayerScreen(
                roomId = roomId,
                username = username,
                onLeave = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onJoin: (String, String) -> Unit,
    onSettings: () -> Unit
) {
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
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
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
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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

        Spacer(modifier = Modifier.size(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (selectedIndex) {
                0 -> PublicScreen(onJoin = onJoin)
                1 -> CreateScreen(onJoin = onJoin)
                2 -> JoinScreen(onJoin = onJoin)
            }
        }
    }
}