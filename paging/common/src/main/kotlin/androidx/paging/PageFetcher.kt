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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan

@ExperimentalCoroutinesApi
@FlowPreview
internal class PageFetcher<Key : Any, Value : Any>(
    private val pagedSourceFactory: PagedSourceFactory<Key, Value>,
    private val initialKey: Key?,
    private val config: PagedList.Config
) {
    // NOTE: This channel is conflated, which means it has a buffer size of 1, and will always
    // broadcast the latest value received.
    private var refreshChannel = Channel<Unit>(Channel.CONFLATED)

    // The object built by paging builder can maintain the scope so that on rotation we don't stop
    // the paging.
    fun createFlow(): Flow<PagedData<Value>> {
        refreshChannel.offer(Unit)
        return refreshChannel.consumeAsFlow()
            .scan(null) { previousGeneration: Pager<Key, Value>?, _ ->
                // TODO: Call pagedSource.invalidate on previous pagedSource
                val pagedSource = pagedSourceFactory()
                val initialKey = when (previousGeneration) {
                    null -> initialKey
                    else -> when (val info = previousGeneration.refreshKeyInfo()) {
                        null -> previousGeneration.initialKey
                        else -> pagedSource.getRefreshKeyFromPage(info.indexInPage, info.page)
                    }
                }
                Pager(initialKey, pagedSource, config)
            }
            .filterNotNull()
            .mapLatest { generation -> PagedData(generation.create(), generation::addHint) }
    }

    fun refresh() {
        refreshChannel.offer(Unit)
    }
}
