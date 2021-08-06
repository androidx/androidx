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
 * Test-only local-only LoadStates builder which defaults each state to [NotLoading], with
 * [LoadState.endOfPaginationReached] = `false`
 */
fun localLoadStatesOf(
    refreshLocal: LoadState = NotLoading(endOfPaginationReached = false),
    prependLocal: LoadState = NotLoading(endOfPaginationReached = false),
    appendLocal: LoadState = NotLoading(endOfPaginationReached = false)
) = CombinedLoadStates(
    refresh = refreshLocal,
    prepend = prependLocal,
    append = appendLocal,
    source = LoadStates(
        refresh = refreshLocal,
        prepend = prependLocal,
        append = appendLocal,
    ),
)

/**
 * Test-only remote LoadStates builder which defaults each state to [NotLoading], with
 * [LoadState.endOfPaginationReached] = `false`
 */
fun remoteLoadStatesOf(
    refresh: LoadState = NotLoading(endOfPaginationReached = false),
    prepend: LoadState = NotLoading(endOfPaginationReached = false),
    append: LoadState = NotLoading(endOfPaginationReached = false),
    refreshLocal: LoadState = NotLoading(endOfPaginationReached = false),
    prependLocal: LoadState = NotLoading(endOfPaginationReached = false),
    appendLocal: LoadState = NotLoading(endOfPaginationReached = false),
    refreshRemote: LoadState = NotLoading(endOfPaginationReached = false),
    prependRemote: LoadState = NotLoading(endOfPaginationReached = false),
    appendRemote: LoadState = NotLoading(endOfPaginationReached = false)
) = CombinedLoadStates(
    refresh = refresh,
    prepend = prepend,
    append = append,
    source = LoadStates(
        refresh = refreshLocal,
        prepend = prependLocal,
        append = appendLocal,
    ),
    mediator = LoadStates(
        refresh = refreshRemote,
        prepend = prependRemote,
        append = appendRemote,
    ),
)

/**
 * Test-only LoadStates builder which defaults each state to [NotLoading], with
 * [LoadState.endOfPaginationReached] = `false`
 */
fun loadStates(
    refresh: LoadState = NotLoading(endOfPaginationReached = false),
    prepend: LoadState = NotLoading(endOfPaginationReached = false),
    append: LoadState = NotLoading(endOfPaginationReached = false),
) = LoadStates(
    refresh = refresh,
    prepend = prepend,
    append = append,
)