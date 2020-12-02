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

import androidx.paging.LoadState.NotLoading
import androidx.paging.TestPagingSource.Companion.ITEMS

internal fun createRefresh(
    range: IntRange,
    combinedLoadStates: CombinedLoadStates
) = PageEvent.Insert.Refresh(
    pages = pages(0, range),
    placeholdersBefore = range.first.coerceAtLeast(0),
    placeholdersAfter = (ITEMS.size - range.last - 1).coerceAtLeast(0),
    combinedLoadStates = combinedLoadStates
)

internal fun createRefresh(
    range: IntRange,
    startState: LoadState = NotLoading.Incomplete,
    endState: LoadState = NotLoading.Incomplete
) = PageEvent.Insert.Refresh(
    pages = pages(0, range),
    placeholdersBefore = range.first.coerceAtLeast(0),
    placeholdersAfter = (ITEMS.size - range.last - 1).coerceAtLeast(0),
    combinedLoadStates = localLoadStatesOf(prependLocal = startState, appendLocal = endState)
)

internal fun createPrepend(
    pageOffset: Int,
    range: IntRange,
    startState: LoadState = NotLoading.Incomplete,
    endState: LoadState = NotLoading.Incomplete
) = PageEvent.Insert.Prepend(
    pages = pages(pageOffset, range),
    placeholdersBefore = range.first.coerceAtLeast(0),
    combinedLoadStates = localLoadStatesOf(prependLocal = startState, appendLocal = endState)
)

internal fun createAppend(
    pageOffset: Int,
    range: IntRange,
    startState: LoadState = NotLoading.Incomplete,
    endState: LoadState = NotLoading.Incomplete
) = PageEvent.Insert.Append(
    pages = pages(pageOffset, range),
    placeholdersAfter = (ITEMS.size - range.last - 1).coerceAtLeast(0),
    combinedLoadStates = localLoadStatesOf(prependLocal = startState, appendLocal = endState)
)

private fun pages(
    pageOffset: Int,
    range: IntRange
) = listOf(
    TransformablePage(
        originalPageOffset = pageOffset,
        data = ITEMS.slice(range)
    )
)
