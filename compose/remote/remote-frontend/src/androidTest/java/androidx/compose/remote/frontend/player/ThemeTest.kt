/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.frontend.player

import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.player.view.RemoteComposeDocument
import androidx.compose.remote.test.screenshot.TargetPlayer
import androidx.compose.remote.test.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class ThemeTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun nightUnspecifiedDarkMode_darkThemeProvided_showsDarkTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument()
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightUnspecifiedDarkMode_darkThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument(darkTheme = false)
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightUnspecifiedDarkMode_darkAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument(darkTheme = false, unspecifiedTheme = false)
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightUnspecifiedNotDarkMode_lightThemeProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument()
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightUnspecifiedNotDarkMode_lightThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument(lightTheme = false)
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightUnspecifiedNotDarkMode_lightAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_UNSPECIFIED)
        val document = getDocument(lightTheme = false, unspecifiedTheme = false)
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemDarkMode_darkThemeProvided_showsDarkTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument()
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemDarkMode_darkThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument(darkTheme = false)
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemDarkMode_darkAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument(darkTheme = false, unspecifiedTheme = false)
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemNotDarkMode_lightThemeProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument()
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemNotDarkMode_lightThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument(lightTheme = false)
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightFollowSystemNotDarkMode_lightAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        val document = getDocument(lightTheme = false, unspecifiedTheme = false)
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightYes_darkThemeProvided_showsDarkTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        val document = getDocument()

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightYes_darkThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        val document = getDocument(darkTheme = false)

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightYes_darkAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        val document = getDocument(darkTheme = false, unspecifiedTheme = false)

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightYesNotInDarkMode_darkAndLightThemesProvided_showsDarkTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        val document = getDocument()
        val isDarkMode = false

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightNo_lightThemeProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        val document = getDocument()

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightNo_lightThemeNotProvided_showsUnspecifiedTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        val document = getDocument(lightTheme = false)

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightNo_lightAndUnspecifiedThemesNotProvided_showsBlank() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        val document = getDocument(lightTheme = false, unspecifiedTheme = false)

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightNoInDarkMode_darkAndLightThemesProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        val document = getDocument()
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    @Test
    fun nightAutoBattery_darkAndLightThemesProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_AUTO_BATTERY)
        val document = getDocument()

        remoteComposeTestRule.runScreenshotTest(document = document)
    }

    @Test
    fun nightAutoBatteryDarkMode_darkAndLightThemesProvided_showsLightTheme() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_AUTO_BATTERY)
        val document = getDocument()
        val isDarkMode = true

        remoteComposeTestRule.runScreenshotTest(
            document = document,
            outerContent = darkModeConfigurationOverride(isDarkMode),
        )
    }

    private fun darkModeConfigurationOverride(
        isDarkMode: Boolean
    ): @Composable (content: @Composable @RemoteComposable () -> Unit) -> Unit =
        @Composable { content ->
            DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(isDarkMode)) {
                content()
            }
        }

    private fun getDocument(
        darkTheme: Boolean = true,
        lightTheme: Boolean = true,
        unspecifiedTheme: Boolean = true,
    ): CoreDocument = getCoreDocument {
        painter.setTextSize(100f).commit()
        if (unspecifiedTheme) {
            setTheme(Theme.UNSPECIFIED)
            painter.setColor(Color.MAGENTA).commit()
            drawTextAnchored("Unspecified theme", 0f, 100f, -1f, 0f, 0)
        }
        if (darkTheme) {
            setTheme(Theme.DARK)
            painter.setColor(Color.CYAN).commit()
            drawTextAnchored("Dark theme", 0f, 200f, -1f, 0f, 0)
        }
        if (lightTheme) {
            setTheme(Theme.LIGHT)
            painter.setColor(Color.GREEN).commit()
            drawTextAnchored("Light theme", 0f, 300f, -1f, 0f, 0)
        }
    }

    private fun getCoreDocument(content: RemoteComposeContextAndroid.() -> Unit): CoreDocument {
        val rcContext =
            RemoteComposeContextAndroid(
                AndroidxPlatformServices(),
                RemoteComposeWriter.HTag(Header.DOC_CONTENT_DESCRIPTION, "Test"),
                RemoteComposeWriter.HTag(Header.DOC_DESIRED_FPS, 120),
            ) {
                apply(content)
            }
        return RemoteComposeDocument(
                ByteArrayInputStream(rcContext.mRemoteWriter.buffer(), 0, rcContext.bufferSize())
            )
            .document
    }
}
