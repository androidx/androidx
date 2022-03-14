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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.macro.EnergyMetric.Companion.MEASURE_BLOCK_SECTION_NAME
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
@SmallTest
class EnergyQueryTest {
    @Test
    fun successfulFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = EnergyQuery.getEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                EnergyQuery.EnergyMetrics("RailsAocLogic", 74972.0),
                EnergyQuery.EnergyMetrics("RailsAocMemory", 19601.0),
                EnergyQuery.EnergyMetrics("RailsCpuBig", 31935.0),
                EnergyQuery.EnergyMetrics("RailsCpuLittle", 303264.0),
                EnergyQuery.EnergyMetrics("RailsCpuMid", 55179.0),
                EnergyQuery.EnergyMetrics("RailsDdrA", 48458.0),
                EnergyQuery.EnergyMetrics("RailsDdrB", 54988.0),
                EnergyQuery.EnergyMetrics("RailsDdrC", 100082.0),
                EnergyQuery.EnergyMetrics("RailsDisplay", 1006934.0),
                EnergyQuery.EnergyMetrics("RailsGpu", 66555.0),
                EnergyQuery.EnergyMetrics("RailsMemoryInterface", 151912.0),
                EnergyQuery.EnergyMetrics("RailsModem", 8369.0),
                EnergyQuery.EnergyMetrics("RailsRadioFrontend", 0.0),
                EnergyQuery.EnergyMetrics("RailsSystemFabric", 122766.0),
                EnergyQuery.EnergyMetrics("RailsTpu", 50775.0),
                EnergyQuery.EnergyMetrics("RailsWifiBt", 493868.0),
            ), actualMetrics)
    }

    @Test
    fun successfulFixedTraceTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = EnergyQuery.getTotalEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                EnergyQuery.EnergyMetrics("Aoc", 94573.0),
                EnergyQuery.EnergyMetrics("CpuBig", 31935.0),
                EnergyQuery.EnergyMetrics("CpuLittle", 303264.0),
                EnergyQuery.EnergyMetrics("CpuMid", 55179.0),
                EnergyQuery.EnergyMetrics("Ddr", 203528.0),
                EnergyQuery.EnergyMetrics("Display", 1006934.0),
                EnergyQuery.EnergyMetrics("Gpu", 66555.0),
                EnergyQuery.EnergyMetrics("MemoryInterface", 151912.0),
                EnergyQuery.EnergyMetrics("Modem", 8369.0),
                EnergyQuery.EnergyMetrics("Radio", 0.0),
                EnergyQuery.EnergyMetrics("SystemFabric", 122766.0),
                EnergyQuery.EnergyMetrics("Tpu", 50775.0),
                EnergyQuery.EnergyMetrics("Wifi", 493868.0),
            ), actualMetrics)
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = EnergyQuery.getEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }

    @Test
    fun emptyFixedTraceTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = EnergyQuery.getTotalEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }
}
