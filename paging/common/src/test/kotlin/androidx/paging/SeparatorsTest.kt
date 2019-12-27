/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import androidx.paging.LoadState.Idle
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

private fun <T : Any> assertEvent(expected: PageEvent<T>, actual: PageEvent<T>) {
    try {
        assertEquals(expected, actual)
    } catch (e: Throwable) {
        throw AssertionError(
            e.localizedMessage
                .replace("),", "),\n")
                .replace("<[", "<[\n ")
                .replace("actual", "\nactual\n")
                .replace("Expected", "\nExpected\n")
                .replace("pages=", "pages=\n")
        )
    }
}

@RunWith(JUnit4::class)
class SeparatorsTest {
    @Test
    fun separatorDrop() {
        val initialState =
            listOf('a', 'b', 'c', 'd')
                .map { listOf(it) }
                .toTransformablePages()
        val outDrop = Drop<Char>(
            loadType = END,
            count = 2,
            placeholdersRemaining = 4
        ).insertSeparators(
            currentPages = initialState
        ) { _, _ -> null }

        // drop count always simply doubles, because each N pages, after separators, become 2N - 1
        assertEvent(
            Drop(
                loadType = END,
                count = 4,
                placeholdersRemaining = 4
            ),
            outDrop
        )
    }

    @Test
    fun separatorRefresh() {
        val initialState = mutableListOf<TransformablePage<String>>()
        val outInsert = PageEvent.Insert.Refresh(
            pages = listOf(
                listOf("a2", "b1"),
                listOf("c1", "c2")
            ).toTransformablePages(),
            placeholdersStart = 0,
            placeholdersEnd = 1,
            loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
        ).insertSeparators(
            currentPages = initialState
        ) { before, after ->
            if (before != null && after != null && before.first() != after.first())
                after.first().toUpperCase().toString()
            else null
        }

        assertEvent(
            PageEvent.Insert.Refresh(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a2", "B", "b1"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1, 1)
                    ),
                    TransformablePage(
                        originalPageOffset = 1,
                        data = listOf("C"),
                        originalPageSize = 2,
                        originalIndices = listOf(0)
                    ),
                    TransformablePage(
                        originalPageOffset = 1,
                        data = listOf("c1", "c2"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1)
                    )
                ),
                placeholdersStart = 0,
                placeholdersEnd = 1,
                loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
            ),
            outInsert
        )
    }

    @Test
    fun separatorEnd() {
        val initialState =
            listOf("a1", "a2")
                .map { listOf(it) }
                .toTransformablePages()
        val outInsert = PageEvent.Insert.End(
            pages = listOf(
                listOf("c1", "d1"),
                listOf("d2", "d3")
            ).toTransformablePages(-1),
            placeholdersEnd = 1,
            loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
        ).insertSeparators(
            currentPages = initialState
        ) { before, after ->
            if (before != null && after != null && before.first() != after.first())
                after.first().toUpperCase().toString()
            else null
        }

        assertEvent(
            PageEvent.Insert.End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 1,
                        data = listOf("C"),
                        originalPageSize = 2,
                        originalIndices = listOf(0)
                    ),
                    TransformablePage(
                        originalPageOffset = 1,
                        data = listOf("c1", "D", "d1"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1, 1)
                    ),
                    TransformablePage(
                        originalPageOffset = 2,
                        data = listOf(),
                        originalPageSize = 2,
                        originalIndices = null
                    ),
                    TransformablePage(
                        originalPageOffset = 2,
                        data = listOf("d2", "d3"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1)
                    )
                ),
                placeholdersEnd = 1,
                loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
            ),
            outInsert
        )
    }

    @Test
    fun separatorStart() {
        val initialState =
            listOf("d1", "d2")
                .map { listOf(it) }
                .toTransformablePages()
        val outInsert = PageEvent.Insert.Start(
            pages = listOf(
                listOf("a1", "b1"),
                listOf("b2", "b3")
            ).toTransformablePages(2),
            placeholdersStart = 1,
            loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
        ).insertSeparators(
            currentPages = initialState
        ) { before, after ->
            if (before != null && after != null && before.first() != after.first())
                after.first().toUpperCase().toString()
            else null
        }

        assertEvent(
            PageEvent.Insert.Start(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = -2,
                        data = listOf("a1", "B", "b1"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1, 1)
                    ),
                    TransformablePage(
                        originalPageOffset = -1,
                        data = listOf(),
                        originalPageSize = 2,
                        originalIndices = null
                    ),
                    TransformablePage(
                        originalPageOffset = -1,
                        data = listOf("b2", "b3"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 1)
                    ),
                    TransformablePage(
                        originalPageOffset = -1,
                        data = listOf("D"),
                        originalPageSize = 2,
                        originalIndices = listOf(1) // note: using last index of 2nd page in
                    )
                ),
                placeholdersStart = 1,
                loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
            ),
            outInsert
        )
    }
}