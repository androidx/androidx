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

class DoubleListTest {
    private val list: MutableDoubleList = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)

    @Test
    fun emptyConstruction() {
        val l = mutableDoubleListOf()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableDoubleList(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableDoubleListOf(1.0, 2.0, 3.0)
        assertEquals(3, l.size)
        assertEquals(1.0, l[0])
        assertEquals(2.0, l[1])
        assertEquals(3.0, l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.removeAt(4)
        assertNotEquals(list.hashCode(), l2.hashCode())
        l2.add(5.0)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.clear()
        assertNotEquals(list.hashCode(), l2.hashCode())
    }

    @Test
    fun equalsTest() {
        val l2 = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)
        assertEquals(list, l2)
        assertNotEquals(list, mutableDoubleListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(5.0)
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[${1.0}, ${2.0}, ${3.0}, ${4.0}, ${5.0}]", list.toString())
        assertEquals("[]", mutableDoubleListOf().toString())
    }

    @Test
    fun joinToString() {
        assertEquals("${1.0}, ${2.0}, ${3.0}, ${4.0}, ${5.0}", list.joinToString())
        assertEquals(
            "x${1.0}, ${2.0}, ${3.0}...",
            list.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${1.0}-${2.0}-${3.0}-${4.0}-${5.0}<",
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
        val l2 = mutableDoubleListOf()
        assertEquals(0, l2.size)
        assertEquals(0, l2.count())
        l2 += 1.0
        assertEquals(1, l2.size)
        assertEquals(1, l2.count())
    }

    @Test
    fun get() {
        assertEquals(1.0, list[0])
        assertEquals(5.0, list[4])
        assertEquals(1.0, list.elementAt(0))
        assertEquals(5.0, list.elementAt(4))
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
            1.0,
            list.elementAtOrElse(0) {
                assertEquals(0, it)
                0.0
            }
        )
        assertEquals(
            0.0,
            list.elementAtOrElse(-1) {
                assertEquals(-1, it)
                0.0
            }
        )
        assertEquals(
            0.0,
            list.elementAtOrElse(5) {
                assertEquals(5, it)
                0.0
            }
        )
    }

    @Test
    fun count() {
        assertEquals(1, list.count { it < 2.0 })
        assertEquals(0, list.count { it < 0.0 })
        assertEquals(5, list.count { it < 10.0 })
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertFalse(list.none())
        assertTrue(mutableDoubleListOf().isEmpty())
        assertTrue(mutableDoubleListOf().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableDoubleListOf().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableDoubleListOf().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5.0 })
        assertTrue(list.any { it == 1.0 })
        assertFalse(list.any { it == 0.0 })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableDoubleListOf()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableDoubleListOf(5.0, 4.0, 3.0, 2.0, 1.0)
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableDoubleListOf()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(reversedSublist, mutableDoubleListOf(5.0, 4.0))
    }

    @Test
    fun forEach() {
        val copy = mutableDoubleListOf()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableDoubleListOf()
        list.forEachReversed { copy += it }
        assertEquals(copy, mutableDoubleListOf(5.0, 4.0, 3.0, 2.0, 1.0))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableDoubleListOf()
        val indices = mutableDoubleListOf()
        list.forEachIndexed { index, item ->
            copy += item
            indices += index.toDouble()
        }
        assertEquals(list, copy)
        assertEquals(indices, mutableDoubleListOf(0.0, 1.0, 2.0, 3.0, 4.0))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableDoubleListOf()
        val indices = mutableDoubleListOf()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += index.toDouble()
        }
        assertEquals(copy, mutableDoubleListOf(5.0, 4.0, 3.0, 2.0, 1.0))
        assertEquals(indices, mutableDoubleListOf(4.0, 3.0, 2.0, 1.0, 0.0))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == 1.0 })
        assertEquals(4, list.indexOfFirst { it == 5.0 })
        assertEquals(-1, list.indexOfFirst { it == 0.0 })
        assertEquals(0, mutableDoubleListOf(8.0, 8.0).indexOfFirst { it == 8.0 })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == 1.0 })
        assertEquals(4, list.indexOfLast { it == 5.0 })
        assertEquals(-1, list.indexOfLast { it == 0.0 })
        assertEquals(1, mutableDoubleListOf(8.0, 8.0).indexOfLast { it == 8.0 })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5.0))
        assertTrue(list.contains(1.0))
        assertFalse(list.contains(0.0))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(mutableDoubleListOf(2.0, 3.0, 1.0)))
        assertFalse(list.containsAll(mutableDoubleListOf(2.0, 3.0, 6.0)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5.0))
        assertEquals(1, list.lastIndexOf(2.0))
        val copy = mutableDoubleListOf()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(1.0))
    }

    @Test
    fun first() {
        assertEquals(1.0, list.first())
    }

    @Test
    fun firstException() {
        assertFailsWith(NoSuchElementException::class) { mutableDoubleListOf().first() }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5.0, list.first { it == 5.0 })
        assertEquals(1.0, mutableDoubleListOf(1.0, 5.0).first { it != 0.0 })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableDoubleListOf().first { it == 8.0 } }
    }

    @Test
    fun last() {
        assertEquals(5.0, list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) { mutableDoubleListOf().last() }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1.0, list.last { it == 1.0 })
        assertEquals(5.0, mutableDoubleListOf(1.0, 5.0).last { it != 0.0 })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableDoubleListOf().last { it == 8.0 } }
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
        val l = mutableDoubleListOf(1.0, 2.0, 3.0)
        l += 4.0
        l.add(5.0)
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableDoubleListOf(2.0, 4.0)
        l.add(2, 5.0)
        l.add(0, 1.0)
        l.add(2, 3.0)
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(-1, 2.0) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(6, 2.0) }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableDoubleListOf(4.0)
        val l2 = mutableDoubleListOf(1.0, 2.0)
        val l3 = mutableDoubleListOf(5.0)
        val l4 = mutableDoubleListOf(3.0)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableDoubleListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(6, mutableDoubleListOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(-1, mutableDoubleListOf()) }
    }

    @Test
    fun addAllList() {
        val l = MutableDoubleList()
        l.add(3.0)
        l.add(4.0)
        l.add(5.0)
        val l2 = mutableDoubleListOf(1.0, 2.0)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableDoubleListOf()))
    }

    @Test
    fun plusAssignList() {
        val l = MutableDoubleList()
        l.add(3.0)
        l.add(4.0)
        l.add(5.0)
        val l2 = mutableDoubleListOf(1.0, 2.0)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun addAllArrayAtIndex() {
        val a1 = doubleArrayOf(4.0)
        val a2 = doubleArrayOf(1.0, 2.0)
        val a3 = doubleArrayOf(5.0)
        val l = mutableDoubleListOf(3.0)
        assertTrue(l.addAll(1, a3))
        assertTrue(l.addAll(0, a2))
        assertTrue(l.addAll(3, a1))
        assertFalse(l.addAll(0, doubleArrayOf()))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(6, doubleArrayOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(-1, doubleArrayOf()) }
    }

    @Test
    fun addAllArray() {
        val a = doubleArrayOf(3.0, 4.0, 5.0)
        val v = mutableDoubleListOf(1.0, 2.0)
        v.addAll(a)
        assertEquals(5, v.size)
        assertEquals(3.0, v[2])
        assertEquals(4.0, v[3])
        assertEquals(5.0, v[4])
    }

    @Test
    fun plusAssignArray() {
        val a = doubleArrayOf(3.0, 4.0, 5.0)
        val v = mutableDoubleListOf(1.0, 2.0)
        v += a
        assertEquals(list, v)
    }

    @Test
    fun clear() {
        val l = mutableDoubleListOf()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun trim() {
        val l = mutableDoubleListOf(1.0)
        l.trim()
        assertEquals(1, l.capacity)
        l += doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        l -= 5.0
        l.trim(5)
        assertEquals(5, l.capacity)
        l.trim(4)
        assertEquals(4, l.capacity)
        l.trim(3)
        assertEquals(4, l.capacity)
    }

    @Test
    fun remove() {
        val l = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)
        l.remove(3.0)
        assertEquals(mutableDoubleListOf(1.0, 2.0, 4.0, 5.0), l)
    }

    @Test
    fun removeAt() {
        val l = mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0)
        l.removeAt(2)
        assertEquals(mutableDoubleListOf(1.0, 2.0, 4.0, 5.0), l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(6) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(-1) }
    }

    @Test
    fun set() {
        val l = mutableDoubleListOf(0.0, 0.0, 0.0, 0.0, 0.0)
        l[0] = 1.0
        l[4] = 5.0
        l[2] = 3.0
        l[1] = 2.0
        l[3] = 4.0
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.set(-1, 1.0) }
        assertFailsWith<IndexOutOfBoundsException> { l.set(6, 1.0) }
        assertEquals(4.0, l.set(3, 1.0))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableDoubleListOf(1.0)
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(mutableDoubleListOf(0.0, 10.0, 15.0)))
        val l = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0, 5.0)
        assertTrue(l.removeAll(mutableDoubleListOf(20.0, 0.0, 15.0, 10.0, 5.0)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllDoubleArray() {
        assertFalse(list.removeAll(doubleArrayOf(0.0, 10.0, 15.0)))
        val l = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0, 5.0)
        assertTrue(l.removeAll(doubleArrayOf(20.0, 0.0, 15.0, 10.0, 5.0)))
        assertEquals(list, l)
    }

    @Test
    fun minusAssignList() {
        val l = mutableDoubleListOf().also { it += list }
        l -= mutableDoubleListOf(0.0, 10.0, 15.0)
        assertEquals(list, l)
        val l2 = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0, 5.0)
        l2 -= mutableDoubleListOf(20.0, 0.0, 15.0, 10.0, 5.0)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignDoubleArray() {
        val l = mutableDoubleListOf().also { it += list }
        l -= doubleArrayOf(0.0, 10.0, 15.0)
        assertEquals(list, l)
        val l2 = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0, 5.0)
        l2 -= doubleArrayOf(20.0, 0.0, 15.0, 10.0, 5.0)
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(list.retainAll(mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)))
        val l = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0)
        assertTrue(l.retainAll(mutableDoubleListOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllDoubleArray() {
        assertFalse(list.retainAll(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)))
        val l = mutableDoubleListOf(0.0, 1.0, 15.0, 10.0, 2.0, 3.0, 4.0, 5.0, 20.0)
        assertTrue(l.retainAll(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)))
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableDoubleListOf(1.0, 9.0, 7.0, 6.0, 2.0, 3.0, 4.0, 5.0)
        l.removeRange(1, 4)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(6, 6) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(100, 200) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(-1, 0) }
        assertFailsWith<IllegalArgumentException> { l.removeRange(3, 2) }
    }

    @Test
    fun sort() {
        val l = mutableDoubleListOf(1.0, 4.0, 2.0, 5.0, 3.0)
        l.sort()
        assertEquals(list, l)
    }

    @Test
    fun sortDescending() {
        val l = mutableDoubleListOf(1.0, 4.0, 2.0, 5.0, 3.0)
        l.sortDescending()
        assertEquals(mutableDoubleListOf(5.0, 4.0, 3.0, 2.0, 1.0), l)
    }

    @Test
    fun sortEmpty() {
        val l = MutableDoubleList(0)
        l.sort()
        l.sortDescending()
        assertEquals(MutableDoubleList(0), l)
    }

    @Test
    fun testEmptyDoubleList() {
        val l = emptyDoubleList()
        assertEquals(0, l.size)
    }

    @Test
    fun doubleListOfEmpty() {
        val l = doubleListOf()
        assertEquals(0, l.size)
    }

    @Test
    fun doubleListOfOneValue() {
        val l = doubleListOf(2.0)
        assertEquals(1, l.size)
        assertEquals(2.0, l[0])
    }

    @Test
    fun doubleListOfTwoValues() {
        val l = doubleListOf(2.0, 1.0)
        assertEquals(2, l.size)
        assertEquals(2.0, l[0])
        assertEquals(1.0, l[1])
    }

    @Test
    fun doubleListOfThreeValues() {
        val l = doubleListOf(2.0, 10.0, -1.0)
        assertEquals(3, l.size)
        assertEquals(2.0, l[0])
        assertEquals(10.0, l[1])
        assertEquals(-1.0, l[2])
    }

    @Test
    fun doubleListOfFourValues() {
        val l = doubleListOf(2.0, 10.0, -1.0, 10.0)
        assertEquals(4, l.size)
        assertEquals(2.0, l[0])
        assertEquals(10.0, l[1])
        assertEquals(-1.0, l[2])
        assertEquals(10.0, l[3])
    }

    @Test
    fun mutableDoubleListOfOneValue() {
        val l = mutableDoubleListOf(2.0)
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(2.0, l[0])
    }

    @Test
    fun mutableDoubleListOfTwoValues() {
        val l = mutableDoubleListOf(2.0, 1.0)
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(2.0, l[0])
        assertEquals(1.0, l[1])
    }

    @Test
    fun mutableDoubleListOfThreeValues() {
        val l = mutableDoubleListOf(2.0, 10.0, -1.0)
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(2.0, l[0])
        assertEquals(10.0, l[1])
        assertEquals(-1.0, l[2])
    }

    @Test
    fun mutableDoubleListOfFourValues() {
        val l = mutableDoubleListOf(2.0, 10.0, -1.0, 10.0)
        assertEquals(4, l.size)
        assertEquals(4, l.capacity)
        assertEquals(2.0, l[0])
        assertEquals(10.0, l[1])
        assertEquals(-1.0, l[2])
        assertEquals(10.0, l[3])
    }

    @Test
    fun binarySearchDoubleList() {
        val l = mutableDoubleListOf(-2.0, -1.0, 2.0, 10.0, 10.0)
        assertEquals(0, l.binarySearch(-2))
        assertEquals(2, l.binarySearch(2))
        assertEquals(3, l.binarySearch(10))

        assertEquals(-1, l.binarySearch(-20))
        assertEquals(-4, l.binarySearch(3))
        assertEquals(-6, l.binarySearch(20))
    }
}
