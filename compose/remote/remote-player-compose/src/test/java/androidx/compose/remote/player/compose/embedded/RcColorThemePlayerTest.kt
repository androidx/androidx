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
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcColorThemePlayerTest {

    @get:Rule val rule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun testResolveThemeMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lightColor = Color(0xFFAA1122)
        val darkColor = Color(0xFF11AA22)
        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val color =
                    RemoteColor.createThemedRemoteColor(
                        lightColorRes = 0,
                        darkColorRes = 0,
                        lightFallback = lightColor,
                        darkFallback = darkColor,
                    )
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(color))
            }
        }

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        var systemNightMode by mutableIntStateOf(Configuration.UI_MODE_NIGHT_NO)

        rule.setContent {
            val baseConfig = LocalConfiguration.current
            val overriddenConfig =
                Configuration(baseConfig).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or systemNightMode
                }
            CompositionLocalProvider(LocalConfiguration provides overriddenConfig) {
                Box(modifier = Modifier.size(50.dp).testTag("player")) {
                    RcPlayer(document = document, theme = currentTheme)
                }
            }
        }

        val cases =
            listOf(
                Triple(Theme.LIGHT, Configuration.UI_MODE_NIGHT_NO, lightColor.toArgb()),
                Triple(Theme.LIGHT, Configuration.UI_MODE_NIGHT_YES, lightColor.toArgb()),
                Triple(Theme.DARK, Configuration.UI_MODE_NIGHT_NO, darkColor.toArgb()),
                Triple(Theme.DARK, Configuration.UI_MODE_NIGHT_YES, darkColor.toArgb()),
                Triple(Theme.SYSTEM, Configuration.UI_MODE_NIGHT_NO, lightColor.toArgb()),
                Triple(Theme.SYSTEM, Configuration.UI_MODE_NIGHT_YES, darkColor.toArgb()),
                Triple(Theme.UNSPECIFIED, Configuration.UI_MODE_NIGHT_NO, lightColor.toArgb()),
                Triple(Theme.UNSPECIFIED, Configuration.UI_MODE_NIGHT_YES, darkColor.toArgb()),
            )

        for ((themeMode, nightFlag, expectedArgb) in cases) {
            currentTheme = themeMode
            systemNightMode = nightFlag
            rule.waitForIdle()
            val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
            assertThat(bitmap.getPixel(25, 25)).isEqualTo(expectedArgb)
        }
    }

    @Test
    fun testAndroidColorThemeMappingResolvesFrameworkColors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = colorThemeRectDocument(colorGroupName = "android")

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        val expectedDarkerGray = context.getColor(R.color.darker_gray)
        val expectedBlack = context.getColor(R.color.black)

        val lightBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(expectedDarkerGray)

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(expectedBlack)
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNonAndroidColorGroup() {
        val document = colorThemeRectDocument(colorGroupName = "custom_group")

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        // Authored fallbacks must remain untouched
        val lightBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(0xFFEEEEEE.toInt())

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testAndroidColorThemeMappingSkipsNullColorGroup() {
        val document = colorThemeRectDocument(colorGroupName = null)

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        // Authored fallbacks must remain untouched
        val lightBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(0xFFEEEEEE.toInt())

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(0xFF111111.toInt())
    }

    @Test
    fun testColorThemeAppliesAccordingToPaintTheme() {
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    ColorTheme(
                        100,
                        -1,
                        (-1).toShort(),
                        (-1).toShort(),
                        0xFFFFFFFF.toInt(), // light color
                        0xFF000000.toInt(), // dark color
                    )
                )
                operations.add(PaintData(PaintBundle().apply { setColorId(100) }))
                operations.add(DrawRect(0f, 0f, 50f, 50f))
            }

        var currentTheme by mutableIntStateOf(Theme.LIGHT)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        val lightBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(0xFFFFFFFF.toInt())

        currentTheme = Theme.DARK
        rule.waitForIdle()

        val darkBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(0xFF000000.toInt())
    }

    @Test
    fun testPlayerStateColorUpdateRepaintsEmbeddedPlayer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            captureRule.captureDocument(context = context) {
                val accent = remember { createNamedRemoteColor("accent", Color.Red) }
                RemoteBox(modifier = RemoteModifier.size(50.rdp).background(accent))
            }
        }

        val playerState = RcPlayerState(document)
        var accent by playerState.colorState("accent")
        assertThat(accent).isEqualTo(Color.Red)

        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(state = playerState)
            }
        }
        rule.waitForIdle()

        val initialBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(initialBitmap.getPixel(25, 25)).isEqualTo(Color.Red.toArgb())

        accent = Color.Green
        rule.waitForIdle()

        assertThat(accent).isEqualTo(Color.Green)
        val updatedBitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(updatedBitmap.getPixel(25, 25)).isEqualTo(Color.Green.toArgb())
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
                        lightColorRes = 0,
                        darkColorRes = 0,
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
                        lightColorRes = R.color.background_light,
                        darkColorRes = R.color.background_dark,
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
                        lightColorRes = R.color.background_light,
                        darkColorRes = R.color.background_dark,
                        lightFallback = Color.White,
                        darkFallback = Color.Black,
                    )
                val color2 =
                    RemoteColor.createThemedRemoteColor(
                        lightColorRes = R.color.background_light,
                        darkColorRes = R.color.background_dark,
                        lightFallback = Color.White,
                        darkFallback = Color.Black,
                    )
                val color3 =
                    RemoteColor.createThemedRemoteColor(
                        lightColorRes = R.color.darker_gray,
                        darkColorRes = R.color.black,
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

    private fun colorThemeRectDocument(colorGroupName: String?): CoreDocument =
        CoreDocument(RemoteClock.SYSTEM).apply {
            val groupId =
                if (colorGroupName != null) {
                    operations.add(TextData(50, colorGroupName))
                    50
                } else {
                    -1
                }
            operations.add(
                ColorTheme(
                    100, // id
                    groupId, // colorGroupId
                    3.toShort(), // lightModeIndex (darker_gray: R.color.darker_gray)
                    2.toShort(), // darkModeIndex (black: R.color.black)
                    0xFFEEEEEE.toInt(), // fallbackLight
                    0xFF111111.toInt(), // fallbackDark
                )
            )
            operations.add(PaintData(PaintBundle().apply { setColorId(100) }))
            operations.add(DrawRect(0f, 0f, 50f, 50f))
        }
}
