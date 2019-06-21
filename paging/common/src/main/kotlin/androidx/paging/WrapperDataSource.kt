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
import androidx.paging.futures.DirectExecutor
import androidx.paging.futures.transform
import com.google.common.util.concurrent.ListenableFuture
import java.util.IdentityHashMap

/**
 * @param Key DataSource key type, same for original and wrapped.
 * @param ValueFrom Value type of original DataSource.
 * @param ValueTo Value type of new DataSource.
 */
internal open class WrapperDataSource<Key : Any, ValueFrom : Any, ValueTo : Any>(
    private val source: DataSource<Key, ValueFrom>,
    private val listFunction: Function<List<ValueFrom>, List<ValueTo>>
) : DataSource<Key, ValueTo>(source.type) {
    private val keyMap = when (source.type) {
        KeyType.ITEM_KEYED -> IdentityHashMap<ValueTo, Key>()
        else -> null
    }

    override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) =
        source.addInvalidatedCallback(onInvalidatedCallback)

    override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) =
        source.removeInvalidatedCallback(onInvalidatedCallback)

    override fun invalidate() = source.invalidate()

    override val isInvalid
        get() = source.isInvalid

    override fun getKeyInternal(item: ValueTo): Key = when {
        keyMap != null -> synchronized(keyMap) {
            return keyMap[item]!!
        }
        // positional / page-keyed
        else -> throw IllegalStateException("Cannot get key by item in non-item keyed DataSource")
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    fun stashKeysIfNeeded(source: List<ValueFrom>, dest: List<ValueTo>) {
        if (keyMap != null) {
            synchronized(keyMap) {
                for (i in dest.indices) {
                    keyMap[dest[i]] =
                        (this.source as ItemKeyedDataSource).getKey(source[i])
                }
            }
        }
    }

    override fun load(params: Params<Key>): ListenableFuture<out BaseResult<ValueTo>> =
        source.load(params).transform(
            Function { input ->
                val result = BaseResult.convert(input, listFunction)
                stashKeysIfNeeded(input.data, result.data)
                result
            },
            DirectExecutor
        )
}
