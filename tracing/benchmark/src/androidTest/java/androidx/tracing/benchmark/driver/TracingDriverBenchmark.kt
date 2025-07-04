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

package androidx.tracing.benchmark.driver

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.benchmark.BASIC_STRING
import androidx.tracing.benchmark.PROCESS_NAME
import androidx.tracing.driver.TRACE_PACKET_BUFFER_SIZE
import androidx.tracing.driver.TraceContext
import androidx.tracing.driver.TraceSink
import androidx.tracing.driver.wire.WireTraceSink
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import okio.blackholeSink
import okio.buffer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TracingDriverBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private fun buildTraceContext(sink: TraceSink, isEnabled: Boolean): TraceContext {
        return TraceContext(sink = sink, isEnabled = isEnabled)
    }

    fun buildInMemorySink(coroutineContext: CoroutineContext): TraceSink {
        return WireTraceSink(
            sequenceId = 1,
            bufferedSink = blackholeSink().buffer(),
            coroutineContext = coroutineContext,
        )
    }

    private val dispatcher = StandardTestDispatcher()
    private val sink = buildInMemorySink(dispatcher)
    // This test intentionally does not close the TraceContext instance. The reason is
    // when we call close() we end up blocking the Thread on which close() was called.
    // Also given the fact that we are using a TestDispatcher here, that blocks forever because
    // there is no good way to advance the TestScheduler by calling advanceUntilIdle().
    // Not calling close() here is okay, given we drain all trace packets before the next
    // measurement loop.
    private val traceContext = buildTraceContext(sink, true)
    private val process = traceContext.getOrCreateProcessTrack(id = 10, name = PROCESS_NAME)

    /**
     * This benchmark runs a subset of basic32 in order to measure just the cost of dispatching an
     * event to the sink
     */
    @Test
    fun beginEnd_basic32_writeOnly() {
        benchmarkRule.measureRepeated {
            repeat(4) {
                repeat(8) { process.trace(BASIC_STRING) {} }
                // 32 total events (or 16 begin/end pairs) will dispatch
                // instead, we reset after 8 begin/end pairs so we only measure
                // producer write cost without sending to sink
                process.resetFillCount()
            }
        }
    }

    /**
     * This benchmark runs the measurement 32 times to ensure emitting the packet is captured once
     * per measurement.
     */
    @Test
    fun beginEnd_basic32() {
        beginEndBenchmark32(measureSerialization = false)
    }

    /**
     * This benchmark runs the measurement 32 times to ensure emitting the packet is captured once
     * per measurement. Additionally it measures the cost of serialization.
     */
    @Test
    fun beginEnd_basic32_withSerialization() {
        beginEndBenchmark32(measureSerialization = true)
    }

    private fun beginEndBenchmark32(measureSerialization: Boolean) {
        // we assert this value at runtime and build the number into the method name so it's
        // clear how many begin/ends it is measuring. test needs to be renamed if const changes.
        assertEquals(32, TRACE_PACKET_BUFFER_SIZE)
        benchmarkRule.measureRepeated {
            repeat(32) { process.trace(BASIC_STRING) {} }
            // The benchmark measurement loop creates packets extremely quickly. To avoid
            // running OOM (when the consumer can't keep up) we wait for the packets to flush.
            // Note that we attempt to wait a consistent amount of time to ensure consistent
            // measurements.
            if (!measureSerialization) {
                runWithMeasurementDisabled { dispatcher.scheduler.advanceUntilIdle() }
            } else {
                dispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    /**
     * This benchmark runs a subset of basic32 in order to measure just the cost of enqeuing a batch
     * to the sink
     */
    @Test
    fun beginEnd_enqueue2() {
        benchmarkRule.measureRepeated {
            process.enqueueSingleUnmodifiedEvent()
            process.enqueueSingleUnmodifiedEvent()
            runWithMeasurementDisabled { dispatcher.scheduler.advanceUntilIdle() }
        }
    }
}
