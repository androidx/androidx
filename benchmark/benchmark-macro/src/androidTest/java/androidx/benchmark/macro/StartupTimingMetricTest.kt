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
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.benchmark.DeviceInfo
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.Outputs
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupTimingMetricTest {
    @MediumTest
    @Test
    @Ignore("b/258335082")
    fun noResults() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(isAbiSupported())
        val packageName = "fake.package.fiction.nostartups"
        val measurements =
            measureStartup(packageName, StartupMode.COLD) {
                // Do nothing
            }
        assertEquals(true, measurements.isEmpty())
    }

    @LargeTest
    @Test
    // Disabled pre-29, as other process may not have tracing for reportFullyDrawn pre-29, due to
    // lack of profileable. Within our test process (other startup tests in this class), we use
    // reflection to force reportFullyDrawn() to be traced. See b/182386956
    @SdkSuppress(minSdkVersion = 29)
    fun startup() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(isAbiSupported())
        val packageName = "androidx.benchmark.integration.macrobenchmark.target"
        val intent =
            Intent("androidx.benchmark.integration.macrobenchmark.target.TRIVIAL_STARTUP_ACTIVITY")
        val scope = MacrobenchmarkScope(packageName = packageName, launchWithClearTask = true)
        val measurements =
            measureStartup(packageName, StartupMode.COLD) {
                // Simulate a cold start
                scope.killProcess()
                scope.dropKernelPageCache()
                scope.pressHome()
                scope.startActivityAndWait(intent)
            }

        assertEquals(listOf("timeToInitialDisplayMs"), measurements.map { it.name })
    }

    /**
     * Validate that reasonable startup and fully drawn metrics are extracted, either from
     * startActivityAndWait, or from in-app Activity based navigation
     */
    private fun validateStartup_fullyDrawn(delayMs: Long, useInAppNav: Boolean = false) {
        val awaitActivityText: (String) -> UiObject2 = { expectedText ->
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                .wait(Until.findObject(By.text(expectedText)), 3000)!!
        }
        assumeTrue(isAbiSupported())
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)

        val scope = MacrobenchmarkScope(packageName = Packages.TEST, launchWithClearTask = true)
        val launchIntent =
            ConfigurableActivity.createIntent(
                text = "ORIGINAL TEXT",
                reportFullyDrawnDelayMs = delayMs,
            )
        // setup initial Activity if needed
        if (useInAppNav) {
            scope.startActivityAndWait(launchIntent)
            if (delayMs > 0) {
                awaitActivityText(ConfigurableActivity.FULLY_DRAWN_TEXT)
            }
        }

        // measure the activity launch
        val measurements =
            measureStartup(Packages.TEST, StartupMode.WARM) {
                // Simulate a warm start, since it's our own process
                if (useInAppNav) {
                    // click the textview, which triggers an activity launch
                    awaitActivityText(
                            if (delayMs > 0) {
                                ConfigurableActivity.FULLY_DRAWN_TEXT
                            } else {
                                "ORIGINAL TEXT"
                            }
                        )
                        .click()
                } else {
                    scope.pressHome()
                    scope.startActivityAndWait(launchIntent)
                }

                if (useInAppNav) {
                    // in app nav destinations always have different strings to differentiate
                    // vs the first activity's strings to prevent races
                    awaitActivityText(
                        if (delayMs > 0) {
                            ConfigurableActivity.INNER_ACTIVITY_FULLY_DRAWN_TEXT
                        } else {
                            ConfigurableActivity.INNER_ACTIVITY_TEXT
                        }
                    )
                } else if (delayMs > 0) {
                    awaitActivityText(ConfigurableActivity.FULLY_DRAWN_TEXT)
                }
            }

        // validate
        assertEquals(
            setOf("timeToInitialDisplayMs", "timeToFullDisplayMs"),
            measurements.map { it.name }.toSet(),
        )

        val timeToInitialDisplayMs =
            measurements.first { it.name == "timeToInitialDisplayMs" }.data.single()
        val timeToFullDisplayMs =
            measurements.first { it.name == "timeToFullDisplayMs" }.data.single()

        if (delayMs == 0L) {
            // since reportFullyDrawn is dispatched before startup is complete,
            // timeToInitialDisplay and timeToFullDisplay should match
            assertEquals(timeToFullDisplayMs, timeToInitialDisplayMs, 0.0001)
        } else {
            // expect to see at a gap of around delayMs or more between two metrics
            assertTrue(
                timeToFullDisplayMs > timeToInitialDisplayMs,
                "Didn't see full draw delayed after initial display: " +
                    "ttid $timeToInitialDisplayMs, ttfd $timeToFullDisplayMs",
            )
        }
    }

    @LargeTest
    @Test
    @Ignore("b/258335082")
    fun startup_fullyDrawn_immediate() {
        validateStartup_fullyDrawn(delayMs = 0)
    }

    @LargeTest
    @Test
    @Ignore("b/258335082")
    fun startup_fullyDrawn_delayed() {
        validateStartup_fullyDrawn(delayMs = 100)
    }

    @Ignore // b/258335082
    @LargeTest
    @Test
    fun startupInAppNav_immediate() {
        assumeFalse(DeviceInfo.isEmulator) // TODO(b/255754739): address failures on Cuttlefish
        validateStartup_fullyDrawn(delayMs = 0, useInAppNav = true)
    }

    @Ignore // b/258335082
    @LargeTest
    @Test
    fun startupInAppNav_fullyDrawn() {
        assumeFalse(DeviceInfo.isEmulator) // TODO(b/255754739): address failures on Cuttlefish
        validateStartup_fullyDrawn(delayMs = 100, useInAppNav = true)
    }

    private fun getApi32WarmMeasurements(metric: Metric): List<Metric.Measurement> {
        assumeTrue(isAbiSupported())
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        val traceFile = createTempFileFromAsset("api32_startup_warm", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.benchmark.integration.macrobenchmark.target",
                testPackageName = "androidx.benchmark.integration.macrobenchmark.test",
                startupMode = StartupMode.WARM,
                apiLevel = 32,
            )

        metric.configure(captureInfo)
        return TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
            metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
        }
    }

    @MediumTest
    @Test
    fun fixedStartupTraceMetricsReport_fullyDrawnBeforeFirstFrame() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(isAbiSupported())
        val traceFile =
            createTempFileFromAsset(
                prefix = "api24_startup_sameproc_immediatefullydrawn",
                suffix = ".perfetto-trace",
            )
        val metric = StartupTimingMetric()
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = Packages.TEST,
                testPackageName = Packages.TEST,
                startupMode = StartupMode.WARM,
                apiLevel = 24,
            )
        metric.configure(captureInfo)

        val measurements =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("timeToInitialDisplayMs", 178.58525),
                    Metric.Measurement("timeToFullDisplayMs", 178.58525),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @MediumTest
    @Test
    fun fixedStartupTraceMetrics() {
        val measurements = getApi32WarmMeasurements(StartupTimingMetric())

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("timeToInitialDisplayMs", 154.629883),
                    Metric.Measurement("timeToFullDisplayMs", 659.641358),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }

    @SuppressLint("NewApi") // suppressed for StartupTimingLegacyMetric, since data is fixed
    @MediumTest
    @Test
    fun fixedStartupTraceMetrics_legacy() {
        val measurements = getApi32WarmMeasurements(StartupTimingLegacyMetric())

        assertEqualMeasurements(
            expected =
                listOf(
                    Metric.Measurement("startupMs", 156.515747),
                    Metric.Measurement("fullyDrawnMs", 644.613729),
                ),
            observed = measurements,
            threshold = 0.0001,
        )
    }
}

@RequiresApi(23)
internal fun measureStartup(
    packageName: String,
    startupMode: StartupMode,
    measureBlock: () -> Unit,
): List<Metric.Measurement> {
    val metric = StartupTimingMetric()
    val captureInfo =
        Metric.CaptureInfo.forLocalCapture(
            targetPackageName = packageName,
            startupMode = startupMode,
        )
    metric.configure(captureInfo)
    val tracePath =
        PerfettoCaptureWrapper()
            .record(
                fileLabel = packageName,
                config =
                    PerfettoConfig.Benchmark(
                        // note - packageName may be this package, so we convert to set then list to
                        // make unique
                        // and on API 23 and below, we use reflection to trace instead within this
                        // process
                        appTagPackages =
                            if (Build.VERSION.SDK_INT >= 24 && packageName != Packages.TEST) {
                                listOf(packageName, Packages.TEST)
                            } else {
                                listOf(packageName)
                            },
                        useStackSamplingConfig = false,
                    ),
                perfettoSdkConfig = PerfettoSdkConfig(packageName, InitialProcessState.Unknown),
                block = measureBlock,
            )!!

    return TraceProcessor.runSingleSessionServer(tracePath) {
        metric.getMeasurements(captureInfo = captureInfo, traceSession = this)
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
