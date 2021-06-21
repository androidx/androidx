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

/**
 * [InitialDataSource] is a placeholder [DataSource] implementation that only returns empty
 * pages and `null` keys.
 *
 * It should be used exclusively in [InitialPagedList] since it is required to be supplied
 * synchronously, but [DataSource.Factory] should run on background thread.
 */
@Suppress("DEPRECATION")
internal class InitialDataSource<K : Any, V : Any> : PageKeyedDataSource<K, V>() {
    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<K, V>) {
        callback.onResult(listOf(), 0, 0, null, null)
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        callback.onResult(listOf(), null)
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        callback.onResult(listOf(), null)
    }
}
