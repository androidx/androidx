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
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

@UseExperimental(ExperimentalCoroutinesApi::class)
internal class PageFetcher<Key : Any, Value : Any>(
    private val pagingSourceFactory: () -> PagingSource<Key, Value>,
    private val initialKey: Key?,
    private val config: PagingConfig
) {

    // NOTE: This channel is conflated, which means it has a buffer size of 1, and will always
    // broadcast the latest value received.
    private var refreshChannel = ConflatedBroadcastChannel<Unit>()

    // The object built by paging builder can maintain the scope so that on rotation we don't stop
    // the paging.
    @UseExperimental(FlowPreview::class)
    val flow = refreshChannel
        .asFlow()
        .onStart { emit(Unit) }
        .scan(null) { previousGeneration: Pager<Key, Value>?, _ ->
            val pagingSource = pagingSourceFactory()
            val initialKey = previousGeneration?.refreshKeyInfo()
                ?.let { pagingSource.getRefreshKey(it) }
                ?: initialKey

            // Hook up refresh signals from DataSource / PagingSource.
            pagingSource.registerInvalidatedCallback(::refresh)
            previousGeneration?.pagingSource?.unregisterInvalidatedCallback(::refresh)
            previousGeneration?.pagingSource?.invalidate() // Note: Invalidate is idempotent.
            previousGeneration?.close()

            Pager(initialKey, pagingSource, config)
        }
        .filterNotNull()
        .mapLatest { generation ->
            PagingData(generation.pageEventFlow, PagerUiReceiver(generation))
        }

    fun refresh() {
        refreshChannel.offer(Unit)
    }

    inner class PagerUiReceiver<Key : Any, Value : Any> constructor(
        private val pager: Pager<Key, Value>
    ) : UiReceiver {
        override fun addHint(hint: ViewportHint) = pager.addHint(hint)

        override fun retry() = pager.retry()

        override fun refresh() = this@PageFetcher.refresh()
    }
}
