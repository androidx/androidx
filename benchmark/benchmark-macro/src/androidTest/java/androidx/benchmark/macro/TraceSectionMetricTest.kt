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

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.filters.MediumTest
import org.junit.Assume.assumeTrue
import org.junit.Test

@MediumTest
@OptIn(ExperimentalMetricApi::class)
class TraceSectionMetricTest {
    private val api24ColdStart =
        createTempFileFromAsset(prefix = "api24_startup_cold", suffix = ".perfetto-trace")
            .absolutePath

    private val api31ColdStart =
        createTempFileFromAsset(prefix = "api31_startup_cold", suffix = ".perfetto-trace")
            .absolutePath

    private val commasInSliceNames =
        createTempFileFromAsset(prefix = "api24_commas_in_slice_names", suffix = ".perfetto-trace")
            .absolutePath

    private val truncatedProcessName =
        createTempFileFromAsset(
                prefix = "api29_cold_startup_processname_truncated",
                suffix = ".perfetto-trace",
            )
            .absolutePath

    @Test
    fun activityThreadMain() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = Packages.TARGET,
            sectionName = "ActivityThreadMain",
            expectedFirstMs = 12.639,
        )

    @Test
    fun activityStart() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = Packages.TARGET,
            sectionName = "activityStart",
            expectedFirstMs = 81.979,
        )

    @Test
    fun startActivityAndWait() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = "androidx.benchmark.integration.macrobenchmark.test",
            sectionName = "startActivityAndWait",
            expectedFirstMs = 1_110.689,
        )

    @Test
    fun launching() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = Packages.TARGET,
            sectionName = "launching: androidx.benchmark.integration.macrobenchmark.target",
            expectedFirstMs = 269.947,
            targetPackageOnly = false, // slice from system_server
        )

    @Test
    fun section1_2() =
        verifyFirstSum(
            tracePath = commasInSliceNames,
            packageName = Packages.TARGET,
            sectionName = "section1,2",
            expectedFirstMs = 0.006615,
        )

    @Test
    fun multiSection_targetOnly() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = Packages.TARGET,
            sectionName = "inflate",
            expectedFirstMs = 4.949,
            expectedMinMs = 4.588,
            expectedMaxMs = 10.242,
            expectedSumMs = 19.779,
            expectedSumCount = 3,
            targetPackageOnly = true,
        )

    @Test
    fun multiSection_unfiltered() =
        verifyFirstSum(
            tracePath = api24ColdStart,
            packageName = Packages.TARGET,
            sectionName = "inflate",
            expectedFirstMs = 13.318, // first inflation, in diff process
            expectedMinMs = 0.836,
            expectedMaxMs = 13.318,
            expectedSumMs = 43.128,
            expectedSumCount = 8,
            targetPackageOnly = false,
        )

    @Test
    fun filterNonTerminatingSlices() =
        verifyFirstSum(
            tracePath = api31ColdStart, // arbitrary trace which includes non-termination slices
            packageName = Packages.TARGET, // ignored
            sectionName = "wait",
            expectedFirstMs = 0.00724,
            expectedMinMs = 0.001615, // filtered out non-terminating -1 duration
            expectedMaxMs = 357.761234,
            expectedSumMs = 811.865025,
            expectedSumCount = 226, // filtered out single case where dur = -1
            targetPackageOnly = false,
        )

    @Test
    fun truncatedProcessName() =
        verifyFirstSum(
            tracePath = truncatedProcessName, // trace with truncated target process name
            packageName = Packages.TARGET,
            sectionName = "Choreographer#doFrame",
            expectedFirstMs = 30.819014,
            expectedMinMs = 0.122031,
            expectedMaxMs = 30.81901,
            expectedSumMs = 31.983701,
            expectedSumCount = 3,
            targetPackageOnly = true,
        )

    companion object {
        private fun verifyMetric(
            tracePath: String,
            packageName: String,
            sectionName: String,
            mode: TraceSectionMetric.Mode,
            expectedMs: Double,
            expectedCount: Int,
            targetPackageOnly: Boolean,
        ) {
            assumeTrue(PerfettoHelper.isAbiSupported())
            // Our API 23 emulators seem to be misconfigured b/438214932
            assumeTrue(!isEmulator || SDK_INT != 23)

            val metric = TraceSectionMetric(sectionName, mode, "testLabel", targetPackageOnly)

            // note that most args are incorrect here, but currently
            // only targetPackageName matters in this context
            val captureInfo =
                Metric.CaptureInfo(
                    targetPackageName = packageName,
                    testPackageName = Packages.TEST,
                    startupMode = StartupMode.COLD,
                    apiLevel = 24,
                )
            metric.configure(captureInfo)

            val result =
                TraceProcessor.runSingleSessionServer(tracePath) {
                    metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
                }

            var measurements =
                if (mode != TraceSectionMetric.Mode.Count) {
                    listOf(Metric.Measurement("testLabel${mode.name}Ms", expectedMs))
                } else {
                    emptyList()
                }

            if (mode == TraceSectionMetric.Mode.Sum || mode == TraceSectionMetric.Mode.Count) {
                measurements =
                    measurements +
                        listOf(Metric.Measurement("testLabelCount", expectedCount.toDouble()))
            }

            assertEqualMeasurements(expected = measurements, observed = result, threshold = 0.001)
        }

        private fun verifyFirstSum(
            tracePath: String,
            packageName: String,
            sectionName: String,
            expectedFirstMs: Double,
            expectedSumMs: Double = expectedFirstMs, // default implies only one matching section
            expectedMinMs: Double = expectedFirstMs, // default implies only one matching section
            expectedMaxMs: Double = expectedFirstMs, // default implies only one matching section
            expectedSumCount: Int = 1,
            targetPackageOnly: Boolean = true,
        ) {
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.First,
                expectedMs = expectedFirstMs,
                expectedCount = 1, // unused
                targetPackageOnly = targetPackageOnly,
            )
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.Sum,
                expectedMs = expectedSumMs,
                expectedCount = expectedSumCount,
                targetPackageOnly = targetPackageOnly,
            )
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.Min,
                expectedMs = expectedMinMs,
                expectedCount = 1, // unused
                targetPackageOnly = targetPackageOnly,
            )
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.Max,
                expectedMs = expectedMaxMs,
                expectedCount = 1, // unused
                targetPackageOnly = targetPackageOnly,
            )
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.Count,
                expectedMs = 1.0, // unused
                expectedCount = expectedSumCount,
                targetPackageOnly = targetPackageOnly,
            )
            verifyMetric(
                tracePath = tracePath,
                packageName = packageName,
                sectionName = sectionName,
                mode = TraceSectionMetric.Mode.Average,
                expectedMs = expectedSumMs / expectedSumCount,
                expectedCount = 1, // unused
                targetPackageOnly = targetPackageOnly,
            )
        }
    }
}
