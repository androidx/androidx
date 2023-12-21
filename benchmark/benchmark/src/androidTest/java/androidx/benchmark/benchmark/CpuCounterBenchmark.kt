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
import androidx.benchmark.CpuCounter
import androidx.benchmark.DeviceInfo
import androidx.benchmark.PropOverride
import androidx.benchmark.Shell
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CpuCounterBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    private val values = CpuCounter.Values()

    private val perfHardenProp = PropOverride("security.perf_harden", "0")
    private var shouldResetEnforce1 = false

    @Before
    fun before() {
        // TODO: make this automatic
        if (Build.VERSION.SDK_INT > 29) {
            val blockedBySelinux = Shell.isSELinuxEnforced()
            Assume.assumeTrue(
                "blocked by selinux = $blockedBySelinux, rooted = ${DeviceInfo.isRooted}",
                !blockedBySelinux || DeviceInfo.isRooted
            )
            if (blockedBySelinux && DeviceInfo.isRooted) {
                Shell.executeScriptSilent("setenforce 0")
                shouldResetEnforce1 = true
            }
            perfHardenProp.forceValue()
        }
        val error = CpuCounter.checkPerfEventSupport()
        Assume.assumeTrue(error, error == null)
    }

    @After
    fun after() {
        perfHardenProp.resetIfOverridden()
        if (shouldResetEnforce1) {
            Shell.executeScriptSilent("setenforce 1")
        }
    }

    /**
     * Measures overhead of starting and stopping
     *
     * We can expect to see some portion of this impact measurements directtly.
     */
    @Test
    fun startStopOnly() = CpuCounter().use { counter ->
        counter.resetEvents(
            listOf(
                CpuCounter.Event.CpuCycles,
                CpuCounter.Event.L1IMisses,
                CpuCounter.Event.Instructions,
            )
        )
        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                counter.reset()
            }
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
    fun perIterationCost() = CpuCounter().use { counter ->
        counter.resetEvents(
            listOf(
                CpuCounter.Event.CpuCycles,
                CpuCounter.Event.L1IMisses,
                CpuCounter.Event.Instructions,
            )
        )
        var out = 0L
        benchmarkRule.measureRepeated {
            counter.reset()
            counter.start()
            counter.stop()
            counter.read(values)
            out += values.getValue(CpuCounter.Event.CpuCycles)
            out += values.getValue(CpuCounter.Event.L1IMisses)
            out += values.getValue(CpuCounter.Event.Instructions)
        }
    }
}
