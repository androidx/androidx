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

import android.annotation.SuppressLint
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.test.filters.MediumTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals

@MediumTest
@OptIn(ExperimentalMetricApi::class)
class TraceSectionMetricTest {
    private val tracePath = createTempFileFromAsset(
        prefix = "api24_startup_cold",
        suffix = ".perfetto-trace"
    ).absolutePath

    private val captureInfo = Metric.CaptureInfo(
        targetPackageName = Packages.TEST,
        testPackageName = Packages.TEST,
        startupMode = StartupMode.COLD,
        apiLevel = 24
    )

    @Test
    fun activityThreadMain() = verifySingleMetric("ActivityThreadMain", 12.639)

    @Test
    fun activityStart() = verifySingleMetric("activityStart", 81.979)

    @Test
    fun startActivityAndWait() = verifySingleMetric("startActivityAndWait", 1_110.689)

    @Test
    fun launching() = verifySingleMetric(
        "launching: androidx.benchmark.integration.macrobenchmark.target",
        269.947
    )

    @SuppressLint("NewApi") // we use a fixed trace - ignore for TraceSectionMetric
    private fun verifySingleMetric(
        sectionName: String,
        expectedMs: Double
    ) {
        assumeTrue(PerfettoHelper.isAbiSupported())

        val metric = TraceSectionMetric(sectionName)
        val expectedKey = sectionName + "Ms"
        metric.configure(Packages.TEST)
        val iterationResult = metric.getMetrics(
            captureInfo = captureInfo,
            tracePath = tracePath
        )

        assertEquals(setOf(expectedKey), iterationResult.singleMetrics.keys)
        assertEquals(expectedMs, iterationResult.singleMetrics[expectedKey]!!, 0.001)
    }
}