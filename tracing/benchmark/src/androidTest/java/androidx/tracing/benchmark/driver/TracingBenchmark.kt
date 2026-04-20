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

import android.content.Context
import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.AbstractTraceDriver
import androidx.tracing.PerfettoTracer
import androidx.tracing.TRACE_PACKET_BUFFER_SIZE
import androidx.tracing.benchmark.BASIC_STRING
import androidx.tracing.benchmark.CATEGORY
import androidx.tracing.wire.TraceDriver
import androidx.tracing.wire.TraceSink
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.blackholeSink
import okio.buffer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TracingBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private fun buildTraceDriver(
        sink: TraceSink,
        @Suppress("SameParameterValue") isEnabled: Boolean,
    ): AbstractTraceDriver {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TraceDriver(context = context, sink = sink, isEnabled = isEnabled)
    }

    fun buildInMemorySink(coroutineContext: CoroutineContext): TraceSink {
        return TraceSink(
            sequenceId = 1,
            bufferedSink = blackholeSink().buffer(),
            coroutineContext = coroutineContext,
        )
    }

    private val dispatcher = StandardTestDispatcher()
    private val sink = buildInMemorySink(dispatcher)
    // This test intentionally does not close the AbstractTraceDriver instance. The reason is
    // when we call close() we end up blocking the Thread on which close() was called.
    // Also given the fact that we are using a TestDispatcher here, that blocks forever because
    // there is no good way to advance the TestScheduler by calling advanceUntilIdle().
    // Not calling close() here is okay, given we drain all trace packets before the next
    // measurement loop.
    private val traceDriver = buildTraceDriver(sink, true)
    private val tracer = traceDriver.tracer as PerfettoTracer

    /**
     * This benchmark runs a subset of basic32 in order to measure just the cost of dispatching an
     * event to the sink
     */
    @Test
    fun beginEnd_basic32_writeOnly_withCategory() {
        benchmarkRule.measureRepeated {
            repeat(4) {
                repeat(8) { tracer.trace(category = CATEGORY, name = BASIC_STRING) {} }
                // 32 total events (or 16 begin/end pairs) will dispatch
                // instead, we reset after 8 begin/end pairs so we only measure
                // producer write cost without sending to sink
                runWithMeasurementDisabled { tracer.resetTraceEvents() }
            }
        }
    }

    @Test
    fun beginEndCoroutine_writeOnly() = runTest {
        benchmarkRule.measureRepeated {
            runBlocking {
                repeat(4) {
                    repeat(8) { tracer.traceCoroutine(category = CATEGORY, name = BASIC_STRING) {} }
                    // 32 total events (or 16 begin/end pairs) will dispatch
                    // instead, we reset after 8 begin/end pairs so we only measure
                    // producer write cost without sending to sink
                    runWithMeasurementDisabled { tracer.resetTraceEvents() }
                }
            }
        }
    }

    // This benchmark is a reference benchmark for `beginEndCoroutine_writeOnly`. We are trying to
    // measure the cost of context propagation by comparing it with a ThreadContextElement that
    // does nothing.
    @Test
    fun referenceForBeginEndCoroutine() = runTest {
        benchmarkRule.measureRepeated {
            runBlocking {
                repeat(4) {
                    repeat(8) {
                        val coroutineContext = currentCoroutineContext()
                        withContext(coroutineContext + TestThreadContextElement()) {
                            tracer.trace(category = CATEGORY, name = BASIC_STRING) {
                                // Do nothing
                            }
                        }
                    }
                    // 32 total events (or 16 begin/end pairs) will dispatch
                    // instead, we reset after 8 begin/end pairs so we only measure
                    // producer write cost without sending to sink
                    runWithMeasurementDisabled { tracer.resetTraceEvents() }
                }
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
     * per measurement. Additionally, it measures the cost of serialization.
     */
    @Test
    fun beginEnd_basic32_withSerialization() {
        beginEndBenchmark32(measureSerialization = true)
    }

    private fun beginEndBenchmark32(measureSerialization: Boolean) {
        // we assert this value at runtime and build the number into the method name so it's
        // clear how many begin/ends it is measuring. test needs to be renamed if const changes.
        assertEquals(TRACE_PACKET_BUFFER_SIZE, 32)
        benchmarkRule.measureRepeated {
            repeat(4) {
                repeat(8) { tracer.trace(category = CATEGORY, name = BASIC_STRING) {} }
                if (!measureSerialization) {
                    // 32 total events (or 16 begin/end pairs) will dispatch
                    // instead, we reset after 8 begin/end pairs so we only measure
                    // producer write cost without sending to sink
                    runWithMeasurementDisabled { tracer.resetTraceEvents() }
                }
            }
            if (measureSerialization) {
                dispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    /**
     * This benchmark runs a subset of basic32 in order to measure just the cost of enqueuing a
     * batch to the sink
     */
    @Test
    fun beginEnd_enqueue2() {
        benchmarkRule.measureRepeated {
            tracer.enqueueSingleUnmodifiedEvent()
            tracer.enqueueSingleUnmodifiedEvent()
            runWithMeasurementDisabled { dispatcher.scheduler.advanceUntilIdle() }
        }
    }
}
