package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.PlexLibraryStats
import com.example.data.TautulliHistoryItem
import com.example.data.TautulliSession
import com.example.ui.TautulliViewModel
import com.example.ui.UiState
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Date

// Brand Palette additions for the redesigned stats dashboard
val NeonGreen = Color(0xFF00FF88)
val NeonGreenDark = Color(0xFF05C46B)
val GlassBorderColor = Color(0xFF2E3138)
val TextMutedGreen = Color(0xFF5EDCA3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: TautulliViewModel,
    modifier: Modifier = Modifier
) {
    val historyState by viewModel.historyState.collectAsState()
    val activityState by viewModel.activityState.collectAsState()
    val useDemoMode by viewModel.useDemoMode.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverType by viewModel.serverType.collectAsState()
    val plexLibraryStats by viewModel.plexLibraryStats.collectAsState()

    val scrollState = rememberScrollState()
    
    // Main Selection Category matching Web version: "movies" or "tvshows"
    var selectedCategory by remember { mutableStateOf("movies") }

    // Loading / Synced diagnostics
    val isLoading = historyState is UiState.Loading || activityState is UiState.Loading
    val spinAngle = remember { Animatable(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            spinAngle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            spinAngle.snapTo(0f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Text(
                                text = "Toplex Analytics",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 19.sp
                            )
                        }
                        Text(
                            text = if (useDemoMode) "Demo Environment" else "Online • ${serverType.uppercase()}",
                            fontSize = 11.sp,
                            color = TextMutedGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.forceRefresh() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh statistics",
                            tint = NeonGreen,
                            modifier = if (isLoading) Modifier.rotate(spinAngle.value) else Modifier
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
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonGreen, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing media logs...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Compiling segmented playtimes, device platforms, and peak loops",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                historyState is UiState.Error && !useDemoMode -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Not connected",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "No Live Stream History Connected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connect to your Tautulli or Plex Media Server in Connection settings, or toggle Demo mode to view premium statistics charts.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.toggleDemoMode(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Toggle Demo Analytics Mode",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111215)
                            )
                        }
                    }
                }
                else -> {
                    val successHistory = (historyState as? UiState.Success)?.data ?: emptyList()
                    val successSessions = (activityState as? UiState.Success)?.data?.sessions ?: emptyList()

                    val enrichedHistory = remember(successHistory, useDemoMode) {
                        getEnrichedHistory(successHistory, useDemoMode)
                    }

                    // Dynamically compile stats based on Selected pill Category (MOVIES vs TV SHOWS)
                    val compiled = remember(enrichedHistory, successSessions, selectedCategory) {
                        // Filter history to last 60 days.
                        val sixtyDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (60L * 24L * 3600L)
                        val sixtyDayHistory = enrichedHistory.filter { (it.date ?: 0L) >= sixtyDaysAgoSecs }
                        
                        // Fallback to full history if 60-day history is empty but complete history is populated, ensuring we never display empty stats unnecessarily
                        val safeHistory = if (sixtyDayHistory.isEmpty() && enrichedHistory.isNotEmpty()) enrichedHistory else sixtyDayHistory

                        val historySegmented = if (selectedCategory == "movies") {
                            safeHistory.filter { it.type == "movie" }
                        } else {
                            safeHistory.filter { it.type == "episode" }
                        }
                        val sessionsSegmented = if (selectedCategory == "movies") {
                            successSessions.filter { it.type == "movie" }
                        } else {
                            successSessions.filter { it.type == "episode" }
                        }
                        compileStats(historySegmented, sessionsSegmented, selectedCategory)
                    }

                    // Derive the most popular item this week for the hero banner
                    val heroItem = remember(enrichedHistory, selectedCategory) {
                        val sevenDaysAgoSecs = (System.currentTimeMillis() / 1000L) - (7L * 24L * 3600L)
                        val weekHistory = enrichedHistory
                            .filter { (it.date ?: 0L) >= sevenDaysAgoSecs }
                            .let { wh ->
                                if (selectedCategory == "movies") wh.filter { it.type == "movie" }
                                else wh.filter { it.type == "episode" }
                            }
                        val groups = weekHistory.groupBy {
                            if (it.type == "episode") it.grandparentTitle?.takeIf { s -> s.isNotEmpty() } ?: it.title ?: "Unknown"
                            else it.title ?: "Unknown"
                        }
                        groups.maxByOrNull { it.value.size }?.let { (title, items) ->
                            TopMediaItem(
                                title = title,
                                count = items.size,
                                type = if (selectedCategory == "movies") "movie" else "episode",
                                subtitle = "",
                                thumb = items.firstOrNull()?.thumb ?: ""
                            )
                        } ?: compiled.topMediaList.firstOrNull()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    ) {
                        // Hero banner — full-bleed background artwork of most popular this week
                        StatsMostPopularHero(
                            topItem = heroItem,
                            category = selectedCategory
                        )

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        // Stats header row — mirrors web StatsBar (Films Added / Watches / Hours / Users)
                        StatsHeaderRow(
                            history = enrichedHistory,
                            libraryStats = plexLibraryStats,
                            category = selectedCategory
                        )

                        // Web style Custom Green Pill Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            CategoryPillButton(
                                text = "MOVIES",
                                selected = selectedCategory == "movies",
                                onClick = { selectedCategory = "movies" }
                            )
                            CategoryPillButton(
                                text = "TV SHOWS",
                                selected = selectedCategory == "tvshows",
                                onClick = { selectedCategory = "tvshows" }
                            )
                        }

                        // Crossfade animation on category change for super smooth interactive styling
                        Crossfade(
                            targetState = compiled,
                            animationSpec = tween(350, easing = EaseOutExpo),
                            label = "analytics_view_transition"
                        ) { stats ->
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                // 0. Watch Chart (BY WEEK) — mirrors WatchChart.jsx
                                WatchChartCard(
                                    weeklyBins = stats.weeklyBins,
                                    dayOfWeekBins = stats.dayOfWeekBins,
                                    totalPlays = stats.totalPlayCount,
                                    category = selectedCategory
                                )

                                // 1. Metrics Grid
                                ServerOverviewGrid(
                                    playsLabel = if (selectedCategory == "movies") "Total Plays" else "Episode Plays",
                                    avgLabel = if (selectedCategory == "movies") "Avg Runtime" else "Avg Episode",
                                    totalPlays = stats.totalPlayCount,
                                    totalDurationSecs = stats.totalPlayTimeSecs,
                                    activeProfiles = stats.uniqueUsersCount,
                                    avgDurationSecs = stats.avgPlayTimeMins * 60L
                                )

                                // 2. Top Popular content ranking with Art posters!
                                PopularContentDeck(
                                    title = if (selectedCategory == "movies") "Most Watched Movies" else "Top TV Series (Shows)",
                                    topMedia = stats.topMediaList
                                )

                                // 3. Active Profile view ratios
                                TopWatchersCard(
                                    title = if (selectedCategory == "movies") "Top Movie Profiles" else "Top Series Profiles",
                                    users = stats.topUsers
                                )

                                // 4. Runtime tiers distributions
                                LibraryAllocationDynamic(
                                    title = if (selectedCategory == "movies") "Movie Watch Length Tiers" else "TV Runtime Shares",
                                    subtitle = "Segmentation by play sessions duration",
                                    categories = stats.libraryDistribution
                                )

                                // 5. Platform choices
                                PlatformDistributionCard(players = stats.playersList, totalPlays = stats.totalPlayCount)

                                // 6. Peak viewing hours
                                WatchHourPeaksCard(hourStats = stats.hourStats)

                                // 7. Month-grouped activity feed — mirrors ActivityFeed.jsx
                                MonthActivityFeedCard(
                                    history = enrichedHistory,
                                    category = selectedCategory
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                        } // end inner padded Column
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsMostPopularHero — full-bleed backdrop showing the top movie/show this week
// Mirrors the web dashboard hero header with artwork + gradient scrim
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatsMostPopularHero(topItem: TopMediaItem?, category: String) {
    if (topItem == null) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // Artwork background — poster thumbnail from Plex, cropped to fill
        if (topItem.thumb.isNotEmpty()) {
            AsyncImage(
                model = topItem.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1115))
            )
        }

        // Gradient scrim: transparent top → near-opaque dark bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.15f),
                        0.45f to Color.Black.copy(alpha = 0.5f),
                        1.0f to Color(0xFF0D0F12).copy(alpha = 0.97f)
                    )
                )
        )

        // Text content anchored to bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "MOST POPULAR THIS WEEK",
                color = NeonGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = topItem.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "${topItem.count} ${if (topItem.count == 1) "watch" else "watches"}",
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Web-inspired glowing green categories selector pill
@Composable
fun CategoryPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) NeonGreen else Color(0xFF1E2024),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "pill_background"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF111215) else TextSecondary,
        animationSpec = tween(durationMillis = 250),
        label = "pill_text"
    )
    val borderStroke = if (selected) {
        Modifier.border(1.dp, NeonGreen, RoundedCornerShape(24.dp))
    } else {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .then(borderStroke)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111215))
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

// Gorgeous Glassmorphic Card container with glowing vector border
@Composable
fun ToplexCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonGreen.copy(alpha = 0.15f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.01f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                clip = false,
                ambientColor = NeonGreen.copy(alpha = 0.02f),
                spotColor = NeonGreen.copy(alpha = 0.08f)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

// Metric block grid
@Composable
fun ServerOverviewGrid(
    playsLabel: String,
    avgLabel: String,
    totalPlays: Int,
    totalDurationSecs: Long,
    activeProfiles: Int,
    avgDurationSecs: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricBox(
                title = "TIME STREAMED",
                value = formatDuration(totalDurationSecs),
                subtitle = "Last 60 days duration",
                icon = Icons.Default.History,
                accentColor = NeonGreen,
                modifier = Modifier.weight(1f)
            )
            MetricBox(
                title = playsLabel.uppercase(),
                value = String.format("%,d", totalPlays),
                subtitle = "Successful plays (60d)",
                icon = Icons.Default.PlayArrow,
                accentColor = NeonGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricBox(
                title = "PROFILES",
                value = activeProfiles.toString(),
                subtitle = "Active users (last 60d)",
                icon = Icons.Default.People,
                accentColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
            val avgMinutes = (avgDurationSecs / 60L).coerceAtLeast(1L)
            val formattedAvg = if (avgMinutes > 60) "${avgMinutes / 60}h ${avgMinutes % 60}m" else "${avgMinutes}m"
            MetricBox(
                title = avgLabel.uppercase(),
                value = formattedAvg,
                subtitle = "Avg watch time (60d)",
                icon = Icons.Default.AccessTime,
                accentColor = Color(0xFF00FF88),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.20f),
                        accentColor.copy(alpha = 0.01f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// List of profiles with green progress feedback
@Composable
fun TopWatchersCard(title: String, users: List<UserPlayStat>) {
    ToplexCard(
        borderColor = NeonGreen.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Most active watchers volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No user streams calculated yet.", color = TextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                users.take(5).forEachIndexed { _, user ->
                    val avatarColor = remember(user.username) {
                        val colorsList = listOf(NeonGreen, Color(0xFF00E676), Color(0xFF26A69A), Color(0xFF4DB6AC), Color(0xFF80CBC4))
                        colorsList[(user.username.hashCode() and Int.MAX_VALUE) % colorsList.size]
                    }
                    val initials = if (user.username.isNotEmpty()) user.username.take(2).uppercase() else "U"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(avatarColor.copy(alpha = 0.12f))
                                .border(1.dp, avatarColor.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = avatarColor
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user.username,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${user.plays} plays",
                                    fontSize = 12.sp,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Glowing green progress slider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(user.percentage.coerceIn(0.01f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(avatarColor.copy(alpha = 0.4f), avatarColor)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Platform distributions styled with sleek outlines
@Composable
fun PlatformDistributionCard(players: List<Pair<String, Int>>, totalPlays: Int) {
    ToplexCard(
        borderColor = NeonGreen.copy(alpha = 0.15f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Platforms & Clients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Most frequent playing accessories",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No players analyzed yet.", color = TextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                players.take(4).forEach { (player, count) ->
                    val share = if (totalPlays > 0) count.toFloat() / totalPlays else 0f
                    val sharePercent = if (totalPlays > 0) (count * 100) / totalPlays else 0
                    val deviceIcon = getPlayerIcon(player)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = deviceIcon,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$count plays ($sharePercent%)",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(share.coerceIn(0.1f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(NeonGreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dynamic Watch Tiers distributions
@Composable
fun LibraryAllocationDynamic(
    title: String,
    subtitle: String,
    categories: List<LibAllocation>
) {
    ToplexCard(
        borderColor = NeonGreen.copy(alpha = 0.15f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom composite stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.03f))
        ) {
            categories.forEach { cat ->
                if (cat.fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(cat.fraction.coerceIn(0.01f, 1f))
                            .background(cat.color)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEach { cat ->
                val fracPercent = (cat.fraction * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(cat.color)
                        )
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$fracPercent%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Popular movies / tv shows layout deck with image posters
@Composable
fun PopularContentDeck(title: String, topMedia: List<TopMediaItem>) {
    ToplexCard(
        borderColor = NeonGreen.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Highest viewer concentration ranks",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (topMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No watched entries calculated.", color = TextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                topMedia.take(4).forEachIndexed { keyIdx, item ->
                    val posterUrl = item.thumb.ifEmpty { getPosterUrlForMedia(item.title, item.type) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.015f), RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image poster render
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = "Poster for ${item.title}",
                            modifier = Modifier
                                .size(width = 46.dp, height = 66.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonGreen.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.type.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen
                                    )
                                }
                                Text(
                                    text = item.subtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${item.count} Plays",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// Peak watching hours bar graphics
@Composable
fun WatchHourPeaksCard(hourStats: List<HourStat>) {
    val peakVal = hourStats.maxOfOrNull { it.weight }?.coerceAtLeast(1f) ?: 1f

    ToplexCard(
        borderColor = NeonGreen.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Weekly Activity Peak Hours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Chronological concentration metrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            // Draw visual gridlines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.04f))
                    )
                }
                Spacer(modifier = Modifier.height(18.dp)) // margin for labels
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                hourStats.forEach { hourItem ->
                    val barFraction = (hourItem.weight / peakVal).coerceIn(0.12f, 1.0f)
                    val isPeak = hourItem.weight == peakVal

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = hourItem.weight.toInt().toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPeak) NeonGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(barFraction * 0.75f)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            NeonGreen,
                                            NeonGreen.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NeonGreen.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hourItem.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPeak) NeonGreen else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Data models and compilation logic
data class UserPlayStat(val username: String, val plays: Int, val percentage: Float)
data class LibAllocation(val name: String, val fraction: Float, val color: Color, val percentStr: String)
data class HourStat(val label: String, val weight: Float)
data class TopMediaItem(val title: String, val count: Int, val type: String, val subtitle: String, val thumb: String = "")

data class CompiledStats(
    val totalPlayCount: Int,
    val totalPlayTimeSecs: Long,
    val uniqueUsersCount: Int,
    val avgPlayTimeMins: Long,
    val topUsers: List<UserPlayStat>,
    val playersList: List<Pair<String, Int>>,
    val libraryDistribution: List<LibAllocation>,
    val hourStats: List<HourStat>,
    val topMediaList: List<TopMediaItem>,
    val weeklyBins: List<Int> = emptyList(),
    val dayOfWeekBins: List<Pair<String, Int>> = emptyList()
)

// Maps media backdrops using Unsplash content
fun getPosterUrlForMedia(title: String, type: String): String {
    val cleanTitle = title.lowercase()
    return when {
        cleanTitle.contains("dune") -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&q=80"
        cleanTitle.contains("breaking bad") || cleanTitle.contains("ozymandias") -> "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=400&q=80"
        cleanTitle.contains("succession") || cleanTitle.contains("wedding") -> "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=400&q=80"
        cleanTitle.contains("severance") || cleanTitle.contains("pilot") -> "https://images.unsplash.com/photo-1497366216548-37526070297c?w=400&q=80"
        cleanTitle.contains("interstellar") -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80"
        cleanTitle.contains("oppenheimer") -> "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=400&q=80"
        type == "movie" -> "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400&q=80"
        else -> "https://images.unsplash.com/photo-1593305841991-05c297ba4575?w=400&q=80"
    }
}

// Statistical compilation core
private fun compileStats(
    historyList: List<TautulliHistoryItem>,
    activeSessions: List<TautulliSession>,
    category: String
): CompiledStats {
    val totalPlays = historyList.size + activeSessions.size
    val totalDurationUnits = historyList.sumOf { it.duration ?: 0L }

    val uniqueWatchers = (historyList.mapNotNull { it.user } + activeSessions.mapNotNull { it.user }).distinct()
    val watcherCount = uniqueWatchers.size

    // Average duration in units
    val avgSeconds = if (historyList.isNotEmpty()) totalDurationUnits / historyList.size else {
        if (category == "movies") 7200L else 2400L
    }

    // Profiles analytics
    val userGroups = historyList.groupBy { it.user ?: "Plex User" }
    val maxPlaysCount = userGroups.maxOfOrNull { it.value.size }?.coerceAtLeast(1) ?: 1
    val computedUsers = userGroups.map { (user, items) ->
        val plays = items.size
        UserPlayStat(
            username = user,
            plays = plays,
            percentage = plays.toFloat() / maxPlaysCount.toFloat()
        )
    }.sortedByDescending { it.plays }

    // Platform accessories compilation
    val playerGroups = (historyList.mapNotNull { it.player } + activeSessions.mapNotNull { it.player })
        .groupBy {
            val name = it.trim()
            if (name.isEmpty()) "Plex Media Player" else name
        }
    val computedPlayers = playerGroups.map { (player, items) ->
        Pair(player, items.size)
    }.sortedByDescending { it.second }

    // Dynamic Watch Duration Tiers based on selected categories
    val totalSumCoerce = totalPlays.coerceAtLeast(1).toFloat()
    val categories = if (category == "movies") {
        // Feature (>90m), Mid length (45-90m), Short clips (<45m)
        val featureCount = historyList.count { (it.duration ?: 0L) > 5400L } + activeSessions.size // assume movie is feature
        val midCount = historyList.count { (it.duration ?: 0L) in 2700L..5400L }
        val shortCount = totalPlays.toInt() - featureCount - midCount
        
        listOf(
            LibAllocation("Feature (>90m)", featureCount.toFloat() / totalSumCoerce, NeonGreen, "${(featureCount * 100) / totalSumCoerce.toInt()}%"),
            LibAllocation("Mid-tier (45-90m)", midCount.toFloat() / totalSumCoerce, Color(0xFF20D680), "${(midCount * 100) / totalSumCoerce.toInt()}%"),
            LibAllocation("Short Clips (<45m)", shortCount.toFloat().coerceAtLeast(0f) / totalSumCoerce, Color(0xFF0D9488), "${((shortCount.coerceAtLeast(0) * 100) / totalSumCoerce.toInt())}%")
        )
    } else {
        // Dramatic Epic (>45m), Standard Episode (20-45m), Short Animation (<20m)
        val epicCount = historyList.count { (it.duration ?: 0L) > 2700L }
        val stdCount = historyList.count { (it.duration ?: 0L) in 1200L..2700L } + activeSessions.size // active episodes
        val shortCount = totalPlays.toInt() - epicCount - stdCount
        
        listOf(
            LibAllocation("Drama (45m+)", epicCount.toFloat() / totalSumCoerce, NeonGreen, "${(epicCount * 100) / totalSumCoerce.toInt()}%"),
            LibAllocation("Standard (20-45m)", stdCount.toFloat() / totalSumCoerce, Color(0xFF20D680), "${(stdCount * 100) / totalSumCoerce.toInt()}%"),
            LibAllocation("Short / Anime (<20m)", shortCount.toFloat().coerceAtLeast(0f) / totalSumCoerce, Color(0xFF0D9488), "${((shortCount.coerceAtLeast(0) * 100) / totalSumCoerce.toInt())}%")
        )
    }

    // Popular Media analysis (grouped by grandparentTitle for TV series, and title for Movies)
    val mediaGrouping = historyList.groupBy {
        if (it.type == "episode" && !it.grandparentTitle.isNullOrEmpty()) {
            Pair(it.grandparentTitle ?: "Unknown TV Show", "episode")
        } else {
            Pair(it.title ?: "Unknown Movie", "movie")
        }
    }
    val compiledPopular = mediaGrouping.map { (key, list) ->
        val (title, type) = key
        val subtitle = if (type == "episode") {
            val episodesSet = list.mapNotNull { it.title }.distinct().size
            if (episodesSet > 1) "$episodesSet episodes watched" else "1 episode watched"
        } else {
            "Standalone Movie"
        }
        TopMediaItem(title, list.size, type, subtitle, thumb = list.firstOrNull()?.thumb ?: "")
    }.sortedByDescending { it.count }

    // Hour peaks analysis
    val hoursBins = IntArray(6)
    historyList.forEach { item ->
        val dateVal = item.date ?: 0L
        if (dateVal > 0) {
            val cal = Calendar.getInstance()
            cal.time = Date(dateVal * 1000)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val binIdx = when (hour) {
                in 2..5 -> 0
                in 6..9 -> 1
                in 10..13 -> 2
                in 14..17 -> 3
                in 18..21 -> 4
                else -> 5
            }
            hoursBins[binIdx]++
        }
    }

    val finalHoursData = listOf(
        HourStat("04:00", hoursBins[0].toFloat()),
        HourStat("08:00", hoursBins[1].toFloat()),
        HourStat("12:00", hoursBins[2].toFloat()),
        HourStat("16:00", hoursBins[3].toFloat()),
        HourStat("20:00", hoursBins[4].toFloat()),
        HourStat("00:00", hoursBins[5].toFloat())
    )

    // Weekly bins (52 weeks, indexed by WEEK_OF_YEAR - 1)
    val weeklyBinsArr = IntArray(52)
    // Day-of-week bins (0=Mon .. 6=Sun)
    val dowBinsArr = IntArray(7)
    historyList.forEach { item ->
        val dateVal = item.date ?: 0L
        if (dateVal > 0) {
            val cal = Calendar.getInstance()
            cal.time = Date(dateVal * 1000)
            val weekIdx = (cal.get(Calendar.WEEK_OF_YEAR) - 1).coerceIn(0, 51)
            weeklyBinsArr[weekIdx]++
            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
            val dowIdx = if (dow == 1) 6 else dow - 2 // Mon=0..Sun=6
            dowBinsArr[dowIdx.coerceIn(0, 6)]++
        }
    }
    val computedDowBins = listOf("M","T","W","T","F","S","S").zip(dowBinsArr.toList())

    return CompiledStats(
        totalPlayCount = totalPlays,
        totalPlayTimeSecs = totalDurationUnits,
        uniqueUsersCount = watcherCount.coerceAtLeast(1),
        avgPlayTimeMins = avgSeconds / 60,
        topUsers = computedUsers,
        playersList = computedPlayers,
        libraryDistribution = categories,
        hourStats = finalHoursData,
        topMediaList = compiledPopular,
        weeklyBins = weeklyBinsArr.toList(),
        dayOfWeekBins = computedDowBins
    )
}

// Converts duration seconds to days/hours/minutes clearly
private fun formatDuration(totalSecs: Long): String {
    val days = totalSecs / (24 * 3600)
    val hours = (totalSecs % (24 * 3600)) / 3600
    val mins = (totalSecs % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h ${mins}m"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}

// Map platform indicators
private fun getPlayerIcon(player: String): ImageVector {
    val name = player.lowercase()
    return when {
        name.contains("ios") || name.contains("iphone") || name.contains("mobile") -> Icons.Default.Smartphone
        name.contains("ipad") || name.contains("tablet") -> Icons.Default.Tablet
        name.contains("chrome") || name.contains("firefox") || name.contains("web") -> Icons.Default.Laptop
        name.contains("cast") || name.contains("airplay") -> Icons.Default.Cast
        else -> Icons.Default.Tv
    }
}

// Full analytical simulated database for Demo state (20 items, 8-week spread)
private fun getEnrichedHistory(realHistory: List<TautulliHistoryItem>, isDemoMode: Boolean): List<TautulliHistoryItem> {
    if (isDemoMode) {
        val b = System.currentTimeMillis() / 1000L
        val w = 604800L   // 1 week in seconds
        val d = 86400L    // 1 day in seconds
        return listOf(
            // Week 0 (this week)
            TautulliHistoryItem(1,  b-1200,   7500, "Apple TV 4K",       "Steve",   "Dune: Part Two",        "",         "",           "movie",   1, "Apple TV",          "192.168.1.144", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&q=80", "1001"),
            TautulliHistoryItem(2,  b-5400,   3600, "Shield Android TV", "Jesse",   "Ozymandias",            "Season 5", "Breaking Bad","episode", 1, "Shield Android TV", "192.168.1.102", "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", "1002"),
            TautulliHistoryItem(3,  b-d,      4800, "iPhone 15",         "Cooper",  "Interstellar",          "",         "",           "movie",   1, "Plex for iOS",      "172.56.21.90",  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&q=80", "1003"),
            TautulliHistoryItem(4,  b-2*d,    2900, "iPad Air",          "Sarah",   "Connor's Wedding",      "Season 4", "Succession", "episode", 1, "Plex for iPad",     "10.0.0.45",     "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=200&q=80", "1004"),
            // Week 1
            TautulliHistoryItem(5,  b-w-d,    3200, "Chrome (MacOS)",    "Mike",    "Pilot",                 "Season 1", "Severance",  "episode", 1, "Plex Web (Chrome)", "99.102.34.12",  "https://images.unsplash.com/photo-1497366216548-37526070297c?w=200&q=80", "1005"),
            TautulliHistoryItem(6,  b-w-2*d,  8100, "LG WebOS TV",       "Steve",   "Oppenheimer",           "",         "",           "movie",   1, "LG TV Client",      "192.168.1.115", "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=200&q=80", "1006"),
            TautulliHistoryItem(7,  b-w-4*d,  5400, "Apple TV 4K",       "Sarah",   "Interstellar",          "",         "",           "movie",   1, "Apple TV",          "10.0.0.45",     "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&q=80", "1007"),
            // Week 2
            TautulliHistoryItem(8,  b-2*w-d,  2700, "Shield Android TV", "Steve",   "Ozymandias",            "Season 5", "Breaking Bad","episode", 1, "Shield Android TV", "192.168.1.144", "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", "1008"),
            TautulliHistoryItem(9,  b-2*w-3*d,3100, "Shield Android TV", "Jesse",   "Pilot",                 "Season 1", "Severance",  "episode", 1, "Shield Android TV", "192.168.1.102", "https://images.unsplash.com/photo-1497366216548-37526070297c?w=200&q=80", "1009"),
            // Week 3
            TautulliHistoryItem(10, b-3*w,    6400, "iPhone 15",         "Sarah",   "Dune: Part Two",        "",         "",           "movie",   1, "Plex for iOS",      "10.0.0.45",     "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&q=80", "1010"),
            TautulliHistoryItem(11, b-3*w-2*d,2400, "Chrome (MacOS)",    "Cooper",  "Connor's Wedding",      "Season 4", "Succession", "episode", 1, "Plex Web (Chrome)", "172.56.21.90",  "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=200&q=80", "1011"),
            TautulliHistoryItem(12, b-3*w-5*d,5500, "iPad Air",          "Mike",    "Oppenheimer",           "",         "",           "movie",   1, "Plex for iPad",     "99.102.34.12",  "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=200&q=80", "1012"),
            // Week 4
            TautulliHistoryItem(13, b-4*w-d,  3300, "Apple TV 4K",       "Steve",   "Connor's Wedding",      "Season 4", "Succession", "episode", 1, "Apple TV",          "192.168.1.144", "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=200&q=80", "1013"),
            TautulliHistoryItem(14, b-4*w-3*d,6100, "Shield Android TV", "Cooper",  "Oppenheimer",           "",         "",           "movie",   1, "Shield Android TV", "192.168.1.102", "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=200&q=80", "1014"),
            // Week 5
            TautulliHistoryItem(15, b-5*w,    2800, "LG WebOS TV",       "Jesse",   "In Perpetuity",         "Season 1", "Severance",  "episode", 1, "LG TV Client",      "172.56.21.90",  "https://images.unsplash.com/photo-1497366216548-37526070297c?w=200&q=80", "1015"),
            TautulliHistoryItem(16, b-5*w-3*d,4500, "iPhone 15",         "Mike",    "Everything Everywhere", "",         "",           "movie",   1, "Plex for iOS",      "99.102.34.12",  "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200&q=80", "1016"),
            // Week 6
            TautulliHistoryItem(17, b-6*w-d,  8500, "Chrome (MacOS)",    "Steve",   "Dune",                  "",         "",           "movie",   1, "Plex Web (Chrome)", "192.168.1.144", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200&q=80", "1017"),
            TautulliHistoryItem(18, b-6*w-4*d,2700, "iPad Air",          "Sarah",   "Say My Name",           "Season 5", "Breaking Bad","episode", 1, "Plex for iPad",     "10.0.0.45",     "https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=200&q=80", "1018"),
            // Week 7
            TautulliHistoryItem(19, b-7*w-d,  9900, "LG WebOS TV",       "Cooper",  "Interstellar",          "",         "",           "movie",   1, "LG TV Client",      "172.56.21.90",  "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&q=80", "1019"),
            TautulliHistoryItem(20, b-7*w-3*d,2400, "Chrome (MacOS)",    "Mike",    "Fishes",                "Season 2", "The Bear",   "episode", 1, "Plex Web (Chrome)", "99.102.34.12",  "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=200&q=80", "1020")
        )
    }
    return realHistory
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsHeaderRow — mirrors web StatsBar.jsx
// Shows: Films/Shows Added This Year | Watches This Year | Hours Watched | Users
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatsHeaderRow(
    history: List<TautulliHistoryItem>,
    libraryStats: PlexLibraryStats?,
    category: String
) {
    val startOfYear = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.MONTH, 0)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000L
    }

    val filtered = if (category == "movies") {
        history.filter { it.type == "movie" }
    } else {
        history.filter { it.type == "episode" }
    }

    val yearFiltered    = filtered.filter { (it.date ?: 0L) >= startOfYear }
    val watchesThisYear = yearFiltered.size
    val hoursWatched    = yearFiltered.sumOf { it.duration ?: 0L } / 3600L
    val uniqueUsers     = history.filter { (it.date ?: 0L) >= startOfYear }
                              .mapNotNull { it.user }.distinct().size
    val addedCount      = if (category == "movies") libraryStats?.moviesAddedThisYear ?: 0
                          else libraryStats?.showsAddedThisYear ?: 0
    val addedLabel      = if (category == "movies") "Films Added" else "Shows Added"

    val chips = listOf(
        Pair("$addedCount", addedLabel),
        Pair("$watchesThisYear", "Watches '${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100}"),
        Pair("${hoursWatched}h", "Watched"),
        Pair("$uniqueUsers", "Active Users")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.forEach { (value, label) ->
            Column(
                modifier = Modifier
                    .background(Color(0xFF1A1D21), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    color = NeonGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = label,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WatchChartCard — mirrors web WatchChart.jsx (52-week bar chart + day-of-week)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WatchChartCard(
    weeklyBins: List<Int>,
    dayOfWeekBins: List<Pair<String, Int>>,
    totalPlays: Int,
    category: String
) {
    val currentWeekIdx = remember {
        (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - 1).coerceIn(0, 51)
    }
    val maxVal = weeklyBins.maxOrNull()?.coerceAtLeast(1) ?: 1
    val totalFromBins = weeklyBins.sum().coerceAtLeast(1)
    val nonZeroWeeks  = weeklyBins.count { it > 0 }.coerceAtLeast(1)
    val avgPerWeek    = totalFromBins.toFloat() / nonZeroWeeks
    val avgPerMonth   = (avgPerWeek * 4.33f).toInt()

    ToplexCard(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BY WEEK",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (category == "movies") "MOVIES" else "TV SHOWS",
                color = NeonGreen.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 52-week bar chart — horizontally scrollable
        val scrollState = rememberScrollState(initial = Int.MAX_VALUE)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .horizontalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.height(72.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyBins.forEachIndexed { idx, count ->
                    val fraction = if (maxVal > 0) count.toFloat() / maxVal else 0f
                    val isFuture  = idx > currentWeekIdx
                    val isCurrent = idx == currentWeekIdx
                    val barColor = when {
                        isFuture  -> Color(0xFF2A2D33)
                        isCurrent -> NeonGreen
                        fraction > 0f -> NeonGreen.copy(alpha = 0.5f + fraction * 0.4f)
                        else          -> Color(0xFF2A2D33)
                    }
                    Box(
                        modifier = Modifier
                            .width(7.dp)
                            .fillMaxHeight(fraction.coerceAtLeast(if (isFuture) 0f else 0.04f))
                            .background(barColor, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats row: total → avg/month → avg/week
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$totalPlays plays", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("  →  ", color = TextMuted, fontSize = 12.sp)
            Text("$avgPerMonth/month", color = TextMuted, fontSize = 13.sp)
            Text("  →  ", color = TextMuted, fontSize = 12.sp)
            Text("${avgPerWeek.toInt()}/week", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Day-of-week mini bars
        if (dayOfWeekBins.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val maxDow = dayOfWeekBins.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayOfWeekBins.forEach { (label, count) ->
                    val frac = count.toFloat() / maxDow
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(frac.coerceAtLeast(0.06f))
                                    .background(
                                        if (frac > 0f) NeonGreen.copy(alpha = 0.6f + frac * 0.4f)
                                        else Color(0xFF2A2D33),
                                        RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthActivityFeedCard — mirrors web ActivityFeed.jsx (month-grouped history)
// ─────────────────────────────────────────────────────────────────────────────
private data class ActivityEntry(
    val day: String,
    val title: String,
    val showTitle: String,
    val user: String,
    val type: String,
    val thumb: String
)

private data class MonthGroup(val monthLabel: String, val items: List<ActivityEntry>)

private fun groupHistoryByMonth(history: List<TautulliHistoryItem>, limit: Int = 20): List<MonthGroup> {
    val sorted = history.sortedByDescending { it.date ?: 0L }.take(limit)
    val cal = Calendar.getInstance()
    val monthNames = arrayOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    return sorted
        .groupBy { item ->
            val ts = (item.date ?: 0L) * 1000L
            cal.timeInMillis = ts
            "${monthNames[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
        }
        .map { (monthLabel, items) ->
            MonthGroup(
                monthLabel = monthLabel,
                items = items.map { item ->
                    cal.timeInMillis = (item.date ?: 0L) * 1000L
                    val day = cal.get(Calendar.DAY_OF_MONTH).toString()
                    val title = if (item.type == "episode" && !item.grandparentTitle.isNullOrEmpty()) {
                        item.title ?: "Unknown"
                    } else {
                        item.title ?: "Unknown"
                    }
                    val showTitle = if (item.type == "episode" && !item.grandparentTitle.isNullOrEmpty()) {
                        item.grandparentTitle ?: ""
                    } else ""
                    ActivityEntry(
                        day = day,
                        title = title,
                        showTitle = showTitle,
                        user = item.user ?: "Plex User",
                        type = item.type ?: "movie",
                        thumb = item.thumb ?: ""
                    )
                }
            )
        }
}

@Composable
fun MonthActivityFeedCard(
    history: List<TautulliHistoryItem>,
    category: String
) {
    val filtered = if (category == "movies") {
        history.filter { it.type == "movie" }
    } else {
        history.filter { it.type == "episode" }
    }
    val groups = remember(filtered) { groupHistoryByMonth(filtered) }
    if (groups.isEmpty()) return

    ToplexCard(modifier = Modifier.fillMaxWidth()) {
        // Header with live badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVITY",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "⚡ LIVE",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        groups.forEach { group ->
            // Month label block (CalendarBlock style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(NeonGreen, RoundedCornerShape(2.dp))
                )
                Text(
                    text = group.monthLabel,
                    color = NeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            // Activity items for this month
            group.items.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .padding(start = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Day number
                    Text(
                        text = entry.day,
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    // Thumb (small)
                    if (entry.thumb.isNotEmpty()) {
                        AsyncImage(
                            model = entry.thumb,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 34.dp, height = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Title + show + user
                    Column(modifier = Modifier.weight(1f)) {
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
                                color = NeonGreen.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "watched by ${entry.user}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

