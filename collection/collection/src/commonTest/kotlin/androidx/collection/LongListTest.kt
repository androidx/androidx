/*
 * Copyright 2023 The Android Open Source Project
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to the kotlin source file.
//
// This file was generated from a template in the template directory.
// Make a change to the original template and run the generateCollections.sh script
// to ensure the change is available on all versions of the map.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

class LongListTest {
    private val list: MutableLongList = mutableLongListOf(1L, 2L, 3L, 4L, 5L)

    @Test
    fun emptyConstruction() {
        val l = mutableLongListOf()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableLongList(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableLongListOf(1L, 2L, 3L)
        assertEquals(3, l.size)
        assertEquals(1L, l[0])
        assertEquals(2L, l[1])
        assertEquals(3L, l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableLongListOf(1L, 2L, 3L, 4L, 5L)
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableLongListOf(1L, 2L, 3L, 4L, 5L)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.removeAt(4)
        assertNotEquals(list.hashCode(), l2.hashCode())
        l2.add(5L)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.clear()
        assertNotEquals(list.hashCode(), l2.hashCode())
    }

    @Test
    fun equalsTest() {
        val l2 = mutableLongListOf(1L, 2L, 3L, 4L, 5L)
        assertEquals(list, l2)
        assertNotEquals(list, mutableLongListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(5L)
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[${1L}, ${2L}, ${3L}, ${4L}, ${5L}]", list.toString())
        assertEquals("[]", mutableLongListOf().toString())
    }

    @Test
    fun joinToString() {
        assertEquals("${1L}, ${2L}, ${3L}, ${4L}, ${5L}", list.joinToString())
        assertEquals(
            "x${1L}, ${2L}, ${3L}...",
            list.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${1L}-${2L}-${3L}-${4L}-${5L}<",
            list.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        assertEquals(
            "one, two, three...",
            list.joinToString(limit = 3) {
                when (it.toInt()) {
                    1 -> "one"
                    2 -> "two"
                    3 -> "three"
                    else -> "whoops"
                }
            }
        )
    }

    @Test
    fun size() {
        assertEquals(5, list.size)
        assertEquals(5, list.count())
        val l2 = mutableLongListOf()
        assertEquals(0, l2.size)
        assertEquals(0, l2.count())
        l2 += 1L
        assertEquals(1, l2.size)
        assertEquals(1, l2.count())
    }

    @Test
    fun get() {
        assertEquals(1L, list[0])
        assertEquals(5L, list[4])
        assertEquals(1L, list.elementAt(0))
        assertEquals(5L, list.elementAt(4))
    }

    @Test
    fun getOutOfBounds() {
        assertFailsWith(IndexOutOfBoundsException::class) { list[5] }
    }

    @Test
    fun getOutOfBoundsNegative() {
        assertFailsWith(IndexOutOfBoundsException::class) { list[-1] }
    }

    @Test
    fun elementAtOfBounds() {
        assertFailsWith(IndexOutOfBoundsException::class) { list.elementAt(5) }
    }

    @Test
    fun elementAtOfBoundsNegative() {
        assertFailsWith(IndexOutOfBoundsException::class) { list.elementAt(-1) }
    }

    @Test
    fun elementAtOrElse() {
        assertEquals(
            1L,
            list.elementAtOrElse(0) {
                assertEquals(0, it)
                0L
            }
        )
        assertEquals(
            0L,
            list.elementAtOrElse(-1) {
                assertEquals(-1, it)
                0L
            }
        )
        assertEquals(
            0L,
            list.elementAtOrElse(5) {
                assertEquals(5, it)
                0L
            }
        )
    }

    @Test
    fun count() {
        assertEquals(1, list.count { it < 2L })
        assertEquals(0, list.count { it < 0L })
        assertEquals(5, list.count { it < 10L })
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertFalse(list.none())
        assertTrue(mutableLongListOf().isEmpty())
        assertTrue(mutableLongListOf().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableLongListOf().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableLongListOf().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5L })
        assertTrue(list.any { it == 1L })
        assertFalse(list.any { it == 0L })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableLongListOf()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableLongListOf(5L, 4L, 3L, 2L, 1L)
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableLongListOf()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(reversedSublist, mutableLongListOf(5L, 4L))
    }

    @Test
    fun forEach() {
        val copy = mutableLongListOf()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableLongListOf()
        list.forEachReversed { copy += it }
        assertEquals(copy, mutableLongListOf(5L, 4L, 3L, 2L, 1L))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableLongListOf()
        val indices = mutableLongListOf()
        list.forEachIndexed { index, item ->
            copy += item
            indices += index.toLong()
        }
        assertEquals(list, copy)
        assertEquals(indices, mutableLongListOf(0L, 1L, 2L, 3L, 4L))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableLongListOf()
        val indices = mutableLongListOf()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += index.toLong()
        }
        assertEquals(copy, mutableLongListOf(5L, 4L, 3L, 2L, 1L))
        assertEquals(indices, mutableLongListOf(4L, 3L, 2L, 1L, 0L))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == 1L })
        assertEquals(4, list.indexOfFirst { it == 5L })
        assertEquals(-1, list.indexOfFirst { it == 0L })
        assertEquals(0, mutableLongListOf(8L, 8L).indexOfFirst { it == 8L })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == 1L })
        assertEquals(4, list.indexOfLast { it == 5L })
        assertEquals(-1, list.indexOfLast { it == 0L })
        assertEquals(1, mutableLongListOf(8L, 8L).indexOfLast { it == 8L })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5L))
        assertTrue(list.contains(1L))
        assertFalse(list.contains(0L))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(mutableLongListOf(2L, 3L, 1L)))
        assertFalse(list.containsAll(mutableLongListOf(2L, 3L, 6L)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5L))
        assertEquals(1, list.lastIndexOf(2L))
        val copy = mutableLongListOf()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(1L))
    }

    @Test
    fun first() {
        assertEquals(1L, list.first())
    }

    @Test
    fun firstException() {
        assertFailsWith(NoSuchElementException::class) { mutableLongListOf().first() }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5L, list.first { it == 5L })
        assertEquals(1L, mutableLongListOf(1L, 5L).first { it != 0L })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableLongListOf().first { it == 8L } }
    }

    @Test
    fun last() {
        assertEquals(5L, list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) { mutableLongListOf().last() }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1L, list.last { it == 1L })
        assertEquals(5L, mutableLongListOf(1L, 5L).last { it != 0L })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableLongListOf().last { it == 8L } }
    }

    @Test
    fun fold() {
        assertEquals("12345", list.fold("") { acc, i -> acc + i.toInt().toString() })
    }

    @Test
    fun foldIndexed() {
        assertEquals(
            "01-12-23-34-45-",
            list.foldIndexed("") { index, acc, i -> "$acc$index${i.toInt()}-" }
        )
    }

    @Test
    fun foldRight() {
        assertEquals("54321", list.foldRight("") { i, acc -> acc + i.toInt().toString() })
    }

    @Test
    fun foldRightIndexed() {
        assertEquals(
            "45-34-23-12-01-",
            list.foldRightIndexed("") { index, i, acc -> "$acc$index${i.toInt()}-" }
        )
    }

    @Test
    fun add() {
        val l = mutableLongListOf(1L, 2L, 3L)
        l += 4L
        l.add(5L)
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableLongListOf(2L, 4L)
        l.add(2, 5L)
        l.add(0, 1L)
        l.add(2, 3L)
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(-1, 2L) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(6, 2L) }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableLongListOf(4L)
        val l2 = mutableLongListOf(1L, 2L)
        val l3 = mutableLongListOf(5L)
        val l4 = mutableLongListOf(3L)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableLongListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(6, mutableLongListOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(-1, mutableLongListOf()) }
    }

    @Test
    fun addAllList() {
        val l = MutableLongList()
        l.add(3L)
        l.add(4L)
        l.add(5L)
        val l2 = mutableLongListOf(1L, 2L)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableLongListOf()))
    }

    @Test
    fun plusAssignList() {
        val l = MutableLongList()
        l.add(3L)
        l.add(4L)
        l.add(5L)
        val l2 = mutableLongListOf(1L, 2L)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun addAllArrayAtIndex() {
        val a1 = longArrayOf(4L)
        val a2 = longArrayOf(1L, 2L)
        val a3 = longArrayOf(5L)
        val l = mutableLongListOf(3L)
        assertTrue(l.addAll(1, a3))
        assertTrue(l.addAll(0, a2))
        assertTrue(l.addAll(3, a1))
        assertFalse(l.addAll(0, longArrayOf()))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(6, longArrayOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(-1, longArrayOf()) }
    }

    @Test
    fun addAllArray() {
        val a = longArrayOf(3L, 4L, 5L)
        val v = mutableLongListOf(1L, 2L)
        v.addAll(a)
        assertEquals(5, v.size)
        assertEquals(3L, v[2])
        assertEquals(4L, v[3])
        assertEquals(5L, v[4])
    }

    @Test
    fun plusAssignArray() {
        val a = longArrayOf(3L, 4L, 5L)
        val v = mutableLongListOf(1L, 2L)
        v += a
        assertEquals(list, v)
    }

    @Test
    fun clear() {
        val l = mutableLongListOf()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun trim() {
        val l = mutableLongListOf(1L)
        l.trim()
        assertEquals(1, l.capacity)
        l += longArrayOf(1L, 2L, 3L, 4L, 5L)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += longArrayOf(1L, 2L, 3L, 4L, 5L)
        l -= 5L
        l.trim(5)
        assertEquals(5, l.capacity)
        l.trim(4)
        assertEquals(4, l.capacity)
        l.trim(3)
        assertEquals(4, l.capacity)
    }

    @Test
    fun remove() {
        val l = mutableLongListOf(1L, 2L, 3L, 4L, 5L)
        l.remove(3L)
        assertEquals(mutableLongListOf(1L, 2L, 4L, 5L), l)
    }

    @Test
    fun removeAt() {
        val l = mutableLongListOf(1L, 2L, 3L, 4L, 5L)
        l.removeAt(2)
        assertEquals(mutableLongListOf(1L, 2L, 4L, 5L), l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(6) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(-1) }
    }

    @Test
    fun set() {
        val l = mutableLongListOf(0L, 0L, 0L, 0L, 0L)
        l[0] = 1L
        l[4] = 5L
        l[2] = 3L
        l[1] = 2L
        l[3] = 4L
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.set(-1, 1L) }
        assertFailsWith<IndexOutOfBoundsException> { l.set(6, 1L) }
        assertEquals(4L, l.set(3, 1L))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableLongListOf(1L)
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(mutableLongListOf(0L, 10L, 15L)))
        val l = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L, 5L)
        assertTrue(l.removeAll(mutableLongListOf(20L, 0L, 15L, 10L, 5L)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllLongArray() {
        assertFalse(list.removeAll(longArrayOf(0L, 10L, 15L)))
        val l = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L, 5L)
        assertTrue(l.removeAll(longArrayOf(20L, 0L, 15L, 10L, 5L)))
        assertEquals(list, l)
    }

    @Test
    fun minusAssignList() {
        val l = mutableLongListOf().also { it += list }
        l -= mutableLongListOf(0L, 10L, 15L)
        assertEquals(list, l)
        val l2 = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L, 5L)
        l2 -= mutableLongListOf(20L, 0L, 15L, 10L, 5L)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignLongArray() {
        val l = mutableLongListOf().also { it += list }
        l -= longArrayOf(0L, 10L, 15L)
        assertEquals(list, l)
        val l2 = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L, 5L)
        l2 -= longArrayOf(20L, 0L, 15L, 10L, 5L)
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(list.retainAll(mutableLongListOf(1L, 2L, 3L, 4L, 5L, 6L)))
        val l = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L)
        assertTrue(l.retainAll(mutableLongListOf(1L, 2L, 3L, 4L, 5L, 6L)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllLongArray() {
        assertFalse(list.retainAll(longArrayOf(1L, 2L, 3L, 4L, 5L, 6L)))
        val l = mutableLongListOf(0L, 1L, 15L, 10L, 2L, 3L, 4L, 5L, 20L)
        assertTrue(l.retainAll(longArrayOf(1L, 2L, 3L, 4L, 5L, 6L)))
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableLongListOf(1L, 9L, 7L, 6L, 2L, 3L, 4L, 5L)
        l.removeRange(1, 4)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(6, 6) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(100, 200) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(-1, 0) }
        assertFailsWith<IllegalArgumentException> { l.removeRange(3, 2) }
    }

    @Test
    fun sort() {
        val l = mutableLongListOf(1L, 4L, 2L, 5L, 3L)
        l.sort()
        assertEquals(list, l)
    }

    @Test
    fun sortDescending() {
        val l = mutableLongListOf(1L, 4L, 2L, 5L, 3L)
        l.sortDescending()
        assertEquals(mutableLongListOf(5L, 4L, 3L, 2L, 1L), l)
    }

    @Test
    fun sortEmpty() {
        val l = MutableLongList(0)
        l.sort()
        l.sortDescending()
        assertEquals(MutableLongList(0), l)
    }

    @Test
    fun testEmptyLongList() {
        val l = emptyLongList()
        assertEquals(0, l.size)
    }

    @Test
    fun longListOfEmpty() {
        val l = longListOf()
        assertEquals(0, l.size)
    }

    @Test
    fun longListOfOneValue() {
        val l = longListOf(2L)
        assertEquals(1, l.size)
        assertEquals(2L, l[0])
    }

    @Test
    fun longListOfTwoValues() {
        val l = longListOf(2L, 1L)
        assertEquals(2, l.size)
        assertEquals(2L, l[0])
        assertEquals(1L, l[1])
    }

    @Test
    fun longListOfThreeValues() {
        val l = longListOf(2L, 10L, -1L)
        assertEquals(3, l.size)
        assertEquals(2L, l[0])
        assertEquals(10L, l[1])
        assertEquals(-1L, l[2])
    }

    @Test
    fun longListOfFourValues() {
        val l = longListOf(2L, 10L, -1L, 10L)
        assertEquals(4, l.size)
        assertEquals(2L, l[0])
        assertEquals(10L, l[1])
        assertEquals(-1L, l[2])
        assertEquals(10L, l[3])
    }

    @Test
    fun mutableLongListOfOneValue() {
        val l = mutableLongListOf(2L)
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(2L, l[0])
    }

    @Test
    fun mutableLongListOfTwoValues() {
        val l = mutableLongListOf(2L, 1L)
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(2L, l[0])
        assertEquals(1L, l[1])
    }

    @Test
    fun mutableLongListOfThreeValues() {
        val l = mutableLongListOf(2L, 10L, -1L)
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(2L, l[0])
        assertEquals(10L, l[1])
        assertEquals(-1L, l[2])
    }

    @Test
    fun mutableLongListOfFourValues() {
        val l = mutableLongListOf(2L, 10L, -1L, 10L)
        assertEquals(4, l.size)
        assertEquals(4, l.capacity)
        assertEquals(2L, l[0])
        assertEquals(10L, l[1])
        assertEquals(-1L, l[2])
        assertEquals(10L, l[3])
    }

    @Test
    fun binarySearchLongList() {
        val l = mutableLongListOf(-2L, -1L, 2L, 10L, 10L)
        assertEquals(0, l.binarySearch(-2))
        assertEquals(2, l.binarySearch(2))
        assertEquals(3, l.binarySearch(10))

        assertEquals(-1, l.binarySearch(-20))
        assertEquals(-4, l.binarySearch(3))
        assertEquals(-6, l.binarySearch(20))
    }
}
