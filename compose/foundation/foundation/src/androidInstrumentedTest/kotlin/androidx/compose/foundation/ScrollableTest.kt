/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.DefaultFlingBehavior
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ModifierLocalScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
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
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.materialize
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ScrollableTest {

    @get:Rule
    val rule = createComposeRule()

    private val scrollableBoxTag = "scrollableBox"

    private lateinit var scope: CoroutineScope

    private fun ComposeContentTestRule.setContentAndGetScope(content: @Composable () -> Unit) {
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }
            content()
        }
    }

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollable_horizontalScroll() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_mouseWheel() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_mouseWheel_badMotionEvent() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Float.NaN, ScrollWheel.Horizontal)
        }

        assertThat(total).isEqualTo(0)
    }

    /*
     * Note: For keyboard scrolling to work (that is, scrolling based on the page up/down keys),
     * at least one child within the scrollable must be focusable. (This matches the behavior in
     * Views.)
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_keyboardPageUpAndDown() {
        var scrollAmount = 0f

        val scrollableState = ScrollableState(
            consumeScrollDelta = {
                scrollAmount += it
                it
            }
        )

        rule.setContent {
            Row(
                Modifier
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .background(Color.Red)
                    .scrollable(
                        state = scrollableState,
                        orientation = Orientation.Horizontal
                    )
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag(scrollableBoxTag)
                        .width(50.dp)
                        .background(Color.Blue)
                        // Required for keyboard scrolling (page up/down keys) to work.
                        .focusable()
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.size(10.dp))

                for (i in 0 until 40) {
                    val color = if (i % 2 == 0) {
                        Color.Yellow
                    } else {
                        Color.Green
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(50.dp)
                            .background(color)
                            .padding(10.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).requestFocus()
        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageDown)
        }

        rule.runOnIdle {
            assertThat(scrollAmount).isLessThan(0f)
        }

        scrollAmount = 0f

        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageUp)
        }

        rule.runOnIdle {
            assertThat(scrollAmount).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_horizontalScroll_reverse() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_reverse_mouseWheel() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_mouseWheel() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_mouseWheel_badMotionEvent() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(Float.NaN, ScrollWheel.Vertical)
        }

        assertThat(total).isEqualTo(0)
    }

    /*
     * Note: For keyboard scrolling to work (that is, scrolling based on the page up/down keys),
     * at least one child within the scrollable must be focusable. (This matches the behavior in
     * Views.)
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_keyboardPageUpAndDown() {
        var scrollAmount = 0f

        val scrollableState = ScrollableState(
            consumeScrollDelta = {
                scrollAmount += it
                it
            }
        )

        rule.setContent {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .scrollable(
                        state = scrollableState,
                        orientation = Orientation.Vertical
                    )
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(scrollableBoxTag)
                        .height(50.dp)
                        .background(Color.Blue)
                        // Required for keyboard scrolling (page up/down keys) to work.
                        .focusable()
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.size(10.dp))

                for (i in 0 until 40) {
                    val color = if (i % 2 == 0) {
                        Color.Yellow
                    } else {
                        Color.Green
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(color)
                            .padding(10.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).requestFocus()
        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageDown)
        }

        rule.runOnIdle {
            assertThat(scrollAmount).isLessThan(0f)
        }

        scrollAmount = 0f

        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageUp)
        }

        rule.runOnIdle {
            assertThat(scrollAmount).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_verticalScroll_reversed() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_reversed_mouseWheel() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_disabledWontCallLambda() {
        val enabled = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                enabled = enabled.value
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() {
        rule.mainClock.autoAdvance = false
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        val expected = prevAfterSomeFling + 115
        assertThat(total).isEqualTo(expected)
    }

    @Test
    fun scrollable_blocksDownEvents_ifFlingingCaught() {
        rule.mainClock.autoAdvance = false
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            orientation = Orientation.Horizontal,
                            state = controller
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .clickable {
                                assertWithMessage("Clickable shouldn't click when fling caught")
                                    .fail()
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            up()
        }
        // shouldn't assert in clickable lambda
    }

    @Test
    fun scrollable_snappingScrolling() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                orientation = Orientation.Vertical,
                state = controller
            )
        }
        rule.waitForIdle()
        assertThat(total).isEqualTo(0f)

        scope.launch {
            controller.animateScrollBy(1000f)
        }
        rule.waitForIdle()
        assertThat(total).isWithin(0.001f).of(1000f)

        scope.launch {
            controller.animateScrollBy(-200f)
        }
        rule.waitForIdle()
        assertThat(total).isWithin(0.001f).of(800f)
    }

    @Test
    fun scrollable_explicitDisposal() {
        rule.mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                assertWithMessage("Animating after dispose!").that(expectEmission.value).isTrue()
                total += it
                it
            }
        )
        setScrollableContent {
            if (emit.value) {
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = controller
                )
            } else {
                Modifier
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)

        // start the fling for a few frames
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // flip the emission
        rule.runOnUiThread {
            emit.value = false
        }
        // propagate the emit flip and record the value
        rule.mainClock.advanceTimeByFrame()
        val prevTotal = total
        // make sure we don't receive any deltas
        rule.runOnUiThread {
            expectEmission.value = false
        }

        // pump the clock until idle
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // still same and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    fun scrollable_nestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState = ScrollableState(
            consumeScrollDelta = {
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
                endVelocity = 0f
            )
        }
        val lastEqualDrag = rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
            innerDrag
        }
        rule.runOnIdle {
            // values should be the same since no fling
            assertThat(innerDrag).isEqualTo(lastEqualDrag)
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForMouseWheel() {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState = ScrollableState(
            consumeScrollDelta = {
                // Since the child has already consumed half, the parent will consume the rest.
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                // Child consumes half, leaving the rest for the parent to consume.
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-200f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
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
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForKeyboardPageUpAndDown() {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState = ScrollableState(
            consumeScrollDelta = {
                // Since the child has already consumed half, the parent will consume the rest.
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                // Child consumes half, leaving the rest for the parent to consume.
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
                                orientation = Orientation.Horizontal
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag(scrollableBoxTag)
                                // Required for keyboard scrolling (page up/down keys) to work.
                                .focusable()
                                .size(300.dp)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).requestFocus()
        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageDown)
        }

        rule.runOnIdle {
            assertThat(outerDrag).isLessThan(0f)
            assertThat(innerDrag).isLessThan(0f)
            // Since child (inner) consumes half of the scroll, the parent (outer) consumes the
            // remainder (which is half as well), so they will be equal.
            assertThat(innerDrag).isEqualTo(outerDrag)
        }

        outerDrag = 0f
        innerDrag = 0f

        rule.onNodeWithTag(scrollableBoxTag).performKeyInput {
            pressKey(Key.PageUp)
        }

        rule.runOnIdle {
            assertThat(outerDrag).isGreaterThan(0f)
            assertThat(innerDrag).isGreaterThan(0f)
            // Since child (inner) consumes half of the scroll, the parent (outer) consumes the
            // remainder (which is half as well), so they will be equal.
            assertThat(innerDrag).isEqualTo(outerDrag)
        }
    }

    @Test
    fun scrollable_nestedFling() {
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState = ScrollableState(
            consumeScrollDelta = {
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        // swipe again with velocity
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }
        assertThat(innerDrag).isGreaterThan(0f)
        assertThat(outerDrag).isGreaterThan(0f)
        // we consumed half delta in child, so exactly half should go to the parent
        assertThat(outerDrag).isEqualTo(innerDrag)
        val lastEqualDrag = innerDrag
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(lastEqualDrag)
            assertThat(outerDrag).isGreaterThan(lastEqualDrag)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_respectsPreConsumption() {
        var value = 0f
        var lastReceivedPreScrollAvailable = 0f
        val preConsumeFraction = 0.7f
        val controller = ScrollableState(
            consumeScrollDelta = {
                val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                assertThat(it - expected).isWithin(0.01f)
                value += it
                it
            }
        )
        val preConsumingParent = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                lastReceivedPreScrollAvailable = available.x
                return available * preConsumeFraction
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // consume all velocity
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(preConsumingParent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }

        val preFlingValue = rule.runOnIdle { value }
        rule.runOnIdle {
            // if scrollable respects pre-fling consumption, it should fling 0px since we
            // pre-consume all
            assertThat(preFlingValue).isEqualTo(value)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_proxiesPostCycles() {
        var value = 0f
        var expectedLeft = 0f
        val velocityFlung = 5000f
        val controller = ScrollableState(
            consumeScrollDelta = {
                val toConsume = it * 0.345f
                value += toConsume
                expectedLeft = it - toConsume
                toConsume
            }
        )
        val parent = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // we should get in post scroll as much as left in controller callback
                assertThat(available.x).isEqualTo(expectedLeft)
                return if (source == NestedScrollSource.Fling) Offset.Zero else available
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                val expected = velocityFlung - consumed.x
                assertThat(consumed.x).isLessThan(velocityFlung)
                assertThat(abs(available.x - expected)).isLessThan(0.1f)
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollAbove_reversed_proxiesPostCycles() {
        var value = 0f
        var expectedLeft = 0f
        val velocityFlung = 5000f
        val controller = ScrollableState(
            consumeScrollDelta = {
                val toConsume = it * 0.345f
                value += toConsume
                expectedLeft = it - toConsume
                toConsume
            }
        )
        val parent = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // we should get in post scroll as much as left in controller callback
                assertThat(available.x).isEqualTo(-expectedLeft)
                return if (source == NestedScrollSource.Fling) Offset.Zero else available
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                val expected = velocityFlung - consumed.x
                assertThat(consumed.x).isLessThan(velocityFlung)
                assertThat(abs(available.x - expected)).isLessThan(0.1f)
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = controller,
                                reverseDirection = true,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollBelow_listensDispatches() {
        var value = 0f
        var expectedConsumed = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                expectedConsumed = it * 0.3f
                value += expectedConsumed
                expectedConsumed
            }
        )
        val child = object : NestedScrollConnection {}
        val dispatcher = NestedScrollDispatcher()

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
                            .testTag(scrollableBoxTag)
                            .nestedScroll(child, dispatcher)
                    )
                }
            }
        }

        val lastValueBeforeFling = rule.runOnIdle {
            val preScrollConsumed = dispatcher
                .dispatchPreScroll(Offset(20f, 20f), NestedScrollSource.Drag)
            // scrollable is not interested in pre scroll
            assertThat(preScrollConsumed).isEqualTo(Offset.Zero)

            val consumed = dispatcher.dispatchPostScroll(
                Offset(20f, 20f),
                Offset(50f, 50f),
                NestedScrollSource.Drag
            )
            assertThat(consumed.x - expectedConsumed).isWithin(0.001f)
            value
        }

        scope.launch {
            val preFlingConsumed = dispatcher.dispatchPreFling(Velocity(50f, 50f))
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }
        rule.waitForIdle()

        scope.launch {
            dispatcher.dispatchPostFling(
                Velocity(1000f, 1000f),
                Velocity(2000f, 2000f)
            )
        }

        rule.runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value).isGreaterThan(lastValueBeforeFling)
        }
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() {
        var childValue = 0f
        var parentValue = 0f
        val childController = ScrollableState(
            consumeScrollDelta = {
                childValue += it
                it
            }
        )
        val parentController = ScrollableState(
            consumeScrollDelta = {
                parentValue += it
                it
            }
        )

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = parentController,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                enabled = false,
                                orientation = Orientation.Horizontal,
                                state = childController
                            )
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        rule.runOnIdle {
            assertThat(childValue).isEqualTo(0f)
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedScroll_disabledConnectionNoOp() {
        var childValue = 0f
        var parentValue = 0f
        var selfValue = 0f
        val childController = ScrollableState(
            consumeScrollDelta = {
                childValue += it / 2
                it / 2
            }
        )
        val middleController = ScrollableState(
            consumeScrollDelta = {
                selfValue += it / 2
                it / 2
            }
        )
        val parentController = ScrollableState(
            consumeScrollDelta = {
                parentValue += it / 2
                it / 2
            }
        )

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = parentController,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
                            .scrollable(
                                enabled = false,
                                orientation = Orientation.Horizontal,
                                state = middleController
                            )
                    ) {
                        Box(
                            Modifier
                                .size(200.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    orientation = Orientation.Horizontal,
                                    state = childController
                                )
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(selfValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        rule.runOnIdle {
            assertThat(childValue).isGreaterThan(0f)
            // disabled middle node doesn't consume
            assertThat(selfValue).isEqualTo(0f)
            // but allow nested scroll to propagate up correctly
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedFlingCancellation_shouldPreventDeltasFromPropagating() {
        var childDeltas = 0f
        var touchSlop = 0f
        val childController = ScrollableState {
            childDeltas += it
            it
        }
        val flingCancellationParent = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Fling && available != Offset.Zero) {
                    throw CancellationException()
                }
                return Offset.Zero
            }
        }

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(modifier = Modifier.nestedScroll(flingCancellationParent)) {
                Box(
                    modifier = Modifier
                        .size(600.dp)
                        .testTag("childScrollable")
                        .scrollable(childController, Orientation.Horizontal)
                )
            }
        }

        // First drag, this won't trigger the cancellation flow.
        rule.onNodeWithTag("childScrollable").performTouchInput {
            down(centerLeft)
            moveBy(Offset(100f, 0f))
            up()
        }

        rule.runOnIdle {
            assertThat(childDeltas).isEqualTo(100f - touchSlop)
        }

        childDeltas = 0f
        var dragged = 0f
        rule.onNodeWithTag("childScrollable").performTouchInput {
            swipeWithVelocity(centerLeft, centerRight, 2000f)
            dragged = centerRight.x - centerLeft.x
        }

        // child didn't receive more deltas after drag, because fling was cancelled by the parent
        assertThat(childDeltas).isEqualTo(dragged - touchSlop)
    }

    @Test
    fun scrollable_bothOrientations_proxiesPostFling() {
        val velocityFlung = 5000f
        val outerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerFlingBehavior = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return initialVelocity
            }
        }
        val parent = object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed.x).isEqualTo(0f)
                assertThat(available.x).isWithin(0.1f).of(velocityFlung)
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Vertical
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = innerState,
                                flingBehavior = innerFlingBehavior,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        setScrollableContent {
            Modifier.scrollable(
                interactionSource = interactionSource,
                orientation = Orientation.Horizontal,
                state = controller
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                up()
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        rule.setContentAndGetScope {
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(100.dp)
                            .scrollable(
                                interactionSource = interactionSource,
                                orientation = Orientation.Horizontal,
                                state = controller
                            )
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose scrollable
        rule.runOnIdle {
            emitScrollableBox = false
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_whenVelocity0() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
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
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isLessThan(0.01f)
        assertThat(flingVelocity).isGreaterThan(-0.01f)
    }

    @Test
    fun scrollable_flingBehaviourCalled() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
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
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(5f).of(1000f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
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
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(5f).of(-1000f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_correctScope() {
        var total = 0f
        var returned = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                returned = scrollBy(123f)
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            up()
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed_correctScope() {
        var total = 0f
        var returned = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        val flingBehaviour = object : FlingBehavior {
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
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            up()
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_setsModifierLocalScrollableContainer() {
        val controller = ScrollableState { it }

        var isOuterInScrollableContainer: Boolean? = null
        var isInnerInScrollableContainer: Boolean? = null
        rule.setContent {
            Box {
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .then(
                            object : ModifierLocalConsumer {
                                override fun onModifierLocalsUpdated(
                                    scope: ModifierLocalReadScope
                                ) {
                                    with(scope) {
                                        isOuterInScrollableContainer =
                                            ModifierLocalScrollableContainer.current
                                    }
                                }
                            }
                        )
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal
                        )
                        .then(
                            object : ModifierLocalConsumer {
                                override fun onModifierLocalsUpdated(
                                    scope: ModifierLocalReadScope
                                ) {
                                    with(scope) {
                                        isInnerInScrollableContainer =
                                            ModifierLocalScrollableContainer.current
                                    }
                                }
                            }
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(isOuterInScrollableContainer).isFalse()
            assertThat(isInnerInScrollableContainer).isTrue()
        }
    }

    @Test
    fun scrollable_scrollByWorksWithRepeatableAnimations() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        rule.runOnIdle {
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
                    }
                )
            }
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            // in the middle of the first animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 750 ms
        rule.runOnIdle {
            // first animation finished
            assertThat(total).isEqualTo(100)
        }

        rule.mainClock.advanceTimeBy(250) // 1250 ms
        rule.runOnIdle {
            // in the middle of the second animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 1750 ms
        rule.runOnIdle {
            // second animation finished
            assertThat(total).isEqualTo(0)
        }

        rule.mainClock.advanceTimeBy(500) // 2250 ms
        rule.runOnIdle {
            // in the middle of the third animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 2750 ms
        rule.runOnIdle {
            // third animation finished
            assertThat(total).isEqualTo(100)
        }
    }

    @Test
    fun scrollable_cancellingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        lateinit var animateJob: Job

        rule.runOnIdle {
            animateJob = scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        // Stop halfway through the animation
        animateJob.cancel()

        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        rule.runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
            assertThat(controller.isScrollInProgress).isTrue()
            scope.launch {
                controller.animateScrollBy(
                    -100f,
                    tween(1000)
                )
            }
        }

        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(total).isGreaterThan(-75f)
            assertThat(total).isLessThan(0f)
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_multiDirectionsShouldPropagateOrthogonalAxisToNextParentWithSameDirection() {
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

        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .testTag("outerScrollable")
                    .size(300.dp)
                    .scrollable(
                        outerStateController,
                        orientation = Orientation.Horizontal
                    )

            ) {
                Box(
                    modifier = Modifier
                        .testTag("middleScrollable")
                        .size(300.dp)
                        .scrollable(
                            middleController,
                            orientation = Orientation.Vertical
                        )

                ) {
                    Box(
                        modifier = Modifier
                            .testTag("innerScrollable")
                            .size(300.dp)
                            .scrollable(
                                innerController,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag("innerScrollable").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }

        rule.runOnIdle {
            assertThat(innerDelta).isGreaterThan(0)
            assertThat(middleDelta).isEqualTo(0)
            assertThat(outerDelta).isEqualTo(innerDelta / 2f)
        }
    }

    @Test
    fun nestedScrollable_shouldImmediateScrollIfChildIsFlinging() {
        var innerDelta = 0f
        var middleDelta = 0f
        var outerDelta = 0f
        var touchSlop = 0f

        val outerStateController = ScrollableState {
            outerDelta += it
            0f
        }

        val middleController = ScrollableState {
            middleDelta += it
            0f
        }

        val innerController = ScrollableState {
            innerDelta += it
            it / 2f
        }

        rule.setContentAndGetScope {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier = Modifier
                    .testTag("outerScrollable")
                    .size(600.dp)
                    .background(Color.Red)
                    .scrollable(
                        outerStateController,
                        orientation = Orientation.Vertical
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .testTag("middleScrollable")
                        .size(300.dp)
                        .background(Color.Blue)
                        .scrollable(
                            middleController,
                            orientation = Orientation.Vertical
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("innerScrollable")
                            .size(50.dp)
                            .background(Color.Yellow)
                            .scrollable(
                                innerController,
                                orientation = Orientation.Vertical
                            )
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("innerScrollable").performTouchInput {
            swipeUp()
        }

        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val previousOuter = outerDelta

        rule.onNodeWithTag("outerScrollable").performTouchInput {
            down(topCenter)
            // Move less than touch slop, should start immediately
            moveBy(Offset(0f, touchSlop / 2))
        }

        rule.mainClock.autoAdvance = true

        rule.runOnIdle {
            assertThat(outerDelta).isEqualTo(previousOuter + touchSlop / 2)
        }
    }

    // b/179417109 Double checks that in a nested scroll cycle, the parent post scroll
    // consumption is taken into consideration.
    @Test
    fun dispatchScroll_shouldReturnConsumedDeltaInNestedScrollChain() {
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

        val connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                preScrollAvailable += available
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                consumedPostScroll += consumed
                postScrollAvailable += available
                return Offset.Zero
            }
        }

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(modifier = Modifier.nestedScroll(connection)) {
                Box(
                    modifier = Modifier
                        .testTag("outerScrollable")
                        .size(300.dp)
                        .scrollable(
                            outerStateController,
                            orientation = Orientation.Horizontal
                        )

                ) {
                    Box(
                        modifier = Modifier
                            .testTag("innerScrollable")
                            .size(300.dp)
                            .scrollable(
                                innerController,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        val scrollDelta = 200f

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(scrollDelta, 0f))
            up()
        }

        rule.runOnIdle {
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
    fun testInspectorValue() {
        val controller = ScrollableState(
            consumeScrollDelta = { it }
        )
        rule.setContentAndGetScope {
            val modifier =
                Modifier.scrollable(controller, Orientation.Vertical).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "state",
                "overscrollEffect",
                "enabled",
                "reverseDirection",
                "flingBehavior",
                "interactionSource",
                "scrollableBringIntoViewConfig",
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun producingEqualMaterializedModifierAfterRecomposition() {
        val state = ScrollableState { it }
        val counter = mutableStateOf(0)
        var materialized: Modifier? = null

        rule.setContent {
            counter.value // just to trigger recomposition
            materialized = currentComposer.materialize(
                Modifier.scrollable(
                    state,
                    Orientation.Vertical,
                    NoOpOverscrollEffect
                )
            )
        }

        lateinit var first: Modifier
        rule.runOnIdle {
            first = requireNotNull(materialized)
            materialized = null
            counter.value++
        }

        rule.runOnIdle {
            val second = requireNotNull(materialized)
            assertThat(first).isEqualTo(second)
        }
    }

    @Test
    fun focusStaysInScrollableEvenThoughThereIsACloserItemOutside() {
        lateinit var focusManager: FocusManager
        val initialFocus = FocusRequester()
        var nextItemIsFocused = false
        rule.setContent {
            focusManager = LocalFocusManager.current
            Column {
                Column(
                    Modifier
                        .size(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .focusRequester(initialFocus)
                            .focusable()
                    )
                    Box(Modifier.size(10.dp))
                    Box(
                        Modifier
                            .size(10.dp)
                            .onFocusChanged { nextItemIsFocused = it.isFocused }
                            .focusable())
                }
                Box(
                    Modifier
                        .size(10.dp)
                        .focusable()
                )
            }
        }

        rule.runOnIdle { initialFocus.requestFocus() }
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Down) }

        rule.runOnIdle { assertThat(nextItemIsFocused).isTrue() }
    }

    @Test
    fun verticalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier
                .pointerInput(Unit) {
                    savePointerInputEvents(tracker, this)
                }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Vertical)
        }

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeDown()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun horizontalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier
                .pointerInput(Unit) {
                    savePointerInputEvents(tracker, this)
                }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Horizontal)
        }

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeLeft()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeRight()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun offsetsScrollable_velocityCalculationShouldConsiderLocalPositions() {
        // arrange
        var velocity = Velocity.Zero
        val fullScreen = mutableStateOf(false)
        lateinit var scrollState: LazyListState
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        rule.setContent {
            scrollState = rememberLazyListState()
            Column(modifier = Modifier.nestedScroll(capturingScrollConnection)) {
                if (!fullScreen.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .height(400.dp)
                    )
                }

                LazyColumn(state = scrollState) {
                    items(100) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .background(Color.Red)
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        }
        // act
        // Register generated velocity with offset
        composeViewSwipeUp()
        rule.waitForIdle()
        val previousVelocity = velocity
        velocity = Velocity.Zero
        // Remove offset and restart scroll
        fullScreen.value = true
        rule.runOnIdle {
            runBlocking {
                scrollState.scrollToItem(0)
            }
        }
        rule.waitForIdle()
        // Register generated velocity without offset, should be larger as there was more
        // screen to cover.
        composeViewSwipeUp()

        // assert
        rule.runOnIdle {
            assertThat(abs(previousVelocity.y)).isNotEqualTo(abs(velocity.y))
        }
    }

    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() {

        val controller = ScrollableState { 0f }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        setScrollableContent {
            defaultFlingBehavior = ScrollableDefaults.flingBehavior() as? DefaultFlingBehavior
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                flingBehavior = defaultFlingBehavior
            )
        }

        scope.launch {
            controller.scroll {
                defaultFlingBehavior?.let {
                    with(it) { performFling(1000f) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        // Simulate turning of animation
        scope.launch {
            controller.scroll {
                withContext(TestScrollMotionDurationScale(0f)) {
                    defaultFlingBehavior?.let {
                        with(it) { performFling(1000f) }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }
    }

    @Test
    fun defaultFlingBehavior_useScrollMotionDurationScale() {

        val controller = ScrollableState { 0f }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        var switchMotionDurationScale by mutableStateOf(true)

        rule.setContentAndGetScope {
            val flingSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
            if (switchMotionDurationScale) {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(1f))
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal,
                            flingBehavior = defaultFlingBehavior
                        )
                )
            } else {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(0f))
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal,
                            flingBehavior = defaultFlingBehavior
                        )
                )
            }
        }

        scope.launch {
            controller.scroll {
                defaultFlingBehavior?.let {
                    with(it) { performFling(1000f) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        switchMotionDurationScale = false
        rule.waitForIdle()

        scope.launch {
            controller.scroll {
                defaultFlingBehavior?.let {
                    with(it) { performFling(1000f) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isEqualTo(1)
        }
    }

    @Test
    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease() {
        val scrollState = ScrollState(0)
        val delta = 10f
        var touchSlop = 0f
        setScrollableContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.scrollable(scrollState, Orientation.Vertical)
        }
        var previousScrollValue = 0
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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

        rule.runOnIdle {
            Assert.assertEquals((previousScrollValue - touchSlop).toInt(), scrollState.value)
        }
    }

    @Test
    fun defaultScrollableState_scrollByWithNan_shouldFilterOutNan() {
        val controller = ScrollableState {
            assertThat(it).isNotNaN()
            0f
        }

        val nanGenerator = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return scrollBy(Float.NaN)
            }
        }

        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                flingBehavior = nanGenerator
            )
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeLeft()
        }
    }

    private fun setScrollableContent(scrollableModifierFactory: @Composable () -> Modifier) {
        rule.setContentAndGetScope {
            Box {
                val scrollable = scrollableModifierFactory()
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .then(scrollable)
                )
            }
        }
    }
}

// Very low tolerance on the difference
internal val VelocityTrackerCalculationThreshold = 1

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEvents(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    if (VelocityTrackerAddPointsFix) {
        savePointerInputEventsWithFix(tracker, pointerInputScope)
    } else {
        savePointerInputEventsLegacy(tracker, pointerInputScope)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEventsWithFix(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event: PointerInputChange? = awaitFirstDown()
                    while (event != null && !event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes
                            .firstOrNull()

                        if (currentEvent != null && !currentEvent.changedToUpIgnoreConsumed()) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(
                                currentEvent.uptimeMillis,
                                currentEvent.position
                            )
                        }

                        event = currentEvent
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEventsLegacy(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event = awaitFirstDown()
                    tracker.addPosition(event.uptimeMillis, event.position)
                    while (!event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes
                            .firstOrNull()

                        if (currentEvent != null) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(
                                currentEvent.uptimeMillis,
                                currentEvent.position
                            )
                            event = currentEvent
                        }
                    }
                }
            }
        }
    }
}

internal fun composeViewSwipeUp() {
    onView(allOf(instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.TOP_CENTER
            )
        )
}

internal fun composeViewSwipeDown() {
    onView(allOf(instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.BOTTOM_CENTER
            )
        )
}

internal fun composeViewSwipeLeft() {
    onView(allOf(instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.CENTER_LEFT
            )
        )
}

internal fun composeViewSwipeRight() {
    onView(allOf(instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.CENTER_RIGHT
            )
        )
}

private fun espressoSwipe(
    start: CoordinatesProvider,
    end: CoordinatesProvider
): GeneralSwipeAction {
    return GeneralSwipeAction(
        Swipe.FAST, start, end,
        Press.FINGER
    )
}

internal class TestScrollMotionDurationScale(override val scaleFactor: Float) : MotionDurationScale
