/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.macro.runSingleSessionServer
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryCountersQueryTest {
    @Test
    @MediumTest
    fun fixedTrace31() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val metrics =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                MemoryCountersQuery.getMemoryCounters(
                    this,
                    "androidx.benchmark.integration.macrobenchmark.target",
                )
            }
        val expectedMetrics =
            MemoryCountersQuery.SubMetrics(
                minorPageFaults = 3431.0,
                majorPageFaults = 6.0,
                pageFaultsBackedBySwapCache = 0.0,
                pageFaultsBackedByReadIO = 8.0,
                memoryCompactionEvents = 0.0,
                memoryReclaimEvents = 0.0,
            )
        assertEquals(expectedMetrics, metrics)
    }
}
