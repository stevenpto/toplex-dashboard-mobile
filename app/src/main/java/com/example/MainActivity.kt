package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.TautulliViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ServerSetupScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TautulliAppNavigator()
      }
    }
  }
}

@Composable
fun TautulliAppNavigator() {
  val navController = rememberNavController()
  val viewModel: TautulliViewModel = viewModel()

  NavHost(navController = navController, startDestination = "setup") {
    composable("setup") {
      ServerSetupScreen(
        viewModel = viewModel,
        onNavigateToDashboard = {
          navController.navigate("main_tabs") {
            popUpTo("setup") { inclusive = false }
          }
        }
      )
    }

    composable("main_tabs") {
      MainTabsContainer(
        viewModel = viewModel,
        onNavigateToSettings = {
          navController.navigate("setup")
        }
      )
    }
  }
}

@Composable
fun MainTabsContainer(
  viewModel: TautulliViewModel,
  onNavigateToSettings: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 0) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
              contentDescription = "Dashboard"
            )
          },
          label = { Text("Dashboard") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )

        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 1) Icons.Filled.BarChart else Icons.Outlined.BarChart,
              contentDescription = "Analytics"
            )
          },
          label = { Text("Stats") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )

        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History,
              contentDescription = "History"
            )
          },
          label = { Text("History") },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
      }
    }
  ) { paddingValues ->
    val modifier = Modifier.padding(paddingValues)
    when (selectedTab) {
      0 -> DashboardScreen(
        viewModel = viewModel,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
      )
      1 -> StatsScreen(
        viewModel = viewModel,
        modifier = modifier
      )
      2 -> HistoryScreen(
        viewModel = viewModel,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
      )
    }
  }
}
