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

import androidx.paging.DataSource.KeyType.ITEM_KEYED
import androidx.paging.DataSource.KeyType.PAGE_KEYED
import androidx.paging.DataSource.KeyType.POSITIONAL
import androidx.paging.DataSource.Params
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * A wrapper around [DataSource] which adapts it to the [PagingSource] API.
 */
internal class LegacyPagingSource<Key : Any, Value : Any>(
    private val fetchDispatcher: CoroutineDispatcher = DirectDispatcher,
    internal val dataSourceFactory: () -> DataSource<Key, Value>
) : PagingSource<Key, Value>() {
    private var pageSize: Int = PAGE_SIZE_NOT_SET
    // Lazily initialize because it must be created on fetchDispatcher, but PagingSourceFactory
    // passed to Pager is a non-suspending method.
    internal val dataSource by lazy {
        dataSourceFactory().also { dataSource ->
            dataSource.addInvalidatedCallback(::invalidate)
            // LegacyPagingSource registers invalidate callback after DataSource is created, so we
            // need to check for race condition here. If DataSource is already invalid, simply
            // propagate invalidation manually.
            if (dataSource.isInvalid && !invalid) {
                dataSource.removeInvalidatedCallback(::invalidate)
                // Note: Calling this.invalidate directly will re-evaluate dataSource's by lazy
                // init block, since we haven't returned a value for dataSource yet.
                super.invalidate()
            }
        }
    }

    fun setPageSize(pageSize: Int) {
        check(this.pageSize == PAGE_SIZE_NOT_SET || pageSize == this.pageSize) {
            "Page size is already set to ${this.pageSize}."
        }
        this.pageSize = pageSize
    }

    /**
     * This only ever happens in testing if Pager / PagedList is not used hence we'll not get the
     * page size. For those cases, guess :).
     */
    private fun guessPageSize(params: LoadParams<Key>): Int {
        if (params is LoadParams.Refresh) {
            if (params.loadSize % PagingConfig.DEFAULT_INITIAL_PAGE_MULTIPLIER == 0) {
                return params.loadSize / PagingConfig.DEFAULT_INITIAL_PAGE_MULTIPLIER
            }
        }
        return params.loadSize
    }

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        val type = when (params) {
            is LoadParams.Refresh -> REFRESH
            is LoadParams.Append -> APPEND
            is LoadParams.Prepend -> PREPEND
        }
        if (pageSize == PAGE_SIZE_NOT_SET) {
            // println because we don't have android logger here
            println(
                """
                WARNING: pageSize on the LegacyPagingSource is not set.
                When using legacy DataSource / DataSourceFactory with Paging3, page size
                should've been set by the paging library but it is not set yet.

                If you are seeing this message in tests where you are testing DataSource
                in isolation (without a Pager), it is expected and page size will be estimated
                based on parameters.

                If you are seeing this message despite using a Pager, please file a bug:
                https://issuetracker.google.com/issues/new?component=413106
                """.trimIndent()
            )
            pageSize = guessPageSize(params)
        }
        val dataSourceParams = Params(
            type,
            params.key,
            params.loadSize,
            params.placeholdersEnabled,
            pageSize
        )

        return withContext(fetchDispatcher) {
            dataSource.load(dataSourceParams).run {
                LoadResult.Page(
                    data,
                    @Suppress("UNCHECKED_CAST")
                    if (data.isEmpty() && params is LoadParams.Prepend) null else prevKey as Key?,
                    @Suppress("UNCHECKED_CAST")
                    if (data.isEmpty() && params is LoadParams.Append) null else nextKey as Key?,
                    itemsBefore,
                    itemsAfter
                )
            }
        }
    }

    override fun invalidate() {
        super.invalidate()
        dataSource.invalidate()
    }

    @OptIn(ExperimentalPagingApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun getRefreshKey(state: PagingState<Key, Value>): Key? {
        return when (dataSource.type) {
            POSITIONAL -> state.anchorPosition?.let { anchorPosition ->
                state.anchorPositionToPagedIndices(anchorPosition) { _, indexInPage ->
                    val offset = state.closestPageToPosition(anchorPosition)?.prevKey ?: 0
                    (offset as Int).plus(indexInPage) as Key?
                }
            }
            PAGE_KEYED -> null
            ITEM_KEYED ->
                state.anchorPosition
                    ?.let { anchorPosition -> state.closestItemToPosition(anchorPosition) }
                    ?.let { item -> dataSource.getKeyInternal(item) }
        }
    }

    override val jumpingSupported: Boolean
        get() = dataSource.type == POSITIONAL

    companion object {
        const val PAGE_SIZE_NOT_SET = Integer.MIN_VALUE
    }
}
