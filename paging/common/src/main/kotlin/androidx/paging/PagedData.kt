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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class PagedData<T : Any> internal constructor(
    internal val flow: Flow<PageEvent<T>>,
    internal val hintReceiver: (ViewportHint) -> Unit
) {
    @ExperimentalCoroutinesApi
    fun <T : Any> Flow<PagedData<T>>.toPagingState(): Flow<PagingState<T>> {
        return flatMapLatest {
            val producer = PagingState.Producer<T>(it.hintReceiver)
            it.flow.map { pageEvent -> producer.processEvent(pageEvent) }
        }
    }
}
