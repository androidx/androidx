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

package androidx.compose.material3.pullrefresh

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
class PullRefreshIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun indicatorDisplayed_refreshingInitially() {
        rule.setContent {
            val state = androidx.compose.material.pullrefresh.rememberPullRefreshState(true, {})
            Box(Modifier.fillMaxSize()) {
                androidx.compose.material.pullrefresh.PullRefreshIndicator(
                    true,
                    state,
                    Modifier.testTag(androidx.compose.material.pullrefresh.IndicatorTag)
                )
            }
        }
        indicatorNode.assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_setRefreshing() {
        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state =
                androidx.compose.material.pullrefresh.rememberPullRefreshState(refreshing, {})
            Box(Modifier.fillMaxSize()) {
                androidx.compose.material.pullrefresh.PullRefreshIndicator(
                    refreshing,
                    state,
                    Modifier.testTag(androidx.compose.material.pullrefresh.IndicatorTag)
                )
            }
        }

        indicatorNode.assertIsNotDisplayed()

        refreshing = true

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_pullRefresh() {
        var refreshCount = 0

        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state = androidx.compose.material.pullrefresh.rememberPullRefreshState(
                refreshing,
                { refreshing = true; refreshCount++ })

            Box(
                Modifier
                    .pullRefresh(state)
                    .testTag(androidx.compose.material.pullrefresh.PullRefreshTag)
            ) {
                LazyColumn {
                    items(30) {
                        androidx.compose.material.ListItem {
                            androidx.compose.material.Text("Item: $it")
                        }
                    }
                }
                androidx.compose.material.pullrefresh.PullRefreshIndicator(
                    refreshing,
                    state,
                    Modifier.testTag(androidx.compose.material.pullrefresh.IndicatorTag)
                )
            }
        }

        indicatorNode.assertIsNotDisplayed()
        pullRefreshNode.performTouchInput { swipeDown() }

        rule.waitForIdle()
        Truth.assertThat(refreshCount).isEqualTo(1)
        Truth.assertThat(refreshing).isTrue()
        indicatorNode.assertIsDisplayed()

        refreshing = false

        rule.waitForIdle()
        indicatorNode.assertIsNotDisplayed()
    }

    @Test
    fun refreshingIndicator_returnsToRest() {
        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state =
                androidx.compose.material.pullrefresh.rememberPullRefreshState(refreshing, {})

            Box(
                Modifier
                    .pullRefresh(state)
                    .testTag(androidx.compose.material.pullrefresh.PullRefreshTag)
            ) {
                LazyColumn {
                    items(30) {
                        androidx.compose.material.ListItem {
                            androidx.compose.material.Text("Item: $it")
                        }
                    }
                }
                androidx.compose.material.pullrefresh.PullRefreshIndicator(
                    refreshing,
                    state,
                    Modifier.testTag(androidx.compose.material.pullrefresh.IndicatorTag)
                )
            }
        }

        indicatorNode.assertIsNotDisplayed()
        val restingBounds = indicatorNode.getUnclippedBoundsInRoot()

        refreshing = true

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
        val refreshingBounds = indicatorNode.getUnclippedBoundsInRoot()

        pullRefreshNode.performTouchInput { swipeDown() }

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
        Truth.assertThat(indicatorNode.getUnclippedBoundsInRoot()).isEqualTo(refreshingBounds)

        refreshing = false

        rule.waitForIdle()
        indicatorNode.assertIsNotDisplayed()
        Truth.assertThat(indicatorNode.getUnclippedBoundsInRoot()).isEqualTo(restingBounds)
    }

    // Regression test for b/271777421
    @Test
    fun indicatorDoesNotCapturePointerEvents() {
        var indicatorSize: IntSize? = null
        lateinit var state: androidx.compose.material.pullrefresh.PullRefreshState
        var downEvent: PointerInputChange? = null

        rule.setContent {
            state = androidx.compose.material.pullrefresh.rememberPullRefreshState(false, {})

            Box {
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        downEvent = awaitFirstDown()
                    }
                })
                androidx.compose.material.pullrefresh.PullRefreshIndicator(
                    refreshing = false,
                    state = state,
                    modifier = Modifier.onSizeChanged {
                        // The indicator starts as offset by its negative height in the y direction,
                        // so work out its height so we can place it inside its normal layout
                        // bounds
                        indicatorSize = it
                    }.testTag(androidx.compose.material.pullrefresh.IndicatorTag)
                )
            }
        }

        rule.runOnIdle {
            // Pull by twice the indicator height (since pull delta is halved) - this will make the
            // indicator fully visible in its layout bounds, so when we performClick() the indicator
            // will be visibly inside those coordinates.
            state.onPull(indicatorSize!!.height.toFloat() * 2)
        }

        rule.onNodeWithTag(androidx.compose.material.pullrefresh.IndicatorTag).performClick()
        rule.runOnIdle {
            // The indicator should not have blocked its sibling (placed first, so below) from
            // seeing touch events.
            Truth.assertThat(downEvent).isNotNull()
        }
    }

    private val pullRefreshNode get() = rule.onNodeWithTag(androidx.compose.material.pullrefresh.PullRefreshTag)
    private val indicatorNode get() = rule.onNodeWithTag(androidx.compose.material.pullrefresh.IndicatorTag).onChild()
}