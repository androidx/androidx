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
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private val LoadStatesIdle = mapOf(
    REFRESH to Idle,
    START to Idle,
    END to Idle
)

internal fun <T : Any> adjacentInsertEvent(
    isPrepend: Boolean,
    page: List<T>,
    originalPageOffset: Int,
    placeholdersRemaining: Int
) = if (isPrepend) {
    Start(
        pages = listOf(
            TransformablePage(
                originalPageOffset = originalPageOffset,
                data = page
            )
        ),
        placeholdersStart = placeholdersRemaining,
        loadStates = LoadStatesIdle
    )
} else {
    End(
        pages = listOf(
            TransformablePage(
                originalPageOffset = originalPageOffset,
                data = page
            )
        ),
        placeholdersEnd = placeholdersRemaining,
        loadStates = LoadStatesIdle
    )
}

@RunWith(JUnit4::class)
class PageEventTest {

    @Test
    fun placeholdersException() {
        assertFailsWith<IllegalArgumentException> {
            Refresh<Char>(
                pages = listOf(),
                placeholdersStart = 1,
                placeholdersEnd = -1,
                loadStates = LoadStatesIdle
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Refresh<Char>(
                pages = listOf(),
                placeholdersStart = -1,
                placeholdersEnd = 1,
                loadStates = LoadStatesIdle
            )
        }
    }

    @Test
    fun dropType() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                count = 2,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropCount() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                count = -1,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropPlaceholders() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                count = 1,
                placeholdersRemaining = -1
            )
        }
    }

    @Test
    fun dropTransform() {
        val drop = Drop<Char>(
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
            loadState = Loading
        )

        assertSame(state, state.map { it + 1 })
        assertSame(state, state.flatMap { listOf(it, it) })
        assertSame(state, state.filter { false })
    }

    @Test
    fun insertMap() {
        val insert = End(
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersEnd = 4,
            loadStates = LoadStatesIdle
        )
        assertEquals(
            End(
                pages = listOf(TransformablePage(listOf("a", "b"))),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
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
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a", "b"),
                        originalPageSize = 4,
                        originalIndices = listOf(0, 2)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ),
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf('a', 'b'),
                        originalPageSize = 4,
                        originalIndices = listOf(0, 2)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ).map { it.toString() }
        )
    }

    @Test
    fun insertFilter() {
        val insert = End(
            pages = listOf(TransformablePage(listOf('a', 'b', 'c', 'd'))),
            placeholdersEnd = 4,
            loadStates = mapOf(
                REFRESH to Idle,
                START to Idle,
                END to Idle
            )
        )

        // filter out C
        val insertNoC = insert.filter { it != 'c' }
        assertEquals(
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf('a', 'b', 'd'),
                        originalPageSize = 4,
                        originalIndices = listOf(0, 1, 3)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ),
            insertNoC
        )

        // now filter out A, to validate filtration when lookup present
        assertEquals(
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf('b', 'd'),
                        originalPageSize = 4,
                        originalIndices = listOf(1, 3)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ),
            insertNoC.filter { it != 'a' }
        )
    }

    @Test
    fun insertFlatMap() {
        val insert = End(
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersEnd = 4,
            loadStates = LoadStatesIdle
        )

        val flatMapped = insert.flatMap {
            listOf("${it}1", "${it}2")
        }

        assertEquals(
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a1", "a2", "b1", "b2"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 0, 1, 1)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ),
            flatMapped
        )

        val flatMappedAgain = flatMapped.flatMap {
            listOf(it, "-")
        }

        assertEquals(
            End(
                pages = listOf(
                    TransformablePage(
                        originalPageOffset = 0,
                        data = listOf("a1", "-", "a2", "-", "b1", "-", "b2", "-"),
                        originalPageSize = 2,
                        originalIndices = listOf(0, 0, 0, 0, 1, 1, 1, 1)
                    )
                ),
                placeholdersEnd = 4,
                loadStates = LoadStatesIdle
            ),
            flatMappedAgain
        )
    }
}
