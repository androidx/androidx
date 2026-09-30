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
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.runBlocking
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
class RemoteOneHandedGestureClickIndicatorTest {

    @get:Rule(order = 0) val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()
    @get:Rule(order = 1) val composeTestRule = createComposeRule()
    @get:Rule(order = 2) val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val creationDisplayInfo =
        createCreationDisplayInfo(context, Size(DISPLAY_SIZE_PX, DISPLAY_SIZE_PX))
    private val testAction = hostAction("testAction".rs)

    @Test
    fun container_atRest_compilesDocument() = runTest {
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = RcPlatformProfiles.ANDROIDX,
            ) {
                ComponentContainer {
                    RemoteButton(onClick = testAction) {
                        RemoteOneHandedGestureClickIndicator(hintProgress = 0f.rf) {
                            RemoteText("Click Me".rs)
                        }
                    }
                }
            }

        val hierarchy = document.displayHierarchy()

        assertThat(hierarchy).contains("Click Me")
    }

    @Test
    fun container_atRest_showsContentAndHidesIndicator() {
        val bitmap = renderIndicatorAtProgress(progress = 0f)

        assertThat(bitmap.hasBlueContent()).isTrue()
        assertThat(bitmap.hasRedIndicator()).isFalse()
    }

    @Test
    fun container_midSequence_hidesContentAndRendersIndicator() {
        val bitmap = renderIndicatorAtProgress(progress = 0.5f)

        assertThat(bitmap.hasBlueContent()).isFalse()
        assertThat(bitmap.hasRedIndicator()).isTrue()
    }

    @Test
    fun container_sequenceEnd_restoresContentAndHidesIndicator() {
        val bitmap = renderIndicatorAtProgress(progress = 1f)

        assertThat(bitmap.hasBlueContent()).isTrue()
        assertThat(bitmap.hasRedIndicator()).isFalse()
    }

    private fun renderIndicatorAtProgress(progress: Float): Bitmap = renderToBitmap {
        RemoteButton(
            onClick = testAction,
            colors = RemoteButtonDefaults.buttonColors(containerColor = Color.Black.rc),
        ) {
            RemoteOneHandedGestureClickIndicator(
                hintProgress = progress.rf,
                gestureIndicatorSize = RemoteOneHandedGestureDefaults.SmallIndicatorSize,
                gestureIndicatorTint = Color.Red.rc,
            ) {
                RemoteText("Click Me".rs, color = Color.Blue.rc)
            }
        }
    }

    private fun renderToBitmap(content: @Composable @RemoteComposable () -> Unit): Bitmap {
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = RcPlatformProfiles.ANDROIDX,
            ) {
                ComponentContainer(content = content)
            }
        }
        composeTestRule.setContent {
            Box(modifier = Modifier.size(200.dp).testTag(PLAYER_TAG)) {
                RcPlayer(document = document)
            }
        }
        composeTestRule.waitForIdle()
        return composeTestRule.onNodeWithTag(PLAYER_TAG).captureToImage().asAndroidBitmap()
    }

    private fun Bitmap.hasBlueContent(): Boolean = hasColorMatching {
        it.blue > 0.8f && it.red < 0.2f && it.green < 0.2f
    }

    private fun Bitmap.hasRedIndicator(): Boolean = hasColorMatching {
        it.red > 0.8f && it.green < 0.2f && it.blue < 0.2f
    }

    private fun Bitmap.hasColorMatching(predicate: (Color) -> Boolean): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (predicate(Color(getPixel(x, y)))) {
                    return true
                }
            }
        }
        return false
    }

    private companion object {
        const val DISPLAY_SIZE_PX = 200f
        const val PLAYER_TAG = "player"
    }
}
