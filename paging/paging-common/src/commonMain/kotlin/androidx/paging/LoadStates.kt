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

import androidx.annotation.RestrictTo
import androidx.paging.LoadState.NotLoading

/**
 * Collection of pagination [LoadState]s - refresh, prepend, and append.
 */
public data class LoadStates(
    /** [LoadState] corresponding to [LoadType.REFRESH] loads. */
    public val refresh: LoadState,
    /** [LoadState] corresponding to [LoadType.PREPEND] loads. */
    public val prepend: LoadState,
    /** [LoadState] corresponding to [LoadType.APPEND] loads. */
    public val append: LoadState
) {
    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inline fun forEach(op: (LoadType, LoadState) -> Unit) {
        op(LoadType.REFRESH, refresh)
        op(LoadType.PREPEND, prepend)
        op(LoadType.APPEND, append)
    }

    internal fun modifyState(loadType: LoadType, newState: LoadState): LoadStates {
        return when (loadType) {
            LoadType.APPEND -> copy(
                append = newState
            )
            LoadType.PREPEND -> copy(
                prepend = newState
            )
            LoadType.REFRESH -> copy(
                refresh = newState
            )
        }
    }

    internal fun get(loadType: LoadType) = when (loadType) {
        LoadType.REFRESH -> refresh
        LoadType.APPEND -> append
        LoadType.PREPEND -> prepend
    }

    internal companion object {
        val IDLE = LoadStates(
            refresh = NotLoading.Incomplete,
            prepend = NotLoading.Incomplete,
            append = NotLoading.Incomplete
        )
    }
}
