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

class FloatListTest {
    private val list: MutableFloatList = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)

    @Test
    fun emptyConstruction() {
        val l = mutableFloatListOf()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableFloatList(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableFloatListOf(1f, 2f, 3f)
        assertEquals(3, l.size)
        assertEquals(1f, l[0])
        assertEquals(2f, l[1])
        assertEquals(3f, l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.removeAt(4)
        assertNotEquals(list.hashCode(), l2.hashCode())
        l2.add(5f)
        assertEquals(list.hashCode(), l2.hashCode())
        l2.clear()
        assertNotEquals(list.hashCode(), l2.hashCode())
    }

    @Test
    fun equalsTest() {
        val l2 = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)
        assertEquals(list, l2)
        assertNotEquals(list, mutableFloatListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(5f)
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[${1f}, ${2f}, ${3f}, ${4f}, ${5f}]", list.toString())
        assertEquals("[]", mutableFloatListOf().toString())
    }

    @Test
    fun joinToString() {
        assertEquals("${1f}, ${2f}, ${3f}, ${4f}, ${5f}", list.joinToString())
        assertEquals(
            "x${1f}, ${2f}, ${3f}...",
            list.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${1f}-${2f}-${3f}-${4f}-${5f}<",
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
        val l2 = mutableFloatListOf()
        assertEquals(0, l2.size)
        assertEquals(0, l2.count())
        l2 += 1f
        assertEquals(1, l2.size)
        assertEquals(1, l2.count())
    }

    @Test
    fun get() {
        assertEquals(1f, list[0])
        assertEquals(5f, list[4])
        assertEquals(1f, list.elementAt(0))
        assertEquals(5f, list.elementAt(4))
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
            1f,
            list.elementAtOrElse(0) {
                assertEquals(0, it)
                0f
            }
        )
        assertEquals(
            0f,
            list.elementAtOrElse(-1) {
                assertEquals(-1, it)
                0f
            }
        )
        assertEquals(
            0f,
            list.elementAtOrElse(5) {
                assertEquals(5, it)
                0f
            }
        )
    }

    @Test
    fun count() {
        assertEquals(1, list.count { it < 2f })
        assertEquals(0, list.count { it < 0f })
        assertEquals(5, list.count { it < 10f })
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertFalse(list.none())
        assertTrue(mutableFloatListOf().isEmpty())
        assertTrue(mutableFloatListOf().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableFloatListOf().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableFloatListOf().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5f })
        assertTrue(list.any { it == 1f })
        assertFalse(list.any { it == 0f })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableFloatListOf()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableFloatListOf(5f, 4f, 3f, 2f, 1f)
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableFloatListOf()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(reversedSublist, mutableFloatListOf(5f, 4f))
    }

    @Test
    fun forEach() {
        val copy = mutableFloatListOf()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableFloatListOf()
        list.forEachReversed { copy += it }
        assertEquals(copy, mutableFloatListOf(5f, 4f, 3f, 2f, 1f))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableFloatListOf()
        val indices = mutableFloatListOf()
        list.forEachIndexed { index, item ->
            copy += item
            indices += index.toFloat()
        }
        assertEquals(list, copy)
        assertEquals(indices, mutableFloatListOf(0f, 1f, 2f, 3f, 4f))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableFloatListOf()
        val indices = mutableFloatListOf()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += index.toFloat()
        }
        assertEquals(copy, mutableFloatListOf(5f, 4f, 3f, 2f, 1f))
        assertEquals(indices, mutableFloatListOf(4f, 3f, 2f, 1f, 0f))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == 1f })
        assertEquals(4, list.indexOfFirst { it == 5f })
        assertEquals(-1, list.indexOfFirst { it == 0f })
        assertEquals(0, mutableFloatListOf(8f, 8f).indexOfFirst { it == 8f })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == 1f })
        assertEquals(4, list.indexOfLast { it == 5f })
        assertEquals(-1, list.indexOfLast { it == 0f })
        assertEquals(1, mutableFloatListOf(8f, 8f).indexOfLast { it == 8f })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5f))
        assertTrue(list.contains(1f))
        assertFalse(list.contains(0f))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(mutableFloatListOf(2f, 3f, 1f)))
        assertFalse(list.containsAll(mutableFloatListOf(2f, 3f, 6f)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5f))
        assertEquals(1, list.lastIndexOf(2f))
        val copy = mutableFloatListOf()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(1f))
    }

    @Test
    fun first() {
        assertEquals(1f, list.first())
    }

    @Test
    fun firstException() {
        assertFailsWith(NoSuchElementException::class) { mutableFloatListOf().first() }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5f, list.first { it == 5f })
        assertEquals(1f, mutableFloatListOf(1f, 5f).first { it != 0f })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableFloatListOf().first { it == 8f } }
    }

    @Test
    fun last() {
        assertEquals(5f, list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) { mutableFloatListOf().last() }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1f, list.last { it == 1f })
        assertEquals(5f, mutableFloatListOf(1f, 5f).last { it != 0f })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) { mutableFloatListOf().last { it == 8f } }
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
        val l = mutableFloatListOf(1f, 2f, 3f)
        l += 4f
        l.add(5f)
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableFloatListOf(2f, 4f)
        l.add(2, 5f)
        l.add(0, 1f)
        l.add(2, 3f)
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(-1, 2f) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(6, 2f) }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableFloatListOf(4f)
        val l2 = mutableFloatListOf(1f, 2f)
        val l3 = mutableFloatListOf(5f)
        val l4 = mutableFloatListOf(3f)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableFloatListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(6, mutableFloatListOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(-1, mutableFloatListOf()) }
    }

    @Test
    fun addAllList() {
        val l = MutableFloatList()
        l.add(3f)
        l.add(4f)
        l.add(5f)
        val l2 = mutableFloatListOf(1f, 2f)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableFloatListOf()))
    }

    @Test
    fun plusAssignList() {
        val l = MutableFloatList()
        l.add(3f)
        l.add(4f)
        l.add(5f)
        val l2 = mutableFloatListOf(1f, 2f)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun addAllArrayAtIndex() {
        val a1 = floatArrayOf(4f)
        val a2 = floatArrayOf(1f, 2f)
        val a3 = floatArrayOf(5f)
        val l = mutableFloatListOf(3f)
        assertTrue(l.addAll(1, a3))
        assertTrue(l.addAll(0, a2))
        assertTrue(l.addAll(3, a1))
        assertFalse(l.addAll(0, floatArrayOf()))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(6, floatArrayOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(-1, floatArrayOf()) }
    }

    @Test
    fun addAllArray() {
        val a = floatArrayOf(3f, 4f, 5f)
        val v = mutableFloatListOf(1f, 2f)
        v.addAll(a)
        assertEquals(5, v.size)
        assertEquals(3f, v[2])
        assertEquals(4f, v[3])
        assertEquals(5f, v[4])
    }

    @Test
    fun plusAssignArray() {
        val a = floatArrayOf(3f, 4f, 5f)
        val v = mutableFloatListOf(1f, 2f)
        v += a
        assertEquals(list, v)
    }

    @Test
    fun clear() {
        val l = mutableFloatListOf()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun trim() {
        val l = mutableFloatListOf(1f)
        l.trim()
        assertEquals(1, l.capacity)
        l += floatArrayOf(1f, 2f, 3f, 4f, 5f)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += floatArrayOf(1f, 2f, 3f, 4f, 5f)
        l -= 5f
        l.trim(5)
        assertEquals(5, l.capacity)
        l.trim(4)
        assertEquals(4, l.capacity)
        l.trim(3)
        assertEquals(4, l.capacity)
    }

    @Test
    fun remove() {
        val l = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)
        l.remove(3f)
        assertEquals(mutableFloatListOf(1f, 2f, 4f, 5f), l)
    }

    @Test
    fun removeAt() {
        val l = mutableFloatListOf(1f, 2f, 3f, 4f, 5f)
        l.removeAt(2)
        assertEquals(mutableFloatListOf(1f, 2f, 4f, 5f), l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(6) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(-1) }
    }

    @Test
    fun set() {
        val l = mutableFloatListOf(0f, 0f, 0f, 0f, 0f)
        l[0] = 1f
        l[4] = 5f
        l[2] = 3f
        l[1] = 2f
        l[3] = 4f
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.set(-1, 1f) }
        assertFailsWith<IndexOutOfBoundsException> { l.set(6, 1f) }
        assertEquals(4f, l.set(3, 1f))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableFloatListOf(1f)
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(mutableFloatListOf(0f, 10f, 15f)))
        val l = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f, 5f)
        assertTrue(l.removeAll(mutableFloatListOf(20f, 0f, 15f, 10f, 5f)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllFloatArray() {
        assertFalse(list.removeAll(floatArrayOf(0f, 10f, 15f)))
        val l = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f, 5f)
        assertTrue(l.removeAll(floatArrayOf(20f, 0f, 15f, 10f, 5f)))
        assertEquals(list, l)
    }

    @Test
    fun minusAssignList() {
        val l = mutableFloatListOf().also { it += list }
        l -= mutableFloatListOf(0f, 10f, 15f)
        assertEquals(list, l)
        val l2 = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f, 5f)
        l2 -= mutableFloatListOf(20f, 0f, 15f, 10f, 5f)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignFloatArray() {
        val l = mutableFloatListOf().also { it += list }
        l -= floatArrayOf(0f, 10f, 15f)
        assertEquals(list, l)
        val l2 = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f, 5f)
        l2 -= floatArrayOf(20f, 0f, 15f, 10f, 5f)
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(list.retainAll(mutableFloatListOf(1f, 2f, 3f, 4f, 5f, 6f)))
        val l = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f)
        assertTrue(l.retainAll(mutableFloatListOf(1f, 2f, 3f, 4f, 5f, 6f)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllFloatArray() {
        assertFalse(list.retainAll(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)))
        val l = mutableFloatListOf(0f, 1f, 15f, 10f, 2f, 3f, 4f, 5f, 20f)
        assertTrue(l.retainAll(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)))
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableFloatListOf(1f, 9f, 7f, 6f, 2f, 3f, 4f, 5f)
        l.removeRange(1, 4)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(6, 6) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(100, 200) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(-1, 0) }
        assertFailsWith<IllegalArgumentException> { l.removeRange(3, 2) }
    }

    @Test
    fun sort() {
        val l = mutableFloatListOf(1f, 4f, 2f, 5f, 3f)
        l.sort()
        assertEquals(list, l)
    }

    @Test
    fun sortDescending() {
        val l = mutableFloatListOf(1f, 4f, 2f, 5f, 3f)
        l.sortDescending()
        assertEquals(mutableFloatListOf(5f, 4f, 3f, 2f, 1f), l)
    }

    @Test
    fun sortEmpty() {
        val l = MutableFloatList(0)
        l.sort()
        l.sortDescending()
        assertEquals(MutableFloatList(0), l)
    }

    @Test
    fun testEmptyFloatList() {
        val l = emptyFloatList()
        assertEquals(0, l.size)
    }

    @Test
    fun floatListOfEmpty() {
        val l = floatListOf()
        assertEquals(0, l.size)
    }

    @Test
    fun floatListOfOneValue() {
        val l = floatListOf(2f)
        assertEquals(1, l.size)
        assertEquals(2f, l[0])
    }

    @Test
    fun floatListOfTwoValues() {
        val l = floatListOf(2f, 1f)
        assertEquals(2, l.size)
        assertEquals(2f, l[0])
        assertEquals(1f, l[1])
    }

    @Test
    fun floatListOfThreeValues() {
        val l = floatListOf(2f, 10f, -1f)
        assertEquals(3, l.size)
        assertEquals(2f, l[0])
        assertEquals(10f, l[1])
        assertEquals(-1f, l[2])
    }

    @Test
    fun floatListOfFourValues() {
        val l = floatListOf(2f, 10f, -1f, 10f)
        assertEquals(4, l.size)
        assertEquals(2f, l[0])
        assertEquals(10f, l[1])
        assertEquals(-1f, l[2])
        assertEquals(10f, l[3])
    }

    @Test
    fun mutableFloatListOfOneValue() {
        val l = mutableFloatListOf(2f)
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(2f, l[0])
    }

    @Test
    fun mutableFloatListOfTwoValues() {
        val l = mutableFloatListOf(2f, 1f)
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(2f, l[0])
        assertEquals(1f, l[1])
    }

    @Test
    fun mutableFloatListOfThreeValues() {
        val l = mutableFloatListOf(2f, 10f, -1f)
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(2f, l[0])
        assertEquals(10f, l[1])
        assertEquals(-1f, l[2])
    }

    @Test
    fun mutableFloatListOfFourValues() {
        val l = mutableFloatListOf(2f, 10f, -1f, 10f)
        assertEquals(4, l.size)
        assertEquals(4, l.capacity)
        assertEquals(2f, l[0])
        assertEquals(10f, l[1])
        assertEquals(-1f, l[2])
        assertEquals(10f, l[3])
    }

    @Test
    fun binarySearchFloatList() {
        val l = mutableFloatListOf(-2f, -1f, 2f, 10f, 10f)
        assertEquals(0, l.binarySearch(-2))
        assertEquals(2, l.binarySearch(2))
        assertEquals(3, l.binarySearch(10))

        assertEquals(-1, l.binarySearch(-20))
        assertEquals(-4, l.binarySearch(3))
        assertEquals(-6, l.binarySearch(20))
    }
}
