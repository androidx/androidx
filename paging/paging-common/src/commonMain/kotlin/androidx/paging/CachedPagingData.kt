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

import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.paging.ActiveFlowTracker.FlowType.PAGED_DATA_FLOW
import androidx.paging.ActiveFlowTracker.FlowType.PAGE_EVENT_FLOW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

/**
 * A PagingData wrapper that makes it "efficiently" share-able between multiple downstreams.
 * It flattens all previous pages such that a new subscriber will get all of them at once (and
 * also not deal with dropped pages, intermediate loading state changes etc).
 */
private class MulticastedPagingData<T : Any>(
    val scope: CoroutineScope,
    val parent: PagingData<T>,
    // used in tests
    val tracker: ActiveFlowTracker? = null
) {
    private val accumulated = CachedPageEventFlow(
        src = parent.flow,
        scope = scope
    ).also {
        tracker?.onNewCachedEventFlow(it)
    }

    fun asPagingData() = PagingData(
        flow = accumulated.downstreamFlow.onStart {
            tracker?.onStart(PAGE_EVENT_FLOW)
        }.onCompletion {
            tracker?.onComplete(PAGE_EVENT_FLOW)
        },
        uiReceiver = parent.uiReceiver,
        hintReceiver = parent.hintReceiver,
        cachedPageEvent = { accumulated.getCachedEvent() }
    )

    suspend fun close() = accumulated.close()
}

/**
 * Caches the [PagingData] such that any downstream collection from this flow will share the same
 * [PagingData].
 *
 * The flow is kept active as long as the given [scope] is active. To avoid leaks, make sure to
 * use a [scope] that is already managed (like a ViewModel scope) or manually cancel it when you
 * don't need paging anymore.
 *
 * A common use case for this caching is to cache [PagingData] in a ViewModel. This can ensure that,
 * upon configuration change (e.g. rotation), then new Activity will receive the existing data
 * immediately rather than fetching it from scratch.
 *
 * Calling [cachedIn] is required to allow calling
 * [submitData][androidx.paging.AsyncPagingDataAdapter] on the same instance of [PagingData]
 * emitted by [Pager] or any of its transformed derivatives, as reloading data from scratch on the
 * same generation of [PagingData] is an unsupported operation.
 *
 * Note that this does not turn the `Flow<PagingData>` into a hot stream. It won't execute any
 * unnecessary code unless it is being collected.
 *
 * @sample androidx.paging.samples.cachedInSample
 *
 * @param scope The coroutine scope where this page cache will be kept alive.
 */
@CheckResult
public fun <T : Any> Flow<PagingData<T>>.cachedIn(
    scope: CoroutineScope
): Flow<PagingData<T>> = cachedIn(scope, null)

internal fun <T : Any> Flow<PagingData<T>>.cachedIn(
    scope: CoroutineScope,
    // used in tests
    tracker: ActiveFlowTracker? = null
): Flow<PagingData<T>> {
    return this.simpleMapLatest {
        MulticastedPagingData(
            scope = scope,
            parent = it,
            tracker = tracker
        )
    }.simpleRunningReduce { prev, next ->
        prev.close()
        next
    }.map {
        it.asPagingData()
    }.onStart {
        tracker?.onStart(PAGED_DATA_FLOW)
    }.onCompletion {
        tracker?.onComplete(PAGED_DATA_FLOW)
    }.shareIn(
        scope = scope,
        started = SharingStarted.Lazily,
        // replay latest multicasted paging data since it is re-connectable.
        replay = 1
    )
}

/**
 * This is only used for testing to ensure we don't leak resources
 */
@VisibleForTesting
internal interface ActiveFlowTracker {
    fun onNewCachedEventFlow(cachedPageEventFlow: CachedPageEventFlow<*>)
    suspend fun onStart(flowType: FlowType)
    suspend fun onComplete(flowType: FlowType)

    enum class FlowType {
        PAGED_DATA_FLOW,
        PAGE_EVENT_FLOW
    }
}
