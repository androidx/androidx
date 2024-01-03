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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("RedundantVisibilityModifier")
public class SimpleArrayMapTest {
    @Test
    public fun equalsEmpty() {
        val empty = SimpleArrayMap<String, String>()
        assertEquals(empty, empty)
        assertTrue(empty == emptyMap<String, String>())
        assertEquals(empty, SimpleArrayMap())
        assertTrue(empty == HashMap<String, String>())
        assertFalse(empty == mapOf("foo" to "bar"))
        val simpleArrayMapNotEmpty = SimpleArrayMap<String, String>()
        simpleArrayMapNotEmpty.put("foo", "bar")
        assertNotEquals(empty, simpleArrayMapNotEmpty)
        val hashMapNotEquals = HashMap<String, String>()
        hashMapNotEquals["foo"] = "bar"
        assertFalse(empty == hashMapNotEquals)
    }

    @Test
    public fun equalsNonEmpty() {
        val map = SimpleArrayMap<String, String>()
        map.put("foo", "bar")
        assertTrue(map == mapOf("foo" to "bar"))
        val otherSimpleArrayMap = SimpleArrayMap<String, String>()
        otherSimpleArrayMap.put("foo", "bar")
        val otherHashMap = HashMap<String, String>()
        otherHashMap["foo"] = "bar"
        assertTrue(map == otherHashMap)
        assertFalse(map == emptyMap<Any, Any>())
        assertNotEquals(map, SimpleArrayMap())
        assertFalse(map == HashMap<String, String>())
    }

    @Test
    public fun getOrDefaultPrefersStoredValue() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertEquals("1", map.getOrDefault("one", "2"))
    }

    @Test
    public fun getOrDefaultUsesDefaultWhenAbsent() {
        val map = SimpleArrayMap<String, String>()
        assertEquals("1", map.getOrDefault("one", "1"))
    }

    @Test
    public fun getOrDefaultReturnsNullWhenNullStored() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertNull(map.getOrDefault("one", "1"))
    }

    @Test
    public fun getOrDefaultDoesNotPersistDefault() {
        val map = SimpleArrayMap<String, String>()
        map.getOrDefault("one", "1")
        assertFalse(map.containsKey("one"))
    }

    @Test
    public fun putIfAbsentDoesNotOverwriteStoredValue() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        map.putIfAbsent("one", "2")
        assertEquals("1", map["one"])
    }

    @Test
    public fun putIfAbsentReturnsStoredValue() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertEquals("1", map.putIfAbsent("one", "2"))
    }

    @Test
    public fun putIfAbsentStoresValueWhenAbsent() {
        val map = SimpleArrayMap<String, String>()
        map.putIfAbsent("one", "2")
        assertEquals("2", map["one"])
    }

    @Test
    public fun putIfAbsentReturnsNullWhenAbsent() {
        val map = SimpleArrayMap<String, String>()
        assertNull(map.putIfAbsent("one", "2"))
    }

    @Test
    public fun replaceWhenAbsentDoesNotStore() {
        val map = SimpleArrayMap<String, String>()
        assertNull(map.replace("one", "1"))
        assertFalse(map.containsKey("one"))
    }

    @Test
    public fun replaceStoresAndReturnsOldValue() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertEquals("1", map.replace("one", "2"))
        assertEquals("2", map["one"])
    }

    @Test
    public fun replaceStoresAndReturnsNullWhenMappedToNull() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertNull(map.replace("one", "1"))
        assertEquals("1", map["one"])
    }

    @Test
    public fun replaceValueKeyAbsent() {
        val map = SimpleArrayMap<String, String>()
        assertFalse(map.replace("one", "1", "2"))
        assertFalse(map.containsKey("one"))
    }

    @Test
    public fun replaceValueMismatchDoesNotReplace() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertFalse(map.replace("one", "2", "3"))
        assertEquals("1", map["one"])
    }

    @Test
    public fun replaceValueMismatchNullDoesNotReplace() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", "1")
        assertFalse(map.replace("one", null, "2"))
        assertEquals("1", map["one"])
    }

    @Test
    public fun replaceValueMatchReplaces() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertTrue(map.replace("one", "1", "2"))
        assertEquals("2", map["one"])
    }

    @Test
    public fun replaceNullValueMismatchDoesNotReplace() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertFalse(map.replace("one", "1", "2"))
        assertNull(map["one"])
    }

    @Test
    public fun replaceNullValueMatchRemoves() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertTrue(map.replace("one", null, "1"))
        assertEquals("1", map["one"])
    }

    @Test
    public fun removeValueKeyAbsent() {
        val map = SimpleArrayMap<String, String>()
        assertFalse(map.remove("one", "1"))
    }

    @Test
    public fun removeValueMismatchDoesNotRemove() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertFalse(map.remove("one", "2"))
        assertTrue(map.containsKey("one"))
    }

    @Test
    public fun removeValueMismatchNullDoesNotRemove() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", "1")
        assertFalse(map.remove("one", null))
        assertTrue(map.containsKey("one"))
    }

    @Test
    public fun removeValueMatchRemoves() {
        val map = SimpleArrayMap<String, String>()
        map.put("one", "1")
        assertTrue(map.remove("one", "1"))
        assertFalse(map.containsKey("one"))
    }

    @Test
    public fun removeNullValueMismatchDoesNotRemove() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertFalse(map.remove("one", "2"))
        assertTrue(map.containsKey("one"))
    }

    @Test
    public fun removeNullValueMatchRemoves() {
        val map = SimpleArrayMap<String, String?>()
        map.put("one", null)
        assertTrue(map.remove("one", null))
        assertFalse(map.containsKey("one"))
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    public fun testNonConcurrentAccesses() {
        val map = SimpleArrayMap<String, String>()
        repeat(100000) { i ->
            try {
                map.put("key $i", "B_DONT_DO_THAT")
                if (i % 500 == 0) {
                    map.clear()
                }
            } catch (e: ConcurrentModificationException) {
                println("Concurrent modification caught on single thread")
                e.printStackTrace()
                fail()
            }
        }
    }

    /**
     * Even though the Javadoc of [SimpleArrayMap.put] says that the key
     * must not be null, the actual implementation allows it, and therefore we must ensure
     * that any future implementations of the class will still honor that contract.
     */
    @Test
    public fun nullKeyCompatibility_canPutNullKeyAndNonNullValue() {
        val map = SimpleArrayMap<String?, Int>()
        assertFalse(map.containsKey(null))
        map.put(null, 42)
        assertTrue(map.containsKey(null))
    }

    @Test
    public fun nullKeyCompatibility_replacesValuesWithNullKey() {
        val firstValue = 42
        val secondValue = 43
        val map = SimpleArrayMap<String?, Int>()
        assertFalse(map.containsKey(null))
        map.put(null, firstValue)
        assertTrue(map.containsKey(null))
        assertEquals(firstValue, map[null])
        assertEquals(firstValue, map.put(null, secondValue))
        assertEquals(secondValue, map[null])
        assertEquals(secondValue, map.remove(null))
        assertFalse(map.containsKey(null))
    }

    @Test
    public fun nullKeyCompatibility_putThenRemoveNullKeyAndValue() {
        val map = SimpleArrayMap<String?, Int?>()
        map.put(null, null)
        assertTrue(map.containsKey(null))
        assertNull(map[null])
        map.remove(null)
        assertFalse(map.containsKey(null))
    }

    @Test
    public fun nullKeyCompatibility_removeNonNullValueWithNullKey() {
        val map = SimpleArrayMap<String?, String?>()
        map.put(null, null)
        assertNull(map.put(null, "42"))
        assertEquals("42", map[null])
        map.remove(null)
    }

    @Test
    public fun nullKeyCompatibility_testReplaceMethodsWithNullKey() {
        val map = SimpleArrayMap<String?, String?>()
        map.put(null, null)
        assertNull(null, map.replace(null, "42"))
        assertFalse(map.replace(null, null, null))
        assertTrue(map.replace(null, "42", null))
        assertFalse(map.replace(null, "42", null))
        assertTrue(map.replace(null, null, null))
        assertTrue(map.containsKey(null))
        assertNull(map[null])
    }

    /**
     * Regression test against NPE in changes in the backing array growth implementation. Various
     * initial capacities are used, and for each capacity we always put in more elements than the
     * initial capacity can hold to exercise the code paths where the capacity is increased and the
     * backing arrays are expanded.
     */
    @Test
    public fun backingArrayGrowth() {
        for (initCapacity in 0..16) {
            for (entries in 1..31) {
                val map = SimpleArrayMap<String, String>(initCapacity)
                for (index in 0 until entries) {
                    map.put("key $index", "value $index")
                }
                for (index in 0 until entries) {
                    assertEquals("value $index", map["key $index"])
                }
            }
        }
    }
}
