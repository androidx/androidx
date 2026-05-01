/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.testutils

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.Outputs
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.runSingleSessionServer
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class CpuChangeFrequencyMetricTest {

    @MediumTest
    @Test
    fun cpuChangeFrequencyMetric_traceWith2TimeOffsetFrequencyChanges_returns2() {
        skipOnIncompatibleTestTargets()
        val traceFile = createTempFileFromAsset("api31_cpu_frequency_change", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName =
                    "androidx.compose.integration.hero.pokedex.macrobenchmark.target",
                testPackageName = "androidx.compose.integration.hero.pokedex.macrobenchmark",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = CpuFrequencyChangeMetric()

        assertMeasurementValues(traceFile, metric, captureInfo, 2.0)
    }

    @MediumTest
    @Test
    fun cpuChangeFrequencyMetric_traceWithNoFrequencyChanges_returns0() {
        skipOnIncompatibleTestTargets()
        val traceFile = createTempFileFromAsset("api31_no_cpu_frequency_change", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName =
                    "androidx.compose.integration.hero.pokedex.macrobenchmark.target",
                testPackageName = "androidx.compose.integration.hero.pokedex.macrobenchmark",
                startupMode = StartupMode.COLD,
                apiLevel = 31,
            )
        val metric = CpuFrequencyChangeMetric()

        assertMeasurementValues(traceFile, metric, captureInfo, 0.0)
    }

    private fun assertMeasurementValues(
        traceFile: File,
        metric: CpuFrequencyChangeMetric,
        captureInfo: Metric.CaptureInfo,
        expectedCpuFrequencyChanges: Double,
    ) {
        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertThat(measurements).hasSize(1)
        val measurement = measurements.first()
        assertThat(measurement.name).isEqualTo("freqChangeCountCpu")
        assertWithMessage("Measurement data for ${measurement.name}")
            .that(measurement.data)
            .hasSize(1)
        assertWithMessage("Expected Number of Frequency Changes for ${measurement.name}")
            .that(measurement.data.first())
            .isEqualTo(expectedCpuFrequencyChanges)
    }

    private fun skipOnIncompatibleTestTargets() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(PerfettoHelper.isAbiSupported())
    }
}

@Suppress("SameParameterValue")
internal fun createTempFileFromAsset(prefix: String, suffix: String): File {
    val file = File.createTempFile(prefix, suffix, Outputs.dirUsableByAppAndShell)
    InstrumentationRegistry.getInstrumentation()
        .context
        .assets
        .open(prefix + suffix)
        .copyTo(file.outputStream())
    return file
}
