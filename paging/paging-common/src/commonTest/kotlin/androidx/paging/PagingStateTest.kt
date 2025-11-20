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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagingStateTest {
    @Test
    fun closestItemToPosition_withoutPlaceholders() {
        val pagingState =
            PagingState(
                pages = listOf(List(10) { it }).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            )

        assertEquals(0, pagingState.closestItemToPosition(-1))
        assertEquals(5, pagingState.closestItemToPosition(5))
        assertEquals(9, pagingState.closestItemToPosition(15))
    }

    @Test
    fun closestItemToPosition_withPlaceholders() {
        val pagingState =
            PagingState(
                pages = listOf(List(10) { it }).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(0, pagingState.closestItemToPosition(5))
        assertEquals(5, pagingState.closestItemToPosition(15))
        assertEquals(9, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestItemToPosition_withEmptyPages() {
        val pagingState =
            PagingState(
                pages = listOf(listOf(), List(10) { it }, listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(0, pagingState.closestItemToPosition(5))
        assertEquals(5, pagingState.closestItemToPosition(15))
        assertEquals(9, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestItemToPosition_onlyEmptyPages() {
        val pagingState =
            PagingState(
                pages = listOf<List<Int>>(listOf(), listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(null, pagingState.closestItemToPosition(5))
        assertEquals(null, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestPageToPosition_withoutPlaceholders() {
        val pages = List(10) { listOf(it) }.asPages()
        val pagingState =
            PagingState(
                pages = pages,
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            )

        assertEquals(pages.first(), pagingState.closestPageToPosition(-1))
        assertEquals(pages[5], pagingState.closestPageToPosition(5))
        assertEquals(pages.last(), pagingState.closestPageToPosition(15))
    }

    @Test
    fun closestPageToPosition_withPlaceholders() {
        val pages = List(10) { listOf(it) }.asPages()
        val pagingState =
            PagingState(
                pages = pages,
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(pages.first(), pagingState.closestPageToPosition(5))
        assertEquals(pages[5], pagingState.closestPageToPosition(15))
        assertEquals(pages.last(), pagingState.closestPageToPosition(25))
    }

    @Test
    fun closestPageToPosition_withEmptyPages() {
        val pages =
            List(10) {
                    when {
                        it % 3 == 0 -> listOf()
                        else -> listOf(it)
                    }
                }
                .asPages()
        val pagingState =
            PagingState(
                pages = pages,
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(pages.first(), pagingState.closestPageToPosition(5))
        assertEquals(pages[5], pagingState.closestPageToPosition(13)) // pages[5].data == [5]
        assertEquals(pages.last(), pagingState.closestPageToPosition(25))
    }

    @Test
    fun closestPageToPosition_onlyEmptyPages() {
        val pagingState =
            PagingState(
                pages = listOf<List<Int>>(listOf(), listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(null, pagingState.closestPageToPosition(5))
        assertEquals(null, pagingState.closestPageToPosition(25))
    }

    @Test
    fun closestItemAroundPosition_withoutPlaceholders() {
        val pagingState =
            PagingState(
                pages = listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            )

        // test negative out of bound anchorPosition
        assertEquals(0, pagingState.closestItemAroundPosition(-1) { true })
        assertEquals(2, pagingState.closestItemAroundPosition(-1) { it == 2 })
        assertEquals(5, pagingState.closestItemAroundPosition(-1) { it == 5 })
        assertEquals(7, pagingState.closestItemAroundPosition(-1) { it == 7 })
        assertEquals(null, pagingState.closestItemAroundPosition(-1) { false })

        // test centered anchorPosition
        assertEquals(4, pagingState.closestItemAroundPosition(5) { it != 5 })
        assertEquals(5, pagingState.closestItemAroundPosition(5) { it == 5 })
        assertEquals(6, pagingState.closestItemAroundPosition(5) { it > 5 })
        assertEquals(null, pagingState.closestItemAroundPosition(5) { false })

        // test left-skewed anchorPosition
        assertEquals(1, pagingState.closestItemAroundPosition(2) { it < 2 })
        assertEquals(8, pagingState.closestItemAroundPosition(2) { it == 8 })
        assertEquals(null, pagingState.closestItemAroundPosition(2) { false })

        // test right-skewed anchorPosition
        assertEquals(8, pagingState.closestItemAroundPosition(7) { it > 7 })
        assertEquals(2, pagingState.closestItemAroundPosition(7) { it == 2 })
        assertEquals(null, pagingState.closestItemAroundPosition(7) { false })

        // test positive out of bound anchorPosition
        assertEquals(9, pagingState.closestItemAroundPosition(15) { true })
        assertEquals(7, pagingState.closestItemAroundPosition(15) { it == 7 })
        assertEquals(4, pagingState.closestItemAroundPosition(15) { it == 4 })
        assertEquals(1, pagingState.closestItemAroundPosition(15) { it == 1 })
        assertEquals(null, pagingState.closestItemAroundPosition(15) { false })
    }

    @Test
    fun closestItemAroundPosition_withPlaceholders() {
        val pagingState =
            PagingState(
                pages = listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        // test negative out of bound anchorPosition
        assertEquals(0, pagingState.closestItemAroundPosition(-1) { true })
        assertEquals(2, pagingState.closestItemAroundPosition(-1) { it == 2 })
        assertEquals(5, pagingState.closestItemAroundPosition(-1) { it == 5 })
        assertEquals(7, pagingState.closestItemAroundPosition(-1) { it == 7 })
        assertEquals(null, pagingState.closestItemAroundPosition(-1) { false })

        // test centered anchorPosition
        assertEquals(4, pagingState.closestItemAroundPosition(15) { it != 5 })
        assertEquals(5, pagingState.closestItemAroundPosition(15) { it == 5 })
        assertEquals(6, pagingState.closestItemAroundPosition(15) { it > 5 })
        assertEquals(null, pagingState.closestItemAroundPosition(15) { false })

        // test left-skewed anchorPosition
        assertEquals(1, pagingState.closestItemAroundPosition(12) { it < 2 })
        assertEquals(8, pagingState.closestItemAroundPosition(12) { it == 8 })
        assertEquals(null, pagingState.closestItemAroundPosition(12) { false })

        // test right-skewed anchorPosition
        assertEquals(8, pagingState.closestItemAroundPosition(17) { it > 7 })
        assertEquals(2, pagingState.closestItemAroundPosition(17) { it == 2 })
        assertEquals(null, pagingState.closestItemAroundPosition(17) { false })

        // test positive out of bound anchorPosition
        assertEquals(9, pagingState.closestItemAroundPosition(25) { true })
        assertEquals(7, pagingState.closestItemAroundPosition(25) { it == 7 })
        assertEquals(4, pagingState.closestItemAroundPosition(25) { it == 4 })
        assertEquals(1, pagingState.closestItemAroundPosition(25) { it == 1 })
        assertEquals(null, pagingState.closestItemAroundPosition(25) { false })
    }

    @Test
    fun closestItemAroundPosition_withEmptyPages() {
        val pagingState =
            PagingState(
                pages =
                    listOf(listOf(0, 1, 2, 3), listOf(), listOf(4, 5, 6), listOf(7, 8, 9))
                        .asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        // test negative out of bound anchorPosition
        assertEquals(0, pagingState.closestItemAroundPosition(-1) { true })
        assertEquals(2, pagingState.closestItemAroundPosition(-1) { it == 2 })
        assertEquals(5, pagingState.closestItemAroundPosition(-1) { it == 5 })
        assertEquals(7, pagingState.closestItemAroundPosition(-1) { it == 7 })
        assertEquals(null, pagingState.closestItemAroundPosition(-1) { false })

        // test centered anchorPosition
        assertEquals(4, pagingState.closestItemAroundPosition(15) { it != 5 })
        assertEquals(5, pagingState.closestItemAroundPosition(15) { it == 5 })
        assertEquals(6, pagingState.closestItemAroundPosition(15) { it > 5 })
        assertEquals(null, pagingState.closestItemAroundPosition(15) { false })

        // test left-skewed anchorPosition
        assertEquals(1, pagingState.closestItemAroundPosition(12) { it < 2 })
        assertEquals(8, pagingState.closestItemAroundPosition(12) { it == 8 })
        assertEquals(null, pagingState.closestItemAroundPosition(12) { false })

        // test right-skewed anchorPosition
        assertEquals(8, pagingState.closestItemAroundPosition(17) { it > 7 })
        assertEquals(2, pagingState.closestItemAroundPosition(17) { it == 2 })
        assertEquals(null, pagingState.closestItemAroundPosition(17) { false })

        // test positive out of bound anchorPosition
        assertEquals(9, pagingState.closestItemAroundPosition(25) { true })
        assertEquals(7, pagingState.closestItemAroundPosition(25) { it == 7 })
        assertEquals(4, pagingState.closestItemAroundPosition(25) { it == 4 })
        assertEquals(1, pagingState.closestItemAroundPosition(25) { it == 1 })
        assertEquals(null, pagingState.closestItemAroundPosition(25) { false })
    }

    @Test
    fun closestItemAroundPosition_onlyEmptyPages() {
        val pagingState =
            PagingState(
                pages = listOf<List<Int>>(listOf(), listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(null, pagingState.closestItemAroundPosition(5) { true })
        assertEquals(null, pagingState.closestItemAroundPosition(15) { it == 5 })
        assertEquals(null, pagingState.closestItemAroundPosition(25) { true })
    }

    @Test
    fun closestItemAroundPosition_prioritizesPrependedItem() {
        val pagingState =
            PagingState(
                pages = listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            )

        // returns previous item if both previous & next items match predicate
        assertEquals(4, pagingState.closestItemAroundPosition(5) { it != 5 })
    }

    @Test
    fun closestItemAroundPosition_prioritizesClosestAppendedItem() {
        val pagingState =
            PagingState(
                pages = listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            )

        // returns matching appended item if it is closer than the matching prepended item
        assertEquals(6, pagingState.closestItemAroundPosition(5) { it != 5 && it != 4 })
    }

    @Test
    fun itemOrNull_noPages() {
        val pagingState =
            PagingState(
                pages = listOf<Page<Int, Int>>(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(null, pagingState.firstItemOrNull())
        assertEquals(null, pagingState.lastItemOrNull())
    }

    @Test
    fun itemOrNull_emptyPages() {
        val pagingState =
            PagingState(
                pages = List(10) { listOf<Int>() }.asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(null, pagingState.firstItemOrNull())
        assertEquals(null, pagingState.lastItemOrNull())
    }

    @Test
    fun itemOrNull_emptyPagesAtEnds() {
        val pagingState =
            PagingState(
                pages = (listOf<List<Int>>() + List(10) { listOf(it) } + listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertEquals(0, pagingState.firstItemOrNull())
        assertEquals(9, pagingState.lastItemOrNull())
    }

    @Test
    fun isEmpty_noPages() {
        val pagingState =
            PagingState(
                pages = listOf<Page<Int, Int>>(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertTrue { pagingState.isEmpty() }
    }

    @Test
    fun isEmpty_emptyPages() {
        val pagingState =
            PagingState(
                pages = List(10) { listOf<Int>() }.asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertTrue { pagingState.isEmpty() }
    }

    @Test
    fun isEmpty_emptyPagesAtEnds() {
        val pagingState =
            PagingState(
                pages = (listOf<List<Int>>() + List(10) { listOf(it) } + listOf()).asPages(),
                anchorPosition = 10,
                config = PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 10,
            )

        assertFalse { pagingState.isEmpty() }
    }
}

private fun <T : Any> List<List<T>>.asPages() = mapIndexed { index, page: List<T> ->
    Page(
        data = page,
        prevKey = if (index > 0) index - 1 else null,
        nextKey = if (index < 9) index else null,
    )
}
