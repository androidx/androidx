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

import androidx.collection.template.MutableTestValueClassList
import androidx.collection.template.emptyTestValueClassList
import androidx.collection.template.mutableTestValueClassListOf
import androidx.collection.template.testValueClassListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// ValueClassList was created with:
// sed -e "s/PACKAGE/androidx.collection.template/" -e "s/VALUE_CLASS/TestValueClass/g" \
//     -e "s/vALUE_CLASS/testValueClass/g" -e "s/BACKING_PROPERTY/value.toLong()/g" \
//     -e "s/TO_PARAM/.toULong()/g" -e "s/PRIMITIVE/Long/g" -e "s/VALUE_PKG/androidx.collection/" \
//     collection/collection/template/ValueClassList.kt.template \
//     > collection/collection/src/commonTest/kotlin/androidx/collection/template/TestValueClassList.kt

class ValueClassListTest {
    private val list: MutableTestValueClassList = mutableTestValueClassListOf().also {
        it += TestValueClass(1UL)
        it += TestValueClass(2UL)
        it += TestValueClass(3UL)
        it += TestValueClass(4UL)
        it += TestValueClass(5UL)
    }

    @Test
    fun emptyConstruction() {
        val l = mutableTestValueClassListOf()
        assertEquals(0, l.size)
        assertEquals(16, l.capacity)
    }

    @Test
    fun sizeConstruction() {
        val l = MutableTestValueClassList(4)
        assertEquals(4, l.capacity)
    }

    @Test
    fun contentConstruction() {
        val l = mutableTestValueClassListOf(
            TestValueClass(1UL),
            TestValueClass(2UL),
            TestValueClass(3UL)
        )
        assertEquals(3, l.size)
        assertEquals(TestValueClass(1UL), l[0])
        assertEquals(TestValueClass(2UL), l[1])
        assertEquals(TestValueClass(3UL), l[2])
        assertEquals(3, l.capacity)
        repeat(2) {
            val l2 = mutableTestValueClassListOf().also {
                it += TestValueClass(1UL)
                it += TestValueClass(2UL)
                it += TestValueClass(3UL)
                it += TestValueClass(4UL)
                it += TestValueClass(5UL)
            }
            assertEquals(list, l2)
            l2.removeAt(0)
        }
    }

    @Test
    fun hashCodeTest() {
        val l2 = mutableTestValueClassListOf().also {
            it += TestValueClass(1UL)
            it += TestValueClass(2UL)
            it += TestValueClass(3UL)
            it += TestValueClass(4UL)
            it += TestValueClass(5UL)
        }
        assertEquals(list.hashCode(), l2.hashCode())
        l2.removeAt(4)
        assertNotEquals(list.hashCode(), l2.hashCode())
        l2.add(TestValueClass(5UL))
        assertEquals(list.hashCode(), l2.hashCode())
        l2.clear()
        assertNotEquals(list.hashCode(), l2.hashCode())
    }

    @Test
    fun equalsTest() {
        val l2 = mutableTestValueClassListOf().also {
            it += TestValueClass(1UL)
            it += TestValueClass(2UL)
            it += TestValueClass(3UL)
            it += TestValueClass(4UL)
            it += TestValueClass(5UL)
        }
        assertEquals(list, l2)
        assertNotEquals(list, mutableTestValueClassListOf())
        l2.removeAt(4)
        assertNotEquals(list, l2)
        l2.add(TestValueClass(5UL))
        assertEquals(list, l2)
        l2.clear()
        assertNotEquals(list, l2)
    }

    @Test
    fun string() {
        assertEquals("[>1<, >2<, >3<, >4<, >5<]", list.toString())
        assertEquals("[]", mutableTestValueClassListOf().toString())
    }

    @Test
    fun size() {
        assertEquals(5, list.size)
        assertEquals(5, list.count())
        val l2 = mutableTestValueClassListOf()
        assertEquals(0, l2.size)
        assertEquals(0, l2.count())
        l2 += TestValueClass(1UL)
        assertEquals(1, l2.size)
        assertEquals(1, l2.count())
    }

    @Test
    fun get() {
        assertEquals(TestValueClass(1UL), list[0])
        assertEquals(TestValueClass(5UL), list[4])
        assertEquals(TestValueClass(1UL), list.elementAt(0))
        assertEquals(TestValueClass(5UL), list.elementAt(4))
    }

    @Test
    fun getOutOfBounds() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            list[5]
        }
    }

    @Test
    fun getOutOfBoundsNegative() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            list[-1]
        }
    }

    @Test
    fun elementAtOfBounds() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            list.elementAt(5)
        }
    }

    @Test
    fun elementAtOfBoundsNegative() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            list.elementAt(-1)
        }
    }

    @Test
    fun elementAtOrElse() {
        assertEquals(TestValueClass(1UL), list.elementAtOrElse(0) {
            assertEquals(0, it)
            TestValueClass(0UL)
        })
        assertEquals(TestValueClass(0UL), list.elementAtOrElse(-1) {
            assertEquals(-1, it)
            TestValueClass(0UL)
        })
        assertEquals(TestValueClass(0UL), list.elementAtOrElse(5) {
            assertEquals(5, it)
            TestValueClass(0UL)
        })
    }

    @Test
    fun count() {
        assertEquals(1, list.count { it.value < 2UL })
        assertEquals(0, list.count { it.value < 0UL })
        assertEquals(5, list.count { it.value < 10UL })
    }

    @Test
    fun isEmpty() {
        assertFalse(list.isEmpty())
        assertFalse(list.none())
        assertTrue(mutableTestValueClassListOf().isEmpty())
        assertTrue(mutableTestValueClassListOf().none())
    }

    @Test
    fun isNotEmpty() {
        assertTrue(list.isNotEmpty())
        assertTrue(list.any())
        assertFalse(mutableTestValueClassListOf().isNotEmpty())
    }

    @Test
    fun indices() {
        assertEquals(IntRange(0, 4), list.indices)
        assertEquals(IntRange(0, -1), mutableTestValueClassListOf().indices)
    }

    @Test
    fun any() {
        assertTrue(list.any { it == TestValueClass(5UL) })
        assertTrue(list.any { it == TestValueClass(1UL) })
        assertFalse(list.any { it == TestValueClass(0UL) })
    }

    @Test
    fun reversedAny() {
        val reversedList = mutableTestValueClassListOf()
        assertFalse(
            list.reversedAny {
                reversedList.add(it)
                false
            }
        )
        val reversedContent = mutableTestValueClassListOf().also {
            it += TestValueClass(5UL)
            it += TestValueClass(4UL)
            it += TestValueClass(3UL)
            it += TestValueClass(2UL)
            it += TestValueClass(1UL)
        }
        assertEquals(reversedContent, reversedList)

        val reversedSublist = mutableTestValueClassListOf()
        assertTrue(
            list.reversedAny {
                reversedSublist.add(it)
                reversedSublist.size == 2
            }
        )
        assertEquals(
            reversedSublist,
            mutableTestValueClassListOf(TestValueClass(5UL), TestValueClass(4UL))
        )
    }

    @Test
    fun forEach() {
        val copy = mutableTestValueClassListOf()
        list.forEach { copy += it }
        assertEquals(list, copy)
    }

    @Test
    fun forEachReversed() {
        val copy = mutableTestValueClassListOf()
        list.forEachReversed { copy += it }
        assertEquals(
            copy,
            mutableTestValueClassListOf().also {
                it += TestValueClass(5UL)
                it += TestValueClass(4UL)
                it += TestValueClass(3UL)
                it += TestValueClass(2UL)
                it += TestValueClass(1UL)
            }
        )
    }

    @Test
    fun forEachIndexed() {
        val copy = mutableTestValueClassListOf()
        val indices = mutableTestValueClassListOf()
        list.forEachIndexed { index, item ->
            copy += item
            indices += TestValueClass(index.toULong())
        }
        assertEquals(list, copy)
        assertEquals(
            indices,
            mutableTestValueClassListOf().also {
                it += TestValueClass(0UL)
                it += TestValueClass(1UL)
                it += TestValueClass(2UL)
                it += TestValueClass(3UL)
                it += TestValueClass(4UL)
            }
        )
    }

    @Test
    fun forEachReversedIndexed() {
        val copy = mutableTestValueClassListOf()
        val indices = mutableTestValueClassListOf()
        list.forEachReversedIndexed { index, item ->
            copy += item
            indices += TestValueClass(index.toULong())
        }
        assertEquals(
            copy,
            mutableTestValueClassListOf().also {
                it += TestValueClass(5UL)
                it += TestValueClass(4UL)
                it += TestValueClass(3UL)
                it += TestValueClass(2UL)
                it += TestValueClass(1UL)
            })
        assertEquals(
            indices,
            mutableTestValueClassListOf().also {
                it += TestValueClass(4UL)
                it += TestValueClass(3UL)
                it += TestValueClass(2UL)
                it += TestValueClass(1UL)
                it += TestValueClass(0UL)
            })
    }

    @Test
    fun indexOfFirst() {
        assertEquals(0, list.indexOfFirst { it == TestValueClass(1UL) })
        assertEquals(4, list.indexOfFirst { it == TestValueClass(5UL) })
        assertEquals(-1, list.indexOfFirst { it == TestValueClass(0UL) })
        assertEquals(
            0,
            mutableTestValueClassListOf(
                TestValueClass(8UL),
                TestValueClass(8UL)
            ).indexOfFirst { it == TestValueClass(8UL) })
    }

    @Test
    fun indexOfLast() {
        assertEquals(0, list.indexOfLast { it == TestValueClass(1UL) })
        assertEquals(4, list.indexOfLast { it == TestValueClass(5UL) })
        assertEquals(-1, list.indexOfLast { it == TestValueClass(0UL) })
        assertEquals(
            1,
            mutableTestValueClassListOf(
                TestValueClass(8UL),
                TestValueClass(8UL)
            ).indexOfLast { it == TestValueClass(8UL) })
    }

    @Test
    fun contains() {
        assertTrue(list.contains(TestValueClass(5UL)))
        assertTrue(list.contains(TestValueClass(1UL)))
        assertFalse(list.contains(TestValueClass(0UL)))
    }

    @Test
    fun containsAllList() {
        assertTrue(
            list.containsAll(
                mutableTestValueClassListOf(
                    TestValueClass(2UL),
                    TestValueClass(3UL),
                    TestValueClass(1UL)
                )
            )
        )
        assertFalse(
            list.containsAll(
                mutableTestValueClassListOf(
                    TestValueClass(2UL),
                    TestValueClass(3UL),
                    TestValueClass(6UL)
                )
            )
        )
    }

    @Test
    fun lastIndexOf() {
        assertEquals(4, list.lastIndexOf(TestValueClass(5UL)))
        assertEquals(1, list.lastIndexOf(TestValueClass(2UL)))
        val copy = mutableTestValueClassListOf()
        copy.addAll(list)
        copy.addAll(list)
        assertEquals(5, copy.lastIndexOf(TestValueClass(1UL)))
    }

    @Test
    fun first() {
        assertEquals(TestValueClass(1UL), list.first())
    }

    @Test
    fun firstException() {
        assertFailsWith(NoSuchElementException::class) {
            mutableTestValueClassListOf().first()
        }
    }

    @Test
    fun firstWithPredicate() {
        assertEquals(TestValueClass(5UL), list.first { it == TestValueClass(5UL) })
        assertEquals(
            TestValueClass(1UL),
            mutableTestValueClassListOf(TestValueClass(1UL), TestValueClass(5UL)).first {
                it != TestValueClass(0UL)
            })
    }

    @Test
    fun firstWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) {
            mutableTestValueClassListOf().first { it == TestValueClass(8UL) }
        }
    }

    @Test
    fun last() {
        assertEquals(TestValueClass(5UL), list.last())
    }

    @Test
    fun lastException() {
        assertFailsWith(NoSuchElementException::class) {
            mutableTestValueClassListOf().last()
        }
    }

    @Test
    fun lastWithPredicate() {
        assertEquals(TestValueClass(1UL), list.last { it == TestValueClass(1UL) })
        assertEquals(
            TestValueClass(5UL),
            mutableTestValueClassListOf(TestValueClass(1UL), TestValueClass(5UL)).last {
                it != TestValueClass(0UL)
            })
    }

    @Test
    fun lastWithPredicateException() {
        assertFailsWith(NoSuchElementException::class) {
            mutableTestValueClassListOf().last { it == TestValueClass(8UL) }
        }
    }

    @Test
    fun fold() {
        assertEquals("12345", list.fold("") { acc, i -> acc + i.value.toString() })
    }

    @Test
    fun foldIndexed() {
        assertEquals(
            "01-12-23-34-45-",
            list.foldIndexed("") { index, acc, i ->
                "$acc$index${i.value}-"
            }
        )
    }

    @Test
    fun foldRight() {
        assertEquals("54321", list.foldRight("") { i, acc -> acc + i.value.toString() })
    }

    @Test
    fun foldRightIndexed() {
        assertEquals(
            "45-34-23-12-01-",
            list.foldRightIndexed("") { index, i, acc ->
                "$acc$index${i.value}-"
            }
        )
    }

    @Test
    fun add() {
        val l = mutableTestValueClassListOf(
            TestValueClass(1UL),
            TestValueClass(2UL),
            TestValueClass(3UL)
        )
        l += TestValueClass(4UL)
        l.add(TestValueClass(5UL))
        assertEquals(list, l)
    }

    @Test
    fun addAtIndex() {
        val l = mutableTestValueClassListOf(TestValueClass(2UL), TestValueClass(4UL))
        l.add(2, TestValueClass(5UL))
        l.add(0, TestValueClass(1UL))
        l.add(2, TestValueClass(3UL))
        assertEquals(list, l)
        assertFailsWith(IndexOutOfBoundsException::class) {
            l.add(-1, TestValueClass(2UL))
        }
        assertFailsWith(IndexOutOfBoundsException::class) {
            l.add(6, TestValueClass(2UL))
        }
    }

    @Test
    fun addAllListAtIndex() {
        val l = mutableTestValueClassListOf(TestValueClass(4UL))
        val l2 = mutableTestValueClassListOf(TestValueClass(1UL), TestValueClass(2UL))
        val l3 = mutableTestValueClassListOf(TestValueClass(5UL))
        val l4 = mutableTestValueClassListOf(TestValueClass(3UL))
        assertTrue(l4.addAll(1, l3))
        assertTrue(l4.addAll(0, l2))
        assertTrue(l4.addAll(3, l))
        assertFalse(l4.addAll(0, mutableTestValueClassListOf()))
        assertEquals(list, l4)
        assertFailsWith(IndexOutOfBoundsException::class) {
            l4.addAll(6, mutableTestValueClassListOf())
        }
        assertFailsWith(IndexOutOfBoundsException::class) {
            l4.addAll(-1, mutableTestValueClassListOf())
        }
    }

    @Test
    fun addAllList() {
        val l = MutableTestValueClassList()
        l.add(TestValueClass(3UL))
        l.add(TestValueClass(4UL))
        l.add(TestValueClass(5UL))
        val l2 = mutableTestValueClassListOf(TestValueClass(1UL), TestValueClass(2UL))
        assertTrue(l2.addAll(l))
        assertEquals(list, l2)
        assertFalse(l2.addAll(mutableTestValueClassListOf()))
    }

    @Test
    fun plusAssignList() {
        val l = MutableTestValueClassList()
        l.add(TestValueClass(3UL))
        l.add(TestValueClass(4UL))
        l.add(TestValueClass(5UL))
        val l2 = mutableTestValueClassListOf(TestValueClass(1UL), TestValueClass(2UL))
        l2 += l
        assertEquals(list, l2)
    }

    @Test
    fun clear() {
        val l = mutableTestValueClassListOf()
        l.addAll(list)
        assertTrue(l.isNotEmpty())
        l.clear()
        assertTrue(l.isEmpty())
    }

    @Test
    fun trim() {
        val l = mutableTestValueClassListOf(TestValueClass(1UL))
        l.trim()
        assertEquals(1, l.capacity)
        l += TestValueClass(1UL)
        l += TestValueClass(2UL)
        l += TestValueClass(3UL)
        l += TestValueClass(4UL)
        l += TestValueClass(5UL)
        l.trim()
        assertEquals(6, l.capacity)
        assertEquals(6, l.size)
        l.clear()
        l.trim()
        assertEquals(0, l.capacity)
        l.trim(100)
        assertEquals(0, l.capacity)
        l += TestValueClass(1UL)
        l += TestValueClass(2UL)
        l += TestValueClass(3UL)
        l += TestValueClass(4UL)
        l += TestValueClass(5UL)
        l -= TestValueClass(5UL)
        l.trim(5)
        assertEquals(5, l.capacity)
        l.trim(4)
        assertEquals(4, l.capacity)
        l.trim(3)
        assertEquals(4, l.capacity)
    }

    @Test
    fun remove() {
        val l = mutableTestValueClassListOf()
        l += list
        l.remove(TestValueClass(3UL))
        assertEquals(
            mutableTestValueClassListOf().also {
                it += TestValueClass(1UL)
                it += TestValueClass(2UL)
                it += TestValueClass(4UL)
                it += TestValueClass(5UL)
            },
            l
        )
    }

    @Test
    fun removeAt() {
        val l = mutableTestValueClassListOf()
        l += list
        l.removeAt(2)
        assertEquals(
            mutableTestValueClassListOf().also {
                it += TestValueClass(1UL)
                it += TestValueClass(2UL)
                it += TestValueClass(4UL)
                it += TestValueClass(5UL)
            },
            l
        )
        assertFailsWith(IndexOutOfBoundsException::class) {
            l.removeAt(6)
        }
        assertFailsWith(IndexOutOfBoundsException::class) {
            l.removeAt(-1)
        }
    }

    @Test
    fun set() {
        val l = mutableTestValueClassListOf()
        repeat(5) { l += TestValueClass(0UL) }
        l[0] = TestValueClass(1UL)
        l[4] = TestValueClass(5UL)
        l[2] = TestValueClass(3UL)
        l[1] = TestValueClass(2UL)
        l[3] = TestValueClass(4UL)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> {
            l.set(-1, TestValueClass(1UL))
        }
        assertFailsWith<IndexOutOfBoundsException> {
            l.set(6, TestValueClass(1UL))
        }
        assertEquals(TestValueClass(4UL), l.set(3, TestValueClass(1UL)));
    }

    @Test
    fun ensureCapacity() {
        val l = mutableTestValueClassListOf(TestValueClass(1UL))
        assertEquals(1, l.capacity)
        l.ensureCapacity(5)
        assertEquals(5, l.capacity)
    }

    @Test
    fun removeAllList() {
        assertFalse(
            list.removeAll(
                mutableTestValueClassListOf(
                    TestValueClass(0UL),
                    TestValueClass(10UL),
                    TestValueClass(15UL)
                )
            )
        )
        val l = mutableTestValueClassListOf()

        l += TestValueClass(0UL)
        l += TestValueClass(1UL)
        l += TestValueClass(15UL)
        l += TestValueClass(10UL)
        l += TestValueClass(2UL)
        l += TestValueClass(3UL)
        l += TestValueClass(4UL)
        l += TestValueClass(5UL)
        l += TestValueClass(20UL)
        l += TestValueClass(5UL)
        assertTrue(
            l.removeAll(
                mutableTestValueClassListOf().also {
                    it += TestValueClass(20UL)
                    it += TestValueClass(0UL)
                    it += TestValueClass(15UL)
                    it += TestValueClass(10UL)
                    it += TestValueClass(5UL)
                }
            )
        )
        assertEquals(list, l)
    }

    @Test
    fun minusAssignList() {
        val l = mutableTestValueClassListOf().also { it += list }
        l -= mutableTestValueClassListOf(
            TestValueClass(0UL),
            TestValueClass(10UL),
            TestValueClass(15UL)
        )
        assertEquals(list, l)
        val l2 = mutableTestValueClassListOf()

        l2 += TestValueClass(0UL)
        l2 += TestValueClass(1UL)
        l2 += TestValueClass(15UL)
        l2 += TestValueClass(10UL)
        l2 += TestValueClass(2UL)
        l2 += TestValueClass(3UL)
        l2 += TestValueClass(4UL)
        l2 += TestValueClass(5UL)
        l2 += TestValueClass(20UL)
        l2 += TestValueClass(5UL)
        l2 -= mutableTestValueClassListOf().also {
            it += TestValueClass(20UL)
            it += TestValueClass(0UL)
            it += TestValueClass(15UL)
            it += TestValueClass(10UL)
            it += TestValueClass(5UL)
        }
        assertEquals(list, l2)
    }

    @Test
    fun retainAll() {
        assertFalse(
            list.retainAll(
                mutableTestValueClassListOf().also {
                    it += TestValueClass(1UL)
                    it += TestValueClass(2UL)
                    it += TestValueClass(3UL)
                    it += TestValueClass(4UL)
                    it += TestValueClass(5UL)
                    it += TestValueClass(6UL)
                }
            )
        )
        val l = mutableTestValueClassListOf()
            l += TestValueClass(0UL)
            l += TestValueClass(1UL)
            l += TestValueClass(15UL)
            l += TestValueClass(10UL)
            l += TestValueClass(2UL)
            l += TestValueClass(3UL)
            l += TestValueClass(4UL)
            l += TestValueClass(5UL)
            l += TestValueClass(20UL)
        assertTrue(
            l.retainAll(
                mutableTestValueClassListOf().also {
                    it += TestValueClass(1UL)
                    it += TestValueClass(2UL)
                    it += TestValueClass(3UL)
                    it += TestValueClass(4UL)
                    it += TestValueClass(5UL)
                    it += TestValueClass(6UL)
                }
            )
        )
        assertEquals(list, l)
    }

    @Test
    fun removeRange() {
        val l = mutableTestValueClassListOf()
        l += TestValueClass(1UL)
        l += TestValueClass(9UL)
        l += TestValueClass(7UL)
        l += TestValueClass(6UL)
        l += TestValueClass(2UL)
        l += TestValueClass(3UL)
        l += TestValueClass(4UL)
        l += TestValueClass(5UL)
        l.removeRange(1, 4)
        assertEquals(list, l)
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeRange(6, 6)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeRange(100, 200)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            l.removeRange(-1, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            l.removeRange(3, 2)
        }
    }

    @Test
    fun testEmptyTestValueClassList() {
        val l = emptyTestValueClassList()
        assertEquals(0, l.size)
    }

    @Test
    fun testValueClassListOfEmpty() {
        val l = testValueClassListOf()
        assertEquals(0, l.size)
    }

    @Test
    fun testValueClassListOfOneValue() {
        val l = testValueClassListOf(TestValueClass(2UL))
        assertEquals(1, l.size)
        assertEquals(TestValueClass(2UL), l[0])
    }

    @Test
    fun testValueClassListOfTwoValues() {
        val l = testValueClassListOf(TestValueClass(2UL), TestValueClass(1UL))
        assertEquals(2, l.size)
        assertEquals(TestValueClass(2UL), l[0])
        assertEquals(TestValueClass(1UL), l[1])
    }

    @Test
    fun testValueClassListOfThreeValues() {
        val l = testValueClassListOf(TestValueClass(2UL), TestValueClass(10UL), TestValueClass(1UL))
        assertEquals(3, l.size)
        assertEquals(TestValueClass(2UL), l[0])
        assertEquals(TestValueClass(10UL), l[1])
        assertEquals(TestValueClass(1UL), l[2])
    }

    @Test
    fun mutableTestValueClassListOfOneValue() {
        val l = mutableTestValueClassListOf(TestValueClass(2UL))
        assertEquals(1, l.size)
        assertEquals(1, l.capacity)
        assertEquals(TestValueClass(2UL), l[0])
    }

    @Test
    fun mutableTestValueClassListOfTwoValues() {
        val l = mutableTestValueClassListOf(TestValueClass(2UL), TestValueClass(1UL))
        assertEquals(2, l.size)
        assertEquals(2, l.capacity)
        assertEquals(TestValueClass(2UL), l[0])
        assertEquals(TestValueClass(1UL), l[1])
    }

    @Test
    fun mutableTestValueClassListOfThreeValues() {
        val l = mutableTestValueClassListOf(
            TestValueClass(2UL),
            TestValueClass(10UL),
            TestValueClass(1UL)
        )
        assertEquals(3, l.size)
        assertEquals(3, l.capacity)
        assertEquals(TestValueClass(2UL), l[0])
        assertEquals(TestValueClass(10UL), l[1])
        assertEquals(TestValueClass(1UL), l[2])
    }
}
