package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TautulliViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ServerSetupScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TautulliViewModel(app)

    composeTestRule.setContent { 
      MyApplicationTheme { 
        ServerSetupScreen(viewModel = viewModel, onNavigateToDashboard = {}) 
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/setup.png")
  }

  @Test
  fun test_render_dashboard() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TautulliViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel, onNavigateToSettings = {})
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun test_render_stats() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TautulliViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        StatsScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun test_render_history() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TautulliViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        HistoryScreen(viewModel = viewModel, onNavigateToSettings = {})
      }
    }
    composeTestRule.waitForIdle()
  }
}
