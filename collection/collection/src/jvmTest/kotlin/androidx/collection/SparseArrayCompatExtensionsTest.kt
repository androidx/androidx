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

package androidx.collection

import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SparseArrayCompatExtensionsTest {
    @Test
    fun sizeProperty() {
        val array = SparseArrayCompat<String>()
        assertEquals(0, array.size)
        array.put(1, "one")
        assertEquals(1, array.size)
    }

    @Test
    fun containsOperator() {
        val array = SparseArrayCompat<String>()
        assertFalse(1 in array)
        array.put(1, "one")
        assertTrue(1 in array)
    }

    @Test
    fun containsOperatorWithItem() {
        val array = SparseArrayCompat<String>()

        array.put(1, "one")
        assertFalse(2 in array)

        array.put(2, "two")
        assertTrue(2 in array)
    }

    @Test
    fun setOperator() {
        val array = SparseArrayCompat<String>()
        array[1] = "one"
        assertEquals("one", array.get(1))
    }

    @Test
    fun plusOperator() {
        val first = SparseArrayCompat<String>().apply { put(1, "one") }
        val second = SparseArrayCompat<String>().apply { put(2, "two") }
        val combined = first + second
        assertEquals(2, combined.size())
        assertEquals(1, combined.keyAt(0))
        assertEquals("one", combined.valueAt(0))
        assertEquals(2, combined.keyAt(1))
        assertEquals("two", combined.valueAt(1))
    }

    @Test
    fun getOrDefault() {
        val array = SparseArrayCompat<Any>()
        val default = Any()
        assertSame(default, array.getOrDefault(1, default))
        array.put(1, "one")
        assertEquals("one", array.getOrDefault(1, default))
    }

    @Test
    fun getOrElse() {
        val array = SparseArrayCompat<Any>()
        val default = Any()
        assertSame(default, array.getOrElse(1) { default })
        array.put(1, "one")
        assertEquals("one", array.getOrElse(1) { fail() })
    }

    @Test
    fun isNotEmpty() {
        val array = SparseArrayCompat<String>()
        assertFalse(array.isNotEmpty())
        array.put(1, "one")
        assertTrue(array.isNotEmpty())
    }

    @Test
    fun forEach() {
        val array = SparseArrayCompat<String>()
        array.forEach { _, _ -> fail() }

        array.put(1, "one")
        array.put(2, "two")
        array.put(6, "six")

        val keys = mutableListOf<Int>()
        val values = mutableListOf<String>()
        array.forEach { key, value ->
            keys.add(key)
            values.add(value)
        }
        assertThat(keys).containsExactly(1, 2, 6)
        assertThat(values).containsExactly("one", "two", "six")
    }

    @Test
    fun keyIterator() {
        val array = SparseArrayCompat<String>()
        assertFalse(array.keyIterator().hasNext())

        array.put(1, "one")
        array.put(2, "two")
        array.put(6, "six")

        val iterator = array.keyIterator()
        assertTrue(iterator.hasNext())
        assertEquals(1, iterator.nextInt())
        assertTrue(iterator.hasNext())
        assertEquals(2, iterator.nextInt())
        assertTrue(iterator.hasNext())
        assertEquals(6, iterator.nextInt())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun valueIterator() {
        val array = SparseArrayCompat<String>()
        assertFalse(array.valueIterator().hasNext())

        array.put(1, "one")
        array.put(2, "two")
        array.put(6, "six")

        val iterator = array.valueIterator()
        assertTrue(iterator.hasNext())
        assertEquals("one", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("two", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("six", iterator.next())
        assertFalse(iterator.hasNext())
    }
}
