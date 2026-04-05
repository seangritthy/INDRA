package com.bongbee.iptv.model

data class Category(
    val name: String,
    val count: Int,
    val url: String,
    val isWeb: Boolean = false,
    val videoResName: String? = null
)

data class Country(
    val name: String,
    val url: String
)

data class Channel(
    val id: String,
    val name: String,
    val logo: String,
    val urls: List<String>,
    val group: String
)

data class ApiChannel(
    val id: String,
    val name: String,
    val network: String,
    val country: String,
    val website: String,
    val categories: List<String>
)

data class ApiGuide(
    val channel: String,
    val site: String,
    val site_name: String,
    val lang: String
)

enum class StreamStatus {
    UNKNOWN,
    CHECKING,
    LIVE,
    ERROR
}

