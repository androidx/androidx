/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/** [Track] representing a `Thread` in the specified [ProcessTrack]. */
@RestrictTo(Scope.LIBRARY_GROUP)
public open class ThreadTrack(
    /** The thread id. */
    public val id: Long,
    /** The name of the thread. */
    public val name: String,
    /** The process track that the thread belongs to. */
    public val process: ProcessTrack,
) : SliceTrack(context = process.context, uuid = monotonicId()) {

    override fun preamblePacket(): TraceEvent? {
        val event = obtainTraceEvent()
        event?.setPreamble(
            TrackDescriptor(
                name = name,
                uuid = uuid,
                parentUuid = process.uuid,
                pid = process.id,
                tid = id,
                type = TRACK_DESCRIPTOR_TYPE_THREAD,
            )
        )
        return event
    }

    override fun endSection() {
        assertThreadIdWhenNotOptimized()
        super.endSection()
    }

    // Note: This method is optimized away by R8.
    // Making this a public method to avoid name mangling by the Kotlin compiler so that
    // we can write a corresponding `-assumenosideeffects` rule.
    public fun assertThreadIdWhenNotOptimized() {
        // Ideally we do this check in SliceTrack. But, SliceTrack is not thread id aware.
        // Therefore, we are doing this in ThreadTrack.
        require(id == currentThreadId()) {
            """
                Invariant violation. Current thread id (${currentThreadId()} does not match
                expected $id. This means that there might be a race condition in the code
                where begin and end sections are being called on separate threads.
            """
                .trimIndent()
        }
    }
}

// An empty thread track when tracing is disabled

private const val EMPTY_THREAD_ID = -1L
private const val EMPTY_THREAD_NAME = "Empty Thread"

internal class EmptyThreadTrack(process: EmptyProcessTrack) :
    ThreadTrack(id = EMPTY_THREAD_ID, name = EMPTY_THREAD_NAME, process = process)
