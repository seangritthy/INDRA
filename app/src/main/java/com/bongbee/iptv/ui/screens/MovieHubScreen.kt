package com.bongbee.iptv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bongbee.iptv.R
import com.bongbee.iptv.model.Movie
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.viewmodel.IptvViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHubScreen(
    viewModel: IptvViewModel,
    onBack: () -> Unit,
    onMovieClick: (Int, String) -> Unit,
    startWithSearch: Boolean = false
) {
    var isSearchActive by remember { mutableStateOf(startWithSearch) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_tmdb), color = TextSecondary) },
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.fetchMovieHubMovies(searchQuery, 1)
                                keyboardController?.hide()
                            }),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = ""
                                        viewModel.fetchMovieHubMovies("", 1)
                                    } else {
                                        isSearchActive = false
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = TextPrimary)
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.fetchMovieHubMovies("", 1)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close), tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.movie_hub),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close), tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search), tint = TextPrimary)
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
        Box(modifier = Modifier.fillMaxSize().appBackground(viewModel).padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // FEATURED SLIDING POSTERS
                if (viewModel.movieHubMovies.isNotEmpty() && !viewModel.isMovieHubLoading) {
                    MovieSlidingHeader(viewModel.movieHubMovies.take(15)) { movie ->
                        onMovieClick(movie.id, movie.mediaType)
                    }
                }

                // Info Section
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sector_info, viewModel.movieHubPage, viewModel.movieHubTotalPages),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.results_count, viewModel.movieHubTotalResults),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // MOVIE GRID
                if (viewModel.isMovieHubLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentCyan)
                    }
                } else if (viewModel.movieHubMovies.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_data_tmdb), color = TextSecondary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(viewModel.movieHubMovies, key = { it.id.toString() + it.title + it.mediaType }) { movie ->
                            MovieCard(movie) {
                                onMovieClick(movie.id, movie.mediaType)
                            }
                        }
                    }
                }

                // NAVIGATION
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.fetchMovieHubMovies(searchQuery, viewModel.movieHubPage - 1) },
                        enabled = viewModel.movieHubPage > 1 && !viewModel.isMovieHubLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ElevatedSurface)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text(stringResource(R.string.prev_sector), style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = AccentCyan.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${viewModel.movieHubPage}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.fetchMovieHubMovies(searchQuery, viewModel.movieHubPage + 1) },
                        enabled = viewModel.movieHubPage < viewModel.movieHubTotalPages && !viewModel.isMovieHubLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ElevatedSurface)
                    ) {
                        Text(stringResource(R.string.next_sector), style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun MovieSlidingHeader(movies: List<Movie>, onClick: (Movie) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { movies.size })
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            if (movies.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % movies.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 40.dp),
        pageSpacing = 16.dp
    ) { page ->
        val movie = movies[page]
        Card(
            modifier = Modifier.fillMaxSize().clickable { onClick(movie) },
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                    )
                )
                
                if (movie.mediaType == "tv") {
                    Surface(
                        color = AccentCyan.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = stringResource(R.string.series),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                Text(
                    text = movie.title,
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
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
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 150f
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (movie.releaseDate != null) {
                    Text(
                        text = movie.releaseDate.split("-").firstOrNull() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Surface(
                color = AccentCyan.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
            ) {
                Text(
                    text = "4K",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 8.sp
                )
            }
        }
    }
}
