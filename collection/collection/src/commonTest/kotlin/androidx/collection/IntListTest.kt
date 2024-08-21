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

class IntListTest {
    private val list: MutableIntList = mutableIntListOf(1, 2, 3, 4, 5)

    @Test
    fun emptyConstruction() {
        val l = mutableIntListOf()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableIntList(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableIntListOf(1, 2, 3)
        assertEquals(3, l.size)
        assertEquals(1, l[0])
        assertEquals(2, l[1])
        assertEquals(3, l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableIntListOf(1, 2, 3, 4, 5)
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableIntListOf(1, 2, 3, 4, 5)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.removeAt(4)
        assertNotEquals(list.hashCode(), l2.hashCode())
        l2.add(5)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.clear()
        assertNotEquals(list.hashCode(), l2.hashCode())
    }

    @Test
    fun equalsTest() {
        val l2 = mutableIntListOf(1, 2, 3, 4, 5)
        assertEquals(list, l2)
        assertNotEquals(list, mutableIntListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(5)
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[${1}, ${2}, ${3}, ${4}, ${5}]", list.toString())
        assertEquals("[]", mutableIntListOf().toString())
    }

    @Test
    fun joinToString() {
        assertEquals("${1}, ${2}, ${3}, ${4}, ${5}", list.joinToString())
        assertEquals(
            "x${1}, ${2}, ${3}...",
            list.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${1}-${2}-${3}-${4}-${5}<",
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
        val l2 = mutableIntListOf()
        assertEquals(0, l2.size)
        assertEquals(0, l2.count())
        l2 += 1
        assertEquals(1, l2.size)
        assertEquals(1, l2.count())
    }

    @Test
    fun get() {
        assertEquals(1, list[0])
        assertEquals(5, list[4])
        assertEquals(1, list.elementAt(0))
        assertEquals(5, list.elementAt(4))
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
            1,
            list.elementAtOrElse(0) {
                assertEquals(0, it)
                0
            }
        )
        assertEquals(
            0,
            list.elementAtOrElse(-1) {
                assertEquals(-1, it)
                0
            }
        )
        assertEquals(
            0,
            list.elementAtOrElse(5) {
                assertEquals(5, it)
                0
            }
        )
    }

    @Test
    fun count() {
        assertEquals(1, list.count { it < 2 })
        assertEquals(0, list.count { it < 0 })
        assertEquals(5, list.count { it < 10 })
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertFalse(list.none())
        assertTrue(mutableIntListOf().isEmpty())
        assertTrue(mutableIntListOf().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableIntListOf().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableIntListOf().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5 })
        assertTrue(list.any { it == 1 })
        assertFalse(list.any { it == 0 })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableIntListOf()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableIntListOf(5, 4, 3, 2, 1)
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableIntListOf()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(reversedSublist, mutableIntListOf(5, 4))
    }

    @Test
    fun forEach() {
        val copy = mutableIntListOf()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableIntListOf()
        list.forEachReversed { copy += it }
        assertEquals(copy, mutableIntListOf(5, 4, 3, 2, 1))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableIntListOf()
        val indices = mutableIntListOf()
        list.forEachIndexed { index, item ->
            copy += item
            indices += index.toInt()
        }
        assertEquals(list, copy)
        assertEquals(indices, mutableIntListOf(0, 1, 2, 3, 4))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableIntListOf()
        val indices = mutableIntListOf()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += index.toInt()
        }
        assertEquals(copy, mutableIntListOf(5, 4, 3, 2, 1))
        assertEquals(indices, mutableIntListOf(4, 3, 2, 1, 0))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == 1 })
        assertEquals(4, list.indexOfFirst { it == 5 })
        assertEquals(-1, list.indexOfFirst { it == 0 })
        assertEquals(0, mutableIntListOf(8, 8).indexOfFirst { it == 8 })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == 1 })
        assertEquals(4, list.indexOfLast { it == 5 })
        assertEquals(-1, list.indexOfLast { it == 0 })
        assertEquals(1, mutableIntListOf(8, 8).indexOfLast { it == 8 })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5))
        assertTrue(list.contains(1))
        assertFalse(list.contains(0))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(mutableIntListOf(2, 3, 1)))
        assertFalse(list.containsAll(mutableIntListOf(2, 3, 6)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5))
        assertEquals(1, list.lastIndexOf(2))
        val copy = mutableIntListOf()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(1))
    }

    @Test
    fun first() {
        assertEquals(1, list.first())
    }

    @Test
    fun firstException() {
        assertFailsWith(NoSuchElementException::class) { mutableIntListOf().first() }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5, list.first { it == 5 })
        assertEquals(1, mutableIntListOf(1, 5).first { it != 0 })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableIntListOf().first { it == 8 } }
    }

    @Test
    fun last() {
        assertEquals(5, list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) { mutableIntListOf().last() }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1, list.last { it == 1 })
        assertEquals(5, mutableIntListOf(1, 5).last { it != 0 })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableIntListOf().last { it == 8 } }
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
        val l = mutableIntListOf(1, 2, 3)
        l += 4
        l.add(5)
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableIntListOf(2, 4)
        l.add(2, 5)
        l.add(0, 1)
        l.add(2, 3)
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(-1, 2) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(6, 2) }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableIntListOf(4)
        val l2 = mutableIntListOf(1, 2)
        val l3 = mutableIntListOf(5)
        val l4 = mutableIntListOf(3)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableIntListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(6, mutableIntListOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(-1, mutableIntListOf()) }
    }

    @Test
    fun addAllList() {
        val l = MutableIntList()
        l.add(3)
        l.add(4)
        l.add(5)
        val l2 = mutableIntListOf(1, 2)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableIntListOf()))
    }

    @Test
    fun plusAssignList() {
        val l = MutableIntList()
        l.add(3)
        l.add(4)
        l.add(5)
        val l2 = mutableIntListOf(1, 2)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun addAllArrayAtIndex() {
        val a1 = intArrayOf(4)
        val a2 = intArrayOf(1, 2)
        val a3 = intArrayOf(5)
        val l = mutableIntListOf(3)
        assertTrue(l.addAll(1, a3))
        assertTrue(l.addAll(0, a2))
        assertTrue(l.addAll(3, a1))
        assertFalse(l.addAll(0, intArrayOf()))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(6, intArrayOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(-1, intArrayOf()) }
    }

    @Test
    fun addAllArray() {
        val a = intArrayOf(3, 4, 5)
        val v = mutableIntListOf(1, 2)
        v.addAll(a)
        assertEquals(5, v.size)
        assertEquals(3, v[2])
        assertEquals(4, v[3])
        assertEquals(5, v[4])
    }

    @Test
    fun plusAssignArray() {
        val a = intArrayOf(3, 4, 5)
        val v = mutableIntListOf(1, 2)
        v += a
        assertEquals(list, v)
    }

    @Test
    fun clear() {
        val l = mutableIntListOf()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun trim() {
        val l = mutableIntListOf(1)
        l.trim()
        assertEquals(1, l.capacity)
        l += intArrayOf(1, 2, 3, 4, 5)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += intArrayOf(1, 2, 3, 4, 5)
        l -= 5
        l.trim(5)
        assertEquals(5, l.capacity)
        l.trim(4)
        assertEquals(4, l.capacity)
        l.trim(3)
        assertEquals(4, l.capacity)
    }

    @Test
    fun remove() {
        val l = mutableIntListOf(1, 2, 3, 4, 5)
        l.remove(3)
        assertEquals(mutableIntListOf(1, 2, 4, 5), l)
    }

    @Test
    fun removeAt() {
        val l = mutableIntListOf(1, 2, 3, 4, 5)
        l.removeAt(2)
        assertEquals(mutableIntListOf(1, 2, 4, 5), l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(6) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(-1) }
    }

    @Test
    fun set() {
        val l = mutableIntListOf(0, 0, 0, 0, 0)
        l[0] = 1
        l[4] = 5
        l[2] = 3
        l[1] = 2
        l[3] = 4
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.set(-1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { l.set(6, 1) }
        assertEquals(4, l.set(3, 1))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableIntListOf(1)
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(mutableIntListOf(0, 10, 15)))
        val l = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(mutableIntListOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllIntArray() {
        assertFalse(list.removeAll(intArrayOf(0, 10, 15)))
        val l = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(intArrayOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun minusAssignList() {
        val l = mutableIntListOf().also { it += list }
        l -= mutableIntListOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= mutableIntListOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignIntArray() {
        val l = mutableIntListOf().also { it += list }
        l -= intArrayOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= intArrayOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(list.retainAll(mutableIntListOf(1, 2, 3, 4, 5, 6)))
        val l = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(mutableIntListOf(1, 2, 3, 4, 5, 6)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllIntArray() {
        assertFalse(list.retainAll(intArrayOf(1, 2, 3, 4, 5, 6)))
        val l = mutableIntListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(intArrayOf(1, 2, 3, 4, 5, 6)))
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableIntListOf(1, 9, 7, 6, 2, 3, 4, 5)
        l.removeRange(1, 4)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(6, 6) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(100, 200) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(-1, 0) }
        assertFailsWith<IllegalArgumentException> { l.removeRange(3, 2) }
    }

    @Test
    fun sort() {
        val l = mutableIntListOf(1, 4, 2, 5, 3)
        l.sort()
        assertEquals(list, l)
    }

    @Test
    fun sortDescending() {
        val l = mutableIntListOf(1, 4, 2, 5, 3)
        l.sortDescending()
        assertEquals(mutableIntListOf(5, 4, 3, 2, 1), l)
    }

    @Test
    fun sortEmpty() {
        val l = MutableIntList(0)
        l.sort()
        l.sortDescending()
        assertEquals(MutableIntList(0), l)
    }

    @Test
    fun testEmptyIntList() {
        val l = emptyIntList()
        assertEquals(0, l.size)
    }

    @Test
    fun intListOfEmpty() {
        val l = intListOf()
        assertEquals(0, l.size)
    }

    @Test
    fun intListOfOneValue() {
        val l = intListOf(2)
        assertEquals(1, l.size)
        assertEquals(2, l[0])
    }

    @Test
    fun intListOfTwoValues() {
        val l = intListOf(2, 1)
        assertEquals(2, l.size)
        assertEquals(2, l[0])
        assertEquals(1, l[1])
    }

    @Test
    fun intListOfThreeValues() {
        val l = intListOf(2, 10, -1)
        assertEquals(3, l.size)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
    }

    @Test
    fun intListOfFourValues() {
        val l = intListOf(2, 10, -1, 10)
        assertEquals(4, l.size)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
        assertEquals(10, l[3])
    }

    @Test
    fun mutableIntListOfOneValue() {
        val l = mutableIntListOf(2)
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(2, l[0])
    }

    @Test
    fun mutableIntListOfTwoValues() {
        val l = mutableIntListOf(2, 1)
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(2, l[0])
        assertEquals(1, l[1])
    }

    @Test
    fun mutableIntListOfThreeValues() {
        val l = mutableIntListOf(2, 10, -1)
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
    }

    @Test
    fun mutableIntListOfFourValues() {
        val l = mutableIntListOf(2, 10, -1, 10)
        assertEquals(4, l.size)
        assertEquals(4, l.capacity)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
        assertEquals(10, l[3])
    }

    @Test
    fun binarySearchIntList() {
        val l = mutableIntListOf(-2, -1, 2, 10, 10)
        assertEquals(0, l.binarySearch(-2))
        assertEquals(2, l.binarySearch(2))
        assertEquals(3, l.binarySearch(10))

        assertEquals(-1, l.binarySearch(-20))
        assertEquals(-4, l.binarySearch(3))
        assertEquals(-6, l.binarySearch(20))
    }
}
