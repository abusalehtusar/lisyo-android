package dev.abu.material3

import android.app.Application
import android.content.Context
import dev.abu.material3.innertube.YouTube
import dev.abu.material3.innertube.models.YouTubeLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
        
        val context: Context
            get() = instance.applicationContext
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize YouTube object
        val locale = Locale.getDefault()
        YouTube.locale = YouTubeLocale(
            gl = locale.country.takeIf { it.isNotBlank() } ?: "US",
            hl = locale.language.takeIf { it.isNotBlank() } ?: "en"
        )
        
        applicationScope.launch {
            // Fetch visitor data if not present
            YouTube.visitorData().onSuccess {
                YouTube.visitorData = it
            }
        }
    }
}
