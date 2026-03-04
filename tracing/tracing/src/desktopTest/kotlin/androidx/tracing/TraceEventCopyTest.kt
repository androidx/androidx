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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TraceEventCopyTest {
    @Test
    fun testCopyFrom() {
        val source = TraceEvent()
        source.type = TRACE_EVENT_TYPE_BEGIN
        source.trackUuid = 123L
        source.timestamp = 456L
        source.name = "test-event"
        source.counterLongValue = 789L
        source.correlationId = 101L
        source.correlationIdString = "cor-id"
        source.primaryCategory = "cat-prim"

        // Metadata
        source.lastMetadataEntryIndex = 0
        source.metadataEntries[0].name = "meta1"
        source.metadataEntries[0].type = METADATA_TYPE_STRING
        source.metadataEntries[0].stringValue = "val1"

        // Categories
        source.lastCategoryIndex = 0
        source.categories[0] = "cat-1"

        // Frames
        source.lastFrameIndex = 0
        source.frames[0].name = "func1"
        source.frames[0].sourceFile = "file1"
        source.frames[0].lineNumber = 10

        val dest = TraceEvent()
        dest.copyFrom(source)

        assertEquals(source.type, dest.type)
        assertEquals(source.trackUuid, dest.trackUuid)
        assertEquals(source.timestamp, dest.timestamp)
        assertEquals(source.name, dest.name)
        assertEquals(source.counterLongValue, dest.counterLongValue)
        assertEquals(source.correlationId, dest.correlationId)
        assertEquals(source.correlationIdString, dest.correlationIdString)
        assertEquals(source.primaryCategory, dest.primaryCategory)

        assertEquals(source.lastMetadataEntryIndex, dest.lastMetadataEntryIndex)
        assertEquals(source.metadataEntries[0].name, dest.metadataEntries[0].name)
        assertEquals(source.metadataEntries[0].stringValue, dest.metadataEntries[0].stringValue)

        assertEquals(source.lastCategoryIndex, dest.lastCategoryIndex)
        assertEquals(source.categories[0], dest.categories[0])

        assertEquals(source.lastFrameIndex, dest.lastFrameIndex)
        assertEquals(source.frames[0].name, dest.frames[0].name)

        // Modify source and check dest is unchanged (for list contents)
        source.metadataEntries[0].stringValue = "val2"
        assertEquals("val1", dest.metadataEntries[0].stringValue)

        source.categories[0] = "cat-2"
        assertEquals("cat-1", dest.categories[0])

        source.frames[0].name = "func2"
        assertEquals("func1", dest.frames[0].name)
    }

    @Test
    fun testCopyFromResizesLists() {
        val source = TraceEvent()
        // Add more entries than expected size to force resize in dest if dest is small
        val count = METADATA_ENTRIES_EXPECTED_SIZE + 2
        source.lastMetadataEntryIndex = count - 1
        while (source.metadataEntries.size < count) {
            source.metadataEntries.add(MetadataEntry())
        }
        for (i in 0 until count) {
            source.metadataEntries[i].name = "meta-$i"
        }

        val dest = TraceEvent()
        // dest has default size (METADATA_ENTRIES_EXPECTED_SIZE)

        dest.copyFrom(source)

        assertEquals(source.lastMetadataEntryIndex, dest.lastMetadataEntryIndex)
        assertEquals(count, dest.metadataEntries.size)
        assertEquals("meta-${count - 1}", dest.metadataEntries[count - 1].name)
    }

    @Test
    fun testCopyFromDoesNotLeakOldData() {
        val source = TraceEvent()
        source.lastMetadataEntryIndex = 0
        source.metadataEntries[0].name = "meta-1"

        source.lastCategoryIndex = 0
        source.categories[0] = "cat-1"

        source.lastFrameIndex = 0
        source.frames[0].name = "frame-1"

        val dest = TraceEvent()
        dest.lastMetadataEntryIndex = 1
        while (dest.metadataEntries.size <= 1) dest.metadataEntries.add(MetadataEntry())
        dest.metadataEntries[1].name = "leaked-meta-name"
        dest.metadataEntries[1].stringValue = "leaked-meta-value"

        dest.lastCategoryIndex = 1
        while (dest.categories.size <= 1) dest.categories.add(DEFAULT_STRING)
        dest.categories[1] = "leaked-cat"

        dest.lastFrameIndex = 1
        while (dest.frames.size <= 1) dest.frames.add(Frame())
        dest.frames[1].name = "leaked-frame"
        dest.frames[1].sourceFile = "leaked-file"

        dest.copyFrom(source)

        assertEquals(0, dest.lastMetadataEntryIndex)
        assertNull(dest.metadataEntries[1].name, "Metadata name not cleared")
        assertEquals(EMPTY, dest.metadataEntries[1].stringValue, "Metadata stringValue not cleared")

        assertEquals(0, dest.lastCategoryIndex)
        assertEquals(DEFAULT_STRING, dest.categories[1], "Category not cleared")

        assertEquals(0, dest.lastFrameIndex)
        assertNull(dest.frames[1].name, "Frame name not cleared")
        assertNull(dest.frames[1].sourceFile, "Frame sourceFile not cleared")
    }
}
