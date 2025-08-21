/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.TestScrollMotionDurationScale
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.matchers.assertThat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SnapFlingBehaviorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun remainingScrollOffset_cannotApproach_shouldRepresentJustSnappingOffsets() {
        val approachOffset = 0.0f
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = approachOffset)
        lateinit var testFlingBehavior: TargetedFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(testFlingBehavior, 0.0f) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        // Will Snap Back.
        rule.runOnIdle {
            assertEquals(scrollOffset.first(), approachOffset)
            assertEquals(scrollOffset[1], testLayoutInfoProvider.minOffset)
            assertEquals(scrollOffset.last(), 0f)
        }
    }

    @Test
    fun remainingScrollOffset_canApproach_shouldRepresentApproachAndSnapOffsets() {
        val approachOffset = 50f
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = approachOffset)
        lateinit var testFlingBehavior: TargetedFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(testFlingBehavior, TestVelocity) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        rule.runOnIdle {
            assertEquals(scrollOffset.first(), approachOffset)
            assertTrue(scrollOffset.contains(testLayoutInfoProvider.maxOffset))
            assertEquals(scrollOffset.last(), 0f)
        }
    }

    @Test
    fun remainingScrollOffset_targetShouldChangeInAccordanceWithAnimation() {
        // Arrange
        val initialOffset = 50f
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = initialOffset)
        lateinit var testFlingBehavior: TargetedFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(testFlingBehavior, TestVelocity) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        // assert the initial value emitted by remainingScrollOffset was the one provider by the
        // snap layout info provider
        assertEquals(scrollOffset.first(), initialOffset)

        // Act: Advance until we reach the maxOffset.
        rule.mainClock.advanceTimeUntil { scrollOffset.last() == testLayoutInfoProvider.maxOffset }

        rule.mainClock.autoAdvance = true
        // Assert
        rule.runOnIdle { assertEquals(scrollOffset.last(), 0f) }
    }

    @Test
    fun performFling_afterSnappingVelocity_everythingWasConsumed_shouldReturnNoVelocity() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        var afterFlingVelocity = 0f
        rule.setContent {
            val scrollableState = rememberScrollableState(consumeScrollDelta = { it })
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)

            LaunchedEffect(Unit) {
                scrollableState.scroll {
                    afterFlingVelocity = with(testFlingBehavior) { performFling(50000f) }
                }
            }
        }

        rule.runOnIdle { assertEquals(NoVelocity, afterFlingVelocity) }
    }

    @Test
    fun performFling_afterSnappingVelocity_didNotConsumeAllScroll_shouldReturnRemainingVelocity() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        var afterFlingVelocity = 0f
        rule.setContent {
            // Consume only half
            val scrollableState = rememberScrollableState(consumeScrollDelta = { it / 2f })
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)

            LaunchedEffect(Unit) {
                scrollableState.scroll {
                    afterFlingVelocity = with(testFlingBehavior) { performFling(50000f) }
                }
            }
        }

        rule.runOnIdle { assertNotEquals(NoVelocity, afterFlingVelocity) }
    }

    @Test
    fun performFling_invalidOffsets_shouldNotPropagateNans_calculateSnapOffset() {
        val testLayoutInfoProvider =
            object : SnapLayoutInfoProvider {
                override fun calculateSnapOffset(velocity: Float): Float = Float.NaN
            }
        lateinit var testFlingBehavior: TargetedFlingBehavior
        val exception =
            kotlin.runCatching {
                rule.setContent {
                    testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
                    VelocityEffect(testFlingBehavior, TestVelocity)
                }
            }
        assert(exception.isFailure)
    }

    @Test
    fun performFling_invalidOffsets_shouldNotPropagateNans_calculateApproachOffset() {
        val testLayoutInfoProvider =
            object : SnapLayoutInfoProvider {
                override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float =
                    Float.NaN

                override fun calculateSnapOffset(velocity: Float): Float = 0.0f
            }
        lateinit var testFlingBehavior: TargetedFlingBehavior
        val exception =
            kotlin.runCatching {
                rule.setContent {
                    testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
                    VelocityEffect(testFlingBehavior, TestVelocity)
                }
            }
        assert(exception.isFailure)
    }

    @Test
    fun findClosestOffset_noFlingDirection_shouldReturnAbsoluteDistance() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val offset = testLayoutInfoProvider.calculateSnapOffset(0f)
        assertEquals(offset, MinOffset)
    }

    @Test
    fun findClosestOffset_flingDirection_shouldReturnCorrectBound() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val forwardOffset =
            testLayoutInfoProvider.calculateSnapOffset(
                with(rule.density) { MinFlingVelocityDp.toPx() }
            )
        val backwardOffset =
            testLayoutInfoProvider.calculateSnapOffset(
                -with(rule.density) { MinFlingVelocityDp.toPx() }
            )
        assertEquals(forwardOffset, MaxOffset)
        assertEquals(backwardOffset, MinOffset)
    }

    @Test
    fun approach_cannotDecay_useLowVelocityApproachAndSnap() {
        val splineAnimationSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val decaySpec: DecayAnimationSpec<Float> = splineAnimationSpec.generateDecayAnimationSpec()
        val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())
        val canNotDecayApproach = decaySpec.calculateTargetValue(0.0f, TestVelocity) + 1
        val testLayoutInfoProvider =
            TestLayoutInfoProvider(maxOffset = 100f, approachOffset = canNotDecayApproach)

        rule.setContent {
            val testFlingBehavior =
                rememberSnapFlingBehavior(
                    snapLayoutInfoProvider = testLayoutInfoProvider,
                    highVelocityApproachSpec = decaySpec,
                    snapAnimationSpec = inspectSpringAnimationSpec,
                )
            VelocityEffect(testFlingBehavior, TestVelocity)
        }

        rule.runOnIdle {
            assertEquals(0, splineAnimationSpec.animationWasExecutions)
            assertEquals(2, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun approach_canDecay_useHighVelocityApproachAndSnap() {
        val splineAnimationSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val decaySpec: DecayAnimationSpec<Float> = splineAnimationSpec.generateDecayAnimationSpec()
        val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())

        val canDecayApproach = decaySpec.calculateTargetValue(0.0f, TestVelocity) - 1
        val testLayoutInfoProvider =
            TestLayoutInfoProvider(maxOffset = 100f, approachOffset = canDecayApproach)

        rule.setContent {
            val testFlingBehavior =
                rememberSnapFlingBehavior(
                    snapLayoutInfoProvider = testLayoutInfoProvider,
                    highVelocityApproachSpec = decaySpec,
                    snapAnimationSpec = inspectSpringAnimationSpec,
                )
            VelocityEffect(testFlingBehavior, TestVelocity)
        }

        rule.runOnIdle {
            assertEquals(1, splineAnimationSpec.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun approach_usedDefaultApproach_useHighVelocityApproachAndSnap() {
        val splineAnimationSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val decaySpec: DecayAnimationSpec<Float> = splineAnimationSpec.generateDecayAnimationSpec()
        val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())

        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = Float.NaN)

        rule.setContent {
            val testFlingBehavior =
                rememberSnapFlingBehavior(
                    snapLayoutInfoProvider = testLayoutInfoProvider,
                    highVelocityApproachSpec = decaySpec,
                    snapAnimationSpec = inspectSpringAnimationSpec,
                )
            VelocityEffect(testFlingBehavior, 5 * TestVelocity)
        }

        rule.runOnIdle {
            assertEquals(1, splineAnimationSpec.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun approach_usedDefaultApproach_shouldDecay() {
        val splineAnimationSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val decaySpec: DecayAnimationSpec<Float> = splineAnimationSpec.generateDecayAnimationSpec()
        val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())

        val flingVelocity = 5 * TestVelocity
        val decayTargetOffset = decaySpec.calculateTargetValue(0.0f, flingVelocity)
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = Float.NaN)
        var actualApproachOffset = 0f

        rule.mainClock.autoAdvance = false

        rule.setContent {
            val testFlingBehavior =
                rememberSnapFlingBehavior(
                    snapLayoutInfoProvider = testLayoutInfoProvider,
                    highVelocityApproachSpec = decaySpec,
                    snapAnimationSpec = inspectSpringAnimationSpec,
                )
            VelocityEffect(testFlingBehavior, flingVelocity) {
                actualApproachOffset = it // note approach offset
            }
        }

        // wait for approach to start executing
        rule.mainClock.advanceTimeUntil { splineAnimationSpec.animationWasExecutions > 0 }

        rule.runOnIdle {
            Truth.assertThat(decayTargetOffset).isWithin(0.1f).of(actualApproachOffset)
        }
    }

    @Test
    fun approach_cannotDecay_shouldJustSnapToBound() {
        val splineAnimationSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val decaySpec: DecayAnimationSpec<Float> = splineAnimationSpec.generateDecayAnimationSpec()
        val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())

        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = MaxOffset)

        var animationOffset = 0f
        rule.setContent {
            val testFlingBehavior =
                rememberSnapFlingBehavior(
                    snapLayoutInfoProvider = testLayoutInfoProvider,
                    highVelocityApproachSpec = decaySpec,
                    snapAnimationSpec = inspectSpringAnimationSpec,
                )
            VelocityEffect(testFlingBehavior, TestVelocity) {
                // note animation offset
                if (it > animationOffset) {
                    animationOffset = it
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, splineAnimationSpec.animationWasExecutions)
            Truth.assertThat(animationOffset).isWithin(0.1f).of(MaxOffset)
        }
    }

    @Suppress("Deprecation")
    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() {

        lateinit var defaultFlingBehavior: SnapFlingBehavior
        lateinit var scope: CoroutineScope
        val state = LazyListState()
        rule.setContent {
            scope = rememberCoroutineScope()
            defaultFlingBehavior = rememberSnapFlingBehavior(state) as SnapFlingBehavior

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                flingBehavior = defaultFlingBehavior as FlingBehavior,
            ) {
                items(200) { Box(modifier = Modifier.size(20.dp)) }
            }
        }

        // Act: Stop clock and fling, one frame should not settle immediately.
        rule.mainClock.autoAdvance = false
        scope.launch { state.scroll { with(defaultFlingBehavior) { performFling(10000f) } } }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(0) }

        rule.mainClock.autoAdvance = true

        val previousIndex = state.firstVisibleItemIndex

        // Simulate turning off system wide animation
        scope.launch {
            state.scroll {
                withContext(TestScrollMotionDurationScale(0f)) {
                    with(defaultFlingBehavior) { performFling(10000f) }
                }
            }
        }

        // Act: Stop clock and fling, one frame should not settle immediately.
        rule.mainClock.autoAdvance = false
        scope.launch { state.scroll { with(defaultFlingBehavior) { performFling(10000f) } } }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(previousIndex) }

        rule.mainClock.autoAdvance = true

        // Assert: let it settle
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isNotEqualTo(previousIndex) }
    }

    @Suppress("Deprecation")
    @Test
    fun defaultFlingBehavior_useScrollMotionDurationScale() {
        // Arrange
        var switchMotionDurationScale by mutableStateOf(false)
        lateinit var defaultFlingBehavior: SnapFlingBehavior
        lateinit var scope: CoroutineScope
        val state = LazyListState()
        rule.setContent {
            scope = rememberCoroutineScope()
            defaultFlingBehavior = rememberSnapFlingBehavior(state) as SnapFlingBehavior

            LazyRow(
                modifier = Modifier.testTag("snappingList").fillMaxSize(),
                state = state,
                flingBehavior = defaultFlingBehavior as FlingBehavior,
            ) {
                items(200) {
                    Box(modifier = Modifier.size(150.dp)) { BasicText(text = it.toString()) }
                }
            }

            if (switchMotionDurationScale) {
                defaultFlingBehavior.motionScaleDuration = TestScrollMotionDurationScale(1f)
            } else {
                defaultFlingBehavior.motionScaleDuration = TestScrollMotionDurationScale(0f)
            }
        }

        // Act: Stop clock and fling, one frame should settle immediately.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("snappingList").performTouchInput {
            swipeWithVelocity(centerRight, center, 10000f)
        }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isGreaterThan(0) }

        // Arrange
        rule.mainClock.autoAdvance = true
        switchMotionDurationScale = true // Let animations run normally
        rule.waitForIdle()

        val previousIndex = state.firstVisibleItemIndex
        // Act: Stop clock and fling, one frame should not settle.
        rule.mainClock.autoAdvance = false
        scope.launch { state.scroll { with(defaultFlingBehavior) { performFling(10000f) } } }

        // Assert: First index hasn't changed because animation hasn't started
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(previousIndex) }
        rule.mainClock.autoAdvance = true

        // Wait for settling
        rule.runOnIdle { Truth.assertThat(state.firstVisibleItemIndex).isNotEqualTo(previousIndex) }
    }

    @Test
    fun performFling_cancellationShouldTriggerAnimationCancellation() {
        val splineSpec =
            InspectSplineAnimationSpec(SplineBasedFloatDecayAnimationSpec(rule.density))
        val splineAnimation = splineSpec.generateDecayAnimationSpec<Float>()
        val springSpec = InspectSpringAnimationSpec(spring())
        val scrollScope =
            object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    throw CancellationException()
                }
            }

        val flingBehavior =
            snapFlingBehavior(
                TestLayoutInfoProvider(),
                decayAnimationSpec = splineAnimation,
                snapAnimationSpec = springSpec,
            )

        rule.runOnUiThread {
            runBlocking(AutoTestFrameClock()) {
                with(scrollScope) { with(flingBehavior) { performFling(3000f) } }
            }
        }

        rule.runOnIdle { assertEquals(0, splineSpec.animationWasExecutions) }
    }

    inner class TestLayoutInfoProvider(
        val minOffset: Float = MinOffset,
        val maxOffset: Float = MaxOffset,
        val approachOffset: Float = 0f,
    ) : SnapLayoutInfoProvider {

        private fun calculateFinalSnappingItem(velocity: Float): FinalSnappingItem {
            return if (velocity.absoluteValue == 0.0f) {
                FinalSnappingItem.ClosestItem
            } else {
                if (velocity > 0) FinalSnappingItem.NextItem else FinalSnappingItem.PreviousItem
            }
        }

        override fun calculateSnapOffset(velocity: Float): Float {
            return calculateFinalOffset(calculateFinalSnappingItem(velocity), minOffset, maxOffset)
        }

        override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
            return if (approachOffset.isNaN()) (decayOffset) else approachOffset
        }
    }
}

@Suppress("Deprecation")
@Composable
private fun VelocityEffect(
    testFlingBehavior: FlingBehavior,
    velocity: Float,
    onSettlingDistanceUpdated: (Float) -> Unit = {},
) {
    val scrollableState = rememberScrollableState(consumeScrollDelta = { it })
    LaunchedEffect(Unit) {
        scrollableState.scroll {
            with(testFlingBehavior as SnapFlingBehavior) {
                performFling(velocity, onSettlingDistanceUpdated)
            }
        }
    }
}

private class InspectSpringAnimationSpec(private val animation: AnimationSpec<Float>) :
    AnimationSpec<Float> {

    var animationWasExecutions = 0

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedAnimationSpec<V> {
        animationWasExecutions++
        return animation.vectorize(converter)
    }
}

private class InspectSplineAnimationSpec(
    private val splineBasedFloatDecayAnimationSpec: SplineBasedFloatDecayAnimationSpec
) : FloatDecayAnimationSpec by splineBasedFloatDecayAnimationSpec {

    private var valueFromNanosCalls = 0
    val animationWasExecutions: Int
        get() = valueFromNanosCalls / 2

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float,
    ): Float {

        if (playTimeNanos == 0L) {
            valueFromNanosCalls++
        }

        return splineBasedFloatDecayAnimationSpec.getValueFromNanos(
            playTimeNanos,
            initialValue,
            initialVelocity,
        )
    }
}

private const val TestVelocity = 1000f
private const val MinOffset = -200f
private const val MaxOffset = 300f

@Composable
private fun rememberSnapFlingBehavior(
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    highVelocityApproachSpec: DecayAnimationSpec<Float>,
    snapAnimationSpec: AnimationSpec<Float>,
): FlingBehavior {

    return remember(snapLayoutInfoProvider, highVelocityApproachSpec) {
        snapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            decayAnimationSpec = highVelocityApproachSpec,
            snapAnimationSpec = snapAnimationSpec,
        )
    }
}
