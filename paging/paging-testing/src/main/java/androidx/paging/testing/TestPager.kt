/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.paging.LoadType
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.Pager
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A fake [Pager] class to simulate how a real Pager and UI would load data from a PagingSource.
 *
 * As Paging's first load is always of type [LoadType.REFRESH], the first load operation of
 * the [TestPager] must be a call to [refresh].
 *
 * This class only supports loads from a single instance of PagingSource. To simulate
 * multi-generational Paging behavior, you must create a new [TestPager] by supplying a
 * new instance of [PagingSource].
 *
 * @param pagingSource the [PagingSource] to load data from.
 * @param config the [PagingConfig] to configure this TestPager's loading behavior.
 */
public class TestPager<Key : Any, Value : Any>(
    private val pagingSource: PagingSource<Key, Value>,
    private val config: PagingConfig,
) {
    private val hasRefreshed = AtomicBoolean(false)

    private val lock = Mutex()

    private val pages = ArrayDeque<LoadResult.Page<Key, Value>>()

    /**
     * Performs a load of [LoadType.REFRESH] on the PagingSource.
     *
     * If initialKey != null, refresh will start loading from the supplied key.
     *
     * Since Paging's first load is always of [LoadType.REFRESH], this method must be the very
     * first load operation to be called on the TestPager before either [append] or [prepend]
     * can be called. However, other non-loading operations can still be invoked. For example,
     * you can call [getLastLoadedPage] before any load operations.
     *
     * Returns the LoadResult upon refresh on the [PagingSource].
     *
     * @param initialKey the [Key] to start loading data from on initial refresh.
     *
     * @throws IllegalStateException TestPager does not support multi-generational paging behavior.
     * As such, multiple calls to refresh() on this TestPager is illegal. The [PagingSource] passed
     * in to this [TestPager] will also be invalidated to prevent reuse of this pager for loads.
     * However, other [TestPager] methods that does not invoke loads can still be called,
     * such as [getLastLoadedPage].
     */
    public suspend fun refresh(initialKey: Key? = null): LoadResult<Key, Value> {
        if (!hasRefreshed.compareAndSet(false, true)) {
            pagingSource.invalidate()
            throw IllegalStateException("TestPager does not support multi-generational access " +
                "and refresh() can only be called once per TestPager. To start a new generation," +
                "create a new TestPager with a new PagingSource.")
        }
        return doInitialLoad(initialKey)
    }

    /**
     * Performs a load of [LoadType.APPEND] on the PagingSource.
     *
     * Since Paging's first load is always of [LoadType.REFRESH], [refresh] must always be called
     * first before this [append] is called.
     *
     * Returns the [LoadResult] from calling [PagingSource.load]. If the [LoadParams.key] is null,
     * such as when there is no more data to append, this append will be no-op by returning null.
     */
    public suspend fun append(): LoadResult<Key, Value>? {
        return doLoad(APPEND)
    }

    /**
     * Performs a load of [LoadType.PREPEND] on the PagingSource.
     *
     * Since Paging's first load is always of [LoadType.REFRESH], [refresh] must always be called
     * first before this [prepend] is called.
     *
     * Returns the [LoadResult] from calling [PagingSource.load]. If the [LoadParams.key] is null,
     * such as when there is no more data to prepend, this prepend will be no-op by returning null.
     */
    public suspend fun prepend(): LoadResult<Key, Value>? {
        return doLoad(PREPEND)
    }

    /**
     * Helper to perform REFRESH loads.
     */
    private suspend fun doInitialLoad(initialKey: Key?): LoadResult<Key, Value> {
        return lock.withLock {
            ensureValidPagingSource()
            pagingSource.load(
                LoadParams.Refresh(initialKey, config.initialLoadSize, config.enablePlaceholders)
            ).also { result ->
                if (result is LoadResult.Page) {
                    pages.addLast(result)
                }
            }
        }
    }

    /**
     * Helper to perform APPEND or PREPEND loads.
     */
    private suspend fun doLoad(loadType: LoadType): LoadResult<Key, Value>? {
        return lock.withLock {
            ensureValidPagingSource()
            if (!hasRefreshed.get()) {
                throw IllegalStateException("TestPager's first load operation must be a refresh. " +
                    "Please call refresh() once before calling ${loadType.name.lowercase()}().")
            }
            when (loadType) {
                REFRESH -> throw IllegalArgumentException(
                    "For LoadType.REFRESH use doInitialLoad()"
                )
                APPEND -> {
                    val key = pages.lastOrNull()?.nextKey ?: return null
                    pagingSource.load(
                        LoadParams.Append(key, config.pageSize, config.enablePlaceholders)
                    ).also { result ->
                        if (result is LoadResult.Page) {
                            pages.addLast(result)
                        }
                    }
                } PREPEND -> {
                    val key = pages.firstOrNull()?.prevKey ?: return null
                    pagingSource.load(
                        LoadParams.Prepend(key, config.pageSize, config.enablePlaceholders)
                    ).also { result ->
                        if (result is LoadResult.Page) {
                            pages.addFirst(result)
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the most recent [LoadResult.Page] loaded from the [PagingSource]. Null if
     * no pages have been returned from [PagingSource]. For example, if PagingSource has
     * only returned [LoadResult.Error] or [LoadResult.Invalid].
     */
    public suspend fun getLastLoadedPage(): LoadResult.Page<Key, Value>? {
        return lock.withLock {
            pages.lastOrNull()
        }
    }

    /**
     * Returns the current list of [LoadResult.Page] loaded so far from the [PagingSource].
     */
    public suspend fun getPages(): List<LoadResult.Page<Key, Value>> {
        return lock.withLock {
            pages.toList()
        }
    }

    /**
     * Returns a [PagingState] to generate a [LoadParams.key] by supplying it to
     * [PagingSource.getRefreshKey]. The key returned from [PagingSource.getRefreshKey]
     * should be used as the [LoadParams.Refresh.key] when calling [refresh] on a new generation of
     * TestPager.
     *
     * The anchorPosition must be within index of loaded items, which can include
     * placeholders if [PagingConfig.enablePlaceholders] is true. For example:
     * - No placeholders: If 40 items have been loaded so far , anchorPosition must be
     * in [0 .. 39].
     * - With placeholders: If there are a total of 100 loadable items, the anchorPosition
     * must be in [0..99].
     *
     * The [anchorPosition] should be the index that the user has hypothetically
     * scrolled to on the UI. Since the [PagingState.anchorPosition] in Paging can be based
     * on any item or placeholder currently visible on the screen, the actual
     * value of [PagingState.anchorPosition] may not exactly match the [anchorPosition] passed
     * to this function even if viewing the same page of data.
     *
     * @param anchorPosition the index representing the last accessed item within the
     * items presented on the UI, which may be a placeholder if
     * [PagingConfig.enablePlaceholders] is true.
     *
     * @throws IllegalStateException if anchorPosition is out of bounds
     */
    public suspend fun getPagingState(anchorPosition: Int): PagingState<Key, Value> {
        lock.withLock {
            checkWithinBoundary(anchorPosition)
            return PagingState(
                pages = pages.toList(),
                anchorPosition = anchorPosition,
                config = config,
                leadingPlaceholderCount = getLeadingPlaceholderCount()
            )
        }
    }

    /**
     * Ensures the anchorPosition is within boundary of loaded data.
     *
     * If placeholders are enabled, the provided anchorPosition must be within boundaries of
     * [0 .. itemCount - 1], which includes placeholders before and after loaded data.
     *
     * If placeholders are disabled, the provided anchorPosition must be within boundaries of
     * [0 .. loaded data size - 1].
     *
     * @throws IllegalStateException if anchorPosition is out of bounds
     */
    private fun checkWithinBoundary(anchorPosition: Int) {
        val loadedSize = pages.fold(0) { acc, page ->
            acc + page.data.size
        }
        val maxBoundary = if (config.enablePlaceholders) {
            (pages.firstOrNull()?.itemsBefore ?: 0) + loadedSize +
                (pages.lastOrNull()?.itemsAfter ?: 0) - 1
        } else {
            loadedSize - 1
        }
        check(anchorPosition in 0..maxBoundary) {
            "anchorPosition $anchorPosition is out of bounds between [0..$maxBoundary]. Please " +
                "provide a valid anchorPosition."
        }
    }

    // Number of placeholders before the first loaded item if placeholders are enabled, otherwise 0.
    private fun getLeadingPlaceholderCount(): Int {
        return if (config.enablePlaceholders) {
            // itemsBefore represents placeholders before first loaded item, and can be
            // one of three.
            // 1. valid int if implemented
            // 2. null if pages empty
            // 3. COUNT_UNDEFINED if not implemented
            val itemsBefore: Int? = pages.firstOrNull()?.itemsBefore
            // finalItemsBefore is `null` if it is either case 2. or 3.
            val finalItemsBefore = if (itemsBefore == null || itemsBefore == COUNT_UNDEFINED) {
                null
            } else {
                itemsBefore
            }
            // This will ultimately return 0 if user didn't implement itemsBefore or if pages
            // are empty, i.e. user called getPagingState before any loads.
            finalItemsBefore ?: 0
        } else {
            0
        }
    }

    private fun ensureValidPagingSource() {
        check(!pagingSource.invalid) {
            "This TestPager cannot perform further loads as PagingSource $pagingSource has " +
                "been invalidated. If the PagingSource is expected to be invalid, you can " +
                "continue to load by creating a new TestPager with a new PagingSource."
        }
    }
}