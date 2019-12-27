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

import androidx.annotation.VisibleForTesting
import androidx.paging.ActiveFlowTracker.FlowType.PAGED_DATA_FLOW
import androidx.paging.ActiveFlowTracker.FlowType.PAGE_EVENT_FLOW
import androidx.paging.multicast.Multicaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

private class MulticastedPagedData<T : Any>(
    val scope: CoroutineScope,
    val parent: PagedData<T>,
    // used in tests
    val tracker: ActiveFlowTracker? = null
) {
    // TODO: instead of relying on paging cache, use something like FlowList that can consalidate
    //  multiple pages into 1 and also avoid caching dropped pages.
    @ExperimentalCoroutinesApi
    @FlowPreview
    private val multicasted = Multicaster(
        scope = scope,
        bufferSize = 100_000,
        piggybackingDownstream = false,
        source = parent.flow.onStart {
            tracker?.onStart(PAGE_EVENT_FLOW)
        }.onCompletion {
            tracker?.onComplete(PAGE_EVENT_FLOW)
        },
        onEach = {},
        keepUpstreamAlive = true
    )

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun asPagedData() = PagedData(
        flow = multicasted.flow,
        receiver = parent.receiver
    )

    @FlowPreview
    @ExperimentalCoroutinesApi
    suspend fun close() = multicasted.close()
}

/**
 * Caches the PagedData such that any downstream collection from this flow will share the same
 * paging data.
 *
 * The flow is kept active as long as the given [scope] is active. To avoid leaks, make sure to
 * use a [scope] that is already managed (like a ViewModel scope) or manually cancel it when you
 * don't need paging anymore.
 *
 * A common use case for this caching is to cache PagedData in a ViewModel. This can ensure that,
 * upon configuration change (e.g. rotation), then new Activity will receive the existing data
 * immediately rather than fetching it from scratch.
 *
 * Note that this does not turn the `Flow<PagedData>` into a hot stream. It won't execute any
 * unnecessary code unless it is being collected.
 *
 * ```
 * class MyViewModel : ViewModel() {
 *     val pagedData : Flow<PagedData<Item>> = PagedDataFlowBuilder(
 *         pagedSourceFactory = <factory>,
 *         config = <config>)
 *     ).build()
 *     .cached(viewModelScope)
 * }
 *
 * class MyActivity : Activity() {
 *     override fun onCreate() {
 *         val pages = myViewModel.pagedData
 *     }
 * }
 * ```
 *
 * @param scope The coroutine scope where this page cache will be kept alive.
 */
@ExperimentalCoroutinesApi
@FlowPreview
fun <T : Any> Flow<PagedData<T>>.cachedIn(
    scope: CoroutineScope
) = cachedIn(scope, null)

@FlowPreview
@ExperimentalCoroutinesApi
internal fun <T : Any> Flow<PagedData<T>>.cachedIn(
    scope: CoroutineScope,
    // used in tests
    tracker: ActiveFlowTracker? = null
): Flow<PagedData<T>> {
    val multicastedFlow = this.map {
        MulticastedPagedData(
            scope = scope,
            parent = it
        )
    }.scan(null as MulticastedPagedData<T>?) { prev, next ->
        prev?.close()
        next
    }.mapNotNull {
        it?.asPagedData()
    }.onStart {
        tracker?.onStart(PAGED_DATA_FLOW)
    }.onCompletion {
        tracker?.onComplete(PAGED_DATA_FLOW)
    }
    return Multicaster(
        scope = scope,
        bufferSize = 1,
        source = multicastedFlow,
        onEach = {},
        keepUpstreamAlive = true
    ).flow
}

/**
 * This is only used for testing to ensure we don't leak resources
 */
@VisibleForTesting
internal interface ActiveFlowTracker {
    suspend fun onStart(flowType: FlowType)
    suspend fun onComplete(flowType: FlowType)

    enum class FlowType {
        PAGED_DATA_FLOW,
        PAGE_EVENT_FLOW
    }
}