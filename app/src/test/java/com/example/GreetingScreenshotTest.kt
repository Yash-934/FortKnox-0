package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.preferences.VaultPreferences
import com.example.ui.screens.LockScreen
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        LockScreen(
          config = VaultPreferences.VaultConfig(
            isInitialized = false,
            masterSalt = null,
            wrappedDek = null,
            wrappedDekIv = null,
            biometricWrappedDek = null,
            biometricWrappedDekIv = null,
            isBiometricEnabled = false,
            autoLockTimeoutSec = 60,
            selfDestructEnabled = true,
            failedAttempts = 0,
            lockoutUntil = 0L
          ),
          onSetupMasterPassword = {},
          onUnlockWithPassword = {},
          onBiometricClick = {},
          errorMessage = null,
          isLoading = false
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
