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

import androidx.benchmark.macro.PowerMetric.Companion.SECTION_NAME
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
        val slice = PerfettoTraceProcessor.querySlices(traceFile.absolutePath, SECTION_NAME).first()
        val actualMetrics = PowerQuery.getPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(
            listOf(
                PowerQuery.PowerMetrics("L15mVddSlcM", 20.138912),
                PowerQuery.PowerMetrics("L8sUfsVccq", 17.775489),
                PowerQuery.PowerMetrics("RailsAocLogic", 18.339422),
                PowerQuery.PowerMetrics("RailsAocMemory", 14.182031),
                PowerQuery.PowerMetrics("RailsCpuBig", 36.136576),
                PowerQuery.PowerMetrics("RailsCpuLittle", 579.97876),
                PowerQuery.PowerMetrics("RailsCpuMid", 674.998301),
                PowerQuery.PowerMetrics("RailsDdrA", 6.01678),
                PowerQuery.PowerMetrics("RailsDdrB", 2.536958),
                PowerQuery.PowerMetrics("RailsDdrC", 12.894647),
                PowerQuery.PowerMetrics("RailsDisplay", 134.277613),
                PowerQuery.PowerMetrics("RailsGps", 3.958581),
                PowerQuery.PowerMetrics("RailsGpu", 0.386151),
                PowerQuery.PowerMetrics("RailsMemoryInterface", 36.141037),
                PowerQuery.PowerMetrics("RailsSystemFabric", 30.41695),
                PowerQuery.PowerMetrics("RailsTpu", 2.245327)
            ), actualMetrics)
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(traceFile.absolutePath, SECTION_NAME).first()
        val actualMetrics = PowerQuery.getPowerMetrics(traceFile.absolutePath, slice)
        assertEquals(emptyList(), actualMetrics)
    }
}
