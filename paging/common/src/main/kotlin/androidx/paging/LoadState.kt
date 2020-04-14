/*
 * Copyright 2019 The Android Open Source Project
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

/**
 * LoadState of a PagedList load - associated with a [LoadType]
 *
 * [LoadState] of any [LoadType] may be observed for UI purposes by registering a listener via
 * [androidx.paging.PagingDataAdapter.addLoadStateListener] or
 * [androidx.paging.AsyncPagingDataDiffer.addLoadStateListener]
 *
 * @see LoadType
 */
sealed class LoadState(val endOfPaginationReached: Boolean) {
    /**
     * Indicates the [PagingData] is not currently loading, and no error currently observed.
     *
     * @param endOfPaginationReached `false` if there is more data to load in the [LoadType] this
     * [LoadState] is associated with, `true` otherwise. This parameter informs [Pager] if it
     * should continue to make requests for additional data in this direction or if it should
     * halt as the end of the dataset has been reached.
     */
    @Suppress("DataClassPrivateConstructor")
    class NotLoading constructor(
        endOfPaginationReached: Boolean
    ) : LoadState(endOfPaginationReached) {
        override fun toString(): String {
            return "NotLoading(endOfPaginationReached=$endOfPaginationReached)"
        }

        override fun equals(other: Any?): Boolean {
            if (other !is NotLoading) return false

            return endOfPaginationReached == other.endOfPaginationReached
        }

        override fun hashCode(): Int {
            return endOfPaginationReached.hashCode()
        }

        internal companion object {
            val Done = NotLoading(true)
            val Idle = NotLoading(false)
        }
    }

    /**
     * Loading is in progress.
     */
    object Loading : LoadState(false)

    /**
     * Loading hit an error.
     *
     * @param error [Throwable] that caused the load operation to generate this error state.
     *
     * @see androidx.paging.PagedList.retry
     */
    data class Error(val error: Throwable) : LoadState(false)
}
