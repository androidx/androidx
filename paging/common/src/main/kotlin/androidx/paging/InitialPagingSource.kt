/*
 * Copyright 2021 The Android Open Source Project
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
 * [InitialPagingSource] is a placeholder [PagingSource] implementation that only returns empty
 * pages and `null` keys.
 *
 * It should be used exclusively in [InitialPagedList] since it is required to be supplied
 * synchronously, but [DataSource.Factory] should run on background thread.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InitialPagingSource<K : Any, V : Any> : PagingSource<K, V>() {
    override suspend fun load(params: LoadParams<K>): LoadResult<K, V> {
        return LoadResult.Page.empty()
    }

    override fun getRefreshKey(state: PagingState<K, V>): K? {
        return null
    }
}
