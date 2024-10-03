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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimationSpecUtilsTest {
    @Test
    fun testFaster() {
        createAnimationSpecs().forEach { spec ->
            val fasterSpec = spec.faster(100f)
            assertEquals(spec.getDuration() / 2f, fasterSpec.getDuration().toFloat(), 10f)
        }
    }

    @Test
    fun testSlower() {
        createAnimationSpecs().forEach { spec ->
            val slowerSpec = spec.slower(50f)
            assertEquals(spec.getDuration() * 2f, slowerSpec.getDuration().toFloat(), 10f)
        }
    }

    @Test
    fun testDuration() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val expected = spec.getDuration() / speedupFactor
                val actual = wrappedSpec.getDuration().toFloat()

                // Tolerance is in nanos, so 10 is not that much
                assertEquals(expected, actual, 10f)
            }
        }
    }

    @Test
    fun testValue() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val duration = spec.getDuration()
                for (i in 0..100) {
                    val expected = spec.at(duration * i / 100)
                    val actual = wrappedSpec.at((duration / speedupFactor * i / 100).toLong())

                    assertEquals(expected, actual, 1e-6f)
                }
            }
        }
    }

    @Test
    fun testSpeed() {
        createAnimationSpecs().forEach { spec ->
            listOf(0.1f, 0.5f, 1f, 2f, 10f).forEach { speedupFactor ->
                val wrappedSpec = spec.speedFactor(speedupFactor)

                val duration = spec.getDuration()
                for (i in 0..100) {
                    val expected = spec.speedAt(duration * i / 100) * speedupFactor
                    val actual = wrappedSpec.speedAt((duration / speedupFactor * i / 100).toLong())

                    // The unit is pixels per second, so this is really small.
                    assertEquals(expected, actual, 0.006f)
                }
            }
        }
    }

    private fun Float.toVector() = Float.VectorConverter.convertToVector(this)

    private fun AnimationVector1D.toFloat() = Float.VectorConverter.convertFromVector(this)

    private fun AnimationSpec<Float>.getDuration() =
        vectorize(Float.VectorConverter)
            .getDurationNanos(0f.toVector(), 1f.toVector(), 0f.toVector()) / 1_000_000

    private fun AnimationSpec<Float>.at(time: Long) =
        vectorize(Float.VectorConverter)
            .getValueFromNanos(time, 0f.toVector(), 1f.toVector(), 0f.toVector())
            .toFloat()

    private fun AnimationSpec<Float>.speedAt(time: Long) =
        vectorize(Float.VectorConverter)
            .getVelocityFromNanos(time, 0f.toVector(), 1f.toVector(), 0f.toVector())
            .toFloat()

    private fun createAnimationSpecs() =
        buildList<AnimationSpec<Float>> {
            listOf(0.2f, 0.4f, 0.8f, 1f).forEach { damping ->
                listOf(50f, 200f, 400f, 1500f, 10_000f).forEach { stiffness ->
                    listOf(0.01f, 0.001f, 0.0001f).forEach { threshold ->
                        add(spring(damping, stiffness, threshold))
                    }
                }
            }
            listOf(0, 100, 300, 1000).forEach { duration ->
                listOf(0, 100, 500, 1500).forEach { delay ->
                    listOf(
                            FastOutSlowInEasing,
                            LinearOutSlowInEasing,
                            FastOutLinearInEasing,
                            LinearEasing
                        )
                        .forEach { easing -> add(tween(duration, delay, easing)) }
                }
            }
        }
}
