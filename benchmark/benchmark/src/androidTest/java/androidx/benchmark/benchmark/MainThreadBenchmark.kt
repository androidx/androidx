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

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainThreadBenchmark {
    @OptIn(ExperimentalBenchmarkConfigApi::class)
    @get:Rule
    val benchmarkRule = BenchmarkRule(
        MicrobenchmarkConfig(
            traceAppTagEnabled = true
        )
    )

    @Suppress("SameParameterValue")
    private fun spinloop(durMs: Long) {
        val waitUntilNs = System.nanoTime() + durMs * 1_000_000
        @Suppress("ControlFlowWithEmptyBody")
        while (System.nanoTime() < waitUntilNs) {
        }
    }

    @Test
    fun measureRepeatedOnMainThread() {
        benchmarkRule.measureRepeatedOnMainThread {
            spinloop(100)
        }
    }

    @Test
    @Ignore // local testing only, can cause ANRs
    @UiThreadTest
    fun measureRepeatedAnnotation() {
        benchmarkRule.measureRepeated {
            spinloop(100)
        }
    }
}
