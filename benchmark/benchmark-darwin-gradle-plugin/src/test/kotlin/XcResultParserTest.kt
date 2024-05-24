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

import androidx.benchmark.darwin.gradle.skia.Metrics
import androidx.benchmark.darwin.gradle.xcode.GsonHelpers
import androidx.benchmark.darwin.gradle.xcode.XcResultParser
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XcResultParserTest {
    @Test
    fun parseXcResultFileTest() {
        val operatingSystem = System.getProperty("os.name")
        // Only run this test on an `Mac OS X` machine.
        assumeTrue(operatingSystem.contains("Mac", ignoreCase = true))
        val xcResultFile = testData("sample-xcode.xcresult")
        val parser =
            XcResultParser(xcResultFile) { args ->
                val builder = ProcessBuilder(*args.toTypedArray())
                val process = builder.start()
                val resultCode = process.waitFor()
                require(resultCode == 0) {
                    "Process terminated unexpectedly (${args.joinToString(separator = " ")})"
                }
                process.inputStream.use { it.reader().readText() }
            }
        val (record, summaries) = parser.parseResults()
        // Usually corresponds to the size of the test suite
        // In the case of KMP benchmarks, this is always 1 per module.
        assertThat(record.actions.testReferences().size).isEqualTo(1)
        assertThat(record.actions.isSuccessful()).isTrue()
        // Metrics typically correspond to the number of tests
        assertThat(record.metrics.size()).isEqualTo(2)
        assertThat(summaries.isNotEmpty()).isTrue()
        val metrics = Metrics.buildMetrics(record, summaries, referenceSha = null)
        val json = GsonHelpers.gsonBuilder().setPrettyPrinting().create().toJson(metrics)
        println()
        println(json)
        println()
        assertThat(json).isNotEmpty()
    }
}
