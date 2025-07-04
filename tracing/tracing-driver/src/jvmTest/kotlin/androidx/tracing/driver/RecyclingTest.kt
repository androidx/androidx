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

package androidx.tracing.driver

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecyclingTest {
    private val sink = NoOpSink()

    private val context: TraceContext = TraceContext(sink = sink, isEnabled = true, isDebug = true)

    fun TraceContext.validateEachTrackHasOnePoolable() {
        validateTrackPools { track -> assertEquals(1, track.pool.poolableCount()) }
    }

    @Test
    internal fun testProcessTrackEvents() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val thread = process.getOrCreateThreadTrack(1, "thread")
            thread.trace("section") {}
        }
        assertTrue(context.isDebug)
        context.validateEachTrackHasOnePoolable()
    }

    @Test
    internal fun testProcessTrackFlows() = runTest {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val thread = process.getOrCreateThreadTrack(1, "thread")
            thread.traceFlow("section") {}
        }
        assertTrue(context.isDebug)
        context.validateEachTrackHasOnePoolable()
    }

    @Test
    internal fun testProcessTrackCounter() = runTest {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val counter = process.getOrCreateCounterTrack("counter")
            counter.setCounter(0L)
        }
        assertTrue(context.isDebug)
        context.validateEachTrackHasOnePoolable()
    }
}
