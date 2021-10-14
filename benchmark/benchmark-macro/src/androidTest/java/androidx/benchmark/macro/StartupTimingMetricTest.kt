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
import android.content.Intent
import android.os.Build
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

@SdkSuppress(minSdkVersion = 23)
@RunWith(AndroidJUnit4::class)
class StartupTimingMetricTest {
    @MediumTest
    @Test
    fun noResults() {
        assumeTrue(isAbiSupported())
        val packageName = "fake.package.fiction.nostartups"
        val iterationResult = measureStartup(packageName, StartupMode.COLD) {
            // Do nothing
        }
        assertEquals(true, iterationResult.singleMetrics.isEmpty())
    }

    @LargeTest
    @Test
    // Disabled pre-29, as other process may not have tracing for reportFullyDrawn pre-29, due to
    // lack of profileable. Within our test process (other startup tests in this class), we use
    // reflection to force reportFullyDrawn() to be traced. See b/182386956
    @SdkSuppress(minSdkVersion = 29)
    fun validateStartup() {
        assumeTrue(isAbiSupported())
        val packageName = "androidx.benchmark.integration.macrobenchmark.target"
        val scope = MacrobenchmarkScope(packageName = packageName, launchWithClearTask = true)
        val iterationResult = measureStartup(packageName, StartupMode.COLD) {
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

        assertEquals(
            setOf("timeToInitialDisplayMs", "timeToFullDisplayMs"),
            iterationResult.singleMetrics.keys
        )
        assertNotNull(iterationResult.timelineRangeNs)
    }

    private fun validateStartup_fullyDrawn(delayMs: Long) {
        assumeTrue(isAbiSupported())
        val scope = MacrobenchmarkScope(packageName = Packages.TEST, launchWithClearTask = true)
        val iterationResult = measureStartup(Packages.TEST, StartupMode.WARM) {
            // Simulate a warm start, since it's our own process
            scope.pressHome()
            scope.startActivityAndWait(
                ConfigurableActivity.createIntent(
                    text = "ORIGINAL TEXT",
                    reportFullyDrawnWithDelay = delayMs
                )
            )

            if (delayMs > 0) {
                UiDevice
                    .getInstance(InstrumentationRegistry.getInstrumentation())
                    .wait(Until.findObject(By.text(ConfigurableActivity.FULLY_DRAWN_TEXT)), 3000)
            }
        }

        assertEquals(
            setOf("timeToInitialDisplayMs", "timeToFullDisplayMs"),
            iterationResult.singleMetrics.keys
        )
        assertNotNull(iterationResult.timelineRangeNs)

        val timeToInitialDisplayMs = iterationResult.singleMetrics["timeToInitialDisplayMs"]!!
        val timeToFullDisplayMs = iterationResult.singleMetrics["timeToFullDisplayMs"]!!

        if (delayMs == 0L) {
            // since reportFullyDrawn is dispatched before startup is complete,
            // timeToInitialDisplay and timeToFullDisplay should match
            assertEquals(timeToFullDisplayMs, timeToInitialDisplayMs, 0.0001)
        } else {
            // expect to see at a gap of around delayMs or more between two metrics
            assertTrue(
                timeToFullDisplayMs > timeToInitialDisplayMs,
                "Didn't see full draw delayed after initial display: " +
                    "ttid $timeToInitialDisplayMs, ttfd $timeToFullDisplayMs"
            )
        }
        assertNotNull(iterationResult.timelineRangeNs)
    }

    @LargeTest
    @Test
    fun validateStartup_fullyDrawn_immediate() {
        validateStartup_fullyDrawn(0)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 29) // TODO: fullydrawn behavior pre-profileable tag
    fun validateStartup_fullyDrawn_delayed() {
        validateStartup_fullyDrawn(100)
    }

    private fun getApi31WarmMetrics(metric: Metric): IterationResult {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset("api31_startup_warm", ".perfetto-trace")
        val packageName = "androidx.benchmark.integration.macrobenchmark.target"
        metric.configure(packageName)
        return metric.getMetrics(
            captureInfo = Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.test",
                startupMode = StartupMode.WARM,
                apiLevel = 31
            ),
            tracePath = traceFile.absolutePath
        )
    }

    @MediumTest
    @Test
    fun fixedStartupTraceMetricsReport_fullyDrawnBeforeFirstFrame() {
        assumeTrue(isAbiSupported())
        val traceFile = createTempFileFromAsset(
            prefix = "api24_startup_sameproc_immediatefullydrawn",
            suffix = ".perfetto-trace"
        )
        val metric = StartupTimingMetric()
        metric.configure(Packages.TEST)
        val metrics = metric.getMetrics(
            captureInfo = Metric.CaptureInfo(
                targetPackageName = Packages.TEST,
                testPackageName = Packages.TEST,
                startupMode = StartupMode.WARM,
                apiLevel = 24
            ),
            tracePath = traceFile.absolutePath
        )

        // check known values
        assertEquals(
            setOf("timeToInitialDisplayMs", "timeToFullDisplayMs"),
            metrics.singleMetrics.keys
        )
        assertEquals(169.67427, metrics.singleMetrics["timeToInitialDisplayMs"]!!, 0.0001)
        assertEquals(169.67427, metrics.singleMetrics["timeToFullDisplayMs"]!!, 0.0001)
        assertEquals(477547965787..477717640057, metrics.timelineRangeNs)
    }

    @MediumTest
    @Test
    fun fixedStartupTraceMetrics() {
        val metrics = getApi31WarmMetrics(StartupTimingMetric())

        // check known values
        assertEquals(
            setOf("timeToInitialDisplayMs", "timeToFullDisplayMs"),
            metrics.singleMetrics.keys
        )
        assertEquals(64.748027, metrics.singleMetrics["timeToInitialDisplayMs"]!!, 0.0001)
        assertEquals(555.968701, metrics.singleMetrics["timeToFullDisplayMs"]!!, 0.0001)
        assertEquals(186982050780778..186982606749479, metrics.timelineRangeNs)
    }

    @SuppressLint("NewApi") // suppressed for StartupTimingLegacyMetric, since data is fixed
    @MediumTest
    @Test
    fun fixedStartupTraceMetrics_legacy() {
        val metrics = getApi31WarmMetrics(StartupTimingLegacyMetric())

        // check known values
        assertEquals(setOf("startupMs", "fullyDrawnMs"), metrics.singleMetrics.keys)
        assertEquals(64.748027, metrics.singleMetrics["startupMs"]!!, 0.0001)
        assertEquals(543.742658, metrics.singleMetrics["fullyDrawnMs"]!!, 0.0001)
        assertEquals(186982050780778..186982115528805, metrics.timelineRangeNs)
    }
}

@RequiresApi(23)
internal fun measureStartup(
    packageName: String,
    startupMode: StartupMode,
    measureBlock: () -> Unit
): IterationResult {
    val wrapper = PerfettoCaptureWrapper()
    val metric = StartupTimingMetric()
    metric.configure(packageName)
    val tracePath = wrapper.record(
        benchmarkName = packageName,
        iteration = 1,
        // note - packageName may be this package, so we convert to set then list to make unique
        // and on API 23 and below, we use reflection to trace instead within this process
        packages = if (Build.VERSION.SDK_INT >= 24 && packageName != Packages.TEST) {
            listOf(packageName, Packages.TEST)
        } else {
            listOf(packageName)
        },
        block = measureBlock
    )!!
    return metric.getMetrics(
        captureInfo = Metric.CaptureInfo(
            targetPackageName = packageName,
            testPackageName = Packages.TEST,
            startupMode = startupMode,
            apiLevel = Build.VERSION.SDK_INT
        ),
        tracePath = tracePath
    )
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
