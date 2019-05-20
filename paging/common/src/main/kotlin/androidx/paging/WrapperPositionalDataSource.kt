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

internal class WrapperPositionalDataSource<A : Any, B : Any>(
    private val source: PositionalDataSource<A>,
    val listFunction: Function<List<A>, List<B>>
) : PositionalDataSource<B>() {
    override val isInvalid
        get() = source.isInvalid

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) =
        source.addInvalidatedCallback(onInvalidatedCallback)

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) =
        source.removeInvalidatedCallback(onInvalidatedCallback)

    override fun invalidate() = source.invalidate()

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<B>) {
        source.loadInitial(params, object : LoadInitialCallback<A>() {
            override fun onResult(data: List<A>, position: Int, totalCount: Int) =
                callback.onResult(convert(listFunction, data), position, totalCount)

            override fun onResult(data: List<A>, position: Int) =
                callback.onResult(convert(listFunction, data), position)

            override fun onError(error: Throwable) = callback.onError(error)
        })
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<B>) {
        source.loadRange(params, object : LoadRangeCallback<A>() {
            override fun onResult(data: List<A>) = callback.onResult(convert(listFunction, data))

            override fun onError(error: Throwable) = callback.onError(error)
        })
    }
}
