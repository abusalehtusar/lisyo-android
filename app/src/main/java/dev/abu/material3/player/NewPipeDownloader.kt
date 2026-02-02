package dev.abu.material3.player

import dev.abu.material3.utils.Logger
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NewPipeDownloader : Downloader() {
    companion object {
        private const val TAG = "NewPipeDownloader"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        // Logger.logInfo(TAG, "Requesting: ${request.httpMethod()} ${request.url()}")
        
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = request.httpMethod()
        
        // Use a consistent User-Agent
        connection.setRequestProperty("User-Agent", USER_AGENT)
        
        request.headers().forEach { (key, value) ->
            if (key.lowercase() != "user-agent") {
                connection.setRequestProperty(key, value.joinToString(","))
            }
        }
        
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        val responseCode = try {
            connection.responseCode
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to get response code for ${request.url()}", e)
            throw e
        }
        
        val responseMessage = connection.responseMessage
        val responseHeaders = connection.headerFields
        
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        
        if (responseCode !in 200..299) {
            Logger.logError(TAG, "Request failed with code $responseCode for ${request.url().take(100)}... Body: ${responseBody.take(200)}...")
        }
        
        return Response(responseCode, responseMessage, responseHeaders, responseBody, request.url())
    }
}
