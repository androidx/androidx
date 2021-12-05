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

import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Drop
import androidx.testutils.DirectDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

internal fun <T : Any> adjacentInsertEvent(
    isPrepend: Boolean,
    page: List<T>,
    originalPageOffset: Int,
    placeholdersRemaining: Int
) = if (isPrepend) {
    localPrepend(
        pages = listOf(
            TransformablePage(
                originalPageOffset = originalPageOffset,
                data = page
            )
        ),
        placeholdersBefore = placeholdersRemaining,
    )
} else {
    localAppend(
        pages = listOf(
            TransformablePage(
                originalPageOffset = originalPageOffset,
                data = page
            )
        ),
        placeholdersAfter = placeholdersRemaining,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class PageEventTest {
    @Test
    fun placeholdersException() {
        assertFailsWith<IllegalArgumentException> {
            localRefresh<Char>(
                pages = listOf(),
                placeholdersBefore = 1,
                placeholdersAfter = -1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            localRefresh<Char>(
                pages = listOf(),
                placeholdersBefore = -1,
                placeholdersAfter = 1,
            )
        }
    }

    @Test
    fun dropType() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                minPageOffset = 0,
                maxPageOffset = 0,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropRange() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                minPageOffset = 2,
                maxPageOffset = 0,
                placeholdersRemaining = 4
            )
        }
    }

    @Test
    fun dropPlaceholders() {
        assertFailsWith<IllegalArgumentException> {
            Drop<Char>(
                loadType = REFRESH,
                minPageOffset = 0,
                maxPageOffset = 0,
                placeholdersRemaining = -1
            )
        }
    }

    @Test
    fun dropTransform() = runBlockingTest {
        val drop = Drop<Char>(
            loadType = PREPEND,
            minPageOffset = 0,
            maxPageOffset = 0,
            placeholdersRemaining = 0
        )

        assertSame(drop, drop.map { it + 1 })
        assertSame(drop, drop.flatMap { listOf(it, it) })
        assertSame(drop, drop.filter { false })
    }

    @Test
    fun stateTransform() = runBlockingTest {
        val state = localLoadStateUpdate<Char>(
            refreshLocal = LoadState.Loading
        )

        assertSame(state, state.map { it + 1 })
        assertSame(state, state.flatMap { listOf(it, it) })
        assertSame(state, state.filter { false })
    }

    @Test
    fun insertMap() = runBlockingTest {
        val insert = localAppend(
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersAfter = 4,
        )
        assertEquals(
            localAppend(
                pages = listOf(TransformablePage(listOf("a", "b"))),
                placeholdersAfter = 4,
            ),
            insert.map { it.toString() }
        )

        assertEquals(
            insert,
            insert.map { it.toString() }.map { it[0] }
        )
    }

    @Test
    fun insertMapTransformed() = runBlockingTest {
        assertEquals(
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf("a", "b"),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(0, 2)
                    )
                ),
                placeholdersAfter = 4,
            ),
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf("a", "b"),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(0, 2)
                    )
                ),
                placeholdersAfter = 4,
            ).map { it.toString() }
        )
    }

    @Test
    fun insertFilter() = runBlockingTest {
        val insert = localAppend(
            pages = listOf(TransformablePage(listOf('a', 'b', 'c', 'd'))),
            placeholdersAfter = 4,
        )

        // filter out C
        val insertNoC = insert.filter { it != 'c' }

        assertEquals(
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf('a', 'b', 'd'),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(0, 1, 3)
                    )
                ),
                placeholdersAfter = 4,
            ),
            insertNoC
        )

        // now filter out A, to validate filtration when lookup present
        assertEquals(
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf('b', 'd'),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(1, 3)
                    )
                ),
                placeholdersAfter = 4,
            ),
            insertNoC.filter { it != 'a' }
        )
    }

    @Test
    fun insertFlatMap() = runBlockingTest {
        val insert = localAppend(
            pages = listOf(TransformablePage(listOf('a', 'b'))),
            placeholdersAfter = 4,
        )

        val flatMapped = insert.flatMap {
            listOf("${it}1", "${it}2")
        }

        assertEquals(
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf("a1", "a2", "b1", "b2"),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(0, 0, 1, 1)
                    )
                ),
                placeholdersAfter = 4,
            ),
            flatMapped
        )

        val flatMappedAgain = flatMapped.flatMap {
            listOf(it, "-")
        }

        assertEquals(
            localAppend(
                pages = listOf(
                    TransformablePage(
                        originalPageOffsets = intArrayOf(0),
                        data = listOf("a1", "-", "a2", "-", "b1", "-", "b2", "-"),
                        hintOriginalPageOffset = 0,
                        hintOriginalIndices = listOf(0, 0, 0, 0, 1, 1, 1, 1)
                    )
                ),
                placeholdersAfter = 4,
            ),
            flatMappedAgain
        )
    }

    @RunWith(Parameterized::class)
    class StaticPagingData(
        private val original: PagingData<String>
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "original = {0}")
            fun initParameters() = listOf(
                PagingData.from(listOf("a", "b", "c")),
                PagingData.empty(),
            )
        }

        private val differ = TestPagingDataDiffer<String>(DirectDispatcher)

        @Test
        fun map() = runBlockingTest {
            val transform = { it: String -> it + it }
            differ.collectFrom(original)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.map(transform)
            val transformedPagingData = original.map { transform(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun flatMap() = runBlockingTest {
            val transform = { it: String -> listOf(it, it) }
            differ.collectFrom(original)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.flatMap(transform)
            val transformedPagingData = original.flatMap { transform(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun filter() = runBlockingTest {
            val predicate = { it: String -> it != "b" }
            differ.collectFrom(original)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.filter(predicate)
            val transformedPagingData = original.filter { predicate(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun insertSeparators() = runBlockingTest {
            val transform = { left: String?, right: String? ->
                if (left == null || right == null) null else "|"
            }
            differ.collectFrom(original)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.flatMapIndexed { index, s ->
                val result = mutableListOf<String>()
                if (index == 0) {
                    transform(null, s)?.let(result::add)
                }
                result.add(s)
                transform(s, originalItems.getOrNull(index + 1))?.let(result::add)
                if (index == originalItems.lastIndex) {
                    transform(s, null)?.let(result::add)
                }
                result
            }
            val transformedPagingData = original.insertSeparators { left, right ->
                transform(left, right)
            }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }
    }
}
