/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SwipeToDismissTest {
    @get:Rule val rule = createComposeRule()

    private val backgroundTag = "background"
    private val dismissContentTag = "dismissContent"
    private val swipeDismissTag = "swipeDismiss"

    private val restorationTester = StateRestorationTester(rule)

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    @Test
    fun test_stateSavedAndRestored() {
        val initialValue = SwipeToDismissBoxValue.Settled
        val expectedValue = SwipeToDismissBoxValue.StartToEnd
        lateinit var scope: CoroutineScope
        lateinit var state: SwipeToDismissBoxState
        restorationTester.setContent {
            scope = rememberCoroutineScope()
            state = rememberSwipeToDismissBoxState(initialValue)
        }
        scope.launch { state.snapTo(expectedValue) }
        assertThat(state.settledValue).isEqualTo(expectedValue)
        restorationTester.emulateSavedInstanceStateRestore()
        assertThat(state.settledValue).isEqualTo(expectedValue)
    }

    @Test
    fun swipeDismiss_testOffset_whenDefault() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled),
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize().testTag(dismissContentTag))
            }
        }

        rule.onNodeWithTag(dismissContentTag).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToEnd() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.StartToEnd),
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize().testTag(dismissContentTag))
            }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag).assertLeftPositionInRootIsEqualTo(width)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToEnd_rtl() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.StartToEnd),
                    backgroundContent = {},
                ) {
                    Box(Modifier.fillMaxSize().testTag(dismissContentTag))
                }
            }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag).assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToStart() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.EndToStart),
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize().testTag(dismissContentTag))
            }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag).assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    fun swipeDismiss_testOffset_whenDismissedToStart_rtl() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.EndToStart),
                    backgroundContent = {},
                ) {
                    Box(Modifier.fillMaxSize().testTag(dismissContentTag))
                }
            }
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag(dismissContentTag).assertLeftPositionInRootIsEqualTo(width)
    }

    @Test
    fun swipeDismiss_testBackgroundMatchesContentSize() {
        rule.setContent {
            SwipeToDismissBox(
                state = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled),
                backgroundContent = { Box(Modifier.fillMaxSize().testTag(backgroundTag)) },
            ) {
                Box(Modifier.size(100.dp))
            }
        }

        rule.onNodeWithTag(backgroundTag).assertIsSquareWithSize(100.dp)
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toEnd() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled)
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                modifier = Modifier.testTag(swipeDismissTag),
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = false,
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize())
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.StartToEnd)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toStart() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled)
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                modifier = Modifier.testTag(swipeDismissTag),
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize())
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.EndToStart)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toEnd_rtl() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState = rememberSwipeToDismissBoxState()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = swipeToDismissBoxState,
                    modifier = Modifier.testTag(swipeDismissTag),
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = false,
                    backgroundContent = {},
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.StartToEnd)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_toStart_rtl() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SwipeToDismissBox(
                    state = swipeToDismissBoxState,
                    modifier = Modifier.testTag(swipeDismissTag),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {},
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.EndToStart)
        }
    }

    @Test
    fun swipeDismiss_dismissBySwipe_disabled() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState =
                rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.StartToEnd)
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                modifier = Modifier.testTag(swipeDismissTag),
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = false,
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize())
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.Settled)
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.Settled)
        }
    }

    @Test
    fun swipeDismiss_containsSettledAnchor_dismissAnchorsDisabled() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState =
                rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.StartToEnd)
            LookaheadScope {
                SwipeToDismissBox(
                    state = swipeToDismissBoxState,
                    modifier = Modifier.testTag(swipeDismissTag),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = false,
                    backgroundContent = {},
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        advanceClock()
        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.anchoredDraggableState.anchors.size).isEqualTo(1)
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.Settled)
        }
    }

    /**
     * This test verifies that SwipeToDismiss, which reports anchors derived from its layout size,
     * works in a scenario with a LookaheadScope root, LazyColumn and AnimatedVisibility when the
     * LazyColumn composes and layouts a new SwipeToDismiss item. This is a regression test for
     * b/297226562.
     */
    @Test
    fun swipeToDismiss_reportsAnchors_inNestedLazyAndLookahead() {
        lateinit var lazyState: LazyListState
        lateinit var scope: CoroutineScope
        val amountOfItems = 100
        val composedItems = mutableMapOf<Int, SwipeToDismissBoxState>()

        rule.setContent {
            scope = rememberCoroutineScope()
            LookaheadScope {
                lazyState = rememberLazyListState()
                LazyColumn(state = lazyState) {
                    items(amountOfItems, key = { item -> item }) { index ->
                        composedItems[index] = rememberSwipeToDismissBoxState()
                        val isDismissed =
                            composedItems[index]!!.currentValue == SwipeToDismissBoxValue.EndToStart
                        AnimatedVisibility(visible = !isDismissed) {
                            SwipeToDismissBox(
                                modifier = Modifier.height(48.dp).fillMaxWidth(),
                                state = composedItems[index]!!,
                                backgroundContent = {},
                                content = {},
                            )
                        }
                    }
                }
            }
        }

        // Ensure that we have less visible items than total items, so that we know a new item will
        // be composed and measured/placed
        val initiallyVisibleItems = lazyState.layoutInfo.visibleItemsInfo.size
        assertWithMessage(
                "Expected visible items to be less than total items so that there are " +
                    "items left to compose later."
            )
            .that(initiallyVisibleItems)
            .isLessThan(amountOfItems)
        assertWithMessage("Expected composed items to match amount of visible items")
            .that(composedItems)
            .hasSize(initiallyVisibleItems)
        assertWithMessage(
                "Expected that item at index $initiallyVisibleItems was not " + "composed yet"
            )
            .that(composedItems)
            .doesNotContainKey(initiallyVisibleItems)

        // Dismiss an item so that the lazy layout is required to compose a new item
        scope.launch {
            composedItems[initiallyVisibleItems - 1]!!.dismiss(SwipeToDismissBoxValue.EndToStart)
        }
        rule.waitForIdle()

        // Assert a new item has been
        assertWithMessage(
                "Expected a new item to have been composed at index " +
                    "${initiallyVisibleItems + 1}"
            )
            .that(lazyState.layoutInfo.visibleItemsInfo)
            .hasSize(initiallyVisibleItems + 1)
        val newItemIndex = lazyState.layoutInfo.visibleItemsInfo.size - 1
        val newItem = composedItems[newItemIndex]
        assertThat(newItem).isNotNull()
        assertWithMessage("Expected item $newItemIndex anchors to have been initialized")
            .that(newItem!!.anchoredDraggableState.anchors.size)
            .isAtLeast(1)
    }

    @Test
    fun swipeDismiss_respectsGesturesEnabled() {
        lateinit var swipeToDismissBoxState: SwipeToDismissBoxState
        rule.setContent {
            swipeToDismissBoxState = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled)
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                modifier = Modifier.testTag(swipeDismissTag),
                gesturesEnabled = false,
                backgroundContent = {},
            ) {
                Box(Modifier.fillMaxSize())
            }
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeRight() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.Settled)
        }

        rule.onNodeWithTag(swipeDismissTag).performTouchInput { swipeLeft() }

        advanceClock()

        rule.runOnIdle {
            assertThat(swipeToDismissBoxState.currentValue)
                .isEqualTo(SwipeToDismissBoxValue.Settled)
        }
    }
}
