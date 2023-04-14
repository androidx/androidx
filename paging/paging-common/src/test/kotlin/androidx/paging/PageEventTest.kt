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
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import org.junit.Before

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
    fun dropTransform() = runTest(UnconfinedTestDispatcher()) {
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
    fun stateTransform() = runTest(UnconfinedTestDispatcher()) {
        val state = localLoadStateUpdate<Char>(
            refreshLocal = LoadState.Loading
        )

        assertSame(state, state.map { it + 1 })
        assertSame(state, state.flatMap { listOf(it, it) })
        assertSame(state, state.filter { false })
    }

    @Test
    fun insertMap() = runTest(UnconfinedTestDispatcher()) {
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
    fun insertMapTransformed() = runTest(UnconfinedTestDispatcher()) {
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
    fun insertFilter() = runTest(UnconfinedTestDispatcher()) {
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
    fun insertFlatMap() = runTest(UnconfinedTestDispatcher()) {
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
        private val data: List<String>
    ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "data = {0}")
            fun initParameters() = listOf(
                listOf("a", "b", "c"),
                emptyList(),
            )
        }

        private val differ = TestPagingDataDiffer<String>(EmptyCoroutineContext)
        private lateinit var pagingData: PagingData<String>

        @Before
        fun init() {
            pagingData = if (data.isNotEmpty()) {
                PagingData.from(data)
            } else {
                PagingData.empty()
            }
        }

        @Test
        fun map() = runTest(UnconfinedTestDispatcher()) {
            val transform = { it: String -> it + it }
            differ.collectFrom(pagingData)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.map(transform)
            val transformedPagingData = pagingData.map { transform(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun flatMap() = runTest(UnconfinedTestDispatcher()) {
            val transform = { it: String -> listOf(it, it) }
            differ.collectFrom(pagingData)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.flatMap(transform)
            val transformedPagingData = pagingData.flatMap { transform(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun filter() = runTest(UnconfinedTestDispatcher()) {
            val predicate = { it: String -> it != "b" }
            differ.collectFrom(pagingData)
            val originalItems = differ.snapshot().items
            val expectedItems = originalItems.filter(predicate)
            val transformedPagingData = pagingData.filter { predicate(it) }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }

        @Test
        fun insertSeparators() = runTest(UnconfinedTestDispatcher()) {
            val transform = { left: String?, right: String? ->
                if (left == null || right == null) null else "|"
            }
            differ.collectFrom(pagingData)
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
            val transformedPagingData = pagingData.insertSeparators { left, right ->
                transform(left, right)
            }
            differ.collectFrom(transformedPagingData)
            assertEquals(expectedItems, differ.snapshot().items)
        }
    }
}
