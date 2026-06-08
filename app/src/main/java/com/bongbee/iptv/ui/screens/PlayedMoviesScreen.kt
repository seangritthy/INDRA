package com.bongbee.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bongbee.iptv.model.WatchHistoryItem
import com.bongbee.iptv.ui.theme.AccentCyan
import com.bongbee.iptv.ui.theme.TextPrimary
import com.bongbee.iptv.ui.theme.TextSecondary
import com.bongbee.iptv.viewmodel.IptvViewModel

@Composable
fun PlayedMoviesScreen(
    viewModel: IptvViewModel,
    onBack: () -> Unit,
    onMovieClick: (Int, String) -> Unit
) {
    val watchHistory = viewModel.watchHistory
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Watched Movies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (watchHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (watchHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "No Watched Movies",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Movies you watch will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(watchHistory) { item ->
                    WatchHistoryItemCard(
                        item = item,
                        onItemClick = { onMovieClick(item.id, item.mediaType) },
                        onDelete = { viewModel.removeFromWatchHistory(item) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Watch History") },
            text = { Text("Are you sure you want to delete all watched movies from your history?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearWatchHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WatchHistoryItemCard(
    item: WatchHistoryItem,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onItemClick)
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Poster Image
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            // Movie Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.watchedAtFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 8.sp
                )

                if (item.mediaType == "tv") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "S${item.season} E${item.episode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontSize = 8.sp
                    )
                }
            }

            // Delete Button Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

