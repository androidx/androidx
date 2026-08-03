/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.DeviceInfo
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.InProcessTracingMode
import androidx.benchmark.Outputs
import androidx.benchmark.ShellFile
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.uiAutomator
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * End-to-end test for compose-runtime-tracing verifying that names of Composables show up in a
 * Perfetto trace.
 */
@OptIn(
    ExperimentalMetricApi::class,
    ExperimentalBenchmarkConfigApi::class,
    ExperimentalPerfettoCaptureApi::class,
)
class TrivialPerfettoSdkBenchmark {

    @Before
    fun checkDeviceSupport() {
        assumeTrue(DeviceInfo.expectedToSupportTracingInTests)
    }

    @Test
    fun test_composable_names_present_in_trace() {
        val traceFiles =
            trace(packageName = PACKAGE_NAME) {
                uiAutomator {
                    val intent = Intent(ACTION).apply { setPackage(PACKAGE_NAME) }
                    startActivityIntent(intent)
                }
            }
        assertTrue(traceFiles.isNotEmpty())
        assertEquals(1, traceFiles.size)
        val traceFile = traceFiles.first()
        // Copy the file to a directory usable by the test.
        val copiedPath =
            Outputs.writeFile("temp.pb") { file ->
                val bytes = ShellFile(traceFile).readBytes()
                file.writeBytes(bytes)
            }
        val sliceNames = COMPOSABLE_NAMES.map { name -> "%$PACKAGE_NAME.$name %$FILE_NAME:%" }
        val slices =
            TraceProcessor.runSingleSessionServer(copiedPath) {
                querySlices(*sliceNames.toTypedArray(), packageName = null).map { it.name }
            }
        assertTrue(slices.isNotEmpty())
        assertEquals(3, slices.size)
    }

    internal inline fun trace(packageName: String, block: () -> Unit): List<String> {
        val wrapper = PerfettoCaptureWrapper()
        val config =
            PerfettoCapture.TracingLibraryConfig(
                targetPackage = packageName,
                inProcessTracingMode = InProcessTracingMode.Require,
            )
        val start = wrapper.startInProcessTracing(config = config)
        assertTrue("Unable to start in-process tracing for $packageName", start.isSuccess())
        block()
        val traceFiles = wrapper.stopInProcessTracing(config)
        return traceFiles
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.compose.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.compose.integration.macrobenchmark.target.TRIVIAL_TRACING_ACTIVITY"

        private const val FILE_NAME = "TrivialTracingActivity.kt"

        private val COMPOSABLE_NAMES =
            listOf(
                "Foo_BBC27C8E_13A7_4A5F_A735_AFDC433F54C3",
                "Bar_4888EA32_ABC5_4550_BA78_1247FEC1AAC9",
                "Baz_609801AB_F5A9_47C3_94蛸5_2E82542F21B8",
            )
    }
}
