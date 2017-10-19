/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.paging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.ArrayList

@RunWith(JUnit4::class)
class NullPaddedListTest {
    @Test
    fun simple() {
        val data = listOf("A", "B", "C", "D", "E", "F")
        val list = NullPaddedList(2, data.subList(2, 4), 2)

        assertNull(list[0])
        assertNull(list[1])
        assertSame(data[2], list[2])
        assertSame(data[3], list[3])
        assertNull(list[4])
        assertNull(list[5])

        assertEquals(6, list.size)
        assertEquals(2, list.leadingNullCount)
        assertEquals(2, list.trailingNullCount)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getEmpty() {
        val list = NullPaddedList(0, ArrayList<String>(), 0)
        list[0]
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getNegative() {
        val list = NullPaddedList(0, listOf("a", "b"), 0)
        list[-1]
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getPastEnd() {
        val list = NullPaddedList(0, listOf("a", "b"), 0)
        list[2]
    }
}
