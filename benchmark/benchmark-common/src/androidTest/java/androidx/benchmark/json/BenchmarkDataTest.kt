/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.json

import android.os.Build
import androidx.benchmark.ResultWriter
import androidx.benchmark.validateArtMainlineVersion
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BenchmarkDataTest {
    @Test
    fun validateLoadLegacy_micro() {
        val legacyString = readTextAsset("micro-legacy-trivialbench.json")

        val benchmarkData = ResultWriter.adapter.fromJson(legacyString)!!
        assertEquals("mokey_go32", benchmarkData.context.build.device)
        assertEquals(2, benchmarkData.benchmarks.size)

        benchmarkData.benchmarks[0].apply {
            assertEquals("increment", name)
            assertEquals(50, metrics["timeNs"]!!.runs.size)
            assertEquals(5, metrics["allocationCount"]!!.runs.size)
        }
        benchmarkData.benchmarks[1].apply {
            assertEquals("nothing", name)
            assertEquals(50, metrics["timeNs"]!!.runs.size)
            assertEquals(5, metrics["allocationCount"]!!.runs.size)
        }
    }

    @Test
    fun validateLoadLegacy_macro() {
        val legacyString = readTextAsset("macro-legacy-trivialscrollbench.json")

        val benchmarkData = ResultWriter.adapter.fromJson(legacyString)!!
        assertEquals("mokey_go32", benchmarkData.context.build.device)
        assertEquals(2, benchmarkData.benchmarks.size)

        benchmarkData.benchmarks[0].apply {
            assertEquals(
                "startup[startup=COLD,compilation=Partial(" +
                    "baselineProfile=UseIfAvailable,iterations=0)]",
                name
            )
            assertEquals(
                mapOf( // Note: parsing error in source data!
                    "compilation" to "Partial(baselineProfile=UseIfAvailable",
                    "startup" to "COLD",
                    "iterations" to "0)"
                ),
                params
            )
            assertEquals(2, metrics["methodsJitCompiled"]!!.runs.size)
            assertEquals(2, metrics["timeToInitialDisplayMs"]!!.runs.size)
        }
        benchmarkData.benchmarks[1].apply {
            assertEquals("startup[startup=COLD,compilation=None]", name)
            assertEquals(mapOf("compilation" to "None", "startup" to "COLD"), params)
            assertEquals(2, metrics["methodsJitCompiled"]!!.runs.size)
            assertEquals(2, metrics["timeToInitialDisplayMs"]!!.runs.size)
        }
    }

    @Test
    fun distinctLabels() {
        assertFailsWith<IllegalArgumentException> {
            BenchmarkData.TestResult(
                name = "name",
                className = "className",
                totalRunTimeNs = 1,
                metrics = emptyList(),
                warmupIterations = 1,
                repeatIterations = 1,
                thermalThrottleSleepSeconds = 0,
                profilerOutputs =
                    listOf(
                        BenchmarkData.TestResult.ProfilerOutput(
                            BenchmarkData.TestResult.ProfilerOutput.Type.MethodTrace,
                            "duplicate label",
                            "filename1.trace"
                        ),
                        BenchmarkData.TestResult.ProfilerOutput(
                            BenchmarkData.TestResult.ProfilerOutput.Type.MethodTrace,
                            "duplicate label",
                            "filename2.trace"
                        )
                    )
            )
        }
    }

    @Test
    fun osCodenameAbbreviated() {
        BenchmarkData.Context().osCodenameAbbreviated.run {
            assertEquals(1, length, "expected 1 char codename, observed $this")
            assertContains('A'..'Z', this[0])
            if (Build.VERSION.SDK_INT != 30) {
                // check we're not incorrectly parsing R from "REL"
                assertNotEquals("R", this)
            }
            if (Build.VERSION.SDK_INT !in listOf(23, 35)) {
                // check we're not incorrectly parsing M from "MAIN"
                assertNotEquals("M", this)
            }
        }
    }

    @Test
    fun artMainlineVersion() {
        validateArtMainlineVersion(artMainlineVersion = BenchmarkData.Context().artMainlineVersion)
    }

    private fun readTextAsset(filename: String): String {
        return InstrumentationRegistry.getInstrumentation()
            .context
            .assets
            .open(filename)
            .reader()
            .use { it.readText() }
    }
}
