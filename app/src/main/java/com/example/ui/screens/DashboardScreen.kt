package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
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
                        Text(
                            text = "Tautulli Dash",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
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

            // Dynamic speed format
            val bandwidthMbps = try {
                val bandwidthBps = (sessionsData.totalBandwidth ?: "0").toDoubleOrNull() ?: 0.0
                String.format("%.1f", bandwidthBps / 1000.0)
            } catch (e: Exception) {
                "0.0"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${bandwidthMbps}M",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = StreamCyan
                )
                Text(
                    text = "Bandwidth",
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

            val hasTranscodes = sessionsData.sessions?.any { session ->
                val decision = (session.transcodeDecision ?: "").lowercase()
                decision.contains("trans") || decision.contains("copy")
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Render Poster thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 75.dp)
                ) {
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (sessionItem.type == "episode") {
                        val showName = sessionItem.grandparentTitle ?: ""
                        val seasonStr = sessionItem.season ?: ""
                        val episodeStr = sessionItem.episode ?: ""
                        val epTitle = sessionItem.title ?: ""
                        val seasonTitle = sessionItem.parentTitle ?: ""

                        Text(
                            text = if (showName.isNotEmpty()) showName else epTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1
                        )

                        val formattedEpInfo = buildString {
                            if (seasonStr.isNotEmpty() && episodeStr.isNotEmpty()) {
                                val sParsed = seasonStr.toIntOrNull()
                                val eParsed = episodeStr.toIntOrNull()
                                if (sParsed != null && eParsed != null) {
                                    append(String.format("S%02dE%02d", sParsed, eParsed))
                                } else {
                                    append("S${seasonStr}E${episodeStr}")
                                }
                            } else if (seasonTitle.isNotEmpty()) {
                                append(seasonTitle)
                                if (episodeStr.isNotEmpty()) {
                                    append(" • Ep. $episodeStr")
                                }
                            }

                            if (isNotEmpty() && epTitle.isNotEmpty() && epTitle != showName) {
                                append(" • ")
                            }
                            if (epTitle.isNotEmpty() && epTitle != showName) {
                                append(epTitle)
                            }
                        }

                        if (formattedEpInfo.isNotEmpty()) {
                            Text(
                                text = formattedEpInfo,
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
                        val year = if (sessionItem.year.isNullOrEmpty()) "" else " (${sessionItem.year})"
                        Text(
                            text = "Movie$year",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.hDp))

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
                            text = "on ${sessionItem.player ?: "Plex Screen"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (sessionItem.state == "paused") Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = sessionItem.state,
                    tint = if (sessionItem.state == "paused") TextMuted else StreamCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = "Resolution",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = sessionItem.videoResolution ?: "1080p",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                val decision = (sessionItem.transcodeDecision ?: "direct play").lowercase()
                val isTranscoding = decision.contains("trans") || decision.contains("copy")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isTranscoding) StreamTranscodeYellow else StreamCyan)
                    )
                    Text(
                        text = decision.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTranscoding) StreamTranscodeYellow else StreamCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = sessionItem.ipAddress ?: "Local Network",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
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
