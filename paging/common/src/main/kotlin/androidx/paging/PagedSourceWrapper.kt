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

/**
 * TODO: Move all call-sites dependent on this to use [PagedSource] directly.
 *
 * A wrapper around [DataSource] which adapts it to the [PagedSource] API.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PagedSourceWrapper<Key : Any, Value : Any>(
    internal val dataSource: DataSource<Key, Value>
) : PagedSource<Key, Value>() {
    init {
        dataSource.addInvalidatedCallback { invalidate() }
    }

    @Suppress("UNCHECKED_CAST")
    override val keyProvider = when (dataSource.type) {
        DataSource.KeyType.POSITIONAL -> KeyProvider.Positional as KeyProvider<Key, Value>
        DataSource.KeyType.PAGE_KEYED -> KeyProvider.PageKeyGlobal()
        DataSource.KeyType.ITEM_KEYED -> object : KeyProvider.ItemKey<Key, Value>() {
            override fun getKey(item: Value) = dataSource.getKeyInternal(item)
        }
    }

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
        val dataSourceParams = DataSource.Params(
            params.loadType,
            params.key,
            params.loadSize,
            params.placeholdersEnabled,
            params.pageSize
        )

        return dataSource.load(dataSourceParams).toLoadResult()
    }

    override fun isRetryableError(error: Throwable) = dataSource.isRetryableError(error)

    override fun invalidate() {
        super.invalidate()
        dataSource.invalidate()
    }
}
