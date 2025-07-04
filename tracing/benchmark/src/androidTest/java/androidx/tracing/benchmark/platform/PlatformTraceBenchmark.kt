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

package androidx.tracing.benchmark.platform

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.benchmark.BASIC_STRING
import androidx.tracing.benchmark.LARGE_STRING_POOL
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlatformTraceBenchmark {
    val config = MicrobenchmarkConfig(traceAppTagEnabled = true)
    @get:Rule val benchmarkRule = BenchmarkRule(config)

    @Test
    fun beginEnd_basic() {
        benchmarkRule.measureRepeated {
            android.os.Trace.beginSection(BASIC_STRING)
            android.os.Trace.endSection()
        }
    }

    /** Measuring overhead of [android.os.Trace] as a reference point. */
    @Test
    fun beginEnd_largeStringPool() {
        var ix = 0
        benchmarkRule.measureRepeated {
            android.os.Trace.beginSection(LARGE_STRING_POOL[ix++ % LARGE_STRING_POOL.size])
            android.os.Trace.endSection()
        }
    }
}
