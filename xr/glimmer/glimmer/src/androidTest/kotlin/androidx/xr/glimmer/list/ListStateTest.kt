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

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.AutoTestFrameClock
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.NoFlingBehavior
import androidx.xr.glimmer.testutils.setContentWithDensity
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ListStateTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun scrollToItemOnState() {
        val state = ListState()
        rule.setGlimmerThemeContent { TestList(state = state) { Text("Item ($it)") } }

        // Make sure the far items are not laid out.
        val targetIndex = Int.MAX_VALUE / 2
        rule.onNodeWithText("Item ($targetIndex)").assertDoesNotExist()

        // Scroll to the far index and check that the item become visible.
        rule.runOnUiThread { runBlocking { state.scrollToItem(targetIndex) } }
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun dispatchRawDeltaOnState() {
        val state = ListState()
        val sizeInDp = 100.dp
        val sizeInPx = with(rule.density) { sizeInDp.toPx() }

        rule.setGlimmerThemeContent {
            TestList(state = state) {
                Text(text = "Item ($it)", modifier = Modifier.size(sizeInDp))
            }
        }

        rule.runOnUiThread { state.dispatchRawDelta(300 * sizeInPx) }
        rule.onNodeWithText("Item (300)").assertIsDisplayed()
    }

    @Test
    fun scrollByOnState() {
        val state = ListState()
        rule.setGlimmerThemeContent {
            TestList(
                state = state,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemContent = { index ->
                    Box(Modifier.size(20.dp).background(Color.Red).testTag("item-tag-$index"))
                },
            )
        }

        // Scroll the first three elements.
        val scrollInPx = with(rule.density) { 50.dp.toPx() }
        rule.runOnUiThread { runBlocking { state.scrollContentBy(scrollInPx) } }

        // Check that the 4th element is almost first in the list.
        val childRect = rule.onNodeWithTag("item-tag-3").getBoundsInRoot()
        val offset: Dp = if (vertical) childRect.top else childRect.left
        offset.assertIsEqualTo(expected = 10.dp, tolerance = 1.dp)
    }

    @Test
    fun saveAndRestoreState() {
        val allowingScope = SaverScope { true }
        val original = ListState(firstVisibleItemIndex = 42, firstVisibleItemScrollOffset = 100)

        val saved = with(ListState.Saver) { allowingScope.save(original) }
        val restored = ListState.Saver.restore(requireNotNull(saved))

        assertThat(restored?.firstVisibleItemIndex).isEqualTo(original.firstVisibleItemIndex)
        assertThat(restored?.firstVisibleItemScrollOffset)
            .isEqualTo(original.firstVisibleItemScrollOffset)
    }

    @Test
    fun respectCanScrollForwardAndCanScrollBackward() {
        val state = ListState(firstVisibleItemIndex = 10)
        rule.setGlimmerThemeContent { TestList(state = state) { Text("Item ($it)") } }
        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isTrue()

        rule.runOnIdle { runBlocking { state.scrollToItem(0) } }

        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isFalse()

        rule.runOnIdle { runBlocking { state.scrollToItem(Int.MAX_VALUE - 1) } }

        assertThat(state.canScrollForward).isFalse()
        assertThat(state.canScrollBackward).isTrue()
    }

    @Test
    fun carriedOverPart_isApplied() {
        val state = ListState()
        rule.setContent {
            TestList(
                state = state,
                modifier = Modifier.size(100.dp),
                itemContent = { index -> FocusableItem(index, Modifier.size(20.dp)) },
            )
        }
        // Set up the value to carry over. We expect it will be used in the next measure pass.
        // The values inside the list state have an opposite sign, so it's negative.
        val scrollToCarryOver = with(rule.density) { -60.dp.toPx() }
        state.applyMeasureResult(
            result = state.layoutInfo as GlimmerListMeasureResult,
            consumedScroll = 0f,
            scrollToCarryOver = scrollToCarryOver,
        )

        // Scroll by a single pixel - we expect that the carried-over value will be added up.
        state.scrollByAndWaitForIdle(1.dp)

        // Check that 4th item is focused.
        rule.onNodeWithTag("item-3").assertIsFocused()
    }

    @Test
    fun scrolling_and_nonScrolling_measurePasses_workTogether_correctly() {
        val state = ListState()
        rule.setContent {
            TestList(
                state = state,
                modifier = Modifier.size(100.dp).background(Color.Black),
                itemContent = { index -> FocusableItem(index, Modifier.size(20.dp)) },
            )
        }

        // Scrolling measure pass (item-2).
        state.scrollByAndWaitForIdle(41.dp)
        rule.onNodeWithTag("item-2").assertIsFocused()

        // Non-scrolling measure pass (item-0).
        runBlocking(Dispatchers.Main + AutoTestFrameClock()) { state.animateScrollToItem(0) }
        rule.waitForIdle()
        rule.onNodeWithTag("item-0").assertIsFocused()

        // Scrolling measure pass (item-1).
        state.scrollByAndWaitForIdle(21.dp)
        rule.onNodeWithTag("item-1").assertIsFocused()
    }

    @Test
    fun initialState_withNonZeroParameters_isAppliedCorrectly() {
        // Set up list with non-trivial state.
        val state = ListState(firstVisibleItemIndex = 50, firstVisibleItemScrollOffset = 42)
        rule.setContentWithDensity(Density(1f)) {
            TestList(
                state = state,
                itemsCount = 100,
                modifier = Modifier.size(400.dp),
                itemContent = { FocusableItem(it, Modifier.size(100.dp)) },
            )
        }

        // Check the auto focus parameters were calculated correctly.
        Truth.assertThat(state.autoFocusState.properties?.focusScroll).isEqualTo(200.0)
        // TODO(b/462040962): Investigate how viewport adjustments reverses contentScroll by
        //  firstVisibleItemScrollOffset when TestList is focused.
        Truth.assertThat(state.autoFocusState.properties?.contentScroll)
            .isEqualTo(if (orientation == Orientation.Vertical) 5042.0 else 5000.0)
    }

    @Test
    fun scrollBy_reportsCorrectConsumedValue() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(
                state = state,
                itemsCount = 100,
                modifier = Modifier.size(400.dp),
                itemContent = { FocusableItem(it, Modifier.size(100.dp)) },
            )
        }

        state.scrollByAndCheckConsumedValue(0.2f)
        state.scrollByAndCheckConsumedValue(0.5f)
        state.scrollByAndCheckConsumedValue(200f)

        state.scrollByAndCheckConsumedValue(-0.2f)
        state.scrollByAndCheckConsumedValue(-0.5f)
        state.scrollByAndCheckConsumedValue(-200f)

        Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        Truth.assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun scrollBy_onTheEdge_doesNotConsumeAllTheDelta() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(
                state = state,
                itemsCount = 100,
                modifier = Modifier.size(400.dp),
                itemContent = { FocusableItem(it, Modifier.size(25.dp)) },
            )
        }

        // Scroll down by 50dp.
        state.scrollByAndCheckConsumedValue(delta = 50f, consumed = 50f)
        // Scroll up by 100dp when only 50dp is available. List should consume only 50dp of it.
        state.scrollByAndCheckConsumedValue(delta = -100f, consumed = -50f)
    }

    @Test
    fun tinyValues_thatNotConsumed_areCarriedOverForSuccessiveScrolls_ifFocusRemainsWithinTheSameItem() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(
                state = state,
                itemsCount = 100,
                modifier = Modifier.size(500.dp),
                flingBehavior = NoFlingBehavior,
                itemContent = { FocusableItem(it, Modifier.size(40.dp)) },
            )
        }

        // Total scroll is 0.5dp * 70 = 35dp. The size of item is 40dp.
        repeat(70) { state.scrollByAndWaitForIdle(delta = 0.5f) }

        assertThat(state.totalScroll).isEqualTo(35)
    }

    @Test
    fun tinyValues_thatNotConsumed_areCarriedOverForSuccessiveScrolls_ifFocusMovesToTheNextItem() {
        val state = ListState()
        rule.setContentWithDensity(Density(1f)) {
            TestList(
                state = state,
                itemsCount = 100,
                modifier = Modifier.size(500.dp),
                flingBehavior = NoFlingBehavior,
                itemContent = { FocusableItem(it, Modifier.size(40.dp)) },
            )
        }

        // Total scroll is 0.5dp * 100 = 50dp. The size of item is 50dp.
        repeat(100) { state.scrollByAndWaitForIdle(delta = 0.5f) }

        assertThat(state.totalScroll).isEqualTo(50)
    }

    @Test
    fun isScrollInProgress_reflectsCorrectState() = runTest {
        val state = ListState()
        rule.setContent { TestList(state = state) { FocusableItem(it) } }

        assertThat(state.isScrollInProgress).isFalse()
        withContext(Dispatchers.Main) {
            state.scroll(MutatePriority.Default) { assertThat(state.isScrollInProgress).isTrue() }
        }
        assertThat(state.isScrollInProgress).isFalse()
    }

    private fun ListState.scrollByAndCheckConsumedValue(delta: Float, consumed: Float = delta) {
        val actualConsumed = scrollByAndWaitForIdle(delta)
        Truth.assertThat(actualConsumed).isWithin(1f).of(consumed)
        rule.waitForIdle()
    }

    /** Calculates the total consumed scroll of the list, including the carried-over part. */
    private val ListState.totalScroll: Float
        // Autofocus properties and scroll have opposite signs for the same direction.
        get() = requireNotNull(autoFocusState.properties).userScroll.toFloat() - carryOverScroll

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
