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
import androidx.annotation.RequiresApi
import androidx.benchmark.DeviceInfo
import androidx.benchmark.DeviceMirroring
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.MemoryProfilingConfig
import androidx.benchmark.Outputs
import androidx.benchmark.json.BenchmarkData
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.runServer
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.tracing.trace
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalMacrobenchmarkApi::class, ExperimentalBenchmarkConfigApi::class)
class MacrobenchmarkTest {

    @Before
    fun setUp() {
        assumeFalse(DeviceInfo.isEmulator)
    }

    @Test
    fun macrobenchmarkWithStartupMode_emptyMetricList() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                macrobenchmarkWithStartupMode(
                    uniqueName = "uniqueName", // ignored, uniqueness not important
                    className = "className",
                    testName = "testName",
                    packageName = "com.ignored",
                    metrics = emptyList(), // invalid
                    compilationMode = CompilationMode.Ignore(),
                    iterations = 1,
                    startupMode = null,
                    experimentalConfig = null,
                    setupBlock = {},
                    measureBlock = {},
                )
            }
        assertTrue(exception.message!!.contains("Empty list of metrics"))
    }

    @Test
    fun macrobenchmarkWithStartupMode_iterations() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                macrobenchmarkWithStartupMode(
                    uniqueName = "uniqueName", // ignored, uniqueness not important
                    className = "className",
                    testName = "testName",
                    packageName = "com.ignored",
                    metrics = listOf(FrameTimingMetric()),
                    compilationMode = CompilationMode.Ignore(),
                    iterations = 0, // invalid
                    startupMode = null,
                    experimentalConfig = null,
                    setupBlock = {},
                    measureBlock = {},
                )
            }
        assertTrue(exception.message!!.contains("Require iterations > 0"))
    }

    @Test
    fun macrobenchmarkWithStartupMode_noMethodTrace() {
        val result =
            macrobenchmarkWithStartupMode(
                uniqueName = "uniqueName", // ignored, uniqueness not important
                className = "className",
                testName = "testName",
                packageName = Packages.TARGET,
                metrics = listOf(StartupTimingMetric()),
                compilationMode = CompilationMode.Ignore(),
                iterations = 1,
                startupMode = StartupMode.COLD,
                experimentalConfig = null,
                setupBlock = {},
                measureBlock = {
                    startActivityAndWait(
                        Intent(
                            "androidx.benchmark.integration.macrobenchmark.target" +
                                ".TRIVIAL_STARTUP_ACTIVITY"
                        )
                    )
                },
            )
        assertEquals(1, result.profilerOutputs!!.size)
        assertEquals(
            result.profilerOutputs!!.single().type,
            BenchmarkData.TestResult.ProfilerOutput.Type.PerfettoTrace,
        )
    }

    enum class Block {
        Setup,
        Measure,
    }

    @RequiresApi(29)
    @OptIn(ExperimentalMetricApi::class)
    fun validateCallbackBehavior(startupMode: StartupMode?) {
        val opOrder = mutableListOf<Block>()
        val setupIterations = mutableListOf<Int?>()
        val measurementIterations = mutableListOf<Int?>()

        assumeTrue(PerfettoHelper.isAbiSupported())
        macrobenchmarkWithStartupMode(
            uniqueName = "MacrobenchmarkTest#validateCallbackBehavior",
            className = "MacrobenchmarkTest",
            testName = "validateCallbackBehavior",
            packageName = Packages.TARGET,
            // disable targetPackageOnly filter, since this process emits the event
            metrics = listOf(TraceSectionMetric(TRACE_LABEL, targetPackageOnly = false)),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 2,
            startupMode = startupMode,
            experimentalConfig = null,
            setupBlock = {
                opOrder += Block.Setup
                setupIterations += iteration
                assertEquals(Packages.TARGET, packageName)
            },
            measureBlock = {
                trace(TRACE_LABEL) {
                    opOrder += Block.Measure
                    measurementIterations += iteration
                }
                assertEquals(Packages.TARGET, packageName)
            },
        )
        if (startupMode == StartupMode.WARM || startupMode == StartupMode.HOT) {
            // measure block is executed an extra time, before first
            // iteration, to warm up process/activity
            assertEquals(
                listOf(
                    Block.Setup,
                    Block.Measure,
                    Block.Setup,
                    Block.Measure,
                    Block.Setup,
                    Block.Measure,
                ),
                opOrder,
            )
            assertEquals(listOf(null, 0, 1), setupIterations)
            assertEquals(listOf(null, 0, 1), measurementIterations)
        } else {
            assertEquals(listOf(Block.Setup, Block.Measure, Block.Setup, Block.Measure), opOrder)
            assertEquals(listOf<Int?>(0, 1), setupIterations)
            assertEquals(listOf<Int?>(0, 1), measurementIterations)
        }
    }

    @LargeTest
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun callbackBehavior_null() = validateCallbackBehavior(null)

    @LargeTest
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun callbackBehavior_cold() = validateCallbackBehavior(StartupMode.COLD)

    @LargeTest
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun callbackBehavior_warm() = validateCallbackBehavior(StartupMode.WARM)

    @LargeTest
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun callbackBehavior_hot() = validateCallbackBehavior(StartupMode.HOT)

    @SuppressLint("BanThreadSleep") // need non-zero duration to assert sum, regardless of clock
    private fun validateSlicesCustomConfig(includeMacroAppTag: Boolean) {
        val atraceApps =
            if (includeMacroAppTag) {
                listOf(Packages.TEST)
            } else {
                emptyList()
            }
        val measurements =
            macrobenchmarkWithStartupMode(
                    uniqueName = "MacrobenchmarkTest#validateSlicesCustomConfig",
                    className = "MacrobenchmarkTest",
                    testName = "validateCallbackBehavior",
                    packageName = Packages.TARGET,
                    // disable targetPackageOnly filter, since this process emits the event
                    metrics = listOf(TraceSectionMetric(TRACE_LABEL, targetPackageOnly = false)),
                    compilationMode = CompilationMode.DEFAULT,
                    iterations = 3,
                    startupMode = null,
                    experimentalConfig = ExperimentalConfig(PerfettoConfig.MinimalTest(atraceApps)),
                    setupBlock = {},
                    measureBlock = { trace(TRACE_LABEL) { Thread.sleep(2) } },
                )
                .metrics[TRACE_LABEL + "SumMs"]!!
                .runs

        assertEquals(3, measurements.size)

        if (includeMacroAppTag) {
            assertTrue(measurements.all { it > 0.0 })
        } else {
            assertEquals(listOf(0.0, 0.0, 0.0), measurements)
        }
    }

    @LargeTest
    @Test
    fun customConfig_thisProcess() = validateSlicesCustomConfig(includeMacroAppTag = true)

    @LargeTest
    @Test
    fun customConfig_noProcess() = validateSlicesCustomConfig(includeMacroAppTag = false)

    @LargeTest
    @Test
    fun macrobenchmark_deviceMirroring_active_throwsError() =
        try {
            DeviceMirroring.isAndroidStudioDeviceMirroringActiveOverride = true
            val exception =
                assertFailsWith<AssertionError> {
                    macrobenchmarkWithStartupMode(
                        uniqueName = "uniqueName", // ignored, uniqueness not important
                        className = "className",
                        testName = "testName",
                        packageName = Packages.TARGET,
                        metrics = listOf(StartupTimingMetric()),
                        compilationMode = CompilationMode.Ignore(),
                        iterations = 1,
                        startupMode = StartupMode.COLD,
                        experimentalConfig = null,
                        setupBlock = {},
                        measureBlock = {},
                    )
                }
            assertTrue(exception.message!!.contains("Android Studio Device Mirroring is active"))
        } finally {
            DeviceMirroring.isAndroidStudioDeviceMirroringActiveOverride = null
        }

    @LargeTest
    @Test
    fun macrobenchmark_deviceMirroring_inactive_runsBenchmark() =
        try {
            DeviceMirroring.isAndroidStudioDeviceMirroringActiveOverride = false
            val result =
                macrobenchmarkWithStartupMode(
                    uniqueName = "uniqueName", // ignored, uniqueness not important
                    className = "className",
                    testName = "testName",
                    packageName = Packages.TARGET,
                    metrics = listOf(TraceSectionMetric(TRACE_LABEL, targetPackageOnly = false)),
                    compilationMode = CompilationMode.Ignore(),
                    iterations = 1,
                    startupMode = StartupMode.COLD,
                    experimentalConfig = null,
                    setupBlock = {},
                    measureBlock = {},
                )
            assertEquals(1, result.metrics[TRACE_LABEL + "SumMs"]!!.runs.size)
        } finally {
            DeviceMirroring.isAndroidStudioDeviceMirroringActiveOverride = null
        }

    @LargeTest
    @Test
    fun macrobenchmark_shellAccessDenied_throwsError() =
        try {
            DeviceInfo.canShellAccessAppFilesOverride = false
            val exception =
                assertFailsWith<AssertionError> {
                    macrobenchmarkWithStartupMode(
                        uniqueName = "uniqueName",
                        className = "className",
                        testName = "testName",
                        packageName = Packages.TARGET,
                        metrics = listOf(StartupTimingMetric()),
                        compilationMode = CompilationMode.Ignore(),
                        iterations = 1,
                        startupMode = StartupMode.COLD,
                        experimentalConfig = null,
                        setupBlock = {},
                        measureBlock = {},
                    )
                }
            assertTrue(exception.message!!.contains("Shell user cannot access app files"))
        } finally {
            DeviceInfo.canShellAccessAppFilesOverride = null
        }

    @LargeTest
    @Test
    fun macrobenchmark_shellAccessGranted_runsBenchmark() =
        try {
            DeviceInfo.canShellAccessAppFilesOverride = true
            val result =
                macrobenchmarkWithStartupMode(
                    uniqueName = "uniqueName",
                    className = "className",
                    testName = "testName",
                    packageName = Packages.TARGET,
                    metrics = listOf(TraceSectionMetric(TRACE_LABEL, targetPackageOnly = false)),
                    compilationMode = CompilationMode.Ignore(),
                    iterations = 1,
                    startupMode = StartupMode.COLD,
                    experimentalConfig = null,
                    setupBlock = {},
                    measureBlock = {},
                )
            assertEquals(1, result.metrics[TRACE_LABEL + "SumMs"]!!.runs.size)
        } finally {
            DeviceInfo.canShellAccessAppFilesOverride = null
        }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun macrobenchmark_memoryProfiling_recordsMemoryTrace() {
        try {
            DeviceInfo.canShellAccessAppFilesOverride = true
            val result =
                macrobenchmarkWithStartupMode(
                    uniqueName = "memoryProfilingTest",
                    className = "className",
                    testName = "testName",
                    packageName = Packages.TARGET,
                    metrics = listOf(TraceSectionMetric(TRACE_LABEL, targetPackageOnly = false)),
                    compilationMode = CompilationMode.Ignore(),
                    iterations = 1,
                    startupMode = StartupMode.COLD,
                    experimentalConfig =
                        ExperimentalConfig(
                            memoryProfilingConfig =
                                MemoryProfilingConfig(
                                    isSampleArtHeapEnabled = true,
                                    isSampleNativeHeapEnabled = true,
                                )
                        ),
                    setupBlock = {},
                    measureBlock = {},
                )
            val memoryProfilerOutputs =
                result.profilerOutputs?.filter { it.label.contains("Memory Profiling") }
            assertNotNull(memoryProfilerOutputs)
            assertEquals(
                1,
                memoryProfilerOutputs.size,
                "Expected exactly one memory profiling trace output",
            )
            val memoryProfilerOutput = memoryProfilerOutputs.single()
            val memoryTracePath = memoryProfilerOutput.filename
            assertTrue(
                Regex("""^memoryProfilingTest_iter\d{3}-memoryProfiling_.*\.perfetto-trace$""")
                    .matches(memoryTracePath),
                "Expected memory trace filename matching '<uniqueName>_iter<iter>-memoryProfiling_<date>.perfetto-trace', but got: $memoryTracePath",
            )
            val memoryTraceFile =
                File(Outputs.outputDirectory, memoryTracePath).let {
                    if (it.exists()) it else File(Outputs.dirUsableByAppAndShell, memoryTracePath)
                }
            assertTrue(
                memoryTraceFile.exists(),
                "Expected memory trace file to exist: ${memoryTraceFile.absolutePath}",
            )
            TraceProcessor.runSingleSessionServer(memoryTraceFile.absolutePath) {
                val artCount =
                    query(
                            "SELECT count(*) as count FROM heap_profile_allocation WHERE heap_name = 'com.android.art'"
                        )
                        .first()
                        .long("count")
                val nativeCount =
                    query(
                            "SELECT count(*) as count FROM heap_profile_allocation WHERE heap_name = 'libc.malloc'"
                        )
                        .first()
                        .long("count")
                assertTrue(
                    artCount >= 0,
                    "Expected non-negative ART heap allocation count, got $artCount",
                )
                assertTrue(
                    nativeCount >= 0,
                    "Expected non-negative native heap allocation count, got $nativeCount",
                )
            }
        } finally {
            DeviceInfo.canShellAccessAppFilesOverride = null
        }
    }

    @LargeTest
    @Test
    fun runPhase_setsIterationAndFileLabelPerIteration() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        TraceProcessor.runServer {
            val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = false)
            val setupIterations = mutableListOf<Int?>()
            val executionIterations = mutableListOf<Int>()
            val fileLabels = mutableListOf<String>()

            val results =
                runPhase(
                    uniqueName = "myTestPhase",
                    packageName = Packages.TARGET,
                    macrobenchmarkPackageName = Packages.TEST,
                    iterations = 3,
                    startupMode = null,
                    scope = scope,
                    profiler = null,
                    metrics = emptyList(),
                    experimentalConfig = null,
                    tracingLibraryConfig = null,
                    setupBlock = { setupIterations.add(iteration) },
                    measureBlock = {
                        executionIterations.add(scope.iteration ?: -1)
                        fileLabels.add(scope.fileLabel)
                    },
                )

            assertEquals(listOf<Int?>(0, 1, 2), setupIterations)
            assertEquals(listOf(0, 1, 2), executionIterations)
            assertEquals(
                listOf("myTestPhase_iter000", "myTestPhase_iter001", "myTestPhase_iter002"),
                fileLabels,
            )
            assertEquals(3, results.size)
            results.forEach { iterResult ->
                assertTrue(File(checkNotNull(iterResult.tracePath)).exists())
            }
        }
    }

    @LargeTest
    @Test
    fun runPhase_appliesTraceSuffixToFileLabel() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        TraceProcessor.runServer {
            val scope = MacrobenchmarkScope(Packages.TARGET, launchWithClearTask = false)
            val fileLabels = mutableListOf<String>()

            val results =
                runPhase(
                    uniqueName = "myTestPhase",
                    packageName = Packages.TARGET,
                    macrobenchmarkPackageName = Packages.TEST,
                    iterations = 1,
                    startupMode = null,
                    scope = scope,
                    profiler = null,
                    metrics = emptyList(),
                    experimentalConfig = null,
                    tracingLibraryConfig = null,
                    traceSuffix = "-memoryProfiling",
                    setupBlock = {},
                    measureBlock = { fileLabels.add(scope.fileLabel) },
                )

            assertEquals(listOf("myTestPhase_iter000-memoryProfiling"), fileLabels)
            assertEquals(1, results.size)
            assertTrue(File(checkNotNull(results.single().tracePath)).exists())
        }
    }

    companion object {
        const val TRACE_LABEL = "MacrobencharkTestTraceLabel"
    }
}
