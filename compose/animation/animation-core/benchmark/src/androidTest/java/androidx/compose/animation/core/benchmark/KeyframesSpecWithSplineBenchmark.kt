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

package androidx.compose.animation.core.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.math.roundToLong
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationSpecApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class KeyframesSpecWithSplineBenchmark {
    private val initialVector = AnimationVector(0f, 0f)
    private val targetVector = AnimationVector(1000f, 1000f)

    private val durationMillisToTest = 10_000
    private val playTimeNanosToEvaluate = (durationMillisToTest * 0.77f).roundToLong() * 1_000_000

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmark_firstFrame() {
        benchmarkRule.measureRepeated {
            val vectorized =
                keyframesWithSpline {
                        durationMillis = durationMillisToTest

                        Offset(12f, 30f) at 10
                        Offset(-30f, 500f) at 500
                        Offset(8f, 10f) at 600
                        Offset(234f, 543f) at 700
                    }
                    .vectorize(Offset.VectorConverter)

            // Get first value to guarantee the spline has been calculated
            vectorized.getValueFromNanos(
                playTimeNanos = playTimeNanosToEvaluate,
                initialValue = initialVector,
                targetValue = targetVector,
                initialVelocity = initialVector
            )
        }
    }

    @Test
    fun benchmark_vectorized() {
        benchmarkRule.measureRepeated {
            keyframesWithSpline {
                    durationMillis = durationMillisToTest

                    Offset(12f, 30f) at 10
                    Offset(-30f, 500f) at 500
                    Offset(8f, 10f) at 600
                    Offset(234f, 543f) at 700
                }
                .vectorize(Offset.VectorConverter)
        }
    }

    @Test
    fun benchmark_frameToFrame() {
        val vectorized =
            keyframesWithSpline {
                    durationMillis = durationMillisToTest

                    Offset(12f, 30f) at 10
                    Offset(-30f, 500f) at 500
                    Offset(8f, 10f) at 600
                    Offset(234f, 543f) at 700
                }
                .vectorize(Offset.VectorConverter)

        // Cal the first frame before measuring, to guarantee the spline interpolation is built
        vectorized.getValueFromNanos(
            // Avoid using a number that may match one of the given timestamps, it will prevent the
            // MonoSpline from initializing, a number with periodic decimals should work well
            playTimeNanos = playTimeNanosToEvaluate * 2 / 3L,
            initialValue = initialVector,
            targetValue = targetVector,
            initialVelocity = initialVector
        )

        val frame0 = playTimeNanosToEvaluate
        val frame1 = frame0 + (1000.0f / 60 * 1_000_000).roundToLong()
        benchmarkRule.measureRepeated {
            for (i in 0..10) {
                vectorized.getValueFromNanos(
                    playTimeNanos = frame0,
                    initialValue = initialVector,
                    targetValue = targetVector,
                    initialVelocity = initialVector
                )
                vectorized.getValueFromNanos(
                    playTimeNanos = frame1,
                    initialValue = initialVector,
                    targetValue = targetVector,
                    initialVelocity = initialVector
                )
            }
        }
    }

    @Test
    fun benchmark_invalidated_frameToFrame() {
        val vectorized =
            keyframesWithSpline {
                    durationMillis = durationMillisToTest

                    Offset(12f, 30f) at 10
                    Offset(-30f, 500f) at 500
                    Offset(8f, 10f) at 600
                    Offset(234f, 543f) at 700
                }
                .vectorize(Offset.VectorConverter)

        // Cal the first frame before measuring, to guarantee the spline interpolation is built
        vectorized.getValueFromNanos(
            // Avoid using a number that may match one of the given timestamps, it will prevent the
            // MonoSpline from initializing, a number with periodic decimals should work well
            playTimeNanos = playTimeNanosToEvaluate * 2 / 3L,
            initialValue = initialVector,
            targetValue = targetVector,
            initialVelocity = initialVector
        )

        val frame0 = (durationMillisToTest * 0.1f).roundToLong() * 1_000_000
        benchmarkRule.measureRepeated {
            vectorized.getValueFromNanos(
                playTimeNanos = frame0,
                initialValue = initialVector,
                targetValue = targetVector,
                initialVelocity = initialVector
            )

            // Flip initial / target to invalidate the vectorized MonoSpline
            vectorized.getValueFromNanos(
                playTimeNanos = frame0,
                initialValue = targetVector,
                targetValue = initialVector,
                initialVelocity = initialVector
            )
        }
    }
}
