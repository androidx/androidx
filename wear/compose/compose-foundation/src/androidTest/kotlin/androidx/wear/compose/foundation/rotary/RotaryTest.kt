/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice.SOURCE_ROTARY_ENCODER
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.RotaryInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewConfigurationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`

// TODO(b/278705775): Add more tests to check Rotary Snap behavior
@OptIn(ExperimentalTestApi::class)
class RotaryScrollTest {
    @get:Rule val rule = createComposeRule()

    private var itemSizePx: Float = 50f
    private var itemSizeDp: Dp = Dp.Infinity
    private val itemsCount = 300

    private val focusRequester = FocusRequester()
    private lateinit var state: LazyListState

    @Before
    fun before() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun scroll_by_one_item() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = { rotateToScrollVertically(itemSizePx) },
        )

        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 1) }
    }

    @Test
    fun no_fling_with_filtered_negative_values_high_res() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(-1f)
                advanceEventTime(20)
                rotateToScrollVertically(-1f)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(2f)
                advanceEventTime(20)
            },
        )

        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 2) }
    }

    @Test
    fun no_filtered_negative_values_low_res() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // Quickly scroll up and down - we should scroll only by 1 item forward
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(50)
            },
            lowRes = true,
        )

        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 1) }
    }

    @Test
    fun slow_scroll_by_two_items() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(300)
                rotateToScrollVertically(itemSizePx)
            },
        )

        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 2) }
    }

    @Test
    fun fast_scroll_with_reverse() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // Scroll forwards by 2 items
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                // Instantly scroll backwards by 2 items
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(10)
                rotateToScrollVertically(-itemSizePx)
            },
        )

        rule.runOnIdle {
            // Check that we're on the same position
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex)
        }
    }

    @Test
    fun fast_scroll_with_fling() {
        var itemIndex = 0

        Assume.assumeTrue(hasRotaryInputDevice())
        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // To produce fling we need to send 3 events,
                // which will be increasing the scroll velocity.
                // First event initializes velocityTracker
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(10)
                // Next 2 events should increase the scroll velocity.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(10)
                rotateToScrollVertically(itemSizePx * 6)
            },
        )

        rule.runOnIdle {
            // We check that we indeed scrolled the list further than
            // amount of pixels which we scrolled by.
            Truth.assertThat(state.firstVisibleItemIndex).isGreaterThan(itemIndex + 12)
        }
    }

    @Test
    fun fading_scroll_without_fling_high_res() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // Fling will not be produced when scroll velocity decreases with each event
                // By decreasing the distance with each event we're
                // sure that the velocity also decreases.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 4)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 3)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx * 2)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
            },
        )

        rule.runOnIdle {
            // We check that we scrolled exactly 15 items, not more as it would be with a fling.
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 15)
        }
    }

    @Test
    fun fading_scroll_without_fling_low_res() {
        var itemIndex = 0

        testScroll(
            beforeScroll = { itemIndex = state.firstVisibleItemIndex },
            rotaryAction = {
                // Fling will not be produced when scroll velocity decreases with each event
                // By decreasing the distance with each event we're
                // sure that the velocity also decreases.
                rotateToScrollVertically(itemSizePx * 5)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx * 4)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx * 3)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx * 2)
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
            },
            lowRes = true,
        )

        rule.runOnIdle {
            // We check that we scrolled exactly 15 items, not more as it would be with a fling.
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(itemIndex + 15)
        }
    }

    @Test
    fun overscroll_is_triggered_when_scrolled_to_the_top_edge() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling towards the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            // Initially overscroll is reversed to the direction of the scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, 2 * itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(2 * itemSizePx)
        }
    }

    @Test
    fun overscroll_is_triggered_when_scrolled_to_the_bottom_edge() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            initialItem = itemsCount - 1,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling towards the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            // Initially overscroll is reversed to the direction of the scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, -2 * itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(-2 * itemSizePx)
        }
    }

    @Test
    fun overscroll_is_not_triggered_when_scrolled_off_the_top_edge() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.overscrollDeltaReceived).isEqualTo(Offset.Zero)
            assertThat(consumedNestedScroll).isEqualTo(0f)
        }
    }

    @Test
    fun overscroll_is_not_triggered_when_scrolled_off_the_bottom_edge() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.overscrollDeltaReceived).isEqualTo(Offset.Zero)
            assertThat(consumedNestedScroll).isEqualTo(0f)
        }
    }

    @Test
    fun flinged_to_the_top_edge() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(-itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling in the opposite direction of scroll
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun flinged_to_the_bottom_edge() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling in the opposite direction of scroll
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun flinged_off_the_edge() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
            },
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun overscroll_is_triggered_when_scrolled_to_the_top_edge_reversed() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling towards the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            // Initially overscroll is reversed to the direction of the scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, 2 * itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(2 * itemSizePx)
        }
    }

    @Test
    fun overscroll_is_triggered_when_scrolled_to_the_bottom_edge_reversed() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling towards the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            // Initially overscroll is reversed to the direction of the scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, -2 * itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(-2 * itemSizePx)
        }
    }

    @Test
    fun overscroll_is_not_triggered_when_scrolled_off_the_top_edge_reversed() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.overscrollDeltaReceived).isEqualTo(Offset.Zero)
            assertThat(consumedNestedScroll).isEqualTo(0f)
        }
    }

    @Test
    fun overscroll_is_not_triggered_when_scrolled_off_the_bottom_edge_reversed() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Keep track of the received scroll but don't consume it
                consumedNestedScroll += it
                0f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.overscrollDeltaReceived).isEqualTo(Offset.Zero)
            assertThat(consumedNestedScroll).isEqualTo(0f)
        }
    }

    @Test
    fun flinged_to_the_top_edge_reversed() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(-itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling in the opposite direction of scroll
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun flinged_to_the_bottom_edge_reversed() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll shouldn't be consumed as we're scrolling in the opposite direction of scroll
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(false)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun flinged_off_the_edge_reversed() {
        Assume.assumeTrue(hasRotaryInputDevice())

        val overscrollController = OffsetOverscrollEffectCounter()

        testOverscroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            rotaryAction = {
                rotateToScrollVertically(itemSizePx)
                advanceEventTime(20)
                rotateToScrollVertically(itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Scroll should be consumed as we're scrolling from the edge
            assertThat(overscrollController.scrollWasConsumed).isEqualTo(true)
            assertThat(overscrollController.flinged).isEqualTo(true)
            assertThat(overscrollController.applyToScrollCount).isGreaterThan(0)
            assertThat(overscrollController.applyToFlingCount).isGreaterThan(0)
        }
    }

    @Test
    fun nested_scroll_consumes_half_when_scrolled_to_the_top_edge() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Consume half of the scroll
                consumedNestedScroll += it * 0.5f
                it * 0.5f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
        )

        rule.runOnIdle {
            // Half of the scroll should be consumed by the overscroll and another half by the
            // nested scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(itemSizePx)
        }
    }

    @Test
    fun nested_scroll_consumes_half_when_scrolled_to_the_top_edge_reversed() {
        val overscrollController = OffsetOverscrollEffectCounter()
        var consumedNestedScroll = 0f

        testOverscrollWithNestedScroll(
            initialItem = itemsCount - 1,
            overscrollEffect = overscrollController,
            consumedNestedScroll = {
                // Consume half of the scroll
                consumedNestedScroll += it * 0.5f
                it * 0.5f
            },
            rotaryAction = {
                rotateToScrollVertically(-itemSizePx)
                // Keeping delay larger than a fling threshold
                advanceEventTime(50)
                rotateToScrollVertically(-itemSizePx)
            },
            reverseDirection = true,
        )

        rule.runOnIdle {
            // Half of the scroll should be consumed by the overscroll and another half by the
            // nested scroll
            assertThat(overscrollController.overscrollDeltaReceived)
                .isEqualTo(Offset(0f, itemSizePx))
            assertThat(consumedNestedScroll).isEqualTo(itemSizePx)
        }
    }

    @Test
    fun snap_with_empty_SLC() {
        rule.setContent {
            state = rememberLazyListState()

            MockRotaryResolution() {
                val state = rememberScalingLazyListState()
                ScalingLazyColumn(
                    modifier =
                        Modifier.size(200.dp)
                            .testTag(TEST_TAG)
                            .rotaryScrollable(
                                RotaryScrollableDefaults.snapBehavior(state),
                                focusRequester,
                            ),
                    state = state,
                ) {}
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(itemSizePx)
            advanceEventTime(20)
            rotateToScrollVertically(-itemSizePx)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun testScroll(
        beforeScroll: () -> Unit,
        rotaryAction: RotaryInjectionScope.() -> Unit,
        reverseDirection: Boolean = false,
        lowRes: Boolean = false,
    ) {
        rule.setContent {
            state = rememberLazyListState()

            MockRotaryResolution(lowRes = lowRes) {
                DefaultLazyColumnItemsWithRotary(
                    itemSize = itemSizeDp,
                    overscrollEffect = null,
                    focusRequester = focusRequester,
                    behavior = RotaryScrollableDefaults.behavior(state),
                    scrollableState = state,
                    reverseDirection = reverseDirection,
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        beforeScroll()
        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotaryAction() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun testOverscroll(
        overscrollEffect: OverscrollEffect,
        rotaryAction: RotaryInjectionScope.() -> Unit,
        initialItem: Int = 0,
        reverseDirection: Boolean = false,
        lowRes: Boolean = false,
    ) {
        rule.setContent {
            state = rememberLazyListState(initialFirstVisibleItemIndex = initialItem)

            MockRotaryResolution(lowRes = lowRes) {
                DefaultLazyColumnItemsWithRotary(
                    itemSize = itemSizeDp,
                    overscrollEffect = overscrollEffect,
                    focusRequester = focusRequester,
                    behavior = RotaryScrollableDefaults.behavior(state),
                    scrollableState = state,
                    reverseDirection = reverseDirection,
                )
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotaryAction() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun testOverscrollWithNestedScroll(
        overscrollEffect: OverscrollEffect,
        rotaryAction: RotaryInjectionScope.() -> Unit,
        consumedNestedScroll: (Float) -> Float = { it },
        initialItem: Int = 0,
        reverseDirection: Boolean = false,
        lowRes: Boolean = false,
    ) {
        val scrollable = ScrollableState(consumedNestedScroll)
        rule.setContent {
            state = rememberLazyListState(initialFirstVisibleItemIndex = initialItem)

            MockRotaryResolution(lowRes = lowRes) {
                Box(Modifier.scrollable(orientation = Orientation.Vertical, state = scrollable)) {
                    DefaultLazyColumnItemsWithRotary(
                        itemSize = itemSizeDp,
                        overscrollEffect = overscrollEffect,
                        focusRequester = focusRequester,
                        behavior = RotaryScrollableDefaults.behavior(state),
                        scrollableState = state,
                        reverseDirection = reverseDirection,
                    )
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.onNodeWithTag(TEST_TAG).performRotaryScrollInput { rotaryAction() }
    }

    @Composable
    private fun DefaultLazyColumnItemsWithRotary(
        itemSize: Dp,
        reverseDirection: Boolean,
        overscrollEffect: OverscrollEffect?,
        focusRequester: FocusRequester,
        behavior: RotaryScrollableBehavior,
        scrollableState: LazyListState,
    ) {
        LazyColumn(
            modifier =
                Modifier.size(200.dp)
                    .testTag(TEST_TAG)
                    .rotaryScrollable(behavior, focusRequester, reverseDirection, overscrollEffect),
            state = scrollableState,
            reverseLayout = reverseDirection,
        ) {
            items(itemsCount) {
                BasicText(modifier = Modifier.height(itemSize), text = "Item #$it")
            }
        }
    }

    private fun hasRotaryInputDevice(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewConfiguration = ViewConfiguration.get(context)
        with(context.getSystemService(Context.INPUT_SERVICE) as InputManager) {
            inputDeviceIds.forEach { deviceId ->
                // To validate that we have a valid rotary device we need to:
                // 1) check that we have a rotary device.
                // 2) check that getScaledMaximumFlingVelocity method returns us a valid fling speed
                if (
                    getInputDevice(deviceId)?.motionRanges?.find {
                        it.source == SOURCE_ROTARY_ENCODER
                    } != null &&
                        ViewConfigurationCompat.getScaledMaximumFlingVelocity(
                            context,
                            viewConfiguration,
                            deviceId,
                            MotionEvent.AXIS_SCROLL,
                            SOURCE_ROTARY_ENCODER,
                        ) != Integer.MIN_VALUE
                )
                    return true
            }
        }
        return false
    }

    companion object {
        const val TEST_TAG = "test-tag"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MockRotaryResolution(lowRes: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current

    // Mocking low-res flag
    val mockContext = spy(context)
    val mockPackageManager = spy(context.packageManager)
    `when`(mockPackageManager.hasSystemFeature("android.hardware.rotaryencoder.lowres"))
        .thenReturn(lowRes)

    doReturn(mockPackageManager).`when`(mockContext).packageManager

    CompositionLocalProvider(LocalContext provides mockContext) { content() }
}

// Custom offset overscroll that only counts the number of times each callback is triggered and
// tracks whether fling or scroll was consumed.
private class OffsetOverscrollEffectCounter : OverscrollEffect {
    var applyToScrollCount: Int = 0
        private set

    var applyToFlingCount: Int = 0
        private set

    var scrollWasConsumed: Boolean = false
        private set

    var flinged: Boolean = false
        private set

    var overscrollDeltaReceived: Offset = Offset.Zero
        private set

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val consumedScroll = performScroll(delta)
        if (consumedScroll.x != 0.0f || consumedScroll.y != 0.0f) {
            scrollWasConsumed = true
        }
        overscrollDeltaReceived += delta - consumedScroll
        applyToScrollCount++
        return Offset(0.0f, 0.0f)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val consumedVelocity = performFling(velocity)
        if (consumedVelocity.x != 0.0f || consumedVelocity.y != 0.0f) {
            flinged = true
        }
        applyToFlingCount++
    }

    override val isInProgress: Boolean = false
    @Deprecated("Deprecated", level = DeprecationLevel.ERROR)
    override val effectModifier: Modifier = Modifier
}
