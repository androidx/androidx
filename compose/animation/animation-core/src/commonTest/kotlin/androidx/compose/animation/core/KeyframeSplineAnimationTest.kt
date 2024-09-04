/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalAnimationSpecApi::class)
class KeyframeSplineAnimationTest {

    /** See [MonoSplineTest] to test the interpolation curves. */
    @Test
    fun interpolatedValues() {
        val animation =
            keyframesWithSpline {
                    durationMillis = 500

                    // At half the time, reach the final Y value (expected final value is Offset(2f,
                    // 2f))
                    Offset(1f, 2f) at 250
                }
                .vectorize(Offset.VectorConverter)

        // Test at a quarter of the time
        assertEquals(AnimationVector2D(0.5f, 1.25f), animation.valueAt(125))
        assertEquals(AnimationVector2D(4f, 10f), animation.velocityAt(125))

        // Test at three quarters of the time
        assertEquals(AnimationVector2D(1.5f, 2f), animation.valueAt(375))
        // Change for X is completely linear, so speed is constant (4f)
        // No changes for Y, so corresponding velocity is 0
        assertEquals(AnimationVector2D(4f, 0f), animation.velocityAt(375))

        // Change start/end value, the animation value and velocity should update to reflect it
        assertEquals(
            AnimationVector2D(0.5f, 1.25f),
            animation.valueAt(375, start = Offset(2f, 2f), end = Offset(0f, 0f))
        )
        assertEquals(
            AnimationVector2D(-4f, -10f),
            animation.velocityAt(375, start = Offset(2f, 2f), end = Offset(0f, 0f))
        )
    }

    // Tests the expected effect that different periodic bias have compared against a regular spline
    @Test
    fun interpolatedValues_periodic() {
        // Testing a curve with points at [0, 1, 0], where the intermediate point is at half of the
        // duration
        // This means that on a linear interpolation, the initial velocity (past 0ms) would be
        // positive and the final velocity would be negative with the same magnitude
        val sharedConfig =
            KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<Float>().apply {
                durationMillis = 1000

                1f at 500 using LinearEasing
            }

        // We'll compare different periodic bias against the regular spline
        val splineAnimation =
            KeyframesWithSplineSpec(sharedConfig, Float.NaN).vectorize(Float.VectorConverter)
        val splineAnimationBalancedBias =
            KeyframesWithSplineSpec(sharedConfig, 0.5f).vectorize(Float.VectorConverter)
        val splineAnimationStartBias =
            KeyframesWithSplineSpec(sharedConfig, 0f).vectorize(Float.VectorConverter)
        val splineAnimationEndBias =
            KeyframesWithSplineSpec(sharedConfig, 1f).vectorize(Float.VectorConverter)

        // Periodic bias affect the velocity at the start and end of the animation.
        // Note that we don't test at exactly 0 since that would always return the initial velocity
        fun VectorizedDurationBasedAnimationSpec<AnimationVector1D>.startVelocity():
            AnimationVector1D {
            return getVelocityFromNanos(
                    playTimeNanos = 1L,
                    initialValue = AnimationVector1D(0f),
                    targetValue = AnimationVector1D(0f),
                    initialVelocity = AnimationVector1D(0f)
                )
                .let { AnimationVector1D(it.value) } // Copy since vector is reused internally
        }
        fun VectorizedDurationBasedAnimationSpec<AnimationVector1D>.endVelocity():
            AnimationVector1D {
            return getVelocityFromNanos(
                    playTimeNanos = 1000 * MillisToNanos,
                    initialValue = AnimationVector1D(0f),
                    targetValue = AnimationVector1D(0f),
                    initialVelocity = AnimationVector1D(0f)
                )
                .let { AnimationVector1D(it.value) } // Copy since vector is reused internally
        }

        val regularV0 = splineAnimation.startVelocity()
        val regularV1 = splineAnimation.endVelocity()

        val balancedV0 = splineAnimationBalancedBias.startVelocity()
        val balancedV1 = splineAnimationBalancedBias.endVelocity()

        val startBiasV0 = splineAnimationStartBias.startVelocity()
        val startBiasV1 = splineAnimationStartBias.endVelocity()

        val endBiasV0 = splineAnimationEndBias.startVelocity()
        val endBiasV1 = splineAnimationEndBias.endVelocity()

        // On splines with periodic bias, the start and end velocity should be the same
        assertEquals(balancedV0.value, balancedV1.value, 0.0001f)
        assertEquals(startBiasV0.value, startBiasV1.value, 0.0001f)
        assertEquals(endBiasV0.value, endBiasV1.value, 0.001f)

        // Velocities on balanced bias should be the average of the start/end velocities of the
        // regular monotonic spline
        val avg = (regularV0.value + regularV1.value) / 2f
        assertEquals(avg, balancedV0.value)
        assertEquals(avg, balancedV1.value)

        // On fully biased at the start, the end velocity remains unchanged
        assertEquals(startBiasV1.value, regularV1.value, 0.00001f)

        // On fully biased at the end, the start velocity remains unchanged
        assertEquals(endBiasV0.value, regularV0.value, 0.00001f)
    }

    @Test
    fun testMultipleEasing() {
        val animation =
            keyframesWithSpline {
                    durationMillis = 300

                    Offset(0f, 0f) at 0 using EaseInCubic
                    Offset(1f, 1f) at 100 using EaseOutCubic
                    Offset(2f, 2f) at 200 using LinearEasing

                    // This easing is never applied since it's at the end
                    Offset(3f, 3f) at 300 using LinearOutSlowInEasing
                }
                .vectorize(Offset.VectorConverter)

        // Initial and target values don't matter since they're overwritten
        var valueVector = animation.valueAt(50)

        // Start with EaseInCubic, which is always a lower value than Linear
        assertTrue(valueVector[0] < 0.5f)
        assertTrue(valueVector[1] < 0.5f)

        // Then, EaseOutCubic, which is always a higher value than linear
        valueVector = animation.valueAt(50 + 100)
        assertTrue(valueVector[0] > (0.5f + 1f))
        assertTrue(valueVector[1] > (0.5f + 1f))

        // Then, LinearEasing which is the same as linear interpolation (in this particular setup)
        valueVector = animation.valueAt(50 + 200)
        assertEquals((0.5f + 2f), valueVector[0], 0.001f)
        assertEquals((0.5f + 2f), valueVector[1], 0.001f)
    }

    @Test
    fun possibleToOverrideStartAndEndValues() {
        val animation =
            keyframesWithSpline {
                    durationMillis = 500
                    Offset(0f, 0f) at 0 // Forcing start to 0f, 0f
                    Offset(1f, 1f) at 250
                    Offset(2f, 2f) at 500 // Forcing end to 2f, 2f
                }
                .vectorize(Offset.VectorConverter)

        val startValue =
            animation.valueAt(
                playTimeMillis = 0L,
                start = Offset(-1f, -1f), // Requested start is -1f, -1f
                end = Offset(-1f, -1f) // Requested end is -1f, -1f
            )
        val endValue =
            animation.valueAt(
                playTimeMillis = 500L,
                start = Offset(-1f, -1f),
                end = Offset(-1f, -1f)
            )
        assertEquals(AnimationVector2D(0f, 0f), startValue)
        assertEquals(AnimationVector2D(2f, 2f), endValue)
    }

    @Test
    fun initialVelocityIgnored() {
        val animation =
            keyframesWithSpline {
                    durationMillis = 500

                    Offset(1f, 1f) at 250
                }
                .vectorize(Offset.VectorConverter)

        // Expected velocity is constant since we are interpolating linearly from 0,0 to 2,2
        val expectedVelocity = AnimationVector2D(4f, 4f)

        val velZero = Offset.Zero
        val velHundred = Offset(100f, 100f)

        // Current implementation ignores initial velocity for interpolation
        assertEquals(expectedVelocity, animation.velocityAt(0, initialVelocity = velZero))
        assertEquals(expectedVelocity, animation.velocityAt(0, initialVelocity = velHundred))

        assertEquals(expectedVelocity, animation.velocityAt(10, initialVelocity = velZero))
        assertEquals(expectedVelocity, animation.velocityAt(10, initialVelocity = velHundred))
    }

    @Test
    fun testDelay() {
        val animation =
            keyframesWithSpline {
                    durationMillis = 500
                    delayMillis = 100

                    Offset(1f, 1f) at 250
                }
                .vectorize(Offset.VectorConverter)

        // Value should always be the initial value during the delay
        assertEquals(AnimationVector2D(0f, 0f), animation.valueAt(0L, start = Offset.Zero))
        assertEquals(AnimationVector2D(3f, 3f), animation.valueAt(50L, start = Offset(3f, 3f)))
        assertEquals(AnimationVector2D(5f, 5f), animation.valueAt(100L, start = Offset(5f, 5f)))

        // Value between keyframes (plus delay)
        assertEquals(AnimationVector2D(0.5f, 0.5f), animation.valueAt(100L + 125L))

        // Value at keyframe (plus delay)
        assertEquals(AnimationVector2D(1f, 1f), animation.valueAt(100L + 250L))

        // Final value (plus delay)
        assertEquals(AnimationVector2D(2f, 2f), animation.valueAt(100L + 500L))
    }

    @Test
    fun testNotEquals0() {
        var animationA = keyframesWithSpline {
            durationMillis = 100

            Offset(1f, 1f) at 30
            Offset(2f, 2f) at 50
        }

        var animationB = keyframesWithSpline {
            durationMillis = 100

            Offset(1f, 1f) at 30
            Offset(2f, 2f) at 50
        }
        // Test with the exact same declaration
        assertNotEquals(animationA, animationB)

        animationB = keyframesWithSpline {
            durationMillis = 100

            Offset(1f, 1f) atFraction 0.3f
            Offset(2f, 2f) atFraction 0.5f
        }
        // Test with equivalent `atFraction` declaration
        assertNotEquals(animationA, animationB)

        animationB = keyframesWithSpline {
            durationMillis = 100

            Offset(2f, 2f) atFraction 0.5f
            Offset(1f, 1f) atFraction 0.3f
        }
        // Test different keyframe declaration order
        assertNotEquals(animationA, animationB)

        val config =
            KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<Offset>().apply {
                durationMillis = 200

                Offset(1f, 1f) at 100
                Offset(2f, 2f) at 200
            }

        // Test re-declaring only config
        assertNotEquals(
            config,
            KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<Offset>().apply {
                durationMillis = 200

                Offset(1f, 1f) at 100
                Offset(2f, 2f) at 200
            }
        )

        animationA = KeyframesWithSplineSpec(config, Float.NaN)
        animationB = KeyframesWithSplineSpec(config, Float.NaN)

        // Test re-using config
        assertNotEquals(animationA, animationB)
    }

    @Test
    fun testNotEquals1() {
        var animationA = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, 1f) at 200
        }
        var animationB = keyframesWithSpline {
            durationMillis = 401

            Offset(1f, 1f) at 200
        }

        // Test different duration
        assertNotEquals(animationA, animationB)

        animationA = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, -1f) at 200
        }
        animationB = keyframesWithSpline {
            durationMillis = 400

            Offset(-1f, 1f) at 200
        }
        // Test different value at keyframe
        assertNotEquals(animationA, animationB)

        animationA = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, 1f) at 200
        }
        animationB = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, 1f) at 201
        }
        // Test different keyframe timestamp
        assertNotEquals(animationA, animationB)

        animationA = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, 1f) at 200
        }
        animationB = keyframesWithSpline {
            durationMillis = 400

            Offset(1f, 1f) at 200
            Offset(1f, 1f) at 201
        }
        // Test different keyframe count
        assertNotEquals(animationA, animationB)
    }

    /**
     * Helper method to get the [AnimationVector2D] value using [Offset] as input.
     *
     * By default, start and end values are `Offset(0f, 0f)` and `Offset(2f, 2f)` respectively.
     */
    private fun VectorizedDurationBasedAnimationSpec<AnimationVector2D>.valueAt(
        playTimeMillis: Long,
        start: Offset = Offset.Zero,
        end: Offset = Offset(2f, 2f),
        initialVelocity: Offset = Offset.Zero
    ): AnimationVector2D =
        getValueFromMillis(
            playTimeMillis = playTimeMillis,
            start = AnimationVector2D(start.x, start.y),
            end = AnimationVector2D(end.x, end.y),
            startVelocity = AnimationVector2D(initialVelocity.x, initialVelocity.y)
        )

    /**
     * Helper method to get the [AnimationVector2D] velocity using [Offset] as input.
     *
     * By default, start and end values are `Offset(0f, 0f)` and `Offset(2f, 2f)` respectively.
     */
    private fun VectorizedDurationBasedAnimationSpec<AnimationVector2D>.velocityAt(
        playTimeMillis: Long,
        start: Offset = Offset.Zero,
        end: Offset = Offset(2f, 2f),
        initialVelocity: Offset = Offset.Zero
    ): AnimationVector2D =
        getVelocityFromNanos(
            playTimeNanos = playTimeMillis * MillisToNanos,
            initialValue = AnimationVector2D(start.x, start.y),
            targetValue = AnimationVector2D(end.x, end.y),
            initialVelocity = AnimationVector2D(initialVelocity.x, initialVelocity.y)
        )
}
