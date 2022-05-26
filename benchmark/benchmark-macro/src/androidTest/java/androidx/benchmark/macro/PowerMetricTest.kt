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
            PowerMetric.Energy(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "energyComponentCpuBigUws" to 31935.0,
                    "energyComponentCpuLittleUws" to 303264.0,
                    "energyComponentCpuMidUws" to 55179.0,
                    "energyComponentDisplayUws" to 1006934.0,
                    "energyComponentGpuUws" to 66555.0,
                    "energyComponentDdrAUws" to 48458.0,
                    "energyComponentDdrBUws" to 54988.0,
                    "energyComponentDdrCUws" to 100082.0,
                    "energyComponentMemoryInterfaceUws" to 151912.0,
                    "energyComponentTpuUws" to 50775.0,
                    "energyComponentAocLogicUws" to 74972.0,
                    "energyComponentAocMemoryUws" to 19601.0,
                    "energyComponentModemUws" to 8369.0,
                    "energyComponentRadioFrontendUws" to 0.0,
                    "energyComponentWifiBtUws" to 493868.0,
                    "energyComponentSystemFabricUws" to 122766.0,
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
            PowerMetric.Power(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "powerCategoryCpuUw" to 80.940907,
                    "powerCategoryDisplayUw" to 208.777524,
                    "powerCategoryGpuUw" to 13.799502,
                    "powerCategoryMemoryUw" to 73.69686899999999,
                    "powerCategoryMachineLearningUw" to 10.52768,
                    "powerCategoryNetworkUw" to 123.74248399999999,
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
            PowerMetric.Power(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "powerCategoryCpuUw" to 80.940907,
                    "powerCategoryDisplayUw" to 208.777524,
                    "powerCategoryMemoryUw" to 73.69686899999999,
                    "powerCategoryNetworkUw" to 123.74248399999999,
                    "powerComponentSystemFabricUw" to 25.454282,
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
            PowerMetric.Energy(categories)
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = emptyMap(),
                sampledMetrics = emptyMap()
            ), actualMetrics
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTraceBatteryDischarge() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_battery_discharge", ".perfetto-trace")

        val actualMetrics = PowerMetric(
            PowerMetric.Battery()
        ).getMetrics(captureInfo, traceFile.absolutePath)

        assertEquals(
            IterationResult(
                singleMetrics = mapOf(
                    "batteryStartMah" to 1020.0,
                    "batteryEndMah" to 1007.0,
                    "batteryDiffMah" to 13.0
                ),
                sampledMetrics = emptyMap()
            ), actualMetrics
        )
    }
}