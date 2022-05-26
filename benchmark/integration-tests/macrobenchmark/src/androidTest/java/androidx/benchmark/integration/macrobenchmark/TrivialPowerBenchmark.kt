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
import androidx.benchmark.macro.BatteryCharge
import androidx.benchmark.macro.PowerCategory
import androidx.benchmark.macro.PowerCategoryDisplayLevel
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.PowerRail
import androidx.benchmark.macro.StartupMode
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
        assumeTrue(PowerRail.hasMetrics())
        assumeTrue(BatteryCharge.hasMinimumCharge())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                PowerMetric(
                    PowerMetric.Battery()
                ),
                PowerMetric(
                    PowerMetric.Power(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }
                    )
                ),
                PowerMetric(
                    PowerMetric.Energy(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }
                    )
                ),
            ),
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
        assumeTrue(PowerRail.hasMetrics())
        assumeTrue(BatteryCharge.hasMinimumCharge())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                PowerMetric(
                    PowerMetric.Battery()
                ),
                PowerMetric(
                    PowerMetric.Power(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }
                    )
                ),
                PowerMetric(
                    PowerMetric.Energy(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }
                    )
                ),
            ),
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
        assumeTrue(PowerRail.hasMetrics())
        assumeTrue(BatteryCharge.hasMinimumCharge())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                PowerMetric(
                    PowerMetric.Battery()
                ),
                PowerMetric(
                    PowerMetric.Power(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.TOTAL }
                    )
                ),
                PowerMetric(
                    PowerMetric.Energy(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.TOTAL }
                    )
                ),
            ),
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
        assumeTrue(PowerRail.hasMetrics())
        assumeTrue(BatteryCharge.hasMinimumCharge())
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                PowerMetric(
                    PowerMetric.Battery()
                ),
                PowerMetric(
                    PowerMetric.Power(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.TOTAL }
                    )
                ),
                PowerMetric(
                    PowerMetric.Energy(
                        PowerCategory.values()
                            .associateWith { PowerCategoryDisplayLevel.TOTAL }
                    )
                ),
            ),
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

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION = "$PACKAGE_NAME.BACKGROUND_WORK_ACTIVITY"
        private const val DURATION_MS = 5000
    }
}