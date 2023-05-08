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

package androidx.benchmark

import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoTrace
import androidx.benchmark.perfetto.perfettoConfig
import androidx.benchmark.perfetto.validateAndEncode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPerfettoCaptureApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class PerfettoTraceTest {

    @Test
    fun record_basic() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        var perfettoTrace: PerfettoTrace? = null
        PerfettoTrace.record(
            fileLabel = "testTrace",
            traceCallback = { trace ->
                perfettoTrace = trace
            }
        ) {
            // noop
        }
        assertNotNull(perfettoTrace)
        assert(perfettoTrace!!.path.matches(Regex(".*/testTrace_[0-9-]+.perfetto-trace"))) {
            "$perfettoTrace didn't match!"
        }
    }

    private fun verifyRecordSuccess(
        config: PerfettoConfig
    ) {
        var perfettoTrace: PerfettoTrace? = null
        val label = "successTrace${config.javaClass.simpleName}"
        PerfettoTrace.record(
            fileLabel = label,
            config = config,
            traceCallback = { trace ->
                perfettoTrace = trace
            }
        ) {
            // noop
        }
        assertNotNull(perfettoTrace)
        assert(perfettoTrace!!.path.matches(Regex(".*/${label}_[0-9-]+.perfetto-trace"))) {
            "$perfettoTrace didn't match!"
        }
    }

    private fun verifyRecordFails(
        config: PerfettoConfig
    ) {
        var perfettoTrace: PerfettoTrace? = null
        val exception = assertFailsWith<IllegalStateException> {
            PerfettoTrace.record(
                fileLabel = "failTrace",
                config = config,
                traceCallback = { trace ->
                    perfettoTrace = trace
                }
            ) {
                // noop
            }
        }
        assertTrue(exception.message!!.contains("Perfetto unexpected exit code"))
        assertNull(perfettoTrace)
    }

    @Test
    fun record_invalidText() = verifyRecordFails(PerfettoConfig.Text("INVALID"))

    @Test
    fun record_invalidBinary() = verifyRecordFails(PerfettoConfig.Binary(byteArrayOf(1, 0, 1)))

    @Test
    fun record_validText() = verifyRecordSuccess(PerfettoConfig.Text("""
        # basic config generated from https://ui.perfetto.dev/#!/record
        buffers: {
            size_kb: 63488
            fill_policy: RING_BUFFER
        }
        buffers: {
            size_kb: 2048
            fill_policy: RING_BUFFER
        }
        data_sources: {
            config {
                name: "linux.ftrace"
                ftrace_config {
                    ftrace_events: "ftrace/print"
                    atrace_categories: "am"
                    atrace_categories: "dalvik"
                    atrace_categories: "gfx"
                    atrace_categories: "view"
                    atrace_categories: "wm"
                }
            }
        }
        duration_ms: 10000
        flush_period_ms: 30000
        incremental_state_config {
            clear_period_ms: 5000
        }
    """.trimIndent()))

    @Test
    fun record_validBinary() = verifyRecordSuccess(
        PerfettoConfig.Binary(
            perfettoConfig(
                atraceApps = listOf(
                    InstrumentationRegistry.getInstrumentation().targetContext.packageName
                )
            ).validateAndEncode()
        )
    )

    @Test
    fun record_reentrant() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        var perfettoTrace: PerfettoTrace? = null
        PerfettoTrace.record(
            fileLabel = "outer",
            traceCallback = { trace ->
                perfettoTrace = trace
            }
        ) {
            // tracing while tracing should fail
            assertFailsWith<IllegalStateException> {
                PerfettoTrace.record(
                    fileLabel = "inner",
                    traceCallback = { _ ->
                        fail("inner trace should not complete / record")
                    }
                ) {
                    // noop
                }
            }
        }
        assertNotNull(perfettoTrace)
    }
}
