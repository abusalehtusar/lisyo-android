package dev.abu.lisyo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abu.lisyo.data.api.SocketManager
import dev.abu.lisyo.ui.theme.inter
import dev.abu.lisyo.ui.theme.jetbrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onLoginClick: () -> Unit) {
    val youtubeCookie by SocketManager.youtubeCookie.collectAsState()
    val username by SocketManager.currentUsername.collectAsState()
    val baseUrl by SocketManager.baseUrl.collectAsState()
    val isLoggedIn = youtubeCookie != null

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf(username) }

    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = {
                Text(
                    "Edit Username",
                    fontFamily = jetbrainsMono,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempUsername,
                    onValueChange = { tempUsername = it },
                    label = { Text("Username", fontFamily = inter) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempUsername.isNotBlank()) {
                            SocketManager.setUsername(tempUsername)
                            showEditUsernameDialog = false
                        }
                    },
                    enabled = tempUsername.isNotBlank()
                ) {
                    Text("Save", fontFamily = inter)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel", fontFamily = inter)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontFamily = jetbrainsMono, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Profile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = jetbrainsMono,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { 
                    Text(
                        "Username", 
                        fontFamily = inter, 
                        fontWeight = FontWeight.SemiBold 
                    ) 
                },
                supportingContent = { 
                    Text(username, fontFamily = inter) 
                },
                leadingContent = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                trailingContent = {
                    IconButton(onClick = { 
                        tempUsername = username
                        showEditUsernameDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "Network",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = jetbrainsMono,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { 
                    Text(
                        "Backend Server", 
                        fontFamily = inter, 
                        fontWeight = FontWeight.SemiBold 
                    ) 
                },
                supportingContent = { 
                    Text(baseUrl, fontFamily = inter) 
                },
                leadingContent = {
                    Icon(Icons.Default.Dns, contentDescription = null)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = jetbrainsMono,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { 
                    Text(
                        "YouTube Login", 
                        fontFamily = inter, 
                        fontWeight = FontWeight.SemiBold 
                    ) 
                },
                supportingContent = { 
                    Text(
                        if (isLoggedIn) "Logged in successfully" 
                        else "Login to access your library and playlists",
                        fontFamily = inter
                    )
                },
                trailingContent = {
                    if (isLoggedIn) {
                        TextButton(onClick = { SocketManager.setYoutubeCookie(null) }) {
                            Text(
                                "Logout", 
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(onClick = onLoginClick) {
                            Text(
                                "Login",
                                fontFamily = inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "Application",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = jetbrainsMono,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Version 1.0.0a",
                        fontFamily = jetbrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Changelogs",
                        fontFamily = jetbrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                    val logs = listOf(
                        "Refined room management logic",
                        "Added synchronization fixes for playback",
                        "Aesthetic UI improvements and layout tweaks",
                        "Restricted concurrent room creation per user",
                        "Implemented in-room termination for hosts"
                    )
                    logs.forEach { log ->
                        Text(
                            "- $log",
                            fontFamily = jetbrainsMono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))

            // Social Media / Developer Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Developer Abu Saleh",
                    fontFamily = jetbrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                SocialItem(icon = Icons.Default.Code, label = "github/abusalehtusar")
                SocialItem(icon = Icons.Default.Person, label = "instagram.com/abusalehtusar")
                SocialItem(icon = Icons.Default.Person, label = "telegram/AbuSalehT")
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SocialItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = jetbrainsMono,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}