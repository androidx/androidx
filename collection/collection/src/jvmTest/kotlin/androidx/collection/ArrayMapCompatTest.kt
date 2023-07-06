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
import kotlin.test.assertTrue

internal class ArrayMapCompatTest {

    @Test
    fun testCanNotIteratePastEnd_entrySetIterator() {
        val map = ArrayMap<String, String>()
        map["key 1"] = "value 1"
        map["key 2"] = "value 2"
        val iterator = map.entries.iterator()

        // Assert iteration over the expected two entries in any order
        assertTrue(iterator.hasNext())
        assertEquals("key 1" to "value 1", iterator.next().toPair())
        assertTrue(iterator.hasNext())
        assertEquals("key 2" to "value 2", iterator.next().toPair())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun testCanNotIteratePastEnd_keySetIterator() {
        val map = ArrayMap<String, String>()
        map["key 1"] = "value 1"
        map["key 2"] = "value 2"
        val iterator = map.keys.iterator()

        // Assert iteration over the expected two keys in any order
        assertTrue(iterator.hasNext())
        assertEquals("key 1", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("key 2", iterator.next())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun testCanNotIteratePastEnd_valuesIterator() {
        val map = ArrayMap<String, String>()
        map["key 1"] = "value 1"
        map["key 2"] = "value 2"
        val iterator = map.values.iterator()

        // Assert iteration over the expected two values in any order
        assertTrue(iterator.hasNext())
        assertEquals("value 1", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("value 2", iterator.next())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun toArray() {
        val map = ArrayMap<String, String>()
        map["key 1"] = "value 1"
        map["key 2"] = "value 2"
        val array = map.entries.toTypedArray()
        assertEquals(2, array.size)
    }
}
