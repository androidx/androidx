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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.assertModifierIsPure
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.assertWithMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.first
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DefaultFlingBehavior
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.hasSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.isLessThan
import androidx.compose.foundation.isNonZero
import androidx.compose.foundation.isNotEqualTo
import androidx.compose.foundation.isNotNaN
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.isZero
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.test.IgnoreIosTarget
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@OptIn(ExperimentalTestApi::class)
class ScrollableTest {

    private val scrollableBoxTag = "scrollableBox"

    private lateinit var scope: CoroutineScope

    private val focusRequester = FocusRequester()

    private fun ComposeUiTest.setContentAndGetScope(content: @Composable () -> Unit) {
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }
            content()
        }
    }

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollable_horizontalScroll() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100,
            )
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_horizontalScroll_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_horizontalScroll_2d_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-100f, 0f)) // only moved horizontally
        }

        var lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(0f, -100f)) // only moved vertically
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(100f, 0f)) // only moved horizontally
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollableHorizontal_diagonalScroll_2d_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(100f, 50f)) // moved more horizontally at 1 quadrant
        }

        runOnIdle {
            assertThat(total).isLessThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(100f, -50f)) // moved more horizontally at 4 quadrant
        }

        runOnIdle {
            assertThat(total).isLessThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-100f, 50f)) // moved more horizontally at 2 quadrant
        }

        runOnIdle {
            assertThat(total).isGreaterThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-100f, -50f)) // moved more horizontally at 3 quadrant
        }

        runOnIdle {
            assertThat(total).isGreaterThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-50f, -100f)) // // moved more vertically
        }

        runOnIdle { assertThat(total).isEqualTo(0f) }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_horizontalScroll_mouseWheel_badMotionEvent() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Float.NaN, ScrollWheel.Horizontal)
        }

        assertThat(total).isEqualTo(0)
    }

    /*
     * Note: For keyboard scrolling to work (that is, scrolling based on the page up/down keys),
     * at least one child within the scrollable must be focusable. (This matches the behavior in
     * Views.)
     */
    @Test
    fun scrollable_horizontalScroll_keyboardPageUpAndDown() = runComposeUiTest {
        var scrollAmount = 0f

        val scrollableState =
            ScrollableState(
                consumeScrollDelta = {
                    scrollAmount += it
                    it
                }
            )

        setContent {
            Row(
                Modifier.fillMaxHeight()
                    .wrapContentWidth()
                    .background(Color.Red)
                    .scrollable(state = scrollableState, orientation = Orientation.Horizontal)
                    .padding(10.dp)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxHeight()
                            .testTag(scrollableBoxTag)
                            .width(50.dp)
                            .background(Color.Blue)
                            // Required for keyboard scrolling (page up/down keys) to work.
                            .focusable()
                            .padding(10.dp)
                )

                Spacer(modifier = Modifier.size(10.dp))

                for (i in 0 until 40) {
                    val color =
                        if (i % 2 == 0) {
                            Color.Yellow
                        } else {
                            Color.Green
                        }

                    Box(
                        modifier =
                            Modifier.fillMaxHeight().width(50.dp).background(color).padding(10.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).requestFocus()
        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageDown) }

        runOnIdle { assertThat(scrollAmount).isLessThan(0f) }

        scrollAmount = 0f

        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageUp) }

        runOnIdle { assertThat(scrollAmount).isGreaterThan(0f) }
    }

    @Test
    fun scrollable_horizontalScroll_reverse() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isLessThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100,
            )
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_horizontalScroll_reverse_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isLessThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_verticalScroll() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Vertical)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_verticalScroll_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Vertical)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_verticalScroll_mouseWheel_badMotionEvent() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Vertical)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput { this.scroll(Offset(0f, -100f)) }

        val lastTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performMouseInput { this.scroll(Offset(-100f, 0f)) }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput { this.scroll(Offset(0f, 100f)) }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollableVertical_diagonalScroll_2d_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Vertical)
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(50f, 100f)) // moved more vertically, 1st quadrant
        }

        runOnIdle {
            assertThat(total).isLessThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-50f, 100f)) // moved more vertically, 2nd quadrant
        }

        runOnIdle {
            assertThat(total).isLessThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-50f, -100f)) // moved more vertically, 3rd quadrant
        }

        runOnIdle {
            assertThat(total).isGreaterThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(50f, -100f)) // moved more vertically, 4th quadrant
        }

        runOnIdle {
            assertThat(total).isGreaterThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(100f, 100f)) // at exact 45 angle
        }

        runOnIdle {
            assertThat(total).isLessThan(0)
            total = 0f
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Offset(-100f, -50f)) // // moved more horizontally
        }

        runOnIdle { assertThat(total).isZero() }
    }

    @Test
    fun scrollable_nestedDiagonalScroll_mouseWheel_triggersOnAngle() = runComposeUiTest {
        var totalVerticalScroll = 0f
        var totalHorizontalScroll = 0f
        val outerController =
            ScrollableState(
                consumeScrollDelta = {
                    totalVerticalScroll += it
                    it
                }
            )

        val innerController =
            ScrollableState(
                consumeScrollDelta = {
                    totalHorizontalScroll += it
                    it
                }
            )
        setContentAndGetScope {
            Box {
                Box(
                    modifier =
                        Modifier.testTag("outerScrollableBoxTag")
                            .size(100.dp)
                            .scrollable(outerController, Orientation.Vertical)
                ) {
                    Box(
                        modifier =
                            Modifier.testTag("innerScrollableBoxTag")
                                .size(100.dp)
                                .scrollable(innerController, Orientation.Horizontal)
                    )
                }
            }
        }

        // mostly horizontal event triggered horizontal scrollable
        onRoot().performMouseInput { this.scroll(Offset(-100f, -50f)) }

        // Here and below we call awaitIdle() before runOnIdle to avoid event loop blocking in the web.
        // awaitIdle waits for idleness co-operatively, unlike waitForIdle (called inside runOnIdle)
        awaitIdle()
        runOnIdle {
            assertThat(totalHorizontalScroll).isGreaterThan(0)
            assertThat(totalVerticalScroll).isZero()
        }

        totalHorizontalScroll = 0f
        totalVerticalScroll = 0f

        // mostly vertical event triggered vertical scrollable
        onRoot().performMouseInput { this.scroll(Offset(-10f, -50f)) }

        awaitIdle()
        runOnIdle {
            assertThat(totalVerticalScroll).isGreaterThan(0)
            assertThat(totalHorizontalScroll).isZero()
        }

        totalHorizontalScroll = 0f
        totalVerticalScroll = 0f

        // started vertical and changed to horizontal triggers only vertical
        // We use the initial event to determine user intention due to mouse wheel events batching.
        onRoot().performMouseInput {
            this.scroll(Offset(-10f, -50f))
            this.scroll(Offset(-50f, -10f))
        }

        awaitIdle()
        runOnIdle {
            assertThat(totalVerticalScroll).isGreaterThan(0)
            assertThat(totalHorizontalScroll).isZero()
        }

        totalHorizontalScroll = 0f
        totalVerticalScroll = 0f

        // started vertical, waited, changed to horizontal means we have a new scrolling direction.
        onRoot().performMouseInput { this.scroll(Offset(-10f, -50f)) }

        awaitIdle()

        onRoot().performMouseInput { this.scroll(Offset(-50f, -10f)) }

        awaitIdle()
        runOnIdle {
            assertThat(totalVerticalScroll).isGreaterThan(0)
            assertThat(totalHorizontalScroll).isGreaterThan(0)
        }
    }

    /*
     * Note: For keyboard scrolling to work (that is, scrolling based on the page up/down keys),
     * at least one child within the scrollable must be focusable. (This matches the behavior in
     * Views.)
     */
    @Test
    fun scrollable_verticalScroll_keyboardPageUpAndDown() = runComposeUiTest {
        var scrollAmount = 0f

        val scrollableState =
            ScrollableState(
                consumeScrollDelta = {
                    scrollAmount += it
                    it
                }
            )

        setContent {
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.Red)
                    .scrollable(state = scrollableState, orientation = Orientation.Vertical)
                    .padding(10.dp)
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(scrollableBoxTag)
                            .height(50.dp)
                            .background(Color.Blue)
                            // Required for keyboard scrolling (page up/down keys) to work.
                            .focusable()
                            .padding(10.dp)
                )

                Spacer(modifier = Modifier.size(10.dp))

                for (i in 0 until 40) {
                    val color =
                        if (i % 2 == 0) {
                            Color.Yellow
                        } else {
                            Color.Green
                        }

                    Box(
                        modifier =
                            Modifier.fillMaxWidth().height(50.dp).background(color).padding(10.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).requestFocus()
        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageDown) }

        runOnIdle { assertThat(scrollAmount).isLessThan(0f) }

        scrollAmount = 0f

        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageUp) }

        runOnIdle { assertThat(scrollAmount).isGreaterThan(0f) }
    }

    @Test
    fun scrollable_verticalScroll_reversed() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Vertical,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isLessThan(0)
                total
            }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_verticalScroll_reversed_mouseWheel() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Vertical,
            )
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal =
            runOnIdle {
                assertThat(total).isLessThan(0)
                total
            }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_disabledWontCallLambda() = runComposeUiTest {
        val enabled = mutableStateOf(true)
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                enabled = enabled.value,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        val prevTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0f)
                enabled.value = false
                total
            }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_startWithoutSlop_ifFlinging() = runComposeUiTest {
        mainClock.autoAdvance = false
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(state = controller, orientation = Orientation.Horizontal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        val expected = prevAfterSomeFling + 115
        assertThat(total).isEqualTo(expected)
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_blocksDownEvents_ifFlingingCaught() = runComposeUiTest {
        mainClock.autoAdvance = false
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(orientation = Orientation.Horizontal, state = controller),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp).testTag(scrollableBoxTag).clickable {
                                fail("Clickable shouldn't click when fling caught")
                            }
                    )
                }
            }
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            up()
        }
        // shouldn't assert in clickable lambda
    }

    @Test
    fun scrollable_snappingScrolling() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollableContent {
            Modifier.scrollable(orientation = Orientation.Vertical, state = controller)
        }
        waitForIdle()
        assertThat(total).isEqualTo(0f)

        scope.launch { controller.animateScrollBy(1000f) }
        waitForIdle()
        assertEquals(total, 1000f, 0.001f)

        scope.launch { controller.animateScrollBy(-200f) }
        waitForIdle()
        assertEquals(total, 800f, 0.001f)
    }

    @Test
    fun scrollable_explicitDisposal() = runComposeUiTest {
        mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    assertWithMessage("Animating after dispose!")
                        .that(expectEmission.value)
                        .isTrue()
                    total += it
                    it
                }
            )
        setScrollableContent {
            if (emit.value) {
                Modifier.scrollable(orientation = Orientation.Horizontal, state = controller)
            } else {
                Modifier
            }
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total).isGreaterThan(0f)

        // start the fling for a few frames
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        // flip the emission
        runOnUiThread { emit.value = false }
        // propagate the emit flip and record the value
        mainClock.advanceTimeByFrame()
        val prevTotal = total
        // make sure we don't receive any deltas
        runOnUiThread { expectEmission.value = false }

        // pump the clock until idle
        mainClock.autoAdvance = true
        waitForIdle()

        // still same and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    fun scrollable_nestedDrag() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Horizontal),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(300.dp)
                                .scrollable(
                                    state = innerState,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
                endVelocity = 0f,
            )
        }
        val lastEqualDrag =
            runOnIdle {
                assertThat(innerDrag).isGreaterThan(0f)
                assertThat(outerDrag).isGreaterThan(0f)
                // we consumed half delta in child, so exactly half should go to the parent
                assertThat(outerDrag).isEqualTo(innerDrag)
                innerDrag
            }
        runOnIdle {
            // values should be the same since no fling
            assertThat(innerDrag).isEqualTo(lastEqualDrag)
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForMouseWheel() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Since the child has already consumed half, the parent will consume the rest.
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Child consumes half, leaving the rest for the parent to consume.
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Horizontal),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(300.dp)
                                .scrollable(
                                    state = innerState,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-200f, ScrollWheel.Horizontal)
        }
        runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // Since child (inner) consumes half of the scroll, the parent (outer) consumes the
            // remainder (which is half as well), so they will be equal.
            assertThat(innerDrag).isEqualTo(outerDrag)
            innerDrag
        }
    }

    /*
     * Note: For keyboard scrolling to work (that is, scrolling based on the page up/down keys),
     * at least one child within the scrollable must be focusable. (This matches the behavior in
     * Views.)
     */
    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForKeyboardPageUpAndDown() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Since the child has already consumed half, the parent will consume the rest.
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Child consumes half, leaving the rest for the parent to consume.
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Horizontal),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .scrollable(
                                    state = innerState,
                                    orientation = Orientation.Horizontal,
                                )
                    ) {
                        Box(
                            modifier =
                                Modifier.testTag(scrollableBoxTag)
                                    // Required for keyboard scrolling (page up/down keys) to work.
                                    .focusable()
                                    .size(300.dp)
                        )
                    }
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).requestFocus()
        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageDown) }

        runOnIdle {
            assertThat(outerDrag).isLessThan(0f)
            assertThat(innerDrag).isLessThan(0f)
            // Since child (inner) consumes half of the scroll, the parent (outer) consumes the
            // remainder (which is half as well), so they will be equal.
            assertThat(innerDrag).isEqualTo(outerDrag)
        }

        outerDrag = 0f
        innerDrag = 0f

        onNodeWithTag(scrollableBoxTag).performKeyInput { pressKey(Key.PageUp) }

        runOnIdle {
            assertThat(outerDrag).isGreaterThan(0f)
            assertThat(innerDrag).isGreaterThan(0f)
            // Since child (inner) consumes half of the scroll, the parent (outer) consumes the
            // remainder (which is half as well), so they will be equal.
            assertThat(innerDrag).isEqualTo(outerDrag)
        }
    }

    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForSemantics_horizontal() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Since the child has already consumed half, the parent will consume the rest.
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    // Child consumes half, leaving the rest for the parent to consume.
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Horizontal),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(300.dp)
                                .scrollable(
                                    state = innerState,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }
        onNodeWithTag(scrollableBoxTag).performSemanticsAction(ScrollBy) {
            it.invoke(200f, 0f)
        }

        runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            assertThat(innerDrag).isEqualTo(outerDrag)
            innerDrag
        }
    }

    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForSemantics_vertical() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Vertical),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(300.dp)
                                .scrollable(state = innerState, orientation = Orientation.Vertical)
                    )
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).performSemanticsAction(ScrollBy) {
            it.invoke(0f, 200f)
        }

        runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            assertThat(innerDrag).isEqualTo(outerDrag)
            innerDrag
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun focusScroll_nestedScroll_childPartialConsumptionForSemantics() = runComposeUiTest {
        var outerDrag = 0f
        val requester = BringIntoViewRequester()
        val connection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    outerDrag += available.x
                    return super.onPreScroll(available, source)
                }
            }
        val scrollState = ScrollState(0)
        setContentAndGetScope {
            Box(Modifier.nestedScroll(connection)) {
                Row(modifier = Modifier.size(300.dp).horizontalScroll(scrollState)) {
                    repeat(5) { Box(modifier = Modifier.testTag(scrollableBoxTag).size(100.dp)) }
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(100.dp)
                                .bringIntoViewRequester(requester)
                    )
                }
            }
        }

        runOnIdle { scope.launch { requester.bringIntoView() } }

        runOnIdle {
            assertThat(outerDrag).isNonZero()
            assertEquals(outerDrag, -scrollState.value.toFloat(), 1f)
        }
    }

    @Test
    fun scrollable_nestedFling() = runComposeUiTest {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState =
            ScrollableState(
                consumeScrollDelta = {
                    outerDrag += it
                    it
                }
            )
        val innerState =
            ScrollableState(
                consumeScrollDelta = {
                    innerDrag += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = outerState, orientation = Orientation.Horizontal),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(300.dp)
                                .scrollable(
                                    state = innerState,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }

        // swipe again with velocity
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
            )
        }
        assertThat(innerDrag).isGreaterThan(0f)
        assertThat(outerDrag).isGreaterThan(0f)
        // we consumed half delta in child, so exactly half should go to the parent
        assertThat(outerDrag).isEqualTo(innerDrag)
        val lastEqualDrag = innerDrag
        runOnIdle {
            assertThat(innerDrag).isGreaterThan(lastEqualDrag)
            assertThat(outerDrag).isGreaterThan(lastEqualDrag)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_respectsPreConsumption() = runComposeUiTest {
        var value = 0f
        var lastReceivedPreScrollAvailable = 0f
        val preConsumeFraction = 0.7f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                    assertEquals(expected, it, 0.01f)
                    value += it
                    it
                }
            )
        val preConsumingParent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    lastReceivedPreScrollAvailable = available.x
                    return available * preConsumeFraction
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    // consume all velocity
                    return available
                }
            }

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).nestedScroll(preConsumingParent),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
            )
        }

        val preFlingValue = runOnIdle { value }
        runOnIdle {
            // if scrollable respects pre-fling consumption, it should fling 0px since we
            // pre-consume all
            assertThat(preFlingValue).isEqualTo(value)
        }
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_nestedScrollAbove_proxiesPostCycles() = runComposeUiTest {
        var value = 0f
        var expectedLeft = 0f
        val velocityFlung = 5000f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    val toConsume = it * 0.345f
                    value += toConsume
                    expectedLeft = it - toConsume
                    toConsume
                }
            )
        val parent =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // we should get in post scroll as much as left in controller callback
                    assertThat(available.x).isEqualTo(expectedLeft)
                    return if (source == NestedScrollSource.SideEffect) Offset.Zero else available
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    val expected = velocityFlung - consumed.x
                    assertThat(consumed.x).isLessThan(velocityFlung)
                    assertThat(abs(available.x - expected)).isLessThan(0.1f)
                    return available
                }
            }

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).nestedScroll(parent),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung,
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    @Ignore // TODO https://youtrack.jetbrains.com/issue/CMP-2965
    fun scrollable_nestedScrollAbove_reversed_proxiesPostCycles() = runComposeUiTest {
        var value = 0f
        var expectedLeft = 0f
        val velocityFlung = 5000f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    val toConsume = it * 0.345f
                    value += toConsume
                    expectedLeft = it - toConsume
                    toConsume
                }
            )
        val parent =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // we should get in post scroll as much as left in controller callback
                    assertThat(available.x).isEqualTo(-expectedLeft)
                    return if (source == NestedScrollSource.SideEffect) Offset.Zero else available
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    val expected = velocityFlung - consumed.x
                    assertThat(consumed.x).isLessThan(velocityFlung)
                    assertThat(abs(available.x - expected)).isLessThan(0.1f)
                    return available
                }
            }

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).nestedScroll(parent),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    reverseDirection = true,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung,
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollBelow_listensDispatches() = runComposeUiTest {
        var value = 0f
        var expectedConsumed = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    expectedConsumed = it * 0.3f
                    value += expectedConsumed
                    expectedConsumed
                }
            )
        val child = object : NestedScrollConnection {}
        val dispatcher = NestedScrollDispatcher()

        setContentAndGetScope {
            Box {
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(state = controller, orientation = Orientation.Horizontal)
                ) {
                    Box(
                        Modifier.size(200.dp)
                            .testTag(scrollableBoxTag)
                            .nestedScroll(child, dispatcher)
                    )
                }
            }
        }

        val lastValueBeforeFling =
            runOnIdle {
                val preScrollConsumed =
                    dispatcher.dispatchPreScroll(Offset(20f, 20f), NestedScrollSource.UserInput)
                // scrollable is not interested in pre scroll
                assertThat(preScrollConsumed).isEqualTo(Offset.Zero)

                val consumed =
                    dispatcher.dispatchPostScroll(
                        Offset(20f, 20f),
                        Offset(50f, 50f),
                        NestedScrollSource.UserInput,
                    )
                assertEquals(expectedConsumed, consumed.x, 0.001f)
                value
            }

        scope.launch {
            val preFlingConsumed = dispatcher.dispatchPreFling(Velocity(50f, 50f))
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }
        waitForIdle()

        scope.launch {
            dispatcher.dispatchPostFling(Velocity(1000f, 1000f), Velocity(2000f, 2000f))
        }

        runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value).isGreaterThan(lastValueBeforeFling)
        }
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() = runComposeUiTest {
        var childValue = 0f
        var parentValue = 0f
        val childController =
            ScrollableState(
                consumeScrollDelta = {
                    childValue += it
                    it
                }
            )
        val parentController =
            ScrollableState(
                consumeScrollDelta = {
                    parentValue += it
                    it
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(
                                state = parentController,
                                orientation = Orientation.Horizontal,
                            )
                ) {
                    Box(
                        Modifier.size(200.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                enabled = false,
                                orientation = Orientation.Horizontal,
                                state = childController,
                            )
                    )
                }
            }
        }

        runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipe(center, center.copy(x = center.x + 100f))
        }

        runOnIdle {
            assertThat(childValue).isEqualTo(0f)
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedScroll_disabledConnectionNoOp() = runComposeUiTest {
        var childValue = 0f
        var parentValue = 0f
        var selfValue = 0f
        val childController =
            ScrollableState(
                consumeScrollDelta = {
                    childValue += it / 2
                    it / 2
                }
            )
        val middleController =
            ScrollableState(
                consumeScrollDelta = {
                    selfValue += it / 2
                    it / 2
                }
            )
        val parentController =
            ScrollableState(
                consumeScrollDelta = {
                    parentValue += it / 2
                    it / 2
                }
            )

        setContentAndGetScope {
            Box {
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .scrollable(
                                state = parentController,
                                orientation = Orientation.Horizontal,
                            )
                ) {
                    Box(
                        Modifier.size(200.dp)
                            .scrollable(
                                enabled = false,
                                orientation = Orientation.Horizontal,
                                state = middleController,
                            )
                    ) {
                        Box(
                            Modifier.size(200.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    orientation = Orientation.Horizontal,
                                    state = childController,
                                )
                        )
                    }
                }
            }
        }

        runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(selfValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipe(center, center.copy(x = center.x + 100f))
        }

        runOnIdle {
            assertThat(childValue).isGreaterThan(0f)
            // disabled middle node doesn't consume
            assertThat(selfValue).isEqualTo(0f)
            // but allow nested scroll to propagate up correctly
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedFlingCancellation_shouldPreventDeltasFromPropagating() = runComposeUiTest {
        var childDeltas = 0f
        var touchSlop = 0f
        val childController = ScrollableState {
            childDeltas += it
            it
        }
        val flingCancellationParent =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.SideEffect && available != Offset.Zero) {
                        throw CancellationException()
                    }
                    return Offset.Zero
                }
            }

        setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(modifier = Modifier.nestedScroll(flingCancellationParent)) {
                Box(
                    modifier =
                        Modifier.size(600.dp)
                            .testTag("childScrollable")
                            .scrollable(childController, Orientation.Horizontal)
                )
            }
        }

        // First drag, this won't trigger the cancellation flow.
        onNodeWithTag("childScrollable").performTouchInput {
            down(centerLeft)
            moveBy(Offset(100f, 0f))
            move(100)
            up()
        }

        runOnIdle { assertThat(childDeltas).isEqualTo(100f - touchSlop) }

        childDeltas = 0f
        var dragged = 0f
        onNodeWithTag("childScrollable").performTouchInput {
            swipeWithVelocity(centerLeft, centerRight, 2000f)
            dragged = centerRight.x - centerLeft.x
        }

        // child didn't receive more deltas after drag, because fling was cancelled by the parent
        assertThat(childDeltas).isEqualTo(dragged - touchSlop)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollable_nestedFling_shouldCancelWhenHitTheBounds_ifRemoved() = runComposeUiTest {

        var shouldEmit by mutableStateOf(true)
        var latestScroll = Offset.Zero
        val connection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    latestScroll += available
                    return super.onPreScroll(available, source)
                }
            }

        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.nestedScroll(connection)) {
                if (shouldEmit) {
                    Column(
                        Modifier.testTag("column")
                            .verticalScroll(
                                rememberScrollState(with(density) { (5 * 200.dp).roundToPx() })
                            )
                    ) {
                        repeat(10) { Box(Modifier.size(200.dp)) }
                    }
                }
            }
        }
        var swipeSize = 0f
        onNodeWithTag("column").performTouchInput {
            swipeSize = bottom - top
            swipeDown()
        }

        mainClock.advanceTimeUntil { latestScroll.y.absoluteValue > swipeSize }
        runOnIdle { shouldEmit = false }
        mainClock.advanceTimeByFrame()
        latestScroll = Offset.Zero

        mainClock.autoAdvance = true
        runOnIdle { assertThat(latestScroll).isEqualTo(Offset.Zero) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollable_nestedFling_shouldContinueSendingDeltasWhenHitBounds() = runComposeUiTest {

        var flingDeltas = Offset.Zero
        val connection =
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.SideEffect) flingDeltas += available
                    return available
                }
            }

        val scrollState = ScrollState(with(density) { (5 * 200.dp).roundToPx() })
        setContent {
            Box(Modifier.nestedScroll(connection)) {
                Column(Modifier.testTag("column").verticalScroll(scrollState)) {
                    repeat(10) { Box(Modifier.size(200.dp)) }
                }
            }
        }

        mainClock.autoAdvance = false
        onNodeWithTag("column").performTouchInput { swipeDown(topCenter.y, bottomCenter.y) }

        mainClock.advanceTimeUntil { scrollState.value == 0 } // hit the bounds
        flingDeltas = Offset.Zero

        mainClock.autoAdvance = true
        waitForIdle()
        assertThat(flingDeltas.y).isNonZero()
    }

    @Test
    fun scrollable_nestedFling_parentShouldFlingWithVelocityLeft() = runComposeUiTest {
        var postFlingCalled = false
        var lastPostFlingVelocity = Velocity.Zero
        var flingDelta = 0.0f
        val fling =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    assertThat(initialVelocity).isEqualTo(lastPostFlingVelocity.y)
                    scrollBy(100f)
                    return initialVelocity
                }
            }
        val topConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // accumulate deltas for second fling only
                    if (source == NestedScrollSource.SideEffect && postFlingCalled) {
                        flingDelta += available.y
                    }
                    return super.onPreScroll(available, source)
                }
            }

        val middleConnection =
            object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    postFlingCalled = true
                    lastPostFlingVelocity = available
                    return super.onPostFling(consumed, available)
                }
            }
        val columnState = ScrollState(with(density) { (5 * 200.dp).roundToPx() })
        setContent {
            Box(
                Modifier.nestedScroll(topConnection)
                    .scrollable(
                        flingBehavior = fling,
                        state = rememberScrollableState { 0f },
                        orientation = Orientation.Vertical,
                    )
            ) {
                Column(
                    Modifier.nestedScroll(middleConnection)
                        .testTag("column")
                        .verticalScroll(columnState)
                ) {
                    repeat(10) { Box(Modifier.size(200.dp)) }
                }
            }
        }

        onNodeWithTag("column").performTouchInput { swipeDown() }

        runOnIdle {
            assertThat(columnState.value).isEqualTo(0) // column is at the bounds
            assertThat(postFlingCalled)
                .isTrue() // we fired a post fling call after the cancellation
            assertThat(lastPostFlingVelocity.y)
                .isNonZero() // the post child fling velocity was not zero
            assertThat(flingDelta).isEqualTo(100f) // the fling delta as propagated correctly
        }
    }

    @Test
    fun scrollable_nestedFling_parentShouldFlingWithVelocityLeft_whenInnerDisappears() = runComposeUiTest {
        var postFlingCalled = false
        var postFlingAvailableVelocity = Velocity.Zero
        var postFlingConsumedVelocity = Velocity.Zero
        var flingDelta by mutableFloatStateOf(0.0f)
        var preFlingVelocity = Velocity.Zero

        val topConnection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // accumulate deltas for second fling only
                    if (source == NestedScrollSource.SideEffect) {
                        flingDelta += available.y
                    }
                    return super.onPreScroll(available, source)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    preFlingVelocity = available
                    return super.onPreFling(available)
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    postFlingCalled = true
                    postFlingAvailableVelocity = available
                    postFlingConsumedVelocity = consumed
                    return super.onPostFling(consumed, available)
                }
            }

        val columnState = ScrollState(with(density) { (50 * 200.dp).roundToPx() })

        setContent {
            Box(Modifier.nestedScroll(topConnection)) {
                if (flingDelta.absoluteValue < 100) {
                    Column(Modifier.testTag("column").verticalScroll(columnState)) {
                        repeat(100) { Box(Modifier.size(200.dp)) }
                    }
                }
            }
        }

        onNodeWithTag("column").performTouchInput { swipeUp() }
        waitForIdle()
        // removed scrollable
        onNodeWithTag("column").assertDoesNotExist()
        runOnIdle {
            // we fired a post fling call after the disappearance
            assertThat(postFlingCalled).isTrue()

            // fling velocity in onPostFling is correctly propagated
            assertThat(postFlingConsumedVelocity + postFlingAvailableVelocity)
                .isEqualTo(preFlingVelocity)
        }
    }

    @Test
    fun scrollable_bothOrientations_proxiesPostFling() = runComposeUiTest {
        val velocityFlung = 5000f
        val outerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerFlingBehavior =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return initialVelocity
                }
            }
        val parent =
            object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    assertThat(consumed.x).isEqualTo(0f)
                    assertEquals(available.x, velocityFlung, 0.1f)
                    return available
                }
            }

        setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.size(300.dp)
                            .nestedScroll(parent)
                            .scrollable(state = outerState, orientation = Orientation.Vertical),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = innerState,
                                    flingBehavior = innerFlingBehavior,
                                    orientation = Orientation.Horizontal,
                                )
                    )
                }
            }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung,
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    fun scrollable_interactionSource() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        setScrollableContent {
            Modifier.scrollable(
                interactionSource = interactionSource,
                orientation = Orientation.Horizontal,
                state = controller,
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is DragInteraction.Start }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput { up() }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Stop }
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_interactionSource_resetWhenDisposed() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        setContentAndGetScope {
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollableBoxTag)
                                .size(100.dp)
                                .scrollable(
                                    interactionSource = interactionSource,
                                    orientation = Orientation.Horizontal,
                                    state = controller,
                                )
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions.first() is DragInteraction.Start }
        }

        // Dispose scrollable
        runOnIdle { emitScrollableBox = false }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions.first() is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Cancel }
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_whenVelocity0() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    flingCalled++
                    flingVelocity = initialVelocity
                    return 0f
                }
            }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            move(100)
            up()
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isLessThan(0.01f)
        assertThat(flingVelocity).isGreaterThan(-0.01f)
    }

    @Test
    fun scrollable_flingBehaviourCalled() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    flingCalled++
                    flingVelocity = initialVelocity
                    return 0f
                }
            }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(this.center, this.center + Offset(115f, 0f), endVelocity = 1000f)
        }
        assertThat(flingCalled).isEqualTo(1)
        assertEquals(1000f, flingVelocity, 5f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed() = runComposeUiTest {
        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    flingCalled++
                    flingVelocity = initialVelocity
                    return 0f
                }
            }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                reverseDirection = true,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(this.center, this.center + Offset(115f, 0f), endVelocity = 1000f)
        }
        assertThat(flingCalled).isEqualTo(1)
        assertEquals(flingVelocity, -1000f, 5f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_correctScope() = runComposeUiTest {
        var total = 0f
        var returned = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    returned = scrollBy(123f)
                    return 0f
                }
            }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal =
            runOnIdle {
                assertThat(total).isGreaterThan(0f)
                total
            }

        onNodeWithTag(scrollableBoxTag).performTouchInput { up() }

        runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed_correctScope() = runComposeUiTest {
        var total = 0f
        var returned = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        val flingBehaviour =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    returned = scrollBy(123f)
                    return 0f
                }
            }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                reverseDirection = true,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal,
            )
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal =
            runOnIdle {
                assertThat(total).isLessThan(0f)
                total
            }

        onNodeWithTag(scrollableBoxTag).performTouchInput { up() }

        runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_scrollByWorksWithRepeatableAnimations() = runComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setContentAndGetScope {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .scrollable(state = controller, orientation = Orientation.Horizontal)
            )
        }

        runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    100f,
                    keyframes {
                        durationMillis = 2500
                        // emulate a repeatable animation:
                        0f at 0
                        100f at 500
                        100f at 1000
                        0f at 1500
                        0f at 2000
                        100f at 2500
                    },
                )
            }
        }

        mainClock.advanceTimeBy(250)
        runOnIdle {
            // in the middle of the first animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 750 ms
        runOnIdle {
            // first animation finished
            assertThat(total).isEqualTo(100f)
        }

        mainClock.advanceTimeBy(250) // 1250 ms
        runOnIdle {
            // in the middle of the second animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 1750 ms
        runOnIdle {
            // second animation finished
            assertThat(total).isEqualTo(0f)
        }

        mainClock.advanceTimeBy(500) // 2250 ms
        runOnIdle {
            // in the middle of the third animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 2750 ms
        runOnIdle {
            // third animation finished
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun scrollable_cancellingAnimateScrollUpdatesIsScrollInProgress() = runComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setContentAndGetScope {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .scrollable(state = controller, orientation = Orientation.Horizontal)
            )
        }

        lateinit var animateJob: Job

        runOnIdle {
            animateJob = scope.launch { controller.animateScrollBy(100f, tween(1000)) }
        }

        mainClock.advanceTimeBy(500)
        runOnIdle { assertThat(controller.isScrollInProgress).isTrue() }

        // Stop halfway through the animation
        animateJob.cancel()

        runOnIdle { assertThat(controller.isScrollInProgress).isFalse() }
    }

    @Test
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() = runComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller =
            ScrollableState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setContentAndGetScope {
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .scrollable(state = controller, orientation = Orientation.Horizontal)
            )
        }

        runOnIdle { scope.launch { controller.animateScrollBy(100f, tween(1000)) } }

        mainClock.advanceTimeBy(500)
        runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
            assertThat(controller.isScrollInProgress).isTrue()
            scope.launch { controller.animateScrollBy(-100f, tween(1000)) }
        }

        runOnIdle { assertThat(controller.isScrollInProgress).isTrue() }

        mainClock.advanceTimeBy(1000)
        mainClock.advanceTimeByFrame()

        runOnIdle {
            assertThat(total).isGreaterThan(-75f)
            assertThat(total).isLessThan(0f)
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_multiDirectionsShouldPropagateOrthogonalAxisToNextParentWithSameDirection() = runComposeUiTest {
        var innerDelta = 0f
        var middleDelta = 0f
        var outerDelta = 0f

        val outerStateController = ScrollableState {
            outerDelta += it
            it
        }

        val middleController = ScrollableState {
            middleDelta += it
            it / 2
        }

        val innerController = ScrollableState {
            innerDelta += it
            it / 2
        }

        setContentAndGetScope {
            Box(
                modifier =
                    Modifier.testTag("outerScrollable")
                        .size(300.dp)
                        .scrollable(outerStateController, orientation = Orientation.Horizontal)
            ) {
                Box(
                    modifier =
                        Modifier.testTag("middleScrollable")
                            .size(300.dp)
                            .scrollable(middleController, orientation = Orientation.Vertical)
                ) {
                    Box(
                        modifier =
                            Modifier.testTag("innerScrollable")
                                .size(300.dp)
                                .scrollable(innerController, orientation = Orientation.Horizontal)
                    )
                }
            }
        }

        onNodeWithTag("innerScrollable").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }

        runOnIdle {
            assertThat(innerDelta).isGreaterThan(0f)
            assertThat(middleDelta).isEqualTo(0f)
            assertThat(outerDelta).isEqualTo(innerDelta / 2f)
        }
    }

    @Test
    fun nestedScrollable_noFlingContinuationInCrossAxis_shouldAllowClicksOnCrossAxis_scrollable() = runComposeUiTest {
        var clicked = 0
        setContentAndGetScope {
            LazyColumn(Modifier.testTag("column")) {
                item {
                    Box(
                        modifier =
                            Modifier.size(20.dp).background(Color.Red).clickable { clicked++ }
                    )
                }
                item {
                    LazyRow(Modifier.testTag("list")) {
                        items(100) { Box(modifier = Modifier.size(20.dp).background(Color.Blue)) }
                    }
                }
            }
        }

        mainClock.autoAdvance = false
        onNodeWithTag("list", useUnmergedTree = true).performTouchInput { swipeLeft() }

        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        onNodeWithTag("column").performTouchInput { click(Offset(10f, 10f)) }

        mainClock.autoAdvance = true

        runOnIdle { assertThat(clicked).isEqualTo(1) }
    }

    // b/179417109 Double checks that in a nested scroll cycle, the parent post scroll
    // consumption is taken into consideration.
    @Test
    fun dispatchScroll_shouldReturnConsumedDeltaInNestedScrollChain() = runComposeUiTest {
        var consumedInner = 0f
        var consumedOuter = 0f
        var touchSlop = 0f

        var preScrollAvailable = Offset.Zero
        var consumedPostScroll = Offset.Zero
        var postScrollAvailable = Offset.Zero

        val outerStateController = ScrollableState {
            consumedOuter += it
            it
        }

        val innerController = ScrollableState {
            consumedInner += it / 2
            it / 2
        }

        val connection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    preScrollAvailable += available
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    consumedPostScroll += consumed
                    postScrollAvailable += available
                    return Offset.Zero
                }
            }

        setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(modifier = Modifier.nestedScroll(connection)) {
                Box(
                    modifier =
                        Modifier.testTag("outerScrollable")
                            .size(300.dp)
                            .scrollable(outerStateController, orientation = Orientation.Horizontal)
                ) {
                    Box(
                        modifier =
                            Modifier.testTag("innerScrollable")
                                .size(300.dp)
                                .scrollable(innerController, orientation = Orientation.Horizontal)
                    )
                }
            }
        }

        val scrollDelta = 200f

        onRoot().performTouchInput {
            down(center)
            moveBy(Offset(scrollDelta, 0f))
            move(100)
            up()
        }

        runOnIdle {
            assertThat(consumedInner).isGreaterThan(0)
            assertThat(consumedOuter).isGreaterThan(0)
            assertThat(touchSlop).isGreaterThan(0)
            assertThat(postScrollAvailable.x).isEqualTo(0f)
            assertThat(consumedPostScroll.x).isEqualTo(scrollDelta - touchSlop)
            assertThat(preScrollAvailable.x).isEqualTo(scrollDelta - touchSlop)
            assertThat(scrollDelta).isEqualTo(consumedInner + consumedOuter + touchSlop)
        }
    }

    @Test
    fun testInspectorValue() = runComposeUiTest {
        val controller = ScrollableState(consumeScrollDelta = { it })
        setContentAndGetScope {
            val modifier =
                Modifier.scrollable(controller, Orientation.Vertical).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "orientation",
                    "state",
                    "overscrollEffect",
                    "enabled",
                    "reverseDirection",
                    "flingBehavior",
                    "interactionSource",
                    "bringIntoViewSpec",
                )
        }
    }

    @Test
    fun producingEqualMaterializedModifierAfterRecomposition() = runComposeUiTest {
        val state = ScrollableState { it }
        val counter = mutableStateOf(0)
        var materialized: Modifier? = null

        setContent {
            counter.value // just to trigger recomposition
            materialized =
                currentComposer.materialize(Modifier.scrollable(state, Orientation.Vertical, null))
        }

        lateinit var first: Modifier
        runOnIdle {
            first = requireNotNull(materialized)
            materialized = null
            counter.value++
        }

        runOnIdle {
            val second = requireNotNull(materialized)
            assertThat(first).isEqualTo(second)
        }
    }

    @Test
    fun focusStaysInScrollableEvenThoughThereIsACloserItemOutside() = runComposeUiTest {
        lateinit var focusManager: FocusManager
        val initialFocus = FocusRequester()
        var nextItemIsFocused = false
        setContent {
            focusManager = LocalFocusManager.current
            Column {
                Column(Modifier.size(10.dp).verticalScroll(rememberScrollState())) {
                    Box(Modifier.size(10.dp).focusRequester(initialFocus).focusable())
                    Box(Modifier.size(10.dp))
                    Box(
                        Modifier.size(10.dp)
                            .onFocusChanged { nextItemIsFocused = it.isFocused }
                            .focusable()
                    )
                }
                Box(Modifier.size(10.dp).focusable())
            }
        }

        runOnIdle { initialFocus.requestFocus() }
        runOnIdle { focusManager.moveFocus(FocusDirection.Down) }

        runOnIdle { assertThat(nextItemIsFocused).isTrue() }
    }

    @Test
    fun verticalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() = runComposeUiTest {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection =
            object : NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    velocity += available
                    return Velocity.Zero
                }
            }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier.pointerInput(Unit) { savePointerInputEvents(tracker, this) }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Vertical)
        }

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeUp() }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeDown() }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun horizontalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() = runComposeUiTest {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection =
            object : NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    velocity += available
                    return Velocity.Zero
                }
            }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier.pointerInput(Unit) { savePointerInputEvents(tracker, this) }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Horizontal)
        }

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeLeft() }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeRight() }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() = runComposeUiTest {

        val controller = ScrollableState { 0f }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        setScrollableContent {
            defaultFlingBehavior = rememberDefaultFlingBehavior()
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                flingBehavior = defaultFlingBehavior,
            )
        }

        scope.launch {
            controller.scroll { defaultFlingBehavior?.let { with(it) { performFling(1000f) } } }
        }

        runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        // Simulate turning of animation
        scope.launch {
            controller.scroll {
                withContext(TestScrollMotionDurationScale(0f)) {
                    defaultFlingBehavior?.let { with(it) { performFling(1000f) } }
                }
            }
        }

        runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }
    }

    @Test
    fun defaultFlingBehavior_useScrollMotionDurationScale() = runComposeUiTest {

        val controller = ScrollableState { 0f }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        var switchMotionDurationScale by mutableStateOf(true)

        setContentAndGetScope {
            val flingSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
            if (switchMotionDurationScale) {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(1f))
                Box(
                    modifier =
                        Modifier.testTag(scrollableBoxTag)
                            .size(100.dp)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal,
                                flingBehavior = defaultFlingBehavior,
                            )
                )
            } else {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(0f))
                Box(
                    modifier =
                        Modifier.testTag(scrollableBoxTag)
                            .size(100.dp)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal,
                                flingBehavior = defaultFlingBehavior,
                            )
                )
            }
        }

        scope.launch {
            controller.scroll { defaultFlingBehavior?.let { with(it) { performFling(1000f) } } }
        }

        runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        switchMotionDurationScale = false
        waitForIdle()

        scope.launch {
            controller.scroll { defaultFlingBehavior?.let { with(it) { performFling(1000f) } } }
        }

        runOnIdle { assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isEqualTo(1) }
    }

    @Test
    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease() = runComposeUiTest {
        val scrollState = ScrollState(0)
        val delta = 10f
        var touchSlop = 0f
        setScrollableContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.scrollable(scrollState, Orientation.Vertical)
        }
        var previousScrollValue = 0
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            // generate various move events
            repeat(30) {
                moveBy(Offset(0f, delta), delayMillis = 8L)
                previousScrollValue += delta.toInt()
            }
            // stop for a moment
            advanceEventTime(3000L)
            up()
        }

        runOnIdle {
            assertEquals((previousScrollValue - touchSlop).toInt(), scrollState.value)
        }
    }

    @Test
    fun defaultScrollableState_scrollByWithNan_shouldFilterOutNan() = runComposeUiTest {
        val controller = ScrollableState {
            assertThat(it).isNotNaN()
            0f
        }

        val nanGenerator =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return scrollBy(Float.NaN)
                }
            }

        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                flingBehavior = nanGenerator,
            )
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeLeft() }
    }

    @Test
    fun equalInputs_shouldResolveToEquals() = runComposeUiTest {
        val state = ScrollableState { 0f }

        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.scrollable(state, Orientation.Horizontal)
            } else {
                Modifier.scrollable(state, Orientation.Vertical)
            }
        }
    }

    @Test
    fun scrollableState_checkLastScrollDirection() = runComposeUiTest {
        val controller = ScrollableState { it }

        setScrollableContent {
            Modifier.scrollable(orientation = Orientation.Horizontal, state = controller)
        }

        // Assert both isLastScrollForward and isLastScrollBackward are false before any scroll
        runOnIdle {
            assertThat(controller.lastScrolledForward).isFalse()
            assertThat(controller.lastScrolledBackward).isFalse()
        }

        lateinit var animateJob: Job

        runOnIdle {
            animateJob = scope.launch { controller.animateScrollBy(100f, tween(1000)) }
        }

        mainClock.advanceTimeBy(500)

        // Assert isLastScrollForward is true during forward-scroll and isLastScrollBackward is
        // false
        runOnIdle {
            assertThat(controller.lastScrolledForward).isTrue()
            assertThat(controller.lastScrolledBackward).isFalse()
        }

        // Stop halfway through the animation
        animateJob.cancel()

        // Assert isLastScrollForward is true after forward-scroll and isLastScrollBackward is false
        runOnIdle {
            assertThat(controller.lastScrolledForward).isTrue()
            assertThat(controller.lastScrolledBackward).isFalse()
        }

        runOnIdle {
            animateJob = scope.launch { controller.animateScrollBy(-100f, tween(1000)) }
        }

        mainClock.advanceTimeBy(500)

        // Assert isLastScrollForward is false during backward-scroll and isLastScrollBackward is
        // true
        runOnIdle {
            assertThat(controller.lastScrolledForward).isFalse()
            assertThat(controller.lastScrolledBackward).isTrue()
        }

        // Stop halfway through the animation
        animateJob.cancel()

        // Assert isLastScrollForward is false after backward-scroll and isLastScrollBackward is
        // true
        runOnIdle {
            assertThat(controller.lastScrolledForward).isFalse()
            assertThat(controller.lastScrolledBackward).isTrue()
        }
    }

    @Test
    fun enabledChange_semanticsShouldBeCleared() = runComposeUiTest {
        var enabled by mutableStateOf(true)
        setContentAndGetScope {
            Box(
                modifier =
                    Modifier.testTag(scrollableBoxTag)
                        .size(100.dp)
                        .scrollable(
                            state = rememberScrollableState { it },
                            orientation = Orientation.Horizontal,
                            enabled = enabled,
                        )
            )
        }

        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy))
        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollByOffset))

        runOnIdle { enabled = false }

        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollBy))
        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollByOffset))

        runOnIdle { enabled = true }

        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy))
        onNodeWithTag(scrollableBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollByOffset))
    }

    @Test
    @IgnoreIosTarget // Fling behavior on iOS does not use screen density
    fun onDensityChange_shouldUpdateFlingBehavior() = runComposeUiTest {
        var density by mutableStateOf(density)
        var flingDelta = 0f
        val fixedSize = 400
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(
                    Modifier.size(with(density) { fixedSize.toDp() })
                        .testTag(scrollableBoxTag)
                        .scrollable(
                            state =
                                rememberScrollableState {
                                    flingDelta += it
                                    it
                                },
                            orientation = Orientation.Vertical,
                        )
                )
            }
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeUp() }

        waitForIdle()

        density = Density(density.density * 2f)
        val previousDelta = flingDelta
        flingDelta = 0.0f

        onNodeWithTag(scrollableBoxTag).performTouchInput { swipeUp() }

        runOnIdle { assertThat(flingDelta).isNotEqualTo(previousDelta) }
    }

    @Test
    fun onNestedFlingCancelled_shouldResetFlingState() = runComposeUiTest {
        mainClock.autoAdvance = false
        var outerStateDeltas = 0f
        val outerState = ScrollableState {
            outerStateDeltas += it
            it
        }

        val innerState = ScrollableState { it }

        val dispatcher = NestedScrollDispatcher()
        var flingJob: Job? = null

        setContentAndGetScope {
            Box(
                Modifier.size(400.dp)
                    .background(Color.Red)
                    .scrollable(
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        state = outerState,
                        orientation = Orientation.Vertical,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(200.dp)
                        .background(Color.Black)
                        .nestedScroll(
                            connection = object : NestedScrollConnection {},
                            dispatcher = dispatcher,
                        )
                        .scrollable(state = innerState, orientation = Orientation.Vertical)
                )
            }
        }

        runOnIdle {
            // causes the inner scrollable to dispatch a post fling to the outer scrollable
            flingJob =
                scope.launch {
                    innerState.scroll {
                        dispatcher.dispatchPreFling(Velocity(0f, 10000f))
                        dispatcher.dispatchPostFling(Velocity.Zero, Velocity(0f, 10000f))
                    }
                }
        }

        mainClock.advanceTimeBy(200L)

        runOnIdle {
            // outer scrollable is flinging from onPostFling
            assertThat(outerStateDeltas).isNonZero()
        }

        outerStateDeltas = 0f

        runOnIdle {
            flingJob?.cancel() // cancel job mid fling

            // try to run fling again
            scope.launch {
                innerState.scroll {
                    dispatcher.dispatchPreFling(Velocity(0f, 10000f))
                    dispatcher.dispatchPostFling(Velocity.Zero, Velocity(0f, 10000f))
                }
            }
        }

        mainClock.autoAdvance = true
        // fling reached outer scrollable even if the previous child fling was cancelled.
        runOnIdle {
            // outer scrollable is flinging from onPostFling
            assertThat(outerStateDeltas).isNonZero()
        }
    }

    private fun ComposeUiTest.setScrollableContent(
        enableInitialFocus: Boolean = false,
        scrollableModifierFactory: @Composable () -> Modifier,
    ) {
        setContentAndGetScope {
            Box {
                val scrollable = scrollableModifierFactory()
                val initialFocus =
                    if (enableInitialFocus) Modifier.focusRequester(focusRequester) else Modifier
                Box(modifier = Modifier.testTag(scrollableBoxTag).size(100.dp).then(scrollable)) {
                    if (enableInitialFocus) {
                        Box(modifier = Modifier.size(100.dp).then(initialFocus).focusable())
                    }
                }
            }
        }

        if (enableInitialFocus) {
            runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }
        }
    }

    @Composable
    private fun rememberDefaultFlingBehavior(): DefaultFlingBehavior {
        val flingSpec = rememberSplineBasedDecay<Float>()
        return remember(flingSpec) {
            DefaultFlingBehavior(flingSpec)
        }
    }
}

// Very low tolerance on the difference
internal const val VelocityTrackerCalculationThreshold = 1

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEvents(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope,
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event: PointerInputChange? = awaitFirstDown()
                    while (event != null && !event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes.firstOrNull()

                        if (currentEvent != null && !currentEvent.changedToUpIgnoreConsumed()) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(currentEvent.uptimeMillis, currentEvent.position)
                        }

                        event = currentEvent
                    }
                }
            }
        }
    }
}

internal suspend fun savePointerInputEventsWithFix(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope,
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event: PointerInputChange? = awaitFirstDown()
                    while (event != null && !event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes.firstOrNull()

                        if (currentEvent != null && !currentEvent.changedToUpIgnoreConsumed()) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(currentEvent.uptimeMillis, currentEvent.position)
                        }

                        event = currentEvent
                    }
                }
            }
        }
    }
}

internal suspend fun savePointerInputEventsLegacy(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope,
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event = awaitFirstDown()
                    tracker.addPosition(event.uptimeMillis, event.position)
                    while (!event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes.firstOrNull()

                        if (currentEvent != null) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(currentEvent.uptimeMillis, currentEvent.position)
                            event = currentEvent
                        }
                    }
                }
            }
        }
    }
}

internal class TestScrollMotionDurationScale(override val scaleFactor: Float) : MotionDurationScale