@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bongbee.iptv

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bongbee.iptv.ui.screens.*
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.util.LocaleHelper
import com.bongbee.iptv.util.UpdateManager
import com.bongbee.iptv.util.UpdateWorker
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: IptvViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KEEP SCREEN ON: Prevents the device from sleeping while app is open
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        UpdateWorker.schedule(this)

        enableEdgeToEdge()
        setContent {
            viewModel = viewModel()
            val context = LocalContext.current

            val localizedContext = LocaleHelper.setLocale(context, viewModel.appLanguage)

            val updateUIState by UpdateManager.updateUIState.collectAsState()
            val isDownloading by UpdateManager.isDownloading.collectAsState()
            val downloadProgress by UpdateManager.downloadProgress.collectAsState()

            var showIntro by remember { mutableStateOf(false) }

            // Handle Notification Permission for Android 13+
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                Log.d("MainActivity", "Notification permission granted: $isGranted")
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Trigger auto-check on startup
                UpdateManager.checkUpdate(context)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                IptvTheme(darkTheme = viewModel.isDarkMode) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

                        // ONLY load the main app content when the intro is finished
                        if (!showIntro) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                IptvApp(viewModel)
                            }
                        }

                        // Intro Video Overlay - Only shows indra_video at start
                        if (showIntro) {
                            IntroVideoPlayer(onVideoFinished = { showIntro = false })
                        }

                        // Update Dialog - Only shows AFTER intro is finished
                        if (!showIntro) {
                            updateUIState?.let { state ->
                                AlertDialog(
                                    onDismissRequest = { if (!state.isMandatory && !isDownloading) UpdateManager.dismissUpdateDialog() },
                                    title = { Text(if (state.isMandatory) stringResource(R.string.update_required) else stringResource(R.string.update_available)) },
                                    text = {
                                        Column {
                                            Text(stringResource(R.string.update_desc, state.version))
                                            if (state.isMandatory) {
                                                Text("\n" + stringResource(R.string.update_mandatory), color = MaterialTheme.colorScheme.error)
                                            }

                                            if (isDownloading) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                LinearProgressIndicator(
                                                    progress = { downloadProgress },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = AccentCyan,
                                                    trackColor = Color.White.copy(alpha = 0.1f)
                                                )
                                                Text(
                                                    text = "Downloading: ${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        if (!isDownloading) {
                                            Button(onClick = {
                                                UpdateManager.startDownload(context, state.url, state.version)
                                            }) {
                                                Text(stringResource(R.string.update_now))
                                            }
                                        }
                                    },
                                    dismissButton = if (!state.isMandatory && !isDownloading) {
                                        {
                                            TextButton(onClick = { UpdateManager.dismissUpdateDialog() }) {
                                                Text(stringResource(R.string.update_later))
                                            }
                                        }
                                    } else null,
                                    containerColor = ElevatedSurface,
                                    titleContentColor = TextPrimary,
                                    textContentColor = TextSecondary,
                                    properties = DialogProperties(
                                        dismissOnBackPress = !state.isMandatory && !isDownloading,
                                        dismissOnClickOutside = !state.isMandatory && !isDownloading
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::viewModel.isInitialized && (viewModel.currentChannel != null || viewModel.currentWebUrl != null)) {
            enterPipMode()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::viewModel.isInitialized) {
            viewModel.setIsInPipMode(isInPictureInPictureMode)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun IntroVideoPlayer(onVideoFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(100)
        onVideoFinished()
    }
}

@Composable
fun IptvApp(viewModel: IptvViewModel) {
    val navController = rememberNavController()
    val currentChannel = viewModel.currentChannel
    val currentWebUrl = viewModel.currentWebUrl

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (currentChannel != null) {
        PlayerScreen(
            channel = currentChannel,
            viewModel = viewModel,
            onClose = { viewModel.closePlayer() }
        )
    } else if (currentWebUrl != null) {
        WebPlayerScreen(
            url = currentWebUrl!!,
            title = viewModel.currentWebTitle ?: stringResource(R.string.web_viewer),
            viewModel = viewModel,
            onClose = { viewModel.closeWebUrl() },
            isAiService = viewModel.isCurrentWebAiHub
        )
    } else {
        NavHost(navController = navController, startDestination = "main_tabs") {
            composable("main_tabs") {
                MainScreenLayout(viewModel, rootNavController = navController)
            }
            composable(
                route = "movie_hub?search={search}",
                arguments = listOf(
                    navArgument("search") { 
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val startWithSearch = backStackEntry.arguments?.getBoolean("search") ?: false
                MovieHubScreen(
                    viewModel = viewModel, 
                    onBack = { navController.popBackStack() }, 
                    onMovieClick = { movieId, mediaType ->
                        navController.navigate("movie_detail/$mediaType/$movieId")
                    },
                    startWithSearch = startWithSearch
                )
            }
            composable(
                route = "movie_detail/{mediaType}/{movieId}",
                arguments = listOf(
                    navArgument("mediaType") { type = NavType.StringType },
                    navArgument("movieId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                MovieDetailScreen(
                    movieId = movieId,
                    mediaType = mediaType,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSeeMoreMovies = { query ->
                        viewModel.fetchMovieHubMovies(query, 1)
                        navController.navigate("movie_hub?search=true")
                    }
                )
            }
            composable("channels/{categoryName}/{categoryUrl}") { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val encodedUrl = backStackEntry.arguments?.getString("categoryUrl") ?: ""
                val categoryUrl = URLDecoder.decode(encodedUrl, "UTF-8")

                ChannelListScreen(
                    categoryName = categoryName,
                    categoryUrl = categoryUrl,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("terminal") {
                TerminalScreen(onBack = { navController.popBackStack() })
            }
            composable("played_movies") {
                PlayedMoviesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId, mediaType ->
                        navController.navigate("movie_detail/$mediaType/$movieId")
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreenLayout(viewModel: IptvViewModel, rootNavController: androidx.navigation.NavHostController) {
    var selectedHomeTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().appBackground(viewModel).padding(padding)) {
            val homeTabs = listOf(
                stringResource(id = R.string.tab_categories),
                stringResource(id = R.string.tab_countries),
                stringResource(id = R.string.tab_regions),
                stringResource(id = R.string.tab_languages)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "INDRA",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { rootNavController.navigate("played_movies") }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Watched Movies", tint = if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight)
                        }
                        IconButton(
                            onClick = { rootNavController.navigate("movie_hub?search=true") }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight)
                        }
                        IconButton(
                            onClick = { viewModel.openWebUrl("https://asiadrama.net", "Drama Center") }
                        ) {
                            Icon(Icons.Default.TheaterComedy, contentDescription = "Drama", tint = if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight)
                        }
                        IconButton(
                            onClick = { rootNavController.navigate("settings") },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight
                    )
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedHomeTab,
                    containerColor = Color.Transparent,
                    contentColor = AccentCyan,
                    edgePadding = 24.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedHomeTab])
                                .height(3.dp)
                                .padding(horizontal = 16.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(AccentCyan, PrimaryBlue)
                                    ),
                                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                ) {
                    homeTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedHomeTab == index,
                            onClick = { selectedHomeTab = index },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selectedHomeTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedHomeTab == index) {
                                        if (viewModel.isDarkMode) TextPrimary else TextPrimaryLight
                                    } else {
                                        if (viewModel.isDarkMode) TextSecondary else TextSecondaryLight
                                    }
                                )
                            }
                        )
                    }
                }

                CategoryListScreen(
                    selectedTab = selectedHomeTab,
                    categories = viewModel.categories,
                    privateCategories = viewModel.privateCategories,
                    countries = viewModel.countries,
                    regions = viewModel.regions,
                    languages = viewModel.languages,
                    showPrivateCategories = viewModel.showPrivateCategories,
                    translateCategory = { viewModel.translateCategory(it) },
                    onCategoryClick = { category, url ->
                        if (category.isWeb || (url.startsWith("http") && !url.endsWith(".m3u"))) {
                            viewModel.openWebUrl(url, category.name)
                        } else {
                            val encodedUrl = URLEncoder.encode(url, "UTF-8")
                            rootNavController.navigate("channels/${category.name}/$encodedUrl")
                        }
                    },
                    viewModel = viewModel,
                    onMovieHubMoreClick = { rootNavController.navigate("movie_hub?search=false") },
                    onMovieClick = { movieId, mediaType -> 
                        rootNavController.navigate("movie_detail/$mediaType/$movieId") 
                    }
                )
            }
        }
    }
}
