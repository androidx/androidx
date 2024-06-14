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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.spring
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DelayedSpringSpecTest {
    @Test
    fun delayedSpring_identicalWithOriginPlusDelay() {
        val delayedRatio = 0.5f

        val originalSpec =
            spring(dampingRatio = 0.7f, stiffness = 500f, visibilityThreshold = 0.1f)
                .vectorize(Float.VectorConverter)

        val delayedSpec =
            DelayedSpringSpec(
                    dampingRatio = 0.7f,
                    stiffness = 500f,
                    visibilityThreshold = 0.1f,
                    delayedRatio = delayedRatio,
                )
                .vectorize(Float.VectorConverter)

        val originalDurationNanos = originalSpec.getDurationNanos()
        val delayedNanos = (originalDurationNanos * delayedRatio).toLong()

        fun assertValuesAt(playTimeNanos: Long) {
            assertValuesAreEqual(
                originalSpec.getValueFromNanos(playTimeNanos),
                delayedSpec.getValueFromNanos(playTimeNanos + delayedNanos)
            )
        }

        assertValuesAt(0)
        assertValuesAt((originalDurationNanos * 0.2).toLong())
        assertValuesAt((originalDurationNanos * 0.35).toLong())
        assertValuesAt((originalDurationNanos * 0.6).toLong())
        assertValuesAt((originalDurationNanos * 0.85).toLong())
        assertValuesAt(originalDurationNanos)
    }

    private fun VectorizedAnimationSpec<AnimationVector1D>.getDurationNanos(): Long =
        getDurationNanos(InitialValue, TargetValue, InitialVelocity)

    private fun VectorizedAnimationSpec<AnimationVector1D>.getValueFromNanos(
        playTimeNanos: Long
    ): Float = getValueFromNanos(playTimeNanos, InitialValue, TargetValue, InitialVelocity).value

    private fun assertValuesAreEqual(value1: Float, value2: Float) {
        assertThat(value1).isWithin(Tolerance).of(value2)
    }

    companion object {
        private val InitialValue = AnimationVector1D(0f)
        private val TargetValue = AnimationVector1D(1f)
        private val InitialVelocity = AnimationVector1D(0f)
        private const val Tolerance = 0.001f
    }
}
