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

import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import com.google.common.truth.Truth.assertThat
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
    val testScope = TestCoroutineScope()

    @Test
    fun placeholders_uncounted() = testScope.runBlockingTest {
        val pagerState = PageFetcherSnapshotState.Holder<Int, Int>(
            config = PagingConfig(2, enablePlaceholders = false)
        ).withLock { it }

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        pagerState.insert(
            loadId = 0, loadType = REFRESH,
            page = Page(
                data = listOf(),
                prevKey = -1,
                nextKey = 1,
                itemsBefore = 50,
                itemsAfter = 50
            )
        )

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        pagerState.insert(
            loadId = 0, loadType = PREPEND,
            page = Page(
                data = listOf(),
                prevKey = -2,
                nextKey = 0,
                itemsBefore = 25
            )
        )
        pagerState.insert(
            loadId = 0, loadType = APPEND,
            page = Page(
                data = listOf(),
                prevKey = 0,
                nextKey = 2,
                itemsBefore = 25
            )
        )

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        // Should automatically decrement remaining placeholders when counted.
        pagerState.insert(
            loadId = 0,
            loadType = PREPEND,
            page = Page(
                data = listOf(0),
                prevKey = -3,
                nextKey = 0,
                itemsBefore = COUNT_UNDEFINED,
                itemsAfter = COUNT_UNDEFINED
            )
        )
        pagerState.insert(
            loadId = 0,
            loadType = APPEND,
            page = Page(
                data = listOf(0),
                prevKey = 0,
                nextKey = 3,
                itemsBefore = COUNT_UNDEFINED,
                itemsAfter = COUNT_UNDEFINED
            )
        )

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        pagerState.drop(
            event = PageEvent.Drop(
                loadType = PREPEND,
                minPageOffset = -2,
                maxPageOffset = -2,
                placeholdersRemaining = 100
            )
        )
        pagerState.drop(
            event = PageEvent.Drop(
                loadType = APPEND,
                minPageOffset = 2,
                maxPageOffset = 2,
                placeholdersRemaining = 100
            )
        )

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)
    }

    @Test
    fun placeholders_counted() = testScope.runBlockingTest {
        val pagerState = PageFetcherSnapshotState.Holder<Int, Int>(
            config = PagingConfig(2, enablePlaceholders = true)
        ).withLock { it }

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        pagerState.insert(
            loadId = 0,
            loadType = REFRESH,
            page = Page(
                data = listOf(),
                prevKey = -1,
                nextKey = 1,
                itemsBefore = 50,
                itemsAfter = 50
            )
        )

        assertEquals(50, pagerState.placeholdersBefore)
        assertEquals(50, pagerState.placeholdersAfter)

        pagerState.insert(
            loadId = 0,
            loadType = PREPEND,
            page = Page(
                data = listOf(),
                prevKey = -2,
                nextKey = 0,
                itemsBefore = 25,
                itemsAfter = COUNT_UNDEFINED
            )
        )
        pagerState.insert(
            loadId = 0,
            loadType = APPEND,
            page = Page(
                data = listOf(),
                prevKey = 0,
                nextKey = 2,
                itemsBefore = COUNT_UNDEFINED,
                itemsAfter = 25
            )
        )

        assertEquals(25, pagerState.placeholdersBefore)
        assertEquals(25, pagerState.placeholdersAfter)

        // Should automatically decrement remaining placeholders when counted.
        pagerState.insert(
            loadId = 0,
            loadType = PREPEND,
            page = Page(
                data = listOf(0),
                prevKey = -3,
                nextKey = 0,
                itemsBefore = COUNT_UNDEFINED,
                itemsAfter = COUNT_UNDEFINED
            )
        )
        pagerState.insert(
            loadId = 0,
            loadType = APPEND,
            page = Page(
                data = listOf(0),
                prevKey = 0,
                nextKey = 3,
                itemsBefore = COUNT_UNDEFINED,
                itemsAfter = COUNT_UNDEFINED
            )
        )

        assertEquals(24, pagerState.placeholdersBefore)
        assertEquals(24, pagerState.placeholdersAfter)

        pagerState.drop(
            event = PageEvent.Drop(
                loadType = PREPEND,
                minPageOffset = -2,
                maxPageOffset = -2,
                placeholdersRemaining = 100
            )
        )
        pagerState.drop(
            event = PageEvent.Drop(
                loadType = APPEND,
                minPageOffset = 2,
                maxPageOffset = 2,
                placeholdersRemaining = 100
            )
        )

        assertEquals(100, pagerState.placeholdersBefore)
        assertEquals(100, pagerState.placeholdersAfter)
    }

    @Test
    fun currentPagingState() = testScope.runBlockingTest {
        val config = PagingConfig(pageSize = 2)
        val state = PageFetcherSnapshotState.Holder<Int, Int>(config = config).withLock { it }

        val pages = listOf(
            Page(
                data = listOf(2, 3),
                prevKey = 1,
                nextKey = 4
            ),
            Page(
                data = listOf(4, 5),
                prevKey = 3,
                nextKey = 6,
                itemsBefore = 4,
                itemsAfter = 4
            ),
            Page(
                data = listOf(6, 7),
                prevKey = 5,
                nextKey = 8
            )
        )

        state.insert(0, REFRESH, pages[1])
        state.insert(0, PREPEND, pages[0])
        state.insert(0, APPEND, pages[2])

        val presenter = pages.toPresenter(1)
        val presenterMissingPrepend = pages.drop(1).toPresenter(0)
        val presenterMissingAppend = pages.dropLast(1).toPresenter(1)
        val presenterExtraPrepend = pages.toMutableList().apply {
            add(
                0,
                Page(
                    data = listOf(0, 1),
                    prevKey = null,
                    nextKey = 2
                )
            )
        }.toPresenter(2)
        val presenterExtraAppend = pages.toMutableList().apply {
            add(
                Page(
                    data = listOf(8, 9),
                    prevKey = 7,
                    nextKey = null
                )
            )
        }.toPresenter(1)

        // Hint in loaded items, fetcher state == presenter state.
        assertThat(state.currentPagingState(presenter.accessHintForPresenterIndex(4)))
            .isEqualTo(
                PagingState(
                    pages = pages,
                    anchorPosition = 4,
                    config = config,
                    leadingPlaceholderCount = 2
                )
            )

        // Hint in placeholders before, fetcher state == presenter state.
        assertThat(state.currentPagingState(presenter.accessHintForPresenterIndex(0)))
            .isEqualTo(
                PagingState(
                    pages = pages,
                    anchorPosition = 0,
                    config = config,
                    leadingPlaceholderCount = 2
                )
            )

        // Hint in placeholders after, fetcher state == presenter state.
        assertThat(state.currentPagingState(presenter.accessHintForPresenterIndex(9)))
            .isEqualTo(
                PagingState(
                    pages = pages,
                    anchorPosition = 9,
                    config = config,
                    leadingPlaceholderCount = 2
                )
            )

        // Hint in loaded items, fetcher state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterMissingPrepend.accessHintForPresenterIndex(4))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 4,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders before, fetcher state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterMissingPrepend.accessHintForPresenterIndex(0))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 0,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders after, fetcher state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterMissingPrepend.accessHintForPresenterIndex(9))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 9,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in loaded items, fetcher state has an extra appended page.
        assertThat(
            state.currentPagingState(presenterMissingAppend.accessHintForPresenterIndex(4))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 4,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders before, fetcher state has an extra appended page.
        assertThat(
            state.currentPagingState(presenterMissingAppend.accessHintForPresenterIndex(0))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 0,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders after, fetcher state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterMissingAppend.accessHintForPresenterIndex(9))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 9,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in loaded items, presenter state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterExtraPrepend.accessHintForPresenterIndex(4))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 4,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders before, presenter state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterExtraPrepend.accessHintForPresenterIndex(0))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 0,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders after, presenter state has an extra prepended page.
        assertThat(
            state.currentPagingState(presenterExtraPrepend.accessHintForPresenterIndex(9))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 9,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in loaded items, presenter state has an extra appended page.
        assertThat(
            state.currentPagingState(presenterExtraAppend.accessHintForPresenterIndex(4))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 4,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders before, presenter state has an extra appended page.
        assertThat(
            state.currentPagingState(presenterExtraAppend.accessHintForPresenterIndex(0))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 0,
                config = config,
                leadingPlaceholderCount = 2
            )
        )

        // Hint in placeholders after, fetcher state has an extra appended page.
        assertThat(
            state.currentPagingState(presenterExtraAppend.accessHintForPresenterIndex(9))
        ).isEqualTo(
            PagingState(
                pages = pages,
                anchorPosition = 9,
                config = config,
                leadingPlaceholderCount = 2
            )
        )
    }

    @Test
    fun loadStates() = testScope.runBlockingTest {
        val config = PagingConfig(pageSize = 2)
        val state = PageFetcherSnapshotState.Holder<Int, Int>(config = config).withLock { it }

        // assert initial state
        assertThat(state.sourceLoadStates.snapshot()).isEqualTo(loadStates(refresh = Loading))

        // assert APPEND state is updated
        state.sourceLoadStates.set(APPEND, NotLoading.Complete)
        assertThat(state.sourceLoadStates.snapshot()).isEqualTo(
            loadStates(
                refresh = Loading,
                append = NotLoading.Complete
            )
        )

        // assert REFRESH state is incrementally updated
        state.sourceLoadStates.set(REFRESH, NotLoading.Incomplete)
        assertThat(state.sourceLoadStates.snapshot()).isEqualTo(
            loadStates(
                refresh = NotLoading.Incomplete,
                append = NotLoading.Complete,
            )
        )
        assertThat(state.sourceLoadStates.get(APPEND)).isEqualTo(NotLoading.Complete)
    }

    private fun List<Page<Int, Int>>.toPresenter(initialPageIndex: Int): PagePresenter<Int> {
        val pageSize = 2
        val initialPage = get(initialPageIndex)
        val presenter = PagePresenter(
            insertEvent = localRefresh(
                pages = listOf(TransformablePage(initialPage.data)),
                placeholdersBefore = initialPage.itemsBefore,
                placeholdersAfter = initialPage.itemsAfter,
            )
        )

        for (i in 0 until initialPageIndex) {
            val offset = i + 1
            presenter.processEvent(
                localPrepend(
                    pages = listOf(
                        TransformablePage(originalPageOffset = -offset, data = get(i).data)
                    ),
                    placeholdersBefore = initialPage.itemsBefore - (offset * pageSize),
                ),
                ProcessPageEventCallbackCapture()
            )
        }

        for (i in (initialPageIndex + 1)..lastIndex) {
            val offset = i - initialPageIndex
            presenter.processEvent(
                localAppend(
                    pages = listOf(
                        TransformablePage(originalPageOffset = offset, data = get(i).data)
                    ),
                    placeholdersAfter = initialPage.itemsAfter - (offset * pageSize),
                ),
                ProcessPageEventCallbackCapture()
            )
        }

        return presenter
    }
}
