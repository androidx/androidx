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
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.benchmark.perfetto.PerfettoHelper
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
@OptIn(ExperimentalMetricApi::class)
class PerfettoTraceProcessorBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val traceFile = createTempFileFromAsset("api32_startup_warm", ".perfetto-trace")

    @Before
    fun setUp() = Assume.assumeTrue(PerfettoHelper.isAbiSupported())

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

    private fun benchmarkWithTrace(block: () -> (Unit)) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(TraceSectionMetric("perfettoTraceProcessor")),
        iterations = 5,
    ) {
        trace("perfettoTraceProcessor", block)
    }

    private fun runComputeStartupMetric() {
        PerfettoTraceProcessor.getJsonMetrics(
            traceFile.absolutePath, "android_startup"
        )
    }

    private fun runSlicesQuery() {
        PerfettoTraceProcessor.rawQuery(
            traceFile.absolutePath, """
                SELECT slice.name, slice.ts, slice.dur, thread_track.id, thread_track.name
                FROM slice
                INNER JOIN thread_track on slice.track_id = thread_track.id
                INNER JOIN thread USING(utid)
                INNER JOIN process USING(upid)
            """.trimIndent()
        )
    }

    private fun runCounterQuery() {
        PerfettoTraceProcessor.rawQuery(
            traceFile.absolutePath, """
                SELECT track.name, counter.value, counter.ts
                FROM track
                JOIN counter ON track.id = counter.track_id
            """.trimIndent()
        )
    }

    private fun runProcessQuery() {
        PerfettoTraceProcessor.rawQuery(
            traceFile.absolutePath, """
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
    }
}
