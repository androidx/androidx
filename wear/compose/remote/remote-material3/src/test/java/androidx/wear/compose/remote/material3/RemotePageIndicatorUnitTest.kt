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

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.player.compose.EnableEmbeddedPlayerRule
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRemotePlayerApi::class)
@Config(sdk = [35], qualifiers = "w1000dp-h500dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class RemotePageIndicatorUnitTest {

    @get:Rule(order = 0) val embeddedPlayerRule = EnableEmbeddedPlayerRule()
    @get:Rule(order = 1) val composeRule = createComposeRule()
    @get:Rule(order = 2) val captureRule = RemoteCaptureTestRule()

    private fun renderPlayerToBitmap(content: @Composable @RemoteComposable () -> Unit): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = displayInfo,
                content = content,
            )
        }
        composeRule.setContent {
            Box(modifier = Modifier.size(500.dp, 500.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        return composeRule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
    }

    private fun Bitmap.hasColorMatching(predicate: (Color) -> Boolean): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (predicate(Color(getPixel(x, y)))) return true
            }
        }
        return false
    }

    private fun Bitmap.countPixelsMatching(predicate: (Color) -> Boolean): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (predicate(Color(getPixel(x, y)))) count++
            }
        }
        return count
    }

    @Test
    fun horizontal_page_indicator_draws_background() {
        val bitmap = renderPlayerToBitmap {
            val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
            RemoteHorizontalPageIndicator(
                state = state,
                backgroundColor = Color.Red.rc,
            )
        }
        assertThat(bitmap.hasColorMatching { it.red > 0.8f && it.green < 0.2f && it.blue < 0.2f })
            .isTrue()
    }

    @Test
    fun vertical_page_indicator_draws_background() {
        val bitmap = renderPlayerToBitmap {
            val state = rememberRemotePageIndicatorState(selectedPage = 1.ri, pageCount = 3)
            RemoteVerticalPageIndicator(
                state = state,
                backgroundColor = Color.Blue.rc,
            )
        }
        assertThat(bitmap.hasColorMatching { it.blue > 0.8f && it.red < 0.2f && it.green < 0.2f })
            .isTrue()
    }

    @Test
    fun horizontal_page_indicator_pageCount1_draws_background() {
        val bitmap = renderPlayerToBitmap {
            val state = rememberRemotePageIndicatorState(selectedPage = 0.ri, pageCount = 1)
            RemoteHorizontalPageIndicator(
                state = state,
                backgroundColor = Color.Green.rc,
            )
        }
        assertThat(bitmap.hasColorMatching { it.green > 0.8f && it.red < 0.2f && it.blue < 0.2f })
            .isTrue()
    }

    @Test
    fun vertical_page_indicator_offset80_shrinks_previous_dot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        val offset80Doc = runBlocking {
            captureRule.captureDocument(context = context, creationDisplayInfo = displayInfo) {
                val state =
                    rememberRemotePageIndicatorState(
                        selectedPage = 2.ri,
                        pageOffset = 0.8f.rf,
                        pageCount = 5,
                    )
                RemoteVerticalPageIndicator(
                    state = state,
                    selectedColor = Color.Blue.rc,
                    unselectedColor = Color.Green.rc,
                    backgroundColor = Color.Black.rc,
                )
            }
        }
        val settledDoc = runBlocking {
            captureRule.captureDocument(context = context, creationDisplayInfo = displayInfo) {
                val state =
                    rememberRemotePageIndicatorState(
                        selectedPage = 3.ri,
                        pageOffset = 0f.rf,
                        pageCount = 5,
                    )
                RemoteVerticalPageIndicator(
                    state = state,
                    selectedColor = Color.Blue.rc,
                    unselectedColor = Color.Green.rc,
                    backgroundColor = Color.Black.rc,
                )
            }
        }
        composeRule.setContent {
            Row {
                Box(modifier = Modifier.size(500.dp, 500.dp).testTag("offset80")) {
                    RcPlayer(document = offset80Doc)
                }
                Box(modifier = Modifier.size(500.dp, 500.dp).testTag("settled")) {
                    RcPlayer(document = settledDoc)
                }
            }
        }
        val offset80Bitmap =
            composeRule.onNodeWithTag("offset80").captureToImage().asAndroidBitmap()
        val settledBitmap = composeRule.onNodeWithTag("settled").captureToImage().asAndroidBitmap()
        val greenPixelsAtOffset80 = offset80Bitmap.countPixelsMatching {
            it.green > 0.5f && it.red < 0.2f && it.blue < 0.2f
        }
        val greenPixelsWhenSettled = settledBitmap.countPixelsMatching {
            it.green > 0.5f && it.red < 0.2f && it.blue < 0.2f
        }
        assertThat(greenPixelsAtOffset80).isLessThan(greenPixelsWhenSettled)
    }
}
