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
import java.util.IdentityHashMap

@Suppress("DEPRECATION")
internal class WrapperItemKeyedDataSource<K : Any, A : Any, B : Any>(
    private val source: ItemKeyedDataSource<K, A>,
    private val listFunction: Function<List<A>, List<B>>
) : ItemKeyedDataSource<K, B>() {

    private val keyMap = IdentityHashMap<B, K>()

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.addInvalidatedCallback(onInvalidatedCallback)
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
        source.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun invalidate() {
        source.invalidate()
    }

    override val isInvalid
        get() = source.isInvalid

    fun convertWithStashedKeys(source: List<A>): List<B> {
        val dest = convert(listFunction, source)
        synchronized(keyMap) {
            // synchronize on keyMap, since multiple loads may occur simultaneously.
            // Note: manually sync avoids locking per-item (e.g. Collections.synchronizedMap)
            for (i in dest.indices) {
                keyMap[dest[i]] = this.source.getKey(source[i])
            }
        }
        return dest
    }

    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<B>) {
        source.loadInitial(
            params,
            object : LoadInitialCallback<A>() {
                override fun onResult(data: List<A>, position: Int, totalCount: Int) {
                    callback.onResult(convertWithStashedKeys(data), position, totalCount)
                }

                override fun onResult(data: List<A>) {
                    callback.onResult(convertWithStashedKeys(data))
                }
            }
        )
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<B>) {
        source.loadAfter(
            params,
            object : LoadCallback<A>() {
                override fun onResult(data: List<A>) {
                    callback.onResult(convertWithStashedKeys(data))
                }
            }
        )
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<B>) {
        source.loadBefore(
            params,
            object : LoadCallback<A>() {
                override fun onResult(data: List<A>) {
                    callback.onResult(convertWithStashedKeys(data))
                }
            }
        )
    }

    override fun getKey(item: B): K = synchronized(keyMap) {
        return keyMap[item]!!
    }
}
