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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@Suppress("TestFunctionName")
private fun <T : Any> TransformablePage(data: List<T>) = TransformablePage(
    data = data,
    originalPageOffset = 0
)

@RunWith(JUnit4::class)
class PageEventTest {
    @Test
    fun placeholdersException() {
        assertFailsWith<IllegalArgumentException> {
            PageEvent.Insert<Char>(
                loadType = REFRESH,
                pages = listOf(),
                placeholdersStart = 1,
                placeholdersEnd = -1
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PageEvent.Insert<Char>(
                loadType = REFRESH,
                pages = listOf(),
                placeholdersStart = -1,
                placeholdersEnd = 1
            )
        }
    }

    @Test
    fun dropType() {
        assertFailsWith<IllegalArgumentException> {
            PageEvent.Drop<Char>(
                loadType = REFRESH,
                count = 2,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropCount() {
        assertFailsWith<IllegalArgumentException> {
            PageEvent.Drop<Char>(
                loadType = REFRESH,
                count = -1,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropPlaceholders() {
        assertFailsWith<IllegalArgumentException> {
            PageEvent.Drop<Char>(
                loadType = REFRESH,
                count = 1,
                placeholdersRemaining = -1
            )
        }
    }

    @Test
    fun dropTransform() {
        val drop = PageEvent.Drop<Char>(
            loadType = START,
            count = 0,
            placeholdersRemaining = 0
        )

        assertSame(drop, drop.map { it + 1 })
        assertSame(drop, drop.flatMap { listOf(it, it) })
        assertSame(drop, drop.filter { false })
    }

    @Test
    fun stateTransform() {
        val state = PageEvent.StateUpdate<Char>(
            loadType = REFRESH,
            loadState = LoadState.Loading
        )

        assertSame(state, state.map { it + 1 })
        assertSame(state, state.flatMap { listOf(it, it) })
        assertSame(state, state.filter { false })
    }

    @Test
    fun insertMap() {
        val insert = PageEvent.Insert(
            loadType = END,
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersStart = 2,
            placeholdersEnd = 4
        )
        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(TransformablePage(listOf("a", "b"))),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            insert.map { it.toString() }
        )

        assertEquals(
            insert,
            insert.map { it.toString() }.map { it[0] }
        )
    }

    @Test
    fun insertMapTransformed() {
        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(TransformablePage(
                    originalPageOffset = 0,
                    data = listOf("a", "b"),
                    sourcePageSize = 4,
                    originalIndices = listOf(0, 2)
                )),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            PageEvent.Insert(
                loadType = END,
                pages = listOf(TransformablePage(
                    originalPageOffset = 0,
                    data = listOf('a', 'b'),
                    sourcePageSize = 4,
                    originalIndices = listOf(0, 2)
                )),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ).map { it.toString() }
        )
    }

    @Test
    fun insertFilter() {
        val insert = PageEvent.Insert(
            loadType = END,
            pages = listOf(TransformablePage(listOf('a', 'b', 'c', 'd'))),
            placeholdersStart = 2,
            placeholdersEnd = 4
        )

        // filter out C
        val insertNoC = insert.filter { it != 'c' }
        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf('a', 'b', 'd'),
                        sourcePageSize = 4,
                        originalIndices = listOf(0, 1, 3)
                    )
                ),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            insertNoC
        )

        // now filter out A, to validate filtration when lookup present
        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf('b', 'd'),
                        sourcePageSize = 4,
                        originalIndices = listOf(1, 3)
                    )
                ),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            insertNoC.filter { it != 'a' }
        )
    }

    @Test
    fun insertFlatMap() {
        val insert = PageEvent.Insert(
            loadType = END,
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersStart = 2,
            placeholdersEnd = 4
        )

        val flatMapped = insert.flatMap {
            listOf("${it}1", "${it}2")
        }

        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a1", "a2", "b1", "b2"),
                        sourcePageSize = 2,
                        originalIndices = listOf(0, 0, 1, 1)
                    )
                ),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            flatMapped
        )

        val flatMappedAgain = flatMapped.flatMap {
            listOf(it, "-")
        }

        assertEquals(
            PageEvent.Insert(
                loadType = END,
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a1", "-", "a2", "-", "b1", "-", "b2", "-"),
                        sourcePageSize = 2,
                        originalIndices = listOf(0, 0, 0, 0, 1, 1, 1, 1)
                    )
                ),
                placeholdersStart = 2,
                placeholdersEnd = 4
            ),
            flatMappedAgain
        )
    }
}