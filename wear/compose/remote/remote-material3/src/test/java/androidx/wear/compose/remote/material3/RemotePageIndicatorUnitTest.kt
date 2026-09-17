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
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
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
@Config(sdk = [35], qualifiers = "w500dp-h500dp")
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
}
