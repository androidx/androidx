/*
 * Copyright 2021 The Android Open Source Project
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

import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

/**
 * reproduces b/203594733
 */
public class CachedPageEventFlowLeakTest {
    private val gcHelper = GarbageCollectionTestHelper()

    private data class Item(
        val generation: Int,
        val pagePos: Int
    )

    private var sourceGeneration = 0
    private val pager = Pager(
        config = PagingConfig(
            pageSize = 10,
            initialLoadSize = 20
        ),
        pagingSourceFactory = {
            val generation = sourceGeneration++
            object : PagingSource<Int, Item>() {
                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
                    return LoadResult.Page(
                        data = (0 until params.loadSize).map {
                            Item(
                                generation = generation,
                                pagePos = it
                            )
                        },
                        prevKey = (params.key ?: 0) - 1,
                        nextKey = (params.key ?: 0) + 1
                    )
                }

                override fun getRefreshKey(state: PagingState<Int, Item>): Int? {
                    return null
                }
            }
        }
    )

    private val tracker = object : ActiveFlowTracker {
        override fun onNewCachedEventFlow(cachedPageEventFlow: CachedPageEventFlow<*>) {
            gcHelper.track(cachedPageEventFlow)
        }

        override suspend fun onStart(flowType: ActiveFlowTracker.FlowType) {
        }

        override suspend fun onComplete(flowType: ActiveFlowTracker.FlowType) {
        }
    }

    private suspend fun <T : Any> collectPages(
        flow: Flow<PagingData<T>>,
        // how many generations to collect
        generationCount: Int,
        // completed once we've invalidated [generationCount] generations and also received
        // a new insert from the last generation
        doneInvalidating: CompletableDeferred<Unit>?,
        /**
         * If true, this method will stop collecting once it reached the desired generation number.
         * Otherwise, it will never return and keep collecting forever.
         */
        finishCollecting: Boolean
    ) {
        // collect expected generations to generate garbage
        var remaining = generationCount
        flow
            .takeWhile {
                !finishCollecting || remaining > 0
            }
            .collectLatest { pagingData ->
                val willInvalidate = remaining-- > 0
                if (willInvalidate) {
                    gcHelper.track(pagingData)
                }
                var invalidated = false
                // invalidate after receiving 1 insert.
                pagingData.flow.collect {
                    if (willInvalidate) {
                        gcHelper.track(it)
                    }
                    if (it is PageEvent.Insert) {
                        if (willInvalidate) {
                            gcHelper.track(it.pages)
                            it.pages.flatMap { it.data }.forEach(gcHelper::track)
                            if (!invalidated) {
                                // invalidate only once per generation to avoid
                                // delayed invalidates
                                invalidated = true
                                pagingData.uiReceiver.refresh()
                            }
                        } else {
                            doneInvalidating?.complete(Unit)
                        }
                    }
                }
            }
        // always complete in case finishCollection is set to true
        doneInvalidating?.complete(Unit)
    }

    @Ignore // b/206837348
    @Test
    public fun dontLeakCachedPageEventFlows_finished() = runTest {
        val scope = CoroutineScope(EmptyCoroutineContext)
        val flow = pager.flow.cachedIn(scope, tracker)
        collectPages(
            flow = flow,
            generationCount = 20,
            doneInvalidating = null,
            finishCollecting = true
        )
        gcHelper.assertLiveObjects(
            // see b/204125064
            // this should ideally be 0 but right now, we keep the previous generation's state
            // to be able to find anchor for the new position but we don't clear it yet. It can
            // only be cleared after the new generation loads a page.
            Item::class to 20,
            CachedPageEventFlow::class to 1
        )
        scope.cancel()
    }

    @Test
    public fun dontLeakNonCachedFlow_finished() = runTest {
        collectPages(
            flow = pager.flow,
            generationCount = 10,
            doneInvalidating = null,
            finishCollecting = true
        )
        gcHelper.assertEverythingIsCollected()
    }

    @Test
    public fun dontLeakPreviousPageInfo_stillCollecting() = runTest {
        // reproduces b/204125064
        val doneInvalidating = CompletableDeferred<Unit>()
        val collection = launch {
            collectPages(
                flow = pager.flow,
                generationCount = 10,
                doneInvalidating = doneInvalidating,
                finishCollecting = false
            )
        }
        // make sure we collected enough generations
        doneInvalidating.await()
        gcHelper.assertLiveObjects(
            // see b/204125064
            // this should ideally be 0 but right now, we keep the previous generation's state
            // to be able to find anchor for the new position but we don't clear it yet. It can
            // only be cleared after the new generation loads a page.
            Item::class to 20
        )
        collection.cancelAndJoin()
    }

    // Broken: b/206981029
    @Ignore
    @Test
    public fun dontLeakPreviousPageInfoWithCache_stillCollecting() = runTest {
        // reproduces b/204125064
        val scope = CoroutineScope(EmptyCoroutineContext)
        val flow = pager.flow.cachedIn(scope, tracker)
        val doneInvalidating = CompletableDeferred<Unit>()
        val collection = launch {
            collectPages(
                flow = flow,
                generationCount = 10,
                doneInvalidating = doneInvalidating,
                finishCollecting = false
            )
        }
        // make sure we collected enough generations
        doneInvalidating.await()
        gcHelper.assertLiveObjects(
            // see b/204125064
            // this should ideally be 0 but right now, we keep the previous generation's state
            // to be able to find anchor for the new position but we don't clear it yet. It can
            // only be cleared after the new generation loads a page.
            Item::class to 20,
            CachedPageEventFlow::class to 1
        )
        collection.cancelAndJoin()
    }
}
