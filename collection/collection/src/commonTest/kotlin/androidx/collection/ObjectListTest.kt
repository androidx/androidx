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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObjectListTest {
    private val list: MutableObjectList<Int> = mutableObjectListOf(1, 2, 3, 4, 5)

    @Test
    fun emptyConstruction() {
        val l = mutableObjectListOf<Int>()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableObjectList<Int>(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableObjectListOf(1, 2, 3)
        assertEquals(3, l.size)
        assertEquals(1, l[0])
        assertEquals(2, l[1])
        assertEquals(3, l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableObjectListOf(1, 2, 3, 4, 5)
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableObjectListOf(1, 2, 3, 4, 5)
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
        val l2 = mutableObjectListOf(1, 2, 3, 4, 5)
        assertEquals(list, l2)
        assertNotEquals(list, mutableObjectListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(5)
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[1, 2, 3, 4, 5]", list.toString())
        assertEquals("[]", mutableObjectListOf<Int>().toString())
        val weirdList = MutableObjectList<Any>()
        weirdList.add(weirdList)
        assertEquals("[(this)]", weirdList.toString())
    }

    @Test
    fun joinToString() {
        assertEquals("1, 2, 3, 4, 5", list.joinToString())
        assertEquals("x1, 2, 3...", list.joinToString(prefix = "x", postfix = "y", limit = 3))
        assertEquals(">1-2-3-4-5<", list.joinToString(separator = "-", prefix = ">", postfix = "<"))
        assertEquals(
            "one, two, three...",
            list.joinToString(limit = 3) {
                when (it) {
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
        val l2 = mutableObjectListOf<Int>()
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
        assertTrue(mutableObjectListOf<Int>().isEmpty())
        assertTrue(mutableObjectListOf<Int>().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableObjectListOf<Int>().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableObjectListOf<Int>().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == 5 })
        assertTrue(list.any { it == 1 })
        assertFalse(list.any { it == 0 })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableObjectListOf<Int>()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableObjectListOf(5, 4, 3, 2, 1)
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableObjectListOf<Int>()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(reversedSublist, mutableObjectListOf(5, 4))
    }

    @Test
    fun forEach() {
        val copy = mutableObjectListOf<Int>()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableObjectListOf<Int>()
        list.forEachReversed { copy += it }
        assertEquals(copy, mutableObjectListOf(5, 4, 3, 2, 1))
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableObjectListOf<Int>()
        val indices = mutableObjectListOf<Int>()
        list.forEachIndexed { index, open ->
            copy += open
            indices += index
        }
        assertEquals(list, copy)
        assertEquals(indices, mutableObjectListOf(0, 1, 2, 3, 4))
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableObjectListOf<Int>()
        val indices = mutableObjectListOf<Int>()
        list.forEachReversedIndexed { index, open ->
            copy += open
            indices += index
        }
        assertEquals(copy, mutableObjectListOf(5, 4, 3, 2, 1))
        assertEquals(indices, mutableObjectListOf(4, 3, 2, 1, 0))
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it < 2 })
        assertEquals(4, list.indexOfFirst { it > 4 })
        assertEquals(-1, list.indexOfFirst { it < 0 })
        assertEquals(0, mutableObjectListOf(8, 8).indexOfFirst { it > 7 })
    }

    @Test
    fun firstOrNullNoParam() {
        assertEquals(1, list.firstOrNull())
        assertNull(emptyObjectList<Int>().firstOrNull())
    }

    @Test
    fun firstOrNull() {
        assertEquals(1, list.firstOrNull { it < 5 })
        assertEquals(3, list.firstOrNull { it > 2 })
        assertEquals(5, list.firstOrNull { it > 4 })
        assertNull(list.firstOrNull { it > 5 })
    }

    @Test
    fun lastOrNullNoParam() {
        assertEquals(5, list.lastOrNull())
        assertNull(emptyObjectList<Int>().lastOrNull())
    }

    @Test
    fun lastOrNull() {
        assertEquals(4, list.lastOrNull { it < 5 })
        assertEquals(5, list.lastOrNull { it > 2 })
        assertEquals(1, list.lastOrNull { it < 2 })
        assertNull(list.firstOrNull { it > 5 })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it < 2 })
        assertEquals(4, list.indexOfLast { it > 4 })
        assertEquals(-1, list.indexOfLast { it < 0 })
        assertEquals(1, objectListOf(8, 8).indexOfLast { it > 7 })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(5))
        assertTrue(list.contains(1))
        assertFalse(list.contains(0))
    }

    @Test
    fun containsAllList() {
        assertTrue(list.containsAll(mutableObjectListOf(2, 3, 1)))
        assertFalse(list.containsAll(mutableObjectListOf(2, 3, 6)))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(5))
        assertEquals(1, list.lastIndexOf(2))
        val copy = mutableObjectListOf<Int>()
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
        assertFailsWith(NoSuchElementException::class) { mutableObjectListOf<Int>().first() }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(5, list.first { it > 4 })
        assertEquals(1, mutableObjectListOf(1, 5).first { it > 0 })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) {
            mutableObjectListOf<Int>().first { it > 8 }
        }
    }

    @Test
    fun last() {
        assertEquals(5, list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) { mutableObjectListOf<Int>().last() }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(1, list.last { it < 2 })
        assertEquals(5, objectListOf(1, 5).last { it > 0 })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith<NoSuchElementException> { objectListOf(2).last { it > 2 } }
    }

    @Test
    fun fold() {
        assertEquals("12345", list.fold("") { acc, i -> acc + i.toString() })
    }

    @Test
    fun foldIndexed() {
        assertEquals("01-12-23-34-45-", list.foldIndexed("") { index, acc, i -> "$acc$index$i-" })
    }

    @Test
    fun foldRight() {
        assertEquals("54321", list.foldRight("") { i, acc -> acc + i.toString() })
    }

    @Test
    fun foldRightIndexed() {
        assertEquals(
            "45-34-23-12-01-",
            list.foldRightIndexed("") { index, i, acc -> "$acc$index$i-" }
        )
    }

    @Test
    fun add() {
        val l = mutableObjectListOf(1, 2, 3)
        l += 4
        l.add(5)
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableObjectListOf(2, 4)
        l.add(2, 5)
        l.add(0, 1)
        l.add(2, 3)
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(-1, 2) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.add(6, 2) }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableObjectListOf(4)
        val l2 = mutableObjectListOf(1, 2)
        val l3 = mutableObjectListOf(5)
        val l4 = mutableObjectListOf(3)
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableObjectListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(6, mutableObjectListOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l4.addAll(-1, mutableObjectListOf()) }
    }

    @Test
    fun addAllObjectList() {
        val l = MutableObjectList<Int>()
        l.add(3)
        l.add(4)
        l.add(5)
        val l2 = mutableObjectListOf(1, 2)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableObjectListOf()))
    }

    @Test
    fun addAllList() {
        val l = listOf(3, 4, 5)
        val l2 = mutableObjectListOf(1, 2)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableObjectListOf()))
    }

    @Test
    fun addAllIterable() {
        val l = listOf(3, 4, 5) as Iterable<Int>
        val l2 = mutableObjectListOf(1, 2)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableObjectListOf()))
    }

    @Test
    fun addAllSequence() {
        val l = listOf(3, 4, 5).asSequence()
        val l2 = mutableObjectListOf(1, 2)
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableObjectListOf()))
    }

    @Test
    fun plusAssignObjectList() {
        val l = objectListOf(3, 4, 5)
        val l2 = mutableObjectListOf(1, 2)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun plusAssignIterable() {
        val l = listOf(3, 4, 5) as Iterable<Int>
        val l2 = mutableObjectListOf(1, 2)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun plusAssignSequence() {
        val l = arrayOf(3, 4, 5).asSequence()
        val l2 = mutableObjectListOf(1, 2)
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun addAllArrayAtIndex() {
        val a1 = arrayOf(4)
        val a2 = arrayOf(1, 2)
        val a3 = arrayOf(5)
        val l = mutableObjectListOf(3)
        assertTrue(l.addAll(1, a3))
        assertTrue(l.addAll(0, a2))
        assertTrue(l.addAll(3, a1))
        assertFalse(l.addAll(0, arrayOf()))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(6, arrayOf()) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.addAll(-1, arrayOf()) }
    }

    @Test
    fun addAllArray() {
        val a = arrayOf(3, 4, 5)
        val v = mutableObjectListOf(1, 2)
        v.addAll(a)
        assertEquals(5, v.size)
        assertEquals(3, v[2])
        assertEquals(4, v[3])
        assertEquals(5, v[4])
    }

    @Test
    fun plusAssignArray() {
        val a = arrayOf(3, 4, 5)
        val v = mutableObjectListOf(1, 2)
        v += a
        assertEquals(list, v)
    }

    @Test
    fun clear() {
        val l = mutableObjectListOf<Int>()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
        repeat(5) { index -> assertNull(l.content[index]) }
    }

    @Test
    fun trim() {
        val l = mutableObjectListOf(1)
        l.trim()
        assertEquals(1, l.capacity)
        l += arrayOf(1, 2, 3, 4, 5)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += arrayOf(1, 2, 3, 4, 5)
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
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        l.remove(3)
        assertEquals(mutableObjectListOf(1, 2, 4, 5), l)
    }

    @Test
    fun removeIf() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5, 6)
        l.removeIf { it == 100 }
        assertEquals(objectListOf(1, 2, 3, 4, 5, 6), l)
        l.removeIf { it % 2 == 0 }
        assertEquals(objectListOf(1, 3, 5), l)
        repeat(3) { assertNull(l.content[3 + it]) }
        l.removeIf { it != 3 }
        assertEquals(objectListOf(3), l)
    }

    @Test
    fun removeAt() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        l.removeAt(2)
        assertNull(l.content[4])
        assertEquals(mutableObjectListOf(1, 2, 4, 5), l)
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(6) }
        assertFailsWith(IndexOutOfBoundsException::class) { l.removeAt(-1) }
    }

    @Test
    fun set() {
        val l = mutableObjectListOf(0, 0, 0, 0, 0)
        l[0] = 1
        l[4] = 5
        l[2] = 3
        l[1] = 2
        l[3] = 4
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l[-1] = 1 }
        assertFailsWith<IndexOutOfBoundsException> { l[6] = 1 }
        assertEquals(4, l.set(3, 1))
    }

    @Test
    fun ensureCapacity() {
        val l = mutableObjectListOf(1)
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllObjectList() {
        assertFalse(list.removeAll(mutableObjectListOf(0, 10, 15)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(mutableObjectListOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllScatterSet() {
        assertFalse(list.removeAll(scatterSetOf(0, 10, 15)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(scatterSetOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllArray() {
        assertFalse(list.removeAll(arrayOf(0, 10, 15)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(arrayOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllList() {
        assertFalse(list.removeAll(listOf(0, 10, 15)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(listOf(20, 0, 15, 10, 5)))
        assertEquals(list, l)
    }

    @Test
    fun removeAllIterable() {
        assertFalse(list.removeAll(listOf(0, 10, 15) as Iterable<Int>))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(listOf(20, 0, 15, 10, 5) as Iterable<Int>))
        assertEquals(list, l)
    }

    @Test
    fun removeAllSequence() {
        assertFalse(list.removeAll(listOf(0, 10, 15).asSequence()))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        assertTrue(l.removeAll(listOf(20, 0, 15, 10, 5).asSequence()))
        assertEquals(list, l)
    }

    @Test
    fun minusAssignObjectList() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= mutableObjectListOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= mutableObjectListOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignScatterSet() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= scatterSetOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= scatterSetOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignArray() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= arrayOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= arrayOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignList() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= listOf(0, 10, 15)
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= listOf(20, 0, 15, 10, 5)
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignIterable() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= listOf(0, 10, 15) as Iterable<Int>
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= listOf(20, 0, 15, 10, 5) as Iterable<Int>
        assertEquals(list, l2)
    }

    @Test
    fun minusAssignSequence() {
        val l = mutableObjectListOf<Int>().also { it += list }
        l -= listOf(0, 10, 15).asSequence()
        assertEquals(list, l)
        val l2 = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20, 5)
        l2 -= listOf(20, 0, 15, 10, 5).asSequence()
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(list.retainAll(mutableObjectListOf(1, 2, 3, 4, 5, 6)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(mutableObjectListOf(1, 2, 3, 4, 5, 6)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllArray() {
        assertFalse(list.retainAll(arrayOf(1, 2, 3, 4, 5, 6)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(arrayOf(1, 2, 3, 4, 5, 6)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllCollection() {
        assertFalse(list.retainAll(listOf(1, 2, 3, 4, 5, 6)))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(listOf(1, 2, 3, 4, 5, 6)))
        assertEquals(list, l)
    }

    @Test
    fun retainAllIterable() {
        assertFalse(list.retainAll(listOf(1, 2, 3, 4, 5, 6) as Iterable<Int>))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(listOf(1, 2, 3, 4, 5, 6) as Iterable<Int>))
        assertEquals(list, l)
    }

    @Test
    fun retainAllSequence() {
        assertFalse(list.retainAll(arrayOf(1, 2, 3, 4, 5, 6).asSequence()))
        val l = mutableObjectListOf(0, 1, 15, 10, 2, 3, 4, 5, 20)
        assertTrue(l.retainAll(arrayOf(1, 2, 3, 4, 5, 6).asSequence()))
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableObjectListOf(1, 9, 7, 6, 2, 3, 4, 5)
        l.removeRange(1, 4)
        assertNull(l.content[5])
        assertNull(l.content[6])
        assertNull(l.content[7])
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(6, 6) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(100, 200) }
        assertFailsWith<IndexOutOfBoundsException> { l.removeRange(-1, 0) }
        assertFailsWith<IllegalArgumentException> { l.removeRange(3, 2) }
    }

    @Test
    fun testEmptyObjectList() {
        val l = emptyObjectList<Int>()
        assertEquals(0, l.size)
    }

    @Test
    fun objectListOfEmpty() {
        val l = objectListOf<Int>()
        assertEquals(0, l.size)
    }

    @Test
    fun objectListOfOneValue() {
        val l = objectListOf(2)
        assertEquals(1, l.size)
        assertEquals(2, l[0])
    }

    @Test
    fun objectListOfTwoValues() {
        val l = objectListOf(2, 1)
        assertEquals(2, l.size)
        assertEquals(2, l[0])
        assertEquals(1, l[1])
    }

    @Test
    fun objectListOfThreeValues() {
        val l = objectListOf(2, 10, -1)
        assertEquals(3, l.size)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
    }

    @Test
    fun objectListOfFourValues() {
        val l = objectListOf(2, 10, -1, 10)
        assertEquals(4, l.size)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
        assertEquals(10, l[3])
    }

    @Test
    fun mutableObjectListOfOneValue() {
        val l = mutableObjectListOf(2)
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(2, l[0])
    }

    @Test
    fun mutableObjectListOfTwoValues() {
        val l = mutableObjectListOf(2, 1)
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(2, l[0])
        assertEquals(1, l[1])
    }

    @Test
    fun mutableObjectListOfThreeValues() {
        val l = mutableObjectListOf(2, 10, -1)
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
    }

    @Test
    fun mutableObjectListOfFourValues() {
        val l = mutableObjectListOf(2, 10, -1, 10)
        assertEquals(4, l.size)
        assertEquals(4, l.capacity)
        assertEquals(2, l[0])
        assertEquals(10, l[1])
        assertEquals(-1, l[2])
        assertEquals(10, l[3])
    }

    @Test
    fun iterator() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val iterator = l.asMutableList().iterator()
        assertTrue(iterator.hasNext())
        assertEquals(1, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(2, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(3, iterator.next())
        assertTrue(iterator.hasNext())
        iterator.remove()
        assertTrue(iterator.hasNext())
        assertEquals(l, mutableObjectListOf(1, 2, 4, 5))

        assertEquals(4, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(5, iterator.next())
        assertFalse(iterator.hasNext())
        iterator.remove()
        assertEquals(l, mutableObjectListOf(1, 2, 4))
    }

    @Test
    fun listIterator() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val iterator = l.asMutableList().listIterator()
        assertEquals(1, iterator.next())
        assertEquals(1, iterator.previous())
        assertEquals(0, iterator.nextIndex())
        iterator.add(6)
        assertEquals(1, iterator.nextIndex())
        assertEquals(0, iterator.previousIndex())
        assertEquals(6, iterator.previous())
        assertEquals(l, mutableObjectListOf(6, 1, 2, 3, 4, 5))
    }

    @Test
    fun listIteratorInitialIndex() {
        val iterator = list.asMutableList().listIterator(2)
        assertEquals(2, iterator.nextIndex())
    }

    @Test
    fun sortList() {
        val list = mutableObjectListOf("0", "11", "33333", "2222")
        list.asMutableList().sortWith { o1, o2 -> o1.length - o2.length }
        assertEquals(objectListOf("0", "11", "2222", "33333"), list)
    }

    @Test
    fun sortSubList() {
        val list = mutableObjectListOf("0", "11", "33333", "2222")
        list.asMutableList().subList(1, 4).sortWith { o1, o2 -> o1.length - o2.length }
        assertEquals(objectListOf("0", "11", "2222", "33333"), list)
    }

    @Test
    fun subList() {
        val l = list.asMutableList().subList(1, 4)
        assertEquals(3, l.size)
        assertEquals(2, l[0])
        assertEquals(3, l[1])
        assertEquals(4, l[2])
    }

    @Test
    fun subListContains() {
        val l = list.asMutableList().subList(1, 4)
        assertTrue(l.contains(2))
        assertTrue(l.contains(3))
        assertTrue(l.contains(4))
        assertFalse(l.contains(5))
        assertFalse(l.contains(1))
    }

    @Test
    fun subListContainsAll() {
        val l = list.asMutableList().subList(1, 4)
        val smallList = listOf(2, 3, 4)
        assertTrue(l.containsAll(smallList))
        val largeList = listOf(3, 4, 5)
        assertFalse(l.containsAll(largeList))
    }

    @Test
    fun subListIndexOf() {
        val l = list.asMutableList().subList(1, 4)
        assertEquals(0, l.indexOf(2))
        assertEquals(2, l.indexOf(4))
        assertEquals(-1, l.indexOf(1))
        val l2 = mutableObjectListOf(2, 1, 1, 3).asMutableList().subList(1, 2)
        assertEquals(0, l2.indexOf(1))
    }

    @Test
    fun subListIsEmpty() {
        val l = list.asMutableList().subList(1, 4)
        assertFalse(l.isEmpty())
        assertTrue(list.asMutableList().subList(4, 4).isEmpty())
    }

    @Test
    fun subListIterator() {
        val l = list.asMutableList().subList(1, 4)
        val l2 = mutableListOf<Int>()
        l.forEach { l2 += it }
        assertEquals(3, l2.size)
        assertEquals(2, l2[0])
        assertEquals(3, l2[1])
        assertEquals(4, l2[2])
    }

    @Test
    fun subListLastIndexOf() {
        val l = list.asMutableList().subList(1, 4)
        assertEquals(0, l.lastIndexOf(2))
        assertEquals(2, l.lastIndexOf(4))
        assertEquals(-1, l.lastIndexOf(1))
        val l2 = mutableObjectListOf(2, 1, 1, 3).asMutableList().subList(1, 3)
        assertEquals(1, l2.lastIndexOf(1))
    }

    @Test
    fun subListAdd() {
        val v = mutableObjectListOf(1, 2, 3)
        val l = v.asMutableList().subList(1, 2)
        assertTrue(l.add(4))
        assertEquals(2, l.size)
        assertEquals(4, v.size)
        assertEquals(2, l[0])
        assertEquals(4, l[1])
        assertEquals(2, v[1])
        assertEquals(4, v[2])
        assertEquals(3, v[3])
    }

    @Test
    fun subListAddIndex() {
        val v = mutableObjectListOf(6, 1, 2, 3)
        val l = v.asMutableList().subList(1, 3)
        l.add(1, 4)
        assertEquals(3, l.size)
        assertEquals(5, v.size)
        assertEquals(1, l[0])
        assertEquals(4, l[1])
        assertEquals(2, l[2])
        assertEquals(1, v[1])
        assertEquals(4, v[2])
        assertEquals(2, v[3])
    }

    @Test
    fun subListAddAllAtIndex() {
        val v = mutableObjectListOf(6, 1, 2, 3)
        val l = v.asMutableList().subList(1, 3)
        l.addAll(1, listOf(4, 5))
        assertEquals(4, l.size)
        assertEquals(6, v.size)
        assertEquals(1, l[0])
        assertEquals(4, l[1])
        assertEquals(5, l[2])
        assertEquals(2, l[3])
        assertEquals(1, v[1])
        assertEquals(4, v[2])
        assertEquals(5, v[3])
        assertEquals(2, v[4])
    }

    @Test
    fun subListAddAll() {
        val v = mutableObjectListOf(6, 1, 2, 3)
        val l = v.asMutableList().subList(1, 3)
        l.addAll(listOf(4, 5))
        assertEquals(4, l.size)
        assertEquals(6, v.size)
        assertEquals(1, l[0])
        assertEquals(2, l[1])
        assertEquals(4, l[2])
        assertEquals(5, l[3])
        assertEquals(1, v[1])
        assertEquals(2, v[2])
        assertEquals(4, v[3])
        assertEquals(5, v[4])
        assertEquals(3, v[5])
    }

    @Test
    fun subListClear() {
        val v = mutableObjectListOf(1, 2, 3, 4, 5)
        val l = v.asMutableList().subList(1, 4)
        l.clear()
        assertEquals(0, l.size)
        assertEquals(2, v.size)
        assertEquals(1, v[0])
        assertEquals(5, v[1])
        assertNull(v.content[2])
        assertNull(v.content[3])
        assertNull(v.content[4])
    }

    @Test
    fun subListListIterator() {
        val l = list.asMutableList().subList(1, 4)
        val listIterator = l.listIterator()
        assertTrue(listIterator.hasNext())
        assertFalse(listIterator.hasPrevious())
        assertEquals(0, listIterator.nextIndex())
        assertEquals(2, listIterator.next())
    }

    @Test
    fun subListListIteratorWithIndex() {
        val l = list.asMutableList().subList(1, 4)
        val listIterator = l.listIterator(1)
        assertTrue(listIterator.hasNext())
        assertTrue(listIterator.hasPrevious())
        assertEquals(1, listIterator.nextIndex())
        assertEquals(3, listIterator.next())
    }

    @Test
    fun subListRemove() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val l2 = l.asMutableList().subList(1, 4)
        assertTrue(l2.remove(3))
        assertEquals(l, mutableObjectListOf(1, 2, 4, 5))
        assertEquals(2, l2.size)
        assertEquals(2, l2[0])
        assertEquals(4, l2[1])
        assertFalse(l2.remove(3))
        assertEquals(l, mutableObjectListOf(1, 2, 4, 5))
        assertEquals(2, l2.size)
    }

    @Test
    fun subListRemoveAll() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val l2 = l.asMutableList().subList(1, 4)
        assertFalse(l2.removeAll(listOf(1, 5, -1)))
        assertEquals(5, l.size)
        assertEquals(3, l2.size)
        assertTrue(l2.removeAll(listOf(3, 4, 5)))
        assertEquals(3, l.size)
        assertEquals(1, l2.size)
    }

    @Test
    fun subListRemoveAt() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val l2 = l.asMutableList().subList(1, 4)
        assertEquals(3, l2.removeAt(1))
        assertEquals(4, l.size)
        assertEquals(2, l2.size)
        assertEquals(4, l2.removeAt(1))
        assertEquals(1, l2.size)
    }

    @Test
    fun subListRetainAll() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val l2 = l.asMutableList().subList(1, 4)
        val l3 = objectListOf(1, 2, 3, 4, 5)
        assertFalse(l2.retainAll(l3.asList()))
        assertFalse(l2.retainAll(listOf(2, 3, 4)))
        assertEquals(3, l2.size)
        assertEquals(5, l.size)
        assertTrue(l2.retainAll(setOf(1, 2, 4)))
        assertEquals(4, l.size)
        assertEquals(2, l2.size)
        assertEquals(l, objectListOf(1, 2, 4, 5))
    }

    @Test
    fun subListSet() {
        val l = mutableObjectListOf(1, 2, 3, 4, 5)
        val l2 = l.asMutableList().subList(1, 4)
        l2[1] = 10
        assertEquals(10, l2[1])
        assertEquals(3, l2.size)
        assertEquals(10, l[2])
    }

    @Test
    fun subListSubList() {
        val l = objectListOf(1, 2, 3, 4, 5).asList().subList(1, 5)
        val l2 = l.subList(1, 3)
        assertEquals(2, l2.size)
        assertEquals(3, l2[0])
    }

    @Suppress("KotlinConstantConditions", "RedundantSuppression")
    @Test
    fun list_outOfBounds_Get_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(1, 2, 3, 4).asMutableList()
            l[-1]
        }
    }

    @Suppress("KotlinConstantConditions", "RedundantSuppression")
    @Test
    fun sublist_outOfBounds_Get_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(1, 2, 3, 4).asMutableList().subList(1, 2)
            l[-1]
        }
    }

    @Test
    fun list_outOfBounds_Get_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(1, 2, 3, 4).asMutableList()
            l[4]
        }
    }

    @Test
    fun sublist_outOfBounds_Get_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(1, 2, 3, 4).asMutableList().subList(1, 2)
            l[1]
        }
    }

    @Test
    fun list_outOfBounds_RemoveAt_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l.removeAt(-1)
        }
    }

    @Test
    fun sublist_outOfBounds_RemoveAt_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l.removeAt(-1)
        }
    }

    @Test
    fun list_outOfBounds_RemoveAt_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l.removeAt(4)
        }
    }

    @Test
    fun sublist_outOfBounds_RemoveAt_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l.removeAt(1)
        }
    }

    @Suppress("KotlinConstantConditions", "RedundantSuppression")
    @Test
    fun list_outOfBounds_Set_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l[-1] = 1
        }
    }

    @Suppress("KotlinConstantConditions", "RedundantSuppression")
    @Test
    fun sublist_outOfBounds_Set_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l[-1] = 1
        }
    }

    @Test
    fun list_outOfBounds_Set_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l[4] = 1
        }
    }

    @Test
    fun sublist_outOfBounds_Set_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l[1] = 1
        }
    }

    @Test
    fun list_outOfBounds_SubList_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l.subList(-1, 1)
        }
    }

    @Test
    fun sublist_outOfBounds_SubList_Below() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l.subList(-1, 1)
        }
    }

    @Test
    fun list_outOfBounds_SubList_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l.subList(5, 5)
        }
    }

    @Test
    fun sublist_outOfBounds_SubList_Above() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l.subList(1, 2)
        }
    }

    @Test
    fun list_outOfBounds_SubList_Order() {
        assertFailsWith(IllegalArgumentException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList()
            l.subList(3, 2)
        }
    }

    @Test
    fun sublist_outOfBounds_SubList_Order() {
        assertFailsWith(IllegalArgumentException::class) {
            val l = mutableObjectListOf(0, 1, 2, 3).asMutableList().subList(1, 2)
            l.subList(1, 0)
        }
    }
}
