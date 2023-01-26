/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.paging.testing

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.PagingSource
import androidx.paging.Pager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Extension method on a [Flow] to generate a `PagingSourceFactory` for constructing a [Pager].
 *
 * The [Flow] of list represents the data source, with each static list representing a generation
 * of data from which a [PagingSource] will load from. With every emission, the current
 * [PagingSource] will be invalidated, thereby triggering a new generation of Paged data.
 *
 * Supports multiple factories and thus multiple collection on the same flow.
 *
 * Returns a `pagingSourceFactory` to be supplied to a [Pager].
 *
 * @param coroutineScope the CoroutineScope to collect from the Flow of list.
 */
public fun <Value : Any> Flow<@JvmSuppressWildcards List<Value>>.asPagingSourceFactory(
    coroutineScope: CoroutineScope
): () -> PagingSource<Int, Value> {

    var data: List<Value>? = null

    val factory = InvalidatingPagingSourceFactory {
        val dataSource = data ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        StaticListPagingSource(dataSource)
    }

    coroutineScope.launch {
        collect { list ->
            data = list
            factory.invalidate()
        }
    }

    return factory
}