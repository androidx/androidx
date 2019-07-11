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

import androidx.paging.PagedSource.Companion.COUNT_UNDEFINED

/**
 * TODO: Move all call-sites dependent on this to use [PagedSource] directly.
 *
 * A wrapper around [DataSource] which adapts it to the [PagedSource] API.
 */
internal class PagedSourceWrapper<Key : Any, Value : Any>(
    private val dataSource: DataSource<Key, Value>
) : PagedSource<Key, Value>() {
    override val keyProvider: KeyProvider<Key, Value> = when (dataSource.type) {
        DataSource.KeyType.POSITIONAL -> {
            @Suppress("UNCHECKED_CAST") // Guaranteed to be the correct key type.
            KeyProvider.Positional<Value>() as KeyProvider<Key, Value>
        }
        DataSource.KeyType.PAGE_KEYED -> KeyProvider.PageKey()
        DataSource.KeyType.ITEM_KEYED -> object : KeyProvider.ItemKey<Key, Value>() {
            override fun getKey(item: Value) = dataSource.getKeyInternal(item)
        }
    }

    override val invalid: Boolean
        get() = dataSource.isInvalid

    override fun invalidate() = dataSource.invalidate()

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        val loadType = when (params.loadType) {
            LoadType.INITIAL -> DataSource.LoadType.INITIAL
            LoadType.START -> DataSource.LoadType.START
            LoadType.END -> DataSource.LoadType.END
        }

        val dataSourceParams = DataSource.Params(
            loadType,
            params.key,
            params.loadSize,
            params.placeholdersEnabled,
            params.pageSize
        )

        return dataSource.load(dataSourceParams).toLoadResult()
    }

    override fun isRetryableError(error: Throwable) = dataSource.isRetryableError(error)
}

/**
 * TODO: This should no longer be necessary once internal implementation has been moved to used
 * [PagedSource] directly.
 *
 * A wrapper around [PagedSource] which adapts it to the [DataSource] API.
 */
internal class DataSourceWrapper<Key : Any, Value : Any>(
    private val pagedSource: PagedSource<Key, Value>
) : DataSource<Key, Value>(
    when (pagedSource.keyProvider) {
        is PagedSource.KeyProvider.Positional -> KeyType.POSITIONAL
        is PagedSource.KeyProvider.PageKey -> KeyType.PAGE_KEYED
        is PagedSource.KeyProvider.ItemKey -> KeyType.ITEM_KEYED
    }

) {
    override suspend fun load(params: Params<Key>): BaseResult<Value> {
        val loadType = when (params.type) {
            LoadType.INITIAL -> PagedSource.LoadType.INITIAL
            LoadType.START -> PagedSource.LoadType.START
            LoadType.END -> PagedSource.LoadType.END
        }

        val dataSourceParams = PagedSource.LoadParams(
            loadType,
            params.key,
            params.initialLoadSize,
            params.placeholdersEnabled,
            params.pageSize
        )

        val initialResult = pagedSource.load(dataSourceParams)
        return BaseResult(
            initialResult.data,
            initialResult.prevKey,
            initialResult.nextKey,
            if (initialResult.itemsBefore != COUNT_UNDEFINED) initialResult.itemsBefore else 0,
            if (initialResult.itemsAfter != COUNT_UNDEFINED) initialResult.itemsAfter else 0,
            initialResult.offset,
            initialResult.counted
        )
    }

    /**
     * @throws IllegalStateException
     */
    override fun getKeyInternal(item: Value): Key {
        return when (val keyProvider = pagedSource.keyProvider) {
            is PagedSource.KeyProvider.Positional ->
                throw IllegalStateException("Cannot get key by item in positionalDataSource")
            is PagedSource.KeyProvider.PageKey ->
                throw IllegalStateException("Cannot get key by item in pageKeyedDataSource")
            is PagedSource.KeyProvider.ItemKey -> keyProvider.getKey(item)
        }
    }
}