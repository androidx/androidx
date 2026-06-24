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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
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
import androidx.compose.ui.test.inputDeviceCenterLeft
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ButtonGroupTest {
    @get:Rule(0) val rule = createComposeRule(ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun testSemanticsValues() {
        val button1Width = 100.dp
        val button2Width = 150.dp
        val spacingWidth = 8.dp

        val state = ButtonGroupState()
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        state.userScroll.toDp().assertIsEqualTo(250.dp, "userScroll after ScrollToIndex(2)")
        assertThat(state.currentItemIndex).isEqualTo(2)
    }

    @Test
    fun testSemanticsScrollToInvalidIndex_updatesScrollValue() {
        val state = ButtonGroupState()
        rule.setGlimmerThemeContent {
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
        state.userScroll.toDp().assertIsEqualTo(50.dp, "userScroll after failed ScrollToIndex(20)")
        assertThat(state.currentItemIndex).isEqualTo(0)
    }

    @Test
    fun stateScroll_initializedOnLayout() {
        val initialItemIndex = 1
        val buttonWidth = 300.dp
        val buttonPaddingWidth = 8.dp

        val state = ButtonGroupState(initialItemIndex = initialItemIndex)
        rule.setGlimmerThemeContent {
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
            state.userScroll.toDp().assertIsEqualTo(expectedInitialScroll, "initial scroll")
        }
    }

    @Test
    fun smallButtonGroup_defaultCenteredInParent() {
        rule.setGlimmerThemeContent {
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
    fun contentPaddingsAreRespected() {
        rule.setGlimmerThemeContent {
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
        // Note that on the right side, the item bounds will not be smaller, as this is a
        // horizontally-scrolling container, so items that don't fit along the x-axis will scroll.
        val expectedItemBounds = parentBounds.shrink(left = 10.dp, top = 20.dp, bottom = 40.dp)

        val actualItemBounds = rule.onNodeWithTag("item").getBoundsInRoot()

        actualItemBounds.assertIsEqualTo(expectedItemBounds, "item bounds")
    }

    @Test
    fun scrollToItem_movesViewport() {
        val state = ButtonGroupState()
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
            state.userScroll.toDp().assertIsEqualTo(50.dp, "initial scroll")
        }
        buttonWidth = 200.dp
        rule.runOnIdle {
            assertThat(state.currentItemIndex).isEqualTo(0)
            // buttonWidth updated to 200dp, we should leave the user at the mid-point still (100dp)
            state.userScroll.toDp().assertIsEqualTo(100.dp, "scroll after resize")
        }
    }

    @Test
    fun currentItemIndex_updatesWhenScrolling() {
        val state = ButtonGroupState()
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent {
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
        rule.setGlimmerThemeContent { ButtonGroup(state = state) {} }
        rule.runOnIdle {
            assertThat(state.itemCount).isEqualTo(0)
            assertThat(state.currentItemIndex).isEqualTo(-1)
        }
    }

    @Test
    fun scrollIndirect() {
        val numButtons = 10
        val state = ButtonGroupState()
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(
                LocalViewConfiguration provides TestViewConfiguration(touchSlop = 0f)
            ) {
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
        }

        fun scrollBy(x: Dp) {
            rule.sendGlimmerIndirectPointerInput { moveBy(Offset(x.toPx(), 0f)) }
        }
        fun down() {
            rule.sendGlimmerIndirectPointerInput { down(inputDeviceCenterLeft) }
        }
        fun up() {
            rule.sendGlimmerIndirectPointerInput { this@sendGlimmerIndirectPointerInput.up() }
        }
        assertThat(state.currentItemIndex).isEqualTo(0)

        // x=50 initially, as this is the center of item 0
        down()
        state.assertState(scroll = 50.dp, currentItem = 0, focusedNodeTag = "button-0")

        // x=90. Each item is 100dp wide, so we are still on item 0
        scrollBy(40.dp)
        state.assertState(scroll = 90.dp, currentItem = 0, focusedNodeTag = "button-0")

        // x=120. We are now on item 1
        scrollBy(30.dp)
        state.assertState(scroll = 120.dp, currentItem = 1, focusedNodeTag = "button-1")

        // x=150, as the scroll will snap to the center of the currently-focused item (item 1)
        up()
        state.assertState(scroll = 150.dp, currentItem = 1, focusedNodeTag = "button-1")

        state.blockingAnimateScrollToItem(2)
        state.assertState(scroll = 250.dp, currentItem = 2, focusedNodeTag = "button-2")
    }

    @Ignore("b/267253920")
    @Test
    fun scrollDirect() {
        val state = ButtonGroupState()

        rule.setGlimmerThemeContent {
            CompositionLocalProvider(
                LocalViewConfiguration provides TestViewConfiguration(touchSlop = 0f)
            ) {
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
        }

        fun scrollBy(x: Dp) {
            rule.onRoot().performTouchInput { moveBy(Offset(-x.toPx(), 0f)) }
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
    fun noEnabledButtonsInGroup_focusGoesToNextButton() {
        rule.setGlimmerThemeContent {
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
    fun verticalAlignment_alignsChildrenInButtonGroup() {
        rule.setGlimmerThemeContent {
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

    /**
     * Asserts that the current values of [ScrollAxisRange.value] and [ScrollAxisRange.maxValue] for
     * this node's [SemanticsProperties.HorizontalScrollAxisRange] match the provided values.
     */
    private fun SemanticsNodeInteraction.assertHorizontalScrollAxisRange(
        expectedValue: Dp,
        expectedMaxValue: Dp,
    ): SemanticsNodeInteraction {
        val range = fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        val actualValue = range.value().toDp()
        val actualMaxValue = range.maxValue().toDp()
        actualValue.assertIsEqualTo(expectedValue, "current horizontal scroll")
        actualMaxValue.assertIsEqualTo(expectedMaxValue, "max horizontal scroll")
        return this
    }

    private fun Float.toDp(): Dp = with(rule.density) { toDp() }

    private fun ButtonGroupState.assertState(
        scroll: Dp,
        currentItem: Int,
        focusedNodeTag: String?,
    ) {
        rule.waitForIdle()
        userScroll.toDp().assertIsEqualTo(scroll, "userScroll")
        assertThat(currentItemIndex).isEqualTo(currentItem)
        if (focusedNodeTag == null) {
            // We are in touch mode, so assert that no buttons are actually focused
            rule.onAllNodes(isFocused()).assertCountEquals(0)
        } else {
            rule.onNodeWithTag(focusedNodeTag).assertIsFocused()
        }
    }
}

private fun ButtonGroupState.blockingAnimateScrollToItem(index: Int) {
    runBlocking(Dispatchers.Main + AutoTestFrameClock()) { animateScrollToItem(index) }
}

private val ButtonGroupState.userScroll: Float
    get() = (this as ButtonGroupStateImpl).userScroll

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
