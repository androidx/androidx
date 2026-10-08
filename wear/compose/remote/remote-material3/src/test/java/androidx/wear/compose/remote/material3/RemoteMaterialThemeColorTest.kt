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

package androidx.wear.compose.remote.material3

import android.R
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteMaterialThemeColorTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    }

    @After
    fun tearDown() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = false
    }

    @Test
    fun defaultColorScheme_usesNamedColors() {
        val document = capturePrimaryBox(colorScheme = null)

        assertThat(document.themedColors ?: emptyList<ColorTheme>()).isEmpty()
    }

    @Test
    fun dynamicColorScheme_usesThemedColors() {
        val document = capturePrimaryBox(colorScheme = { dynamicRemoteColorScheme() })

        assertThat(document.themedColors).isNotEmpty()
    }

    @Test
    fun dynamicColorScheme_wearWidgetsProfile_fallsBackToNamedColors() {
        val document =
            capturePrimaryBox(
                colorScheme = { dynamicRemoteColorScheme() },
                profile = RcPlatformProfiles.WEAR_WIDGETS,
            )

        assertThat(document.themedColors ?: emptyList<ColorTheme>()).isEmpty()
    }

    @Test
    fun dynamicColorScheme_inEmbeddedPlayer_alwaysUsesDarkSystemColors() {
        val document = capturePrimaryBox(colorScheme = { dynamicRemoteColorScheme() })

        val expectedPrimary = context.getColor(R.color.system_primary_dark)

        var currentTheme by mutableIntStateOf(Theme.DARK)
        rule.setContent {
            Box(modifier = Modifier.size(50.dp).testTag("player")) {
                RcPlayer(document = document, theme = currentTheme)
            }
        }
        rule.waitForIdle()

        val darkBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(darkBitmap.getPixel(25, 25)).isEqualTo(expectedPrimary)

        // Wear is always dark, so a light player theme still resolves the dark system token.
        currentTheme = Theme.LIGHT
        rule.waitForIdle()

        val lightBitmap: Bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
        assertThat(lightBitmap.getPixel(25, 25)).isEqualTo(expectedPrimary)
    }

    private fun capturePrimaryBox(
        colorScheme: (@Composable () -> RemoteColorScheme)?,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
    ): CoreDocument = runBlocking {
        captureRule.captureDocument(context = context, profile = profile) {
            val content =
                @Composable @RemoteComposable {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(50.rdp)
                                .background(RemoteMaterialTheme.colorScheme.primary)
                    )
                }
            if (colorScheme != null) {
                RemoteMaterialTheme(colorScheme = colorScheme(), content = content)
            } else {
                RemoteMaterialTheme(content = content)
            }
        }
    }
}
