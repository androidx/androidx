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

class LongSparseArrayKotlinTest {
    @Test
    fun getOrDefaultPrefersStoredValue() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertEquals("1", map.get(1L, "2"))
    }

    @Test
    fun getOrDefaultUsesDefaultWhenAbsent() {
        val map = LongSparseArray<String>()
        assertEquals("1", map.get(1L, "1"))
    }

    @Test
    fun getOrDefaultReturnsNullWhenNullStored() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertNull(map.get(1L, "1"))
    }

    @Test
    fun getOrDefaultDoesNotPersistDefault() {
        val map = LongSparseArray<String>()
        map.get(1L, "1")
        assertFalse(map.containsKey(1L))
    }

    @Test
    fun putIfAbsentDoesNotOverwriteStoredValue() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        map.putIfAbsent(1L, "2")
        assertEquals("1", map[1L])
    }

    @Test
    fun putIfAbsentReturnsStoredValue() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertEquals("1", map.putIfAbsent(1L, "2"))
    }

    @Test
    fun putIfAbsentStoresValueWhenAbsent() {
        val map = LongSparseArray<String>()
        map.putIfAbsent(1L, "2")
        assertEquals("2", map[1L])
    }

    @Test
    fun putIfAbsentReturnsNullWhenAbsent() {
        val map = LongSparseArray<String>()
        assertNull(map.putIfAbsent(1L, "2"))
    }

    @Test
    fun replaceWhenAbsentDoesNotStore() {
        val map = LongSparseArray<String>()
        assertNull(map.replace(1L, "1"))
        assertFalse(map.containsKey(1L))
    }

    @Test
    fun replaceStoresAndReturnsOldValue() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertEquals("1", map.replace(1L, "2"))
        assertEquals("2", map[1L])
    }

    @Test
    fun replaceStoresAndReturnsNullWhenMappedToNull() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertNull(map.replace(1L, "1"))
        assertEquals("1", map[1L])
    }

    @Test
    fun replaceValueKeyAbsent() {
        val map = LongSparseArray<String>()
        assertFalse(map.replace(1L, "1", "2"))
        assertFalse(map.containsKey(1L))
    }

    @Test
    fun replaceValueMismatchDoesNotReplace() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertFalse(map.replace(1L, "2", "3"))
        assertEquals("1", map[1L])
    }

    @Test
    fun replaceValueMismatchNullDoesNotReplace() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertFalse(map.replace(1L, null, "2"))
        assertEquals("1", map[1L])
    }

    @Test
    fun replaceValueMatchReplaces() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertTrue(map.replace(1L, "1", "2"))
        assertEquals("2", map[1L])
    }

    @Test
    fun replaceNullValueMismatchDoesNotReplace() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertFalse(map.replace(1L, "1", "2"))
        assertNull(map[1L])
    }

    @Test
    fun replaceNullValueMatchRemoves() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertTrue(map.replace(1L, null, "1"))
        assertEquals("1", map[1L])
    }

    @Test
    fun removeValueKeyAbsent() {
        val map = LongSparseArray<String>()
        assertFalse(map.remove(1L, "1"))
    }

    @Test
    fun removeValueMismatchDoesNotRemove() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertFalse(map.remove(1L, "2"))
        assertTrue(map.containsKey(1L))
    }

    @Test
    fun removeValueMismatchNullDoesNotRemove() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertFalse(map.remove(1L, null))
        assertTrue(map.containsKey(1L))
    }

    @Test
    fun removeValueMatchRemoves() {
        val map = LongSparseArray<String>()
        map.put(1L, "1")
        assertTrue(map.remove(1L, "1"))
        assertFalse(map.containsKey(1L))
    }

    @Test
    fun removeNullValueMismatchDoesNotRemove() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertFalse(map.remove(1L, "2"))
        assertTrue(map.containsKey(1L))
    }

    @Test
    fun removeNullValueMatchRemoves() {
        val map = LongSparseArray<String?>()
        map.put(1L, null)
        assertTrue(map.remove(1L, null))
        assertFalse(map.containsKey(1L))
    }

    @Test
    fun isEmpty() {
        val LongSparseArray = LongSparseArray<String>()
        assertTrue(LongSparseArray.isEmpty()) // Newly created LongSparseArray should be empty

        // Adding elements should change state from empty to not empty.
        for (i in 0L..5L - 1) {
            LongSparseArray.put(i, i.toString())
            assertFalse(LongSparseArray.isEmpty())
        }
        LongSparseArray.clear()
        assertTrue(LongSparseArray.isEmpty()) // A cleared LongSparseArray should be empty.
        val key1 = 1L
        val key2 = 2L
        val value1 = "some value"
        val value2 = "some other value"
        LongSparseArray.append(key1, value1)
        assertFalse(LongSparseArray.isEmpty()) // has 1 element.
        LongSparseArray.append(key2, value2)
        assertFalse(LongSparseArray.isEmpty()) // has 2 elements.
        assertFalse(LongSparseArray.isEmpty()) // consecutive calls should be OK.
        LongSparseArray.remove(key1)
        assertFalse(LongSparseArray.isEmpty()) // has 1 element.
        LongSparseArray.remove(key2)
        assertTrue(LongSparseArray.isEmpty())
    }

    @Test
    fun containsKey() {
        val array = LongSparseArray<String>()
        array.put(1L, "one")
        assertTrue(array.containsKey(1L))
        assertFalse(array.containsKey(2L))
    }

    @Test
    fun containsValue() {
        val array = LongSparseArray<String>()
        array.put(1L, "one")
        assertTrue(array.containsValue("one"))
        assertFalse(array.containsValue("two"))
    }

    @Test
    fun putAll() {
        val dest = LongSparseArray<String>()
        dest.put(1L, "one")
        dest.put(3L, "three")
        val source = LongSparseArray<String>()
        source.put(1L, "uno")
        source.put(2L, "dos")
        dest.putAll(source)
        assertEquals(3, dest.size)
        assertEquals("uno", dest[1L])
        assertEquals("dos", dest[2L])
        assertEquals("three", dest[3L])
    }

    @Test
    fun putAllVariance() {
        val dest = LongSparseArray<Any>()
        dest.put(1L, 1L)
        val source = LongSparseArray<String>()
        dest.put(2L, "two")
        dest.putAll(source)
        assertEquals(2, dest.size)
        assertEquals(1L, dest[1L])
        assertEquals("two", dest[2L])
    }

    @Test
    fun copyConstructor() {
        val source = LongSparseArray<String>()
        source.put(42, "cuarenta y dos")
        source.put(3, "tres")
        source.put(11, "once")
        val dest = LongSparseArray(source)
        assertNotSame(source, dest)

        assertEquals(source.size, dest.size)
        for (i in 0 until source.size) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }
}