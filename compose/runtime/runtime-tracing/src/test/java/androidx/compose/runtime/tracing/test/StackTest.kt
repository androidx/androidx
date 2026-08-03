/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.tracing.test

import androidx.compose.runtime.tracing.BLOCK_CAPACITY
import androidx.compose.runtime.tracing.Stack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTest {
    @Test
    fun basicTest() {
        val instance = Any()
        val stack = Stack<Any>()
        // Add
        repeat(256) { stack += instance }
        // Remove
        repeat(256) { assertEquals(instance, stack.removeLastOrNull()) }
    }

    @Test
    fun testRemoveBeforeAdd() {
        val stack = Stack<Any>()
        assertNull(stack.removeLastOrNull())
    }

    @Test
    fun testPooling() {
        val instance = Any()
        val stack = Stack<Any>(blkCount = 2)
        repeat(2) {
            // Add
            repeat(256) { stack += instance }
            // Remove
            repeat(256) {
                val element = stack.removeLastOrNull()
                assertEquals(instance, element)
            }
            assertTrue { stack.isEmpty() }
            assertFalse { stack.isNotEmpty() }
            assertEquals(stack.blkIdx, 0)
            assertEquals(stack.blkArray.size, 256 / BLOCK_CAPACITY)
            assertTrue(stack.blkArray.contentsAreNull())
        }
    }

    private fun Array<*>?.contentsAreNull(): Boolean {
        if (this == null) return true
        for (element in this) {
            if (element == null) continue
            return when {
                element is Array<*> -> element.contentsAreNull()
                else -> false
            }
        }
        return true
    }
}
