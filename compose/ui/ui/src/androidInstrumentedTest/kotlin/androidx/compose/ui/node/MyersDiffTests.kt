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

package androidx.compose.ui.node

import org.junit.Assert.assertEquals
import org.junit.Test

class MyersDiffTests {

    @Test
    fun testDiffWithRemovesAtStart() {
        val a = listOf(0, 0, 0, 0, 0, 1, 2)
        val b = listOf(1, 2, 3)
        val (c, log) = executeListDiff(a, b)
        assertEquals(b, c)
        assertEquals(
            """
            Remove(0 at 0)
            Remove(0 at 0)
            Remove(0 at 0)
            Remove(0 at 0)
            Remove(0 at 0)
            Equals(x = 5, y = 0)
            Equals(x = 6, y = 1)
            Insert(3 at 2)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun testDiffWithRemovesAtEnd() {
        val a = listOf(0, 1, 2, 3, 4)
        val b = listOf(0, 1, 2)
        val (c, log) = executeListDiff(a, b)
        assertEquals(b, c)
        assertEquals(
            """
            Equals(x = 0, y = 0)
            Equals(x = 1, y = 1)
            Equals(x = 2, y = 2)
            Remove(3 at 3)
            Remove(4 at 3)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun testDiffWithInsertsAtEnd() {
        val a = listOf(0, 1, 2)
        val b = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val (c, log) = executeListDiff(a, b)
        assertEquals(b, c)
        assertEquals(
            """
            Remove(0 at 0)
            Equals(x = 1, y = 0)
            Equals(x = 2, y = 1)
            Insert(3 at 2)
            Insert(4 at 3)
            Insert(5 at 4)
            Insert(6 at 5)
            Insert(7 at 6)
            Insert(8 at 7)
            Insert(9 at 8)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun testDiff() {
        val a = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val b = listOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10)
        val (c, log) = executeListDiff(a, b)
        assertEquals(b, c)
        assertEquals(
            """
            Equals(x = 0, y = 0)
            Equals(x = 1, y = 1)
            Equals(x = 2, y = 2)
            Equals(x = 3, y = 3)
            Equals(x = 4, y = 4)
            Remove(5 at 5)
            Equals(x = 6, y = 5)
            Equals(x = 7, y = 6)
            Equals(x = 8, y = 7)
            Equals(x = 9, y = 8)
            Equals(x = 10, y = 9)
            """.trimIndent(),
            log.joinToString("\n")
        )
    }

    @Test
    fun stringDiff() {
        stringDiff(
            "ihfiwjfowijefoiwjfe",
            "ihfawwjwfowwijefwicwfe"
        )

        stringDiff("", "abcde")

        stringDiff("abcde", "")

        stringDiff(
            "aaaa",
            "bbbb",
            """
            Remove(a at 0)
            Remove(a at 0)
            Remove(a at 0)
            Remove(a at 0)
            Insert(b at 0)
            Insert(b at 1)
            Insert(b at 2)
            Insert(b at 3)
            """.trimIndent()
        )

        stringDiff("abcd", "bcda")

        stringDiff(
            "abc",
            "abccbacbac"
        )
    }
}

fun stringDiff(before: String, after: String, expectedLog: String? = null) {
    val (result, log) = executeListDiff(before.toCharArray().asList(), after.toCharArray().asList())
    if (expectedLog != null) {
        assertEquals(expectedLog, log.joinToString("\n"))
    }
    assertEquals(result.joinToString(separator = ""), after)
}

data class DiffResult<T>(val result: List<T>, val log: List<String>)

fun <T> executeListDiff(x: List<T>, y: List<T>): DiffResult<T> {
    val log = mutableListOf<String>()
    val result = x.toMutableList()
    executeDiff(x.size, y.size, object : DiffCallback {
        override fun areItemsTheSame(oldIndex: Int, newIndex: Int): Boolean {
            return x[oldIndex] == y[newIndex]
        }

        override fun insert(newIndex: Int) {
            log.add("Insert(${y[newIndex]} at $newIndex)")
            result.add(newIndex, y[newIndex])
        }

        override fun remove(atIndex: Int, oldIndex: Int) {
            log.add("Remove(${x[oldIndex]} at $atIndex)")
            result.removeAt(atIndex)
        }

        override fun same(oldIndex: Int, newIndex: Int) {
            log.add("Equals(x = $oldIndex, y = $newIndex)")
        }
    })
    return DiffResult(result, log)
}
