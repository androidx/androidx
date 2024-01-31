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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PullToRefreshIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun indicatorDisplayed_refreshing() {
        rule.setContent {
            val density = LocalDensity.current
            Box(Modifier.fillMaxSize()) {
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 0.5f
                        override val verticalOffset = with(density) { 50.dp.toPx() }
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = true
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                    modifier = Modifier.testTag(INDICATOR_TAG)
                )
            }
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_notRefreshing() {
        rule.setContent {
            val density = LocalDensity.current
            Box(Modifier.fillMaxSize()) {
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 0.5f
                        override val verticalOffset = with(density) { 50.dp.toPx() }
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = false
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                    modifier = Modifier.testTag(INDICATOR_TAG)
                )
            }
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicatorRespects_changingOffset() {
        val containerSize = 30.dp
        val verticalOffsetDp = mutableStateOf(0.dp)
        val expectedOffset = derivedStateOf {
            verticalOffsetDp.value - (containerSize + SpinnerSize) / 2
        }
        rule.setContent {
            val density = LocalDensity.current
            Box(Modifier.fillMaxSize()) {
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 0.5f
                        override val verticalOffset =
                            with(density) { verticalOffsetDp.value.toPx() }
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = false
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                    modifier = Modifier.size(containerSize).testTag(INDICATOR_TAG)
                )
            }
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(expectedOffset.value)

        verticalOffsetDp.value = 100.dp
        rule.waitForIdle()

        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(expectedOffset.value)
    }

    // Regression test for b/271777421
    @Test
    fun indicatorDoesNotCapturePointerEvents() {
        var verticalOffset by mutableFloatStateOf(0f)
        var indicatorSize: IntSize? = null
        var downEvent: PointerInputChange? = null

        rule.setContent {
            Box {
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        downEvent = awaitFirstDown()
                    }
                })
                PullToRefreshContainer(
                    state = object : PullToRefreshState {
                        override val positionalThreshold: Float
                            get() = TODO("Not yet implemented")
                        override val progress = 0f
                        override val verticalOffset = verticalOffset
                        override var nestedScrollConnection: NestedScrollConnection
                            get() = TODO("Not yet implemented")
                            set(_) {}
                        override val isRefreshing = false
                        override fun startRefresh() {
                            TODO("Not yet implemented")
                        }

                        override fun endRefresh() {
                            TODO("Not yet implemented")
                        }
                    },
                    modifier = Modifier.onSizeChanged {
                        // The indicator starts as offset by its negative height in the y direction,
                        // so work out its height so we can place it inside its normal layout
                        // bounds
                        indicatorSize = it
                    }.testTag(INDICATOR_TAG)
                )
            }
        }

        rule.runOnIdle {
            // Pull by twice the indicator height (since pull delta is halved) - this will make the
            // indicator fully visible in its layout bounds, so when we performClick() the indicator
            // will be visibly inside those coordinates.
            verticalOffset = indicatorSize!!.height.toFloat() * 2
        }

        rule.onNodeWithTag(INDICATOR_TAG).performClick()
        rule.runOnIdle {
            // The indicator should not have blocked its sibling (placed first, so below) from
            // seeing touch events.
            Truth.assertThat(downEvent).isNotNull()
        }
    }

    private val INDICATOR_TAG = "pull-refresh-indicator"
}
