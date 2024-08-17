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

import androidx.collection.template.MutableTestValueClassSet
import androidx.collection.template.TestValueClassSet
import androidx.collection.template.emptyTestValueClassSet
import androidx.collection.template.mutableTestValueClassSetOf
import androidx.collection.template.testValueClassSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// ValueClassSet was created with:
// sed -e "s/PACKAGE/androidx.collection.template/" -e "s/VALUE_CLASS/TestValueClass/g" \
//     -e "s/vALUE_CLASS/testValueClass/g" -e "s/BACKING_PROPERTY/value.toLong()/g" \
//     -e "s/TO_PARAM/.toULong()/g" -e "s/PRIMITIVE/Long/g" -e "s/VALUE_PKG/androidx.collection/" \
//     collection/collection/template/ValueClassSet.kt.template \
//     >
// collection/collection/src/commonTest/kotlin/androidx/collection/template/TestValueClassSet.kt

@OptIn(ExperimentalUnsignedTypes::class)
class ValueClassSetTest {
    @Test
    fun emptyTestValueClassSetConstructor() {
        val set = MutableTestValueClassSet()
        assertEquals(7, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun immutableEmptyTestValueClassSet() {
        val set: TestValueClassSet = emptyTestValueClassSet()
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun zeroCapacityTestValueClassSet() {
        val set = MutableTestValueClassSet(0)
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun emptyTestValueClassSetWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val set = MutableTestValueClassSet(1800)
        assertEquals(4095, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun mutableTestValueClassSetBuilder() {
        val empty = mutableTestValueClassSetOf()
        assertEquals(0, empty.size)

        val withElements = mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        assertEquals(2, withElements.size)
        assertTrue(TestValueClass(1UL) in withElements)
        assertTrue(TestValueClass(2UL) in withElements)
    }

    @Test
    fun addToTestValueClassSet() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        assertTrue(set.add(TestValueClass(2UL)))

        assertEquals(2, set.size)
        val elements = ULongArray(2)
        var index = 0
        set.forEach { element -> elements[index++] = element.value }
        elements.sort()
        assertEquals(1UL, elements[0])
        assertEquals(2UL, elements[1])
    }

    @Test
    fun addToSizedTestValueClassSet() {
        val set = MutableTestValueClassSet(12)
        set += TestValueClass(1UL)

        assertEquals(1, set.size)
        assertEquals(TestValueClass(1UL), set.first())
    }

    @Test
    fun addExistingElement() {
        val set = MutableTestValueClassSet(12)
        set += TestValueClass(1UL)
        assertFalse(set.add(TestValueClass(1UL)))
        set += TestValueClass(1UL)

        assertEquals(1, set.size)
        assertEquals(TestValueClass(1UL), set.first())
    }

    @Test
    fun addAllTestValueClassSet() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL))
        assertFalse(set.addAll(mutableTestValueClassSetOf(TestValueClass(1UL))))
        assertEquals(1, set.size)
        assertTrue(set.addAll(mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))))
        assertEquals(2, set.size)
        assertTrue(TestValueClass(2UL) in set)
    }

    @Test
    fun plusAssignTestValueClassSet() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL))
        set += mutableTestValueClassSetOf(TestValueClass(1UL))
        assertEquals(1, set.size)
        set += mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        assertEquals(2, set.size)
        assertTrue(TestValueClass(2UL) in set)
    }

    @Test
    fun firstWithValue() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(2UL)
        var element = TestValueClass(ULong.MAX_VALUE)
        var otherElement = TestValueClass(ULong.MAX_VALUE)
        set.forEach { if (element.value == ULong.MAX_VALUE) element = it else otherElement = it }
        assertEquals(element, set.first())
        set -= element
        assertEquals(otherElement, set.first())
    }

    @Test
    fun firstEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableTestValueClassSet()
            set.first()
        }
    }

    @Test
    fun firstMatching() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(2UL)
        assertEquals(TestValueClass(1UL), set.first { it.value < 2UL })
        assertEquals(TestValueClass(2UL), set.first { it.value > 1UL })
    }

    @Test
    fun firstMatchingEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableTestValueClassSet()
            set.first { it.value > 0UL }
        }
    }

    @Test
    fun firstMatchingNoMatch() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableTestValueClassSet()
            set += TestValueClass(1UL)
            set += TestValueClass(2UL)
            set.first { it.value < 0UL }
        }
    }

    @Test
    fun remove() {
        val set = MutableTestValueClassSet()
        assertFalse(set.remove(TestValueClass(1UL)))

        set += TestValueClass(1UL)
        assertTrue(set.remove(TestValueClass(1UL)))
        assertEquals(0, set.size)

        set += TestValueClass(1UL)
        set -= TestValueClass(1UL)
        assertEquals(0, set.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val set = MutableTestValueClassSet(6)
        set += TestValueClass(1UL)
        set += TestValueClass(5UL)
        set += TestValueClass(6UL)
        set += TestValueClass(9UL)
        set += TestValueClass(11UL)
        set += TestValueClass(13UL)

        // Removing all the entries will mark the medata as deleted
        set.remove(TestValueClass(1UL))
        set.remove(TestValueClass(5UL))
        set.remove(TestValueClass(6UL))
        set.remove(TestValueClass(9UL))
        set.remove(TestValueClass(11UL))
        set.remove(TestValueClass(13UL))

        assertEquals(0, set.size)

        val capacity = set.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        set += TestValueClass(3UL)

        assertEquals(1, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun removeAllTestValueClassSet() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        assertFalse(
            set.removeAll(mutableTestValueClassSetOf(TestValueClass(3UL), TestValueClass(5UL)))
        )
        assertEquals(2, set.size)
        assertTrue(
            set.removeAll(
                mutableTestValueClassSetOf(
                    TestValueClass(3UL),
                    TestValueClass(1UL),
                    TestValueClass(5UL)
                )
            )
        )
        assertEquals(1, set.size)
        assertFalse(TestValueClass(1UL) in set)
    }

    @Test
    fun minusAssignTestValueClassSet() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        set -= mutableTestValueClassSetOf(TestValueClass(3UL), TestValueClass(5UL))
        assertEquals(2, set.size)
        set -=
            mutableTestValueClassSetOf(
                TestValueClass(3UL),
                TestValueClass(1UL),
                TestValueClass(5UL)
            )
        assertEquals(1, set.size)
        assertFalse(TestValueClass(1UL) in set)
    }

    @Test
    fun insertManyEntries() {
        val set = MutableTestValueClassSet()

        for (i in 0 until 1700) {
            set += TestValueClass(i.toULong())
        }

        assertEquals(1700, set.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val set = MutableTestValueClassSet()

            for (j in 0 until i) {
                set += TestValueClass(j.toULong())
            }

            val elements = ULongArray(i)
            var index = 0
            set.forEach { element -> elements[index++] = element.value }
            elements.sort()

            index = 0
            elements.forEach { element ->
                assertEquals(element, index.toULong())
                index++
            }
        }
    }

    @Test
    fun clear() {
        val set = MutableTestValueClassSet()

        for (i in 0 until 32) {
            set += TestValueClass(i.toULong())
        }

        val capacity = set.capacity
        set.clear()

        assertEquals(0, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun string() {
        val set = MutableTestValueClassSet()
        assertEquals("[]", set.toString())

        set += TestValueClass(1UL)
        set += TestValueClass(5UL)
        assertTrue("[>1<, >5<]" == set.toString() || "[>5<, >1<]" == set.toString())
    }

    @Test
    fun equalsTest() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(5UL)

        assertFalse(set.equals(null))
        assertEquals(set, set)

        val set2 = MutableTestValueClassSet()
        set2 += TestValueClass(5UL)

        assertNotEquals(set, set2)

        set2 += TestValueClass(1UL)
        assertEquals(set, set2)
    }

    @Test
    fun contains() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(5UL)

        assertTrue(set.contains(TestValueClass(1UL)))
        assertTrue(set.contains(TestValueClass(5UL)))
        assertFalse(set.contains(TestValueClass(2UL)))
    }

    @Test
    fun empty() {
        val set = MutableTestValueClassSet()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        assertTrue(set.none())
        assertFalse(set.any())

        set += TestValueClass(1UL)

        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
        assertTrue(set.any())
        assertFalse(set.none())
    }

    @Test
    fun count() {
        val set = MutableTestValueClassSet()
        assertEquals(0, set.count())

        set += TestValueClass(1UL)
        assertEquals(1, set.count())

        set += TestValueClass(5UL)
        set += TestValueClass(6UL)
        set += TestValueClass(9UL)
        set += TestValueClass(11UL)
        set += TestValueClass(13UL)

        assertEquals(2, set.count { it.value < 6UL })
        assertEquals(0, set.count { it.value < 0UL })
    }

    @Test
    fun any() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(5UL)
        set += TestValueClass(6UL)
        set += TestValueClass(9UL)
        set += TestValueClass(11UL)
        set += TestValueClass(13UL)

        assertTrue(set.any { it.value >= 11UL })
        assertFalse(set.any { it.value < 0UL })
    }

    @Test
    fun all() {
        val set = MutableTestValueClassSet()
        set += TestValueClass(1UL)
        set += TestValueClass(5UL)
        set += TestValueClass(6UL)
        set += TestValueClass(9UL)
        set += TestValueClass(11UL)
        set += TestValueClass(13UL)

        assertTrue(set.all { it.value > 0UL })
        assertFalse(set.all { it.value < 0UL })
    }

    @Test
    fun trim() {
        val set =
            mutableTestValueClassSetOf().also {
                it += TestValueClass(1UL)
                it += TestValueClass(2UL)
                it += TestValueClass(3UL)
                it += TestValueClass(4UL)
                it += TestValueClass(5UL)
                it += TestValueClass(7UL)
            }
        val capacity = set.capacity
        assertEquals(0, set.trim())
        set.clear()
        assertEquals(capacity, set.trim())
        assertEquals(0, set.capacity)

        set += TestValueClass(1UL)
        set += TestValueClass(2UL)
        set += TestValueClass(3UL)
        set += TestValueClass(4UL)
        set += TestValueClass(5UL)
        set += TestValueClass(7UL)
        set += TestValueClass(6UL)
        set += TestValueClass(8UL)
        set += TestValueClass(9UL)
        set += TestValueClass(10UL)
        set += TestValueClass(11UL)
        set += TestValueClass(12UL)
        set += TestValueClass(13UL)
        set += TestValueClass(14UL)

        set -= TestValueClass(6UL)
        set -= TestValueClass(8UL)
        set -= TestValueClass(9UL)
        set -= TestValueClass(10UL)
        set -= TestValueClass(11UL)
        set -= TestValueClass(12UL)
        set -= TestValueClass(13UL)
        set -= TestValueClass(14UL)
        assertTrue(set.trim() > 0)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun testValueClassSetOfEmpty() {
        assertEquals(emptyTestValueClassSet(), testValueClassSetOf())
        assertEquals(0, testValueClassSetOf().size)
    }

    @Test
    fun testValueClassSetOfOne() {
        val set = testValueClassSetOf(TestValueClass(1UL))
        assertEquals(1, set.size)
        assertEquals(TestValueClass(1UL), set.first())
    }

    @Test
    fun testValueClassSetOfTwo() {
        val set = testValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        assertEquals(2, set.size)
        assertTrue(TestValueClass(1UL) in set)
        assertTrue(TestValueClass(2UL) in set)
        assertFalse(TestValueClass(5UL) in set)
    }

    @Test
    fun testValueClassSetOfThree() {
        val set = testValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL), TestValueClass(3UL))
        assertEquals(3, set.size)
        assertTrue(TestValueClass(1UL) in set)
        assertTrue(TestValueClass(2UL) in set)
        assertTrue(TestValueClass(3UL) in set)
        assertFalse(TestValueClass(5UL) in set)
    }

    @Test
    fun mutableTestValueClassSetOfOne() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL))
        assertEquals(1, set.size)
        assertEquals(TestValueClass(1UL), set.first())
    }

    @Test
    fun mutableTestValueClassSetOfTwo() {
        val set = mutableTestValueClassSetOf(TestValueClass(1UL), TestValueClass(2UL))
        assertEquals(2, set.size)
        assertTrue(TestValueClass(1UL) in set)
        assertTrue(TestValueClass(2UL) in set)
        assertFalse(TestValueClass(5UL) in set)
    }

    @Test
    fun mutableTestValueClassSetOfThree() {
        val set =
            mutableTestValueClassSetOf(
                TestValueClass(1UL),
                TestValueClass(2UL),
                TestValueClass(3UL)
            )
        assertEquals(3, set.size)
        assertTrue(TestValueClass(1UL) in set)
        assertTrue(TestValueClass(2UL) in set)
        assertTrue(TestValueClass(3UL) in set)
        assertFalse(TestValueClass(5UL) in set)
    }

    @Test
    fun asTestValueClassSet() {
        assertEquals(
            testValueClassSetOf(TestValueClass(1UL)),
            mutableTestValueClassSetOf(TestValueClass(1UL)).asTestValueClassSet()
        )
    }
}
