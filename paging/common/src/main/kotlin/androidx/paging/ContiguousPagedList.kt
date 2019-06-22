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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executor

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class ContiguousPagedList<K : Any, V : Any>(
    override val dataSource: DataSource<K, V>,
    coroutineScope: CoroutineScope,
    mainThreadExecutor: Executor,
    backgroundThreadExecutor: Executor,
    boundaryCallback: BoundaryCallback<V>?,
    config: Config,
    initialResult: DataSource.BaseResult<V>,
    lastLoad: Int
) : PagedList<V>(
    PagedStorage<V>(),
    mainThreadExecutor,
    backgroundThreadExecutor,
    boundaryCallback,
    config
), PagedStorage.Callback, Pager.PageConsumer<V> {
    internal companion object {
        internal const val LAST_LOAD_UNSPECIFIED = -1

        internal fun getPrependItemsRequested(
            prefetchDistance: Int,
            index: Int,
            leadingNulls: Int
        ) = prefetchDistance - (index - leadingNulls)

        internal fun getAppendItemsRequested(
            prefetchDistance: Int,
            index: Int,
            itemsBeforeTrailingNulls: Int
        ) = index + prefetchDistance + 1 - itemsBeforeTrailingNulls
    }

    var prependItemsRequested = 0

    var appendItemsRequested = 0

    var replacePagesWithNulls = false

    private val shouldTrim: Boolean

    private val pager: Pager<K, V>

    override val isDetached
        get() = pager.isDetached

    override val isContiguous = true

    override val lastKey
        get() = when (dataSource.type) {
            DataSource.KeyType.POSITIONAL -> {
                @Suppress("UNCHECKED_CAST")
                lastLoad as K
            }
            else -> lastItem?.let { dataSource.getKeyInternal(it) }
        }

    /**
     * Given a page result, apply or drop it, and return whether more loading is needed.
     */
    override fun onPageResult(type: LoadType, pageResult: DataSource.BaseResult<V>): Boolean {
        var continueLoading = false
        val page = pageResult.data

        // if we end up trimming, we trim from side that's furthest from most recent access
        val trimFromFront = lastLoad > storage.middleOfLoadedRange

        // is the new page big enough to warrant pre-trimming (i.e. dropping) it?
        val skipNewPage = shouldTrim && storage.shouldPreTrimNewPage(
            config.maxSize,
            requiredRemainder,
            page.size
        )

        if (type == LoadType.END) {
            if (skipNewPage && !trimFromFront) {
                // don't append this data, drop it
                appendItemsRequested = 0
            } else {
                storage.appendPage(page, this@ContiguousPagedList)
                appendItemsRequested -= page.size
                if (appendItemsRequested > 0 && page.size != 0) {
                    continueLoading = true
                }
            }
        } else if (type == LoadType.START) {
            if (skipNewPage && trimFromFront) {
                // don't append this data, drop it
                prependItemsRequested = 0
            } else {
                storage.prependPage(page, this@ContiguousPagedList)
                prependItemsRequested -= page.size
                if (prependItemsRequested > 0 && page.size != 0) {
                    continueLoading = true
                }
            }
        } else {
            throw IllegalArgumentException("unexpected result type $type")
        }

        if (shouldTrim) {
            // Try and trim, but only if the side being trimmed isn't actually fetching.
            // For simplicity (both of impl here, and contract w/ DataSource) we don't
            // allow fetches in same direction - this means reading the load state is safe.
            if (trimFromFront) {
                if (pager.loadStateManager.start != LoadState.LOADING) {
                    if (storage.trimFromFront(
                            replacePagesWithNulls,
                            config.maxSize,
                            requiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        // trimmed from front, ensure we can fetch in that dir
                        pager.loadStateManager.setState(
                            LoadType.START,
                            LoadState.IDLE,
                            null
                        )
                    }
                }
            } else {
                if (pager.loadStateManager.end != LoadState.LOADING) {
                    if (storage.trimFromEnd(
                            replacePagesWithNulls,
                            config.maxSize,
                            requiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        pager.loadStateManager.setState(LoadType.END, LoadState.IDLE, null)
                    }
                }
            }
        }

        triggerBoundaryCallback(type, page)
        return continueLoading
    }

    override fun onStateChanged(type: LoadType, state: LoadState, error: Throwable?) =
        dispatchStateChange(type, state, error)

    private fun triggerBoundaryCallback(type: LoadType, page: List<V>) {
        if (boundaryCallback != null) {
            val deferEmpty = storage.size == 0
            val deferBegin = (!deferEmpty && type == LoadType.START && page.isEmpty())
            val deferEnd = (!deferEmpty && type == LoadType.END && page.isEmpty())
            deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd)
        }
    }

    override fun retry() {
        super.retry()
        pager.retry()

        if (pager.loadStateManager.refresh == LoadState.RETRYABLE_ERROR) {
            // Loading the next PagedList failed, signal the retry callback.
            refreshRetryCallback?.run()
        }
    }

    init {
        this.lastLoad = lastLoad
        pager = Pager(
            coroutineScope,
            config,
            PagedSourceWrapper(dataSource), // TODO: Fix non-final in constructor.
            mainThreadExecutor,
            backgroundThreadExecutor,
            this,
            storage,
            initialResult
        )

        if (config.enablePlaceholders) {
            // Placeholders enabled, pass raw data to storage init
            storage.init(
                initialResult.leadingNulls,
                initialResult.data,
                initialResult.trailingNulls,
                initialResult.offset,
                this
            )
        } else {
            // If placeholder are disabled, avoid passing leading/trailing nulls,
            // since DataSource may have passed them anyway
            storage.init(
                0,
                initialResult.data,
                0,
                initialResult.offset + initialResult.leadingNulls,
                this
            )
        }

        shouldTrim =
            dataSource.supportsPageDropping && config.maxSize != Config.MAX_SIZE_UNBOUNDED

        if (this.lastLoad == LAST_LOAD_UNSPECIFIED) {
            // Because the ContiguousPagedList wasn't initialized with a last load position,
            // initialize it to the middle of the initial load
            this.lastLoad = (initialResult.leadingNulls + initialResult.offset +
                    initialResult.data.size / 2)
        }
        triggerBoundaryCallback(LoadType.REFRESH, initialResult.data)
    }

    override fun dispatchCurrentLoadState(callback: LoadStateListener) =
        pager.loadStateManager.dispatchCurrentLoadState(callback)

    override fun setInitialLoadState(loadState: LoadState, error: Throwable?) =
        pager.loadStateManager.setState(LoadType.REFRESH, loadState, error)

    @MainThread
    override fun dispatchUpdatesSinceSnapshot(snapshot: PagedList<V>, callback: Callback) {
        val snapshotStorage = snapshot.storage

        val newlyAppended = storage.numberAppended - snapshotStorage.numberAppended
        val newlyPrepended = storage.numberPrepended - snapshotStorage.numberPrepended

        val previousTrailing = snapshotStorage.trailingNullCount
        val previousLeading = snapshotStorage.leadingNullCount

        // Validate that the snapshot looks like a previous version of this list - if it's not,
        // we can't be sure we'll dispatch callbacks safely
        if (snapshotStorage.isEmpty() ||
            newlyAppended < 0 ||
            newlyPrepended < 0 ||
            storage.trailingNullCount != maxOf(previousTrailing - newlyAppended, 0) ||
            storage.leadingNullCount != maxOf(previousLeading - newlyPrepended, 0) ||
            storage.storageCount != snapshotStorage.storageCount + newlyAppended + newlyPrepended
        ) {
            throw IllegalArgumentException(
                "Invalid snapshot provided - doesn't appear to be a snapshot of this PagedList"
            )
        }

        if (newlyAppended != 0) {
            val changedCount = minOf(previousTrailing, newlyAppended)
            val addedCount = newlyAppended - changedCount

            val endPosition = snapshotStorage.leadingNullCount + snapshotStorage.storageCount
            if (changedCount != 0) {
                callback.onChanged(endPosition, changedCount)
            }
            if (addedCount != 0) {
                callback.onInserted(endPosition + changedCount, addedCount)
            }
        }
        if (newlyPrepended != 0) {
            val changedCount = minOf(previousLeading, newlyPrepended)
            val addedCount = newlyPrepended - changedCount

            if (changedCount != 0) {
                callback.onChanged(previousLeading, changedCount)
            }
            if (addedCount != 0) {
                callback.onInserted(0, addedCount)
            }
        }
    }

    @MainThread
    override fun loadAroundInternal(index: Int) {
        val prependItems =
            getPrependItemsRequested(config.prefetchDistance, index, storage.leadingNullCount)
        val appendItems = getAppendItemsRequested(
            config.prefetchDistance,
            index,
            storage.leadingNullCount + storage.storageCount
        )

        prependItemsRequested = maxOf(prependItems, prependItemsRequested)
        if (prependItemsRequested > 0) {
            pager.trySchedulePrepend()
        }

        appendItemsRequested = maxOf(appendItems, appendItemsRequested)
        if (appendItemsRequested > 0) {
            pager.tryScheduleAppend()
        }
    }

    override fun detach() = pager.detach()

    @MainThread
    override fun onInitialized(count: Int) {
        notifyInserted(0, count)
        // simple heuristic to decide if, when dropping pages, we should replace with placeholders
        replacePagesWithNulls = storage.leadingNullCount > 0 || storage.trailingNullCount > 0
    }

    @MainThread
    override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {
        // finally dispatch callbacks, after prepend may have already been scheduled
        notifyChanged(leadingNulls, changed)
        notifyInserted(0, added)

        offsetAccessIndices(added)
    }

    @MainThread
    override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {
        // finally dispatch callbacks, after append may have already been scheduled
        notifyChanged(endPosition, changed)
        notifyInserted(endPosition + changed, added)
    }

    @MainThread
    override fun onPagePlaceholderInserted(pageIndex: Int) {
        throw IllegalStateException("Tiled callback on ContiguousPagedList")
    }

    @MainThread
    override fun onPageInserted(start: Int, count: Int) {
        throw IllegalStateException("Tiled callback on ContiguousPagedList")
    }

    override fun onPagesRemoved(startOfDrops: Int, count: Int) = notifyRemoved(startOfDrops, count)

    override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) =
        notifyChanged(startOfDrops, count)
}
