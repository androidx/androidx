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

package androidx.tracing.driver

/** [Track] representing a `Thread` in the specified [ProcessTrack]. */
public open class ThreadTrack(
    /** The thread id. */
    internal val id: Int,
    /** The name of the thread. */
    internal val name: String,
    /** The process track that the thread belongs to. */
    internal val process: ProcessTrack,
) : SliceTrack(context = process.context, uuid = monotonicId()) {

    init {
        emitTraceEvent(immediateDispatch = true) { packet ->
            packet.setPreamble(
                TrackDescriptor(
                    name = name,
                    uuid = uuid,
                    parentUuid = INVALID_LONG,
                    pid = process.id,
                    tid = id,
                    type = TRACK_DESCRIPTOR_TYPE_THREAD,
                )
            )
        }
    }
}

// An empty thread track when tracing is disabled

private const val EMPTY_THREAD_ID = -1
private const val EMPTY_THREAD_NAME = "Empty Thread"

internal class EmptyThreadTrack(process: EmptyProcessTrack) :
    ThreadTrack(id = EMPTY_THREAD_ID, name = EMPTY_THREAD_NAME, process = process) {}
