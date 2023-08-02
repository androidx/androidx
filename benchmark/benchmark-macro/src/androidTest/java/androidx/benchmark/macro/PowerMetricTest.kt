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
import androidx.benchmark.macro.Metric.Measurement
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

@OptIn(ExperimentalMetricApi::class)
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

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerMetric(PowerMetric.Energy(categories)).getResult(captureInfo, this)
        }

        assertEqualMeasurements(
            expected = listOf(
                Measurement("energyComponentCpuBigUws", 31935.0),
                Measurement("energyComponentCpuLittleUws", 303264.0),
                Measurement("energyComponentCpuMidUws", 55179.0),
                Measurement("energyComponentDisplayUws", 1006934.0),
                Measurement("energyComponentGpuUws", 66555.0),
                Measurement("energyComponentDdrAUws", 48458.0),
                Measurement("energyComponentDdrBUws", 54988.0),
                Measurement("energyComponentDdrCUws", 100082.0),
                Measurement("energyComponentMemoryInterfaceUws", 151912.0),
                Measurement("energyComponentTpuUws", 50775.0),
                Measurement("energyComponentAocLogicUws", 74972.0),
                Measurement("energyComponentAocMemoryUws", 19601.0),
                Measurement("energyComponentModemUws", 8369.0),
                Measurement("energyComponentRadioFrontendUws", 0.0),
                Measurement("energyComponentWifiBtUws", 493868.0),
                Measurement("energyComponentSystemFabricUws", 122766.0),
                Measurement("energyTotalUws", 2589658.0)
            ),
            observed = actualMetrics,
            threshold = 0.1
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTracePowerTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val categories = PowerCategory.values()
            .associateWith { PowerCategoryDisplayLevel.TOTAL }

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerMetric(PowerMetric.Power(categories)).getResult(captureInfo, this)
        }

        assertEqualMeasurements(
            expected = listOf(
                Measurement("powerCategoryCpuUw", 80.94090814845532),
                Measurement("powerCategoryDisplayUw", 208.77752436243003),
                Measurement("powerCategoryGpuUw", 13.799502384408045),
                Measurement("powerCategoryMemoryUw", 73.69686916856728),
                Measurement("powerCategoryMachineLearningUw", 10.527679867302508),
                Measurement("powerCategoryNetworkUw", 123.74248393116318),
                Measurement("powerUncategorizedUw", 25.454281567489115),
                Measurement("powerTotalUw", 536.9392494298155),
            ),
            observed = actualMetrics,
            threshold = 0.00001
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

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerMetric(PowerMetric.Power(categories)).getResult(captureInfo, this)
        }

        assertEqualMeasurements(
            expected = listOf(
                Measurement("powerCategoryCpuUw", 80.94090814845532),
                Measurement("powerCategoryDisplayUw", 208.77752436243003),
                Measurement("powerCategoryMemoryUw", 73.69686916856728),
                Measurement("powerCategoryNetworkUw", 123.74248393116318),
                Measurement("powerComponentSystemFabricUw", 25.454281567489115),
                Measurement("powerUnselectedUw", 24.327182251710553),
                Measurement("powerTotalUw", 536.9392494298155)
            ),
            observed = actualMetrics,
            threshold = 0.00001
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val categories = PowerCategory.values()
            .associateWith { PowerCategoryDisplayLevel.BREAKDOWN }

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerMetric(PowerMetric.Energy(categories)).getResult(captureInfo, this)
        }

        assertEquals(emptyList(), actualMetrics)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Test
    fun successfulFixedTraceBatteryDischarge() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_battery_discharge", ".perfetto-trace")

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerMetric(PowerMetric.Battery()).getResult(captureInfo, this)
        }

        assertEqualMeasurements(
            expected = listOf(
                Measurement("batteryStartMah", 1020.0),
                Measurement("batteryEndMah", 1007.0),
                Measurement("batteryDiffMah", 13.0)
            ),
            observed = actualMetrics,
            threshold = 0.1
        )
    }
}
