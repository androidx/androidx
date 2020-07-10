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
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class PageFetcherSnapshotStateTest {

    @Test
    fun placeholders_uncounted() {
        val pagerState = PageFetcherSnapshotState<Int, Int>(
            config = PagingConfig(2, enablePlaceholders = false),
            hasRemoteState = false
        )

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)

        pagerState.insert(
            loadId = 0, loadType = REFRESH, page = Page(
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
            loadId = 0, loadType = PREPEND, page = Page(
                data = listOf(),
                prevKey = -2,
                nextKey = 0,
                itemsBefore = 25
            )
        )
        pagerState.insert(
            loadId = 0, loadType = APPEND, page = Page(
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

        pagerState.drop(loadType = PREPEND, pageCount = 1, placeholdersRemaining = 100)
        pagerState.drop(loadType = APPEND, pageCount = 1, placeholdersRemaining = 100)

        assertEquals(0, pagerState.placeholdersBefore)
        assertEquals(0, pagerState.placeholdersAfter)
    }

    @Test
    fun placeholders_counted() {
        val pagerState = PageFetcherSnapshotState<Int, Int>(
            config = PagingConfig(2, enablePlaceholders = true),
            hasRemoteState = false
        )

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

        pagerState.drop(loadType = PREPEND, pageCount = 1, placeholdersRemaining = 100)
        pagerState.drop(loadType = APPEND, pageCount = 1, placeholdersRemaining = 100)

        assertEquals(100, pagerState.placeholdersBefore)
        assertEquals(100, pagerState.placeholdersAfter)
    }
}
