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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class PagingStateTest {
    @Test
    fun closestItemToPosition_withoutPlaceholders() {
        val pagingState = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = List(10) { it },
                    prevKey = null,
                    nextKey = null
                )
            ),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10, prefetchDistance = 0),
            placeholdersBefore = 0
        )

        assertEquals(0, pagingState.closestItemToPosition(-1))
        assertEquals(5, pagingState.closestItemToPosition(5))
        assertEquals(9, pagingState.closestItemToPosition(15))
    }

    @Test
    fun closestItemToPosition_withPlaceholders() {
        val pagingState = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = List(10) { it },
                    prevKey = null,
                    nextKey = null
                )
            ),
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10, prefetchDistance = 0),
            placeholdersBefore = 10
        )

        assertEquals(0, pagingState.closestItemToPosition(5))
        assertEquals(5, pagingState.closestItemToPosition(15))
        assertEquals(9, pagingState.closestItemToPosition(25))
    }

    @Test
    fun closestPageToPosition_withoutPlaceholders() {
        val pages = List(10) {
            PagingSource.LoadResult.Page(
                data = listOf(it),
                prevKey = null,
                nextKey = if (it < 9) it else null
            )
        }
        val pagingState = PagingState(
            pages = pages,
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10, prefetchDistance = 0),
            placeholdersBefore = 0
        )

        assertEquals(pages.first(), pagingState.closestPageToPosition(-1))
        assertEquals(pages[5], pagingState.closestPageToPosition(5))
        assertEquals(pages.last(), pagingState.closestPageToPosition(15))
    }

    @Test
    fun closestPageToPosition_withPlaceholders() {
        val pages = List(10) {
            PagingSource.LoadResult.Page(
                data = listOf(it),
                prevKey = null,
                nextKey = if (it < 9) it else null
            )
        }
        val pagingState = PagingState(
            pages = pages,
            anchorPosition = 10,
            config = PagingConfig(pageSize = 10, prefetchDistance = 0),
            placeholdersBefore = 10
        )

        assertEquals(pages.first(), pagingState.closestPageToPosition(5))
        assertEquals(pages[5], pagingState.closestPageToPosition(15))
        assertEquals(pages.last(), pagingState.closestPageToPosition(25))
    }
}
