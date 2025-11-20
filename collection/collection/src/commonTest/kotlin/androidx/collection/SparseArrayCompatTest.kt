/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SparseArrayCompatTest {

    @Test
    fun getOrDefaultPrefersStoredValue() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertEquals("1", map.get(1, "2"))
    }

    @Test
    fun getOrDefaultUsesDefaultWhenAbsent() {
        val map = SparseArrayCompat<String>()
        assertEquals("1", map.get(1, "1"))
    }

    @Test
    fun getOrDefaultReturnsNullWhenNullStored() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertNull(map.get(1, "1"))
    }

    @Test
    fun getOrDefaultDoesNotPersistDefault() {
        val map = SparseArrayCompat<String>()
        map.get(1, "1")
        assertFalse(map.containsKey(1))
    }

    @Test
    fun putIfAbsentDoesNotOverwriteStoredValue() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        map.putIfAbsent(1, "2")
        assertEquals("1", map.get(1))
    }

    @Test
    fun putIfAbsentReturnsStoredValue() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertEquals("1", map.putIfAbsent(1, "2"))
    }

    @Test
    fun putIfAbsentStoresValueWhenAbsent() {
        val map = SparseArrayCompat<String>()
        map.putIfAbsent(1, "2")
        assertEquals("2", map.get(1))
    }

    @Test
    fun putIfAbsentReturnsNullWhenAbsent() {
        val map = SparseArrayCompat<String>()
        assertNull(map.putIfAbsent(1, "2"))
    }

    @Test
    fun replaceWhenAbsentDoesNotStore() {
        val map = SparseArrayCompat<String>()
        assertNull(map.replace(1, "1"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun replaceStoresAndReturnsOldValue() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertEquals("1", map.replace(1, "2"))
        assertEquals("2", map.get(1))
    }

    @Test
    fun replaceStoresAndReturnsNullWhenMappedToNull() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertNull(map.replace(1, "1"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueKeyAbsent() {
        val map = SparseArrayCompat<String>()
        assertFalse(map.replace(1, "1", "2"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun replaceValueMismatchDoesNotReplace() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertFalse(map.replace(1, "2", "3"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueMismatchNullDoesNotReplace() {
        val map = SparseArrayCompat<String?>()
        map.put(1, "1")
        assertFalse(map.replace(1, null, "2"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueMatchReplaces() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertTrue(map.replace(1, "1", "2"))
        assertEquals("2", map.get(1))
    }

    @Test
    fun replaceNullValueMismatchDoesNotReplace() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertFalse(map.replace(1, "1", "2"))
        assertNull(map.get(1))
    }

    @Test
    fun replaceNullValueMatchRemoves() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertTrue(map.replace(1, null, "1"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun removeValueKeyAbsent() {
        val map = SparseArrayCompat<String>()
        assertFalse(map.remove(1, "1"))
    }

    @Test
    fun removeValueMismatchDoesNotRemove() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertFalse(map.remove(1, "2"))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeValueMismatchNullDoesNotRemove() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertFalse(map.remove(1, null))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeValueMatchRemoves() {
        val map = SparseArrayCompat<String>()
        map.put(1, "1")
        assertTrue(map.remove(1, "1"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun removeNullValueMismatchDoesNotRemove() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertFalse(map.remove(1, "2"))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeNullValueMatchRemoves() {
        val map = SparseArrayCompat<String?>()
        map.put(1, null)
        assertTrue(map.remove(1, null))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun isEmpty() {
        val sparseArrayCompat = SparseArrayCompat<String>()
        assertTrue(sparseArrayCompat.isEmpty()) // Newly created SparseArrayCompat should be empty

        // Adding elements should change state from empty to not empty.
        for (i in 0 until 5) {
            sparseArrayCompat.put(i, i.toString())
            assertFalse(sparseArrayCompat.isEmpty())
        }
        sparseArrayCompat.clear()
        assertTrue(sparseArrayCompat.isEmpty()) // A cleared SparseArrayCompat should be empty.

        val key1 = 1
        val key2 = 2
        val value1 = "some value"
        val value2 = "some other value"
        sparseArrayCompat.append(key1, value1)
        assertFalse(sparseArrayCompat.isEmpty()) // has 1 element.
        sparseArrayCompat.append(key2, value2)
        assertFalse(sparseArrayCompat.isEmpty()) // has 2 elements.
        assertFalse(sparseArrayCompat.isEmpty()) // consecutive calls should be OK.

        sparseArrayCompat.remove(key1)
        assertFalse(sparseArrayCompat.isEmpty()) // has 1 element.
        sparseArrayCompat.remove(key2)
        assertTrue(sparseArrayCompat.isEmpty())
    }

    @Test
    fun containsKey() {
        val array = SparseArrayCompat<String>()
        array.put(1, "one")

        assertTrue(array.containsKey(1))
        assertFalse(array.containsKey(2))
    }

    @Test
    fun containsValue() {
        val array = SparseArrayCompat<String>()
        array.put(1, "one")

        assertTrue(array.containsValue("one"))
        assertFalse(array.containsValue("two"))
    }

    @Test
    fun putAll() {
        val dest = SparseArrayCompat<String>()
        dest.put(1, "one")
        dest.put(3, "three")

        val source = SparseArrayCompat<String>()
        source.put(1, "uno")
        source.put(2, "dos")

        dest.putAll(source)
        assertEquals(3, dest.size())
        assertEquals("uno", dest.get(1))
        assertEquals("dos", dest.get(2))
        assertEquals("three", dest.get(3))
    }

    @Test
    fun putAllVariance() {
        val dest = SparseArrayCompat<Any>()
        dest.put(1, 1L)

        val source = SparseArrayCompat<String>()
        dest.put(2, "two")

        dest.putAll(source)
        assertEquals(2, dest.size())
        assertEquals(1L, dest.get(1))
        assertEquals("two", dest.get(2))
    }

    @Test
    fun keyAt_outOfBounds() {
        val source = SparseArrayCompat<String>(10)
        assertEquals(0, source.size())

        assertFailsWith<IndexOutOfBoundsException> { source.keyAt(10000) }
    }

    @Test
    fun keyAt_outOfBoundsWithinAllocatedRange() {
        val source = SparseArrayCompat<String>(10)
        assertEquals(0, source.size())

        assertFailsWith<IndexOutOfBoundsException> { source.keyAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { source.keyAt(9) }
    }

    @Test
    fun valueAt_outOfBounds() {
        val source = SparseArrayCompat<String>(10)
        assertEquals(0, source.size())

        assertFailsWith<IndexOutOfBoundsException> { source.valueAt(10000) }
    }

    @Test
    fun valueAt_outOfBoundsWithinAllocatedRange() {
        val source = SparseArrayCompat<String>(10)
        assertEquals(0, source.size())

        assertFailsWith<IndexOutOfBoundsException> { source.valueAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { source.valueAt(9) }
    }
}
