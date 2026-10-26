/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.pulltorefresh

import android.os.Build
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PullToRefreshIndicatorTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun indicatorDisplayed_refreshing() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.testTag(INDICATOR_TAG),
                    state = rememberPullToRefreshState(),
                    isRefreshing = true,
                )
            }
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_notRefreshing() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.testTag(INDICATOR_TAG),
                    isRefreshing = false,
                    state = rememberPullToRefreshState(),
                )
            }
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicatorRespects_changingOffset() {
        val containerSize = 30.dp
        val verticalOffsetDp = 200.dp
        val state = PullToRefreshState()
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.size(containerSize).testTag(INDICATOR_TAG),
                    state = state,
                    isRefreshing = true,
                    maxDistance = verticalOffsetDp,
                )
            }
            LaunchedEffect(true) { state.animateToThreshold() }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(verticalOffsetDp - (containerSize + SpinnerSize) / 2)

        runBlocking { state.snapTo(0.5f) }
        rule.waitForIdle()

        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(100.dp - (containerSize + SpinnerSize) / 2)
    }

    // Regression test for b/271777421
    @Test
    fun indicatorDoesNotCapturePointerEvents() {
        var downEvent: PointerInputChange? = null

        rule.setContent {
            Box {
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitEachGesture { downEvent = awaitFirstDown() }
                    }
                )

                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.testTag(INDICATOR_TAG),
                    state = rememberPullToRefreshState(),
                    isRefreshing = true,
                )
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(INDICATOR_TAG).performClick()
        rule.runOnIdle {
            // The indicator should not have blocked its sibling (placed first, so below) from
            // seeing touch events.
            Truth.assertThat(downEvent).isNotNull()
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun loadingIndicator_pulledWithoutRecomposition_drawsIndicator() {
        val state = PullToRefreshState()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.testTag(INDICATOR_TAG),
                    state = state,
                    isRefreshing = false,
                    containerColor = Color.White,
                    color = Color.Black,
                    elevation = 0.dp,
                    // Places the fully pulled indicator at its layout position.
                    maxDistance = LoaderIndicatorHeight,
                )
            }
        }
        rule.waitForIdle()

        // Pull the indicator while the clock is paused, so no recomposition happens before the
        // indicator is drawn.
        runBlocking { state.snapTo(1f) }

        rule.onNodeWithTag(INDICATOR_TAG).captureToImage().toPixelMap().let {
            Truth.assertThat(it[it.width / 2, it.height / 2]).isEqualTo(Color.Black)
        }
    }

    private val INDICATOR_TAG = "pull-refresh-indicator"
}
