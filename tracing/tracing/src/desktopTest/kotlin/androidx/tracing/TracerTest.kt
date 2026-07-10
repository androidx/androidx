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
}
