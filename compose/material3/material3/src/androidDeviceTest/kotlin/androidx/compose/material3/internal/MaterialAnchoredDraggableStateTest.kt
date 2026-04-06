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

package androidx.compose.material3.internal

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.MonotonicFrameClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialAnchoredDraggableStateTest {

    private val AnimationSpec = tween<Float>(durationMillis = 16)
    private val DecayAnimationSpec = exponentialDecay<Float>()

    private enum class TestValue {
        A,
        B,
        C,
    }

    private class PseudoFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            // We use the system time, but in a real test environment with StandardTestDispatcher,
            // this simply allows the coroutine to proceed.
            return onFrame(System.nanoTime())
        }
    }

    @Test
    fun materialAnchoredDraggableState_secondaryConstructor_initialization() {
        val state = MaterialAnchoredDraggableState(initialValue = TestValue.A)
        assertThat(state.currentValue).isEqualTo(TestValue.A)
        assertThat(state.targetValue).isEqualTo(TestValue.A)
    }

    @Test
    fun materialAnchoredDraggableState_settle_onRelease_reachesTarget() =
        runTest(PseudoFrameClock()) {
            val state =
                MaterialAnchoredDraggableState(
                    initialValue = TestValue.A,
                    positionalThreshold = { distance -> distance * 0.3f }, // 30%
                    velocityThreshold = { 100f },
                    animationSpec = AnimationSpec,
                    decayAnimationSpec = DecayAnimationSpec,
                )
            val anchors = DraggableAnchors {
                TestValue.A at 0f
                TestValue.B at 100f
            }
            state.updateAnchors(anchors)

            // 1. Drag past threshold but below 50%
            state.dispatchRawDelta(40f)
            assertThat(state.targetValue).isEqualTo(TestValue.B)

            // 2. "Release" with 0 velocity
            state.settle(velocity = 0f)

            // 3. Should have settled at B
            assertThat(state.currentValue).isEqualTo(TestValue.B)
            assertThat(state.requireOffset()).isEqualTo(100f)
        }

    @Test
    fun materialAnchoredDraggableState_currentValue_remainsAtSettledValueDuringDrag() {
        val state =
            MaterialAnchoredDraggableState(
                initialValue = TestValue.A,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { 0f },
                animationSpec = AnimationSpec,
                decayAnimationSpec = DecayAnimationSpec,
            )
        val anchors = DraggableAnchors {
            TestValue.A at 0f
            TestValue.B at 100f
        }
        state.updateAnchors(anchors)

        // Drag 60% of the way to B
        state.dispatchRawDelta(60f)

        // In Material3, currentValue is settledValue, so it should still be A
        assertThat(state.currentValue).isEqualTo(TestValue.A)
        // closestValue should track the nearest anchor (B)
        assertThat(state.closestValue).isEqualTo(TestValue.B)
    }

    @Test
    fun materialAnchoredDraggableState_targetValue_respectsThresholdDuringDrag() {
        val state =
            MaterialAnchoredDraggableState(
                initialValue = TestValue.A,
                positionalThreshold = { _ -> 25f }, // 25% threshold for a 100px distance
                velocityThreshold = { 0f },
                animationSpec = AnimationSpec,
                decayAnimationSpec = DecayAnimationSpec,
            )
        val anchors = DraggableAnchors {
            TestValue.A at 0f
            TestValue.B at 100f
        }
        state.updateAnchors(anchors)

        // Drag 30px (past the 25px threshold)
        state.dispatchRawDelta(30f)

        // targetValue should update to B because we crossed the Material3 threshold,
        // even though we haven't crossed the 50% mark foundation typically uses for targetValue.
        assertThat(state.targetValue).isEqualTo(TestValue.B)

        // Drag back to 20px (below threshold)
        state.dispatchRawDelta(-10f)
        assertThat(state.targetValue).isEqualTo(TestValue.A)
    }

    @Test
    fun materialAnchoredDraggableState_calculateTargetValue_isStableAtClashingAnchors() {
        val state =
            MaterialAnchoredDraggableState(
                initialValue = TestValue.A,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { 0f },
                animationSpec = AnimationSpec,
                decayAnimationSpec = DecayAnimationSpec,
            )
        // Two anchors at the same position
        val anchors = DraggableAnchors {
            TestValue.A at 0f
            TestValue.B at 0f
            TestValue.C at 100f
        }
        state.updateAnchors(anchors)

        // Even if there's a clash, if we are at A's position, we should stay at A
        assertThat(state.targetValue).isEqualTo(TestValue.A)
    }

    @Test
    fun materialAnchoredDraggableState_targetValue_respectsVeto() {
        val state =
            MaterialAnchoredDraggableState(
                initialValue = TestValue.A,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { 0f },
                animationSpec = AnimationSpec,
                decayAnimationSpec = DecayAnimationSpec,
                confirmValueChange = { newValue -> newValue != TestValue.B }, // Veto B
            )
        val anchors = DraggableAnchors {
            TestValue.A at 0f
            TestValue.B at 100f
            TestValue.C at 200f
        }
        state.updateAnchors(anchors)

        // Drag past B's threshold
        state.dispatchRawDelta(110f)

        // B is vetoed, so targetValue should remain A (or skip to C if threshold crossed,
        // but here we only crossed B's)
        assertThat(state.targetValue).isEqualTo(TestValue.A)

        // Drag past C's threshold (threshold for C is 0.5 * 200 = 100)
        state.dispatchRawDelta(50f) // Total 160
        assertThat(state.targetValue).isEqualTo(TestValue.C)
    }
}
