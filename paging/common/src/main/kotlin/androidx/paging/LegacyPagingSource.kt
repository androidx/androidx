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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * A wrapper around [DataSource] which adapts it to the [PagingSource] API.
 */
internal class LegacyPagingSource<Key : Any, Value : Any>(
    internal val dataSource: DataSource<Key, Value>,
    private val fetchDispatcher: CoroutineDispatcher = DirectDispatcher
) : PagingSource<Key, Value>() {
    init {
        dataSource.addInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        val dataSourceParams = Params(
            params.loadType,
            params.key,
            params.loadSize,
            params.placeholdersEnabled,
            params.pageSize
        )

        return withContext(fetchDispatcher) {
            dataSource.load(dataSourceParams).toLoadResult<Key>()
        }
    }

    override fun invalidate() {
        super.invalidate()
        dataSource.invalidate()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRefreshKey(state: PagingState<Key, Value>): Key? {
        return when (dataSource.type) {
            POSITIONAL -> state.anchorPosition as Key
            PAGE_KEYED -> null
            ITEM_KEYED -> state.closestItemToPosition(state.anchorPosition)
                .let { dataSource.getKeyInternal(it) }
        }
    }
}
