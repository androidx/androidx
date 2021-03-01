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
 * Type of load a [PagingData] can trigger a [PagingSource] to perform.
 *
 * [LoadState] of any [LoadType] may be observed for UI purposes by registering a listener via
 * [androidx.paging.PagingDataAdapter.addLoadStateListener] or
 * [androidx.paging.AsyncPagingDataDiffer.addLoadStateListener].
 *
 * @see LoadState
 */
public enum class LoadType {
    /**
     * [PagingData] content being refreshed, which can be a result of [PagingSource]
     * invalidation, refresh that may contain content updates, or the initial load.
     */
    REFRESH,

    /**
     * Load at the start of a [PagingData].
     */
    PREPEND,

    /**
     * Load at the end of a [PagingData].
     */
    APPEND
}