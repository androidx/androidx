/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DataTest {
    @Test
    fun testDataExtensions() {
        val data =
            workDataOf("one" to 1, "two" to 2L, "three" to "Three", "four" to longArrayOf(1L, 2L))
        assertEquals(data.getInt("one", 0), 1)
        assertEquals(data.getLong("two", 0L), 2L)
        assertEquals(data.getString("three"), "Three")
        val longArray = data.getLongArray("four")
        assertNotNull(longArray)
        assertEquals(longArray!!.size, 2)
        assertEquals(longArray[0], 1L)
        assertEquals(longArray[1], 2L)
        assertTrue(data.hasKeyWithValueOfType<Int>("one"))
        assertTrue(data.hasKeyWithValueOfType<Long>("two"))
        assertTrue(data.hasKeyWithValueOfType<String>("three"))
        assertTrue(data.hasKeyWithValueOfType<Array<Long>>("four"))
        assertFalse(data.hasKeyWithValueOfType<Any>("nothing"))
        assertFalse(data.hasKeyWithValueOfType<Float>("two"))
    }

    @Test
    fun getNullableStringArray_withNullValues() {
        val array = arrayOf("foo", null, "bar")
        val data = workDataOf("array" to array)
        val result = data.getNullableStringArray("array")
        assertNotNull(result)
        assertArrayEquals(array, result)
    }

    @Test
    fun getNullableStringArray_noNullValues() {
        val array = arrayOf("foo", "bar", "baz")
        val data = workDataOf("array" to array)
        val result = data.getNullableStringArray("array")
        assertNotNull(result)
        assertArrayEquals(array, result)
    }

    @Test
    fun getNullableStringArray_allNulls() {
        val array = arrayOf<String?>(null, null, null)
        val data = workDataOf("array" to array)
        val result = data.getNullableStringArray("array")
        assertNotNull(result)
        assertArrayEquals(array, result)
    }

    @Test
    fun getNullableStringArray_emptyArray() {
        val array = arrayOf<String?>()
        val data = workDataOf("array" to array)
        val result = data.getNullableStringArray("array")
        assertNotNull(result)
        assertArrayEquals(array, result)
    }

    @Test
    fun getNullableStringArray_keyNotFound() {
        val data = workDataOf()
        val result = data.getNullableStringArray("nonexistent_key")
        assertNull(result)
    }

    @Test
    fun getNullableStringArray_wrongType() {
        val data = workDataOf("array" to 123)
        val result = data.getNullableStringArray("array")
        assertNull(result)
    }

    @Test
    fun getNullableStringArray_wrongArrayType() {
        val data = workDataOf("array" to intArrayOf(1, 2, 3))
        val result = data.getNullableStringArray("array")
        assertNull(result)
    }
}
