package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.PeriodBarChart
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChartEntry
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
    val sampleEntries = listOf(
      ChartEntry("Jan", credit = 85000.0, debit = 42000.0),
      ChartEntry("Feb", credit = 85000.0, debit = 38000.0),
      ChartEntry("Mar", credit = 90000.0, debit = 51000.0)
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        PeriodBarChart(entries = sampleEntries)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
