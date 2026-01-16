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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceEventTest {

    @Test
    internal fun testTraceEventOnePrimaryCategory() {
        val event = TraceEvent()
        val scope = TraceEventScope()
        scope.event = event
        event.primaryCategory = "primary"
        repeat(2) { scope.addCategory("category $it") }
        assertEquals(CATEGORIES_EXPECTED_SIZE, event.categories.size)
        assertEquals(EMPTY, event.categories[0]) // There is a hole
        assertEquals(2, event.lastCategoryIndex)
        event.reset()
        assertEquals(EMPTY, event.categories[1])
        assertEquals(EMPTY, event.categories[2])
    }

    @Test
    internal fun testRecyclingOfTraceEventForCategories() {
        val event = TraceEvent()
        val scope = TraceEventScope()
        scope.event = event
        repeat(6) { scope.addCategory("category $it") }
        // 0 slot is reserved for primaryCategory
        assertEquals(expected = 7, event.categories.size)
        event.reset()
        // Make sure we resize correctly
        assertTrue { event.categories.size == CATEGORIES_EXPECTED_SIZE }
        assertEquals(
            expected = MutableList(size = CATEGORIES_EXPECTED_SIZE) { DEFAULT_STRING },
            actual = event.categories,
        )
    }

    @Test
    internal fun testRecyclingOfTraceEventForCallStackFrames() {
        val event = TraceEvent()
        val scope = TraceEventScope()
        scope.event = event
        repeat(6) {
            scope.addCallStackEntry(name = "call stack $it", sourceFile = null, lineNumber = -1)
        }
        assertEquals(expected = 6, event.frames.size)
        event.reset()
        // Make sure we resize correctly
        assertTrue { event.frames.size == FRAMES_EXPECTED_SIZE }
        assertEquals(
            expected = MutableList(size = FRAMES_EXPECTED_SIZE) { Frame() },
            actual = event.frames,
        )
    }
}
