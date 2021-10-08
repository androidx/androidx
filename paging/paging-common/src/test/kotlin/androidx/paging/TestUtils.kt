/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.paging.PageEvent.LoadStateUpdate
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND

private fun <T : Any> localInsert(
    loadType: LoadType,
    pages: List<TransformablePage<T>>,
    placeholdersBefore: Int,
    placeholdersAfter: Int,
    source: LoadStates
) = when (loadType) {
    REFRESH -> PageEvent.Insert.Refresh(
        pages = pages,
        placeholdersBefore = placeholdersBefore,
        placeholdersAfter = placeholdersAfter,
        sourceLoadStates = source,
        mediatorLoadStates = null
    )
    PREPEND -> PageEvent.Insert.Prepend(
        pages = pages,
        placeholdersBefore = placeholdersBefore,
        sourceLoadStates = source,
        mediatorLoadStates = null,
    )
    APPEND -> PageEvent.Insert.Append(
        pages = pages,
        placeholdersAfter = placeholdersAfter,
        sourceLoadStates = source,
        mediatorLoadStates = null
    )
}

internal fun <T : Any> localRefresh(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersBefore: Int = 0,
    placeholdersAfter: Int = 0,
    source: LoadStates = loadStates()
) = localInsert(
    loadType = REFRESH,
    pages = pages,
    placeholdersBefore = placeholdersBefore,
    placeholdersAfter = placeholdersAfter,
    source = source
)

internal fun <T : Any> localPrepend(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersBefore: Int = 0,
    source: LoadStates = loadStates()
) = localInsert(
    loadType = PREPEND,
    pages = pages,
    placeholdersBefore = placeholdersBefore,
    placeholdersAfter = -1,
    source = source
)

internal fun <T : Any> localAppend(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersAfter: Int = 0,
    source: LoadStates = loadStates()
) = localInsert(
    loadType = APPEND,
    pages = pages,
    placeholdersBefore = -1,
    placeholdersAfter = placeholdersAfter,
    source = source
)

private fun <T : Any> remoteInsert(
    loadType: LoadType,
    pages: List<TransformablePage<T>>,
    placeholdersBefore: Int,
    placeholdersAfter: Int,
    source: LoadStates,
    mediator: LoadStates,
) = when (loadType) {
    REFRESH -> PageEvent.Insert.Refresh(
        pages = pages,
        placeholdersBefore = placeholdersBefore,
        placeholdersAfter = placeholdersAfter,
        sourceLoadStates = source,
        mediatorLoadStates = mediator,
    )
    PREPEND -> PageEvent.Insert.Prepend(
        pages = pages,
        placeholdersBefore = placeholdersBefore,
        sourceLoadStates = source,
        mediatorLoadStates = mediator,
    )
    APPEND -> PageEvent.Insert.Append(
        pages = pages,
        placeholdersAfter = placeholdersAfter,
        sourceLoadStates = source,
        mediatorLoadStates = mediator
    )
}

internal fun <T : Any> remoteRefresh(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersBefore: Int = 0,
    placeholdersAfter: Int = 0,
    source: LoadStates = loadStates(),
    mediator: LoadStates = loadStates(),
) = remoteInsert(
    loadType = REFRESH,
    pages = pages,
    placeholdersBefore = placeholdersBefore,
    placeholdersAfter = placeholdersAfter,
    source = source,
    mediator = mediator,
)

internal fun <T : Any> remotePrepend(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersBefore: Int = 0,
    source: LoadStates = loadStates(),
    mediator: LoadStates = loadStates(),
) = remoteInsert(
    loadType = PREPEND,
    pages = pages,
    placeholdersBefore = placeholdersBefore,
    placeholdersAfter = -1,
    source = source,
    mediator = mediator,
)

internal fun <T : Any> remoteAppend(
    pages: List<TransformablePage<T>> = listOf(emptyPage()),
    placeholdersAfter: Int = 0,
    source: LoadStates = loadStates(),
    mediator: LoadStates = loadStates(),
) = remoteInsert(
    loadType = APPEND,
    pages = pages,
    placeholdersBefore = -1,
    placeholdersAfter = placeholdersAfter,
    source = source,
    mediator = mediator,
)

internal fun <T : Any> localLoadStateUpdate(
    refreshLocal: LoadState = LoadState.NotLoading.Incomplete,
    prependLocal: LoadState = LoadState.NotLoading.Incomplete,
    appendLocal: LoadState = LoadState.NotLoading.Incomplete,
) = LoadStateUpdate<T>(
    source = loadStates(refreshLocal, prependLocal, appendLocal),
    mediator = null
)

internal fun <T : Any> localLoadStateUpdate(
    source: LoadStates = LoadStates.IDLE
) = LoadStateUpdate<T>(source, null)

internal fun <T : Any> remoteLoadStateUpdate(
    source: LoadStates = LoadStates.IDLE,
    mediator: LoadStates = LoadStates.IDLE,
) = LoadStateUpdate<T>(source, mediator)

internal fun <T : Any> remoteLoadStateUpdate(
    refreshLocal: LoadState = LoadState.NotLoading.Incomplete,
    prependLocal: LoadState = LoadState.NotLoading.Incomplete,
    appendLocal: LoadState = LoadState.NotLoading.Incomplete,
    refreshRemote: LoadState = LoadState.NotLoading.Incomplete,
    prependRemote: LoadState = LoadState.NotLoading.Incomplete,
    appendRemote: LoadState = LoadState.NotLoading.Incomplete,
) = LoadStateUpdate<T>(
    source = loadStates(refreshLocal, prependLocal, appendLocal),
    mediator = loadStates(refreshRemote, prependRemote, appendRemote),
)

private fun <T : Any> emptyPage() = TransformablePage<T>(0, emptyList())
