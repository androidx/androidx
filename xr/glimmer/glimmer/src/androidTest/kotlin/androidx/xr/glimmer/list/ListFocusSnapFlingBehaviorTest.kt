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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.testutils.NoFlingBehavior
import androidx.xr.glimmer.testutils.setContentWithDensity
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Ignore
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(Parameterized::class)
class ListFocusSnapFlingBehaviorTest(orientation: Orientation) :
    BaseListTestWithOrientation(orientation) {

    @Test
    fun focusLineSnapsToTheCenter_ifItIsAfterCenter() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(state = state) { FocusableItem(it, Modifier.size(50.dp)) }
        }

        val targetItem = rule.onNodeWithTag("item-4")
        // Freeze the clock to prevent snapping animation from starting prematurely.
        rule.mainClock.withFrozenTime {
            // Slowly scroll the list to bring item-4 into focus,
            // ensuring the focus line is positioned before the center of the item.
            rule
                .onNodeWithTag(LIST_TEST_TAG)
                .performIndirectSwipe(rule, 205f, moveDuration = 20_000L)
            // Allow the scroll to finish, but prevent snapping animation.
            advanceTimeByFrame()
            // Verify the item is focused and the focus line is before the center.
            targetItem.assertIsFocused()
            assertThat(state.focusLinePosition).isLessThan(targetItem.boundsCenterInRoot)
        }

        // Let the list settle, allowing the focus line to snap to the center.
        rule.waitForIdle()

        // Confirm the focus line has snapped to the center.
        assertThat(state.focusLinePosition).isEqualTo(targetItem.boundsCenterInRoot)
    }

    @Test
    fun focusLineSnapsToTheCenter_ifItIsBeforeCenter() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(state = state) { FocusableItem(it, Modifier.size(50.dp)) }
        }

        val targetItem = rule.onNodeWithTag("item-3")
        // Freeze the clock to prevent snapping animation from starting prematurely.
        rule.mainClock.withFrozenTime {
            // Slowly scroll the list to bring item-3 into focus,
            // ensuring the focus line is positioned after the center of the item.
            rule
                .onNodeWithTag(LIST_TEST_TAG)
                .performIndirectSwipe(rule, 195f, moveDuration = 20_000L)
            // Allow the scroll to finish, but prevent snapping animation.
            advanceTimeByFrame()
            // Verify the item is focused and the focus line is after the center.
            targetItem.assertIsFocused()
            assertThat(state.focusLinePosition).isGreaterThan(targetItem.boundsCenterInRoot)
        }

        // Let the list settle, allowing the focus line to snap to the center.
        rule.waitForIdle()

        // Confirm the focus line has snapped to the center.
        assertThat(state.focusLinePosition).isEqualTo(targetItem.boundsCenterInRoot)
    }

    @Test
    @Ignore("b/458681644") // Fling gesture results in the wrong direction velocity.
    fun focusLine_flingsBeyondTheItem_whereGestureFinished() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(state = state, modifier = Modifier.mainAxisSize(300.dp)) {
                FocusableItem(it, Modifier.size(50.dp))
            }
        }

        // Freeze the clock to prevent fling animation from starting prematurely.
        rule.mainClock.withFrozenTime {
            // The initial scroll distance is set such that focus would normally land on item-3
            // without a fling, but the fling behavior should carry the focus line further.
            // Perform a quick, indirect swipe to initiate a fling.
            rule.onNodeWithTag(LIST_TEST_TAG).performIndirectSwipe(rule, 155f, 40L)
            // Allow the scroll to occur, but prevent approach + snapping animations.
            advanceTimeByFrame()
            // Check that the focus line is within item-3 bounds immediately after the gesture ends.
            rule.onNodeWithTag("item-3").assertIsFocused()
        }

        // Allow the fling animation to complete, which should move the focus further.
        rule.waitForIdle()

        // After the fling finishes, confirm that the focus line
        // moved beyond item-3 and landed on item-6.
        rule.onNodeWithTag("item-6").assertIsFocused()
    }

    @Test
    fun noFlingBehavior_doesNotMoveFocusLine() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(state = state, flingBehavior = NoFlingBehavior) {
                FocusableItem(it, Modifier.size(50.dp))
            }
        }

        var focusLinePosition = -1f
        // Freeze the clock to prevent snapping or fling animations from starting prematurely.
        rule.mainClock.withFrozenTime {
            // Scroll the list to focus item-4,
            // but ensure the focus line is not centered within the item.
            rule
                .onNodeWithTag(LIST_TEST_TAG)
                .performIndirectSwipe(rule, 215f, moveDuration = 20_000L)

            // Allow the scroll to occur, but prevent approach + snapping animations.
            advanceTimeByFrame()

            // Verify the item is focused, but the focus line is not centered.
            val focusedItem = rule.onNodeWithTag("item-4")
            focusedItem.assertIsFocused()
            assertThat(state.focusLinePosition).isNotEqualTo(focusedItem.boundsCenterInRoot)

            // Remember the focus line's position.
            focusLinePosition = state.focusLinePosition
        }

        // Let the list settle to ensure there are no pending animations.
        rule.waitForIdle()

        // Confirm that the focus line did not change its position.
        assertThat(state.focusLinePosition).isEqualTo(focusLinePosition)
    }

    private fun MainTestClock.withFrozenTime(action: MainTestClock.() -> Unit) {
        try {
            autoAdvance = false
            action()
        } finally {
            autoAdvance = true
        }
    }

    private val SemanticsNodeInteraction.boundsCenterInRoot: Float
        get() {
            val bounds = getUnclippedBoundsInRoot()
            return with(rule.density) {
                if (orientation == Orientation.Vertical) {
                    (bounds.top + bounds.bottom).toPx() / 2
                } else {
                    (bounds.left + bounds.right).toPx() / 2
                }
            }
        }

    private val ListState.focusLinePosition: Float
        get() {
            val focusScroll = requireNotNull(autoFocusState.properties).focusScroll.toFloat()
            val startPadding = layoutInfo.beforeContentPadding
            return startPadding + focusScroll
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
