package dev.abu.material3.ui.screens

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.abu.material3.data.api.SocketManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onDismiss: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login to YouTube") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val cookies = CookieManager.getInstance().getCookie(url)
                                if (cookies != null && cookies.contains("SAPISID")) {
                                    // Found login session
                                    // In a real app we would store this
                                    onDismiss()
                                }
                            }
                        }
                        loadUrl("https://accounts.google.com/ServiceLogin?continue=https://www.youtube.com/signin?action_handle_signin=true&next=https://m.youtube.com/")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
