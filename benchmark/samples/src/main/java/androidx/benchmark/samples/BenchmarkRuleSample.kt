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

@file:Suppress("JUnitMalformedDeclaration", "unused")

package androidx.benchmark.samples

import android.graphics.Bitmap
import androidx.annotation.Sampled
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun doSomeWork() {}

private fun doSomeWorkOnMainThread() {}

@Sampled
fun benchmarkRuleSample() {
    @RunWith(AndroidJUnit4::class)
    class MyBenchmark {
        @get:Rule val benchmarkRule = BenchmarkRule()

        @Test
        fun measureWork() {
            benchmarkRule.measureRepeated { doSomeWork() }
        }
    }
}

@Sampled
fun measureRepeatedOnMainThreadSample() {
    @RunWith(AndroidJUnit4::class)
    class MainThreadBenchmark {
        @get:Rule val benchmarkRule = BenchmarkRule()

        @Test
        fun measureWork() {
            benchmarkRule.measureRepeatedOnMainThread {
                // this block is run on the main thread
                doSomeWorkOnMainThread()
            }
        }
    }
}

private val benchmarkRule = BenchmarkRule()

private fun constructTestBitmap(): Bitmap {
    TODO()
}

private fun processBitmap(@Suppress("UNUSED_PARAMETER") bitmap: Bitmap) {
    TODO()
}

@Sampled
fun runWithMeasurementDisabledSample() {
    @Test
    fun bitmapProcessing() =
        benchmarkRule.measureRepeated {
            val input: Bitmap = runWithMeasurementDisabled { constructTestBitmap() }
            processBitmap(input)
        }
}
