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

package androidx.benchmark.benchmark

import android.os.Build
import androidx.benchmark.Arguments
import androidx.benchmark.CpuEventCounter
import androidx.benchmark.DeviceInfo
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CpuEventCounterBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val values = CpuEventCounter.Values()

    @Before
    fun before() {
        // skip test if need root, or event fails to enable
        CpuEventCounter.forceEnable()?.let { errorMessage -> assumeTrue(errorMessage, false) }

        assumeFalse(DeviceInfo.isEmulator && Build.VERSION.SDK_INT == 28) // see b/357101113

        assumeFalse(
            "cpu events enabled for all benchmarks, disabling this test",
            Arguments.cpuEventCounterEnable,
        )
    }

    @After
    fun after() {
        CpuEventCounter.reset()
    }

    /**
     * Measures overhead of starting and stopping
     *
     * We can expect to see some portion of this impact measurements directtly.
     */
    @Test
    fun startStopOnly() =
        CpuEventCounter().use { counter ->
            counter.resetEvents(
                listOf(
                    CpuEventCounter.Event.CpuCycles,
                    CpuEventCounter.Event.L1IMisses,
                    CpuEventCounter.Event.Instructions,
                )
            )
            benchmarkRule.measureRepeated {
                runWithMeasurementDisabled { counter.reset() }
                counter.start()
                counter.stop()
            }
        }

    /**
     * Measures full per measurement iteration cost
     *
     * This is important not because of direct intrusiveness to timing measurements, but because it
     * may correlate with other intrusiveness, e.g. cache interference from reset/reading values
     */
    @Test
    fun perIterationCost() =
        CpuEventCounter().use { counter ->
            counter.resetEvents(
                listOf(
                    CpuEventCounter.Event.CpuCycles,
                    CpuEventCounter.Event.L1IMisses,
                    CpuEventCounter.Event.Instructions,
                )
            )
            var out = 0L
            benchmarkRule.measureRepeated {
                counter.reset()
                counter.start()
                counter.stop()
                counter.read(values)
                out += values.getValue(CpuEventCounter.Event.CpuCycles)
                out += values.getValue(CpuEventCounter.Event.L1IMisses)
                out += values.getValue(CpuEventCounter.Event.Instructions)
            }
        }
}
