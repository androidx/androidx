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

package androidx.benchmark.macro

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class PowerMetricTest {
    private val captureInfo = Metric.CaptureInfo(
        31,
        "androidx.benchmark.integration.macrobenchmark.target",
        "androidx.benchmark.macro",
        StartupMode.COLD
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTraceEnergyBreakdown() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val categories = PowerCategory.values()
            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }

        val actualMetrics = PowerMetric(
            PowerMetric.Type.Energy(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "energyRailsCpuBigUws" to 31935.0,
                    "energyRailsCpuLittleUws" to 303264.0,
                    "energyRailsCpuMidUws" to 55179.0,
                    "energyRailsDisplayUws" to 1006934.0,
                    "energyRailsGpuUws" to 66555.0,
                    "energyRailsDdrAUws" to 48458.0,
                    "energyRailsDdrBUws" to 54988.0,
                    "energyRailsDdrCUws" to 100082.0,
                    "energyRailsMemoryInterfaceUws" to 151912.0,
                    "energyRailsTpuUws" to 50775.0,
                    "energyRailsAocLogicUws" to 74972.0,
                    "energyRailsAocMemoryUws" to 19601.0,
                    "energyRailsModemUws" to 8369.0,
                    "energyRailsRadioFrontendUws" to 0.0,
                    "energyRailsWifiBtUws" to 493868.0,
                    "energyRailsSystemFabricUws" to 122766.0,
                    "energyTotalUws" to 2589658.0
                ),
                sampledMetrics = emptyMap()
            ), actualMetrics)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTracePowerTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val categories = PowerCategory.values()
            .associateWith { PowerCategoryDisplayLevel.TOTAL }

        val actualMetrics = PowerMetric(
            PowerMetric.Type.Power(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "powerCpuUw" to 80.940907,
                    "powerDisplayUw" to 208.777524,
                    "powerGpuUw" to 13.799502,
                    "powerMemoryUw" to 73.69686899999999,
                    "powerMachineLearningUw" to 10.52768,
                    "powerNetworkUw" to 123.74248399999999,
                    "powerUncategorizedUw" to 25.454282,
                    "powerTotalUw" to 536.939248
                ),
                sampledMetrics = emptyMap()
            ), actualMetrics
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTracePowerMix() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val categories = mapOf(
            PowerCategory.DISPLAY to PowerCategoryDisplayLevel.TOTAL,
            PowerCategory.MEMORY to PowerCategoryDisplayLevel.TOTAL,
            PowerCategory.CPU to PowerCategoryDisplayLevel.TOTAL,
            PowerCategory.NETWORK to PowerCategoryDisplayLevel.TOTAL,
            PowerCategory.UNCATEGORIZED to PowerCategoryDisplayLevel.BREAKDOWN
        )

        val actualMetrics = PowerMetric(
            PowerMetric.Type.Power(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "powerCpuUw" to 80.940907,
                    "powerDisplayUw" to 208.777524,
                    "powerMemoryUw" to 73.69686899999999,
                    "powerNetworkUw" to 123.74248399999999,
                    "powerRailsSystemFabricUw" to 25.454282,
                    "powerUnselectedUw" to 24.327182,
                    "powerTotalUw" to 536.939248
                ),
                sampledMetrics = emptyMap()
            ), actualMetrics
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val categories = PowerCategory.values()
            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }

        val actualMetrics = PowerMetric(
            PowerMetric.Type.Energy(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = emptyMap(),
                sampledMetrics = emptyMap()
            ), actualMetrics
        )
    }
}