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

package androidx.xr.glimmer.pager

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(Parameterized::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class GlimmerHorizontalPagerFocusTest(private val config: GlimmerPagerParamConfig) :
    BaseParameterizedGlimmerPagerTest() {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun focusReenter_afterFocusMove_focusesOnCurrentPage() = runTest {
        val state = GlimmerPagerState { 2 }
        val anotherFocusTargetRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setGlimmerThemeContent {
            focusManager = LocalFocusManager.current
            Column {
                GlimmerParameterizedPager(config = config, state = state) { page ->
                    Page("Page $page")
                }
                Box(Modifier.size(100.dp).focusRequester(anotherFocusTargetRequester).focusable())
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()

        runOnUiThread { state.scrollToPage(1) }
        rule.onNodeWithTag("Page 1").assertIsFocused()

        rule.runOnIdle { anotherFocusTargetRequester.requestFocus() }

        rule.onNodeWithTag("Page 1").assertIsNotFocused()

        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.onNodeWithTag("Page 1").assertIsFocused()
    }

    @Test
    fun swipeForward_movesFocusForward() {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val page2FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 3 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                    2 ->
                        Page(
                            "Page 2",
                            modifier = Modifier.onFocusEvent { page2FocusEvents.add(it) },
                        )
                }
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).isEmpty()
            assertThat(page2FocusEvents).isEmpty()
        }
        val pageSize = state.layoutInfo.pageSize

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()
        performIndirectSwipe(pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(1)
            assertThat(page0FocusEvents[0].isFocused).isFalse()

            assertThat(page1FocusEvents).hasSize(2)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
            assertThat(page1FocusEvents[1].isFocused).isTrue()

            assertThat(page2FocusEvents).hasSize(1)
            assertThat(page2FocusEvents[0].isFocused).isFalse()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()
        performIndirectSwipe(pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).isEmpty()

            assertThat(page1FocusEvents).hasSize(1)
            assertThat(page1FocusEvents[0].isFocused).isFalse()

            assertThat(page2FocusEvents).hasSize(1)
            assertThat(page2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackward_movesFocusBackward() = runTest {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val page2FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 3 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                    2 ->
                        Page(
                            "Page 2",
                            modifier = Modifier.onFocusEvent { page2FocusEvents.add(it) },
                        )
                }
            }
        }
        runOnUiThread { state.scrollToPage(2) }
        rule.onNodeWithTag("Page 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(3)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()
            assertThat(page0FocusEvents[2].isFocused).isFalse()

            assertThat(page1FocusEvents).isEmpty()

            assertThat(page2FocusEvents).hasSize(2)
            assertThat(page2FocusEvents[0].isFocused).isFalse()
            assertThat(page2FocusEvents[1].isFocused).isTrue()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()
        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe(-pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(1)
            assertThat(page0FocusEvents[0].isFocused).isFalse()

            assertThat(page1FocusEvents).hasSize(2)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
            assertThat(page1FocusEvents[1].isFocused).isTrue()

            assertThat(page2FocusEvents).hasSize(1)
            assertThat(page2FocusEvents[0].isFocused).isFalse()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()
        performIndirectSwipe(-pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(1)
            assertThat(page0FocusEvents[0].isFocused).isTrue()

            assertThat(page1FocusEvents).hasSize(1)
            assertThat(page1FocusEvents[0].isFocused).isFalse()

            assertThat(page2FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardShortDistance_doesNotMoveFocus() {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 2 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                }
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).isEmpty()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe((pageSize * 0.1f * config.scrollSign).toInt())

        rule.onNodeWithTag("Page 0").assertIsFocused()
    }

    @Test
    fun swipeBackwardShortDistance_doesNotMoveFocus() = runTest {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 2 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                }
            }
        }
        runOnUiThread { state.scrollToPage(1) }
        rule.onNodeWithTag("Page 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(3)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()
            assertThat(page0FocusEvents[2].isFocused).isFalse()

            assertThat(page1FocusEvents).hasSize(2)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
            assertThat(page1FocusEvents[1].isFocused).isTrue()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe(-(pageSize * 0.1f * config.scrollSign).toInt())

        rule.onNodeWithTag("Page 1").assertIsFocused()
    }

    @Test
    fun swipeForwardAlmostPageWidth_movesFocusToNextPage() {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 2 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                }
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).isEmpty()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe((pageSize * 0.9f * config.scrollSign).toInt())

        rule.onNodeWithTag("Page 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(1)
            assertThat(page0FocusEvents[0].isFocused).isFalse()

            assertThat(page1FocusEvents).hasSize(2)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
            assertThat(page1FocusEvents[1].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackwardAlmostPageWidth_movesFocusToPreviousPage() = runTest {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 2 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                }
            }
        }
        runOnUiThread { state.scrollToPage(1) }
        rule.onNodeWithTag("Page 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(3)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()
            assertThat(page0FocusEvents[2].isFocused).isFalse()

            assertThat(page1FocusEvents).hasSize(2)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
            assertThat(page1FocusEvents[1].isFocused).isTrue()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe(-pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).hasSize(1)
            assertThat(page1FocusEvents[0].isFocused).isFalse()
        }
    }

    @Test
    fun swipeForward_pagerIsNotFocused_doesNotMoveFocus() {
        val nonPagerFocusRequester = FocusRequester()
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val page2FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 3 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                    2 ->
                        Page(
                            "Page 2",
                            modifier = Modifier.onFocusEvent { page2FocusEvents.add(it) },
                        )
                }
            }
            Box(modifier = Modifier.focusRequester(nonPagerFocusRequester).focusTarget())
        }
        rule.runOnIdle { nonPagerFocusRequester.requestFocus() }
        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()

        val pageSize = state.layoutInfo.pageSize
        performIndirectSwipe(pageSize * config.scrollSign)

        rule.onNodeWithTag("Page 0").assertIsNotFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).isEmpty()
            assertThat(page1FocusEvents).isEmpty()
            assertThat(page2FocusEvents).isEmpty()
        }
    }

    @Test
    fun scrollToPage_pagerIsNotFocused_doesNotMovesFocus() = runTest {
        val nonPagerFocusRequester = FocusRequester()
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val page2FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 3 }
        rule.setGlimmerThemeContent {
            Column {
                GlimmerParameterizedPager(config = config, state = state) { page ->
                    when (page) {
                        0 ->
                            Page(
                                "Page 0",
                                modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                            )
                        1 ->
                            Page(
                                "Page 1",
                                modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                            )
                        2 ->
                            Page(
                                "Page 2",
                                modifier = Modifier.onFocusEvent { page2FocusEvents.add(it) },
                            )
                    }
                }
                Box(modifier = Modifier.focusRequester(nonPagerFocusRequester).focusTarget())
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).isEmpty()
            assertThat(page2FocusEvents).isEmpty()
        }

        rule.runOnIdle { nonPagerFocusRequester.requestFocus() }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()

        runOnUiThread { state.scrollToPage(2) }

        rule.onNodeWithTag("Page 2").assertIsNotFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).isEmpty()
            assertThat(page1FocusEvents).isEmpty()
            assertThat(page2FocusEvents).hasSize(1)
            assertThat(page2FocusEvents[0].isFocused).isFalse()
        }
    }

    @Test
    fun scrollToPage_movesFocus() = runTest {
        val page0FocusEvents = mutableListOf<FocusState>()
        val page1FocusEvents = mutableListOf<FocusState>()
        val page2FocusEvents = mutableListOf<FocusState>()
        val state = GlimmerPagerState { 3 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page ->
                when (page) {
                    0 ->
                        Page(
                            "Page 0",
                            modifier = Modifier.onFocusEvent { page0FocusEvents.add(it) },
                        )
                    1 ->
                        Page(
                            "Page 1",
                            modifier = Modifier.onFocusEvent { page1FocusEvents.add(it) },
                        )
                    2 ->
                        Page(
                            "Page 2",
                            modifier = Modifier.onFocusEvent { page2FocusEvents.add(it) },
                        )
                }
            }
        }
        rule.onNodeWithTag("Page 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(2)
            assertThat(page0FocusEvents[0].isFocused).isFalse()
            assertThat(page0FocusEvents[1].isFocused).isTrue()

            assertThat(page1FocusEvents).isEmpty()
            assertThat(page2FocusEvents).isEmpty()
        }

        page0FocusEvents.clear()
        page1FocusEvents.clear()
        page2FocusEvents.clear()

        runOnUiThread { state.scrollToPage(2) }

        rule.onNodeWithTag("Page 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(page0FocusEvents).hasSize(1)
            assertThat(page0FocusEvents[0].isFocused).isFalse()

            assertThat(page1FocusEvents).isEmpty()

            assertThat(page2FocusEvents).hasSize(2)
            assertThat(page2FocusEvents[0].isFocused).isFalse()
            assertThat(page2FocusEvents[1].isFocused).isTrue()
        }
    }

    @Test
    fun updateGlimmerPagerState_resetsFocusTrackingAndReRequestsFocus() = runTest {
        var state by mutableStateOf(GlimmerPagerState { 2 })

        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.onNodeWithTag("Page 0").assertIsFocused()

        runOnUiThread { state.scrollToPage(1) }
        rule.onNodeWithTag("Page 1").assertIsFocused()

        val newState = GlimmerPagerState(currentPage = 1) { 2 }
        state = newState

        rule.waitForIdle()

        rule.onNodeWithTag("Page 1").assertIsFocused()
    }

    @Composable
    private fun Page(text: String, modifier: Modifier = Modifier) {
        Box(modifier.fillMaxSize().focusable().testTag(text)) { Text(text) }
    }

    private fun performIndirectSwipe(distancePx: Int, durationMillis: Long = 200L) {
        require(distancePx != 0)
        rule
            .onRoot()
            .performIndirectSwipe(rule, distancePx.toFloat(), moveDuration = durationMillis)
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllGlimmerPagerTestParams
    }
}
