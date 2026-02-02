/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.benchmark.macro

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryCountersMetricTest {
    @MediumTest
    @Test
    fun memoryCounterMetric_defaultConstructor() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = MemoryCountersMetric()
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("minorPageFaults", 3431.0),
                    Metric.Measurement("majorPageFaults", 6.0),
                    Metric.Measurement("pageFaultsBackedBySwapCache", 0.0),
                    Metric.Measurement("pageFaultsBackedByReadIO", 8.0),
                    Metric.Measurement("memoryCompactionEvents", 0.0),
                    Metric.Measurement("memoryReclaimEvents", 0.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @MediumTest
    @Test
    fun memoryCounterMetric_processSuffixAndMetricSuffixSpecified() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = MemoryCountersMetric(processNameSuffix = "", metricNameSuffix = "App")
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("minorPageFaultsApp", 3431.0),
                    Metric.Measurement("majorPageFaultsApp", 6.0),
                    Metric.Measurement("pageFaultsBackedBySwapCacheApp", 0.0),
                    Metric.Measurement("pageFaultsBackedByReadIOApp", 8.0),
                    Metric.Measurement("memoryCompactionEventsApp", 0.0),
                    Metric.Measurement("memoryReclaimEventsApp", 0.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @MediumTest
    @Test
    fun memoryCounterMetric_metricSuffixSpecified() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_cold", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = MemoryCountersMetric(metricNameSuffix = "App")
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("minorPageFaultsApp", 3431.0),
                    Metric.Measurement("majorPageFaultsApp", 6.0),
                    Metric.Measurement("pageFaultsBackedBySwapCacheApp", 0.0),
                    Metric.Measurement("pageFaultsBackedByReadIOApp", 8.0),
                    Metric.Measurement("memoryCompactionEventsApp", 0.0),
                    Metric.Measurement("memoryReclaimEventsApp", 0.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }
}
