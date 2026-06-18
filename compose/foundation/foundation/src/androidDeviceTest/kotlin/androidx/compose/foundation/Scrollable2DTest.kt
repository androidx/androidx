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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.materialize
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Test

/**
 * [scrollable2D] tests that don't test device input.
 *
 * For testing touch, mouse, trackpad input, use one of the subclasses of [Scrollable2DInputTest].
 */
@LargeTest
class Scrollable2DTest : AbstractScrollable2DTest() {
    @Test
    fun scrollable_snappingScrolling() {
        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        setScrollable2DContent { Modifier.scrollable2D(state = scrollable2DState) }
        rule.waitForIdle()
        assertThat(total).isEqualTo(Offset.Zero)

        scope.launch { scrollable2DState.animateScrollBy(Offset(1000f, 1000f)) }
        rule.waitForIdle()
        assertThat(total.x).isWithin(0.001f).of(1000f)
        assertThat(total.y).isWithin(0.001f).of(1000f)

        scope.launch { scrollable2DState.animateScrollBy(Offset(-200f, -200f)) }
        rule.waitForIdle()
        assertThat(total.x).isWithin(0.001f).of(800f)
        assertThat(total.y).isWithin(0.001f).of(800f)
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
    fun scrollable_nestedScrollBelow_listensDispatches() {
        var value = Offset.Zero
        var expectedConsumed = Offset.Zero
        val scrollable2DState =
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
                Box(modifier = Modifier.size(300.dp).scrollable2D(state = scrollable2DState)) {
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
    fun scrollable_scrollByWorksWithRepeatableAnimations() {
        rule.mainClock.autoAdvance = false

        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = scrollable2DState))
        }

        rule.runOnIdle {
            scope.launch {
                scrollable2DState.animateScrollBy(
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
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = scrollable2DState))
        }

        lateinit var animateJob: Job

        rule.runOnIdle {
            animateJob =
                scope.launch { scrollable2DState.animateScrollBy(Offset(100f, 100f), tween(1000)) }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle { assertThat(scrollable2DState.isScrollInProgress).isTrue() }

        // Stop halfway through the animation
        animateJob.cancel()

        rule.runOnIdle { assertThat(scrollable2DState.isScrollInProgress).isFalse() }
    }

    @Test
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = Offset.Zero
        val scrollable2DState =
            Scrollable2DState(
                consumeScrollDelta = {
                    total += it
                    it
                }
            )
        rule.setContentAndGetScope {
            Box(modifier = Modifier.size(100.dp).scrollable2D(state = scrollable2DState))
        }

        rule.runOnIdle {
            scope.launch { scrollable2DState.animateScrollBy(Offset(100f, 100f), tween(1000)) }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(0f)
            assertThat(total.y).isGreaterThan(0f)
            assertThat(total.x).isLessThan(100f)
            assertThat(total.y).isLessThan(100f)
            assertThat(scrollable2DState.isScrollInProgress).isTrue()
            scope.launch { scrollable2DState.animateScrollBy(Offset(-100f, -100f), tween(1000)) }
        }

        rule.runOnIdle { assertThat(scrollable2DState.isScrollInProgress).isTrue() }

        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(total.x).isGreaterThan(-75f)
            assertThat(total.y).isGreaterThan(-75f)
            assertThat(total.x).isLessThan(0f)
            assertThat(total.y).isLessThan(0f)
            assertThat(scrollable2DState.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun testInspectorValue() {
        val scrollable2DState = Scrollable2DState(consumeScrollDelta = { it })
        rule.setContentAndGetScope {
            val modifier = Modifier.scrollable2D(scrollable2DState).first() as InspectableValue
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
    fun defaultFlingBehavior_useScrollMotionDurationScale() {
        val scrollable2DState = Scrollable2DState { Offset.Zero }
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
                            .scrollable2D(
                                state = scrollable2DState,
                                flingBehavior = defaultFlingBehavior,
                            )
                )
            } else {
                defaultFlingBehavior =
                    DefaultFlingBehavior(flingSpec, TestScrollMotionDurationScale(0f))
                Box(
                    modifier =
                        Modifier.testTag(scrollable2DBoxTag)
                            .size(100.dp)
                            .scrollable2D(
                                state = scrollable2DState,
                                flingBehavior = defaultFlingBehavior,
                            )
                )
            }
        }

        scope.launch {
            scrollable2DState.scroll {
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
            scrollable2DState.scroll {
                scroll2DScope = this
                defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
            }
        }

        rule.runOnIdle { assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isEqualTo(1) }
    }

    @Test
    fun defaultScrollable2DState_scrollByWithNan_shouldFilterOutNan() {
        val scrollable2DState = Scrollable2DState {
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
            Modifier.scrollable2D(state = scrollable2DState, flingBehavior = nanGenerator)
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
            flingJob?.cancel() // cancel job mid-fling

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

    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() {
        val scrollable2DState = Scrollable2DState { Offset.Zero }
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
            Modifier.scrollable2D(state = scrollable2DState, flingBehavior = defaultFlingBehavior)
        }

        scope.launch {
            scrollable2DState.scroll {
                scroll2DScope = this
                defaultFlingBehavior?.let { with(it) { adaptingScope.performFling(1000f) } }
            }
        }

        rule.runOnIdle {
            assertThat(defaultFlingBehavior?.lastAnimationCycleCount).isGreaterThan(1)
        }

        // Simulate turning of animation
        scope.launch {
            scrollable2DState.scroll {
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
}
