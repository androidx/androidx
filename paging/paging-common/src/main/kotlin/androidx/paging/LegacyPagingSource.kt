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

import androidx.annotation.RestrictTo
import androidx.paging.DataSource.KeyType.ITEM_KEYED
import androidx.paging.DataSource.KeyType.PAGE_KEYED
import androidx.paging.DataSource.KeyType.POSITIONAL
import androidx.paging.DataSource.Params
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.internal.BUGANIZER_URL
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withContext

/**
 * A wrapper around [DataSource] which adapts it to the [PagingSource] API.
 *
 */
@OptIn(DelicateCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LegacyPagingSource<Key : Any, Value : Any>(
    private val fetchContext: CoroutineContext,
    internal val dataSource: DataSource<Key, Value>
) : PagingSource<Key, Value>() {
    private var pageSize: Int = PAGE_SIZE_NOT_SET

    init {
        dataSource.addInvalidatedCallback(::invalidate)
        // technically, there is a possibly race where data source might call back our invalidate.
        // in practice, it is fine because all fields are initialized at this point.
        registerInvalidatedCallback {
            dataSource.removeInvalidatedCallback(::invalidate)
            dataSource.invalidate()
        }
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setPageSize(pageSize: Int) {
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
                $BUGANIZER_URL
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

        return withContext(fetchContext) {
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

    private companion object {
        private const val PAGE_SIZE_NOT_SET = Int.MIN_VALUE
    }
}
