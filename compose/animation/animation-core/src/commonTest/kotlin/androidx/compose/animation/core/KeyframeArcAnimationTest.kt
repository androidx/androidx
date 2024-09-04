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

import androidx.compose.animation.core.ArcMode.Companion.ArcAbove
import androidx.compose.animation.core.ArcMode.Companion.ArcBelow
import androidx.compose.animation.core.ArcMode.Companion.ArcLinear
import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("JoinDeclarationAndAssignment") // Looks kinda messy
@OptIn(ExperimentalAnimationSpecApi::class)
class KeyframeArcAnimationTest {
    private val timeMillis = 3000
    private val initialValue = 0f
    private val targetValue = 600f
    private val error = 0.0001f

    @Test
    fun test2DArcKeyFrame_interpolatedValues() {
        var arcVector: AnimationVector2D
        var linearVector: AnimationVector2D

        // Test above, below, linear keyframes
        val keyframeAnimation =
            keyframes {
                    durationMillis = timeMillis

                    Offset(initialValue, initialValue) at 0 using LinearEasing using ArcAbove
                    Offset(200f, 200f) at 1000 using LinearEasing using ArcBelow
                    Offset(400f, 400f) atFraction 2f / 3f using LinearEasing using ArcLinear
                }
                .vectorize(Offset.VectorConverter)

        arcVector =
            keyframeAnimation.getValueFromNanos(
                (500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(1f / 6f)
        assertTrue(arcVector[0] > linearVector[0]) // X is higher for ArcAbove (in this scenario)
        assertTrue(arcVector[1] < linearVector[1]) // Y is lower for ArcAbove (in this scenario)

        arcVector =
            keyframeAnimation.getValueFromNanos(
                (1500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(3f / 6f)
        assertTrue(arcVector[0] < linearVector[0]) // X is lower for ArcBelow
        assertTrue(arcVector[1] > linearVector[1]) // Y is higher for ArcBelow

        arcVector =
            keyframeAnimation.getValueFromNanos(
                (2500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(5f / 6f)
        assertEquals(linearVector[0], arcVector[0], error) // X is equals for ArcLinear
        assertEquals(linearVector[1], arcVector[1], error) // Y is equals for ArcLinear
    }

    @Test
    fun test2DArcKeyFrame_multipleEasing() {
        var arcVector: AnimationVector2D
        var linearVector: AnimationVector2D

        // We test different Easing curves using Linear arc mode
        val keyframeAnimation =
            keyframes {
                    durationMillis = timeMillis

                    Offset.Zero at 0 using EaseInCubic using ArcLinear
                    Offset(200f, 200f) at 1000 using LinearEasing using ArcLinear
                    Offset(400f, 400f) atFraction 2f / 3f using EaseOutCubic using ArcLinear
                }
                .vectorize(Offset.VectorConverter)

        // Start with EaseInCubic, which is always a lower value
        arcVector =
            keyframeAnimation.getValueFromNanos(
                (500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(1f / 6f)
        // X & Y are lower for EaseInCubic
        assertTrue(arcVector[0] < linearVector[0])
        assertTrue(arcVector[1] < linearVector[1])

        // Then, LinearEasing, which is always equals
        arcVector =
            keyframeAnimation.getValueFromNanos(
                (1500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(3f / 6f)
        assertEquals(linearVector[0], arcVector[0], error) // X is equals with LinearEasing
        assertEquals(linearVector[1], arcVector[1], error) // Y is equals with LinearEasing

        // Then, EaseOutCubic, which is always a higher value
        arcVector =
            keyframeAnimation.getValueFromNanos(
                (2500).toLong() * 1_000_000,
                createFilledVector(initialValue),
                createFilledVector(targetValue),
                createFilledVector(0f)
            )
        linearVector = linearValueAt(5f / 6f)
        // X & Y are higher for EaseOutCubic
        assertTrue(arcVector[0] > linearVector[0])
        assertTrue(arcVector[1] > linearVector[1])
    }

    private inline fun <reified V : AnimationVector> linearValueAt(timePercent: Float): V {
        val value = timePercent * targetValue
        return createFilledVector<V>(value)
    }
}
