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

import kotlinx.coroutines.flow.Flow

/**
 * Primary entry point into Paging; constructor for a reactive stream of [PagingData].
 *
 * Each [PagingData] represents a snapshot of the backing paginated data. Updates to the backing
 * dataset should be represented by a new instance of [PagingData].
 *
 * [PagingSource.invalidate] and calls to [AsyncPagingDataDiffer.refresh] or
 * [PagingDataAdapter.refresh] will notify [Pager] that the backing dataset has been updated and a
 * new [PagingData] / [PagingSource] pair will be generated to represent an updated snapshot.
 *
 * [PagingData] can be transformed to alter data as it loads, and presented in a `RecyclerView` via
 * `AsyncPagingDataDiffer` or `PagingDataAdapter`.
 *
 * LiveData support is available as an extension property provided by the
 * `androidx.paging:paging-runtime` artifact.
 *
 * RxJava support is available as extension properties provided by the
 * `androidx.paging:paging-rxjava2` artifact.
 */
public class Pager<Key : Any, Value : Any>
// Experimental usage is propagated to public API via constructor argument.
@ExperimentalPagingApi constructor(
    config: PagingConfig,
    initialKey: Key? = null,
    remoteMediator: RemoteMediator<Key, Value>?,
    pagingSourceFactory: () -> PagingSource<Key, Value>
) {
    // Experimental usage is internal, so opt-in is allowed here.
    @JvmOverloads
    @OptIn(ExperimentalPagingApi::class)
    public constructor(
        config: PagingConfig,
        initialKey: Key? = null,
        pagingSourceFactory: () -> PagingSource<Key, Value>
    ) : this(config, initialKey, null, pagingSourceFactory)

    /**
     * A cold [Flow] of [PagingData], which emits new instances of [PagingData] once they become
     * invalidated by [PagingSource.invalidate] or calls to [AsyncPagingDataDiffer.refresh] or
     * [PagingDataAdapter.refresh].
     */
    public val flow: Flow<PagingData<Value>> = PageFetcher(
        pagingSourceFactory = if (
            pagingSourceFactory is SuspendingPagingSourceFactory<Key, Value>
        ) {
            pagingSourceFactory::create
        } else {
            // cannot pass it as is since it is not a suspend function. Hence, we wrap it in {}
            // which means we are calling the original factory inside a suspend function
            {
                pagingSourceFactory()
            }
        },
        initialKey = initialKey,
        config = config,
        remoteMediator = remoteMediator
    ).flow
}
