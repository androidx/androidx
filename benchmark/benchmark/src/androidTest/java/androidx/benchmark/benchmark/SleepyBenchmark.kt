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

package androidx.benchmark.benchmark

import android.opengl.Matrix
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * This benchmark is designed to show the difference between environments with minimum clocks
 * set, and those without.
 *
 * The lowest levels of the clock stability waterfall (stable performance mode, and throttle
 * detection) are expected to show bigger variations between the two methods, and less stability in
 * [matrixMathSleepy].
 */
@LargeTest
@RunWith(JUnit4::class)
class SleepyBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val sourceMatrix = FloatArray(16) { it.toFloat() }
    val resultMatrix = FloatArray(16)

    @Suppress("NOTHING_TO_INLINE")
    inline fun theyDidTheMatrixMath() {
        repeat(10) {
            Matrix.translateM(resultMatrix, 0, sourceMatrix, 0, 1F, 2F, 3F)
        }
    }

    @Test
    fun matrixMathSleepy() {
        benchmarkRule.measureRepeated {
            theyDidTheMatrixMath()

            runWithTimingDisabled {
                Thread.sleep(10)
                theyDidTheMatrixMath() // reheat the cache
            }
        }
    }

    @Test
    fun matrixMath() {
        benchmarkRule.measureRepeated {
            theyDidTheMatrixMath()
        }
    }
}
