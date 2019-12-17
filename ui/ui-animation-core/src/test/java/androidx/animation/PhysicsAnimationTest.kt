/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PhysicsAnimationTest {

    @Test
    fun velocityCalculation() {
        val builder = PhysicsBuilder<Float>()
        val animation = builder.build()

        val start = 200f
        val end = 500f
        val playTime = 150L

        val velocity = animation.getVelocity(playTime, start, end, 0f)
        val (_, expectedVelocity) = builder.toSpring(end).updateValues(start, 0f, playTime)
        assertThat(velocity).isEqualTo(expectedVelocity)
    }

    @Test
    fun velocityCalculationForInts() {
        val builder = PhysicsBuilder<Int>()
        val animation = builder.build(IntToVectorConverter)

        val start = 200
        val end = 500
        val playTime = 150L

        val velocity = animation.getVelocity(playTime, start, end, 0f)

        val (_, expectedVelocity) = builder.toSpring(end)
            .updateValues(start.toFloat(), 0f, playTime)
        assertThat(velocity).isEqualTo(expectedVelocity)
    }

    @Test
    fun animationWithoutRangePreservesVelocity() {
        val builder = PhysicsBuilder<Float>()
        val animation = builder.build()

        // first animation that will be interrupted after 150 ms
        val start1 = 200f
        val end1 = 500f
        val interruptionTime = 150L
        val interruptionValue = animation.getValue(interruptionTime, start1, end1, 0f)

        val interruptionVelocity = animation.getVelocity(interruptionTime, start1, end1, 0f)

        // second animation will go from interruptionValue to interruptionValue with
        // applying the velocity from the first interrupted animation.
        val start2 = interruptionValue
        val end2 = interruptionValue
        val startVelocity2 = interruptionVelocity

        // let's verify values after 15 ms of the second animation
        val playTime = 15L
        val resultValue = animation.getValue(playTime, start2, end2, startVelocity2)
        val resultVelocity = animation.getVelocity(playTime, start2, end2, startVelocity2)

        val (expectedValue, expectedVelocity) = builder.toSpring(end2).updateValues(
            start2,
            interruptionVelocity,
            playTime
        )

        assertThat(resultValue).isEqualTo(expectedValue)
        assertThat(resultVelocity).isEqualTo(expectedVelocity)
    }

    private fun PhysicsBuilder<out Number>.toSpring(endValue: Number) =
        SpringSimulation(endValue.toFloat()).also {
            it.dampingRatio = dampingRatio
            it.stiffness = stiffness
        }
}