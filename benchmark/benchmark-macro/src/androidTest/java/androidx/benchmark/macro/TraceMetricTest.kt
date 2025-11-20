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
class TraceMetricTest {
    private val api31HotStart =
        createTempFileFromAsset(prefix = "api31_startup_hot", suffix = ".perfetto-trace")
            .absolutePath

    @Test
    fun verifyActivityResume() = verifyActivityResume(tracePath = api31HotStart, expectedMs = 0.322)

    class ActivityResumeMetric : TraceMetric() {
        override fun getMeasurements(
            captureInfo: CaptureInfo,
            traceSession: TraceProcessor.Session,
        ): List<Measurement> {
            val rowSequence =
                traceSession.query(
                    """
                SELECT
                    slice.name as name,
                    slice.ts as ts,
                    slice.dur as dur
                FROM slice
                    INNER JOIN thread_track on slice.track_id = thread_track.id
                    INNER JOIN thread USING(utid)
                    INNER JOIN process USING(upid)
                WHERE
                    process.name LIKE "${captureInfo.targetPackageName}"
                        AND slice.name LIKE "activityResume"
                """
                        .trimIndent()
                )
            val row = rowSequence.firstOrNull()
            val activityResultNs = row?.long("dur")
            println("ns $row, $activityResultNs")
            return if (activityResultNs != null) {
                listOf(Measurement("activityResumeMs", activityResultNs / 1_000_000.0))
            } else {
                emptyList()
            }
        }
    }

    companion object {
        private val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = Packages.TARGET,
                testPackageName = Packages.TEST,
                startupMode = StartupMode.HOT,
                apiLevel = 31,
            )

        private fun verifyActivityResume(
            tracePath: String,
            @Suppress("SameParameterValue") expectedMs: Double,
        ) {
            assumeTrue(PerfettoHelper.isAbiSupported())
            // Our API 23 emulators seem to be misconfigured b/438214932
            assumeTrue(!isEmulator || SDK_INT != 23)
            val metric = ActivityResumeMetric()
            metric.configure(captureInfo)

            val result =
                TraceProcessor.runSingleSessionServer(tracePath) {
                    metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
                }

            assertEqualMeasurements(
                expected = listOf(Metric.Measurement("activityResumeMs", expectedMs)),
                observed = result,
                threshold = 0.001,
            )
        }
    }
}
