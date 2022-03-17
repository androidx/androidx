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

import androidx.benchmark.macro.PowerMetric.Companion.MEASURE_BLOCK_SECTION_NAME
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
class PowerQueryTest {
    @Test
    fun successfulFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = PowerQuery.getPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                PowerQuery.PowerMetrics("RailsAocLogic", 15.544682),
                PowerQuery.PowerMetrics("RailsAocMemory", 4.064068),
                PowerQuery.PowerMetrics("RailsCpuBig", 6.621397),
                PowerQuery.PowerMetrics("RailsCpuLittle", 62.878706),
                PowerQuery.PowerMetrics("RailsCpuMid", 11.440804),
                PowerQuery.PowerMetrics("RailsDdrA", 10.047273),
                PowerQuery.PowerMetrics("RailsDdrB", 11.401203),
                PowerQuery.PowerMetrics("RailsDdrC", 20.750985),
                PowerQuery.PowerMetrics("RailsDisplay", 208.777524),
                PowerQuery.PowerMetrics("RailsGpu", 13.799502),
                PowerQuery.PowerMetrics("RailsMemoryInterface", 31.497408),
                PowerQuery.PowerMetrics("RailsModem", 1.735227),
                PowerQuery.PowerMetrics("RailsRadioFrontend", 0.0),
                PowerQuery.PowerMetrics("RailsSystemFabric", 25.454282),
                PowerQuery.PowerMetrics("RailsTpu", 10.52768),
                PowerQuery.PowerMetrics("RailsWifiBt", 102.398507),
            ), actualMetrics)
    }

    @Test
    fun successfulFixedTraceTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = PowerQuery.getTotalPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                PowerQuery.PowerMetrics("Aoc", 19.60875),
                PowerQuery.PowerMetrics("CpuBig", 6.621397),
                PowerQuery.PowerMetrics("CpuLittle", 62.878706),
                PowerQuery.PowerMetrics("CpuMid", 11.440804),
                PowerQuery.PowerMetrics("Ddr", 42.199461),
                PowerQuery.PowerMetrics("Display", 208.777524),
                PowerQuery.PowerMetrics("Gpu", 13.799502),
                PowerQuery.PowerMetrics("MemoryInterface", 31.497408),
                PowerQuery.PowerMetrics("Modem", 1.735227),
                PowerQuery.PowerMetrics("Radio", 0.0),
                PowerQuery.PowerMetrics("SystemFabric", 25.454282),
                PowerQuery.PowerMetrics("Tpu", 10.52768),
                PowerQuery.PowerMetrics("Wifi", 102.398507),
            ), actualMetrics)
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = PowerQuery.getPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }

    @Test
    fun emptyFixedTraceTotal() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = PowerQuery.getTotalPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }
}
