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
import androidx.paging.PagingSource.LoadResult.Page
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@FlowPreview
@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class PagerStateTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun coerceWithHint_valid() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
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
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(-1, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(1, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStart() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(-2, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStartMultiplePages() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(-3, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(2, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceStartWithIndexInPage() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(-2, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(0, indexInPage)
                assertEquals(0, pageIndex)
                assertEquals(0, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEnd() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(2, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(1, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEndMultiplePages() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(3, 0).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(3, hintOffset)
            }
        }
    }

    @Test
    fun coerceWithHint_coerceEndWithIndexInPageOffset() = testScope.runBlockingTest {
        val pagerState = PagerState<Int, Int>(2, 6)

        pagerState.insert(0, REFRESH, Page(listOf(4, 5), 3, 6))
        advanceUntilIdle()
        pagerState.insert(0, START, Page(listOf(2, 3), 1, 4))
        advanceUntilIdle()
        pagerState.insert(0, END, Page(listOf(6, 7), 5, 8))
        advanceUntilIdle()

        with(pagerState) {
            ViewportHint(2, 2).withCoercedHint { indexInPage, pageIndex, hintOffset ->
                assertEquals(1, indexInPage)
                assertEquals(2, pageIndex)
                assertEquals(3, hintOffset)
            }
        }
    }
}
