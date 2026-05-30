package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TautulliHistoryItem
import com.example.ui.TautulliViewModel
import com.example.ui.UiState
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TautulliViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyState by viewModel.historyState.collectAsState()
    val useDemoMode by viewModel.useDemoMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = PlexOrange
                            )
                            Text(
                                text = "Playback History",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Last 14 days · Movies & TV",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.forceRefresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = historyState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PlexOrange)
                    }
                }

                is UiState.Success -> {
                    HistoryContent(historyList = state.data)
                }

                is UiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

private data class HistoryActivityEntry(
    val day: String,
    val title: String,
    val showTitle: String,
    val user: String,
    val type: String,
    val thumb: String,
    val durationSecs: Long,
    val player: String,
    val dateUnix: Long
)

private data class HistoryMonthGroup(val monthLabel: String, val items: List<HistoryActivityEntry>)

private fun buildHistoryMonthGroups(history: List<TautulliHistoryItem>): List<HistoryMonthGroup> {
    val fourteenDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (14L * 24L * 3600L)
    val recent = history
        .filter { (it.date ?: 0L) >= fourteenDaysAgoSecs }
        .sortedByDescending { it.date ?: 0L }
    val cal = Calendar.getInstance()
    val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    return recent
        .groupBy { item ->
            val ts = (item.date ?: 0L) * 1000L
            cal.timeInMillis = ts
            "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        }
        .map { (monthLabel, items) ->
            HistoryMonthGroup(
                monthLabel = monthLabel,
                items = items.map { item ->
                    cal.timeInMillis = (item.date ?: 0L) * 1000L
                    val isEpisode = item.type == "episode" && !item.grandparentTitle.isNullOrEmpty()
                    HistoryActivityEntry(
                        day = cal.get(Calendar.DAY_OF_MONTH).toString(),
                        title = item.title ?: "Unknown",
                        showTitle = if (isEpisode) item.grandparentTitle ?: "" else "",
                        user = item.user ?: "Plex User",
                        type = item.type ?: "movie",
                        thumb = item.thumb ?: "",
                        durationSecs = item.duration ?: 0L,
                        player = item.player ?: "Plex",
                        dateUnix = item.date ?: 0L
                    )
                }
            )
        }
}

private fun formatHistoryDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
fun HistoryContent(historyList: List<TautulliHistoryItem>) {
    val groups = remember(historyList) { buildHistoryMonthGroups(historyList) }

    if (groups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HistoryToggleOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "No Movies or TV shows watched in the last 14 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groups.forEach { group ->
            // Month header
            item(key = "header_${group.monthLabel}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(PlexOrange, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = group.monthLabel,
                        color = PlexOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${group.items.size} ${if (group.items.size == 1) "play" else "plays"}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            items(group.items, key = { "${group.monthLabel}_${it.dateUnix}_${it.title}_${it.user}" }) { entry ->
                HistoryTimelineItem(entry = entry)
                Spacer(modifier = Modifier.height(10.dp))
            }

            item(key = "spacer_${group.monthLabel}") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HistoryTimelineItem(entry: HistoryActivityEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Day number badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PlexOrange.copy(alpha = 0.12f))
                .border(1.dp, PlexOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.day,
                color = PlexOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // Thumbnail
        if (entry.thumb.isNotEmpty()) {
            AsyncImage(
                model = entry.thumb,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 36.dp, height = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.type == "episode") Icons.Default.Tv else Icons.Default.Movie,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (entry.type == "episode") Color(0xFF1A3A5C) else Color(0xFF2A1A0C)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (entry.type == "episode") "TV" else "MOVIE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (entry.type == "episode") Color(0xFF5BB8F5) else PlexOrange
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.showTitle.isNotEmpty()) {
                Text(
                    text = entry.showTitle,
                    color = Color(0xFF5BB8F5).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.user,
                    color = PlexOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (entry.durationSecs > 0) {
                    Text(text = "·", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = formatHistoryDuration(entry.durationSecs),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(text = "·", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = entry.player,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

