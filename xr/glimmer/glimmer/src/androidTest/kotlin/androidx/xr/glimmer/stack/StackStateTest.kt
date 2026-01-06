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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.createGlimmerRule
import androidx.xr.glimmer.performIndirectSwipe
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class StackStateTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun initialState_propertiesReturnDefaultValues() {
        val state = StackState()

        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)
        assertThat(state.isScrollInProgress).isFalse()
        assertThat(state.canScrollForward).isFalse()
        assertThat(state.canScrollBackward).isFalse()
        assertThat(state.lastScrolledForward).isFalse()
        assertThat(state.lastScrolledBackward).isFalse()
        assertThat(state.layoutInfoInternal.viewportSize).isEqualTo(IntSize.Zero)
    }

    @Test
    fun negativeInitialTopItem_throws() {
        assertThrows(IllegalArgumentException::class.java) { StackState(initialTopItem = -1) }
    }

    @Test
    fun dispatchRawDelta_whenNotAttachedToStack_returnsZeroConsumedDelta() {
        val state = StackState()

        val consumed = state.dispatchRawDelta(100f)

        assertThat(consumed).isEqualTo(0f)
    }

    @Test
    fun scrollToItem_whenNotAttachedToStack_completesWithoutError() = runTest {
        val state = StackState()

        runOnUiThread { state.scrollToItem(5) }
        // Test succeeds if no exception is thrown.
    }

    @Test
    fun animateScrollToItem_whenNotAttachedToStack_completesWithoutError() = runTest {
        val state = StackState()

        state.animateScrollToItem(5)
        // Test succeeds if no exception is thrown.
    }

    @Test
    fun scroll_whenNotAttachedToStack_completesWithoutErrorAndDoesNotExecuteBlock() = runTest {
        val state = StackState()

        var blockExecuted = false
        runOnUiThread {
            state.scroll(MutatePriority.Default) {
                blockExecuted = true
                scrollBy(100f)
            }
        }

        assertThat(blockExecuted).isFalse()
    }

    @Test
    fun topItem_scrollForward_updatesToNextWhenTopItemLeavesViewport() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(itemHeight * 0.1f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(itemHeight * 0.4f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(itemHeight * 0.4f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(itemHeight * 0.1f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(1) }
    }

    @Test
    fun topItem_scrollBackward_updatesToPreviousWhenPreviousItemEntersViewport() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(1) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(1) }

        rule.runOnUiThread { state.dispatchRawDelta(-itemHeight * 0.1f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(-itemHeight * 0.4f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(-itemHeight * 0.4f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }

        rule.runOnUiThread { state.dispatchRawDelta(-itemHeight * 0.1f) }
        rule.runOnIdle { assertThat(state.topItem).isEqualTo(0) }
    }

    @Test
    fun scrollToItem_displaysTargetItem() = runTest {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        runOnUiThread { state.scrollToItem(3) }

        rule.onNodeWithTag("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun animateScrollToItem_displaysTargetItem() {
        lateinit var scope: CoroutineScope
        val state = StackState()
        rule.setContent {
            scope = rememberCoroutineScope()
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }

        scope.launch { state.animateScrollToItem(3) }

        rule.onNodeWithTag("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
    }

    @Test
    fun scroll_forwardThenBackward_displaysNextThenPreviousItem() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        runOnUiThread { state.scroll(MutatePriority.Default) { scrollBy(itemHeight.toFloat()) } }
        rule.onNodeWithTag("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(1)

        runOnUiThread { state.scroll(MutatePriority.Default) { scrollBy(-itemHeight.toFloat()) } }
        rule.onNodeWithTag("Item 0").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
    }

    @Test
    fun dispatchRawDelta_forwardWithinSameItem_updatesOffsetFraction() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }

        rule.runOnIdle { state.dispatchRawDelta(itemHeight * 0.1f) }

        rule.onNodeWithTag("Item 0").assertIsDisplayed()
        rule.onNodeWithTag("Item 1").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(0)
        assertThat(state.topItemOffsetFraction).isWithin(0.01f).of(0.1f)
    }

    @Test
    fun dispatchRawDelta_backwardWithinSameItem_updatesOffsetFraction() = runTest {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        runOnUiThread { state.scrollToItem(3) }
        rule.onNodeWithTag("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(3)
        assertThat(state.topItemOffsetFraction).isEqualTo(0f)

        rule.runOnIdle { state.dispatchRawDelta(-(itemHeight * 0.1f)) }

        rule.onNodeWithTag("Item 2").assertIsDisplayed()
        rule.onNodeWithTag("Item 3").assertIsDisplayed()
        assertThat(state.topItem).isEqualTo(2)
        assertThat(state.topItemOffsetFraction).isWithin(0.01f).of(0.9f)
    }

    @Test
    fun isScrollInProgress_isTrueDuringSwipe() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.mainClock.autoAdvance = false

        performIndirectSwipe(itemHeight)

        assertThat(state.isScrollInProgress).isTrue()

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(state.isScrollInProgress).isFalse()
        assertThat(state.topItem).isEqualTo(1)
    }

    @Test
    fun canScrollForwardAndBackward_areCorrectAtAndBetweenBoundaries() = runTest {
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) { items(5) { index -> StackItem("Item $index") } }
        }
        rule.waitForIdle()

        // At the beginning
        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isFalse()

        runOnUiThread { state.scrollToItem(2) }

        rule.waitForIdle()

        // In the middle
        assertThat(state.canScrollForward).isTrue()
        assertThat(state.canScrollBackward).isTrue()

        runOnUiThread { state.scrollToItem(4) }

        rule.waitForIdle()

        // At the end
        assertThat(state.canScrollForward).isFalse()
        assertThat(state.canScrollBackward).isTrue()
    }

    @Test
    fun lastScrolledForwardAndBackward_areUpdatedAfterSwipe() {
        var itemHeight = 0
        val state = StackState()
        rule.setContent {
            VerticalStack(state = state) {
                items(5) { index -> StackItem("Item $index") { itemHeight = it } }
            }
        }
        rule.waitForIdle()

        assertThat(state.lastScrolledBackward).isFalse()
        assertThat(state.lastScrolledForward).isFalse()

        performIndirectSwipe(itemHeight)
        rule.waitForIdle()

        assertThat(state.lastScrolledBackward).isFalse()
        assertThat(state.lastScrolledForward).isTrue()

        performIndirectSwipe(-itemHeight)
        rule.waitForIdle()

        assertThat(state.lastScrolledBackward).isTrue()
        assertThat(state.lastScrolledForward).isFalse()
    }

    @Test
    fun saveAndRestoreState_restoresTopItem() {
        val allowingScope = SaverScope { true }
        val original = StackState(initialTopItem = 5)

        val saved = with(StackState.Saver) { allowingScope.save(original) }!!
        val restored = StackState.Saver.restore(saved)!!

        assertThat(restored.topItem).isEqualTo(5)
    }

    @Test
    fun layoutInfo_viewportSizeIsCorrect() {
        val state = StackState()
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index") }
            }
        }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(state.layoutInfoInternal.viewportSize)
                .isEqualTo(IntSize(width = 100.dp.roundToPx(), height = 100.dp.roundToPx()))
        }
    }

    @Test
    fun scrollForward_updatesMeasuredItemHeights() = runTest {
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        val itemHeights = listOf(10.dp, 20.dp, 30.dp, 40.dp, 50.dp)
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index", Modifier.height(itemHeights[index])) }
            }
        }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }

        runOnUiThread { state.scrollToItem(2) }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(40.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(50.dp.roundToPx())
        }
    }

    @Test
    fun scrollForwardInProgress_updatesMeasuredItemHeights() {
        lateinit var scope: CoroutineScope
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        val itemHeights = listOf(10.dp, 20.dp, 30.dp, 40.dp, 50.dp)
        rule.setContent {
            scope = rememberCoroutineScope()
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index", Modifier.height(itemHeights[index])) }
            }
        }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }
        rule.mainClock.autoAdvance = false
        scope.launch { state.animateScrollToItem(1, animationSpec = tween(durationMillis = 500)) }

        rule.mainClock.advanceTimeBy(100) // Advance the clock partway through the animation

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(40.dp.roundToPx())
        }
    }

    @Test
    fun scrollBackward_updatesMeasuredItemHeights() = runTest {
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        val itemHeights =
            listOf(10.dp, 20.dp, 30.dp, 40.dp, 50.dp, 60.dp, 70.dp, 80.dp, 90.dp, 100.dp)
        rule.setContent {
            VerticalStack(modifier = Modifier.size(200.dp), state = state) {
                items(10) { index -> StackItem("Item $index", Modifier.height(itemHeights[index])) }
            }
        }
        runOnUiThread { state.scrollToItem(8) }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(90.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(100.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(0)
        }

        runOnUiThread { state.scrollToItem(4) }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(50.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(60.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(70.dp.roundToPx())
        }

        runOnUiThread { state.scrollToItem(1) }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(40.dp.roundToPx())
        }
    }

    @Test
    fun scrollBackwardInProgress_updatesMeasuredItemHeights() = runTest {
        lateinit var scope: CoroutineScope
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        val itemHeights = listOf(10.dp, 20.dp, 30.dp, 40.dp, 50.dp)
        rule.setContent {
            scope = rememberCoroutineScope()
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(5) { index -> StackItem("Item $index", Modifier.height(itemHeights[index])) }
            }
        }
        runOnUiThread { state.scrollToItem(2) }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(40.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(50.dp.roundToPx())
        }
        rule.mainClock.autoAdvance = false
        scope.launch { state.animateScrollToItem(1, animationSpec = tween(durationMillis = 500)) }

        rule.mainClock.advanceTimeBy(100) // Advance the clock partway through the animation

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(40.dp.roundToPx())
        }

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(40.dp.roundToPx())
        }
    }

    @Test
    fun itemHeightsChange_updatesMeasuredItemHeights() {
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        var itemHeights by mutableStateOf(listOf(10.dp, 20.dp, 30.dp))
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp), state = state) {
                items(3) { index -> StackItem("Item $index", Modifier.height(itemHeights[index])) }
            }
        }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }

        rule.runOnIdle { itemHeights = listOf(30.dp, 20.dp, 10.dp) }

        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(10.dp.roundToPx())
        }
    }

    @Test
    fun itemInserted_withKeys_updatesMeasuredItemHeights() {
        data class TestItem(val key: Int, val height: Dp)
        val items = mutableStateListOf(TestItem(0, 10.dp), TestItem(2, 30.dp))
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp).testTag("stack"), state = state) {
                items(items, key = { it.key }) { item ->
                    StackItem("Item ${item.key}", Modifier.height(item.height))
                }
            }
        }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(0)
        }

        rule.runOnIdle { items.add(1, TestItem(1, 20.dp)) }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }
    }

    @Test
    fun itemRemoved_withKeys_updatesMeasuredItemHeights() {
        data class TestItem(val key: Int, val height: Dp)
        val items = mutableStateListOf(TestItem(0, 10.dp), TestItem(1, 20.dp), TestItem(2, 30.dp))
        val state = StackState()
        val layoutInfo = state.layoutInfoInternal
        rule.setContent {
            VerticalStack(modifier = Modifier.size(100.dp).testTag("stack"), state = state) {
                items(items, key = { it.key }) { item ->
                    StackItem("Item ${item.key}", Modifier.height(item.height))
                }
            }
        }
        rule.waitForIdle()
        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(20.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(30.dp.roundToPx())
        }

        rule.runOnIdle { items.removeAt(1) }
        rule.waitForIdle()

        with(rule.density) {
            assertThat(layoutInfo.measuredTopItemHeight).isEqualTo(10.dp.roundToPx())
            assertThat(layoutInfo.measuredNextItemHeight).isEqualTo(30.dp.roundToPx())
            assertThat(layoutInfo.measuredNextNextItemHeight).isEqualTo(0)
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
                .focusTarget()
                .testTag(text)
        ) {
            Text(text)
        }
    }

    private fun performIndirectSwipe(distancePx: Int) {
        rule.onRoot().performIndirectSwipe(rule, distancePx.toFloat())
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }
}
