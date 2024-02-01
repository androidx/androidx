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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PullToRefreshStateImplTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun refreshTrigger_onlyAfterThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        val positionalThreshold = 400f
        lateinit var state: PullToRefreshState

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            state = remember {
                PullToRefreshStateImpl(
                    initialRefreshing = false,
                    positionalThreshold = positionalThreshold,
                    enabled = { true },
                )
            }
            if (state.isRefreshing) {
                LaunchedEffect(true) {
                    refreshCount++
                    state.endRefresh()
                }
            }
            Box(
                Modifier
                    .nestedScroll(state.nestedScrollConnection)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }
        // Account for DragModifier - pull down twice the threshold value.

        // Less than threshold
        pullRefreshNode.performTouchInput {
            swipeDown(endY = 2 * positionalThreshold + touchSlop - 1f)
        }

        rule.waitForIdle()

        // Equal to threshold
        pullRefreshNode.performTouchInput {
            swipeDown(endY = 2 * positionalThreshold + touchSlop)
        }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            // Since onRefresh was not called, we should reset the position back to 0
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.verticalOffset).isEqualTo(0f)
        }

        pullRefreshNode.performTouchInput {
            swipeDown(endY = 2 * positionalThreshold + touchSlop + 1f)
        }
        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun progressAndVerticalOffset_scaleCorrectly_untilThreshold() {
        lateinit var state: PullToRefreshStateImpl
        var refreshCount = 0
        val positionalThreshold = 400f

        rule.setContent {
            state = remember {
                PullToRefreshStateImpl(
                    initialRefreshing = false,
                    positionalThreshold = positionalThreshold,
                    enabled = { true },
                )
            }
            if (state.isRefreshing) {
                LaunchedEffect(true) {
                    refreshCount++
                    state.endRefresh()
                }
            }
            Box(
                Modifier
                    .nestedScroll(state.nestedScrollConnection)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.distancePulled = positionalThreshold
        rule.runOnIdle {
            // Expected values given drag modifier of 0.5f
            assertThat(state.progress).isEqualTo(0.5f)
            assertThat(state.calculateVerticalOffset()).isEqualTo(200f)
            assertThat(refreshCount).isEqualTo(0)
        }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_beyondThreshold() {
        lateinit var state: PullToRefreshStateImpl
        lateinit var scope: CoroutineScope
        var refreshCount = 0
        val positionalThreshold = 400f

        rule.setContent {
            state = remember {
                PullToRefreshStateImpl(
                    initialRefreshing = false,
                    positionalThreshold = positionalThreshold,
                    enabled = { true },
                )
            }
            scope = rememberCoroutineScope()
            if (state.isRefreshing) {
                LaunchedEffect(true) {
                    refreshCount++
                    state.endRefresh()
                }
            }
            Box(
                Modifier
                    .nestedScroll(state.nestedScrollConnection)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.distancePulled = 2 * positionalThreshold
        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(1f)
            // Account for PullMultiplier.
            assertThat(state.calculateVerticalOffset()).isEqualTo(positionalThreshold)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.distancePulled += positionalThreshold

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(1.5f)
            assertThat(refreshCount).isEqualTo(0)
        }

        scope.launch { state.onRelease(0f) }
        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun positionIsCapped() {
        lateinit var state: PullToRefreshStateImpl
        var refreshCount = 0
        val positionalThreshold = 400f

        rule.setContent {
            state = remember {
                PullToRefreshStateImpl(
                    initialRefreshing = false,
                    positionalThreshold = positionalThreshold,
                    enabled = { true },
                )
            }
            if (state.isRefreshing) {
                LaunchedEffect(true) {
                    refreshCount++
                    state.endRefresh()
                }
            }
            Box(
                Modifier
                    .nestedScroll(state.nestedScrollConnection)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }
        state.distancePulled = 10 * positionalThreshold

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(5f) // Account for PullMultiplier.
            // Indicator position is capped to 2 times the refresh threshold.
            assertThat(state.calculateVerticalOffset()).isEqualTo(2 * positionalThreshold)
            assertThat(refreshCount).isEqualTo(0)
        }
    }

    @Test
    fun nestedPreScroll_negativeDelta_notRefreshing() {
        var refreshCount = 0
        val positionalThreshold = 200f
        lateinit var state: PullToRefreshStateImpl
        val dispatcher = NestedScrollDispatcher()
        val connection = object : NestedScrollConnection {}

        rule.setContent {
            state = remember {
                PullToRefreshStateImpl(
                    initialRefreshing = false,
                    positionalThreshold = positionalThreshold,
                    enabled = { true },
                )
            }
            if (state.isRefreshing) {
                LaunchedEffect(true) {
                    refreshCount++
                    state.endRefresh()
                }
            }
            Box(
                Modifier
                    .nestedScroll(state.nestedScrollConnection)
                    .testTag(PullRefreshTag)) {
                Box(
                    Modifier
                        .size(100.dp)
                        .nestedScroll(connection, dispatcher))
            }
        }
        // 100 pixels up
        val dragUpOffset = Offset(0f, -100f)

        rule.runOnIdle {
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is not showing, so we should consume nothing
            assertThat(preConsumed).isEqualTo(Offset.Zero)
            assertThat(state.verticalOffset).isEqualTo(0f)
        }

        // Pull the state by a bit
        state.distancePulled = 200f

        rule.runOnIdle {
            assertThat(state.calculateVerticalOffset())
                .isEqualTo(100f /* 200 / 2 for drag multiplier */)
            val preConsumed = dispatcher.dispatchPreScroll(dragUpOffset, NestedScrollSource.Drag)
            // Pull refresh is currently showing, so we should consume all the delta
            assertThat(preConsumed).isEqualTo(dragUpOffset)
            assertThat(state.calculateVerticalOffset())
                .isEqualTo(50f /* (200 - 100) / 2 for drag multiplier */)
        }
    }

    @Test
    fun state_restoresPullRefreshState() {
        val restorationTester = StateRestorationTester(rule)
        var pullToRefreshState: PullToRefreshState? = null
        lateinit var scope: CoroutineScope
        restorationTester.setContent {
            pullToRefreshState = rememberPullToRefreshState()
            scope = rememberCoroutineScope()
        }

        with(pullToRefreshState!!) {
            rule.runOnIdle { scope.launch { startRefresh() } }
            pullToRefreshState = null
            restorationTester.emulateSavedInstanceStateRestore()
            assertThat(isRefreshing).isTrue()
        }
    }

    private val PullRefreshTag = "PullRefresh"
    private val pullRefreshNode = rule.onNodeWithTag(PullRefreshTag)
}
