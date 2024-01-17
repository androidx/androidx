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

package androidx.benchmark.integration.macrobenchmark

import androidx.benchmark.Outputs
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoTrace
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.trace
import java.io.File
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SmallTest
@SdkSuppress(minSdkVersion = 29)
@OptIn(
    ExperimentalMetricApi::class,
    ExperimentalPerfettoTraceProcessorApi::class,
    ExperimentalPerfettoCaptureApi::class
)
class PerfettoTraceProcessorBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val traceFile = createTempFileFromAsset("api32_startup_warm", ".perfetto-trace")

    @Before
    fun setUp() = Assume.assumeTrue(PerfettoHelper.isAbiSupported())

    @Test
    fun loadServer() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = measureBlockMetric,
        iterations = 5,
    ) {
        measureBlock {
            PerfettoTraceProcessor.runServer {}
        }
    }

    @Test
    fun singleTrace() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = measureBlockMetric,
        iterations = 5,
    ) {
        measureBlock {
            PerfettoTraceProcessor.runServer {
                loadTrace(PerfettoTrace(traceFile.absolutePath)) {}
            }
        }
    }

    @Test
    fun doubleTrace() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = measureBlockMetric,
        iterations = 5,
    ) {
        measureBlock {
            PerfettoTraceProcessor.runServer {
                loadTrace(PerfettoTrace(traceFile.absolutePath)) {}
                loadTrace(PerfettoTrace(traceFile.absolutePath)) {}
            }
        }
    }

    @Test
    fun computeSingleMetric() = benchmarkWithTrace {
        runComputeStartupMetric()
    }

    @Test
    fun executeSingleSliceQuery() = benchmarkWithTrace {
        runSlicesQuery()
    }

    @Test
    fun executeMultipleQueries() = benchmarkWithTrace {
        runSlicesQuery()
        runCounterQuery()
        runProcessQuery()
    }

    @Test
    fun executeMultipleQueriesAndComputeMetric() = benchmarkWithTrace {
        runComputeStartupMetric()
        runSlicesQuery()
        runCounterQuery()
        runProcessQuery()
    }

    private fun benchmarkWithTrace(block: PerfettoTraceProcessor.Session.() -> Unit) =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = measureBlockMetric,
            iterations = 5,
        ) {
            measureBlock {
                // This will run perfetto trace processor http server on the specified port 10555.
                // Note that this is an arbitrary number and the default cannot be used because
                // the macrobenchmark instance of the server is running at the same time.
                PerfettoTraceProcessor.runSingleSessionServer(
                    absoluteTracePath = traceFile.absolutePath,
                    block = block
                )
            }
        }

    private fun PerfettoTraceProcessor.Session.runComputeStartupMetric() {
        getTraceMetrics("android_startup")
    }

    private fun PerfettoTraceProcessor.Session.runSlicesQuery() {
        query(
            """
                SELECT slice.name, slice.ts, slice.dur, thread_track.id, thread_track.name
                FROM slice
                INNER JOIN thread_track on slice.track_id = thread_track.id
                INNER JOIN thread USING(utid)
                INNER JOIN process USING(upid)
            """.trimIndent()
        )
    }

    private fun PerfettoTraceProcessor.Session.runCounterQuery() {
        query(
            """
                SELECT track.name, counter.value, counter.ts
                FROM track
                JOIN counter ON track.id = counter.track_id
            """.trimIndent()
        )
    }

    private fun PerfettoTraceProcessor.Session.runProcessQuery() {
        query(
            """
                SELECT upid
                FROM counter
                JOIN process_counter_track ON process_counter_track.id = counter.track_id
                WHERE process_counter_track.name = 'mem.swap' AND value > 1000
            """.trimIndent()
        )
    }

    private fun createTempFileFromAsset(prefix: String, suffix: String): File {
        val file = File.createTempFile(prefix, suffix, Outputs.dirUsableByAppAndShell)
        InstrumentationRegistry
            .getInstrumentation()
            .context
            .assets
            .open(prefix + suffix)
            .copyTo(file.outputStream())
        return file
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val SECTION_NAME = "PerfettoTraceProcessorBenchmark"

        /**
         * Measures single call to [measureBlock] function
         */
        private val measureBlockMetric = listOf(
            TraceSectionMetric(
                sectionName = SECTION_NAME,
                targetPackageOnly = false // tracing in test process, not target app
            )
        )

        /**
         * This block is measured by [measureBlockMetric]
         */
        internal inline fun <T> measureBlock(block: () -> T): T = trace(SECTION_NAME) { block() }
    }
}
