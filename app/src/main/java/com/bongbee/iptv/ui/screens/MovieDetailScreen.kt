package com.bongbee.iptv.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.bongbee.iptv.R
import com.bongbee.iptv.model.Movie
import com.bongbee.iptv.ui.theme.*
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bongbee.iptv.util.AdBlocker
import com.bongbee.iptv.util.VidSrcExtractor
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    mediaType: String,
    @Suppress("UNUSED_PARAMETER") viewModel: IptvViewModel,
    onBack: () -> Unit,
    onSeeMoreMovies: (String) -> Unit
) {
    var currentMovieId by remember { mutableIntStateOf(movieId) }
    var currentMediaType by remember { mutableStateOf(mediaType) }
    var movieDetails by remember { mutableStateOf<JSONObject?>(null) }
    var similarMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var cast by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var keywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // TV show specific states
    var selectedSeason by remember { mutableIntStateOf(1) }
    var selectedEpisode by remember { mutableIntStateOf(1) }
    var episodes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isEpisodesLoading by remember { mutableStateOf(false) }

    // Cloudnestra only
    val servers = listOf("Cloudnestra" to "cloudnestra.com")
    var selectedServer by remember { mutableStateOf(servers[0]) }

    // Video extraction states
    var extractedVideoUrl by remember { mutableStateOf<String?>(null) }
    var isExtractingVideo by remember { mutableStateOf(false) }

    // Fullscreen handling
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val apiKey = com.bongbee.iptv.BuildConfig.TMDB_API_KEY
    val extractor = remember { VidSrcExtractor(context) }

    val embedUrl = remember(currentMovieId, selectedSeason, selectedEpisode, currentMediaType) {
        if (currentMediaType == "movie") {
            "https://vidsrc.to/embed/movie/$currentMovieId"
        } else {
            "https://vidsrc.to/embed/tv/$currentMovieId/$selectedSeason/$selectedEpisode"
        }
    }

    // Decode Cloudnestra rcp/ link
    suspend fun decodeCloudnestraUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = url.substringAfter("/rcp/")
            val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))

            val patterns = listOf(
                Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)"""),
                Regex("""(https?://[^\s"']+\.mp4[^\s"']*)"""),
                Regex("""(https?://[^\s"']+/(?:manifest|playlist|master)[^\s"']*)""")
            )

            for (pattern in patterns) {
                pattern.find(decoded)?.value?.let { return@withContext it }
            }
            if (decoded.startsWith("http")) return@withContext decoded
            null
        } catch (e: Exception) {
            Log.e("Cloudnestra", "Decode failed", e)
            null
        }
    }

    // Extract video URL — reset when embed URL changes (episode/season switch)
    LaunchedEffect(embedUrl) {
        extractedVideoUrl = null
        isExtractingVideo = true
        var retries = 0
        val maxRetries = 5
        while (extractedVideoUrl == null && retries < maxRetries) {
            try {
                val result = withContext(Dispatchers.Main) {
                    extractor.extractVideoUrl(embedUrl)
                }

                if (!result.isNullOrEmpty()) {
                    extractedVideoUrl = if (result.contains("cloudnestra.com/rcp/")) {
                        decodeCloudnestraUrl(result) ?: result
                    } else {
                        result
                    }
                } else {
                    retries++
                    delay(3000)
                }
            } catch (e: Exception) {
                Log.e("MovieDetail", "Extraction error (attempt ${retries + 1}/$maxRetries)", e)
                retries++
                delay(3000)
            }
        }
        isExtractingVideo = false
    }

    // Load movie/TV details
    LaunchedEffect(currentMovieId, currentMediaType) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val type = currentMediaType
                val detailUrl = "https://api.themoviedb.org/3/$type/$currentMovieId?api_key=$apiKey&language=en-US"
                movieDetails = JSONObject(URL(detailUrl).readText())

                // Cast
                val creditsUrl = "https://api.themoviedb.org/3/$type/$currentMovieId/credits?api_key=$apiKey&language=en-US"
                val castArray = JSONObject(URL(creditsUrl).readText()).getJSONArray("cast")
                cast = List(minOf(castArray.length(), 15)) { castArray.getJSONObject(it) }

                // Keywords
                val keywordsUrl = "https://api.themoviedb.org/3/$type/$currentMovieId/keywords?api_key=$apiKey"
                val kResponse = JSONObject(URL(keywordsUrl).readText())
                val kArray = if (type == "movie") kResponse.getJSONArray("keywords") else kResponse.getJSONArray("results")
                keywords = List(kArray.length()) { kArray.getJSONObject(it).getString("name") }

                // Similar
                val similarUrl = "https://api.themoviedb.org/3/$type/$currentMovieId/similar?api_key=$apiKey&language=en-US"
                val results = JSONObject(URL(similarUrl).readText()).getJSONArray("results")
                similarMovies = List(minOf(results.length(), 15)) {
                    val obj = results.getJSONObject(it)
                    Movie(
                        id = obj.getInt("id"),
                        title = if (currentMediaType == "movie") obj.getString("title") else obj.getString("name"),
                        posterPath = obj.optString("poster_path").takeIf { p -> p.isNotEmpty() && p != "null" },
                        releaseDate = (if (currentMediaType == "movie") obj.optString("release_date") else obj.optString("first_air_date")).takeIf { d -> d.isNotEmpty() && d != "null" },
                        mediaType = currentMediaType
                    )
                }

                if (currentMediaType == "tv") {
                    selectedSeason = 1
                    selectedEpisode = 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Load TV episodes
    LaunchedEffect(currentMovieId, selectedSeason) {
        if (currentMediaType == "tv") {
            isEpisodesLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val seasonUrl = "https://api.themoviedb.org/3/tv/$currentMovieId/season/$selectedSeason?api_key=$apiKey&language=en-US"
                    val response = URL(seasonUrl).readText()
                    val results = JSONObject(response).getJSONArray("episodes")
                    episodes = List(results.length()) { results.getJSONObject(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isEpisodesLoading = false
                }
            }
        }
    }

    SideEffect {
        activity?.requestedOrientation = if (fullScreenView != null)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    if (fullScreenView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { fullScreenView!! }, modifier = Modifier.fillMaxSize())
            BackHandler {
                fullScreenCallback?.onCustomViewHidden()
                fullScreenView = null
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movieDetails?.optString(if (currentMediaType == "movie") "title" else "name") ?: stringResource(R.string.epg_metadata), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background, titleContentColor = TextPrimary)
            )
        },
        containerColor = Background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else if (movieDetails != null) {
            val movie = movieDetails!!
            val title = movie.optString(if (currentMediaType == "movie") "title" else "name")
            val year = movie.optString(if (currentMediaType == "movie") "release_date" else "first_air_date").split("-").firstOrNull() ?: stringResource(R.string.na)
            val userScore = (movie.optDouble("vote_average", 0.0) * 10).toInt()
            val runtime = if (currentMediaType == "movie") movie.optInt("runtime", 0) else movie.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0
            val overview = movie.optString("overview")
            val tagline = movie.optString("tagline")
            val backdropPath = movie.optString("backdrop_path")
            val posterPath = movie.optString("poster_path")
            val status = movie.optString("status")
            val originalLanguage = movie.optString("original_language").uppercase()
            val creators = movie.optJSONArray("created_by")?.let { if (it.length() > 0) it.getJSONObject(0).optString("name") else null }
            val networks = movie.optJSONArray("networks")?.let { if (it.length() > 0) it.getJSONObject(0).optString("name") else null }
            val countries = movie.optJSONArray("production_countries")?.let { if (it.length() > 0) it.getJSONObject(0).optString("name") else "N/A" } ?: "N/A"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Backdrop & Poster Header
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w1280$backdropPath",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Background))))
                    
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Card(
                            modifier = Modifier.size(width = 110.dp, height = 160.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w500$posterPath",
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = year, style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .border(2.dp, if (userScore >= 70) Color.Green else if (userScore >= 40) Color.Yellow else Color.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "$userScore%", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.Timer, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                Text(" $runtime min", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Tagline & Overview
                if (!tagline.isNullOrEmpty()) {
                    Text(text = tagline, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = TextSecondary)
                }
                Text(text = stringResource(R.string.overview_label), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                Text(text = overview, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                // Streaming Player
                Text(text = stringResource(R.string.indra_streaming), modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)

                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, AccentCyan),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    when {
                        isExtractingVideo || extractedVideoUrl == null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = AccentCyan)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Loading video source...", color = Color.White, fontSize = 14.sp)
                                    Text("This may take a moment", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        extractedVideoUrl != null -> {
                            key(extractedVideoUrl) {
                                AndroidView(
                                    factory = { factoryContext ->
                                        WebView(factoryContext).apply {
                                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                                            CookieManager.getInstance().setAcceptCookie(true)
                                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                @Suppress("DEPRECATION")
                                                databaseEnabled = true
                                                mediaPlaybackRequiresUserGesture = false
                                                setSupportMultipleWindows(true)
                                                javaScriptCanOpenWindowsAutomatically = true
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                            }

                                            webViewClient = object : WebViewClient() {
                                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                                    val url = request?.url?.toString() ?: ""
                                                    if (AdBlocker.isAd(url)) return AdBlocker.createEmptyResource()
                                                    return super.shouldInterceptRequest(view, request)
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    val antiBotJs = """
                                                        (function() {
                                                            Object.defineProperty(navigator, 'webdriver', { get: () => false });
                                                            Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
                                                            setTimeout(() => {
                                                                document.querySelectorAll('.ad,.ads,iframe[src*=ads]').forEach(el => el.remove());
                                                            }, 1500);
                                                        })();
                                                    """.trimIndent()
                                                    view?.evaluateJavascript(antiBotJs, null)
                                                }

                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                                                    val host = request.url.host?.lowercase() ?: ""
                                                    val trusted = listOf("cloudnestra", "vidsrc", "vidplay", "embed", "cloudflare", "turnstile")
                                                    return !trusted.any { host.contains(it) }
                                                }
                                            }

                                            webChromeClient = object : WebChromeClient() {
                                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                fullScreenView = view
                                                fullScreenCallback = callback
                                                activity?.window?.let { window ->
                                                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                                                    controller.hide(WindowInsetsCompat.Type.systemBars())
                                                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                                }
                                            }

                                            override fun onHideCustomView() {
                                                fullScreenView = null
                                                fullScreenCallback?.onCustomViewHidden()
                                                activity?.window?.let { window ->
                                                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                                                    controller.show(WindowInsetsCompat.Type.systemBars())
                                                }
                                            }
                                            }

                                            loadUrl(extractedVideoUrl!!)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Server Selection
                Text(text = stringResource(R.string.select_server), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(servers) { server ->
                        val isSelected = selectedServer == server
                        Surface(
                            onClick = { selectedServer = server },
                            color = if (isSelected) AccentCyan else ElevatedSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = server.first,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (isSelected) Color.Black else TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Creators
                if (creators != null) {
                    Text(text = "CREATOR", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = creators, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                }

                // Cast
                if (cast.isNotEmpty()) {
                    Text(text = stringResource(R.string.series_cast), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(cast) { person ->
                            val name = person.optString("name")
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(85.dp).clickable { onSeeMoreMovies(name) }
                            ) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w185${person.optString("profile_path")}",
                                    contentDescription = name,
                                    modifier = Modifier.size(70.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Text(person.optString("character"), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // Facts
                Text(text = stringResource(R.string.facts), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoItem(icon = Icons.Default.Public, label = stringResource(R.string.country), value = countries, modifier = Modifier.weight(1f).clickable { onSeeMoreMovies(countries) })
                    InfoItem(icon = Icons.Default.Language, label = stringResource(R.string.language), value = originalLanguage, modifier = Modifier.weight(1f).clickable { onSeeMoreMovies(originalLanguage) })
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    FactItem(stringResource(R.string.fact_status), status)
                    if (networks != null) FactItem(stringResource(R.string.fact_network), networks)
                    FactItem(stringResource(R.string.fact_type), if (currentMediaType == "tv") stringResource(R.string.type_scripted) else stringResource(R.string.type_movie))
                    FactItem(stringResource(R.string.fact_lang), originalLanguage)
                }

                // Keywords
                if (keywords.isNotEmpty()) {
                    Text(text = stringResource(R.string.keywords), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        keywords.forEach { keyword ->
                            Surface(
                                color = ElevatedSurface,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                modifier = Modifier.clickable { onSeeMoreMovies(keyword) }
                            ) {
                                Text(
                                    text = keyword,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // TV Show Season & Episodes
                if (currentMediaType == "tv") {
                    val numSeasons = movie.optInt("number_of_seasons", 1)
                    Text(text = stringResource(R.string.season_label), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((1..numSeasons).toList()) { seasonNum ->
                            val isSelected = selectedSeason == seasonNum
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSeason = seasonNum; selectedEpisode = 1 },
                                label = { Text("${stringResource(R.string.season_label)} $seasonNum") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentCyan, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    Text(text = stringResource(R.string.episodes_label), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    if (isEpisodesLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 6
                        ) {
                            episodes.forEach { ep ->
                                val epNum = ep.optInt("episode_number")
                                EpisodeGridItem(
                                    epNum = epNum,
                                    isSelected = selectedEpisode == epNum
                                ) { selectedEpisode = epNum }
                            }
                        }
                    }
                }

                // Similar Movies
                if (similarMovies.isNotEmpty()) {
                    Text(text = stringResource(R.string.related_movies), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        items(similarMovies) { movieItem ->
                            MovieRowCard(movieItem) {
                                currentMovieId = movieItem.id
                                currentMediaType = movieItem.mediaType
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ====================== Helper Composables ======================

@Composable
fun FactItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun EpisodeGridItem(epNum: Int, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AccentCyan else ElevatedSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) AccentCyan else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = epNum.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else TextPrimary
            )
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = ElevatedSurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MovieRowCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(100.dp).fillMaxHeight().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 100f)))
            Text(
                text = movie.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

