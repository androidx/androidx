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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RemoteButtonTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun button_enabled_and_has_action_click_modifier_is_added(): Unit = runTest {
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
            ) {
                RemoteButton(
                    modifier = RemoteModifier.buttonSizeModifier(),
                    onClick = testAction,
                    enabled = true.rb,
                ) {
                    RemoteText("button_enabled".rs)
                }
            }
        val actualContent = document.displayHierarchy()

        assertThat(actualContent.normalizeWhiteSpace()).contains("CLICK_MODIFIER")
    }

    @Test
    fun button_disabled_click_modifier_is_not_added(): Unit = runTest {
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
            ) {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    enabled = false.rb,
                ) {
                    RemoteText("button_disabled".rs)
                }
            }
        val actualContent = document.displayHierarchy()

        assertThat(actualContent.normalizeWhiteSpace()).doesNotContain("CLICK_MODIFIER")
    }

    @Test
    fun button_border_width_is_scaled_with_density() = runTest {
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        val density = displayInfo.density.density
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = displayInfo,
                profile = RcPlatformProfiles.WEAR_WIDGETS,
            ) {
                ComponentContainer {
                    RemoteButton(
                        modifier = RemoteModifier.size(100.rdp, 50.rdp),
                        onClick = testAction,
                        border = 8.rdp,
                        borderColor = RemoteColor(Color.Red),
                        colors =
                            RemoteButtonDefaults.buttonColors(
                                containerColor = RemoteColor(Color.Black)
                            ),
                        shape = RemoteRectangleShape,
                    ) {
                        RemoteText("button".rs)
                    }
                }
            }

        composeTestRule.setContent {
            val widthDp = (500f / density).dp
            val heightDp = (500f / density).dp
            Box(modifier = Modifier.size(widthDp, heightDp).testTag(ROOT_TEST_TAG)) {
                RcPlayer(document = document)
            }
        }
        composeTestRule.waitForIdle()

        val bitmap = composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().asAndroidBitmap()

        val y = bitmap.height / 2
        var redPixelsCount = 0
        var firstRedX = -1
        var lastRedX = -1
        for (x in 0 until bitmap.width / 2) {
            val color = Color(bitmap.getPixel(x, y))
            if (color.red > 0.8f && color.green < 0.2f && color.blue < 0.2f) {
                redPixelsCount++
                if (firstRedX == -1) firstRedX = x
                lastRedX = x
            }
        }

        val expectedBorderWidthPx = (8 * density).toInt()
        assertWithMessage(
                "Expected border width of $expectedBorderWidthPx px (border=8.rdp * density=$density), " +
                    "found $redPixelsCount red pixels at y=$y in bitmap size ${bitmap.width}x${bitmap.height} " +
                    "(firstRedX=$firstRedX, lastRedX=$lastRedX)"
            )
            .that(abs(redPixelsCount - expectedBorderWidthPx))
            .isAtMost(1)
    }

    private fun String.normalizeWhiteSpace() = this.replace(Regex("\\s+"), " ").trim()

    private val testAction = hostAction("testAction".rs, 1.rf)

    private companion object {
        const val ROOT_TEST_TAG = "ROOT_TEST_TAG"
    }
}
