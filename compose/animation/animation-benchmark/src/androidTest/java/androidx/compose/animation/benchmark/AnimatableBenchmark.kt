/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.animation.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatableBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    /** Measures the cost of creating a new instance of [Animatable]. */
    @Test
    fun instantiation() {
        benchmarkRule.measureRepeated { Animatable(0f) }
    }

    /**
     * Measures the performance of running a simple animation from a start to an end value. This
     * benchmark uses `runBlocking` to execute the `suspend` function `animateTo`.
     */
    @Test
    fun animateTo() {
        // We need to pass in our own clock
        val immediateClock =
            object : MonotonicFrameClock {
                override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
                    return onFrame(System.nanoTime())
                }
            }

        val animatable = Animatable(0f)
        val animationSpec = tween<Float>(durationMillis = 100)

        benchmarkRule.measureRepeated {
            runBlocking {
                withContext(immediateClock) {
                    runWithMeasurementDisabled { animatable.snapTo(0f) }
                    animatable.animateTo(targetValue = 100f, animationSpec = animationSpec)
                }
            }
        }
    }
}
