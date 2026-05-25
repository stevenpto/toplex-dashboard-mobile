package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TautulliViewModel
import com.example.ui.TestConnectionState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen(
    viewModel: TautulliViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val useDemoMode by viewModel.useDemoMode.collectAsState()
    val testState by viewModel.testState.collectAsState()

    var tempUrl by remember { mutableStateOf("") }
    var tempKey by remember { mutableStateOf("") }

    LaunchedEffect(serverUrl, apiKey) {
        if (tempUrl.isEmpty()) tempUrl = serverUrl
        if (tempKey.isEmpty()) tempKey = apiKey
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = PlexOrange
                        )
                        Text(
                            text = "Connection Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Connect to Tautulli Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tautulli is a Python-based web app for monitoring Plex Media Server. This companion app connects to your self-hosted Tautulli server to fetch dashboards, streaming speeds, history, and active session streams.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (useDemoMode) DarkSurfaceVariant else DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.toggleDemoMode(!useDemoMode)
                    }
                    .border(
                        width = 1.5.dp,
                        color = if (useDemoMode) PlexOrange else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (useDemoMode) PlexOrange.copy(alpha = 0.15f) else DarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (useDemoMode) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.StopScreenShare,
                            contentDescription = "Demo Mode",
                            tint = if (useDemoMode) PlexOrange else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Interactive Demo Mode",
                            fontWeight = FontWeight.Bold,
                            color = if (useDemoMode) PlexOrange else Color.White
                        )
                        Text(
                            text = if (useDemoMode) "Showing gorgeous simulated analytics" else "Disabled — use connection details below",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = useDemoMode,
                        onCheckedChange = { active ->
                            viewModel.toggleDemoMode(active)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlexOrange,
                            checkedTrackColor = PlexOrange.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBg
                        )
                    )
                }
            }

            if (!useDemoMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Server Details",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("Server URL Address") },
                            placeholder = { Text("http://192.168.1.100:8181") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "URL"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlexOrange,
                                focusedLabelColor = PlexOrange,
                                cursorColor = PlexOrange
                            )
                        )

                        OutlinedTextField(
                            value = tempKey,
                            onValueChange = { tempKey = it },
                            label = { Text("Tautulli API Key") },
                            placeholder = { Text("32-character hexadecimal key") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "API Key"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlexOrange,
                                focusedLabelColor = PlexOrange,
                                cursorColor = PlexOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        AnimatedVisibility(
                            visible = testState != TestConnectionState.Idle,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            when (val state = testState) {
                                TestConnectionState.Testing -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = PlexOrange,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Testing server connection...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                is TestConnectionState.Success -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E3A1E))
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Connected",
                                            tint = Color.Green
                                        )
                                        Text(
                                            text = "Success! Verified Server: ${state.serverName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Green,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                is TestConnectionState.Error -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF3F1B1B))
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = Color.Red
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.testConnection(tempUrl, tempKey)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PlexOrange
                                ),
                                border = BorderStroke(1.dp, PlexOrange)
                            ) {
                                Text("Test Connection")
                            }

                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.saveCredentials(tempUrl, tempKey)
                                    onNavigateToDashboard()
                                },
                                enabled = tempUrl.isNotBlank() && tempKey.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlexOrange,
                                    contentColor = Color(0xFF111215)
                                )
                            ) {
                                Text("Apply & Connect")
                            }
                        }
                    }
                }
            }

            if (useDemoMode) {
                Button(
                    onClick = { onNavigateToDashboard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlexOrange,
                        contentColor = Color(0xFF111215)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Explore Demo Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Explore"
                        )
                    }
                }
            }
        }
    }
}
