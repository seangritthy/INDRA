package com.bongbee.iptv.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class Movie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val mediaType: String = "movie",
    val season: Int = 1,
    val episode: Int = 1
) {
    val posterUrl: String
        get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath"
                else "https://via.placeholder.com/500x750?text=MVHD"

    suspend fun getDirectStreamUrl(): String? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(30000) {
            // Try Consumet API first (actual direct stream), then fall back to embed URLs
            val sources = listOf(
                suspend { getConsumetStream() },
                suspend { getSuperEmbedStream() },
                suspend { getEmbedSuStream() },
                suspend { getVidLinkStream() },
                suspend { getVidSrcStream() },
                suspend { get2EmbedStream() }
            )
            
            for (source in sources) {
                try {
                    val url = source()
                    if (url != null) {
                        return@withTimeoutOrNull url
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500) // Small delay between attempts
            }
            null
        }
    }
    
    private suspend fun getSuperEmbedStream(): String? {
        return try {
            if (mediaType == "tv") "https://superembed.stream/embed/tv/$id/$season/$episode"
            else "https://superembed.stream/embed/movie/$id"
        } catch (e: Exception) { null }
    }
    
    private suspend fun getEmbedSuStream(): String? {
        return try {
            if (mediaType == "tv") "https://embed.su/embed/tv/$id/$season/$episode"
            else "https://embed.su/embed/movie/$id"
        } catch (e: Exception) { null }
    }
    
    private suspend fun getVidLinkStream(): String? {
        return try {
            if (mediaType == "tv") "https://vidlink.pro/embed/tv/$id/$season/$episode"
            else "https://vidlink.pro/embed/movie/$id"
        } catch (e: Exception) { null }
    }
    
    private suspend fun getConsumetStream(): String? {
        return try {
            val type = if (mediaType == "tv") "tv" else "movie"
            val url = if (mediaType == "tv") {
                "https://api.consumet.org/movies/tmdb/$type/$id?season=$season&episode=$episode"
            } else {
                "https://api.consumet.org/movies/tmdb/$type/$id"
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            
            var finalUrl: String? = null
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                
                val sources = json.optJSONArray("sources")
                if (sources != null) {
                    for (i in 0 until sources.length()) {
                        val source = sources.getJSONObject(i)
                        val streamUrl = source.optString("url")
                        if (streamUrl.isNotEmpty() && 
                            (streamUrl.contains(".m3u8") || streamUrl.contains(".mp4"))) {
                            finalUrl = streamUrl
                            break
                        }
                    }
                }
                
                if (finalUrl == null) {
                    finalUrl = json.optString("stream", "").takeIf { it.isNotEmpty() }
                }
                if (finalUrl == null) {
                    finalUrl = json.optString("url", "").takeIf { it.isNotEmpty() }
                }
            }
            connection.disconnect()
            finalUrl
        } catch (e: Exception) { null }
    }
    
    private suspend fun getVidSrcStream(): String? {
        return try {
            if (mediaType == "tv") "https://vidsrc.xyz/embed/tv/$id/$season/$episode"
            else "https://vidsrc.xyz/embed/movie/$id"
        } catch (e: Exception) { null }
    }
    
    private suspend fun get2EmbedStream(): String? {
        return try {
            if (mediaType == "tv") {
                "https://www.2embed.cc/embedtv/$id&s=$season&e=$episode"
            } else {
                "https://www.2embed.cc/embed/$id"
            }
        } catch (e: Exception) { null }
    }
}
