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

package androidx.compose.animation.core

import androidx.collection.intListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntListExtensionTest {
    @Test
    fun binarySearch() {
        val l = intListOf(1, 3, 5)
        assertEquals(0, l.binarySearch(1))
        assertEquals(-2, l.binarySearch(2))
        assertEquals(1, l.binarySearch(3))
        assertEquals(-3, l.binarySearch(4))
        assertEquals(2, l.binarySearch(5))

        assertEquals(-2, l.binarySearch(2, fromIndex = 1))
        assertEquals(-3, l.binarySearch(2, fromIndex = 2))
        assertEquals(-3, l.binarySearch(5, toIndex = l.size - 1))

        // toIndex is exclusive, fails with size + 1
        assertThrows(IndexOutOfBoundsException::class.java) {
            l.binarySearch(
                element = 3,
                toIndex = l.size + 1
            )
        }
        assertThrows(IndexOutOfBoundsException::class.java) {
            l.binarySearch(
                element = 3,
                fromIndex = -1
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            l.binarySearch(
                element = 3,
                fromIndex = 1,
                toIndex = 0
            )
        }
    }
}
