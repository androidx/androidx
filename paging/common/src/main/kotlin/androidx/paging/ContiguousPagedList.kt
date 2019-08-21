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
import androidx.paging.PageLoadType.END
import androidx.paging.PageLoadType.REFRESH
import androidx.paging.PageLoadType.START
import androidx.paging.PagedList.LoadState.Idle
import androidx.paging.PagedList.LoadState.Loading
import androidx.paging.PagedSource.KeyProvider
import androidx.paging.PagedSource.LoadResult.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class ContiguousPagedList<K : Any, V : Any>(
    pagedSource: PagedSource<K, V>,
    coroutineScope: CoroutineScope,
    notifyDispatcher: CoroutineDispatcher,
    backgroundDispatcher: CoroutineDispatcher,
    boundaryCallback: BoundaryCallback<V>?,
    config: Config,
    initialResult: PagedSource.LoadResult<K, V>,
    lastLoad: Int
) : PagedList<V>(
    coroutineScope,
    pagedSource,
    PagedStorage<V>(),
    notifyDispatcher,
    backgroundDispatcher,
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

    private var prependItemsRequested = 0

    private var appendItemsRequested = 0

    private var replacePagesWithNulls = false

    private val shouldTrim = (pagedSource.keyProvider is KeyProvider.Positional ||
            pagedSource.keyProvider is KeyProvider.ItemKey) &&
            config.maxSize != Config.MAX_SIZE_UNBOUNDED

    private val pager = Pager(
        coroutineScope,
        config,
        pagedSource,
        notifyDispatcher,
        backgroundDispatcher,
        this,
        initialResult,
        storage
    )

    override val isDetached
        get() = pager.isDetached

    override val lastKey
        get() = when (val keyProvider = pagedSource.keyProvider) {
            is KeyProvider.Positional -> {
                @Suppress("UNCHECKED_CAST")
                lastLoad as K
            }
            is KeyProvider.PageKey ->
                throw IllegalStateException("Cannot get key by item from KeyProvider.PageKey")
            is KeyProvider.ItemKey -> lastItem?.let { keyProvider.getKey(it) }
        }

    /**
     * Given a page result, apply or drop it, and return whether more loading is needed.
     */
    override fun onPageResult(
        type: PageLoadType,
        pageResult: PagedSource.LoadResult<*, V>
    ): Boolean {
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

        if (type == END) {
            if (skipNewPage && !trimFromFront) {
                // don't append this data, drop it
                appendItemsRequested = 0
            } else {
                storage.appendPage(page, this@ContiguousPagedList)
                appendItemsRequested -= page.size
                if (appendItemsRequested > 0 && page.isNotEmpty()) {
                    continueLoading = true
                }
            }
        } else if (type == START) {
            if (skipNewPage && trimFromFront) {
                // don't append this data, drop it
                prependItemsRequested = 0
            } else {
                storage.prependPage(page, this@ContiguousPagedList)
                prependItemsRequested -= page.size
                if (prependItemsRequested > 0 && page.isNotEmpty()) {
                    continueLoading = true
                }
            }
        } else {
            throw IllegalArgumentException("unexpected result type $type")
        }

        if (shouldTrim) {
            // Try and trim, but only if the side being trimmed isn't actually fetching.
            // For simplicity (both of impl here, and contract w/ PagedSource) we don't
            // allow fetches in same direction - this means reading the load state is safe.
            if (trimFromFront) {
                if (pager.loadStateManager.startState !is Loading) {
                    if (storage.trimFromFront(
                            replacePagesWithNulls,
                            config.maxSize,
                            requiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        // trimmed from front, ensure we can fetch in that dir
                        pager.loadStateManager.setState(START, Idle)
                    }
                }
            } else {
                if (pager.loadStateManager.endState !is Loading) {
                    if (storage.trimFromEnd(
                            replacePagesWithNulls,
                            config.maxSize,
                            requiredRemainder,
                            this@ContiguousPagedList
                        )
                    ) {
                        pager.loadStateManager.setState(END, Idle)
                    }
                }
            }
        }

        triggerBoundaryCallback(type, page)
        return continueLoading
    }

    override fun onStateChanged(type: PageLoadType, state: LoadState) =
        dispatchStateChange(type, state)

    private fun triggerBoundaryCallback(type: PageLoadType, page: List<V>) {
        if (boundaryCallback != null) {
            val deferEmpty = storage.size == 0
            val deferBegin = (!deferEmpty && type == START && page.isEmpty())
            val deferEnd = (!deferEmpty && type == END && page.isEmpty())
            deferBoundaryCallbacks(deferEmpty, deferBegin, deferEnd)
        }
    }

    override fun retry() {
        super.retry()
        pager.retry()

        pager.loadStateManager.refreshState.run {
            // If loading the next PagedList failed, signal the retry callback.
            if (this is LoadState.Error && retryable) {
                refreshRetryCallback?.run()
            }
        }
    }

    init {
        this.lastLoad = lastLoad
        if (config.enablePlaceholders) {
            // Placeholders enabled, pass raw data to storage init
            storage.init(
                if (initialResult.itemsBefore != COUNT_UNDEFINED) initialResult.itemsBefore else 0,
                initialResult.data,
                if (initialResult.itemsAfter != COUNT_UNDEFINED) initialResult.itemsAfter else 0,
                0,
                this
            )
        } else {
            // If placeholder are disabled, avoid passing leading/trailing nulls, since PagedSource
            // may have passed them anyway.
            storage.init(
                0,
                initialResult.data,
                0,
                if (initialResult.itemsBefore != COUNT_UNDEFINED) initialResult.itemsBefore else 0,
                this
            )
        }

        if (this.lastLoad == LAST_LOAD_UNSPECIFIED) {
            // Because the ContiguousPagedList wasn't initialized with a last load position,
            // initialize it to the middle of the initial load
            val itemsBefore =
                if (initialResult.itemsBefore != COUNT_UNDEFINED) initialResult.itemsBefore else 0
            this.lastLoad = itemsBefore + initialResult.data.size / 2
        }
        triggerBoundaryCallback(REFRESH, initialResult.data)
    }

    override fun dispatchCurrentLoadState(callback: LoadStateListener) {
        pager.loadStateManager.dispatchCurrentLoadState(callback)
    }

    override fun setInitialLoadState(loadType: PageLoadType, loadState: LoadState) {
        pager.loadStateManager.setState(loadType, loadState)
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
        // Simple heuristic to decide if, when dropping pages, we should replace with placeholders.
        // If we're not presenting placeholders at initialization time, we won't add them when
        // we drop a page. Note that we don't use config.enablePlaceholders, since the
        // PagedSource may have opted not to load any.
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
