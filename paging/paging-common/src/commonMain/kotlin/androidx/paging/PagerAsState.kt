/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("PagerAsState")

package androidx.paging

import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull

/**
 * Converts a Flow of [PagingData] into a Flow of [ItemSnapshotList]. An emitted [ItemSnapshotList]
 * contains a snapshot of all loaded data and can be cached for repeated reads.
 *
 * To reflect the latest snapshot, this flow emits a new [ItemSnapshotList] whenever new items are
 * loaded or whenever loaded items are dropped (i.e. to fulfill [PagingConfig.maxSize]).
 *
 * The flow is kept active as long as the collection scope is active. To avoid leaks, make sure to
 * use a scope that is already managed (like a ViewModel scope) or manually cancel it when you don't
 * need paging anymore.
 *
 * To use this flow with multiple collectors, convert it to a SharedFlow with Flow operators such as
 * stateIn() or sharedIn().
 *
 * Note that this Flow remains as a cold flow and does not start any loading until collected upon.
 *
 * [T] - the paged item type
 *
 * @param [onLoadError] the callback invoked when any loads return
 *   [androidx.paging.LoadState.Error]. Provides the [CombinedLoadStates] containing the error.
 *   No-op by default. See [Pager.retry] for a recovery option.
 */
public fun <T : Any> Flow<PagingData<T>>.asState(
    onLoadError: (CombinedLoadStates) -> Unit = {}
): Flow<ItemSnapshotList<T>> {
    var errorCombinedLoadStates: CombinedLoadStates? = null

    return channelFlow {
        this@asState.collectLatest { pagingData ->
            pagingData.flow
                .filterNot { it is PageEvent.StaticList }
                .simpleScan(null) { pageStore: PageStore<T>?, pageEvent ->
                    var currPageStore = pageStore
                    when {
                        pageEvent is PageEvent.LoadStateUpdate -> {
                            if (pageEvent.source.hasError || pageEvent.mediator?.hasError == true) {
                                errorCombinedLoadStates =
                                    errorCombinedLoadStates.computeNewState(
                                        pageEvent.source,
                                        pageEvent.mediator,
                                    )
                                onLoadError(errorCombinedLoadStates)
                            }
                        }
                        pageEvent is PageEvent.Insert && pageEvent.loadType == LoadType.REFRESH -> {
                            require(pageStore == null) {
                                "PageStore should be null on REFRESH. This likely " +
                                    "indicates an error in the library. Please file a bug " +
                                    "in the Buganizer"
                            }
                            currPageStore =
                                PageStore(
                                    pages = pageEvent.pages,
                                    placeholdersBefore = pageEvent.placeholdersBefore,
                                    placeholdersAfter = pageEvent.placeholdersAfter,
                                )
                        }
                        else -> {
                            requireNotNull(pageStore) {
                                "PageStore should only be null on REFRESH. This likely " +
                                    "indicates an error in the library. Please file a bug " +
                                    "in the Buganizer"
                            }
                            currPageStore.processEvent(pageEvent)
                        }
                    }
                    currPageStore
                }
                .filterNotNull()
                .distinctUntilChangedBy {
                    // distinctUntilChanged() doesn't work here because at this point the old
                    // PageStore would have been mutated to the new PageStore after
                    // processing events (if any), so the old store always equals new store.
                    // Instead, we use distinctUntilChangedBy to take a snapshot of and cache
                    // the old hashCode for comparing with the updated PagStore's new hashcode.
                    it.hashCode()
                }
                .collect { pageStore ->
                    // send to outer channelFlow
                    send(pageStore.snapshot())
                }
        }
    }
}
