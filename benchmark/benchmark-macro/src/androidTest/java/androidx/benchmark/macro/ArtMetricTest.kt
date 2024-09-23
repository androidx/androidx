/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.benchmark.DeviceInfo
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test

@MediumTest
@SdkSuppress(minSdkVersion = 24)
class ArtMetricTest {
    data class SubMetric(
        val count: Long,
        val sum: Double,
    )

    @Test
    fun basic() =
        verifyArtMetrics(
            apiLevel = 35,
            artMainlineVersion = null, // unknown, but not important on 35
            expectedJit = SubMetric(177, 433.488508),
            expectedClassInit = SubMetric(2013, 147.052337),
            expectedClassVerify = SubMetric(0, 0.0)
        )

    @Test
    fun filterOutClassInit() =
        verifyArtMetrics(
            apiLevel = 31,
            artMainlineVersion = DeviceInfo.ART_MAINLINE_MIN_VERSION_CLASS_INIT_TRACING - 1,
            expectedJit = SubMetric(177, 433.488508),
            expectedClassInit = null, // drops class init
            expectedClassVerify = SubMetric(0, 0.0)
        )

    @Test
    fun oldVersionMainline() =
        verifyArtMetrics(
            apiLevel = 31,
            artMainlineVersion = DeviceInfo.ART_MAINLINE_MIN_VERSION_CLASS_INIT_TRACING,
            expectedJit = SubMetric(177, 433.488508),
            expectedClassInit = SubMetric(2013, 147.052337),
            expectedClassVerify = SubMetric(0, 0.0)
        )

    companion object {
        private fun verifyArtMetrics(
            apiLevel: Int,
            artMainlineVersion: Long?,
            expectedJit: SubMetric,
            expectedClassInit: SubMetric?,
            expectedClassVerify: SubMetric
        ) {
            val tracePath =
                createTempFileFromAsset(
                        prefix = "api35_startup_cold_classinit",
                        suffix = ".perfetto-trace"
                    )
                    .absolutePath

            assumeTrue(PerfettoHelper.isAbiSupported())

            val metric = ArtMetric()
            val captureInfo =
                Metric.CaptureInfo(
                    apiLevel = apiLevel,
                    artMainlineVersion = artMainlineVersion,
                    targetPackageName = "androidx.compose.integration.hero.macrobenchmark.target",
                    testPackageName = Packages.TEST,
                    startupMode = StartupMode.COLD,
                )
            metric.configure(captureInfo)
            val result =
                PerfettoTraceProcessor.runSingleSessionServer(tracePath) {
                    metric.getMeasurements(
                        // note that most args are incorrect here, but currently
                        // only targetPackageName matters in this context
                        captureInfo = captureInfo,
                        traceSession = this
                    )
                }

            val expectedMeasurements =
                listOf(
                    Metric.Measurement("artJitSumMs", expectedJit.sum),
                    Metric.Measurement("artJitCount", expectedJit.count.toDouble()),
                    Metric.Measurement("artVerifyClassSumMs", expectedClassVerify.sum),
                    Metric.Measurement("artVerifyClassCount", expectedClassVerify.count.toDouble()),
                ) +
                    if (expectedClassInit != null) {
                        listOf(
                            Metric.Measurement("artClassInitSumMs", expectedClassInit.sum),
                            Metric.Measurement(
                                "artClassInitCount",
                                expectedClassInit.count.toDouble()
                            ),
                        )
                    } else {
                        emptyList()
                    }

            assertThat(result).containsExactlyElementsIn(expectedMeasurements)
            assertEqualMeasurements(
                expected = expectedMeasurements,
                observed = result,
                threshold = 0.001
            )
        }
    }
}
