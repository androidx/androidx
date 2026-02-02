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
class MemoryUsageMetricTest {
    @MediumTest
    @Test
    fun memoryUsageMetric_defaultConstructor() {
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
        val metric = MemoryUsageMetric(mode = MemoryUsageMetric.Mode.Max)
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("memoryHeapSizeMaxKb", 3067.0),
                    Metric.Measurement("memoryRssAnonMaxKb", 47260.0),
                    Metric.Measurement("memoryRssFileMaxKb", 67668.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @MediumTest
    @Test
    fun memoryUsageMetric_processSuffixAndMetricSuffixSpecified() {
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
        val metric =
            MemoryUsageMetric(
                mode = MemoryUsageMetric.Mode.Max,
                processNameSuffix = "",
                metricNameSuffix = "App",
            )
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("memoryAppHeapSizeMaxKb", 3067.0),
                    Metric.Measurement("memoryAppRssAnonMaxKb", 47260.0),
                    Metric.Measurement("memoryAppRssFileMaxKb", 67668.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @MediumTest
    @Test
    fun memoryUsageMetric_metricSuffixSpecified() {
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
        val metric = MemoryUsageMetric(mode = MemoryUsageMetric.Mode.Max, metricNameSuffix = "App")
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("memoryAppHeapSizeMaxKb", 3067.0),
                    Metric.Measurement("memoryAppRssAnonMaxKb", 47260.0),
                    Metric.Measurement("memoryAppRssFileMaxKb", 67668.0),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }
}
