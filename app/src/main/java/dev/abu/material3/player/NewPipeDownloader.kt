package dev.abu.material3.player

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NewPipeDownloader : Downloader() {
    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = request.httpMethod()
        request.headers().forEach { (key, value) ->
            connection.setRequestProperty(key, value.joinToString(","))
        }
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage
        val responseHeaders = connection.headerFields
        
        val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        
        return Response(responseCode, responseMessage, responseHeaders, responseBody, request.url())
    }
}
