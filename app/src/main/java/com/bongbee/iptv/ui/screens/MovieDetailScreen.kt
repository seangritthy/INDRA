package com.bongbee.iptv.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.bongbee.iptv.util.AdBlocker
import com.bongbee.iptv.util.VidSrcExtractor
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    mediaType: String,
    viewModel: IptvViewModel,
    onBack: () -> Unit,
    onSeeMoreMovies: (String) -> Unit
) {
    var currentMovieId by remember { mutableIntStateOf(movieId) }
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

    // Server Selection
    val servers = listOf(
        "VidSrc.to" to "vidsrc.to",
        "VidSrc.xyz" to "vidsrc.xyz",
        "VidSrc.mov" to "vidsrc.mov",
        "VidSrc.net" to "vidsrc.net",
        "VidSrc.me" to "vidsrc.me",
        "VidSrc.wtf" to "vidsrc.wtf",
        "VidSrc.wtf (API)" to "api.vidsrc.wtf",
        "VidSrc-api.wtf" to "vidsrc-api.wtf",
        "2Embed" to "2embed.cc",
        "Embed.su" to "embed.su",
        "MultiEmbed" to "multiembed.mov",
        "AutoEmbed" to "autoembed.to",
        "MoviesAPI" to "moviesapi.xyz",
        "Vidzee" to "vidzee.cc",
        "VidRock" to "vidrock.cc",
        "Vidnest" to "vidnest.net",
        "RiveEmbed" to "riveembed.com",
        "SmashyStream" to "smashystream.com",
        "111Movies" to "111movies.com",
        "Videasy" to "videasy.cc",
        "VidLink" to "vidlink.pro",
        "VidFast" to "vidfast.pro",
        "PrimeWire" to "primewire.tf",
        "WarezCDN" to "warezcdn.com",
        "SuperFlix" to "superflix.net",
        "Vidup.io" to "vidup.io"
    )
    var selectedServer by remember { mutableStateOf(servers[0]) }

    // Fullscreen handling
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val apiKey = "5e10bf06e4f15dae6e9ff35ff35e8df2"

    LaunchedEffect(currentMovieId) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Details
                val detailUrl = "https://api.themoviedb.org/3/$mediaType/$currentMovieId?api_key=$apiKey&language=en-US"
                val detailResponse = URL(detailUrl).readText()
                movieDetails = JSONObject(detailResponse)

                // Add to watch history
                val title = movieDetails?.optString(if (mediaType == "movie") "title" else "name") ?: "Unknown"
                val posterPath = movieDetails?.optString("poster_path")
                val releaseDate = if (mediaType == "movie") movieDetails?.optString("release_date") else movieDetails?.optString("first_air_date")
                val movie = Movie(
                    id = currentMovieId,
                    title = title,
                    posterPath = posterPath,
                    releaseDate = releaseDate,
                    mediaType = mediaType,
                    season = selectedSeason,
                    episode = selectedEpisode
                )
                viewModel.addToWatchHistory(movie)

                // Credits (Cast)
                val creditsUrl = "https://api.themoviedb.org/3/$mediaType/$currentMovieId/credits?api_key=$apiKey&language=en-US"
                val creditsResponse = URL(creditsUrl).readText()
                val castArray = JSONObject(creditsResponse).getJSONArray("cast")
                val castList = mutableListOf<JSONObject>()
                for (i in 0 until minOf(castArray.length(), 15)) {
                    castList.add(castArray.getJSONObject(i))
                }
                cast = castList

                // Keywords
                val keywordsUrl = "https://api.themoviedb.org/3/$mediaType/$currentMovieId/keywords?api_key=$apiKey"
                val keywordsResponse = URL(keywordsUrl).readText()
                val kArray = if (mediaType == "movie") {
                    JSONObject(keywordsResponse).getJSONArray("keywords")
                } else {
                    JSONObject(keywordsResponse).getJSONArray("results")
                }
                val kList = mutableListOf<String>()
                for (i in 0 until kArray.length()) {
                    kList.add(kArray.getJSONObject(i).getString("name"))
                }
                keywords = kList

                // Similar
                val similarUrl = "https://api.themoviedb.org/3/$mediaType/$currentMovieId/similar?api_key=$apiKey&language=en-US"
                val similarResponse = URL(similarUrl).readText()
                val results = JSONObject(similarResponse).getJSONArray("results")
                val movies = mutableListOf<Movie>()
                for (i in 0 until minOf(results.length(), 15)) {
                    val obj = results.getJSONObject(i)
                    movies.add(Movie(
                        id = obj.getInt("id"),
                        title = if (mediaType == "movie") obj.getString("title") else obj.getString("name"),
                        posterPath = obj.optString("poster_path", null),
                        releaseDate = if (mediaType == "movie") obj.optString("release_date", null) else obj.optString("first_air_date", null)
                    ))
                }
                similarMovies = movies

                if (mediaType == "tv") {
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

    LaunchedEffect(currentMovieId, selectedSeason) {
        if (mediaType == "tv") {
            isEpisodesLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val seasonUrl = "https://api.themoviedb.org/3/tv/$currentMovieId/season/$selectedSeason?api_key=$apiKey&language=en-US"
                    val response = URL(seasonUrl).readText()
                    val results = JSONObject(response).getJSONArray("episodes")
                    val eps = mutableListOf<JSONObject>()
                    for (i in 0 until results.length()) {
                        eps.add(results.getJSONObject(i))
                    }
                    episodes = eps
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isEpisodesLoading = false
                }
            }
        }
    }

    // Fullscreen side-effect
    SideEffect {
        if (fullScreenView != null) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (fullScreenView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { fullScreenView!! }, modifier = Modifier.fillMaxSize())
            BackHandler {
                fullScreenCallback?.onCustomViewHidden()
                fullScreenView = null
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(movieDetails?.optString(if (mediaType == "movie") "title" else "name") ?: stringResource(R.string.epg_metadata), style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close)) }
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
                val title = movie.optString(if (mediaType == "movie") "title" else "name")
                val year = movie.optString(if (mediaType == "movie") "release_date" else "first_air_date").split("-").firstOrNull() ?: stringResource(R.string.na)
                val rating = movie.optDouble("vote_average", 0.0)
                val runtime = if (mediaType == "movie") movie.optInt("runtime", 0) else movie.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0
                val overview = movie.optString("overview")
                val tagline = movie.optString("tagline")
                val backdropPath = movie.optString("backdrop_path")
                val posterPath = movie.optString("poster_path")
                val status = movie.optString("status")
                val originalLanguage = movie.optString("original_language").uppercase()

                val creators = movie.optJSONArray("created_by")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("name") else null
                }
                val networks = movie.optJSONArray("networks")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("name") else null
                }

                val countries = movie.optJSONArray("production_countries")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("name") else stringResource(R.string.unknown)
                } ?: stringResource(R.string.unknown)
                val language = movie.optJSONArray("spoken_languages")?.let {
                    if (it.length() > 0) it.getJSONObject(0).optString("english_name") else stringResource(R.string.unknown)
                } ?: stringResource(R.string.unknown)

                val userScore = (rating * 10).toInt()

                // Precise Server URL Builder logic to avoid 404s and White Screens
                val videoUrl = remember(selectedServer, currentMovieId, selectedSeason, selectedEpisode) {
                    val domain = selectedServer.second
                    when {
                        domain.contains("2embed.cc") -> {
                            if (mediaType == "movie") "https://www.2embed.cc/embed/$currentMovieId"
                            else "https://www.2embed.cc/embedtv/$currentMovieId&s=$selectedSeason&e=$selectedEpisode"
                        }
                        domain.contains("vidsrc.to") -> {
                            // vidsrc.to uses /embed/movie/ID or /embed/tv/ID/S/E
                            if (mediaType == "movie") "https://vidsrc.to/embed/movie/$currentMovieId"
                            else "https://vidsrc.to/embed/tv/$currentMovieId/$selectedSeason/$selectedEpisode"
                        }
                        domain.contains("vidsrc.xyz") || domain.contains("vidsrc.net") ||
                                domain.contains("vidsrc.me") || domain.contains("vidsrc.mov") ||
                                domain.contains("vidsrc.wtf") -> {
                            // These mirrors often perform better with direct path segments or explicit tmdb prefix
                            if (mediaType == "movie") "https://$domain/embed/movie/$currentMovieId"
                            else "https://$domain/embed/tv/$currentMovieId/$selectedSeason/$selectedEpisode"
                        }
                        domain.contains("autoembed.to") -> {
                            if (mediaType == "movie") "https://autoembed.to/movie/tmdb/$currentMovieId"
                            else "https://autoembed.to/tv/tmdb/$currentMovieId/$selectedSeason/$selectedEpisode"
                        }
                        domain.contains("moviesapi.xyz") -> {
                            if (mediaType == "movie") "https://moviesapi.xyz/api/v2/embed/movie?tmdb=$currentMovieId"
                            else "https://moviesapi.xyz/api/v2/embed/tv?tmdb=$currentMovieId&season=$selectedSeason&episode=$selectedEpisode"
                        }
                        else -> {
                            if (mediaType == "movie") "https://$domain/embed/movie/$currentMovieId"
                            else "https://$domain/embed/tv/$currentMovieId/$selectedSeason/$selectedEpisode"
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    // Header Backdrop
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w1280$backdropPath",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Background))))

                        Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
                            // Wrapped the Poster Card in a Box to layer the logo
                            Box(modifier = Modifier.width(90.dp).height(135.dp)) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxSize().shadow(8.dp)
                                ) {
                                    AsyncImage(
                                        model = "https://image.tmdb.org/t/p/w500$posterPath",
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Logo on the top right of the poster - Resized small
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .size(20.dp) // Resized to be smaller (was 28.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
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

                    // Metadata Summary
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status.uppercase(), AccentCyan)
                        StatusBadge(originalLanguage, Color.White)
                        if (networks != null) StatusBadge(networks.uppercase(), Color.Yellow)
                        movie.optJSONArray("genres")?.let { genres ->
                            for (i in 0 until minOf(genres.length(), 1)) {
                                StatusBadge(genres.getJSONObject(i).getString("name").uppercase(), Color.Gray)
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
                        key(videoUrl) {
                            AndroidView(
                                factory = { factoryContext ->
                                    WebView(factoryContext).apply {
                                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                                        CookieManager.getInstance().setAcceptCookie(true)
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            setSupportMultipleWindows(true)
                                            javaScriptCanOpenWindowsAutomatically = true
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            
                                            // Set modern mobile User-Agent for better streaming compatibility
                                            userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                                val url = request?.url?.toString() ?: ""
                                                if (AdBlocker.isAd(url)) return AdBlocker.createEmptyResource()
                                                return super.shouldInterceptRequest(view, request)
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                val blockAdsJs = """
                                                    (function() {
                                                        // Anti-bot spoofs
                                                        try {
                                                            Object.defineProperty(navigator, 'webdriver', { get: () => false });
                                                            Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
                                                            Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
                                                        } catch(e) {}

                                                        window.open = function() { return null; };
                                                        var purge = function() {
                                                            var selectors = [
                                                                '.ad', '.ads', '.adsbygoogle', '[id*="google_ads"]', '.modal', '.popup', '.overlay',
                                                                'iframe[src*="ads"]', 'div[class*="ad-"]', 'div[id*="ad-"]',
                                                                '.tp-modal', '.tp-backdrop', '.close-button', '.close-icon',
                                                                '#dismiss', '#close', '.ads-wrapper', '.popunder', '.popup-container',
                                                                '.sponsored', '.banner-ad', '.video-ad', '#player-ads', '.fixed-ad'
                                                            ];
                                                            selectors.forEach(function(s) {
                                                                document.querySelectorAll(s).forEach(function(el) {
                                                                    if (el.querySelector('video') || el.id === 'player' || el.className.includes('video-js')) return;
                                                                    el.style.display = 'none';
                                                                    el.remove();
                                                                });
                                                            });
                                                            
                                                            document.querySelectorAll('div, section, aside').forEach(function(el) {
                                                                var style = window.getComputedStyle(el);
                                                                var z = parseInt(style.zIndex);
                                                                if (z > 50 && !el.querySelector('video') && style.position === 'fixed') {
                                                                    el.style.display = 'none';
                                                                    el.remove();
                                                                }
                                                            });
                                                        };
                                                        setInterval(purge, 1500);
                                                        purge();
                                                    })();
                                                """.trimIndent()
                                                view?.evaluateJavascript(blockAdsJs, null)
                                            }

                                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                                                val host = request.url.host?.lowercase() ?: ""
                                                val trusted = listOf("vidsrc", "2embed", "suembed", "embed.su", "cloudflare", "turnstile", "vidplay", "vidlink", "autoembed", "moviesapi", "vidzee", "vidrock", "vidnest", "riveembed", "smashystream", "111movies", "videasy", "vidfast")
                                                if (trusted.any { host.contains(it) }) return false
                                                return true
                                            }
                                        }

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                fullScreenView = view
                                                fullScreenCallback = callback
                                                activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                                            }
                                            override fun onHideCustomView() {
                                                fullScreenView = null
                                                fullScreenCallback?.onCustomViewHidden()
                                                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                                            }
                                        }

                                        // Enhanced Anti-Bot Headers matching the mobile UA
                                        val headers = mapOf(
                                            "Referer" to "https://${selectedServer.second}/",
                                            "Sec-Ch-Ua" to "\"Chromium\";v=\"130\", \"Not?A_Brand\";v=\"99\", \"Google Chrome\";v=\"130\"",
                                            "Sec-Ch-Ua-Mobile" to "?1",
                                            "Sec-Ch-Ua-Platform" to "\"Android\"",
                                            "Sec-Fetch-Dest" to "iframe",
                                            "Sec-Fetch-Mode" to "navigate",
                                            "Sec-Fetch-Site" to "cross-site",
                                            "Upgrade-Insecure-Requests" to "1",
                                            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                                            "Accept-Language" to "en-US,en;q=0.9"
                                        )
                                        loadUrl(videoUrl, headers)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
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
                                Text(text = server.first, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (isSelected) Color.Black else TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (creators != null) {
                        Text(text = "CREATOR", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = creators, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                    }

                    // Cast Section (Clickable)
                    if (cast.isNotEmpty()) {
                        Text(text = stringResource(R.string.series_cast), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(cast) { person ->
                                val name = person.optString("name")
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(85.dp)
                                        .clickable { onSeeMoreMovies(name) }
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

                    // Facts & Details Items
                    Text(text = stringResource(R.string.facts), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoItem(icon = Icons.Default.Public, label = stringResource(R.string.country), value = countries, modifier = Modifier.weight(1f).clickable { onSeeMoreMovies(countries) })
                        InfoItem(icon = Icons.Default.Language, label = stringResource(R.string.language), value = language, modifier = Modifier.weight(1f).clickable { onSeeMoreMovies(language) })
                    }
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        FactItem(stringResource(R.string.fact_status), status)
                        if (networks != null) FactItem(stringResource(R.string.fact_network), networks)
                        FactItem(stringResource(R.string.fact_type), if (mediaType == "tv") stringResource(R.string.type_scripted) else stringResource(R.string.type_movie))
                        FactItem(stringResource(R.string.fact_lang), originalLanguage)
                    }

                    // Keywords (Clickable)
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

                    // TV Show Selection
                    if (mediaType == "tv") {
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
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(24.dp)) }
                        } else {
                            // Episode Grid
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

                    if (similarMovies.isNotEmpty()) {
                        Text(text = stringResource(R.string.related_movies), modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentCyan, letterSpacing = 2.sp)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            items(similarMovies) { movie -> MovieRowCard(movie) { currentMovieId = movie.id } }
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

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
fun ControlItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
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
    Card(modifier = Modifier.width(100.dp).fillMaxHeight().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 100f)))
            Text(text = movie.title, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), border = BorderStroke(1.dp, color.copy(alpha = 0.5f)), shape = RoundedCornerShape(4.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp)
    }
}
