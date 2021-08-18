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
import kotlin.test.assertTrue

class IndexBasedMutableIteratorKotlinTest {

    @Test
    fun iterateAll() {
        val iterator = ArraySet(setOf("a", "b", "c")).iterator()
        assertEquals(iterator.asSequence().toSet(), setOf("a", "b", "c"))
    }

    @Test
    fun iterateEmptyList() {
        val iterator = ArraySet<String>().iterator()
        assertFalse(iterator.hasNext())
        assertTrue(
            runCatching {
                iterator.next()
            }.exceptionOrNull() is NoSuchElementException
        )
    }

    @Test
    fun removeSameItemTwice() {
        val iterator = ArraySet(listOf("a", "b", "c")).iterator()
        iterator.next() // move to next
        iterator.remove()
        assertTrue(
            runCatching {
                iterator.remove()
            }.exceptionOrNull() is IllegalStateException
        )
    }

    @Test
    fun removeLast() = removeViaIterator(
        original = setOf("a", "b", "c"),
        toBeRemoved = setOf("c"),
        expected = setOf("a", "b")
    )

    @Test
    fun removeFirst() = removeViaIterator(
        original = setOf("a", "b", "c"),
        toBeRemoved = setOf("a"),
        expected = setOf("b", "c")
    )

    @Test
    fun removeMid() = removeViaIterator(
        original = setOf("a", "b", "c"),
        toBeRemoved = setOf("b"),
        expected = setOf("a", "c")
    )

    @Test
    fun removeConsecutive() = removeViaIterator(
        original = setOf("a", "b", "c", "d"),
        toBeRemoved = setOf("b", "c"),
        expected = setOf("a", "d")
    )

    @Test
    fun removeLastTwo() = removeViaIterator(
        original = setOf("a", "b", "c", "d"),
        toBeRemoved = setOf("c", "d"),
        expected = setOf("a", "b")
    )

    @Test
    fun removeFirstTwo() = removeViaIterator(
        original = setOf("a", "b", "c", "d"),
        toBeRemoved = setOf("a", "b"),
        expected = setOf("c", "d")
    )

    @Test
    fun removeMultiple() = removeViaIterator(
        original = setOf("a", "b", "c", "d"),
        toBeRemoved = setOf("a", "c"),
        expected = setOf("b", "d")
    )

    private fun removeViaIterator(
        original: Set<String>,
        toBeRemoved: Set<String>,
        expected: Set<String>
    ) {
        val subject = ArraySet(original)
        val iterator = subject.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next in toBeRemoved) {
                iterator.remove()
            }
        }
        assertTrue(subject.size == expected.size && subject.containsAll(expected))
    }
}
