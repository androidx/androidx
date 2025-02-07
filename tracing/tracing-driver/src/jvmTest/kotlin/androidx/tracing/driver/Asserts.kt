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

import kotlin.test.assertNotNull
import perfetto.protos.MutableTrackEvent

internal fun List<PooledTracePacket>.trackEventPacket(
    name: String,
    type: MutableTrackEvent.Type? = null
): PooledTracePacket? {
    return find { packet ->
        var result = packet.tracePacket.track_event?.name == name
        if (type != null) {
            val sameType = type == packet.tracePacket.track_event?.type
            result = result and sameType
        }
        result
    }
}

internal fun List<PooledTracePacket>.assertTraceSection(name: String) {
    val begin = trackEventPacket(name = name, type = MutableTrackEvent.Type.TYPE_SLICE_BEGIN)
    val end = trackEventPacket(name = name, type = MutableTrackEvent.Type.TYPE_SLICE_END)
    assertNotNull(begin) {
        "Cannot find a track event of type ${MutableTrackEvent.Type.TYPE_SLICE_BEGIN} for $name"
    }
    assertNotNull(end) {
        "Cannot find a track event of type ${MutableTrackEvent.Type.TYPE_SLICE_END} for $name"
    }
}
