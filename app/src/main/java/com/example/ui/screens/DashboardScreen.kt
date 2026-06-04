package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TautulliSession
import com.example.data.TautulliSessions
import com.example.ui.TautulliViewModel
import com.example.ui.UiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TautulliViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activityState by viewModel.activityState.collectAsState()
    val useDemoMode by viewModel.useDemoMode.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Image(
                            painter            = painterResource(R.drawable.ic_toplex_logo),
                            contentDescription = "to/plex",
                            modifier           = Modifier.height(28.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (useDemoMode) PlexOrange else StreamCyan)
                            )
                            Text(
                                text = if (useDemoMode) "Demo Mode Active" else "Connected: $serverUrl",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
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
            when (val state = activityState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PlexOrange)
                    }
                }

                is UiState.Success -> {
                    DashboardContent(
                        sessionsData = state.data,
                        useDemoMode = useDemoMode
                    )
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

@Composable
fun DashboardContent(
    sessionsData: TautulliSessions,
    useDemoMode: Boolean
) {
    val sessionsList = sessionsData.sessions ?: emptyList()
    val count = sessionsData.streamCount ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Quick statistics overlay
        item {
            ServerSummaryBadge(sessionsData = sessionsData)
        }

        // Section title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Playing Now",
                    tint = PlexOrange
                )
                Text(
                    text = "Currently Streaming ($count)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (sessionsList.isEmpty()) {
            item {
                EmptyStreamsCardContent()
            }
        } else {
            items(sessionsList) { session ->
                SessionCard(sessionItem = session, useDemoMode = useDemoMode)
            }
        }
    }
}

@Composable
fun ServerSummaryBadge(sessionsData: TautulliSessions) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${sessionsData.streamCount ?: 0}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = PlexOrange
                )
                Text(
                    text = "Streams",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
                    .align(Alignment.CenterVertically)
            )

            val totalMbps = ((sessionsData.totalBandwidth ?: "0").toLongOrNull() ?: 0L)
            val lanMbps   = ((sessionsData.lanBandwidth   ?: "0").toLongOrNull() ?: 0L)
            val wanMbps   = ((sessionsData.wanBandwidth   ?: "0").toLongOrNull() ?: 0L)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1fM", totalMbps / 1000.0),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = StreamCyan
                )
                Text(
                    text = "Bandwidth",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (lanMbps > 0L || wanMbps > 0L) {
                    Text(
                        text = "LAN: ${String.format("%.1f", lanMbps / 1000.0)}  WAN: ${String.format("%.1f", wanMbps / 1000.0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
                    .align(Alignment.CenterVertically)
            )

            val hasTranscodes = sessionsData.sessions?.any { session ->
                (session.transcodeDecision ?: "").lowercase() == "transcode"
            } ?: false
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (hasTranscodes) "ON" else "OFF",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasTranscodes) StreamTranscodeYellow else Color.White
                )
                Text(
                    text = "Transcoding",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SessionCard(
    sessionItem: TautulliSession,
    useDemoMode: Boolean
) {
    val viewOffset = sessionItem.viewOffset ?: 0L
    val duration   = sessionItem.duration   ?: 0L
    val remainingMs   = if (duration > 0L) (duration - viewOffset).coerceAtLeast(0L) else 0L
    val remainingMins = (remainingMs / 60000L).toInt()
    val etaFormatted: String? = if (duration > 0L && remainingMs > 0L) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() + remainingMs }
        val hour = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
        val min  = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
        "$hour:${String.format("%02d", min)} $amPm"
    } else null

    val decision = (sessionItem.transcodeDecision ?: "direct play").lowercase().trim()
    val decisionLabel = when (decision) {
        "direct play"   -> "Direct Play"
        "direct stream" -> "Direct Stream"
        "transcode"     -> "Transcode"
        else            -> decision.replaceFirstChar { it.uppercase() }
    }
    val decisionColor = when (decision) {
        "transcode"     -> StreamTranscodeYellow
        "direct stream" -> StreamCyan
        else            -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Poster with resolution badge overlay
                Box(modifier = Modifier.size(width = 70.dp, height = 105.dp)) {
                    if (!sessionItem.thumb.isNullOrEmpty()) {
                        AsyncImage(
                            model = sessionItem.thumb,
                            contentDescription = "Poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (sessionItem.type == "episode") Icons.Default.Tv else Icons.Default.Movie,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    // Resolution badge overlaid on poster bottom-left
                    val res = sessionItem.videoResolution ?: ""
                    if (res.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = res.uppercase(),
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Content column — top: title/episode/time, bottom: stream type + user
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top group: title, episode info, time remaining
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (sessionItem.type == "episode") {
                            val showName   = sessionItem.grandparentTitle ?: ""
                            val seasonStr  = sessionItem.season ?: ""
                            val episodeStr = sessionItem.episode ?: ""
                            val epTitle    = sessionItem.title ?: ""

                            Text(
                                text = showName.ifEmpty { epTitle },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            val epInfo = buildString {
                                val s = seasonStr.toIntOrNull()
                                val e = episodeStr.toIntOrNull()
                                if (s != null && e != null) append("S$s • E$e")
                                else if (seasonStr.isNotEmpty()) append("S${seasonStr}E${episodeStr}")
                                if (isNotEmpty() && epTitle.isNotEmpty()) append(" – ")
                                if (epTitle.isNotEmpty()) append(epTitle)
                            }
                            if (epInfo.isNotEmpty()) {
                                Text(
                                    text = epInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Text(
                                text = sessionItem.title ?: "Unknown Title",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            val year = sessionItem.year?.ifEmpty { null }
                            Text(
                                text = if (year != null) "Movie ($year)" else "Movie",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        // Time remaining / ETA
                        if (remainingMins > 0 && etaFormatted != null) {
                            Text(
                                text = "$remainingMins min${if (remainingMins != 1) "s" else ""} left / ETA: $etaFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Bottom group: stream decision + user/player
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = decisionLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = decisionColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = sessionItem.username ?: "Guest",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PlexOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "on ${sessionItem.player ?: "Plex"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (sessionItem.state == "paused") Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = sessionItem.state,
                    tint = if (sessionItem.state == "paused") TextMuted else StreamCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            val progress = ((sessionItem.progressPercent ?: 0) / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = PlexOrange,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun EmptyStreamsCardContent() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TvOff,
                contentDescription = "Empty",
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Plex is Silent",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "No media stream session is currently active.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NewReleases,
            contentDescription = "Error icon",
            tint = PlexOrange,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connection Offline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToSettings,
            colors = ButtonDefaults.buttonColors(containerColor = PlexOrange, contentColor = Color(0xFF111215))
        ) {
            Text("Adjust Settings")
        }
    }
}

private val Int.hDp get() = this.dp
