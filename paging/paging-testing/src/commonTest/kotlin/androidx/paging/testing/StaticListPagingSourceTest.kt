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

package androidx.paging.testing

import androidx.kruth.assertThat
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class StaticListPagingSourceTest {

    private val DATA = List(100) { it }
    private val CONFIG = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
    )

    @Test
    fun refresh() = runPagingSourceTest { _, pager ->
        val result = pager.refresh() as LoadResult.Page
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = listOf(0, 1, 2, 3, 4),
                prevKey = null,
                nextKey = 5,
                itemsBefore = 0,
                itemsAfter = 95
            )
        )
    }

    @Test
    fun refresh_withEmptyData() = runPagingSourceTest(StaticListPagingSource(emptyList())) {
            _, pager ->

        val result = pager.refresh() as LoadResult.Page
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
                itemsBefore = 0,
                itemsAfter = 0
            )
        )
    }

    @Test
    fun refresh_initialKey() = runPagingSourceTest { _, pager ->
        val result = pager.refresh(initialKey = 20) as LoadResult.Page
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = listOf(20, 21, 22, 23, 24),
                prevKey = 19,
                nextKey = 25,
                itemsBefore = 20,
                itemsAfter = 75
            )
        )
    }

    @Test
    fun refresh_initialKey_withEmptyData() = runPagingSourceTest(
        StaticListPagingSource(emptyList())
    ) { _, pager ->

        val result = pager.refresh(initialKey = 20) as LoadResult.Page
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
                itemsBefore = 0,
                itemsAfter = 0
            )
        )
    }

    @Test
    fun refresh_negativeKeyClippedToZero() = runPagingSourceTest { _, pager ->
        val result = pager.refresh(initialKey = -1) as LoadResult.Page
        // loads first page
        assertThat(result).isEqualTo(
            listOf(0, 1, 2, 3, 4).asPage()
        )
    }

    @Test
    fun refresh_KeyLargerThanDataSize_loadsLastPage() = runPagingSourceTest { _, pager ->
        val result = pager.refresh(initialKey = 140) as LoadResult.Page
        // loads last page
        assertThat(result).isEqualTo(
            listOf(95, 96, 97, 98, 99).asPage()
        )
    }

    @Test
    fun append() = runPagingSourceTest { _, pager ->
        pager.run {
            refresh()
            append()
        }
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                LoadResult.Page(
                    data = listOf(0, 1, 2, 3, 4),
                    prevKey = null,
                    nextKey = 5,
                    itemsBefore = 0,
                    itemsAfter = 95
                ),
                LoadResult.Page(
                    data = listOf(5, 6, 7),
                    prevKey = 4,
                    nextKey = 8,
                    itemsBefore = 5,
                    itemsAfter = 92
                )
            )
        ).inOrder()
    }

    @Test
    fun append_consecutively() = runPagingSourceTest { _, pager ->
        pager.run {
            refresh()
            append()
            append()
            append()
        }
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                listOf(0, 1, 2, 3, 4).asPage(),
                listOf(5, 6, 7).asPage(),
                listOf(8, 9, 10).asPage(),
                listOf(11, 12, 13).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun append_loadSizeLargerThanAvailableData() = runPagingSourceTest { _, pager ->
        val result = pager.run {
            refresh(initialKey = 94)
            append() as LoadResult.Page
        }
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = listOf(99),
                prevKey = 98,
                nextKey = null,
                itemsBefore = 99,
                itemsAfter = 0
            )
        )
    }

    @Test
    fun prepend() = runPagingSourceTest { _, pager ->
        pager.run {
            refresh(20)
            prepend()
        }
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                LoadResult.Page(
                    data = listOf(17, 18, 19),
                    prevKey = 16,
                    nextKey = 20,
                    itemsBefore = 17,
                    itemsAfter = 80
                ),
                LoadResult.Page(
                    data = listOf(20, 21, 22, 23, 24),
                    prevKey = 19,
                    nextKey = 25,
                    itemsBefore = 20,
                    itemsAfter = 75
                ),
            )
        ).inOrder()
    }

    @Test
    fun prepend_consecutively() = runPagingSourceTest { _, pager ->
        pager.run {
            refresh(initialKey = 50)
            prepend()
            prepend()
            prepend()
        }
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                listOf(41, 42, 43).asPage(),
                listOf(44, 45, 46).asPage(),
                listOf(47, 48, 49).asPage(),
                listOf(50, 51, 52, 53, 54).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun prepend_loadSizeLargerThanAvailableData() = runPagingSourceTest { _, pager ->
        val result = pager.run {
            refresh(initialKey = 2)
            prepend()
        }
        assertThat(result).isEqualTo(
            LoadResult.Page(
                data = listOf(0, 1),
                prevKey = null,
                nextKey = 2,
                itemsBefore = 0,
                itemsAfter = 98
            )
        )
    }

    @Test
    fun jump_enabled() {
        val source = StaticListPagingSource(DATA)
        assertThat(source.jumpingSupported).isTrue()
    }

    @Test
    fun refreshKey() = runPagingSourceTest { pagingSource, pager ->
        val state = pager.run {
            refresh() // [0, 1, 2, 3, 4]
            append() // [5, 6, 7]
            // the anchorPos should be 7
            getPagingState(anchorPosition = 7)
        }

        val refreshKey = pagingSource.getRefreshKey(state)
        val expected = 7 - (CONFIG.initialLoadSize / 2)
        assertThat(expected).isEqualTo(5)
        assertThat(refreshKey).isEqualTo(expected)
    }

    @Test
    fun refreshKey_negativeKeyClippedToZero() = runPagingSourceTest { pagingSource, pager ->
        val state = pager.run {
            refresh(2) // [2, 3, 4, 5, 6]
            prepend() // [0, 1]
            getPagingState(anchorPosition = 1)
        }
        // before clipping, refreshKey = 1 - (CONFIG.initialLoadSize / 2) = -1
        val refreshKey = pagingSource.getRefreshKey(state)
        assertThat(refreshKey).isEqualTo(0)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun runPagingSourceTest(
        source: PagingSource<Int, Int> = StaticListPagingSource(DATA),
        pager: TestPager<Int, Int> = TestPager(CONFIG, source),
        block: suspend (pagingSource: PagingSource<Int, Int>, pager: TestPager<Int, Int>) -> Unit
    ) {
        runTest {
            block(source, pager)
        }
    }

    private fun List<Int>.asPage(): LoadResult.Page<Int, Int> {
        val indexStart = firstOrNull()
        val indexEnd = lastOrNull()
        return LoadResult.Page(
            data = this,
            prevKey = indexStart?.let {
                if (indexStart <= 0 || isEmpty()) null else indexStart - 1
            },
            nextKey = indexEnd?.let {
                if (indexEnd >= DATA.lastIndex || isEmpty()) null else indexEnd + 1
            },
            itemsBefore = indexStart ?: -1,
            itemsAfter = if (indexEnd == null) -1 else DATA.lastIndex - indexEnd
        )
    }
}
