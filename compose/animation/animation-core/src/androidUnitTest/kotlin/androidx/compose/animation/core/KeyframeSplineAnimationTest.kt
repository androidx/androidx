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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Offset
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalAnimationSpecApi::class)
@RunWith(JUnit4::class)
class KeyframeSplineAnimationTest {

    /**
     * See [MonoSplineTest] to test the interpolation curves.
     */
    @Test
    fun interpolatedValues() {
        val animation = keyframesWithSpline {
            durationMillis = 500

            // At half the time, reach the final Y value (expected final value is Offset(2f, 2f))
            Offset(1f, 2f) at 250
        }.vectorize(Offset.VectorConverter)

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

    @Test
    fun possibleToOverrideStartAndEndValues() {
        val animation = keyframesWithSpline {
            durationMillis = 500
            Offset(0f, 0f) at 0 // Forcing start to 0f, 0f
            Offset(1f, 1f) at 250
            Offset(2f, 2f) at 500 // Forcing end to 2f, 2f
        }.vectorize(Offset.VectorConverter)

        val startValue = animation.valueAt(
            playTimeMillis = 0L,
            start = Offset(-1f, -1f), // Requested start is -1f, -1f
            end = Offset(-1f, -1f) // Requested end is -1f, -1f
        )
        val endValue = animation.valueAt(
            playTimeMillis = 500L,
            start = Offset(-1f, -1f),
            end = Offset(-1f, -1f)
        )
        assertEquals(AnimationVector2D(0f, 0f), startValue)
        assertEquals(AnimationVector2D(2f, 2f), endValue)
    }

    @Test
    fun initialVelocityIgnored() {
        val animation = keyframesWithSpline {
            durationMillis = 500

            Offset(1f, 1f) at 250
        }.vectorize(Offset.VectorConverter)

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
        val animation = keyframesWithSpline {
            durationMillis = 500
            delayMillis = 100

            Offset(1f, 1f) at 250
        }.vectorize(Offset.VectorConverter)

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

        val config = KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<Offset>().apply {
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

        animationA = KeyframesWithSplineSpec(config)
        animationB = KeyframesWithSplineSpec(config)

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
