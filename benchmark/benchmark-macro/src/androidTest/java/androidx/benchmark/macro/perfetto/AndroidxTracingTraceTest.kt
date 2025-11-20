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

package androidx.benchmark.macro.perfetto

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.macro.FileLinkingRule
import androidx.benchmark.macro.Packages
import androidx.benchmark.macro.runSingleSessionServer
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.benchmark.traceprocessor.toSlices
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.Trace
import androidx.tracing.trace
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for androidx.tracing.Trace, which validate actual trace content
 *
 * These can't be defined in the androidx.tracing library, as Trace capture / validation APIs are
 * only available to the benchmark group.
 */
@RunWith(AndroidJUnit4::class)
class AndroidxTracingTraceTest {
    @get:Rule val linkRule = FileLinkingRule()

    @Before
    @After
    fun cleanup() {
        PerfettoHelper.cleanupPerfettoState()
    }

    @LargeTest
    @Test
    fun captureAndValidateTrace() {
        // Our API 23 emulators seem to be misconfigured b/438214932
        assumeTrue(!isEmulator || SDK_INT != 23)
        assumeTrue(isAbiSupported())

        val traceFilePath = linkRule.createReportedTracePath(Packages.TEST)
        val perfettoCapture = PerfettoCapture()

        perfettoCapture.start(
            PerfettoConfig.Benchmark(
                appTagPackages = listOf(Packages.TEST),
                useStackSamplingConfig = false,
            )
        )

        assertTrue(
            Trace.isEnabled(),
            "In-process tracing should be enabled immediately after trace capture is started",
        )

        repeat(20) {
            "$PREFIX$it"
                .also { label ->
                    // actual test content. This is done in the middle of the other sections
                    // to isolate it from trace truncation issues
                    if (it == 10) {
                        Trace.setCounter("${PREFIX}counter", 1)
                        Trace.beginSection("${PREFIX}beginSection")
                        Trace.beginAsyncSection("${PREFIX}beginAsyncSection", 9827)
                        Thread.sleep(50)
                        Trace.setCounter("${PREFIX}counter", 0)
                        Trace.endSection()
                        Trace.endAsyncSection("${PREFIX}beginAsyncSection", 9827)
                    }

                    // trace sections before and after actual test content, to look for problems in
                    // front/back trace truncation. If these sections are missing, it's most likely
                    // issues in trace capture
                    trace(label) { Thread.sleep(50) }
                }
        }

        perfettoCapture.stop(traceFilePath, null)

        val queryResult =
            TraceProcessor.runSingleSessionServer(traceFilePath) { query(query = QUERY) }

        val matchingSlices = queryResult.toSlices()
        assertEquals(
            List(10) { "$PREFIX$it" } +
                listOf(
                    "${PREFIX}counter1.0",
                    "${PREFIX}beginSection",
                    "${PREFIX}beginAsyncSection",
                    "${PREFIX}counter0.0",
                ) +
                List(10) { "$PREFIX${it + 10}" },
            matchingSlices.map { it.name },
        )
        matchingSlices.forEach {
            if (it.name.startsWith("${PREFIX}counter")) {
                assertEquals(0L, it.dur) // counter has no length
            } else {
                assertTrue(it.dur > 30_000_000) // should be at least 30ms
            }
        }
    }

    companion object {
        const val PREFIX = "AndroidxTracingTraceTest_"

        const val QUERY =
            """
            ------ select all relevant standard slices
            SELECT
                slice.name as name,
                slice.ts as ts,
                slice.dur as dur
            FROM slice
                INNER JOIN thread_track on slice.track_id = thread_track.id
                INNER JOIN thread USING(utid)
                INNER JOIN process USING(upid)
            WHERE
                slice.name LIKE "$PREFIX%" AND
                process.name LIKE "androidx.benchmark.macro.test"
            UNION
            ------ add in async slices
            SELECT
                slice.name as name,
                slice.ts as ts,
                slice.dur as dur
            FROM slice
                INNER JOIN process_track on slice.track_id = process_track.id
                INNER JOIN process USING(upid)
            WHERE
                slice.name LIKE "$PREFIX%" AND
                process.name LIKE "androidx.benchmark.macro.test"
            UNION
            ------ add in the counter values, with value prepended to name
            SELECT
                counter_track.name || counter.value as name,
                counter.ts as ts,
                0 as dur
            FROM counter
                INNER JOIN counter_track on counter.track_id = counter_track.id
            WHERE
                counter_track.name LIKE "${PREFIX}counter"
            ------ order the whole thing by timestamp
            ORDER BY
                ts
        """
    }
}
