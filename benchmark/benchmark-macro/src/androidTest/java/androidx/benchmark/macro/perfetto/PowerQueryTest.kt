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

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.PowerCategory
import androidx.benchmark.macro.PowerMetric.Companion.MEASURE_BLOCK_SECTION_NAME
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PowerQueryTest {
    @Test
    fun successfulFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api32_odpm_rails", ".perfetto-trace")
        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerQuery.getPowerMetrics(
                this,
                querySlices(MEASURE_BLOCK_SECTION_NAME, packageName = null).first()
            )
        }

        assertEquals(
            mapOf(
                PowerCategory.CPU to PowerQuery.CategoryMeasurement(
                    energyUws = 390378.0,
                    powerUw = 80.94090814845532,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "CpuBig",
                            energyUws = 31935.0,
                            powerUw = 6.621397470454074
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "CpuLittle",
                            energyUws = 303264.0,
                            powerUw = 62.878706199460915
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "CpuMid",
                            energyUws = 55179.0,
                            powerUw = 11.440804478540327
                        )
                    )
                ),
                PowerCategory.DISPLAY to PowerQuery.CategoryMeasurement(
                    energyUws = 1006934.0,
                    powerUw = 208.77752436243003,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "Display",
                            energyUws = 1006934.0,
                            powerUw = 208.77752436243003
                        )
                    )
                ),
                PowerCategory.GPU to PowerQuery.CategoryMeasurement(
                    energyUws = 66555.0,
                    powerUw = 13.799502384408045,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "Gpu",
                            energyUws = 66555.0,
                            powerUw = 13.799502384408045
                        )
                    )
                ),
                PowerCategory.MEMORY to PowerQuery.CategoryMeasurement(
                    energyUws = 355440.0,
                    powerUw = 73.69686916856728,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "DdrA",
                            energyUws = 48458.0,
                            powerUw = 10.047273481235745
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "DdrB",
                            energyUws = 54988.0,
                            powerUw = 11.401202571013892
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "DdrC",
                            energyUws = 100082.0,
                            powerUw = 20.75098486419241
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "MemoryInterface",
                            energyUws = 151912.0,
                            powerUw = 31.497408252125233
                        ),
                    )
                ),
                PowerCategory.MACHINE_LEARNING to PowerQuery.CategoryMeasurement(
                    energyUws = 50775.0,
                    powerUw = 10.527679867302508,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "Tpu",
                            energyUws = 50775.0,
                            powerUw = 10.527679867302508
                        )
                    )
                ),
                PowerCategory.NETWORK to PowerQuery.CategoryMeasurement(
                    energyUws = 596810.0,
                    powerUw = 123.74248393116318,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "AocLogic",
                            energyUws = 74972.0,
                            powerUw = 15.544681733360978
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "AocMemory",
                            energyUws = 19601.0,
                            powerUw = 4.0640680074642335
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "Modem",
                            energyUws = 8369.0,
                            powerUw = 1.7352270371138296
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "RadioFrontend",
                            energyUws = 0.0,
                            powerUw = 0.0
                        ),
                        PowerQuery.ComponentMeasurement(
                            name = "WifiBt",
                            energyUws = 493868.0,
                            powerUw = 102.39850715322413
                        )
                    )
                ),
                PowerCategory.UNCATEGORIZED to PowerQuery.CategoryMeasurement(
                    energyUws = 122766.0,
                    powerUw = 25.454281567489115,
                    components = listOf(
                        PowerQuery.ComponentMeasurement(
                            name = "SystemFabric",
                            energyUws = 122766.0,
                            powerUw = 25.454281567489115
                        )
                    )
                )
            ), actualMetrics
        )
    }

    @Test
    fun emptyFixedTrace() {
        assumeTrue(isAbiSupported())

        val traceFile = createTempFileFromAsset("api31_odpm_rails_empty", ".perfetto-trace")

        val actualMetrics = PerfettoTraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            PowerQuery.getPowerMetrics(
                this,
                querySlices(MEASURE_BLOCK_SECTION_NAME, packageName = null).first()
            )
        }

        assertEquals(emptyMap(), actualMetrics)
    }
}
