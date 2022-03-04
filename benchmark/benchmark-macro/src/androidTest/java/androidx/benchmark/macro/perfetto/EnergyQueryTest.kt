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

import androidx.benchmark.macro.EnergyMetric.Companion.SECTION_NAME
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
        val slice = PerfettoTraceProcessor.querySlices(traceFile.absolutePath, SECTION_NAME).first()
        val actualMetrics = EnergyQuery.getEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                EnergyQuery.EnergyMetrics("L15mVddSlcM", 94814.0),
                EnergyQuery.EnergyMetrics("L8sUfsVccq", 83687.0),
                EnergyQuery.EnergyMetrics("RailsAocLogic", 86342.0),
                EnergyQuery.EnergyMetrics("RailsAocMemory", 66769.0),
                EnergyQuery.EnergyMetrics("RailsCpuBig", 170131.0),
                EnergyQuery.EnergyMetrics("RailsCpuLittle", 2730540.0),
                EnergyQuery.EnergyMetrics("RailsCpuMid", 3177892.0),
                EnergyQuery.EnergyMetrics("RailsDdrA", 28327.0),
                EnergyQuery.EnergyMetrics("RailsDdrB", 11944.0),
                EnergyQuery.EnergyMetrics("RailsDdrC", 60708.0),
                EnergyQuery.EnergyMetrics("RailsDisplay", 632179.0),
                EnergyQuery.EnergyMetrics("RailsGps", 18637.0),
                EnergyQuery.EnergyMetrics("RailsGpu", 1818.0),
                EnergyQuery.EnergyMetrics("RailsMemoryInterface", 170152.0),
                EnergyQuery.EnergyMetrics("RailsSystemFabric", 143203.0),
                EnergyQuery.EnergyMetrics("RailsTpu", 10571.0)
            ), actualMetrics)
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(traceFile.absolutePath, SECTION_NAME).first()
        val actualMetrics = EnergyQuery.getEnergyMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }
}
