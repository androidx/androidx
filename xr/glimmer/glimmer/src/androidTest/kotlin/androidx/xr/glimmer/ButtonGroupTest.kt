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

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.inputDeviceCenter
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * [ButtonGroup] tests that should behave identically in each [LayoutDirection].
 *
 * Scroll values and item indices are relative to the start of the content, so they are the same in
 * every layout direction. Only physical positions, and the physical direction of scroll gestures,
 * are mirrored in RTL.
 *
 * Tests that compare layout directions against each other are in [ButtonGroupRtlTest].
 */
@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ButtonGroupTest(private val layoutDirection: LayoutDirection) {
    @get:Rule(order = 0)
    val rule = createComposeRule(ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(order = 1) val glimmerRule = createGlimmerRule()

    private val isRtl: Boolean
        get() = layoutDirection == LayoutDirection.Rtl

    private lateinit var focusManager: FocusManager

    @Test
    fun testSemanticsValues() {
        val button1Width = 100.dp
        val button2Width = 150.dp
        val spacingWidth = 8.dp

        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.testTag("buttonGroup").width(50.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(spacingWidth),
            ) {
                Button(modifier = Modifier.width(button1Width), onClick = {}) {}
                Button(modifier = Modifier.width(button2Width), onClick = {}) {}
            }
        }
        rule
            .onNodeWithTag("buttonGroup")
            .assert(expectValue(SemanticsProperties.IsTraversalGroup, true))
            .assertHorizontalScrollAxisRange(
                expectedValue = 50.dp,
                expectedMaxValue = button1Width + button2Width + spacingWidth,
            )
    }

    @Test
    fun testSemanticsScrollToValidIndex_updatesScrollValue() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp).height(100.dp).testTag("buttonGroup"),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // 500dp ButtonGroup containing 10x100dp buttons will have to scroll
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        rule.onNodeWithTag("buttonGroup").performSemanticsAction(SemanticsActions.ScrollToIndex) {
            assertThat(it(2)).isTrue()
        }
        rule.waitForIdle()
        // We scrolled to the center of index 2 (2*100dp + 50dp for the center of this item)
        with(ButtonGroupTestDensity) { state.userScroll.toDp() }
            .assertIsEqualTo(250.dp, "userScroll after ScrollToIndex(2)")
        assertThat(state.currentItemIndex).isEqualTo(2)
    }

    @Test
    fun testSemanticsScrollToInvalidIndex_updatesScrollValue() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp).height(100.dp).testTag("buttonGroup"),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // 500dp ButtonGroup containing 10x100dp buttons will have to scroll
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        rule.onNodeWithTag("buttonGroup").performSemanticsAction(SemanticsActions.ScrollToIndex) {
            assertThat(it(20)).isFalse()
        }
        rule.waitForIdle()
        // We stayed at item 0; the center of item 0 is at 50dp
        with(ButtonGroupTestDensity) { state.userScroll.toDp() }
            .assertIsEqualTo(50.dp, "userScroll after failed ScrollToIndex(20)")
        assertThat(state.currentItemIndex).isEqualTo(0)
    }

    @Test
    fun stateScroll_initializedOnLayout() {
        val initialItemIndex = 1
        val buttonWidth = 300.dp
        val buttonPaddingWidth = 8.dp

        val state = ButtonGroupState(initialItemIndex = initialItemIndex)
        setContent {
            ButtonGroup(state = state) {
                repeat(5) { i ->
                    Button(modifier = Modifier.width(buttonWidth), onClick = {}) { Text("$i") }
                }
            }
        }
        val expectedInitialScroll =
            ((buttonWidth + buttonPaddingWidth) * initialItemIndex) + (buttonWidth / 2)
        rule.runOnIdle {
            assertThat(state.currentItemIndex).isEqualTo(1)
            with(ButtonGroupTestDensity) { state.userScroll.toDp() }
                .assertIsEqualTo(expectedInitialScroll, "initial scroll")
        }
    }

    @Test
    fun firstItemIsAtStart() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp).testTag("buttonGroup"),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        val buttonGroupBounds = rule.onNodeWithTag("buttonGroup").getBoundsInRoot()
        val box0Bounds = rule.onNodeWithTag("box-0").getBoundsInRoot()
        val box1Bounds = rule.onNodeWithTag("box-1").getBoundsInRoot()

        // The first item is at the start edge, and the next item is after it
        if (isRtl) {
            box0Bounds.right.assertIsEqualTo(buttonGroupBounds.right, "box-0 right")
            box1Bounds.right.assertIsEqualTo(box0Bounds.left, "box-1 right")
        } else {
            box0Bounds.left.assertIsEqualTo(buttonGroupBounds.left, "box-0 left")
            box1Bounds.left.assertIsEqualTo(box0Bounds.right, "box-1 left")
        }

        // Scroll is measured from the start, so is the same in every layout direction
        rule
            .onNodeWithTag("buttonGroup")
            .assertHorizontalScrollAxisRange(expectedValue = 50.dp, expectedMaxValue = 1000.dp)
        rule.runOnIdle { assertThat(state.currentItemIndex).isEqualTo(0) }
    }

    @Test
    fun smallButtonGroup_defaultCenteredInParent() {
        setContent {
            // Default horizontalArrangement aligns to center when there is extra space
            ButtonGroup(modifier = Modifier.width(200.dp).testTag("parent")) {
                // 3 children that fit within the viewport
                Box(Modifier.width(50.dp))
                Box(Modifier.width(50.dp).testTag("centerItem"))
                Box(Modifier.width(50.dp))
            }
        }
        // Center item's centerX should be the same as the parent container's centerX
        val middleChildCenterX = rule.onNodeWithTag("centerItem").getBoundsInRoot().centerX
        val parentCenterX = rule.onNodeWithTag("parent").getBoundsInRoot().centerX
        middleChildCenterX.assertIsEqualTo(parentCenterX, "middle child centerX")
    }

    @Test
    fun smallButtonGroup_scrollToEnd_lastItemCenteredAndSelected() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(modifier = Modifier.width(200.dp).testTag("parent"), state = state) {
                // 3 children that fit within the viewport
                Box(Modifier.width(50.dp))
                Box(Modifier.width(50.dp))
                Box(Modifier.width(50.dp).testTag("lastItem"))
            }
        }

        runBlocking { state.scrollToItem(2) }

        val lastChildCenterX = rule.onNodeWithTag("lastItem").getBoundsInRoot().centerX
        val parentCenterX = rule.onNodeWithTag("parent").getBoundsInRoot().centerX

        // When scrolled to the end, the last item should be roughly at the center of the container
        lastChildCenterX.assertIsEqualTo(parentCenterX, "last child centerX")
        assertThat(state.currentItemIndex).isEqualTo(2)
    }

    @Test
    fun contentPaddingsAreRespected() {
        setContent {
            // 200x200 ButtonGroup contains a single 200x200 Button, but the contentPadding means
            // that the Button will not fit perfectly in the container.
            ButtonGroup(
                modifier = Modifier.width(200.dp).height(200.dp).testTag("parent"),
                contentPadding =
                    PaddingValues(start = 10.dp, top = 20.dp, end = 30.dp, bottom = 40.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier.testTag("item").width(200.dp).fillMaxHeight(),
                ) {}
            }
        }
        val parentBounds = rule.onNodeWithTag("parent").getBoundsInRoot()
        // itemBounds should be smaller than the parentBounds by the contentPadding amount.
        // Note that on the end side, the item bounds will not be smaller, as this is a
        // horizontally-scrolling container, so items that don't fit along the x-axis will scroll.
        val expectedItemBounds =
            if (isRtl) {
                parentBounds.shrink(right = 10.dp, top = 20.dp, bottom = 40.dp)
            } else {
                parentBounds.shrink(left = 10.dp, top = 20.dp, bottom = 40.dp)
            }

        val actualItemBounds = rule.onNodeWithTag("item").getBoundsInRoot()

        actualItemBounds.assertIsEqualTo(expectedItemBounds, "item bounds")
    }

    @Test
    fun scrollToItem_movesViewport() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(modifier = Modifier.width(500.dp).height(100.dp), state = state) {
                // 500dp ButtonGroup containing 10x100dp buttons will have to scroll
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("box-0").assertIsDisplayed()
        rule.onNodeWithTag("box-5").assertIsNotDisplayed()

        runBlocking { state.scrollToItem(5) }
        assertThat(state.currentItemIndex).isEqualTo(5)

        rule.onNodeWithTag("box-0").assertIsNotDisplayed()
        rule.onNodeWithTag("box-5").assertIsDisplayed()
    }

    @Test
    fun animateScrollToItem_movesViewport() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(modifier = Modifier.width(500.dp).height(100.dp), state = state) {
                // 500dp ButtonGroup containing 10x100dp buttons will have to scroll
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("box-0").assertIsDisplayed()
        rule.onNodeWithTag("box-5").assertIsNotDisplayed()

        state.blockingAnimateScrollToItem(5)
        rule.waitForIdle()
        assertThat(state.currentItemIndex).isEqualTo(5)

        rule.onNodeWithTag("box-0").assertIsNotDisplayed()
        rule.onNodeWithTag("box-5").assertIsDisplayed()
    }

    @Test
    fun itemCountIncrease_updatesItemCount() {
        val state = ButtonGroupState()
        var buttonCount by mutableIntStateOf(1)
        setContent {
            ButtonGroup(state = state) {
                repeat(buttonCount) { i -> Button(onClick = {}) { Text("Button $i") } }
            }
        }
        rule.runOnIdle { assertThat(state.itemCount).isEqualTo(1) }
        buttonCount = 5
        rule.runOnIdle { assertThat(state.itemCount).isEqualTo(5) }
    }

    @Test
    fun itemCountDecrease_updatesCurrentItemIndex() {
        val state = ButtonGroupState()
        var buttonCount by mutableIntStateOf(5)
        setContent {
            ButtonGroup(state = state) {
                repeat(buttonCount) { i -> Button(onClick = {}) { Text("Button $i") } }
            }
        }
        rule.runOnIdle {
            runBlocking { state.scrollToItem(3) }
            assertThat(state.currentItemIndex).isEqualTo(3)
        }
        buttonCount = 1
        rule.runOnIdle { assertThat(state.currentItemIndex).isEqualTo(0) }
    }

    @Test
    fun itemResize_currentItemIndexIsUpdated() {
        val state = ButtonGroupState()
        var buttonWidth by mutableStateOf(100.dp)
        setContent {
            ButtonGroup(state = state) {
                repeat(5) { i ->
                    Button(onClick = {}, modifier = Modifier.width(buttonWidth)) {
                        Text("Button $i")
                    }
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.currentItemIndex).isEqualTo(0)
            // buttonWidth is 100dp, so we should be at the mid-point of this item (50dp)
            with(ButtonGroupTestDensity) { state.userScroll.toDp() }
                .assertIsEqualTo(50.dp, "initial scroll")
        }
        buttonWidth = 200.dp
        rule.runOnIdle {
            assertThat(state.currentItemIndex).isEqualTo(0)
            // buttonWidth updated to 200dp, we should leave the user at the mid-point still (100dp)
            with(ButtonGroupTestDensity) { state.userScroll.toDp() }
                .assertIsEqualTo(100.dp, "scroll after resize")
        }
    }

    @Test
    fun currentItemIndex_updatesWhenScrolling() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(modifier = Modifier.width(200.dp), state = state) {
                repeat(10) { i ->
                    Button(onClick = {}, modifier = Modifier.width(100.dp)) { Text("Button $i") }
                }
            }
        }
        rule.runOnIdle {
            runBlocking { state.scrollToItem(2) }
            assertThat(state.currentItemIndex).isEqualTo(2)

            runBlocking { state.scrollToItem(5) }
            assertThat(state.currentItemIndex).isEqualTo(5)
        }
    }

    @Test
    fun currentItemIndex_updatesOnAnimatedScrolling() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(modifier = Modifier.width(200.dp), state = state) {
                repeat(10) { i ->
                    Button(onClick = {}, modifier = Modifier.width(100.dp)) { Text("Button $i") }
                }
            }
        }
        state.blockingAnimateScrollToItem(2)
        rule.waitForIdle()
        assertThat(state.currentItemIndex).isEqualTo(2)

        state.blockingAnimateScrollToItem(5)
        rule.waitForIdle()
        assertThat(state.currentItemIndex).isEqualTo(5)
    }

    @Test
    fun scrollToItem_withInvalidIndex_throws() {
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(state = state) {
                repeat(3) { i -> Button(onClick = {}) { Text("Button $i") } }
            }
        }
        rule.runOnIdle {
            runBlocking { assertFailsWith<IllegalArgumentException> { state.scrollToItem(5) } }
        }
    }

    @Test
    fun emptyButtonGroup_hasNoItemsOrCurrentItemIndex() {
        val state = ButtonGroupState()
        setContent { ButtonGroup(state = state) {} }
        rule.runOnIdle {
            assertThat(state.itemCount).isEqualTo(0)
            assertThat(state.currentItemIndex).isEqualTo(-1)
        }
    }

    @Test
    fun scrollIndirect() {
        val numButtons = 10
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(numButtons) { i ->
                    Button(
                        onClick = {},
                        modifier = Modifier.width(100.dp).testTag("button-$i"),
                    ) {
                        Text("Button $i")
                    }
                }
            }
        }
        assertThat(state.currentItemIndex).isEqualTo(0)

        // x=50 initially, as this is the center of item 0
        indirectPointerDown()
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = "button-0")

        // x=90. Each item is 100dp wide, so we are still on item 0
        indirectScrollForwardBy(40.dp)
        state.assertState(scroll = 90.dp, currentItem = 0, focusedNodeTag = "button-0")

        // x=120. We are now on item 1
        indirectScrollForwardBy(30.dp)
        state.assertState(scroll = 120.dp, currentItem = 1, focusedNodeTag = "button-1")

        // x=150, as the scroll will snap to the center of the currently-focused item (item 1)
        indirectPointerUp()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        state.blockingAnimateScrollToItem(2)
        state.assertState(scroll = 250.dp, currentItem = 2, focusedNodeTag = "button-2")

        // Scrolling backwards moves back towards item 0. x=210 is still within item 2 (whose
        // bounds are [200, 300)), but x=190 is within item 1
        indirectPointerDown()
        indirectScrollForwardBy((-40).dp)
        state.assertState(scroll = 210.dp, currentItem = 2, focusedNodeTag = "button-2")
        indirectScrollForwardBy((-20).dp)
        state.assertState(scroll = 190.dp, currentItem = 1, focusedNodeTag = "button-1")

        // x=150, as the scroll will snap to the center of the currently-focused item (item 1)
        indirectPointerUp()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")
    }

    @Test
    fun scrollIndirect_isClampedToOneItemPerGesture() {
        val numButtons = 10
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(numButtons) { i ->
                    Button(
                        onClick = {},
                        modifier = Modifier.width(100.dp).testTag("button-$i"),
                    ) {
                        Text("Button $i")
                    }
                }
            }
        }

        // x=50 initially, as this is the center of item 0
        indirectPointerDown()
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = "button-0")

        // Scrolling far past item 1 within one gesture is clamped to the center of item 1 (x=150),
        // as a gesture can only move by one item
        indirectScrollForwardBy(400.dp)
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        indirectPointerUp()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        // Lifting and pressing again starts a new gesture, so we can move one more item (x=250)
        indirectPointerDown()
        indirectScrollForwardBy(400.dp)
        indirectPointerUp()
        state.assertState(scroll = 250.dp, currentItem = 2, focusedNodeTag = "button-2")

        // Scrolling backwards is clamped in the same way (x=150)
        indirectPointerDown()
        indirectScrollForwardBy((-400).dp)
        indirectPointerUp()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        // Programmatic scrolls are not clamped
        state.blockingAnimateScrollToItem(4)
        state.assertState(scroll = 450.dp, currentItem = 4, focusedNodeTag = "button-4")
    }

    @Test
    fun flingIndirect_isClampedToOneItemPerGesture() {
        val numButtons = 10
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(numButtons) { i ->
                    Button(
                        onClick = {},
                        modifier = Modifier.width(100.dp).testTag("button-$i"),
                    ) {
                        Text("Button $i")
                    }
                }
            }
        }

        // x=50 initially, as this is the center of item 0
        rule.runOnIdle {
            with(ButtonGroupTestDensity) { state.userScroll.toDp() }
                .assertIsEqualTo(50.dp, "initial scroll")
            assertThat(state.currentItemIndex).isEqualTo(0)
        }

        // A fast fling far past item 1 still only moves by one item, settling on the center of
        // item 1 (x=150), even though the velocity would carry a regular fling much further
        indirectFlingForwardBy(400.dp)
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        // Each new fling can move one more item (x=250)
        indirectFlingForwardBy(400.dp)
        state.assertState(scroll = 250.dp, currentItem = 2, focusedNodeTag = "button-2")

        // Flinging backwards is clamped in the same way (x=150)
        indirectFlingForwardBy((-400).dp)
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")
    }

    @Test
    fun scrollIndirect_isClampedToNeighborsOfCurrentItem_afterProgrammaticScroll() {
        val buttonWidths = listOf(50.dp, 100.dp, 150.dp, 200.dp, 250.dp)
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                buttonWidths.forEachIndexed { i, width ->
                    Button(onClick = {}, modifier = Modifier.width(width).testTag("button-$i")) {
                        Text("Button $i")
                    }
                }
            }
        }

        // x=25 initially, as this is the center of item 0. A first gesture is clamped to the
        // center of item 1 (x=100)
        indirectPointerDown()
        state.assertState(scroll = 25.dp, currentItem = 0, focusedNodeTag = "button-0")
        indirectScrollForwardBy(400.dp)
        indirectPointerUp()
        state.assertState(scroll = 100.dp, currentItem = 1, focusedNodeTag = "button-1")

        // Programmatically animate to item 3 (x=400), which is not limited to one item
        state.blockingAnimateScrollToItem(3)
        state.assertState(scroll = 400.dp, currentItem = 3, focusedNodeTag = "button-3")

        // Scrolling far forwards is clamped to the center of item 4 (x=625), i.e. +225dp. This
        // would be x=225 (item 2) if the clamp was still anchored to item 1
        indirectPointerDown()
        indirectScrollForwardBy(400.dp)
        state.assertState(scroll = 625.dp, currentItem = 4, focusedNodeTag = "button-4")
        indirectPointerUp()
        state.assertState(scroll = 625.dp, currentItem = 4, focusedNodeTag = "button-4")

        // Scrolling far backwards is clamped to the center of item 3 (x=400), i.e. -225dp, rather
        // than reaching item 2 (x=225) or beyond
        indirectPointerDown()
        indirectScrollForwardBy((-400).dp)
        indirectPointerUp()
        state.assertState(scroll = 400.dp, currentItem = 3, focusedNodeTag = "button-3")
    }

    @Test
    fun scroll_startedBeforeFirstLayout_isClampedToNeighborsOfCurrentItem() {
        val state = ButtonGroupState()

        // Start a scroll before the ButtonGroup has been laid out, so the current item (and so the
        // scroll anchor) isn't known yet when the scroll starts. The scroll waits for layoutDone
        // before scrolling.
        val layoutDone = CompletableDeferred<Unit>()
        val scrollJob =
            CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.UNDISPATCHED) {
                state.scroll {
                    layoutDone.await()
                    scrollBy(with(ButtonGroupTestDensity) { 400.dp.toPx() })
                }
            }
        assertThat(state.currentItemIndex).isEqualTo(-1)

        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(10) { i ->
                    Button(onClick = {}, modifier = Modifier.width(100.dp).testTag("button-$i")) {
                        Text("Button $i")
                    }
                }
            }
        }
        // x=50 initially, as this is the center of item 0
        rule.waitForIdle()
        with(ButtonGroupTestDensity) { state.userScroll.toDp() }.assertIsEqualTo(50.dp, "scroll")
        assertThat(state.currentItemIndex).isEqualTo(0)

        layoutDone.complete(Unit)
        runBlocking { scrollJob.join() }

        // The anchor is computed from the scroll position when the scroll first receives a delta,
        // so scrolling far past item 1 is still clamped to the center of item 1 (x=150)
        rule.waitForIdle()
        with(ButtonGroupTestDensity) { state.userScroll.toDp() }.assertIsEqualTo(150.dp, "scroll")
        assertThat(state.currentItemIndex).isEqualTo(1)
    }

    @Ignore("b/267253920")
    @Test
    fun scrollDirect() {
        val state = ButtonGroupState()

        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(10) { i ->
                    Button(
                        onClick = {},
                        modifier = Modifier.width(100.dp).testTag("button-$i"),
                    ) {
                        Text("Button $i")
                    }
                }
            }
        }

        fun scrollBy(x: Dp) {
            // Dragging the content towards its start scrolls forwards. The start is on the left in
            // LTR, and on the right in RTL.
            val physicalDistance = if (isRtl) x else -x
            rule.onRoot().performTouchInput {
                moveBy(Offset(with(ButtonGroupTestDensity) { physicalDistance.toPx() }, 0f))
            }
        }
        fun down() {
            rule.onRoot().performTouchInput { down(center) }
        }
        fun up() {
            rule.onRoot().performTouchInput { this@performTouchInput.up() }
        }
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = null)

        // x=50 initially, as this is the center of item 0
        down()
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = null)

        // x=90. Each item is 100dp wide, so we are still on item 0
        scrollBy(40.dp)
        state.assertState(scroll = 90.dp, currentItem = 0, focusedNodeTag = null)

        // x=120. We are now on item 1
        scrollBy(30.dp)
        state.assertState(scroll = 120.dp, currentItem = 1, focusedNodeTag = null)

        // x=150, as the scroll will snap to the center of the currently-focused item (item 1)
        up()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = null)

        state.blockingAnimateScrollToItem(2)
        state.assertState(scroll = 250.dp, currentItem = 2, focusedNodeTag = null)
    }

    @Test
    fun absoluteArrangement_laysOutLeftToRight() {
        // Absolute arrangements ignore layout direction, so the first item is on the left in every
        // layout direction
        val state = ButtonGroupState()
        setContent {
            ButtonGroup(
                modifier = Modifier.width(500.dp).height(100.dp).testTag("buttonGroup"),
                state = state,
                horizontalArrangement = Arrangement.Absolute.spacedBy(0.dp),
                contentPadding = PaddingValues.Zero,
            ) {
                repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
            }
        }
        val buttonGroupBounds = rule.onNodeWithTag("buttonGroup").getBoundsInRoot()
        rule
            .onNodeWithTag("box-0")
            .getBoundsInRoot()
            .left
            .assertIsEqualTo(buttonGroupBounds.left, "box-0 left")
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = null)

        state.blockingAnimateScrollToItem(5)
        state.assertState(scroll = 550.dp, currentItem = 5, focusedNodeTag = null)
        rule.onNodeWithTag("box-0").assertIsNotDisplayed()
        rule.onNodeWithTag("box-5").assertIsDisplayed()
    }

    @Test
    fun noEnabledButtonsInGroup_focusGoesToNextButton() {
        setContent {
            ButtonGroup {
                repeat(10) { i ->
                    Button(enabled = false, onClick = {}, modifier = Modifier.width(100.dp)) {
                        Text("Button $i")
                    }
                }
            }
            Button(onClick = {}, modifier = Modifier.width(100.dp).testTag("focused_button")) {
                Text("Focused Button")
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag("focused_button").assertIsFocused()
    }

    @Test
    fun focusEnteringFromStartSide_focusesCurrentItem() {
        val state = ButtonGroupState(initialItemIndex = 3)
        setButtonGroupBetweenFocusableItems(state)
        rule.onNodeWithTag("startItem").requestFocus()
        rule.onNodeWithTag("startItem").assertIsFocused()

        // The start side is on the left in LTR, and on the right in RTL, so focus enters the
        // ButtonGroup by moving right in LTR, and left in RTL
        moveFocus(if (isRtl) FocusDirection.Left else FocusDirection.Right)

        // Focus goes to the current item, rather than the item closest to the start edge
        state.assertState(scroll = 350.dp, currentItem = 3, focusedNodeTag = "button-3")
    }

    @Test
    fun focusEnteringFromEndSide_focusesCurrentItem() {
        val state = ButtonGroupState(initialItemIndex = 3)
        setButtonGroupBetweenFocusableItems(state)
        rule.onNodeWithTag("endItem").requestFocus()
        rule.onNodeWithTag("endItem").assertIsFocused()

        // The end side is on the right in LTR, and on the left in RTL, so focus enters the
        // ButtonGroup by moving left in LTR, and right in RTL
        moveFocus(if (isRtl) FocusDirection.Right else FocusDirection.Left)

        // Focus goes to the current item, rather than the item closest to the end edge
        state.assertState(scroll = 350.dp, currentItem = 3, focusedNodeTag = "button-3")
    }

    @Test
    fun verticalAlignment_alignsChildrenInButtonGroup() {
        setContent {
            ButtonGroup(modifier = Modifier.height(500.dp), verticalAlignment = Alignment.Bottom) {
                Button(onClick = {}, modifier = Modifier.height(100.dp).testTag("button")) {
                    Text("Button")
                }
            }
        }
        rule.waitForIdle()
        rule
            .onNodeWithTag("button")
            .assertHeightIsEqualTo(100.dp)
            .assertTopPositionInRootIsEqualTo(400.dp)
    }

    /** Sets [content] using [setButtonGroupTestContent], with this test's [layoutDirection]. */
    private fun setContent(content: @Composable () -> Unit) {
        rule.setButtonGroupTestContent(layoutDirection = layoutDirection, content = content)
    }

    /**
     * Sets content with a [ButtonGroup] that uses [state], between two focusable items tagged
     * `startItem` and `endItem`, in a [Row]. The [ButtonGroup]'s viewport only shows part of its
     * content, so the items closest to its edges are different from the current item.
     */
    private fun setButtonGroupBetweenFocusableItems(state: ButtonGroupState) {
        setContent {
            focusManager = LocalFocusManager.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).testTag("startItem").focusable())
                ButtonGroup(
                    modifier = Modifier.width(300.dp),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues.Zero,
                ) {
                    repeat(10) { i ->
                        Button(
                            onClick = {},
                            modifier = Modifier.width(100.dp).testTag("button-$i"),
                        ) {
                            Text("Button $i")
                        }
                    }
                }
                Box(Modifier.size(50.dp).testTag("endItem").focusable())
            }
        }
    }

    private fun moveFocus(direction: FocusDirection) {
        rule.runOnIdle { assertThat(focusManager.moveFocus(direction)).isTrue() }
    }

    /**
     * Starts an indirect pointer gesture from the center of the input device, so that there is room
     * to move both forwards and backwards within the device's bounds.
     */
    private fun indirectPointerDown() {
        rule.sendGlimmerIndirectPointerInput { down(inputDeviceCenter) }
    }

    private fun indirectPointerUp() {
        rule.sendGlimmerIndirectPointerInput { up() }
    }

    /**
     * Moves the current indirect pointer gesture by [distance] towards the end of the content (or
     * towards the start, if negative).
     *
     * In RTL, scrollable components invert indirect pointer input along the horizontal axis (see
     * ScrollableDefaults.reverseDirection), so moving towards negative x scrolls forwards.
     */
    private fun indirectScrollForwardBy(distance: Dp) {
        val physicalDistance = if (isRtl) -distance else distance
        rule.sendGlimmerIndirectPointerInput {
            moveBy(Offset(with(ButtonGroupTestDensity) { physicalDistance.toPx() }, 0f))
        }
    }

    /**
     * Performs a complete, fast indirect pointer gesture (down, quick moves, up) of [distance]
     * towards the end of the content (or towards the start, if negative), so that the gesture ends
     * with a high velocity and triggers a fling.
     *
     * See [indirectScrollForwardBy] for how the direction is mirrored in RTL.
     */
    private fun indirectFlingForwardBy(distance: Dp) {
        val physicalDistance = if (isRtl) -distance else distance
        rule.sendGlimmerIndirectPointerInput {
            down(inputDeviceCenter)
            evenlyDividedMoveX(
                distance = with(ButtonGroupTestDensity) { physicalDistance.toPx() },
                steps = defaultStepCountForMoveToVelocityTrigger,
                duration = FastFlingDurationMillis,
            )
            up()
        }
    }

    /**
     * Asserts that the current values of [ScrollAxisRange.value] and [ScrollAxisRange.maxValue] for
     * this node's [SemanticsProperties.HorizontalScrollAxisRange] match the provided values.
     */
    private fun SemanticsNodeInteraction.assertHorizontalScrollAxisRange(
        expectedValue: Dp,
        expectedMaxValue: Dp,
    ): SemanticsNodeInteraction {
        val range = fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        val actualValue = with(ButtonGroupTestDensity) { range.value().toDp() }
        val actualMaxValue = with(ButtonGroupTestDensity) { range.maxValue().toDp() }
        actualValue.assertIsEqualTo(expectedValue, "current horizontal scroll")
        actualMaxValue.assertIsEqualTo(expectedMaxValue, "max horizontal scroll")
        return this
    }

    private fun ButtonGroupState.assertState(
        scroll: Dp,
        currentItem: Int,
        focusedNodeTag: String?,
    ) {
        rule.assertButtonGroupState(
            state = this,
            scroll = scroll,
            currentItem = currentItem,
            focusedNodeTag = focusedNodeTag,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(LayoutDirection.Ltr, LayoutDirection.Rtl)

        /** The duration of a fast fling gesture, short enough to generate a high velocity. */
        private const val FastFlingDurationMillis = 20L
    }
}

/**
 * [ButtonGroup] tests that compare LTR and RTL layouts against each other.
 *
 * Tests that should behave identically in each [LayoutDirection] are in [ButtonGroupTest].
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ButtonGroupRtlTest {
    @get:Rule(order = 0)
    val rule = createComposeRule(ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(order = 1) val glimmerRule = createGlimmerRule()

    @Test
    fun rtl_scrollingContent_isMirrorOfLtr() {
        // Varied widths, spacing, and asymmetric padding, so any mistake in mirroring shows up
        val itemWidths = listOf(60.dp, 120.dp, 80.dp, 200.dp, 100.dp, 140.dp, 70.dp)
        val ltrState = ButtonGroupState()
        val rtlState = ButtonGroupState()
        setLtrAndRtlButtonGroups(
            ltrState = ltrState,
            rtlState = rtlState,
            itemWidths = itemWidths,
            contentPadding = PaddingValues(start = 10.dp, end = 30.dp),
        )
        assertRtlIsMirrorOfLtr(itemWidths.size)

        for (index in listOf(1, 3, 6, 4, 0)) {
            runBlocking {
                ltrState.scrollToItem(index)
                rtlState.scrollToItem(index)
            }
            rule.waitForIdle()
            assertThat(rtlState.currentItemIndex).isEqualTo(index)
            with(ButtonGroupTestDensity) {
                rtlState.userScroll
                    .toDp()
                    .assertIsEqualTo(
                        ltrState.userScroll.toDp(),
                        "RTL userScroll after scrolling to $index",
                    )
            }
            assertRtlIsMirrorOfLtr(itemWidths.size)
        }
    }

    @Test
    fun rtl_nonScrollingContent_isMirrorOfLtr() {
        // Content fits within the viewport, so it is offset differently from scrolling content
        val itemWidths = listOf(60.dp, 100.dp, 80.dp)
        val ltrState = ButtonGroupState()
        val rtlState = ButtonGroupState()
        setLtrAndRtlButtonGroups(
            ltrState = ltrState,
            rtlState = rtlState,
            itemWidths = itemWidths,
            contentPadding = PaddingValues(start = 10.dp, end = 30.dp),
        )
        assertRtlIsMirrorOfLtr(itemWidths.size)

        for (index in listOf(2, 1, 0)) {
            runBlocking {
                ltrState.scrollToItem(index)
                rtlState.scrollToItem(index)
            }
            rule.waitForIdle()
            assertThat(rtlState.currentItemIndex).isEqualTo(index)
            assertRtlIsMirrorOfLtr(itemWidths.size)
        }
    }

    @Test
    fun layoutDirectionChange_keepsCurrentItem() {
        val state = ButtonGroupState()
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        rule.setButtonGroupTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ButtonGroup(
                    modifier = Modifier.width(500.dp).height(100.dp).testTag("buttonGroup"),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues.Zero,
                ) {
                    repeat(10) { i -> Box(Modifier.width(100.dp).height(100.dp).testTag("box-$i")) }
                }
            }
        }
        runBlocking { state.scrollToItem(3) }
        rule.assertButtonGroupState(state, scroll = 350.dp, currentItem = 3, focusedNodeTag = null)
        val ltrGroupBounds = rule.onNodeWithTag("buttonGroup").getBoundsInRoot()
        val ltrBox3Bounds = rule.onNodeWithTag("box-3").getBoundsInRoot()

        layoutDirection = LayoutDirection.Rtl
        rule.assertButtonGroupState(state, scroll = 350.dp, currentItem = 3, focusedNodeTag = null)

        // box-3 should now be at the mirrored position
        val rtlGroupBounds = rule.onNodeWithTag("buttonGroup").getBoundsInRoot()
        val rtlBox3Bounds = rule.onNodeWithTag("box-3").getBoundsInRoot()
        (rtlGroupBounds.right - rtlBox3Bounds.right).assertIsEqualTo(
            ltrBox3Bounds.left - ltrGroupBounds.left,
            "box-3 distance from start",
        )
    }

    /**
     * Asserts that each item in the RTL ButtonGroup set by [setLtrAndRtlButtonGroups] is laid out
     * at the mirror image of the corresponding item in the LTR ButtonGroup.
     *
     * Unclipped bounds are used, so that items that are scrolled out of view are also checked.
     */
    private fun assertRtlIsMirrorOfLtr(itemCount: Int) {
        rule.waitForIdle()
        val (ltrGroupLeft, _) = unclippedHorizontalBounds("ltr-group")
        val (_, rtlGroupRight) = unclippedHorizontalBounds("rtl-group")
        repeat(itemCount) { i ->
            val (ltrItemLeft, ltrItemRight) = unclippedHorizontalBounds("ltr-item-$i")
            val (rtlItemLeft, rtlItemRight) = unclippedHorizontalBounds("rtl-item-$i")
            (rtlGroupRight - rtlItemRight).assertIsEqualTo(
                ltrItemLeft - ltrGroupLeft,
                "item $i distance from start",
            )
            (rtlGroupRight - rtlItemLeft).assertIsEqualTo(
                ltrItemRight - ltrGroupLeft,
                "item $i end distance from start",
            )
        }
    }

    /** Returns the left and right edges of the node with [tag], relative to the root, unclipped. */
    private fun unclippedHorizontalBounds(tag: String): Pair<Dp, Dp> {
        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        val left = node.positionInRoot.x
        val right = left + node.size.width
        return with(ButtonGroupTestDensity) { left.toDp() to right.toDp() }
    }

    /**
     * Sets content with two identical [ButtonGroup]s (one LTR, one RTL) stacked vertically, each
     * containing items with the given [itemWidths]. The groups are tagged `ltr-group` and
     * `rtl-group`, and their items are tagged `ltr-item-<index>` and `rtl-item-<index>`.
     */
    private fun setLtrAndRtlButtonGroups(
        ltrState: ButtonGroupState,
        rtlState: ButtonGroupState,
        itemWidths: List<Dp>,
        contentPadding: PaddingValues,
    ) {
        @Composable
        fun TestButtonGroup(
            tagPrefix: String,
            layoutDirection: LayoutDirection,
            state: ButtonGroupState,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ButtonGroup(
                    modifier = Modifier.width(400.dp).height(50.dp).testTag("$tagPrefix-group"),
                    state = state,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = contentPadding,
                ) {
                    itemWidths.forEachIndexed { i, width ->
                        Box(Modifier.width(width).height(50.dp).testTag("$tagPrefix-item-$i"))
                    }
                }
            }
        }
        rule.setButtonGroupTestContent {
            Column {
                TestButtonGroup(
                    tagPrefix = "ltr",
                    layoutDirection = LayoutDirection.Ltr,
                    state = ltrState,
                )
                TestButtonGroup(
                    tagPrefix = "rtl",
                    layoutDirection = LayoutDirection.Rtl,
                    state = rtlState,
                )
            }
        }
    }
}

private fun ButtonGroupState.blockingAnimateScrollToItem(index: Int) {
    runBlocking(Dispatchers.Main + AutoTestFrameClock()) { animateScrollToItem(index) }
}

private val DpRect.centerX: Dp
    get() = (right + left) / 2f

/** Returns the receiver [DpRect] with each of the 4 edges moved inward by the specified amounts. */
private fun DpRect.shrink(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
): DpRect =
    DpRect(
        left = this.left + left,
        top = this.top + top,
        right = this.right - right,
        bottom = this.bottom - bottom,
    )

private fun DpRect.assertIsEqualTo(expected: DpRect, subject: String) {
    left.assertIsEqualTo(expected.left, "$subject (left)")
    top.assertIsEqualTo(expected.top, "$subject (top)")
    right.assertIsEqualTo(expected.right, "$subject (right)")
    bottom.assertIsEqualTo(expected.bottom, "$subject (bottom)")
}

/**
 * [setGlimmerThemeContent] with density, touch-slop, and layout direction set as needed for
 * [ButtonGroup] tests.
 *
 * We need touch-slop set to 0 so that scroll is not consumed by the ButtonGroup, throwing off our
 * calculations.
 *
 * We need density set to a nicely-divisible number like 2 so that rounding errors don't occur when
 * we translate from dp -> px -> dp during assertions.
 *
 * @param layoutDirection the [LayoutDirection] provided to [content]
 */
private fun ComposeContentTestRule.setButtonGroupTestContent(
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    content: @Composable () -> Unit,
) {
    setGlimmerThemeContent(density = ButtonGroupTestDensity) {
        CompositionLocalProvider(
            LocalViewConfiguration provides TestViewConfiguration(touchSlop = 0f),
            LocalLayoutDirection provides layoutDirection,
            content = content,
        )
    }
}

/**
 * Waits for idle, then asserts that [state] has the given [scroll] and [currentItem], and that the
 * node tagged [focusedNodeTag] is focused (or that no node is focused, if [focusedNodeTag] is
 * `null`).
 */
private fun ComposeContentTestRule.assertButtonGroupState(
    state: ButtonGroupState,
    scroll: Dp,
    currentItem: Int,
    focusedNodeTag: String?,
) {
    waitForIdle()
    with(ButtonGroupTestDensity) { state.userScroll.toDp() }.assertIsEqualTo(scroll, "userScroll")
    assertThat(state.currentItemIndex).isEqualTo(currentItem)
    if (focusedNodeTag == null) {
        // We are in touch mode, so assert that no buttons are actually focused
        onAllNodes(isFocused()).assertCountEquals(0)
    } else {
        onNodeWithTag(focusedNodeTag).assertIsFocused()
    }
}

private val ButtonGroupState.userScroll: Float
    get() = (this as ButtonGroupStateImpl).userScroll

// TODO(b/562338491): Integrate into ComposeUiTestConfig if possible
private val ButtonGroupTestDensity = Density(2f)
