/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.abbenchmarking.macrobenchmarking

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.text.trimIndent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MacroBenchmarkResultFilesTest {

    @get:Rule val tempFolder = TemporaryFolder()
    private lateinit var tempDir: File

    private val originalErr = System.err
    private lateinit var errContent: ByteArrayOutputStream

    @Before
    fun setUpStreams() {
        errContent = ByteArrayOutputStream()
        System.setErr(PrintStream(errContent))
        tempDir = tempFolder.root
    }

    @After
    fun restoreStreams() {
        System.setErr(originalErr)
    }

    private val sampleOutputJson =
        """
        {
            "context": {
                "build": { "device": "tokay", "model": "Pixel 9", "version": { "sdk": 36 } }
            },
            "benchmarks": [
                {
                    "name": "startup",
                    "className": "com.example.benchmark.ExampleStartupBenchmark",
                    "metrics": {
                        "frameCount": {
                            "runs": [ 1.0, 1.0, 1.0, 1.0, 1.0 ]
                        },
                        "timeToInitialDisplayMs": {
                            "runs": [ 573.02, 487.69, 485.15, 514.24, 554.33 ]
                        }
                    },
                    "sampledMetrics": {
                        "frameDurationCpuMs": {
                            "runs": [ [ 265.24 ], [ 256.02 ], [ 255.95 ], [ 264.82 ], [ 263.43 ] ]
                        },
                        "frameOverrunMs": {
                            "runs": [ [ 420.11 ], [ 328.18 ], [ 318.14 ], [ 343.78 ], [ 413.82 ] ]
                        }
                    }
                }
            ]
        }
    """
            .trimIndent()

    @Test
    fun parseBenchmarkRuns_withValidFile() {
        // GIVEN a single, valid benchmark JSON file
        val jsonFile = File(tempDir, "benchmark.json").apply { writeText(sampleOutputJson) }

        // WHEN parseBenchmarkRuns is called
        val result = parseBenchmarkRuns(listOf(jsonFile))

        // THEN the result map should be correctly structured
        assertThat(result).hasSize(1)
        assertThat(result.keys).containsExactly("startup")

        // AND the "startup" benchmark data should be parsed correctly
        val startupMetrics = result["startup"]!!
        assertThat(startupMetrics).hasSize(4) // 2 from metrics, 2 from sampledMetrics
        assertThat(startupMetrics.keys)
            .containsExactly(
                "frameCount",
                "timeToInitialDisplayMs",
                "frameDurationCpuMs",
                "frameOverrunMs",
            )

        // AND regular metrics should be parsed
        assertThat(startupMetrics["timeToInitialDisplayMs"])
            .containsExactly(573.02, 487.69, 485.15, 514.24, 554.33)
            .inOrder()

        // AND sampled metrics should be flattened and parsed
        assertThat(startupMetrics["frameDurationCpuMs"])
            .containsExactly(265.24, 256.02, 255.95, 264.82, 263.43)
            .inOrder()
    }

    @Test
    fun parseBenchmarkRuns_withInvalidJson_returnsEmptyMap() {
        // GIVEN a file with invalid JSON content
        val invalidJsonFile =
            File(tempDir, "invalid.json").apply { writeText("{ not a valid json }") }

        // WHEN parseBenchmarkRuns is called
        val result = parseBenchmarkRuns(listOf(invalidJsonFile))

        // THEN the result should be an empty map
        assertThat(result).isEmpty()
    }

    @Test
    fun parseBenchmarkRuns_withEmptyFile_returnsEmptyMap() {
        // GIVEN an empty file
        val emptyFile = File(tempDir, "empty.json").apply { createNewFile() }

        // WHEN parseBenchmarkRuns is called
        val result = parseBenchmarkRuns(listOf(emptyFile))

        // THEN the result should be an empty map
        assertThat(result).isEmpty()
    }

    @Test
    fun getBenchmarkDataFromOutputFile_withValidcsv() {
        // GIVEN a valid CSV file with multiple benchmarks and metrics
        val csvContent =
            """
            benchmark_name,metric_name,metric_value
            startup,timeToInitialDisplayMs,100.1
            startup,timeToInitialDisplayMs,102.3
            startup,frameDurationCpuMs,5.5
            startup,frameDurationCpuMs,8.1
        """
                .trimIndent()
        val csvFile = File(tempDir, "results.csv").apply { writeText(csvContent) }

        // WHEN the function is called
        val result = getBenchmarkDataFromOutputFile(csvFile)

        // THEN the data should be parsed into the correct nested map structure
        assertThat(result).hasSize(1)
        assertThat(result.keys).containsExactly("startup")

        // AND startup metrics should be correct
        val startupMetrics = result["startup"]!!
        assertThat(startupMetrics).hasSize(2)
        assertThat(startupMetrics["timeToInitialDisplayMs"]).isEqualTo(doubleArrayOf(100.1, 102.3))
        assertThat(startupMetrics["frameDurationCpuMs"]).isEqualTo(doubleArrayOf(5.5, 8.1))
    }

    @Test
    fun getBenchmarkDataFromOutputFil_withNonexistentFile() {
        // GIVEN a file path that does not exist
        val nonExistentFile = File(tempDir, "non_existent.csv")

        // WHEN the function is called
        val result = getBenchmarkDataFromOutputFile(nonExistentFile)

        // THEN it should return an empty map
        assertThat(result).isEmpty()
        // AND print an error message
        assertThat(errContent.toString()).contains("Error: File not found")
    }

    @Test
    fun getBenchmarkDataFromOutputFile_withEmptyFile() {
        // GIVEN an empty file
        val emptyFile = File(tempDir, "empty.csv").apply { createNewFile() }

        // WHEN the function is called
        val result = getBenchmarkDataFromOutputFile(emptyFile)

        // THEN it should return an empty map
        assertThat(result).isEmpty()
    }

    @Test
    fun getBenchmarkDataFromOutputFile_withMalformedMetricValue() {
        // GIVEN a CSV file with a non-numeric metric_value value
        val csvContent =
            """
            benchmark_name,metric_name,metric_value
            startup,timeToInitialDisplayMs,100.1
            startup,timeToInitialDisplayMs,not_a_number
            startup,timeToInitialDisplayMs,102.3
        """
                .trimIndent()
        val csvFile = File(tempDir, "malformed.csv").apply { writeText(csvContent) }

        // WHEN the function is called
        val result = getBenchmarkDataFromOutputFile(csvFile)

        // THEN it should skip the malformed row and parse the valid ones
        assertThat(result).hasSize(1)
        val startupMetrics = result["startup"]!!
        assertThat(startupMetrics["timeToInitialDisplayMs"]).isEqualTo(doubleArrayOf(100.1, 102.3))
        // AND it should print a warning
        assertThat(errContent.toString()).contains("Warning: Could not parse metric value")
    }
}
