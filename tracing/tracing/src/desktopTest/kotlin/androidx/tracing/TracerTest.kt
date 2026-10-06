/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.tracing

import androidx.tracing.Tracer.Companion.getStubTracer
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals

class TracerTest {
    @Test
    internal fun testStubTracerEmitsNoTracePackets() {
        val tracer = getStubTracer() as PerfettoTracer
        tracer.trace(category = "category", name = "name") {
            // Do nothing
        }
        // Force a flush
        tracer.context.flush()
        val sink = tracer.context.sink as EmptyTraceSink
        // There should be no actual events
        assertEquals(0, sink.enqueues.get())
    }

    @Test
    internal fun testFlushWhenFillCountIsZeroDoesNotEnqueue() {
        val context =
            TraceContext(
                sink = EmptyTraceSink,
                isGloballyEnabled = true,
                isCategoryEnabled = { it != META_TRACE_CATEGORY },
                isDebug = true,
            )
        // Instantiating PerfettoTracer initializes context.process
        PerfettoTracer(context = context, categoryEnabled = { true })
        val sink = context.sink as EmptyTraceSink
        sink.enqueues.set(0)
        // Flush when nothing has been traced; since fillCount == 0, nothing should be enqueued
        context.flush()
        assertEquals(0, sink.enqueues.get())
        context.close()
    }

    @Test
    internal fun testConcurrentTraceAndFlush() {
        val context =
            TraceContext(
                sink = EmptyTraceSink,
                isGloballyEnabled = true,
                isCategoryEnabled = { it != META_TRACE_CATEGORY },
                isDebug = true,
            )
        val tracer: Tracer = PerfettoTracer(context = context, categoryEnabled = { true })
        val counter = tracer.counter("test", "counter")

        val iterations = 500
        val isTracingDone = AtomicBoolean(false)
        val workerThreads = mutableListOf<Thread>()
        val flusherThreads = mutableListOf<Thread>()
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())

        // Worker threads doing tracing and counter updates
        repeat(4) {
            workerThreads += Thread {
                try {
                    repeat(iterations) {
                        tracer.trace(category = "test", name = "section") {
                            counter.setValue(1L)
                        }
                    }
                } catch (t: Throwable) {
                    errors += t
                }
            }
        }

        // Flusher threads calling flush() concurrently
        repeat(2) {
            flusherThreads += Thread {
                try {
                    while (!isTracingDone.get()) {
                        context.flush()
                    }
                } catch (t: Throwable) {
                    errors += t
                }
            }
        }

        workerThreads.forEach { it.start() }
        flusherThreads.forEach { it.start() }

        workerThreads.forEach { it.join() }
        isTracingDone.set(true)
        flusherThreads.forEach { it.join() }

        assertEquals(emptyList(), errors)
        context.close()
    }
}
