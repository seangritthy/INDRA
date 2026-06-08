package com.bongbee.iptv.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bongbee.iptv.R
import com.bongbee.iptv.model.Channel
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.viewmodel.IptvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    categoryName: String,
    categoryUrl: String,
    viewModel: IptvViewModel,
    onBack: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(categoryUrl) {
        viewModel.fetchChannels(categoryUrl)
    }

    val filteredChannels = remember(viewModel.channels, searchQuery) {
        if (searchQuery.isEmpty()) {
            viewModel.channels
        } else {
            viewModel.channels.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.group.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search channels...", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (searchQuery.isNotEmpty()) searchQuery = "" else isSearchActive = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = TextPrimary)
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                viewModel.translateCategory(categoryName), 
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!viewModel.isLoading) {
                                Text(
                                    text = "${viewModel.channels.size} Channels",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentCyan
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().appBackground(viewModel)) {
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isEmpty()) stringResource(R.string.no_channels) else "No channels match your search", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp, 
                        top = padding.calculateTopPadding() + 8.dp, 
                        end = 20.dp, 
                        bottom = padding.calculateBottomPadding() + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredChannels, key = { it.id + it.name }) { channel ->
                        ChannelCard(channel, viewModel) {
                            viewModel.playChannel(channel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ChannelPreviewPlayer(
    url: String, 
    modifier: Modifier = Modifier,
    onPlaybackStateChanged: (Int) -> Unit,
    onError: (PlaybackException) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
            volume = 0f // Mute preview
        }
    }

    DisposableEffect(url) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                onPlaybackStateChanged(state)
            }
            override fun onPlayerError(error: PlaybackException) {
                onError(error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
}

@Composable
fun ChannelCard(channel: Channel, viewModel: IptvViewModel, onClick: () -> Unit) {
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var hasError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, if (hasError) Color.Red.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Preview Background
            channel.urls.firstOrNull()?.let { url ->
                ChannelPreviewPlayer(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    onPlaybackStateChanged = { 
                        playbackState = it 
                        if (it == Player.STATE_READY) hasError = false
                    },
                    onError = { hasError = true }
                )
            }

            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TV",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = viewModel.translateChannelName(channel.name.ifEmpty { stringResource(R.string.unknown_channel) }),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (hasError) Color.Red else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            
            // Status Tag
            if (playbackState == Player.STATE_READY && !hasError) {
                Surface(
                    color = PrimaryPurple,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 8.sp
                    )
                }
            } else if (hasError) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "ERROR",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
