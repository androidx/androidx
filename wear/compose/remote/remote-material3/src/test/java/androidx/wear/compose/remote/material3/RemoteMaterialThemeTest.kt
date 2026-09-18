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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import androidx.wear.compose.remote.material3.util.TestImageVectors
import java.io.ByteArrayInputStream
import kotlin.test.Ignore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RemoteMaterialThemeTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun sets_theme_color() {
        val expectedTint = ColorScheme().onSurface

        runTest {
            RemoteMaterialTheme {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun named_color_can_be_overridden() {
        val expectedTint = Color.Yellow

        runTest(colorOverrides = mapOf("WearM3.onSurface" to Color.Yellow)) {
            RemoteMaterialTheme {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    @Test
    @Ignore("Fails because of b/502878815")
    fun button_named_color_can_be_overridden() {
        val expectedTint = Color.Yellow

        runTest(colorOverrides = mapOf("WearM3.primary" to Color.Yellow)) {
            RemoteMaterialTheme {
                RemoteButton(onClick = Action.Empty) { RemoteText("button_enabled".rs) }
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun theme_color_can_be_overridden_explicitly() {
        val expectedTint = Color.Yellow
        val remoteColorScheme =
            RemoteColorScheme(colorScheme = ColorScheme(onSurface = Color.Yellow))

        runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                RemoteIcon(
                    TestImageVectors.VolumeUp,
                    contentDescription = null,
                    tint = RemoteMaterialTheme.colorScheme.onSurface,
                )
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun color_scheme_can_be_copied_and_overridden() {
        val expectedTint = Color.Yellow
        val remoteColorScheme = RemoteColorScheme().copy(onSurface = Color.Yellow.rc)

        runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                RemoteIcon(
                    TestImageVectors.VolumeUp,
                    contentDescription = null,
                    tint = RemoteMaterialTheme.colorScheme.onSurface,
                )
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    @Test
    fun custom_color_scheme_propagates_colors() {
        val customColorScheme = ColorScheme(primary = Color.Magenta, onSurface = Color.Cyan)
        val remoteColorScheme = RemoteColorScheme(customColorScheme)
        val expectedTint = Color.Cyan

        runTest {
            RemoteMaterialTheme(colorScheme = remoteColorScheme) {
                val iconTint = RemoteMaterialTheme.colorScheme.onSurface
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null, tint = iconTint)
            }
        }

        assertRootNodeContainsColor(expectedTint)
    }

    private fun runTest(
        colorOverrides: Map<String, Color> = emptyMap(),
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            val bytes = captureSingleRemoteDocument(context = context, content = composable).bytes
            CoreDocument().apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        }
        val playerState = RcPlayerState(document)
        colorOverrides.forEach { (name, color) ->
            playerState.colorState(name).value = color
        }
        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag(ROOT_TEST_TAG)) {
                RcPlayer(state = playerState)
            }
        }
        rule.waitForIdle()
    }

    private fun assertRootNodeContainsColor(expectedColor: Color) {
        rule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    companion object {
        private const val ROOT_TEST_TAG = "ROOT_TEST_TAG"
    }
}
