/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.paging.PagingSource.LoadResult.Page
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class PagingStateTest {
    @Test
    fun closestItemToPosition_withoutPlaceholders() {
        val pagingState = PagingState(
            pages = listOf(List(10) { it }).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 0
        )

        assertEquals(0, pagingState.closestItemToPosition(-1))
        assertEquals(5, pagingState.closestItemToPosition(5))
        assertEquals(9, pagingState.closestItemToPosition(15))
    }

    @Test
    fun closestItemToPosition_withPlaceholders() {
        val pagingState = PagingState(
            pages = listOf(List(10) { it }).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(0, pagingState.closestItemToPosition(5))
        assertEquals(5, pagingState.closestItemToPosition(15))
        assertEquals(9, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestItemToPosition_withEmptyPages() {
        val pagingState = PagingState(
            pages = listOf(
                listOf(),
                List(10) { it },
                listOf()
            ).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(0, pagingState.closestItemToPosition(5))
        assertEquals(5, pagingState.closestItemToPosition(15))
        assertEquals(9, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestItemToPosition_onlyEmptyPages() {
        val pagingState = PagingState(
            pages = listOf<List<Int>>(
                listOf(),
                listOf()
            ).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(null, pagingState.closestItemToPosition(5))
        assertEquals(null, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestPageToPosition_withoutPlaceholders() {
        val pages = List(10) { listOf(it) }.asPages()
        val pagingState = PagingState(
            pages = pages,
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 0
        )

        assertEquals(pages.first(), pagingState.closestPageToPosition(-1))
        assertEquals(pages[5], pagingState.closestPageToPosition(5))
        assertEquals(pages.last(), pagingState.closestPageToPosition(15))
    }

    @Test
    fun closestPageToPosition_withPlaceholders() {
        val pages = List(10) { listOf(it) }.asPages()
        val pagingState = PagingState(
            pages = pages,
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(pages.first(), pagingState.closestPageToPosition(5))
        assertEquals(pages[5], pagingState.closestPageToPosition(15))
        assertEquals(pages.last(), pagingState.closestPageToPosition(25))
    }

    @Test
    fun closestPageToPosition_withEmptyPages() {
        val pages = List(10) {
            when {
                it % 3 == 0 -> listOf()
                else -> listOf(it)
            }
        }.asPages()
        val pagingState = PagingState(
            pages = pages,
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(pages.first(), pagingState.closestPageToPosition(5))
        assertEquals(pages[5], pagingState.closestPageToPosition(13)) // pages[5].data == [5]
        assertEquals(pages.last(), pagingState.closestPageToPosition(25))
    }

    @Test
    fun closestPageToPosition_onlyEmptyPages() {
        val pagingState = PagingState(
            pages = listOf<List<Int>>(
                listOf(),
                listOf()
            ).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(null, pagingState.closestPageToPosition(5))
        assertEquals(null, pagingState.closestPageToPosition(25))
    }

    @Test
    fun itemOrNull_noPages() {
        val pagingState = PagingState(
            pages = listOf<Page<Int, Int>>(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(null, pagingState.firstItemOrNull())
        assertEquals(null, pagingState.lastItemOrNull())
    }

    @Test
    fun itemOrNull_emptyPages() {
        val pagingState = PagingState(
            pages = List(10) { listOf<Int>() }.asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(null, pagingState.firstItemOrNull())
        assertEquals(null, pagingState.lastItemOrNull())
    }

    @Test
    fun itemOrNull_emptyPagesAtEnds() {
        val pagingState = PagingState(
            pages = (listOf<List<Int>>() + List(10) { listOf(it) } + listOf()).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertEquals(0, pagingState.firstItemOrNull())
        assertEquals(9, pagingState.lastItemOrNull())
    }

    @Test
    fun isEmpty_noPages() {
        val pagingState = PagingState(
            pages = listOf<Page<Int, Int>>(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertTrue { pagingState.isEmpty() }
    }

    @Test
    fun isEmpty_emptyPages() {
        val pagingState = PagingState(
            pages = List(10) { listOf<Int>() }.asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertTrue { pagingState.isEmpty() }
    }

    @Test
    fun isEmpty_emptyPagesAtEnds() {
        val pagingState = PagingState(
            pages = (listOf<List<Int>>() + List(10) { listOf(it) } + listOf()).asPages(),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10),
            leadingPlaceholderCount = 10
        )

        assertFalse { pagingState.isEmpty() }
    }
}

private fun <T : Any> List<List<T>>.asPages() = mapIndexed { index, page: List<T> ->
    Page(
        data = page,
        prevKey = if (index > 0) index - 1 else null,
        nextKey = if (index < 9) index else null
    )
}
