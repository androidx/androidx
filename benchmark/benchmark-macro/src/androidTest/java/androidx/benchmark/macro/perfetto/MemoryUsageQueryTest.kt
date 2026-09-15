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
import androidx.benchmark.macro.createTempFileFromAsset
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
                    MemoryUsageMetric.SubMetric.HeapSize to 3067,
                    MemoryUsageMetric.SubMetric.RssAnon to 47260,
                    MemoryUsageMetric.SubMetric.RssFile to 67668,
                    MemoryUsageMetric.SubMetric.RssShmem to 1160,
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
    @SdkSuppress(minSdkVersion = 33)
    @MediumTest
    fun fixedTrace33() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api33_startup_memory", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                mapOf(
                    MemoryUsageMetric.SubMetric.HeapSize to 11172,
                    MemoryUsageMetric.SubMetric.RssAnon to 52724,
                    MemoryUsageMetric.SubMetric.RssFile to 102604,
                    MemoryUsageMetric.SubMetric.RssShmem to 756,
                    MemoryUsageMetric.SubMetric.Swap to 25156,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "com.android.developers.androidify",
                    mode = MemoryUsageMetric.Mode.Last,
                ),
            )
            assertEquals(
                mapOf(
                    MemoryUsageMetric.SubMetric.HeapSize to 11172,
                    MemoryUsageMetric.SubMetric.RssAnon to 52724,
                    MemoryUsageMetric.SubMetric.RssFile to 102604,
                    MemoryUsageMetric.SubMetric.RssShmem to 756,
                    MemoryUsageMetric.SubMetric.Swap to 32400,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "com.android.developers.androidify",
                    mode = MemoryUsageMetric.Mode.Max,
                ),
            )
        }
    }

    @SdkSuppress(minSdkVersion = 36)
    @Test
    @MediumTest
    fun fixedTraceStartupMemoryLast36() {
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("startup_androidify_bitmap", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                mapOf(
                    MemoryUsageMetric.SubMetric.HeapSize to 11560,
                    MemoryUsageMetric.SubMetric.RssAnon to 58248,
                    MemoryUsageMetric.SubMetric.RssFile to 149060,
                    MemoryUsageMetric.SubMetric.RssShmem to 1236,
                    MemoryUsageMetric.SubMetric.Swap to 21276,
                    MemoryUsageMetric.SubMetric.BitmapMemory to 7264,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "com.android.developers.androidify",
                    mode = MemoryUsageMetric.Mode.Last,
                ),
            )
        }
    }

    @SdkSuppress(minSdkVersion = 36)
    @Test
    @MediumTest
    fun fixedTraceStartupMemoryMax34() {
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("startup_androidify_bitmap", ".perfetto-trace")
        TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            assertEquals(
                mapOf(
                    MemoryUsageMetric.SubMetric.HeapSize to 11560,
                    MemoryUsageMetric.SubMetric.RssAnon to 60688,
                    MemoryUsageMetric.SubMetric.RssFile to 149060,
                    MemoryUsageMetric.SubMetric.RssShmem to 1236,
                    MemoryUsageMetric.SubMetric.Swap to 25208,
                    MemoryUsageMetric.SubMetric.BitmapMemory to 7264,
                ),
                MemoryUsageQuery.getMemoryUsageKb(
                    this,
                    "com.android.developers.androidify",
                    mode = MemoryUsageMetric.Mode.Max,
                ),
            )
        }
    }
}
