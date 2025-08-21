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
import androidx.compose.foundation.gestures.Scroll2DScope
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.materialize
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Scrollable2DTest {

    @get:Rule val rule = createComposeRule()

    private val scrollable2DBoxTag = "scrollableBox"

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
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = controller) }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total.x).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_verticalScroll() {
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = controller) }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total.y).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_diagonalScroll() {
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = controller) }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y + 100f),
                durationMillis = 100,
            )
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total.x).isLessThan(0.01f) }
        rule.runOnIdle { assertThat(total.y).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_disabledWontCallLambda() {
        val enabled = mutableStateOf(true)
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent {
            Modifier.scrollable2D(state = controller, enabled = enabled.value)
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y + 100f),
                durationMillis = 100,
            )
        }
        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                enabled.value = false
                total
            }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y + 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() {
        rule.mainClock.autoAdvance = false
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = controller) }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total.x).isGreaterThan(0f)
        assertThat(total.y).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling.x).isGreaterThan(prev.x)
        assertThat(prevAfterSomeFling.y).isGreaterThan(prev.y)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 115f))
            up()
        }
        val expected = prevAfterSomeFling + Offset(115f, 115f)
        assertThat(total).isEqualTo(expected)
    }

    @Test
    fun scrollable_blocksDownEvents_ifFlingingCaught() {
        rule.mainClock.autoAdvance = false
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).scrollable2D(state = controller),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp).testTag(scrollable2DBoxTag).clickable {
                                assertWithMessage("Clickable shouldn't click when fling caught ")
                                    .fail()
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total.x).isGreaterThan(0f)
        assertThat(total.y).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling.x).isGreaterThan(prev.x)
        assertThat(prevAfterSomeFling.y).isGreaterThan(prev.y)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(this.center)
            up()
        }
        // shouldn't assert in clickable lambda
    }

    @Test
    fun scrollable_snappingScrolling() {
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = controller) }
        rule.waitForIdle()
        assertThat(total).isEqualTo(Offset.Zero)

        scope.launch { controller.animateScrollBy(Offset(1000f, 1000f)) }
        rule.waitForIdle()
        assertThat(total.x).isWithin(0.001f).of(1000f)
        assertThat(total.y).isWithin(0.001f).of(1000f)

        scope.launch { controller.animateScrollBy(Offset(-200f, -200f)) }
        rule.waitForIdle()
        assertThat(total.x).isWithin(0.001f).of(800f)
        assertThat(total.y).isWithin(0.001f).of(800f)
    }

    @Test
    fun scrollable_explicitDisposal() {
        rule.mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    assertWithMessage("Animating after dispose!")
                        .that(expectEmission.value)
                        .isTrue()
                    total += it
                    it
                }
            )
        setScrollable2DContent {
            if (emit.value) {
                Modifier.scrollable2D(state = controller)
            } else {
                Modifier
            }
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 100,
                endVelocity = 4000f,
            )
        }
        assertThat(total.x).isGreaterThan(0f)
        assertThat(total.y).isGreaterThan(0f)

        // start the fling for a few frames
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // flip the emission
        rule.runOnUiThread { emit.value = false }
        // propagate the emit flip and record the value
        rule.mainClock.advanceTimeByFrame()
        val prevTotal = total
        // make sure we don't receive any deltas
        rule.runOnUiThread { expectEmission.value = false }

        // pump the clock until idle
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // still same and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    fun scrollable_nestedDrag() {
        var innerDrag = Offset.Zero
        var outerDrag = Offset.Zero
        val outerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    outerDrag += it
                    it
                }
            )
        val innerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    innerDrag += it / 2f
                    it / 2f
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).scrollable2D(state = outerState),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollable2DBoxTag)
                                .size(300.dp)
                                .scrollable2D(state = innerState)
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 300,
                endVelocity = 0f,
            )
        }
        val lastEqualDrag =
            rule.runOnIdle {
                assertThat(innerDrag.x).isGreaterThan(0f)
                assertThat(innerDrag.y).isGreaterThan(0f)
                assertThat(outerDrag.x).isGreaterThan(0f)
                assertThat(outerDrag.y).isGreaterThan(0f)
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

    @Test
    fun scrollable_nestedScroll_childPartialConsumptionForSemantics_horizontal() {
        var innerDrag = Offset.Zero
        var outerDrag = Offset.Zero
        val outerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    // Since the child has already consumed half, the parent will consume the rest.
                    outerDrag += it
                    it
                }
            )
        val innerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    // Child consumes half, leaving the rest for the parent to consume.
                    innerDrag += it / 2f
                    it / 2f
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).scrollable2D(state = outerState),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollable2DBoxTag)
                                .size(300.dp)
                                .scrollable2D(state = innerState)
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performSemanticsAction(ScrollBy) {
            it.invoke(200f, 200f)
        }

        rule.runOnIdle {
            assertThat(innerDrag.x).isGreaterThan(0f)
            assertThat(innerDrag.y).isGreaterThan(0f)
            assertThat(outerDrag.x).isGreaterThan(0f)
            assertThat(outerDrag.y).isGreaterThan(0f)
            assertThat(innerDrag).isEqualTo(outerDrag)
            innerDrag
        }
    }

    @Test
    fun scrollable_nestedFling() {
        var innerDrag = Offset.Zero
        var outerDrag = Offset.Zero
        val outerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    outerDrag += it
                    it
                }
            )
        val innerState =
            Scrollable2DState(
                consumeScrollDelta = {
                    innerDrag += it / 2f
                    it / 2f
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).scrollable2D(state = outerState),
                ) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollable2DBoxTag)
                                .size(300.dp)
                                .scrollable2D(state = innerState)
                    )
                }
            }
        }

        // swipe again with velocity
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 300,
            )
        }
        assertThat(innerDrag.x).isGreaterThan(0f)
        assertThat(innerDrag.y).isGreaterThan(0f)
        assertThat(outerDrag.x).isGreaterThan(0f)
        assertThat(outerDrag.y).isGreaterThan(0f)
        // we consumed half delta in child, so exactly half should go to the parent
        assertThat(outerDrag).isEqualTo(innerDrag)
        val lastEqualDrag = innerDrag
        rule.runOnIdle {
            assertThat(innerDrag.x).isGreaterThan(lastEqualDrag.x)
            assertThat(innerDrag.y).isGreaterThan(lastEqualDrag.y)
            assertThat(outerDrag.x).isGreaterThan(lastEqualDrag.x)
            assertThat(outerDrag.y).isGreaterThan(lastEqualDrag.y)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_respectsPreConsumption() {
        var value = Offset.Zero
        var lastReceivedPreScrollAvailable = Offset.Zero
        val preConsumeFraction = 0.7f
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                    assertThat(it.x).isWithin(0.01f).of(expected.x)
                    assertThat(it.y).isWithin(0.01f).of(expected.y)
                    value += it
                    it
                }
            )
        val preConsumingParent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    lastReceivedPreScrollAvailable = available
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
                    modifier = Modifier.size(300.dp).nestedScroll(preConsumingParent),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollable2DBoxTag)
                                .scrollable2D(state = controller)
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y + 200f),
                durationMillis = 300,
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
        var value = Offset.Zero
        var expectedLeft = Offset.Zero
        val velocityFlung = 5000f
        val controller =
            Scrollable2DState(
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
                    assertThat(available.x).isEqualTo(expectedLeft.x)
                    assertThat(available.y).isEqualTo(expectedLeft.y)
                    return if (source == NestedScrollSource.SideEffect) Offset.Zero else available
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    // part of the velocity was consumed. Since we flung at a 45 angle our
                    // it means our cos(velocity angle) and sin (velocity angle) will be around 0.7f
                    assertThat(consumed.x).isLessThan(velocityFlung * 0.7f)
                    assertThat(consumed.y).isLessThan(velocityFlung * 0.7f)
                    return available
                }
            }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp).nestedScroll(parent),
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp)
                                .testTag(scrollable2DBoxTag)
                                .scrollable2D(state = controller)
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y + 500f),
                durationMillis = 300,
                endVelocity = velocityFlung,
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollBelow_listensDispatches() {
        var value = Offset.Zero
        var expectedConsumed = Offset.Zero
        val controller =
            Scrollable2DState(
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
                Box(modifier = Modifier.size(300.dp).scrollable2D(state = controller)) {
                    Box(
                        Modifier.size(200.dp)
                            .testTag(scrollable2DBoxTag)
                            .nestedScroll(child, dispatcher)
                    )
                }
            }
        }

        val lastValueBeforeFling =
            rule.runOnIdle {
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
                assertThat(consumed.x).isWithin(0.001f).of(expectedConsumed.x)
                assertThat(consumed.y).isWithin(0.001f).of(expectedConsumed.y)
                value
            }

        scope.launch {
            val preFlingConsumed = dispatcher.dispatchPreFling(Velocity(50f, 50f))
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }
        rule.waitForIdle()

        scope.launch {
            dispatcher.dispatchPostFling(Velocity(1000f, 1000f), Velocity(2000f, 2000f))
        }

        rule.runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value.x).isGreaterThan(lastValueBeforeFling.x)
            assertThat(value.y).isGreaterThan(lastValueBeforeFling.y)
        }
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() {
        var childValue = Offset.Zero
        var parentValue = Offset.Zero
        val childController =
            Scrollable2DState(
                consumeScrollDelta = {
                    childValue += it
                    it
                }
            )
        val parentController =
            Scrollable2DState(
                consumeScrollDelta = {
                    parentValue += it
                    it
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(modifier = Modifier.size(300.dp).scrollable2D(state = parentController)) {
                    Box(
                        Modifier.size(200.dp)
                            .testTag(scrollable2DBoxTag)
                            .scrollable2D(enabled = false, state = childController)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(parentValue).isEqualTo(Offset.Zero)
            assertThat(childValue).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            swipe(center, Offset(x = center.x + 100f, y = center.y + 100f))
        }

        rule.runOnIdle {
            assertThat(childValue).isEqualTo(Offset.Zero)
            assertThat(parentValue.x).isGreaterThan(0f)
            assertThat(parentValue.y).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedScroll_disabledConnectionNoOp() {
        var childValue = Offset.Zero
        var parentValue = Offset.Zero
        var selfValue = Offset.Zero
        val childController =
            Scrollable2DState(
                consumeScrollDelta = {
                    childValue += it / 2f
                    it / 2f
                }
            )
        val middleController =
            Scrollable2DState(
                consumeScrollDelta = {
                    selfValue += it / 2f
                    it / 2f
                }
            )
        val parentController =
            Scrollable2DState(
                consumeScrollDelta = {
                    parentValue += it / 2f
                    it / 2f
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(modifier = Modifier.size(300.dp).scrollable2D(state = parentController)) {
                    Box(
                        Modifier.size(200.dp)
                            .scrollable2D(enabled = false, state = middleController)
                    ) {
                        Box(
                            Modifier.size(200.dp)
                                .testTag(scrollable2DBoxTag)
                                .scrollable2D(state = childController)
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(parentValue).isEqualTo(Offset.Zero)
            assertThat(selfValue).isEqualTo(Offset.Zero)
            assertThat(childValue).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            swipe(center, Offset(x = center.x + 100f, y = center.x + 100f))
        }

        rule.runOnIdle {
            assertThat(childValue.x).isGreaterThan(0f)
            assertThat(childValue.y).isGreaterThan(0f)
            // disabled middle node doesn't consume
            assertThat(selfValue).isEqualTo(Offset.Zero)
            // but allow nested scroll to propagate up correctly
            assertThat(parentValue.x).isGreaterThan(0f)
            assertThat(parentValue.y).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedFlingCancellation_shouldPreventDeltasFromPropagating() {
        var childDeltas = Offset.Zero
        val childController = Scrollable2DState {
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

        rule.setContent {
            WithTouchSlop(0f) {
                Box(modifier = Modifier.nestedScroll(flingCancellationParent)) {
                    Box(
                        modifier =
                            Modifier.size(600.dp)
                                .testTag("childScrollable")
                                .scrollable2D(childController)
                    )
                }
            }
        }

        // First drag, this won't trigger the cancellation flow.
        rule.onNodeWithTag("childScrollable").performTouchInput {
            down(centerLeft)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle { assertThat(childDeltas).isEqualTo(Offset(100f, 100f)) }

        childDeltas = Offset.Zero
        var dragged = Offset.Zero
        rule.onNodeWithTag("childScrollable").performTouchInput {
            swipeWithVelocity(centerLeft, topRight, 2000f)
            dragged = topRight - centerLeft
        }

        // child didn't receive more deltas after drag, because fling was cancelled by the parent
        assertThat(childDeltas).isEqualTo(dragged)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollable_nestedFling_shouldCancelWhenHitTheBounds_ifRemoved() {
        var shouldEmmit by mutableStateOf(true)
        var latestScroll = Offset.Zero
        val connection =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    latestScroll += available
                    return super.onPreScroll(available, source)
                }
            }

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(Modifier.nestedScroll(connection)) {
                if (shouldEmmit) {
                    Box(
                        Modifier.size(400.dp)
                            .testTag("scrollable")
                            .scrollable2D(rememberScrollable2DState { Offset.Zero })
                    )
                }
            }
        }
        var swipeSize = 0f
        rule.onNodeWithTag("scrollable").performTouchInput {
            swipeSize = bottom - top
            swipeDown()
        }

        rule.mainClock.advanceTimeUntil { latestScroll.y.absoluteValue > swipeSize }
        rule.runOnIdle { shouldEmmit = false }
        rule.mainClock.advanceTimeByFrame()
        latestScroll = Offset.Zero

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { assertThat(latestScroll).isEqualTo(Offset.Zero) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollable_nestedFling_shouldContinueSendingDeltasWhenHitBounds() {
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

        var simulateHitBounds = false
        val scrollState = Scrollable2DState { if (simulateHitBounds) Offset.Zero else it }
        rule.setContent {
            Box(Modifier.nestedScroll(connection)) {
                Box(Modifier.size(200.dp).testTag("column").scrollable2D(scrollState))
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("column").performTouchInput { swipeDown(center.y, bottomCenter.y) }

        rule.mainClock.advanceTimeBy(200)
        simulateHitBounds = true

        flingDeltas = Offset.Zero
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertThat(flingDeltas.y).isNonZero()
    }

    @Test
    fun scrollable_nestedFling_parentShouldFlingWithVelocityLeft() {
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
        val columnMaxValue = with(rule.density) { 200.dp.roundToPx() * 5 }
        val columnState = ScrollState(columnMaxValue)
        rule.setContent {
            Box(
                Modifier.nestedScroll(topConnection)
                    .scrollable2D(
                        flingBehavior = fling,
                        state = rememberScrollable2DState { Offset.Zero },
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

        rule.onNodeWithTag("column").performTouchInput { swipeDown() }

        rule.runOnIdle {
            assertThat(columnState.value).isZero() // column is at the bounds
            assertThat(postFlingCalled)
                .isTrue() // we fired a post fling call after the cancellation
            assertThat(lastPostFlingVelocity.y)
                .isNonZero() // the post child fling velocity was not zero
            assertThat(flingDelta).isEqualTo(100f) // the fling delta as propagated correctly
        }
    }

    @Test
    fun scrollable_nestedFling_parentShouldFlingWithVelocityLeft_whenInnerDisappears() {
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

        val columnState = ScrollState(with(rule.density) { 200.dp.roundToPx() * 50 })

        rule.setContent {
            Box(Modifier.nestedScroll(topConnection)) {
                if (flingDelta.absoluteValue < 100) {
                    Column(Modifier.testTag("column").verticalScroll(columnState)) {
                        repeat(100) { Box(Modifier.size(200.dp)) }
                    }
                }
            }
        }

        rule.onNodeWithTag("column").performTouchInput { swipeUp() }
        rule.waitForIdle()
        // removed scrollable
        rule.onNodeWithTag("column").assertDoesNotExist()
        rule.runOnIdle {
            // we fired a post fling call after the disappearance
            assertThat(postFlingCalled).isTrue()

            // fling velocity in onPostFling is correctly propagated
            assertThat(postFlingConsumedVelocity + postFlingAvailableVelocity)
                .isEqualTo(preFlingVelocity)
        }
    }

    @Test
    fun scrollable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        setScrollable2DContent {
            Modifier.scrollable2D(interactionSource = interactionSource, state = controller)
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )

        rule.setContentAndGetScope {
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier =
                            Modifier.testTag(scrollable2DBoxTag)
                                .size(100.dp)
                                .scrollable2D(
                                    interactionSource = interactionSource,
                                    state = controller,
                                )
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose scrollable
        rule.runOnIdle { emitScrollableBox = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_whenVelocity0() {
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
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
        setScrollable2DContent {
            Modifier.scrollable2D(state = controller, flingBehavior = flingBehaviour)
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
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
        var total = Offset.Zero
        val controller =
            Scrollable2DState(
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
        setScrollable2DContent {
            Modifier.scrollable2D(state = controller, flingBehavior = flingBehaviour)
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            swipeWithVelocity(this.center, this.center + Offset(115f, 0f), endVelocity = 1000f)
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(5f).of(1000f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_correctScope() {
        var total = Offset.Zero
        var returned = 0f
        val controller =
            Scrollable2DState(
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
        setScrollable2DContent {
            Modifier.scrollable2D(state = controller, flingBehavior = flingBehaviour)
        }
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 100f))
        }

        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                total
            }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            moveBy(Offset(x = 100f, y = 100f))
            up()
        }

        rule.runOnIdle {
            assertThat(total.x).isWithin(1f).of(prevTotal.x + (123 * 0.7f) + 100f)
            assertThat(total.y).isWithin(1f).of(prevTotal.y + (123 * 0.7f) + 100f)
            assertThat(returned.roundToInt()).isEqualTo(123)
        }
    }

    @Test
    fun scrollable_setsModifierLocalScrollableContainer() {
        val controller = Scrollable2DState { it }

        var isOuterInScrollableContainer: Boolean? = null
        var isInnerInScrollableContainer: Boolean? = null
        rule.setContent {
            Box {
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .then(
                                ScrollableContainerReaderNodeElement {
                                    isOuterInScrollableContainer = it
                                }
                            )
                            .scrollable2D(state = controller)
                            .then(
                                ScrollableContainerReaderNodeElement {
                                    isInnerInScrollableContainer = it
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
    fun scrollable_setsModifierLocalScrollableContainer_scrollDisabled() {
        val controller = Scrollable2DState { it }

        var isOuterInScrollableContainer: Boolean? = null
        var isInnerInScrollableContainer: Boolean? = null
        rule.setContent {
            Box {
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .then(
                                ScrollableContainerReaderNodeElement {
                                    isOuterInScrollableContainer = it
                                }
                            )
                            .scrollable2D(state = controller, enabled = false)
                            .then(
                                ScrollableContainerReaderNodeElement {
                                    isInnerInScrollableContainer = it
                                }
                            )
                )
            }
        }

        rule.runOnIdle {
            assertThat(isOuterInScrollableContainer).isFalse()
            assertThat(isInnerInScrollableContainer).isFalse()
        }
    }

    @Test
    fun scrollable_setsModifierLocalScrollableContainer_scrollUpdates() {
        val controller = Scrollable2DState { it }

        var isInnerInScrollableContainer: Boolean? = null
        val enabled = mutableStateOf(true)
        rule.setContent {
            Box {
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .scrollable2D(state = controller, enabled = enabled.value)
                            .then(
                                ScrollableContainerReaderNodeElement {
                                    isInnerInScrollableContainer = it
                                }
                            )
                )
            }
        }

        rule.runOnIdle { assertThat(isInnerInScrollableContainer).isTrue() }

        rule.runOnIdle { enabled.value = false }

        rule.runOnIdle { assertThat(isInnerInScrollableContainer).isFalse() }
    }

    @Test
    fun scrollable_scrollByWorksWithRepeatableAnimations() {
        rule.mainClock.autoAdvance = false

        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = controller))
        }

        rule.runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    Offset(100f, 100f),
                    keyframes {
                        durationMillis = 2500
                        // emulate a repeatable animation:
                        Offset(0f, 0f) at 0
                        Offset(100f, 100f) at 500
                        Offset(100f, 100f) at 1000
                        Offset(0f, 0f) at 1500
                        Offset(0f, 0f) at 2000
                        Offset(100f, 100f) at 2500
                    },
                )
            }
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            // in the middle of the first animation
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(total.x).isLessThan(100f)
            assertThat(total.y).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 750 ms
        rule.runOnIdle {
            // first animation finished
            assertThat(total.x).isEqualTo(100)
            assertThat(total.y).isEqualTo(100)
        }

        rule.mainClock.advanceTimeBy(250) // 1250 ms
        rule.runOnIdle {
            // in the middle of the second animation
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(total.x).isLessThan(100f)
            assertThat(total.y).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 1750 ms
        rule.runOnIdle {
            // second animation finished
            assertThat(total).isEqualTo(Offset.Zero)
        }

        rule.mainClock.advanceTimeBy(500) // 2250 ms
        rule.runOnIdle {
            // in the middle of the third animation
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(total.x).isLessThan(100f)
            assertThat(total.y).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 2750 ms
        rule.runOnIdle {
            // third animation finished
            assertThat(total).isEqualTo(Offset(100f, 100f))
        }
    }

    @Test
    fun scrollable_cancellingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = controller))
        }

        lateinit var animateJob: Job

        rule.runOnIdle {
            animateJob =
                scope.launch { controller.animateScrollBy(Offset(100f, 100f), tween(1000)) }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle { assertThat(controller.isScrollInProgress).isTrue() }

        // Stop halfway through the animation
        animateJob.cancel()

        rule.runOnIdle { assertThat(controller.isScrollInProgress).isFalse() }
    }

    @Test
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = Offset.Zero
        val controller =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = controller))
        }

        rule.runOnIdle {
            scope.launch { controller.animateScrollBy(Offset(100f, 100f), tween(1000)) }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(total.x).isLessThan(100f)
            assertThat(total.y).isLessThan(100f)
            assertThat(controller.isScrollInProgress).isTrue()
            scope.launch { controller.animateScrollBy(Offset(-100f, -100f), tween(1000)) }
        }

        rule.runOnIdle { assertThat(controller.isScrollInProgress).isTrue() }

        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(-75f)
            assertThat(total.y).isGreaterThan(-75f)
            assertThat(total.x).isLessThan(0f)
            assertThat(total.y).isLessThan(0f)
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    // b/179417109 Double checks that in a nested scroll cycle, the parent post scroll
    // consumption is taken into consideration.
    @Test
    fun dispatchScroll_shouldReturnConsumedDeltaInNestedScrollChain() {
        var consumedInner = Offset.Zero
        var consumedOuter = Offset.Zero

        var preScrollAvailable = Offset.Zero
        var consumedPostScroll = Offset.Zero
        var postScrollAvailable = Offset.Zero

        val outerStateController = Scrollable2DState {
            consumedOuter += it
            it
        }

        val innerController = Scrollable2DState {
            consumedInner += it / 2f
            it / 2f
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

        rule.setContent {
            WithTouchSlop(0f) {
                Box(modifier = Modifier.nestedScroll(connection)) {
                    Box(
                        modifier =
                            Modifier.testTag("outerScrollable")
                                .size(300.dp)
                                .scrollable2D(outerStateController)
                    ) {
                        Box(
                            modifier =
                                Modifier.testTag("innerScrollable")
                                    .size(300.dp)
                                    .scrollable2D(innerController)
                        )
                    }
                }
            }
        }

        val scrollDelta = 200f

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(scrollDelta, scrollDelta))
            up()
        }

        rule.runOnIdle {
            assertThat(consumedInner.x).isGreaterThan(0)
            assertThat(consumedInner.y).isGreaterThan(0)
            assertThat(consumedOuter.x).isGreaterThan(0)
            assertThat(consumedOuter.y).isGreaterThan(0)
            assertThat(postScrollAvailable.x).isEqualTo(0f)
            assertThat(consumedPostScroll.x).isEqualTo(scrollDelta)
            assertThat(preScrollAvailable.x).isEqualTo(scrollDelta)
            assertThat(scrollDelta).isEqualTo(consumedInner.x + consumedOuter.x)
            assertThat(scrollDelta).isEqualTo(consumedInner.y + consumedOuter.y)
        }
    }

    @Test
    fun testInspectorValue() {
        val controller = Scrollable2DState(consumeScrollDelta = { it })
        rule.setContentAndGetScope {
            val modifier = Modifier.scrollable2D(controller).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable2D")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "overscrollEffect",
                    "enabled",
                    "flingBehavior",
                    "interactionSource",
                )
        }
    }

    @Test
    fun producingEqualMaterializedModifierAfterRecomposition() {
        val state = Scrollable2DState { it }
        val counter = mutableStateOf(0)
        var materialized: Modifier? = null

        rule.setContent {
            counter.value // just to trigger recomposition
            materialized = currentComposer.materialize(Modifier.scrollable2D(state))
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
    fun scrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
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
        val controller = Scrollable2DState { _ -> Offset.Zero }

        setScrollable2DContent {
            Modifier.pointerInput(Unit) { savePointerInputEvents(tracker, this) }
                .nestedScroll(capturingScrollConnection)
                .scrollable2D(controller)
        }

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { swipeLeft() }

        // assert
        rule.runOnIdle {
            val diff = (velocity - tracker.calculateVelocity()).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { swipeRight() }

        // assert
        rule.runOnIdle {
            val diff = (velocity - tracker.calculateVelocity()).x.absoluteValue
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() {

        val controller = Scrollable2DState { Offset.Zero }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        lateinit var scroll2DScope: Scroll2DScope
        val adaptingScope =
            object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return scroll2DScope.scrollBy(Offset(pixels, 0f)).x
                }
            }
        setScrollable2DContent {
            defaultFlingBehavior = ScrollableDefaults.flingBehavior() as? DefaultFlingBehavior
            Modifier.scrollable2D(state = controller, flingBehavior = defaultFlingBehavior)
        }

        scope.launch {
            controller.scroll {
                scroll2DScope = this
                defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        // Simulate turning of animation
        scope.launch {
            controller.scroll {
                scroll2DScope = this
                withContext(TestScrollMotionDurationScale(0f)) {
                    defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
                }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }
    }

    @Test
    fun defaultFlingBehavior_useScrollMotionDurationScale() {

        val controller = Scrollable2DState { Offset.Zero }
        var defaultFlingBehavior: DefaultFlingBehavior? = null
        var switchMotionDurationScale by mutableStateOf(true)
        lateinit var scroll2DScope: Scroll2DScope
        val adaptingScope =
            object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return scroll2DScope.scrollBy(Offset(pixels, 0f)).x
                }
            }

        rule.setContentAndGetScope {
            val flingSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
            if (switchMotionDurationScale) {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(1f))
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .scrollable2D(state = controller, flingBehavior = defaultFlingBehavior)
                )
            } else {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(0f))
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .scrollable2D(state = controller, flingBehavior = defaultFlingBehavior)
                )
            }
        }

        scope.launch {
            controller.scroll {
                scroll2DScope = this
                defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        switchMotionDurationScale = false
        rule.waitForIdle()

        scope.launch {
            controller.scroll {
                scroll2DScope = this
                defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
            }
        }

        rule.runOnIdle { assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isEqualTo(1) }
    }

    @Test
    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease() {
        var values = Offset.Zero
        val scrollState = Scrollable2DState {
            values += it
            it
        }
        val delta = 10f

        rule.setContentAndGetScope {
            WithTouchSlop(0f) {
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag).size(100.dp).scrollable2D(scrollState)
                )
            }
        }

        var previousScrollValue = 0f
        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput {
            down(center)
            // generate various move events
            repeat(30) {
                moveBy(Offset(delta, delta), delayMillis = 8L)
                previousScrollValue += delta.toInt()
            }
            // stop for a moment
            advanceEventTime(3000L)
            up()
        }

        rule.runOnIdle {
            Assert.assertEquals((Offset(previousScrollValue, previousScrollValue)), values)
        }
    }

    @Test
    fun defaultScrollable2DState_scrollByWithNan_shouldFilterOutNan() {
        val controller = Scrollable2DState {
            assertThat(it.x).isNotNaN()
            assertThat(it.y).isNotNaN()
            Offset.Zero
        }

        val nanGenerator =
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    return scrollBy(Float.NaN)
                }
            }

        setScrollable2DContent {
            Modifier.scrollable2D(state = controller, flingBehavior = nanGenerator)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { swipeLeft() }
    }

    @Test
    fun equalInputs_shouldResolveToEquals() {
        val state = Scrollable2DState { Offset.Zero }

        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.scrollable2D(state, enabled = false)
            } else {
                Modifier.scrollable2D(state, enabled = true)
            }
        }
    }

    @Test
    fun enabledChange_semanticsShouldBeCleared() {
        var enabled by mutableStateOf(true)
        rule.setContentAndGetScope {
            Box(
                modifier =
                    Modifier.testTag(scrollable2DBoxTag)
                        .size(100.dp)
                        .scrollable2D(state = rememberScrollable2DState { it }, enabled = enabled)
            )
        }

        rule.onNodeWithTag(scrollable2DBoxTag).assert(SemanticsMatcher.keyIsDefined(ScrollBy))
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollByOffset))

        rule.runOnIdle { enabled = false }

        rule.onNodeWithTag(scrollable2DBoxTag).assert(SemanticsMatcher.keyNotDefined(ScrollBy))
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollByOffset))

        rule.runOnIdle { enabled = true }

        rule.onNodeWithTag(scrollable2DBoxTag).assert(SemanticsMatcher.keyIsDefined(ScrollBy))
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollByOffset))
    }

    @Test
    fun onDensityChange_shouldUpdateFlingBehavior() {
        var density by mutableStateOf(rule.density)
        var flingDelta = Offset.Zero
        val fixedSize = 400
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(
                    Modifier.size(with(density) { fixedSize.toDp() })
                        .testTag(scrollable2DBoxTag)
                        .scrollable2D(
                            state =
                                rememberScrollable2DState {
                                    flingDelta += it
                                    it
                                }
                        )
                )
            }
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { swipeUp() }

        rule.waitForIdle()

        density = Density(rule.density.density * 2f)
        val previousDelta = flingDelta
        flingDelta = Offset.Zero

        rule.onNodeWithTag(scrollable2DBoxTag).performTouchInput { swipeUp() }

        rule.runOnIdle {
            assertThat(flingDelta.x).isNotEqualTo(previousDelta.x)
            assertThat(flingDelta.y).isNotEqualTo(previousDelta.y)
        }
    }

    @Test
    fun onNestedFlingCancelled_shouldResetFlingState() {
        rule.mainClock.autoAdvance = false
        var outerStateDeltas = Offset.Zero
        val outerState = Scrollable2DState {
            outerStateDeltas += it
            it
        }

        val innerState = Scrollable2DState { it }

        val dispatcher = NestedScrollDispatcher()
        var flingJob: Job? = null

        rule.setContentAndGetScope {
            Box(
                Modifier.size(400.dp)
                    .background(Color.Red)
                    .scrollable2D(
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        state = outerState,
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
                        .scrollable2D(state = innerState)
                )
            }
        }

        rule.runOnIdle {
            // causes the inner scrollable to dispatch a post fling to the outer scrollable
            flingJob =
                scope.launch {
                    innerState.scroll {
                        dispatcher.dispatchPreFling(Velocity(10000f, 10000f))
                        dispatcher.dispatchPostFling(Velocity.Zero, Velocity(10000f, 10000f))
                    }
                }
        }

        rule.mainClock.advanceTimeBy(200L)

        rule.runOnIdle {
            // outer scrollable is flinging from onPostFling
            assertThat(outerStateDeltas.x).isNonZero()
            assertThat(outerStateDeltas.y).isNonZero()
        }

        outerStateDeltas = Offset.Zero

        rule.runOnIdle {
            flingJob?.cancel() // cancel job mid fling

            // try to run fling again
            scope.launch {
                innerState.scroll {
                    dispatcher.dispatchPreFling(Velocity(10000f, 10000f))
                    dispatcher.dispatchPostFling(Velocity.Zero, Velocity(10000f, 10000f))
                }
            }
        }

        rule.mainClock.autoAdvance = true
        // fling reached outer scrollable even if the previous child fling was cancelled.
        rule.runOnIdle {
            // outer scrollable is flinging from onPostFling
            assertThat(outerStateDeltas.x).isNonZero()
            assertThat(outerStateDeltas.y).isNonZero()
        }
    }

    private fun setScrollable2DContent(scrollableModifierFactory: @Composable () -> Modifier) {
        rule.setContentAndGetScope {
            Box {
                val scrollable = scrollableModifierFactory()
                Box(modifier = Modifier.testTag(scrollable2DBoxTag).size(100.dp).then(scrollable))
            }
        }
    }
}
