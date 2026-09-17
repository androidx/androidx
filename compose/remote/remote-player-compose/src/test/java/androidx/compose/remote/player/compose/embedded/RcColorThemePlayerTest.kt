/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import android.R
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RcColorThemePlayerTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun testResolveThemeMode() {
        assertThat(resolveThemeMode(Theme.LIGHT, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.LIGHT, isSystemInDarkTheme = true)).isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.DARK, isSystemInDarkTheme = false)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.DARK, isSystemInDarkTheme = true)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.SYSTEM, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.SYSTEM, isSystemInDarkTheme = true)).isEqualTo(Theme.DARK)
        assertThat(resolveThemeMode(Theme.UNSPECIFIED, isSystemInDarkTheme = false))
            .isEqualTo(Theme.LIGHT)
        assertThat(resolveThemeMode(Theme.UNSPECIFIED, isSystemInDarkTheme = true))
            .isEqualTo(Theme.DARK)
    }

    @Test
    fun testAndroidColorThemeMappingResolvesFrameworkColors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex (darker_gray: R.color.darker_gray)
                    2.toShort(), // darkModeIndex (black: R.color.black)
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = "android" }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        val expectedBlack = context.getColor(R.color.black)
        val expectedDarkerGray = context.getColor(R.color.darker_gray)

        assertThat(colorTheme.mDarkMode).isEqualTo(expectedBlack)
        assertThat(colorTheme.mLightMode).isEqualTo(expectedDarkerGray)
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNonAndroidColorGroup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex
                    2.toShort(), // darkModeIndex
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = "custom_group" }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        // Authored fallbacks must remain untouched
        assertThat(colorTheme.mLightMode).isEqualTo(0xFFEEEEEE.toInt())
        assertThat(colorTheme.mDarkMode).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNullColorGroup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colorTheme =
            ColorTheme(
                    100, // id
                    -1, // colorGroupId
                    3.toShort(), // lightModeIndex
                    2.toShort(), // darkModeIndex
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
                .apply { mColorGroupName = null }

        AndroidColorThemeResolver.mapColors(context, listOf(colorTheme))

        // Authored fallbacks must remain untouched
        assertThat(colorTheme.mLightMode).isEqualTo(0xFFEEEEEE.toInt())
        assertThat(colorTheme.mDarkMode).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testColorThemeAppliesAccordingToPaintTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = CoreDocument(RemoteClock.SYSTEM)
        val remoteContext =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setAndroidContext(context) }

        val colorTheme =
            ColorTheme(
                100,
                -1,
                (-1).toShort(),
                (-1).toShort(),
                0xFFFFFFFF.toInt(), // light color
                0xFF000000.toInt(), // dark color
            )
        document.getOperationsReflection().add(colorTheme)

        // Light mode
        remoteContext.paintTheme = Theme.LIGHT
        colorTheme.apply(remoteContext)
        assertThat(remoteContext.getColor(100)).isEqualTo(0xFFFFFFFF.toInt())

        // Dark mode
        remoteContext.paintTheme = Theme.DARK
        colorTheme.apply(remoteContext)
        assertThat(remoteContext.getColor(100)).isEqualTo(0xFF000000.toInt())
    }

    @Test
    fun testSnapshotRemoteComposeStateUpdateColor() {
        val state = SnapshotRemoteComposeState()
        val colorId = 100
        state.updateColor(colorId, 0xFFFF0000.toInt())
        assertThat(state.getColor(colorId)).isEqualTo(0xFFFF0000.toInt())

        state.updateColor(colorId, 0xFF00FF00.toInt())
        assertThat(state.getColor(colorId)).isEqualTo(0xFF00FF00.toInt())
    }

    @Test
    fun testCreateThemedRemoteColorInEmbeddedPlayerLightAndDarkTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lightFallback = Color(0xFF123456)
        val darkFallback = Color(0xFF654321)

        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val color =
                    RemoteColor.createThemedRemoteColor(
                        lightMode = 0,
                        darkMode = 0,
                        lightFallback = lightFallback,
                        darkFallback = darkFallback,
                    )
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color))
            }
        }

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        val lightBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(lightFallback.toArgb())

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(darkFallback.toArgb())
    }

    @Test
    fun testCreateThemedRemoteColorResolvesAndroidSystemColorsInEmbeddedPlayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lightFallback = Color.Red
        val darkFallback = Color.Blue

        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val color =
                    RemoteColor.createThemedRemoteColor(
                        lightMode = R.color.background_light,
                        darkMode = R.color.background_dark,
                        lightFallback = lightFallback,
                        darkFallback = darkFallback,
                    )
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color))
            }
        }

        val expectedLight = context.getColor(R.color.background_light)
        val expectedDark = context.getColor(R.color.background_dark)

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        val lightBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(expectedLight)

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(expectedDark)
    }

    @Test
    fun testCreateThemedRemoteColorDeduplicatesVariableId() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val color1 =
                    RemoteColor.createThemedRemoteColor(
                        lightMode = R.color.background_light,
                        darkMode = R.color.background_dark,
                        lightFallback = Color.White,
                        darkFallback = Color.Black,
                    )
                val color2 =
                    RemoteColor.createThemedRemoteColor(
                        lightMode = R.color.background_light,
                        darkMode = R.color.background_dark,
                        lightFallback = Color.White,
                        darkFallback = Color.Black,
                    )
                val color3 =
                    RemoteColor.createThemedRemoteColor(
                        lightMode = R.color.darker_gray,
                        darkMode = R.color.black,
                        lightFallback = Color.Gray,
                        darkFallback = Color.Black,
                    )
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color1))
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color2))
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color3))
            }
        }

        val themedColors = document.themedColors
        assertThat(themedColors).hasSize(2)
    }
}
