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

package androidx.tracing

/** Makes it possible to associate debug annotations & categories to a [TraceEvent]. */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
internal class TraceEventScope() : EventMetadata() {
    /** The [TraceEvent] reference being mutated. */
    // Bare mutable fields for performance
    @JvmField internal var event: TraceEvent? = null
    /** The [SliceTrack] that owns the [TraceEvent] instance. */
    // Bare mutable fields for performance
    @JvmField internal var owner: SliceTrack? = null

    override fun addMetadataEntry(name: String, value: Boolean) {
        val entry = nextMetadataEntry()
        entry.name = name
        entry.type = METADATA_TYPE_BOOLEAN
        entry.booleanValue = value
    }

    override fun addMetadataEntry(name: String, value: Long) {
        val entry = nextMetadataEntry()
        entry.name = name
        entry.type = METADATA_TYPE_LONG
        entry.longValue = value
    }

    override fun addMetadataEntry(name: String, value: Double) {
        val entry = nextMetadataEntry()
        entry.name = name
        entry.type = METADATA_TYPE_DOUBLE
        entry.doubleValue = value
    }

    override fun addMetadataEntry(name: String, value: String) {
        val entry = nextMetadataEntry()
        entry.name = name
        entry.type = METADATA_TYPE_STRING
        entry.stringValue = value
    }

    override fun addCallStackEntry(name: String, sourceFile: String?, lineNumber: Int) {
        val frame = nextCallStackEntry()
        frame.name = name
        frame.sourceFile = sourceFile
        frame.lineNumber = lineNumber
    }

    override fun addCorrelationId(id: Long) {
        val event = event!!
        event.correlationId = id
    }

    override fun addCorrelationId(id: String) {
        val event = event!!
        event.correlationIdString = id
    }

    override fun addCategory(name: String) {
        val event = event!!
        event.lastCategoryIndex += 1
        if (event.lastCategoryIndex >= event.categories.size) {
            // Resize if necessary.
            event.categories += DEFAULT_STRING
        }
        event.categories[event.lastCategoryIndex] = name
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nextMetadataEntry(): MetadataEntry {
        val event = event!!
        event.lastMetadataEntryIndex += 1
        if (event.lastMetadataEntryIndex >= event.metadataEntries.size) {
            // Resize if necessary.
            event.metadataEntries += MetadataEntry()
        }
        return event.metadataEntries[event.lastMetadataEntryIndex]
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun nextCallStackEntry(): Frame {
        val event = event!!
        event.lastFrameIndex += 1
        if (event.lastFrameIndex >= event.frames.size) {
            // Resize if necessary.
            event.frames += Frame()
        }
        return event.frames[event.lastFrameIndex]
    }

    @DelicateTracingApi
    override fun dispatchToTraceSink() {
        val owner = owner!!
        owner.dispatchTraceEvent(event = event)
    }
}
