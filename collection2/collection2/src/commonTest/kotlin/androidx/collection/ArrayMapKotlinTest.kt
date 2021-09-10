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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArrayMapKotlinTest {
    @Test
    fun testCanNotIteratePastEnd_entrySetIterator() {
        val map: MutableMap<String, String> = ArrayMap()
        map.put("key 1", "value 1")
        map.put("key 2", "value 2")
        val expectedEntriesToIterate = mutableSetOf(
            Pair("key 1", "value 1"),
            Pair("key 2", "value 2")
        )
        val iterator: Iterator<Map.Entry<String, String>> = map.entries.iterator()

        // Assert iteration over the expected two entries in any order
        assertTrue(iterator.hasNext())
        val firstEntry = iterator.next().toPair()
        assertTrue(expectedEntriesToIterate.remove(firstEntry))
        assertTrue(iterator.hasNext())
        val secondEntry = iterator.next().toPair()
        assertTrue(expectedEntriesToIterate.remove(secondEntry))
        assertFalse(iterator.hasNext())
        assertTrue(runCatching { iterator.next() }.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun testCanNotIteratePastEnd_keySetIterator() {
        val map: MutableMap<String, String> = ArrayMap()
        map.put("key 1", "value 1")
        map.put("key 2", "value 2")
        val expectedKeysToIterate = mutableSetOf("key 1", "key 2")
        val iterator = map.keys.iterator()

        // Assert iteration over the expected two keys in any order
        assertTrue(iterator.hasNext())
        val firstKey = iterator.next()
        assertTrue(expectedKeysToIterate.remove(firstKey))
        assertTrue(iterator.hasNext())
        val secondKey = iterator.next()
        assertTrue(expectedKeysToIterate.remove(secondKey))
        assertFalse(iterator.hasNext())
        assertTrue(runCatching { iterator.next() }.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun testCanNotIteratePastEnd_valuesIterator() {
        val map: MutableMap<String, String> = ArrayMap()
        map.put("key 1", "value 1")
        map.put("key 2", "value 2")
        val expectedValuesToIterate = mutableSetOf("value 1", "value 2")
        val iterator = map.values.iterator()

        // Assert iteration over the expected two values in any order
        assertTrue(iterator.hasNext())
        val firstValue = iterator.next()
        assertTrue(expectedValuesToIterate.remove(firstValue))
        assertTrue(iterator.hasNext())
        val secondValue = iterator.next()
        assertTrue(expectedValuesToIterate.remove(secondValue))
        assertFalse(iterator.hasNext())
        assertTrue(runCatching { iterator.next() }.exceptionOrNull() is NoSuchElementException)
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    fun testNonConcurrentAccesses() {
        val mMap = ArrayMap<String, String>()
        var i = 0
        while (i < 100000) {
            mMap["key $i"] = "B_DONT_DO_THAT"
            i++
            if (i % 200 == 0) {
                print(".")
            }
            if (i % 500 == 0) {
                mMap.clear()
                print("X")
            }
            i++
        }
        // Test is expected to pass without throwing anything.
    }
}