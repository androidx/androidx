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

import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.benchmark.Outputs
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class StartupTimingMetricTest {
    @MediumTest
    @Test
    fun noResults() {
        assumeTrue(isAbiSupported())
        val packageName = "fake.package.fiction.nostartups"
        val iterationResult = measureStartup(packageName) {
            // Do nothing
        }
        assertEquals(true, iterationResult.singleMetrics.isEmpty())
    }

    @LargeTest
    @Test
    fun validateStartup() {
        assumeTrue(isAbiSupported())
        val packageName = "androidx.benchmark.integration.macrobenchmark.target"
        val scope = MacrobenchmarkScope(packageName = packageName, launchWithClearTask = true)
        val metrics = measureStartup(packageName) {
            // Simulate a cold start
            scope.killProcess()
            scope.dropKernelPageCache()
            scope.pressHome()
            scope.startActivityAndWait {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.action =
                    "androidx.benchmark.integration.macrobenchmark.target.TRIVIAL_STARTUP_ACTIVITY"
            }
        }
        val hasStartupMetrics = "startupMs" in metrics.singleMetrics
        assertEquals(hasStartupMetrics, true)
        assertNotNull(metrics.timelineRangeNs)
    }

    private fun validateStartup_fullyDrawn(delay: Long) {
        assumeTrue(isAbiSupported())
        val packageName = "androidx.benchmark.macro.test"
        val scope = MacrobenchmarkScope(packageName = packageName, launchWithClearTask = true)
        val iterationResult = measureStartup(packageName) {
            // Simulate a warm start, since it's our own process
            scope.pressHome()
            scope.startActivityAndWait(
                ConfigurableActivity.createIntent(
                    text = "ORIGINAL TEXT",
                    reportFullyDrawnWithDelay = delay
                )
            )

            if (delay > 0) {
                UiDevice
                    .getInstance(InstrumentationRegistry.getInstrumentation())
                    .wait(Until.findObject(By.text(ConfigurableActivity.FULLY_DRAWN_TEXT)), 3000)
            }
        }
        assertTrue("startupMs" in iterationResult.singleMetrics)
        assertTrue("fullyDrawnMs" in iterationResult.singleMetrics)

        val startupMs = iterationResult.singleMetrics["startupMs"]!!
        val fullyDrawnMs = iterationResult.singleMetrics["fullyDrawnMs"]!!

        val startupShouldBeFaster = delay > 0
        assertEquals(
            startupShouldBeFaster,
            startupMs < fullyDrawnMs,
            "startup $startupMs, fully drawn $fullyDrawnMs"
        )
        assertNotNull(iterationResult.timelineRangeNs)
    }

    @LargeTest
    @Test
    fun validateStartup_fullyDrawn_immediate() {
        validateStartup_fullyDrawn(0)
    }

    @LargeTest
    @Test
    fun validateStartup_fullyDrawn_delayed() {
        validateStartup_fullyDrawn(100)
    }

    @MediumTest
    @Test
    fun fixedStartupTraceMetrics() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("WarmStartup", ".trace")
        val metric = StartupTimingMetric()
        val packageName = "androidx.benchmark.integration.macrobenchmark.target"
        metric.configure(packageName)
        val metrics = metric.getMetrics(packageName, traceFile.absolutePath)

        // check known values
        val hasStartupMetrics = "startupMs" in metrics.singleMetrics
        assertEquals(hasStartupMetrics, true)
        assertEquals(54.82037, metrics.singleMetrics["startupMs"]!!, 0.0001)
        assertEquals(4131145997215L, metrics.timelineRangeNs?.first)
        assertEquals(4131200817585L, metrics.timelineRangeNs?.last)
    }
}

@RequiresApi(29)
internal fun measureStartup(packageName: String, measureBlock: () -> Unit): IterationResult {
    val wrapper = PerfettoCaptureWrapper()
    val metric = StartupTimingMetric()
    metric.configure(packageName)
    val tracePath = wrapper.record(
        benchmarkName = packageName,
        iteration = 1,
        packages = listOf(packageName),
        block = measureBlock
    )!!
    return metric.getMetrics(packageName, tracePath)
}

@Suppress("SameParameterValue")
internal fun createTempFileFromAsset(prefix: String, suffix: String): File {
    val file = File.createTempFile(prefix, suffix, Outputs.dirUsableByAppAndShell)
    InstrumentationRegistry
        .getInstrumentation()
        .context
        .assets
        .open(prefix + suffix)
        .copyTo(file.outputStream())
    return file
}
