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

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_COUNTER: Int = 1
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_THREAD: Int = 2
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TRACK_DESCRIPTOR_TYPE_PROCESS: Int = 3

/** Low level representation of a Track, written once to the Trace. */
public class TrackDescriptor(
    /** Display name of the Track in the trace. */
    public var name: String,
    /**
     * Unique ID of the Track - each corresponding [TraceEvent] in the trace will set
     * [TraceEvent.trackUuid] to this value.
     */
    public var uuid: Long,
    /**
     * If set, this Track will be nested inside of the parent track (for example, when a
     * [ThreadTrack] is a child of a [ProcessTrack]).
     */
    public var parentUuid: Long,
    /**
     * One of [TRACK_DESCRIPTOR_TYPE_THREAD], [TRACK_DESCRIPTOR_TYPE_COUNTER],
     * [TRACK_DESCRIPTOR_TYPE_PROCESS]
     */
    public var type: Int,
    /** If type == [TRACK_DESCRIPTOR_TYPE_PROCESS], represents the PID of the process. */
    public var pid: Int,
    /** If type == [TRACK_DESCRIPTOR_TYPE_THREAD], represents the TID of the thread. */
    public var tid: Int,
)
