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

import androidx.arch.core.util.Function

@Suppress("DEPRECATION")
internal class WrapperPageKeyedDataSource<K : Any, A : Any, B : Any>(
    private val source: PageKeyedDataSource<K, A>,
    private val listFunction: Function<List<A>, List<B>>
) : PageKeyedDataSource<K, B>() {
    override val isInvalid
        get() = source.isInvalid

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.addInvalidatedCallback(onInvalidatedCallback)
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun invalidate() = source.invalidate()

    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<K, B>) {
        source.loadInitial(
            params,
            object : LoadInitialCallback<K, A>() {
                override fun onResult(
                    data: List<A>,
                    position: Int,
                    totalCount: Int,
                    previousPageKey: K?,
                    nextPageKey: K?
                ) {
                    val convertedData = convert(listFunction, data)
                    callback.onResult(
                        convertedData,
                        position,
                        totalCount,
                        previousPageKey,
                        nextPageKey
                    )
                }

                override fun onResult(data: List<A>, previousPageKey: K?, nextPageKey: K?) {
                    val convertedData = convert(listFunction, data)
                    callback.onResult(convertedData, previousPageKey, nextPageKey)
                }
            }
        )
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<K, B>) {
        source.loadBefore(
            params,
            object : LoadCallback<K, A>() {
                override fun onResult(data: List<A>, adjacentPageKey: K?) =
                    callback.onResult(convert(listFunction, data), adjacentPageKey)
            }
        )
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<K, B>) {
        source.loadAfter(
            params,
            object : LoadCallback<K, A>() {
                override fun onResult(data: List<A>, adjacentPageKey: K?) =
                    callback.onResult(convert(listFunction, data), adjacentPageKey)
            }
        )
    }
}
