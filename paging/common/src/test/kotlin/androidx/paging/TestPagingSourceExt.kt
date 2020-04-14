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

import androidx.paging.TestPagingSource.Companion.items

internal fun createRefresh(
    range: IntRange,
    startState: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
    endState: LoadState = LoadState.NotLoading(endOfPaginationReached = false)
) = PageEvent.Insert.Refresh(
    pages = pages(0, range),
    placeholdersStart = range.first.coerceAtLeast(0),
    placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0),
    loadStates = mapOf(
        LoadType.REFRESH to LoadState.NotLoading(endOfPaginationReached = false),
        LoadType.START to startState,
        LoadType.END to endState
    )
)

internal fun createPrepend(
    pageOffset: Int,
    range: IntRange,
    startState: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
    endState: LoadState = LoadState.NotLoading(endOfPaginationReached = false)
) = PageEvent.Insert.Start(
    pages = pages(pageOffset, range),
    placeholdersStart = range.first.coerceAtLeast(0),
    loadStates = mapOf(
        LoadType.REFRESH to LoadState.NotLoading(endOfPaginationReached = false),
        LoadType.START to startState,
        LoadType.END to endState
    )
)

internal fun createAppend(
    pageOffset: Int,
    range: IntRange,
    startState: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
    endState: LoadState = LoadState.NotLoading(endOfPaginationReached = false)
) = PageEvent.Insert.End(
    pages = pages(pageOffset, range),
    placeholdersEnd = (items.size - range.last - 1).coerceAtLeast(0),
    loadStates = mapOf(
        LoadType.REFRESH to LoadState.NotLoading(endOfPaginationReached = false),
        LoadType.START to startState,
        LoadType.END to endState
    )
)

private fun pages(
    pageOffset: Int,
    range: IntRange
) = listOf(
    TransformablePage(
        originalPageOffset = pageOffset,
        data = items.slice(range),
        originalPageSize = range.count(),
        originalIndices = null
    )
)
