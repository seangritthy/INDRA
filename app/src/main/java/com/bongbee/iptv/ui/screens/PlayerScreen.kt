package com.bongbee.iptv.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bongbee.iptv.R
import com.bongbee.iptv.model.Channel
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.util.findActivity
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(channel: Channel, viewModel: IptvViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    var isFullscreen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val currentUrl = remember(channel) { channel.urls.firstOrNull() ?: "" }

    val dataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotEmpty()) {
            val mediaSource = if (currentUrl.contains(".m3u8")) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(currentUrl))
            } else {
                MediaItem.fromUri(currentUrl)
            }
            
            if (mediaSource is androidx.media3.exoplayer.source.MediaSource) {
                exoPlayer.setMediaSource(mediaSource)
            } else if (mediaSource is MediaItem) {
                exoPlayer.setMediaItem(mediaSource)
            }
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    BackHandler {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onClose()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isFullscreen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ElevatedSurface,
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.channel_menu),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(24.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(viewModel.channels) { ch ->
                            val isSelected = ch.id == channel.id && ch.name == channel.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playChannel(ch)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .background(if (isSelected) PrimaryPurple.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ch.logo.isNotEmpty()) {
                                    AsyncImage(
                                        model = ch.logo,
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column {
                                    Text(
                                        text = viewModel.translateChannelName(ch.name),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) AccentCyan else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (ch.group.isNotEmpty()) {
                                        Text(
                                            text = viewModel.translateCategory(ch.group),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Box(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .aspectRatio(16f / 9f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setBackgroundColor(0xFF000000.toInt())
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            setFullscreenButtonClickListener { isFull ->
                                isFullscreen = isFull
                                activity?.requestedOrientation = if (isFull) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                                
                                activity?.window?.let { window ->
                                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                                    if (isFull) {
                                        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    } else {
                                        controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Watermark
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Watermark",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(60.dp)
                        .alpha(0.4f),
                    contentScale = ContentScale.Fit
                )

                if (!isFullscreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                            Text(
                                text = viewModel.translateChannelName(channel.name),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = viewModel.translateChannelName(channel.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (channel.group.isNotEmpty()) {
                        Text(
                            text = viewModel.translateCategory(channel.group),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryPurple
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Add more channel details or related content here
                }
            }
        }
    }
}
