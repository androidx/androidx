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

package androidx.tracing.benchmark.tracing

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.Trace
import androidx.tracing.benchmark.BASIC_STRING
import androidx.tracing.trace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidxTraceBenchmark {
    val config = MicrobenchmarkConfig(traceAppTagEnabled = true)
    @get:Rule val benchmarkRule = BenchmarkRule(config)

    @Test
    fun beginEnd_basic() {
        benchmarkRule.measureRepeated {
            Trace.beginSection(BASIC_STRING)
            Trace.endSection()
        }
    }

    @Test
    fun beginEnd_basic_trace() {
        benchmarkRule.measureRepeated { trace(BASIC_STRING) {} }
    }
}
