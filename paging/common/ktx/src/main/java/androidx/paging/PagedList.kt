/*
 * Copyright 2018 The Android Open Source Project
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

import java.util.concurrent.Executor

/**
 * Constructs a [PagedList], convenience for [PagedList.Builder].
 *
 * @param [Key] Type of key used to load data from the DataSource.
 * @param [Value] Type of items held and loaded by the PagedList.
 *
 * @param dataSource DataSource the PagedList will load from.
 * @param config Config that defines how the PagedList loads data from its DataSource.
 * @param notifyExecutor Executor that receives PagedList updates, and where
 * [PagedList.Callback] calls are dispatched. Generally, this is the UI/main thread.
 * @param fetchExecutor Executor used to fetch from DataSources, generally a background thread pool
 * for e.g. I/O or network loading.
 * @param boundaryCallback BoundaryCallback for listening to out-of-data events.
 * @param initialKey Key the DataSource should load around as part of initialization.
 */
@Suppress("FunctionName")
fun <Key : Any, Value : Any> PagedList(
    dataSource: DataSource<Key, Value>,
    config: PagedList.Config,
    notifyExecutor: Executor,
    fetchExecutor: Executor,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    initialKey: Key? = null
): PagedList<Value> {
    @Suppress("DEPRECATION")
    return PagedList.Builder(dataSource, config)
        .setNotifyExecutor(notifyExecutor)
        .setFetchExecutor(fetchExecutor)
        .setBoundaryCallback(boundaryCallback)
        .setInitialKey(initialKey)
        .build()
}
