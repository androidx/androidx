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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingSource.LoadResult.Page
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class PageFetcherSnapshotStateTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun coerceWithHint_valid() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(0, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(1, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceOnlyIndexInPage() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7]
            //          ^
            ViewportHint(-1, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(1, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStart() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [_, _], [2, 3], [4, 5], [6, 7]
            //  ^
            ViewportHint(-2, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(-2, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStartMultiplePages() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [_, _], [_, _], [2, 3], [4, 5], [6, 7]
            //  ^
            ViewportHint(-3, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(-4, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStartWithIndexInPage() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [_, _], [2, 3], [4, 5], [6, 7]
            //     ^
            ViewportHint(-2, 1).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(-1, hintOffset)
            }

            // Hint references element marked below:
            // [_, _], [_, _], [2, 3], [4, 5], [6, 7]
            //     ^
            ViewportHint(-2, -1).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(-3, hintOffset)
            }

            // Hint references element marked below:
            // [_, _], [2, 3], [4, 5], [6, 7]
            //          ^
            ViewportHint(-2, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEnd() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7], [_, _]
            //                          ^
            ViewportHint(2, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(1, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEndMultiplePages() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(pageSize = 2, maxSize = 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7], [_, _]
            //                             ^
            ViewportHint(2, 1).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(2, hintOffset)
            }

            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7], [_, _], [_, _]
            //                                  ^
            ViewportHint(3, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(3, hintOffset)
            }

            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7], [_, _]
            //                     ^
            ViewportHint(2, -1).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEndWithIndexInPageOffset() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6, hasRemoteState = false)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, PREPEND, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, APPEND, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            // Hint references element marked below:
            // [2, 3], [4, 5], [6, 7], [_, _], [_, _]
            //                                  ^
            ViewportHint(2, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(3, hintOffset)
            }
        }
    }
}
