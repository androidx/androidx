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
 * Collection of pagination [LoadState]s for both a [PagingSource], and [RemoteMediator].
 */
data class CombinedLoadStates(
    /**
     * [LoadStates] corresponding to loads from a [PagingSource].
     */
    val source: LoadStates,

    /**
     * [LoadStates] corresponding to loads from a [RemoteMediator], or `null` if [RemoteMediator]
     * not present.
     */
    val mediator: LoadStates? = null
) {
    /**
     * Convenience for accessing [REFRESH][LoadType.REFRESH] [LoadState], which always defers to
     * [LoadState] of [mediator] if it exists, otherwise equivalent to [LoadState] of [source].
     *
     * For use cases that require reacting to [LoadState] of [source] and [mediator]
     * specifically, e.g., showing cached data when network loads via [mediator] fail,
     * [LoadStates] exposed via [source] and [mediator] should be used directly.
     */
    val refresh: LoadState = (mediator ?: source).refresh

    /**
     * Convenience for accessing [PREPEND][LoadType.PREPEND] [LoadState], which always defers to
     * [LoadState] of [mediator] if it exists, otherwise equivalent to [LoadState] of [source].
     *
     * For use cases that require reacting to [LoadState] of [source] and [mediator]
     * specifically, e.g., showing cached data when network loads via [mediator] fail,
     * [LoadStates] exposed via [source] and [mediator] should be used directly.
     */
    val prepend: LoadState = (mediator ?: source).prepend

    /**
     * Convenience for accessing [APPEND][LoadType.APPEND] [LoadState], which always defers to
     * [LoadState] of [mediator] if it exists, otherwise equivalent to [LoadState] of [source].
     *
     * For use cases that require reacting to [LoadState] of [source] and [mediator]
     * specifically, e.g., showing cached data when network loads via [mediator] fail,
     * [LoadStates] exposed via [source] and [mediator] should be used directly.
     */
    val append: LoadState = (mediator ?: source).append

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    inline fun forEach(op: (LoadType, Boolean, LoadState) -> Unit) {
        source.forEach { type, state ->
            op(type, false, state)
        }
        mediator?.forEach { type, state ->
            op(type, true, state)
        }
    }

    internal companion object {
        val IDLE_SOURCE = CombinedLoadStates(
            source = LoadStates.IDLE
        )
        val IDLE_MEDIATOR = CombinedLoadStates(
            source = LoadStates.IDLE,
            mediator = LoadStates.IDLE
        )
    }
}
