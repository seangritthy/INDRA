package com.bongbee.iptv.model

import java.text.SimpleDateFormat
import java.util.*

data class WatchHistoryItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val mediaType: String = "movie",
    val season: Int = 1,
    val episode: Int = 1,
    val watchedAt: Long = System.currentTimeMillis()
) {
    val posterUrl: String
        get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath"
        else "https://via.placeholder.com/500x750?text=MVHD"

    val watchedAtFormatted: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(watchedAt))
        }
}

