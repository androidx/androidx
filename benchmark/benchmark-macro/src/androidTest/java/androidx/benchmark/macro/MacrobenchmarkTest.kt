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

import androidx.annotation.RequiresApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.tracing.trace
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@SmallTest
class MacrobenchmarkTest {
    @Test
    fun macrobenchmarkWithStartupMode_emptyMetricList() {
        val exception = assertFailsWith<IllegalArgumentException> {
            macrobenchmarkWithStartupMode(
                uniqueName = "uniqueName", // ignored, uniqueness not important
                className = "className",
                testName = "testName",
                packageName = "com.ignored",
                metrics = emptyList(), // invalid
                compilationMode = CompilationMode.noop,
                iterations = 1,
                startupMode = null,
                setupBlock = {},
                measureBlock = {}
            )
        }
        assertTrue(exception.message!!.contains("Empty list of metrics"))
    }

    @Test
    fun macrobenchmarkWithStartupMode_iterations() {
        val exception = assertFailsWith<IllegalArgumentException> {
            macrobenchmarkWithStartupMode(
                uniqueName = "uniqueName", // ignored, uniqueness not important
                className = "className",
                testName = "testName",
                packageName = "com.ignored",
                metrics = listOf(FrameTimingMetric()),
                compilationMode = CompilationMode.noop,
                iterations = 0, // invalid
                startupMode = null,
                setupBlock = {},
                measureBlock = {}
            )
        }
        assertTrue(exception.message!!.contains("Require iterations > 0"))
    }

    enum class Block { Setup, Measure }

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
            metrics = listOf(TraceSectionMetric(TRACE_LABEL)),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 2,
            startupMode = startupMode,
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

    companion object {
        const val TRACE_LABEL = "MacrobencharkTestTraceLabel"
    }
}