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

internal class TraceAttributesImpl : TraceAttributes() {
    /** The [TraceEvent] reference being mutated. */
    // Bare mutable fields for performance
    @JvmField internal var event: TraceEvent? = null
    /** The [SliceTrack] that owns the [TraceEvent] instance. */
    // Bare mutable fields for performance
    @JvmField internal var owner: SliceTrack? = null

    override fun addAttribute(name: String, value: Long) {
        val entry = nextAttributeEntry()
        entry.name = name
        entry.longValue = value
    }

    override fun addAttribute(name: String, value: String) {
        val entry = nextAttributeEntry()
        entry.name = name
        entry.stringValue = value
    }

    @DelicateTracingApi
    override fun dispatchToTraceSink() {
        val owner = owner!!
        owner.dispatchTraceEvent(event = event)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nextAttributeEntry(): AttributeEntry {
        val event = event!!
        event.lastAttributeIndex += 1
        if (event.lastAttributeIndex >= event.attributes.size) {
            // Resize if necessary.
            event.attributes += AttributeEntry()
        }
        return event.attributes[event.lastAttributeIndex]
    }
}
