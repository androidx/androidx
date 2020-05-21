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

/**
 * Converts a list of incremental LoadState updates to local source to a list of expected
 * [CombinedLoadStates] events.
 */
@OptIn(ExperimentalStdlibApi::class)
fun List<Pair<LoadType, LoadState>>.toCombinedLoadStatesLocal() = scan(
    CombinedLoadStates(
        source = LoadStates(
            refresh = NotLoading(endOfPaginationReached = false),
            prepend = NotLoading(endOfPaginationReached = false),
            append = NotLoading(endOfPaginationReached = false)
        )
    )
) { prev, update ->
    prev.set(update.first, false, update.second)
}

/**
 * Test-only local-only LoadStates builder which defaults each state to [NotLoading], with
 * [LoadState.endOfPaginationReached] = `false`
 */
fun localLoadStatesOf(
    refreshLocal: LoadState = NotLoading(endOfPaginationReached = false),
    prependLocal: LoadState = NotLoading(endOfPaginationReached = false),
    appendLocal: LoadState = NotLoading(endOfPaginationReached = false)
) = CombinedLoadStates(
    source = LoadStates(
        refresh = refreshLocal,
        prepend = prependLocal,
        append = appendLocal
    )
)

/**
 * Test-only remote LoadStates builder which defaults each state to [NotLoading], with
 * [LoadState.endOfPaginationReached] = `false`
 */
fun remoteLoadStatesOf(
    refreshLocal: LoadState = NotLoading(endOfPaginationReached = false),
    prependLocal: LoadState = NotLoading(endOfPaginationReached = false),
    appendLocal: LoadState = NotLoading(endOfPaginationReached = false),
    refreshRemote: LoadState = NotLoading(endOfPaginationReached = false),
    prependRemote: LoadState = NotLoading(endOfPaginationReached = false),
    appendRemote: LoadState = NotLoading(endOfPaginationReached = false)
) = CombinedLoadStates(
    source = LoadStates(
        refresh = refreshLocal,
        prepend = prependLocal,
        append = appendLocal
    ),
    mediator = LoadStates(
        refresh = refreshRemote,
        prepend = prependRemote,
        append = appendRemote
    )
)

private fun CombinedLoadStates.set(
    loadType: LoadType,
    fromMediator: Boolean,
    loadState: LoadState
) = when (loadType) {
    LoadType.REFRESH -> if (fromMediator) {
        copy(
            mediator = mediator?.copy(refresh = loadState)
                ?: LoadStates(
                    refresh = loadState,
                    prepend = NotLoading(false),
                    append = NotLoading(false)
                )
        )
    } else {
        copy(
            source = source.copy(refresh = loadState)
        )
    }
    LoadType.PREPEND -> if (fromMediator) {
        copy(
            mediator = mediator?.copy(prepend = loadState)
                ?: LoadStates(
                    refresh = NotLoading(false),
                    prepend = loadState,
                    append = NotLoading(false)
                )
        )
    } else {
        copy(
            source = source.copy(prepend = loadState)
        )
    }
    LoadType.APPEND -> if (fromMediator) {
        copy(
            mediator = mediator?.copy(append = loadState)
                ?: LoadStates(
                    refresh = NotLoading(false),
                    prepend = NotLoading(false),
                    append = loadState
                )
        )
    } else {
        copy(
            source = source.copy(append = loadState)
        )
    }
}
