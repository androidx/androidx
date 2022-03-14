/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.Shell
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.EnergyMetric
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TotalEnergyMetric
import androidx.benchmark.macro.TotalPowerMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.concurrent.thread
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalMetricApi::class)
class TrivialPowerBenchmark() {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureEnergyPower() {
        assumeTrue(hasRailMetrics())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(PowerMetric(), EnergyMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            Thread.sleep(DURATION_MS.toLong())
        }
    }

    @Test
    fun measureEnergyPowerMultiple() {
        assumeTrue(hasRailMetrics())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(PowerMetric(), EnergyMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            var done = false
            val threads = emptyList<Thread>()
            try {
                repeat(8) {
                    threads.toMutableList().add(
                        thread(start = true) {
                        while (!done) { }
                    })
                }
                Thread.sleep(DURATION_MS.toLong())
            } finally {
                done = true
                threads.forEach {
                    it.join()
                }
            }
        }
    }

    @Test
    fun measureTotalEnergyPower() {
        assumeTrue(hasRailMetrics())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(TotalEnergyMetric(), TotalPowerMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            Thread.sleep(DURATION_MS.toLong())
        }
    }

    @Test
    fun measureTotalEnergyPowerMultiple() {
        assumeTrue(hasRailMetrics())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(TotalPowerMetric(), TotalEnergyMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 3,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            var done = false
            val threads = emptyList<Thread>()
            try {
                repeat(8) {
                    threads.toMutableList().add(
                        thread(start = true) {
                            while (!done) { }
                        })
                }
                Thread.sleep(DURATION_MS.toLong())
            } finally {
                done = true
                threads.forEach {
                    it.join()
                }
            }
        }
    }

    private fun hasRailMetrics(): Boolean {
        var commandHal2 = "dumpsys android.hardware.power.stats.IPowerStats/default delta"
        var commandHal1 = "lshal debug android.hardware.power.stats@1.0::IPowerStats/default delta"
        var resultHal2 = Shell.executeCommand(commandHal2)
        var resultHal1 = Shell.executeCommand(commandHal1)
        if ((resultHal2.isEmpty()) && (resultHal1.isEmpty())) {
            throw UnsupportedOperationException("""
                Rail metrics are not available on this device. To check a device for
                power/energy measurement support, it must output something for one of
                the following commands:

                adb shell $commandHal2
                adb shell $commandHal1

                """.trimIndent())
        }
        return true
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION = "$PACKAGE_NAME.TRIVIAL_STARTUP_ACTIVITY"
        private const val DURATION_MS = 5000
    }
}