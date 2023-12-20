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

package androidx.benchmark.integration.dryrun.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DryRunBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkOne() {
        var iterationCount = 0
        benchmarkRule.measureRepeated {
            iterationCount++
        }
        // dry run mode always runs 1 loops
        assertEquals(1, iterationCount)
    }

    // We have two benchmarks here to verify startupMode is overridden
    @Test
    fun benchmarkTwo() {
        var iterationCount = 0
        benchmarkRule.measureRepeated {
            iterationCount++
        }
        // dry run mode always runs 1 loops
        assertEquals(1, iterationCount)
    }
}
