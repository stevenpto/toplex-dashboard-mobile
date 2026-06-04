package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PlexMovieDetail
import com.example.data.TautulliHistoryItem
import com.example.ui.TautulliViewModel
import com.example.ui.UiState
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PlexOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

// ─── private data classes ─────────────────────────────────────────────────────

private data class UserMovieEntry(
    val title: String,
    val thumb: String,
    val watchCount: Int,
    val ratingKey: String,
    val subtitle: String = ""
)

private data class UserActivityRow(
    val username: String,
    val movies: List<UserMovieEntry>
)

// ─── helper functions ─────────────────────────────────────────────────────────

private fun derivePopularThisWeek(history: List<TautulliHistoryItem>): List<UserMovieEntry> {
    val sevenDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (7L * 24L * 3600L)
    return history
        .filter { it.type == "movie" && (it.date ?: 0L) >= sevenDaysAgoSecs }
        .groupBy { it.ratingKey ?: it.title ?: "?" }
        .map { (_, items) ->
            val first = items.first()
            UserMovieEntry(
                title      = first.title ?: "Unknown",
                thumb      = first.thumb ?: "",
                watchCount = items.size,
                ratingKey  = first.ratingKey ?: ""
            )
        }
        .sortedByDescending { it.watchCount }
        .take(4)
}

private fun deriveUserActivityRows(history: List<TautulliHistoryItem>): List<UserActivityRow> {
    val thirtyDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (30L * 24L * 3600L)
    return history
        .filter { it.type == "movie" && (it.date ?: 0L) >= thirtyDaysAgoSecs }
        .groupBy { it.user ?: "Plex User" }
        .map { (user, items) ->
            val last4 = items.sortedByDescending { it.date ?: 0L }.take(4)
            UserActivityRow(
                username = user,
                movies   = last4.map { m ->
                    UserMovieEntry(
                        title      = m.title ?: "Unknown",
                        thumb      = m.thumb ?: "",
                        watchCount = 1,
                        ratingKey  = m.ratingKey ?: ""
                    )
                }
            )
        }
        .filter { it.movies.isNotEmpty() }
        .sortedBy { it.username.lowercase() }
}

// ─── TV helper functions ──────────────────────────────────────────────────────

private fun derivePopularTVThisWeek(history: List<TautulliHistoryItem>): List<UserMovieEntry> {
    val sevenDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (7L * 24L * 3600L)
    return history
        .filter { it.type == "episode" && (it.date ?: 0L) >= sevenDaysAgoSecs }
        .groupBy { it.grandparentTitle ?: it.title ?: "?" }
        .map { (showTitle, items) ->
            val first = items.first()
            UserMovieEntry(
                title      = showTitle,
                thumb      = first.grandparentThumb?.takeIf { it.isNotEmpty() } ?: first.thumb ?: "",
                watchCount = items.size,
                ratingKey  = first.ratingKey ?: ""
            )
        }
        .sortedByDescending { it.watchCount }
        .take(4)
}

private fun deriveUserTVActivityRows(history: List<TautulliHistoryItem>): List<UserActivityRow> {
    val thirtyDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (30L * 24L * 3600L)
    return history
        .filter { it.type == "episode" && (it.date ?: 0L) >= thirtyDaysAgoSecs }
        .groupBy { it.user ?: "Plex User" }
        .map { (user, items) ->
            val last4 = items.sortedByDescending { it.date ?: 0L }.take(4)
            UserActivityRow(
                username = user,
                movies   = last4.map { ep ->
                    val epTitle    = ep.title?.takeIf { it.isNotEmpty() } ?: ""
                    val seasonName = ep.parentTitle?.takeIf { it.isNotEmpty() } ?: ""
                    val subtitleText = when {
                        epTitle.isNotEmpty() && seasonName.isNotEmpty() -> "$seasonName · $epTitle"
                        epTitle.isNotEmpty() -> epTitle
                        seasonName.isNotEmpty() -> seasonName
                        else -> ""
                    }
                    UserMovieEntry(
                        title      = ep.grandparentTitle ?: ep.title ?: "Unknown",
                        thumb      = ep.grandparentThumb?.takeIf { it.isNotEmpty() } ?: ep.thumb ?: "",
                        watchCount = 1,
                        ratingKey  = ep.ratingKey ?: "",
                        subtitle   = subtitleText
                    )
                }
            )
        }
        .filter { it.movies.isNotEmpty() }
        .sortedBy { it.username.lowercase() }
}

// ─── main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: TautulliViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyState    by viewModel.historyState.collectAsState()
    val movieDetailState by viewModel.movieDetailState.collectAsState()

    var selectedCategory by remember { mutableStateOf("movies") }
    val history            = (historyState as? UiState.Success)?.data ?: emptyList()
    val popularThisWeek    = remember(history) { derivePopularThisWeek(history) }
    val userActivityRows   = remember(history) { deriveUserActivityRows(history) }
    val popularTVThisWeek  = remember(history) { derivePopularTVThisWeek(history) }
    val userTVActivityRows = remember(history) { deriveUserTVActivityRows(history) }

    // Pick up to 6 random thumbs from watched content for the header collage
    val headerThumbs = remember(history, selectedCategory) {
        val thumbs = if (selectedCategory == "movies") {
            history.filter { it.type == "movie" && !it.thumb.isNullOrEmpty() }
                .map { it.thumb!! }
        } else {
            history.filter { it.type == "episode" && !it.grandparentThumb.isNullOrEmpty() }
                .map { it.grandparentThumb!! }
        }
        thumbs.distinct().shuffled().take(6)
    }

    Scaffold(
        topBar = { /* header is inline in the list */ },
        containerColor = DarkBg,
        modifier        = modifier.fillMaxSize()
    ) { innerPadding ->
        when (historyState) {
            is UiState.Loading -> {
                Box(
                    modifier            = Modifier.padding(innerPadding).fillMaxSize(),
                    contentAlignment    = Alignment.Center
                ) { CircularProgressIndicator(color = PlexOrange) }
            }
            is UiState.Error -> {
                ErrorStateView(
                    message              = (historyState as UiState.Error).message,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
            else -> {
                LazyColumn(
                    modifier       = Modifier.padding(innerPadding).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ── collage hero header ───────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            // Poster collage background
                            if (headerThumbs.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .height(180.dp)
                                ) {
                                    val cols = headerThumbs.chunked(3)
                                    cols.forEach { col ->
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(180.dp)
                                        ) {
                                            col.forEach { url ->
                                                AsyncImage(
                                                    model              = url,
                                                    contentDescription = null,
                                                    contentScale       = ContentScale.Crop,
                                                    modifier           = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                                // Dark scrim over the collage
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colorStops = arrayOf(
                                                    0.0f to Color.Black.copy(alpha = 0.55f),
                                                    0.6f to Color.Black.copy(alpha = 0.65f),
                                                    1.0f to DarkBg
                                                )
                                            )
                                        )
                                )
                            } else {
                                // No data yet — plain dark background
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(DarkBg)
                                )
                            }

                            // Content: title row + refresh + subtitle + toggle
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Title + refresh
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.People,
                                            contentDescription = "Users",
                                            tint               = PlexOrange
                                        )
                                        Text(
                                            text       = "Users",
                                            fontWeight = FontWeight.Bold,
                                            fontSize   = 20.sp,
                                            color      = Color.White
                                        )
                                    }
                                    IconButton(onClick = { viewModel.forceRefresh() }) {
                                        Icon(
                                            imageVector        = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint               = Color.White
                                        )
                                    }
                                }

                                // Bottom: subtitle + toggle
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text     = "Last 30 days · ${if (selectedCategory == "movies") "Movies" else "TV Shows"}",
                                        fontSize = 11.sp,
                                        color    = Color.White.copy(alpha = 0.7f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        CategoryPillButton(
                                            text     = "MOVIES",
                                            selected = selectedCategory == "movies",
                                            onClick  = { selectedCategory = "movies" }
                                        )
                                        CategoryPillButton(
                                            text     = "TV SHOWS",
                                            selected = selectedCategory == "tvshows",
                                            onClick  = { selectedCategory = "tvshows" }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── hint ──────────────────────────────────────────────
                    item {
                        Text(
                            text     = "Double-tap any poster to view details",
                            fontSize = 11.sp,
                            color    = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    if (selectedCategory == "movies") {
                        // ── Popular This Week (Movies) ────────────────────
                        if (popularThisWeek.isNotEmpty()) {
                            item {
                                UsersSectionHeader(
                                    title    = "POPULAR THIS WEEK",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(popularThisWeek, key = { idx, it -> "pop_${idx}_${it.ratingKey.ifEmpty { it.title }}" }) { _, movie ->
                                        PopularMoviePosterCard(
                                            movie       = movie,
                                            onDoubleTap = {
                                                if (movie.ratingKey.isNotEmpty()) viewModel.fetchMovieDetail(movie.ratingKey)
                                            }
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }

                        // ── Recent Activity (Movies) ──────────────────────
                        item {
                            UsersSectionHeader(
                                title    = "RECENT ACTIVITY",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        if (userActivityRows.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text      = "No movie activity found in the last 30 days.",
                                        color     = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(userActivityRows, key = { idx, it -> "user_${idx}_${it.username}" }) { _, row ->
                                UserActivityCard(
                                    row              = row,
                                    onMovieDoubleTap = { ratingKey ->
                                        if (ratingKey.isNotEmpty()) viewModel.fetchMovieDetail(ratingKey)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // ── Popular This Week (TV Shows) ──────────────────
                        if (popularTVThisWeek.isNotEmpty()) {
                            item {
                                UsersSectionHeader(
                                    title    = "POPULAR THIS WEEK",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier              = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(popularTVThisWeek, key = { idx, it -> "tvpop_${idx}_${it.ratingKey.ifEmpty { it.title }}" }) { _, show ->
                                        PopularShowPosterCard(show = show)
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }

                        // ── Recent Activity (TV Shows) ────────────────────
                        item {
                            UsersSectionHeader(
                                title    = "RECENT ACTIVITY",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        if (userTVActivityRows.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text      = "No TV activity found in the last 30 days.",
                                        color     = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(userTVActivityRows, key = { idx, it -> "tvuser_${idx}_${it.username}" }) { _, row ->
                                UserActivityCard(
                                    row              = row,
                                    onMovieDoubleTap = { /* no detail sheet for TV yet */ },
                                    modifier         = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── movie detail overlay ──────────────────────────────────────────────────
    when (val detail = movieDetailState) {
        is UiState.Loading -> {
            Box(
                modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PlexOrange) }
        }
        is UiState.Success -> {
            MovieDetailBottomSheet(
                detail    = detail.data,
                onDismiss = { viewModel.clearMovieDetail() }
            )
        }
        is UiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearMovieDetail() },
                title            = { Text("Could not load details", color = Color.White) },
                text             = { Text(detail.message, color = TextSecondary) },
                confirmButton    = {
                    TextButton(onClick = { viewModel.clearMovieDetail() }) {
                        Text("OK", color = PlexOrange)
                    }
                },
                containerColor = DarkSurface
            )
        }
        null -> { /* nothing */ }
    }
}

// ─── section header ───────────────────────────────────────────────────────────

@Composable
private fun UsersSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(PlexOrange, RoundedCornerShape(2.dp))
        )
        Text(
            text          = title,
            color         = TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}

// ─── popular movie poster card ────────────────────────────────────────────────

@Composable
private fun PopularMoviePosterCard(
    movie: UserMovieEntry,
    onDoubleTap: () -> Unit
) {
    Column(
        modifier              = Modifier.width(120.dp),
        horizontalAlignment   = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 170.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .pointerInput(movie.ratingKey) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                }
        ) {
            if (movie.thumb.isNotEmpty()) {
                AsyncImage(
                    model              = movie.thumb,
                    contentDescription = movie.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize().background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                }
            }

            // watch-count badge
            if (movie.watchCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text       = "${movie.watchCount}×",
                        color      = PlexOrange,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text       = movie.title,
            color      = Color.White,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            text     = "${movie.watchCount} ${if (movie.watchCount == 1) "watch" else "watches"}",
            color    = PlexOrange,
            fontSize = 10.sp
        )
    }
}

// ─── popular TV show poster card ──────────────────────────────────────────────

@Composable
private fun PopularShowPosterCard(show: UserMovieEntry) {
    Column(
        modifier            = Modifier.width(120.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 170.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
        ) {
            if (show.thumb.isNotEmpty()) {
                AsyncImage(
                    model              = show.thumb,
                    contentDescription = show.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize().background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                }
            }

            // episode-count badge
            if (show.watchCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text       = "${show.watchCount}×",
                        color      = PlexOrange,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text       = show.title,
            color      = Color.White,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            text     = "${show.watchCount} ${if (show.watchCount == 1) "play" else "plays"}",
            color    = PlexOrange,
            fontSize = 10.sp
        )
    }
}

// ─── per-user activity card ───────────────────────────────────────────────────

@Composable
private fun UserActivityCard(
    row: UserActivityRow,
    onMovieDoubleTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initials     = row.username.take(2).uppercase()
    val avatarColor  = remember(row.username) {
        val palette = listOf(
            Color(0xFFF9A01B), Color(0xFF26A69A), Color(0xFF5C6BC0),
            Color(0xFFEF5350), Color(0xFF66BB6A), Color(0xFFAB47BC)
        )
        palette[(row.username.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    Card(
        colors   = CardDefaults.cardColors(containerColor = DarkSurface),
        shape    = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 14.dp, start = 14.dp, end = 14.dp, bottom = 16.dp)) {
            // user avatar + name
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.18f))
                        .border(1.5.dp, avatarColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initials,
                        color      = avatarColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text       = row.username,
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Adaptive poster layout: single row on tablet (≥600dp), 2×2 grid on phone
            val isTablet = LocalConfiguration.current.screenWidthDp >= 600
            if (isTablet) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.movies.forEach { movie ->
                        UserMoviePosterItem(
                            movie       = movie,
                            onDoubleTap = { onMovieDoubleTap(movie.ratingKey) },
                            modifier    = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                val chunks = row.movies.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunks.forEach { pair ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { movie ->
                                UserMoviePosterItem(
                                    movie       = movie,
                                    onDoubleTap = { onMovieDoubleTap(movie.ratingKey) },
                                    modifier    = Modifier.weight(1f)
                                )
                            }
                            // pad last row if odd number of movies
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─── user movie poster item ───────────────────────────────────────────────────

@Composable
private fun UserMoviePosterItem(
    movie: UserMovieEntry,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .pointerInput(movie.ratingKey) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                }
        ) {
            if (movie.thumb.isNotEmpty()) {
                AsyncImage(
                    model              = movie.thumb,
                    contentDescription = movie.title,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize().background(Color(0xFF1E2024)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Movie, null, tint = TextMuted, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text       = movie.title,
            color      = TextSecondary,
            fontSize   = 10.sp,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
        if (movie.subtitle.isNotEmpty()) {
            Text(
                text       = movie.subtitle,
                color      = TextMuted,
                fontSize   = 9.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

// ─── movie detail bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MovieDetailBottomSheet(
    detail: PlexMovieDetail,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = DarkSurface,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // poster + core info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 155.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                ) {
                    if (!detail.thumb.isNullOrEmpty()) {
                        AsyncImage(
                            model              = detail.thumb,
                            contentDescription = detail.title,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(DarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Movie, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Column(
                    modifier              = Modifier.weight(1f).padding(top = 4.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = detail.title,
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 22.sp
                    )
                    if (detail.year != null) {
                        Text(text = detail.year, color = TextSecondary, fontSize = 13.sp)
                    }

                    // rating + content rating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (detail.rating != null) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Star, null, tint = PlexOrange, modifier = Modifier.size(14.dp))
                                Text(
                                    text       = String.format("%.1f", detail.rating),
                                    color      = PlexOrange,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (!detail.contentRating.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, TextMuted, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text       = detail.contentRating,
                                    color      = TextMuted,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!detail.duration.isNullOrEmpty()) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Text(detail.duration, color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    if (detail.directors.isNotEmpty()) {
                        Text(
                            text     = "Dir: ${detail.directors.joinToString(", ")}",
                            color    = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // genre chips
            if (detail.genres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    detail.genres.take(5).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .background(PlexOrange.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .border(1.dp, PlexOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text       = genre,
                                color      = PlexOrange,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // summary
            if (!detail.summary.isNullOrEmpty()) {
                Text(
                    text          = "ABOUT",
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text       = detail.summary,
                    color      = Color.White.copy(alpha = 0.85f),
                    fontSize   = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // cast
            if (detail.actors.isNotEmpty()) {
                Text(
                    text          = "CAST",
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    detail.actors.take(6).forEach { actor ->
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = actor, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
