/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.stack

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.createGlimmerRule
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class VerticalStackFocusTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun initialFocus_largerSecondItem_focusesOnFirstItem() {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item { Box(Modifier.focusable().size(10.dp).testTag("Item 0")) }
                item { Box(Modifier.fillMaxSize().focusable().testTag("Item 1")) }
            }
        }

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()
    }

    @Test
    fun focusReenter_afterFocusMove_focusesOnTopItem() = runTest {
        val state = StackState()
        val anotherFocusTargetRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            Column {
                VerticalStack(state = state) {
                    item { StackItem("Item 0") }
                    item { StackItem("Item 1") }
                }
                Box(Modifier.size(100.dp).focusRequester(anotherFocusTargetRequester).focusable())
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsFocused()

        rule.runOnIdle { anotherFocusTargetRequester.requestFocus() }

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()

        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Previous) }

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsFocused()
    }

    @Test
    fun swipeForward_movesFocusForward() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).isEmpty()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackward_movesFocusBackward() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(2)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
            assertThat(item2FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()
        performIndirectSwipe(-itemHeight)

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardShortDistance_doesNotMoveFocus() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeBackwardShortDistance_doesNotMoveFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.1f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
        }
    }

    @Test
    fun swipeForwardAlmostItemHeight_movesFocusToNextItem() {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe((itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isTrue()
        }
    }

    @Test
    fun swipeBackwardAlmostItemHeight_movesFocusToPreviousItem() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.onNodeWithTag("Item 1").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(3)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()
            assertThat(item0FocusEvents[2].isFocused).isFalse()

            assertThat(item1FocusEvents).hasSize(2)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
            assertThat(item1FocusEvents[1].isFocused).isTrue()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        performIndirectSwipe(-(itemHeight * 0.9f).toInt())

        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()
        }
    }

    @Test
    fun swipeForward_stackIsNotFocused_doesNotMoveFocus() {
        val nonStackFocusRequester = FocusRequester()
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    ) {
                        itemHeight = it
                    }
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
            Box(modifier = Modifier.focusRequester(nonStackFocusRequester).focusTarget())
        }
        rule.runOnIdle { nonStackFocusRequester.requestFocus() }
        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        performIndirectSwipe(itemHeight)

        rule.onNodeWithTag("Item 0").assertIsNotFocused()
        rule.onNodeWithTag("Item 1").assertIsNotFocused()
        rule.onNodeWithTag("Item 2").assertIsNotFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).isEmpty()
            assertThat(item1FocusEvents).isEmpty()
            assertThat(item2FocusEvents).isEmpty()
        }
    }

    @Test
    fun scrollToItem_movesFocus() = runTest {
        val item0FocusEvents = mutableListOf<FocusState>()
        val item1FocusEvents = mutableListOf<FocusState>()
        val item2FocusEvents = mutableListOf<FocusState>()
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                item {
                    StackItem(
                        "Item 0",
                        modifier = Modifier.onFocusEvent { item0FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 1",
                        modifier = Modifier.onFocusEvent { item1FocusEvents.add(it) },
                    )
                }
                item {
                    StackItem(
                        "Item 2",
                        modifier = Modifier.onFocusEvent { item2FocusEvents.add(it) },
                    )
                }
            }
        }
        rule.onNodeWithTag("Item 0").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(2)
            assertThat(item0FocusEvents[0].isFocused).isFalse()
            assertThat(item0FocusEvents[1].isFocused).isTrue()

            assertThat(item1FocusEvents).hasSize(1)
            assertThat(item1FocusEvents[0].isFocused).isFalse()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isFalse()
        }

        item0FocusEvents.clear()
        item1FocusEvents.clear()
        item2FocusEvents.clear()

        runOnUiThread { state.scrollToItem(2) }

        rule.onNodeWithTag("Item 2").assertIsFocused()
        rule.runOnIdle {
            assertThat(item0FocusEvents).hasSize(1)
            assertThat(item0FocusEvents[0].isFocused).isFalse()

            assertThat(item1FocusEvents).isEmpty()

            assertThat(item2FocusEvents).hasSize(1)
            assertThat(item2FocusEvents[0].isFocused).isTrue()
        }
    }

    @Composable
    private fun StackItem(
        text: String,
        modifier: Modifier = Modifier,
        onHeightChanged: (Int) -> Unit = {},
    ) {
        Box(
            modifier
                .onSizeChanged { onHeightChanged(it.height) }
                .fillMaxSize()
                .focusable()
                .testTag(text)
        ) {
            Text(text)
        }
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
}
