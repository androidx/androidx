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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.DifferentialVelocityTracker
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.Scrollable2DState
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue
import org.junit.Assert
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Base class for [scrollable2D] tests via touch, mouse-wheel and trackpad. */
abstract class Scrollable2DInputTest : AbstractScrollable2DTest() {
    /**
     * Whether fling behavior needs to be tested with the corresponding input device.
     *
     * When this return false, it also means [performScrollGestureWithVelocity] is not supported.
     */
    abstract val testFlingBehavior: Boolean

    /**
     * The factor of tolerance for fling velocity assertions.
     *
     * This is needed because currently, the velocity tracker for trackpad
     * ([DifferentialVelocityTracker]) is rather inaccurate.
     *
     * For example, in [scrollable_flingBehaviourCalled], the requested velocity is 1000, but the
     * velocity it produces is ~970. This is due to it using [VelocityTracker1D.Strategy.Impulse]
     * instead of [VelocityTracker1D.Strategy.Lsq2]
     */
    abstract val flingVelocityComparisonFactor: Float

    /** Performs the device-specific gesture for scrolling by [delta] over [durationMillis] */
    abstract fun SemanticsNodeInteraction.performScrollGesture(
        start: InjectionScope.() -> Offset = { center },
        delta: InjectionScope.() -> Offset,
        durationMillis: Long = 200,
        preventFling: Boolean = false,
        also: InjectionScope.() -> Unit = {},
    )

    /**
     * Performs the device-specific gesture for scrolling by [delta] over [durationMillis], with the
     * given [endVelocity].
     */
    abstract fun SemanticsNodeInteraction.performScrollGestureWithVelocity(
        start: InjectionScope.() -> Offset = { center },
        delta: InjectionScope.() -> Offset,
        endVelocity: Float,
        durationMillis: Long? = null,
        also: InjectionScope.() -> Unit = {},
    )

    /** A version of [performScrollGesture] that takes an immediate [delta] */
    fun SemanticsNodeInteraction.performScrollGesture(
        start: InjectionScope.() -> Offset = { center },
        delta: Offset,
        durationMillis: Long = 200,
        preventFling: Boolean = false,
        also: InjectionScope.() -> Unit = {},
    ) {
        performScrollGesture(
            start = start,
            delta = { delta },
            durationMillis = durationMillis,
            preventFling = preventFling,
            also = also,
        )
    }

    /** A version of [performScrollGestureWithVelocity] that takes an immediate [delta] */
    fun SemanticsNodeInteraction.performScrollGestureWithVelocity(
        start: InjectionScope.() -> Offset = { center },
        delta: Offset,
        endVelocity: Float,
        durationMillis: Long? = null,
        also: InjectionScope.() -> Unit = {},
    ) =
        performScrollGestureWithVelocity(
            start = start,
            delta = { delta },
            endVelocity = endVelocity,
            durationMillis = durationMillis,
            also = also,
        )

    /** Performs a device-specific scroll-down gesture. */
    fun SemanticsNodeInteraction.performScrollDownGesture(
        start: InjectionScope.() -> Float = { top },
        delta: InjectionScope.() -> Float = { bottom - start() },
        durationMillis: Long = 200,
        also: InjectionScope.() -> Unit = {},
    ) {
        performScrollGesture(
            start = { Offset(centerX, this.start()) },
            delta = { Offset(0f, this.delta()) },
            durationMillis = durationMillis,
            also = also,
        )
    }

    /** Performs a device-specific scroll-up gesture. */
    fun SemanticsNodeInteraction.performScrollUpGesture(
        start: InjectionScope.() -> Float = { bottom },
        delta: InjectionScope.() -> Float = { top - start() },
        durationMillis: Long = 200,
        also: InjectionScope.() -> Unit = {},
    ) {
        performScrollGesture(
            start = { Offset(centerX, this.start()) },
            delta = { Offset(0f, this.delta()) },
            durationMillis = durationMillis,
            also = also,
        )
    }

    /** Performs a device-specific scroll-left gesture. */
    fun SemanticsNodeInteraction.performScrollLeftGesture(
        start: InjectionScope.() -> Float = { right },
        delta: InjectionScope.() -> Float = { left - start() },
        durationMillis: Long = 200,
        also: InjectionScope.() -> Unit = {},
    ) {
        performScrollGesture(
            start = { Offset(this.start(), centerY) },
            delta = { Offset(this.delta(), 0f) },
            durationMillis = durationMillis,
            also = also,
        )
    }

    /** Performs a device-specific scroll-right gesture. */
    fun SemanticsNodeInteraction.performScrollRightGesture(
        start: InjectionScope.() -> Float = { left },
        delta: InjectionScope.() -> Float = { right - start() },
        durationMillis: Long = 200,
        also: InjectionScope.() -> Unit = {},
    ) {
        performScrollGesture(
            start = { Offset(this.start(), centerY) },
            delta = { Offset(this.delta(), 0f) },
            durationMillis = durationMillis,
            also = also,
        )
    }

    @Test
    fun scrollable_horizontalScroll() {
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = scrollable2DState) }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(100f, 0f), durationMillis = 100)
        rule.runOnIdle { assertThat(total.x).isGreaterThan(1f) }

        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(-100f, 0f), durationMillis = 100)
        rule.runOnIdle { assertThat(total.x).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_verticalScroll() {
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = scrollable2DState) }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(0f, 100f), durationMillis = 100)
        rule.runOnIdle { assertThat(total.y).isGreaterThan(1f) }

        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(0f, -100f), durationMillis = 100)
        rule.runOnIdle { assertThat(total.y).isLessThan(0.01f) }
    }

    @Test
    fun scrollable_diagonalScroll() {
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = scrollable2DState) }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(100f, 100f), durationMillis = 100)
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(1f)
            assertThat(total.y).isGreaterThan(1f)
        }

        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(-100f, -100f), durationMillis = 100)
        rule.runOnIdle {
            assertThat(total.x).isLessThan(0.01f)
            assertThat(total.y).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_disabledWontCallLambda() {
        val enabled = mutableStateOf(true)
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent {
            Modifier.scrollable2D(state = scrollable2DState, enabled = enabled.value)
        }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(100f, 100f), durationMillis = 100)
        val prevTotal =
            rule.runOnIdle {
                assertThat(total.x).isGreaterThan(0f)
                assertThat(total.y).isGreaterThan(0f)
                enabled.value = false
                total
            }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(100f, 100f), durationMillis = 100)
        rule.runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() {
        assumeTrue(testFlingBehavior)

        rule.mainClock.autoAdvance = false
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = scrollable2DState) }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(
                delta = Offset(200f, 200f),
                endVelocity = 4000f,
                durationMillis = 100,
            )
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
        assumeTrue(testFlingBehavior)

        rule.mainClock.autoAdvance = false
        var total = Offset.Zero
        val scrollable2DState =
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
                    modifier = Modifier.size(300.dp).scrollable2D(state = scrollable2DState),
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
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(
                delta = Offset(200f, 200f),
                endVelocity = 4000f,
                durationMillis = 100,
            )
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
    fun scrollable_explicitDisposal() {
        assumeTrue(testFlingBehavior)

        rule.mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = Offset.Zero
        val scrollable2DState =
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
                Modifier.scrollable2D(state = scrollable2DState)
            } else {
                Modifier
            }
        }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(
                delta = Offset(200f, 200f),
                endVelocity = 4000f,
                durationMillis = 100,
            )
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

        // still same and didn't fail in onScrollConsumptionRequested lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    fun scrollable_nestedDrag() {
        assumeTrue(testFlingBehavior)

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
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(
                delta = Offset(200f, 200f),
                endVelocity = 0f,
                durationMillis = 300,
            )
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
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(200f, 200f), durationMillis = 300)
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
        val scrollable2DState =
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
                                .scrollable2D(state = scrollable2DState)
                    )
                }
            }
        }

        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(200f, 200f), durationMillis = 300)

        val preFlingValue = rule.runOnIdle { value }
        rule.runOnIdle {
            // if scrollable respects pre-fling consumption, it should fling 0px since we
            // pre-consume all
            assertThat(preFlingValue).isEqualTo(value)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_proxiesPostCycles() {
        assumeTrue(testFlingBehavior)

        var value = Offset.Zero
        var expectedLeft = Offset.Zero
        val velocityFlung = 5000f
        val scrollable2DState =
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
                    // we should get in post scroll as much as left in scrollable2DState callback
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
                                .scrollable2D(state = scrollable2DState)
                    )
                }
            }
        }

        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(
                delta = Offset(500f, 500f),
                endVelocity = velocityFlung,
                durationMillis = 300,
            )

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() {
        var childValue = Offset.Zero
        var parentValue = Offset.Zero
        val childScrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    childValue += it
                    it
                }
            )
        val parentScrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    parentValue += it
                    it
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier.size(300.dp).scrollable2D(state = parentScrollable2DState)
                ) {
                    Box(
                        Modifier.size(200.dp)
                            .testTag(scrollable2DBoxTag)
                            .scrollable2D(enabled = false, state = childScrollable2DState)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(parentValue).isEqualTo(Offset.Zero)
            assertThat(childValue).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag(scrollable2DBoxTag).performScrollGesture(delta = Offset(100f, 100f))

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
        val childScrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    childValue += it / 2f
                    it / 2f
                }
            )
        val middleScrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    selfValue += it / 2f
                    it / 2f
                }
            )
        val parentScrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    parentValue += it / 2f
                    it / 2f
                }
            )

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier.size(300.dp).scrollable2D(state = parentScrollable2DState)
                ) {
                    Box(
                        Modifier.size(200.dp)
                            .scrollable2D(enabled = false, state = middleScrollable2DState)
                    ) {
                        Box(
                            Modifier.size(200.dp)
                                .testTag(scrollable2DBoxTag)
                                .scrollable2D(state = childScrollable2DState)
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

        rule.onNodeWithTag(scrollable2DBoxTag).performScrollGesture(delta = Offset(100f, 100f))

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
        assumeTrue(testFlingBehavior)

        var childDeltas = Offset.Zero
        val childScrollable2DState = Scrollable2DState {
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
                                .scrollable2D(childScrollable2DState)
                    )
                }
            }
        }

        // First drag, this won't trigger the cancellation flow.
        rule
            .onNodeWithTag("childScrollable")
            .performScrollGesture(
                start = { centerLeft },
                delta = Offset(100f, 100f),
                preventFling = true,
            )

        rule.runOnIdle { assertThat(childDeltas).isEqualTo(Offset(100f, 100f)) }

        childDeltas = Offset.Zero
        var dragged = Offset.Zero
        rule
            .onNodeWithTag("childScrollable")
            .performScrollGestureWithVelocity(
                start = { centerLeft },
                delta = { topRight - centerLeft },
                endVelocity = 2000f,
                also = { dragged = topRight - centerLeft },
            )

        // child didn't receive more deltas after drag, because fling was canceled by the parent.
        // Comparison is approximate because childDeltas is the result of many additions, and is
        // therefore not precise.
        assertThat(childDeltas.x).isWithin(0.5f).of(dragged.x)
        assertThat(childDeltas.y).isWithin(0.5f).of(dragged.y)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollable_nestedFling_shouldCancelWhenHitTheBounds_ifRemoved() {
        assumeTrue(testFlingBehavior)

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
        rule.onNodeWithTag("scrollable").performScrollDownGesture { swipeSize = bottom - top }

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
        assumeTrue(testFlingBehavior)

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
        rule.onNodeWithTag("column").performScrollDownGesture(start = { centerY })

        rule.mainClock.advanceTimeBy(200)
        simulateHitBounds = true

        flingDeltas = Offset.Zero
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertThat(flingDeltas.y).isNonZero()
    }

    @Test
    fun scrollable_nestedFling_parentShouldFlingWithVelocityLeft() {
        assumeTrue(testFlingBehavior)

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

        rule.onNodeWithTag("column").performScrollDownGesture()

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
        assumeTrue(testFlingBehavior)

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

        rule.onNodeWithTag("column").performScrollUpGesture()
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
    fun scrollable_flingBehaviourCalled_whenVelocity0() {
        assumeTrue(testFlingBehavior)

        var total = Offset.Zero
        val scrollable2DState =
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
            Modifier.scrollable2D(state = scrollable2DState, flingBehavior = flingBehaviour)
        }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGesture(delta = Offset(115f, 0f), preventFling = true)
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isLessThan(0.01f)
        assertThat(flingVelocity).isGreaterThan(-0.01f)
    }

    @Test
    fun scrollable_flingBehaviourCalled() {
        assumeTrue(testFlingBehavior)

        var total = Offset.Zero
        val scrollable2DState =
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
            Modifier.scrollable2D(state = scrollable2DState, flingBehavior = flingBehaviour)
        }
        rule
            .onNodeWithTag(scrollable2DBoxTag)
            .performScrollGestureWithVelocity(delta = Offset(115f, 0f), endVelocity = 1000f)
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(1000f * flingVelocityComparisonFactor).of(1000f)
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

        val outerScrollable2DState = Scrollable2DState {
            consumedOuter += it
            it
        }

        val innerScrollable2DState = Scrollable2DState {
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
                                .scrollable2D(outerScrollable2DState)
                    ) {
                        Box(
                            modifier =
                                Modifier.testTag("innerScrollable")
                                    .size(300.dp)
                                    .scrollable2D(innerScrollable2DState)
                        )
                    }
                }
            }
        }

        val scrollDelta = 200f

        rule
            .onRoot()
            .performScrollGesture(delta = Offset(scrollDelta, scrollDelta), preventFling = true)

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
    fun onDensityChange_shouldUpdateFlingBehavior() {
        assumeTrue(testFlingBehavior)

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

        rule.onNodeWithTag(scrollable2DBoxTag).performScrollUpGesture()

        rule.waitForIdle()

        density = Density(rule.density.density * 2f)
        val previousDelta = flingDelta
        flingDelta = Offset.Zero

        rule.onNodeWithTag(scrollable2DBoxTag).performScrollUpGesture()

        rule.runOnIdle { assertThat(flingDelta).isNotEqualTo(previousDelta) }
    }

    fun scrollable_noMomentum_shouldChangeScrollStateAfterRelease(
        performInputAndGetScrollValue: SemanticsNodeInteraction.(delta: Float) -> Offset
    ) {
        var values = Offset.Zero
        val scrollState = Scrollable2DState {
            values += it
            it
        }

        rule.setContentAndGetScope {
            WithTouchSlop(0f) {
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag).size(100.dp).scrollable2D(scrollState)
                )
            }
        }

        val scrollValue = rule.onNodeWithTag(scrollable2DBoxTag).performInputAndGetScrollValue(10f)

        rule.runOnIdle { Assert.assertEquals(scrollValue, values) }
    }
}
