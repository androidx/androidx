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

package androidx.compose.runtime.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeMultiValueMapTest {

    @Test
    fun testInsertRemovePrimitives() {
        val safeMultiValueMap = SafeMultiValueMap<String, Int>()

        assertTrue(safeMultiValueMap.isEmpty())

        safeMultiValueMap.add("A", 0)
        safeMultiValueMap.add("A", 1)
        safeMultiValueMap.add("A", 2)
        safeMultiValueMap.add("B", -1)

        assertFalse(safeMultiValueMap.isEmpty())
        assertEquals(setOf(0, 1, 2, -1), safeMultiValueMap.values().asList().toSet())

        assertEquals(0, safeMultiValueMap.removeFirst("A", Int.MAX_VALUE))
        assertEquals(1, safeMultiValueMap.removeFirst("A", Int.MAX_VALUE))
        assertEquals(2, safeMultiValueMap.removeFirst("A", Int.MAX_VALUE))
        assertEquals(-1, safeMultiValueMap.removeFirst("B", Int.MAX_VALUE))

        assertTrue(safeMultiValueMap.isEmpty())
    }

    @Test
    fun testRemoveLast() {
        val safeMultiValueMap = SafeMultiValueMap<String, Any?>()

        assertTrue(safeMultiValueMap.isEmpty())

        safeMultiValueMap.add("A", null)
        safeMultiValueMap.add("A", mutableListOf(1, 2, 3))
        safeMultiValueMap.add("A", Unit)
        safeMultiValueMap.add("B", -1)

        assertFalse(safeMultiValueMap.isEmpty())

        assertEquals(Unit, safeMultiValueMap.removeLast("A", "<MISSING>"))
        assertEquals(mutableListOf(1, 2, 3), safeMultiValueMap.removeLast("A", "<MISSING>"))
        assertEquals(null, safeMultiValueMap.removeLast("A", "<MISSING>"))
        assertEquals(-1, safeMultiValueMap.removeFirst("B", "<MISSING>"))

        assertTrue(safeMultiValueMap.isEmpty())
    }

    @Test
    fun testInsertRemoveNullable() {
        val safeMultiValueMap = SafeMultiValueMap<String, Any?>()

        assertTrue(safeMultiValueMap.isEmpty())

        safeMultiValueMap.add("A", null)
        safeMultiValueMap.add("A", null)
        safeMultiValueMap.add("A", Unit)
        safeMultiValueMap.add("A", null)
        safeMultiValueMap.add("B", "100")

        assertFalse(safeMultiValueMap.isEmpty())

        assertEquals(null, safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(null, safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(Unit, safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(null, safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals("100", safeMultiValueMap.removeFirst("B", Int.MAX_VALUE))

        assertTrue(safeMultiValueMap.isEmpty())
    }

    @Test
    fun testInsertRemoveMutableList() {
        val safeMultiValueMap = SafeMultiValueMap<String, Any?>()

        assertTrue(safeMultiValueMap.isEmpty())

        safeMultiValueMap.add("A", mutableListOf(1, 2, 3, 4))
        safeMultiValueMap.add("A", mutableListOf(5, 6, 7, 8))
        safeMultiValueMap.add("A", mutableListOf(9, 10))
        safeMultiValueMap.add("B", null)

        assertFalse(safeMultiValueMap.isEmpty())

        assertEquals(mutableListOf(1, 2, 3, 4), safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(mutableListOf(5, 6, 7, 8), safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(mutableListOf(9, 10), safeMultiValueMap.removeFirst("A", "<MISSING>"))
        assertEquals(null, safeMultiValueMap.removeFirst("B", "<MISSING>"))

        assertTrue(safeMultiValueMap.isEmpty())
    }
}
