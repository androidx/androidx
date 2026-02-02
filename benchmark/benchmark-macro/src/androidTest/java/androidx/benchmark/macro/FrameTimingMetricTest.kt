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
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameTimingMetricTest {
    private val frameDurationCpuMsExpectedValues =
        listOf(
            6.881407,
            5.648542,
            3.830261,
            4.343438,
            4.820522,
            11.301147,
            4.205469,
            4.076615,
            4.973699,
            4.408334,
        )
    private val frameOverrunMsExpectedValues =
        listOf(
            -5.207137,
            -11.699862,
            -14.025295,
            -12.300155,
            -11.944858,
            -8.031123,
            -9.73489,
            -10.849726,
            -11.046253,
            -10.997936,
        )

    @MediumTest
    @Test
    fun frameTimingMetric_defaultConstructor() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_scroll", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = FrameTimingMetric()
        metric.configure(captureInfo)
        assertMeasurementValues(
            traceFile,
            metric,
            captureInfo,
            "frameCount",
            "frameDurationCpuMs",
            "frameOverrunMs",
            frameCount = 96.0,
            frameDurationCpuMsExpectedValues = this.frameDurationCpuMsExpectedValues,
            frameOverrunMsExpectedValues = this.frameOverrunMsExpectedValues,
        )
    }

    @MediumTest
    @Test
    fun frameTimingMetric_processSuffixAndMetricSuffixSpecified() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_scroll", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = FrameTimingMetric(processNameSuffix = "", metricNameSuffix = "App")
        metric.configure(captureInfo)

        val measurementNames =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                    metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
                }
                .map { measurement -> measurement.name }
                .toSet()

        assertEquals(
            setOf("frameCountApp", "frameDurationCpuMsApp", "frameOverrunMsApp"),
            measurementNames,
        )

        assertMeasurementValues(
            traceFile,
            metric,
            captureInfo,
            "frameCountApp",
            "frameDurationCpuMsApp",
            "frameOverrunMsApp",
            frameCount = 96.0,
            frameDurationCpuMsExpectedValues = this.frameDurationCpuMsExpectedValues,
            frameOverrunMsExpectedValues = this.frameOverrunMsExpectedValues,
        )
    }

    private fun assertMeasurementValues(
        traceFile: File,
        metric: FrameTimingMetric,
        captureInfo: Metric.CaptureInfo,
        frameCountMeasurementName: String,
        frameDurationCpuMsMeasurementName: String,
        frameOverrunMsMeasurementName: String,
        frameCount: Double,
        frameDurationCpuMsExpectedValues: List<Double>, // First 10 values only.
        frameOverrunMsExpectedValues: List<Double>, // First 10 values only.
    ) {
        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        // Check names of all collected measurements.
        val measurementNames = measurements.map { measurement -> measurement.name }.toSet()
        assertEquals(
            setOf(
                frameCountMeasurementName,
                frameDurationCpuMsMeasurementName,
                frameOverrunMsMeasurementName,
            ),
            measurementNames,
        )

        // Check measurement values.
        val frameCountMeasurement =
            measurements
                .filter { measurement -> measurement.name == frameCountMeasurementName }
                .toList()
        assertEqualMeasurements(
            expected = listOf(Metric.Measurement(frameCountMeasurementName, frameCount)),
            observed = frameCountMeasurement,
            threshold = 0.0001,
        )

        val frameDurationCpuMsValuesObserved =
            measurements
                .filter { measurement -> measurement.name == frameDurationCpuMsMeasurementName }
                .toList()[0]
                .data
                .take(10)
        val frameOverrunMsValuesObserved =
            measurements
                .filter { measurement -> measurement.name == frameOverrunMsMeasurementName }
                .toList()[0]
                .data
                .take(10)
        assertEquals(frameDurationCpuMsValuesObserved, frameDurationCpuMsExpectedValues)
        assertEquals(frameOverrunMsValuesObserved, frameOverrunMsExpectedValues)
    }
}
