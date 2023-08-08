/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.internal

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class LruHashMapTest {

    private val map = LruHashMap<String, String>(initialCapacity = 16, loadFactor = 0.75F)

    @Test
    fun isEmptyReturnsTrueIfEmpty() {
        assertTrue(map.isEmpty)
    }

    @Test
    fun isEmptyReturnsFalseIfInserted() {
        map.put("a", "A")

        assertFalse(map.isEmpty)
    }

    @Test
    fun isEmptyReturnsTrueIfLastEntryRemoved() {
        map.put("a", "A")

        map.remove("a")

        assertTrue(map.isEmpty)
    }

    @Test
    fun getReturnsNullIfNotExists() {
        val value = map["a"]

        assertNull(value)
    }

    @Test
    fun getReturnsValueIfExists() {
        map.put("a", "A")

        val value = map["a"]

        assertEquals("A", value)
    }

    @Test
    fun getReturnsNullIfValueRemoved() {
        map.put("a", "A")
        map.remove("a")

        val value = map["a"]

        assertNull(value)
    }

    @Test
    fun iteratesInInsertionOrder() {
        map.put("a", "A")
        map.put("b", "B")

        val list = map.entries.map { (k, v) -> k to v }

        assertContentEquals(listOf("a" to "A", "b" to "B"), list)
    }

    @Test
    fun getMovesEntryToTheEnd() {
        map.put("a", "A")
        map.put("b", "B")
        map["a"]

        val list = map.entries.map { (k, v) -> k to v }

        assertContentEquals(listOf("b" to "B", "a" to "A"), list)
    }

    @Test
    fun putMovesEntryToTheEnd() {
        map.put("a", "A")
        map.put("b", "B")
        map.put("a", "A")

        val list = map.entries.map { (k, v) -> k to v }

        assertContentEquals(listOf("b" to "B", "a" to "A"), list)
    }
}
