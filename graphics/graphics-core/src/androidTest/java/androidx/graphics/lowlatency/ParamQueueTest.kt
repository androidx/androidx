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

package androidx.graphics.lowlatency

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
internal class ParamQueueTest {

    @Test
    fun testAdd() {
        val queue = ParamQueue<Int>()
        queue.add(1)
        queue.next {
            assertEquals(1, it)
        }
    }

    @Test
    fun testNextWithEmptyQueue() {
        var blockInvoked = false
        ParamQueue<Int>().next { blockInvoked = true }
        // empty queue should not call this lambda
        assertFalse(blockInvoked)
    }

    @Test
    fun testRelease() {
        val queue = ParamQueue<Int>()
        with(queue) {
            for (i in 1 until 5) {
                add(i)
            }
        }
        val list = queue.release()
        var count = 1
        for (entry in list) {
            assertEquals(count++, entry)
        }
    }

    @Test
    fun testClear() {
        with(ParamQueue<Int>()) {
            for (i in 0 until 2) {
                add(i)
            }
            clear()
            val list = release()
            assertTrue(list.isEmpty())
        }
    }

    @Test
    fun testNext() {
        with(ParamQueue<Int>()) {
            for (i in 0 until 10) {
                add(i)
            }
            var count = 0
            for (i in 0 until 10) {
                next { it ->
                    assertEquals(i, it)
                    count++
                }
            }
            assertEquals(10, count)
        }
    }
}
