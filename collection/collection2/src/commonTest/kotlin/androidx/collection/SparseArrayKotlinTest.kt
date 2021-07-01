/*
 * Copyright 2020 The Android Open Source Project
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
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SparseArrayKotlinTest {
    @Test
    fun getOrDefaultPrefersStoredValue() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertEquals("1", map.get(1, "2"))
    }

    @Test
    fun getOrDefaultUsesDefaultWhenAbsent() {
        val map = SparseArray<String>()
        assertEquals("1", map.get(1, "1"))
    }

    @Test
    fun getOrDefaultReturnsNullWhenNullStored() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertNull(map.get(1, "1"))
    }

    @Test
    fun getOrDefaultDoesNotPersistDefault() {
        val map = SparseArray<String>()
        map.get(1, "1")
        assertFalse(map.containsKey(1))
    }

    @Test
    fun putIfAbsentDoesNotOverwriteStoredValue() {
        val map = SparseArray<String>()
        map.put(1, "1")
        map.putIfAbsent(1, "2")
        assertEquals("1", map.get(1))
    }

    @Test
    fun putIfAbsentReturnsStoredValue() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertEquals("1", map.putIfAbsent(1, "2"))
    }

    @Test
    fun putIfAbsentStoresValueWhenAbsent() {
        val map = SparseArray<String>()
        map.putIfAbsent(1, "2")
        assertEquals("2", map.get(1))
    }

    @Test
    fun putIfAbsentReturnsNullWhenAbsent() {
        val map = SparseArray<String>()
        assertNull(map.putIfAbsent(1, "2"))
    }

    @Test
    fun replaceWhenAbsentDoesNotStore() {
        val map = SparseArray<String>()
        assertNull(map.replace(1, "1"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun replaceStoresAndReturnsOldValue() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertEquals("1", map.replace(1, "2"))
        assertEquals("2", map.get(1))
    }

    @Test
    fun replaceStoresAndReturnsNullWhenMappedToNull() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertNull(map.replace(1, "1"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueKeyAbsent() {
        val map = SparseArray<String>()
        assertFalse(map.replace(1, "1", "2"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun replaceValueMismatchDoesNotReplace() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertFalse(map.replace(1, "2", "3"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueMismatchNullDoesNotReplace() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertFalse(map.replace(1, null, "2"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun replaceValueMatchReplaces() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertTrue(map.replace(1, "1", "2"))
        assertEquals("2", map.get(1))
    }

    @Test
    fun replaceNullValueMismatchDoesNotReplace() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertFalse(map.replace(1, "1", "2"))
        assertNull(map.get(1))
    }

    @Test
    fun replaceNullValueMatchRemoves() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertTrue(map.replace(1, null, "1"))
        assertEquals("1", map.get(1))
    }

    @Test
    fun removeValueKeyAbsent() {
        val map = SparseArray<String>()
        assertFalse(map.remove(1, "1"))
    }

    @Test
    fun removeValueMismatchDoesNotRemove() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertFalse(map.remove(1, "2"))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeValueMismatchNullDoesNotRemove() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertFalse(map.remove(1, null))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeValueMatchRemoves() {
        val map = SparseArray<String>()
        map.put(1, "1")
        assertTrue(map.remove(1, "1"))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun removeNullValueMismatchDoesNotRemove() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertFalse(map.remove(1, "2"))
        assertTrue(map.containsKey(1))
    }

    @Test
    fun removeNullValueMatchRemoves() {
        val map = SparseArray<String?>()
        map.put(1, null)
        assertTrue(map.remove(1, null))
        assertFalse(map.containsKey(1))
    }

    @Test
    fun isEmpty() {
        val array = SparseArray<String>()
        assertTrue(array.isEmpty()) // Newly created SparseArrayCompat should be empty

        // Adding elements should change state from empty to not empty.
        for (i in 0..4) {
            array.put(i, i.toString())
            assertFalse(array.isEmpty())
        }
        array.clear()
        assertTrue(array.isEmpty()) // A cleared SparseArrayCompat should be empty.
        val key1 = 1
        val key2 = 2
        val value1 = "some value"
        val value2 = "some other value"
        array.append(key1, value1)
        assertFalse(array.isEmpty()) // has 1 element.
        array.append(key2, value2)
        assertFalse(array.isEmpty()) // has 2 elements.
        assertFalse(array.isEmpty()) // consecutive calls should be OK.
        array.remove(key1)
        assertFalse(array.isEmpty()) // has 1 element.
        array.remove(key2)
        assertTrue(array.isEmpty())
    }

    @Test
    fun containsKey() {
        val array = SparseArray<String>()
        array.put(1, "one")
        assertTrue(array.containsKey(1))
        assertFalse(array.containsKey(2))
    }

    @Test
    fun containsValue() {
        val array = SparseArray<String>()
        array.put(1, "one")
        assertTrue(array.containsValue("one"))
        assertFalse(array.containsValue("two"))
    }

    @Test
    fun putAll() {
        val dest = SparseArray<String>()
        dest.put(1, "one")
        dest.put(3, "three")
        val source = SparseArray<String>()
        source.put(1, "uno")
        source.put(2, "dos")
        dest.putAll(source)
        assertEquals(3, dest.size.toLong())
        assertEquals("uno", dest.get(1))
        assertEquals("dos", dest.get(2))
        assertEquals("three", dest.get(3))
    }

    @Test
    fun putAllVariance() {
        val dest = SparseArray<Any>()
        dest.put(1, 1L)
        val source = SparseArray<String>()
        dest.put(2, "two")
        dest.putAll(source)
        assertEquals(2, dest.size.toLong())
        assertEquals(1L, dest.get(1))
        assertEquals("two", dest.get(2))
    }

    @Test
    fun copyConstructor() {
        val source = SparseArray<String>()
        source.put(42, "cuarenta y dos")
        source.put(3, "tres")
        source.put(11, "once")
        val dest = SparseArray(source)
        assertNotSame(source, dest)

        assertEquals(source.size, dest.size)
        for (i in 0 until source.size) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }
}