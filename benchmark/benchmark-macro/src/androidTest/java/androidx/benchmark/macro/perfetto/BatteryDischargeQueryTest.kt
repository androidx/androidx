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

import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
@SmallTest
class BatteryDischargeQueryTest {
    @Test
    fun successfulFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_battery_discharge", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, PowerMetric.MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = BatteryDischargeQuery.getBatteryDischargeMetrics(
            traceFile.absolutePath, slice
        )
        assertEquals(listOf(
            BatteryDischargeQuery.BatteryDischargeMeasurement(
                name = "Start",
                chargeMah = 1020.0
            ),
            BatteryDischargeQuery.BatteryDischargeMeasurement(
                name = "End",
                chargeMah = 1007.0
            ),
            BatteryDischargeQuery.BatteryDischargeMeasurement(
                name = "Diff",
                chargeMah = 13.0
            )
        ), actualMetrics)
    }
}