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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PullToRefreshStateImplTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun refreshTrigger_onlyAfterThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state = PullToRefreshState()

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            var isRefreshing by mutableStateOf(false)
            PullToRefreshBox(
                modifier = Modifier.testTag(PullRefreshTag),
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    refreshCount++
                    isRefreshing = false
                }
            ) {
                LazyColumn { items(100) { Text("item $it") } }
            }
        }
        // Account for DragModifier - pull down twice the threshold value.
        pullRefreshNode.performTouchInput {
            swipeDown(endY = 2 * positionalThreshold + touchSlop - 1f)
        }

        rule.waitForIdle()

        // Equal to threshold
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * positionalThreshold + touchSlop) }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            // Since onRefresh was not called, we should reset the position back to 0
            assertThat(state.distanceFraction).isEqualTo(0f)
        }

        pullRefreshNode.performTouchInput {
            swipeDown(endY = 2 * positionalThreshold + touchSlop + 1f)
        }

        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun progressAndVerticalOffset_scaleCorrectly_untilThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state =
            object : PullToRefreshState {

                var distanceFractionState by mutableStateOf(0f)
                override val distanceFraction: Float
                    get() = distanceFractionState

                override val isAnimating: Boolean
                    get() = false

                override suspend fun animateToThreshold() {}

                override suspend fun animateToHidden() {}

                override suspend fun snapTo(targetValue: Float) {
                    distanceFractionState = targetValue
                }
            }

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(PullRefreshTag),
                isRefreshing = false,
                onRefresh = { refreshCount++ },
                state = state
            ) {
                LazyColumn { items(100) { Text("item $it") } }
            }
        }

        pullRefreshNode.performTouchInput { swipeDown(endY = positionalThreshold + touchSlop) }

        rule.runOnIdle {
            // Expected values given drag modifier of 0.5f
            assertThat(state.distanceFraction).isEqualTo(0.5f)
            assertThat(refreshCount).isEqualTo(0)
        }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_beyondThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state =
            object : PullToRefreshState {

                var distanceFractionState by mutableStateOf(0f)
                override val distanceFraction: Float
                    get() = distanceFractionState

                override val isAnimating: Boolean
                    get() = false

                override suspend fun animateToThreshold() {}

                override suspend fun animateToHidden() {}

                override suspend fun snapTo(targetValue: Float) {
                    distanceFractionState = targetValue
                }
            }

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(PullRefreshTag),
                isRefreshing = false,
                onRefresh = { refreshCount++ },
                state = state
            ) {
                LazyColumn { items(100) { Text("item $it") } }
            }
        }

        pullRefreshNode.performTouchInput { swipeDown(endY = positionalThreshold * 2 + touchSlop) }
        rule.runOnIdle {
            assertThat(state.distanceFraction).isEqualTo(1f)
            // Account for PullMultiplier.
            assertThat(refreshCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput { swipeDown(endY = 3 * positionalThreshold + touchSlop) }

        rule.runOnIdle {
            assertThat(state.distanceFraction).isWithin(0.1f).of(1.5f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    //
    @Test
    fun positionIsCapped() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state =
            object : PullToRefreshState {

                var distanceFractionState by mutableStateOf(0f)
                override val distanceFraction: Float
                    get() = distanceFractionState

                override val isAnimating: Boolean
                    get() = false

                override suspend fun animateToThreshold() {}

                override suspend fun animateToHidden() {}

                override suspend fun snapTo(targetValue: Float) {
                    distanceFractionState = targetValue
                }
            }

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(PullRefreshTag),
                isRefreshing = false,
                onRefresh = { refreshCount++ },
                state = state
            ) {
                LazyColumn { items(100) { Text("item $it") } }
            }
        }
        pullRefreshNode.performTouchInput { swipeDown(endY = 10 * positionalThreshold + touchSlop) }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(2f) }
    }

    @Test
    fun state_restoresPullRefreshState() {
        val restorationTester = StateRestorationTester(rule)
        var state: PullToRefreshState? = null
        restorationTester.setContent { state = rememberPullToRefreshState() }

        runBlocking { state!!.snapTo(0.5f) }
        state = null
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state!!.distanceFraction).isEqualTo(0.5f)
    }

    private val PullRefreshTag = "PullRefresh"
    private val pullRefreshNode = rule.onNodeWithTag(PullRefreshTag)
}
