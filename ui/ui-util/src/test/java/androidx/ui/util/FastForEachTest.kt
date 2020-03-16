/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FastForEachTest {
    @Test
    fun regularIteration() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        list.fastForEach {
            otherList.add(it)
        }
        // The correct iteration order, all items in there
        assertEquals(otherList, list)
    }

    @Test
    fun shortIteration() {
        val list = listOf(1, 5, 10)
        val otherList = mutableListOf<Int>()
        list.fastForEach {
            if (it == 5) {
                return@fastForEach
            }
            otherList.add(it)
        }
        // Should have only one item in it
        assertEquals(2, otherList.size)
        assertEquals(1, otherList[0])
        assertEquals(10, otherList[1])
    }
}