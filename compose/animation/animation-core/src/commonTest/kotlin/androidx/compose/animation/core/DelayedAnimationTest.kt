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

import androidx.kruth.assertThat
import kotlin.test.Test

class DelayedAnimationTest {
    @Test
    fun duration() {
        val tweenSpec = tween<Float>(1000, easing = LinearEasing)
        val delayed = delayed(tweenSpec, 500L * MillisToNanos)

        val vectorizedSpec = delayed.vectorize(Float.VectorConverter)

        val initialValue = AnimationVector1D(0f)
        val targetValue = AnimationVector1D(1000f)
        val initialVelocity = AnimationVector1D(30f)

        assertThat(
                vectorizedSpec.getDurationNanos(
                    initialValue = initialValue,
                    targetValue = targetValue,
                    initialVelocity = initialVelocity
                )
            )
            .isEqualTo(1500L * MillisToNanos)
    }

    @Test
    fun values() {
        val delayNanos = 500L * MillisToNanos
        val tweenSpec = tween<Float>(1000, easing = LinearEasing)
        val delayed = delayed(tweenSpec, delayNanos)

        val vectorizedSpec = delayed.vectorize(Float.VectorConverter)

        val initialValue = AnimationVector1D(0f)
        val targetValue = AnimationVector1D(1000f)
        val initialVelocity = AnimationVector1D(30f)

        assertThat(
                vectorizedSpec
                    .getValueFromNanos(
                        playTimeNanos = 0L,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(0f)

        assertThat(
                vectorizedSpec
                    .getValueFromNanos(
                        playTimeNanos = delayNanos,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(0f)

        assertThat(
                vectorizedSpec
                    .getValueFromNanos(
                        playTimeNanos = 1000L * MillisToNanos,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(500f)

        assertThat(
                vectorizedSpec
                    .getValueFromNanos(
                        playTimeNanos = 1500L * MillisToNanos,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(1000f)
    }

    @Test
    fun velocity() {
        val delayNanos = 500L * MillisToNanos
        val springSpec = spring<Float>()
        val delayed = delayed(springSpec, delayNanos)

        val vectorizedSpec = delayed.vectorize(Float.VectorConverter)

        val initialValue = AnimationVector1D(0f)
        val targetValue = AnimationVector1D(1000f)
        val initialVelocity = AnimationVector1D(30f)

        assertThat(
                vectorizedSpec
                    .getVelocityFromNanos(
                        playTimeNanos = 0L,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(30f)

        assertThat(
                vectorizedSpec
                    .getVelocityFromNanos(
                        playTimeNanos = delayNanos,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(30f)

        val vectorizedSpringSpec = springSpec.vectorize(Float.VectorConverter)

        val springDuration =
            vectorizedSpringSpec.getDurationNanos(
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocity = initialVelocity
            )

        assertThat(
                vectorizedSpec
                    .getVelocityFromNanos(
                        playTimeNanos = (delayNanos) + (springDuration / 5),
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(
                vectorizedSpringSpec
                    .getVelocityFromNanos(
                        playTimeNanos = springDuration / 5,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )

        assertThat(
                vectorizedSpec
                    .getVelocityFromNanos(
                        playTimeNanos = (delayNanos) + (springDuration / 2),
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(
                vectorizedSpringSpec
                    .getVelocityFromNanos(
                        playTimeNanos = springDuration / 2,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )

        assertThat(
                vectorizedSpec
                    .getVelocityFromNanos(
                        playTimeNanos = (delayNanos) + springDuration,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
            .isEqualTo(
                vectorizedSpringSpec
                    .getVelocityFromNanos(
                        playTimeNanos = springDuration,
                        initialValue = initialValue,
                        targetValue = targetValue,
                        initialVelocity = initialVelocity
                    )[0]
            )
    }

    @Test
    fun zeroAnimation() {
        val springSpec = spring<Float>()
        val delayed = delayed(springSpec, 0L)

        val vectorizedSpec = delayed.vectorize(Float.VectorConverter)

        val initialValue = AnimationVector1D(1f)
        val targetValue = AnimationVector1D(1f)
        val initialVelocity = AnimationVector1D(0f)

        assertThat(
                vectorizedSpec.getDurationNanos(
                    initialValue = initialValue,
                    targetValue = targetValue,
                    initialVelocity = initialVelocity
                )
            )
            .isEqualTo(0L)
    }

    @Test
    fun comparison() {
        val tweenSpec = tween<Float>(1000, easing = LinearEasing)
        val delayed1 = delayed(tweenSpec, 500L * MillisToNanos)
        val delayed2 = delayed(tweenSpec, 500L * MillisToNanos)
        val delayed3 = delayed(tweenSpec, 400L * MillisToNanos)
        val delayed4 = delayed(spring<Float>(), 400L * MillisToNanos)
        assertThat(delayed1).isEqualTo(delayed2)
        assertThat(delayed1).isNotEqualTo(delayed3)
        assertThat(delayed1).isNotEqualTo(delayed4)

        assertThat(delayed1.hashCode()).isEqualTo(delayed2.hashCode())
        assertThat(delayed1.hashCode()).isNotEqualTo(delayed3.hashCode())
        assertThat(delayed1.hashCode()).isNotEqualTo(delayed4.hashCode())
    }
}
