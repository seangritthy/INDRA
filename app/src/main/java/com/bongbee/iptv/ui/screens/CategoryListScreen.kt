package com.bongbee.iptv.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.bongbee.iptv.R
import com.bongbee.iptv.model.Category
import com.bongbee.iptv.model.Country
import com.bongbee.iptv.model.Movie
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.delay

@Composable
fun CategoryListScreen(
    selectedTab: Int,
    categories: List<Category>,
    privateCategories: List<Category>,
    countries: List<Country>,
    regions: List<Country>,
    languages: List<Country>,
    showPrivateCategories: Boolean,
    translateCategory: (String) -> String,
    onCategoryClick: (Category, String) -> Unit,
    viewModel: IptvViewModel,
    onMovieHubMoreClick: () -> Unit,
    onMovieClick: (Int, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        if (selectedTab == 0) {
            // HERO SLIDESHOW
            if (categories.isNotEmpty()) {
                val featured = categories.take(11)
                SectionSlideshow(stringResource(R.string.featured_channels), featured, translateCategory) {
                    onCategoryClick(it, it.url)
                }
            }

            // TMDB MOVIE CATEGORIES
            MovieRowSection(stringResource(R.string.trending_movies), viewModel.trendingMovies, "trending_movies", viewModel, onMovieClick)
            MovieRowSection(stringResource(R.string.popular_movies), viewModel.popularMovies, "popular_movies", viewModel, onMovieClick)
            MovieRowSection(stringResource(R.string.top_rated_movies), viewModel.topRatedMovies, "top_rated_movies", viewModel, onMovieClick)
            MovieRowSection(stringResource(R.string.upcoming_movies), viewModel.upcomingMovies, "upcoming_movies", viewModel, onMovieClick)

            // TV SHOW CATEGORIES
            MovieRowSection(stringResource(R.string.trending_tv), viewModel.trendingTvShows, "trending_tv", viewModel, onMovieClick)
            MovieRowSection(stringResource(R.string.popular_tv), viewModel.popularTvShows, "popular_tv", viewModel, onMovieClick)
            MovieRowSection(stringResource(R.string.top_rated_tv), viewModel.topRatedTvShows, "top_rated_tv", viewModel, onMovieClick)
            
            // ON THE AIR with total count and Show More (Next)
            OnTheAirSection(viewModel, onMovieClick)
            
            // GLOBAL TOTALS AT THE BOTTOM
            TotalsSummary(viewModel)
        } else {
            // Other Tabs (Countries, Regions, Languages)
            val items = when (selectedTab) {
                1 -> countries
                2 -> regions
                3 -> languages
                else -> emptyList()
            }

            val title = when (selectedTab) {
                1 -> stringResource(R.string.by_country)
                2 -> stringResource(R.string.by_region)
                3 -> stringResource(R.string.by_language)
                else -> ""
            }

            if (items.isNotEmpty()) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )

                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                CountryCard(item) {
                                    onCategoryClick(Category(item.name, 0, item.url), item.url)
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnTheAirSection(
    viewModel: IptvViewModel, 
    onMovieClick: (Int, String) -> Unit
) {
    if (viewModel.onTheAirTvShows.isEmpty()) return
    
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.on_the_air),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = stringResource(R.string.results_count, viewModel.onTheAirTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
            TextButton(onClick = { viewModel.fetchNextSector("on_the_air") }) {
                Text(stringResource(R.string.next_sector), color = AccentCyan, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.onTheAirTvShows) { movie ->
                MovieHubCard(movie) { onMovieClick(movie.id, movie.mediaType) }
            }
        }
    }
}

@Composable
fun MovieRowSection(
    title: String, 
    movies: List<Movie>, 
    categoryKey: String,
    viewModel: IptvViewModel,
    onMovieClick: (Int, String) -> Unit
) {
    if (movies.isEmpty()) return
    
    val totalCount = when(categoryKey) {
        "trending_movies" -> viewModel.trendingMoviesTotal
        "popular_movies" -> viewModel.popularMoviesTotal
        "top_rated_movies" -> viewModel.topRatedMoviesTotal
        "upcoming_movies" -> viewModel.upcomingMoviesTotal
        "trending_tv" -> viewModel.trendingTvTotal
        "popular_tv" -> viewModel.popularTvTotal
        "top_rated_tv" -> viewModel.topRatedTvTotal
        else -> 0
    }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = stringResource(R.string.results_count, totalCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
            TextButton(onClick = { viewModel.fetchNextSector(categoryKey) }) {
                Text(stringResource(R.string.next_sector), color = AccentCyan, style = MaterialTheme.typography.labelSmall)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieHubCard(movie) { onMovieClick(movie.id, movie.mediaType) }
            }
        }
    }
}

@Composable
fun TotalsSummary(viewModel: IptvViewModel) {
    val totalMovies = viewModel.trendingMoviesTotal + viewModel.popularMoviesTotal + viewModel.topRatedMoviesTotal + viewModel.upcomingMoviesTotal
    val totalTv = viewModel.trendingTvTotal + viewModel.popularTvTotal + viewModel.topRatedTvTotal + viewModel.onTheAirTotal
    
    Surface(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        color = ElevatedSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.total_movies, totalMovies), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.total_tv_shows, totalTv), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
            Text(stringResource(R.string.total_all, totalMovies + totalTv), color = AccentCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MovieHubCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(195.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            
            if (movie.mediaType == "tv") {
                Surface(
                    color = AccentCyan.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = stringResource(R.string.series),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        color = Color.Black
                    )
                }
            }

            Text(
                text = movie.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionSlideshow(
    title: String,
    items: List<Category>,
    translateCategory: (String) -> String,
    onItemClick: (Category) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items) {
        while (true) {
            delay(8000)
            if (items.isNotEmpty()) {
                val nextP = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextP)
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 2.sp
        )
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val category = items[page]
            CategoryCardLarge(category, translateCategory) { onItemClick(category) }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CategoryVideoPlayer(videoResName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(videoResName) {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/raw/$videoResName")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
            volume = 0f
        }
    }

    DisposableEffect(videoResName) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
}

@Composable
fun CategoryCardLarge(
    category: Category,
    translateCategory: (String) -> String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            category.videoResName?.let { videoName ->
                CategoryVideoPlayer(videoResName = videoName, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            } ?: run {
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(PrimaryPurple.copy(alpha = 0.1f), Color.Transparent), radius = 400f)))
            }

            Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart), verticalArrangement = Arrangement.Center) {
                Icon(imageVector = getCategoryIcon(category.name), contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = translateCategory(category.name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (category.isWeb) stringResource(R.string.streaming_badge) else stringResource(R.string.channels_count_label, category.count),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FeaturedWebSlideshow(
    title: String,
    color: Color,
    videoResName: String? = null,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 2.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 24.dp).clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSurface)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                videoResName?.let { videoName ->
                    CategoryVideoPlayer(videoResName = videoName, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(color.copy(alpha = 0.1f), Color.Transparent), radius = 400f)))
                
                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = color.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.exclusive),
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.web_portal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryCard(country: Country, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = country.name.take(2),
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = country.name.drop(2).trim(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "movies" -> Icons.Default.Movie
        "news" -> Icons.Default.Info
        "sports" -> Icons.Default.Sports
        "music" -> Icons.Default.MusicNote
        "kids" -> Icons.Default.ChildCare
        "entertainment" -> Icons.Default.TheaterComedy
        "documentary" -> Icons.AutoMirrored.Filled.MenuBook
        "education" -> Icons.Default.School
        "series" -> Icons.Default.Tv
        "animation" -> Icons.Default.Face
        "religious" -> Icons.Default.Church
        "science" -> Icons.Default.Science
        "travel" -> Icons.Default.TravelExplore
        else -> Icons.Default.LiveTv
    }
}
