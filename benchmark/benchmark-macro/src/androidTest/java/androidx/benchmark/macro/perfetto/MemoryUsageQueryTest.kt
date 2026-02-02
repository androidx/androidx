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
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.MemoryUsageMetric.SubMetric
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
class MemoryUsageQueryTest {
    @Test
    @MediumTest
    fun fixedTrace31() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            // Note: this particular trace has same values for last and max
            val expected =
                mapOf(
                    SubMetric.HeapSize to 3067,
                    SubMetric.RssAnon to 47260,
                    SubMetric.RssFile to 67668,
                    SubMetric.RssShmem to 1160,
                )
            assertEquals(
                expected,
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "androidx.benchmark.integration.macrobenchmark.target",
                    mode = MemoryUsageMetric.Mode.Last,
                ),
            )
            assertEquals(
                expected,
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "androidx.benchmark.integration.macrobenchmark.target",
                    mode = MemoryUsageMetric.Mode.Max,
                ),
            )
        }
    }

    @Test
    @MediumTest
    fun fixedTrace33() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api33_motionlayout_messagejson", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                mapOf(
                    SubMetric.HeapSize to 25019,
                    SubMetric.RssAnon to 78516,
                    SubMetric.RssFile to 88036,
                    SubMetric.RssShmem to 1540,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "androidx.constraintlayout.compose.integration.macrobenchmark.target",
                    mode = MemoryUsageMetric.Mode.Last,
                ),
            )
            assertEquals(
                mapOf(
                    SubMetric.HeapSize to 25019,
                    SubMetric.RssAnon to 78516,
                    SubMetric.RssFile to 88168,
                    SubMetric.RssShmem to 1540,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "androidx.constraintlayout.compose.integration.macrobenchmark.target",
                    mode = MemoryUsageMetric.Mode.Max,
                ),
            )
        }
    }

    @Test
    @MediumTest
    fun fixedGpuTrace34() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api34_startup_cold", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                mapOf(
                    SubMetric.Gpu to 30840,
                    SubMetric.HeapSize to 3385,
                    SubMetric.RssAnon to 47152,
                    SubMetric.RssFile to 96868,
                    SubMetric.RssShmem to 16336,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "com.android.systemui.people",
                    mode = MemoryUsageMetric.Mode.Last,
                ),
            )
        }
    }
}
