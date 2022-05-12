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

import androidx.benchmark.macro.PowerCategory
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
            mapOf(
                PowerCategory.CPU to PowerQuery.CategoryMeasurement(
                    energyUws = 390378.0,
                    powerUw = 80.940907,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsCpuBig",
                            energyUws = 31935.0,
                            powerUw = 6.621397
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsCpuLittle",
                            energyUws = 303264.0,
                            powerUw = 62.878706
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsCpuMid",
                            energyUws = 55179.0,
                            powerUw = 11.440804
                        )
                    )
                ),
                PowerCategory.DISPLAY to PowerQuery.CategoryMeasurement(
                    energyUws = 1006934.0,
                    powerUw = 208.777524,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsDisplay",
                            energyUws = 1006934.0,
                            powerUw = 208.777524
                        )
                    )
                ),
                PowerCategory.GPU to PowerQuery.CategoryMeasurement(
                    energyUws = 66555.0,
                    powerUw = 13.799502,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsGpu",
                            energyUws = 66555.0,
                            powerUw = 13.799502
                        )
                    )
                ),
                PowerCategory.MEMORY to PowerQuery.CategoryMeasurement(
                    energyUws = 355440.0,
                    powerUw = 73.69686899999999,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsDdrA",
                            energyUws = 48458.0,
                            powerUw = 10.047273
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsDdrB",
                            energyUws = 54988.0,
                            powerUw = 11.401203
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsDdrC",
                            energyUws = 100082.0,
                            powerUw = 20.750985),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsMemoryInterface",
                            energyUws = 151912.0,
                            powerUw = 31.497408
                        ),
                    )
                ),
                PowerCategory.MACHINE_LEARNING to PowerQuery.CategoryMeasurement(
                    energyUws = 50775.0,
                    powerUw = 10.52768,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsTpu",
                            energyUws = 50775.0,
                            powerUw = 10.52768
                        )
                    )
                ),
                PowerCategory.NETWORK to PowerQuery.CategoryMeasurement(
                    energyUws = 596810.0,
                    powerUw = 123.74248399999999,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsAocLogic",
                            energyUws = 74972.0,
                            powerUw = 15.544682),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsAocMemory",
                            energyUws = 19601.0,
                            powerUw = 4.064068
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsModem",
                            energyUws = 8369.0,
                            powerUw = 1.735227
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsRadioFrontend",
                            energyUws = 0.0,
                            powerUw = 0.0
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RailsWifiBt",
                            energyUws = 493868.0,
                            powerUw = 102.398507
                        )
                    )
                ),
                PowerCategory.UNCATEGORIZED to PowerQuery.CategoryMeasurement(
                    energyUws = 122766.0,
                    powerUw = 25.454282,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "RailsSystemFabric",
                            energyUws = 122766.0,
                            powerUw = 25.454282
                        )
                    )
                )
            ), actualMetrics)
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")
        val slice = PerfettoTraceProcessor.querySlices(
            traceFile.absolutePath, MEASURE_BLOCK_SECTION_NAME
        ).first()
        val actualMetrics = PowerQuery.getPowerMetrics(
            traceFile.absolutePath, slice
        )
        assertEquals(emptyMap(), actualMetrics)
    }
}
