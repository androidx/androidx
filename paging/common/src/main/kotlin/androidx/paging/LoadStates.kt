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

/**
 * Collection of pagination [LoadState]s - refresh, prepend, and append.
 */
data class LoadStates(
    /** [LoadState] corresponding to [LoadType.REFRESH] loads. */
    val refresh: LoadState,
    /** [LoadState] corresponding to [LoadType.PREPEND] loads. */
    val prepend: LoadState,
    /** [LoadState] corresponding to [LoadType.APPEND] loads. */
    val append: LoadState
) {
    init {
        require(!refresh.endOfPaginationReached) {
            "Refresh state may not set endOfPaginationReached = true"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    inline fun forEach(op: (LoadType, LoadState) -> Unit) {
        op(LoadType.REFRESH, refresh)
        op(LoadType.PREPEND, prepend)
        op(LoadType.APPEND, append)
    }

    internal companion object {
        val IDLE_SOURCE = LoadStates(
            refresh = LoadState.NotLoading.Idle,
            prepend = LoadState.NotLoading.Idle,
            append = LoadState.NotLoading.Idle
        )
        val IDLE_MEDIATOR = LoadStates(
            refresh = LoadState.NotLoading.IdleRemote,
            prepend = LoadState.NotLoading.IdleRemote,
            append = LoadState.NotLoading.IdleRemote
        )
    }
}
