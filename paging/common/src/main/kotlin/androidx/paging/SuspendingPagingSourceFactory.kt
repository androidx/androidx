/*
 * Copyright 2020 The Android Open Source Project
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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Utility class to convert the paging source factory to a suspend one.
 *
 * This is internal because it is only necessary for the legacy paging source implementation
 * where the data source must be created on the given thread pool for API guarantees.
 * see: b/173029013
 * see: b/168061354
 */
internal class SuspendingPagingSourceFactory<Key : Any, Value : Any>(
    private val dispatcher: CoroutineDispatcher,
    private val delegate: () -> PagingSource<Key, Value>
) : () -> PagingSource<Key, Value> {
    suspend fun create(): PagingSource<Key, Value> {
        return withContext(dispatcher) {
            delegate()
        }
    }

    override fun invoke(): PagingSource<Key, Value> {
        return delegate()
    }
}
