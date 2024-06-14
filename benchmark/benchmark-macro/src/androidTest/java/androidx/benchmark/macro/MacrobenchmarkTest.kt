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
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.ExperimentalConfig
import androidx.benchmark.json.BenchmarkData
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.tracing.trace
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
                    measureBlock = {}
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
                    measureBlock = {}
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
                }
            )
        assertEquals(1, result.profilerOutputs!!.size)
        assertEquals(
            result.profilerOutputs!!.single().type,
            BenchmarkData.TestResult.ProfilerOutput.Type.PerfettoTrace
        )
    }

    enum class Block {
        Setup,
        Measure
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
            }
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
                    Block.Measure
                ),
                opOrder
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
                    measureBlock = { trace(TRACE_LABEL) { Thread.sleep(2) } }
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

    companion object {
        const val TRACE_LABEL = "MacrobencharkTestTraceLabel"
    }
}
