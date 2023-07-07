/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.paging.testing

import androidx.annotation.VisibleForTesting
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingDataDiffer
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult

/**
 * An interface to implement the error recovery strategy when [PagingSource]
 * returns a [LoadResult.Error].
 */
@VisibleForTesting
public fun interface LoadErrorHandler {
    /**
     * The lambda that should return an [ErrorRecovery] given the [CombinedLoadStates]
     * indicating which [LoadState] contains the [LoadState.Error].
     *
     * Sample use case:
     * val onError = LoadErrorHandler { combinedLoadStates ->
     *     if (combinedLoadStates.refresh is LoadResult.Error) {
     *         ErrorRecovery.RETRY
     *     } else {
     *         ErrorRecovery.THROW
     *     }
     * }
     */
    public fun onError(combinedLoadStates: CombinedLoadStates): ErrorRecovery
}

/**
 * The method of recovery when [PagingSource] returns [LoadResult.Error]. The error
 * is indicated when [PagingDataDiffer.loadStateFlow] emits a [CombinedLoadStates] where one or
 * more of the [LoadState] is [LoadState.Error].
 */
@VisibleForTesting
public enum class ErrorRecovery {
    /**
     * Rethrow the original [Throwable][LoadState.Error.error] that was caught when loading from
     * the data source.
     */
    THROW,

    /**
     * Retry the failed load. This does not guarantee a successful load as the data source
     * may still return an error.
     */
    RETRY,

    /**
     * Returns a snapshot with any data that has been loaded up till the point of error.
     */
    RETURN_CURRENT_SNAPSHOT,
}
