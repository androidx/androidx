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

/** [Track] representing a numerical value that can change over the duration of the trace. */
public open class CounterTrack(
    /** The name of the counter track */
    private val name: String,
    /** The parent track the counter belongs to. */
    private val parent: Track,
) : Track(context = parent.context, uuid = monotonicId()) {
    internal val packetLock = Any()

    init {
        synchronized(packetLock) {
            emitTraceEvent(immediateDispatch = true) { event ->
                event.setPreamble(
                    TrackDescriptor(
                        name = name,
                        uuid = uuid,
                        parentUuid = parent.uuid,
                        type = TRACK_DESCRIPTOR_TYPE_COUNTER,
                        pid = INVALID_INT,
                        tid = INVALID_INT,
                    )
                )
            }
        }
    }

    public fun setCounter(value: Long) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitTraceEvent { packet -> packet.setCounterLong(uuid, value) }
            }
        }
    }

    public fun setCounter(value: Double) {
        if (context.isEnabled) {
            synchronized(packetLock) {
                emitTraceEvent { packet -> packet.setCounterDouble(uuid, value) }
            }
        }
    }
}

// An empty counter track when tracing is disabled

private const val EMPTY_COUNTER_NAME = "Empty Counter"

internal class EmptyCounterTrack(process: EmptyProcessTrack) :
    CounterTrack(name = EMPTY_COUNTER_NAME, parent = process) {}
