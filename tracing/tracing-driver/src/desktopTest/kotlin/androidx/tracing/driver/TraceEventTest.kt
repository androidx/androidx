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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceEventTest {
    @Test
    internal fun testRecyclingOfTraceEventForCategories() {
        val event = TraceEvent()
        val scope = TraceEventScope()
        scope.event = event
        repeat(6) { scope.addCategory("category $it") }

        assertTrue { event.categories.size == 6 }
        event.reset()
        // Make sure we resize correctly
        assertTrue { event.categories.size == CATEGORIES_EXPECTED_SIZE }
        assertEquals(
            expected = MutableList(size = CATEGORIES_EXPECTED_SIZE) { DEFAULT_STRING },
            actual = event.categories,
        )
    }
}
