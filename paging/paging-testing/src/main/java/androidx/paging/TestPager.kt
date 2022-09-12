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

import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.Pager
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

    private var nextKey: Key? = null
    private var prevKey: Key? = null
    private val pages = ArrayDeque<LoadResult.Page<Key, Value>>()

    // TODO add instruction that refresh() must be called before either append() or prepend()
    /**
     * Performs a load of [LoadType.REFRESH] on the PagingSource.
     *
     * If initialKey != null, refresh will start loading from the supplied key.
     *
     * Since Paging's first load is always of [LoadType.REFRESH], this method must be the very
     * first load operation to be called on the TestPager. For example, you can call
     * [getLastLoadedPage] before any load operations.
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
        ensureValidPagingSource()
        if (!hasRefreshed.compareAndSet(false, true)) {
            pagingSource.invalidate()
            throw IllegalStateException("TestPager does not support multi-generational access " +
                "and refresh() can only be called once per TestPager. To start a new generation," +
                "create a new TestPager with a new PagingSource.")
        }

        return lock.withLock {
            pagingSource.load(
                LoadParams.Refresh(initialKey, config.initialLoadSize, config.enablePlaceholders)
            ).also { result ->
                if (result is LoadResult.Page) {
                    pages.addLast(result)
                    nextKey = result.nextKey
                    prevKey = result.prevKey
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

    private fun ensureValidPagingSource() {
        check(!pagingSource.invalid) {
            "This TestPager cannot perform further loads as PagingSource $pagingSource has " +
                "been invalidated. If the PagingSource is expected to be invalid, you can " +
                "continue to load by creating a new TestPager with a new PagingSource."
        }
    }
}