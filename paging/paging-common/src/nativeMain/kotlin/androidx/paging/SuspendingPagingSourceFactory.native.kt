/*
 * Copyright 2025 The Android Open Source Project
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

internal actual class SuspendingPagingSourceFactory<Key : Any, Value : Any>
actual constructor(
    private val dispatcher: CoroutineDispatcher,
    private val delegate: () -> PagingSource<Key, Value>,
) : () -> PagingSource<Key, Value> {
    actual suspend fun create(): PagingSource<Key, Value> {
        return withContext(dispatcher) { delegate() }
    }

    actual override fun invoke(): PagingSource<Key, Value> {
        return delegate()
    }
}
