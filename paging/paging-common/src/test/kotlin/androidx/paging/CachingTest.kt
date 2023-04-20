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

import androidx.paging.ActiveFlowTracker.FlowType
import androidx.paging.ActiveFlowTracker.FlowType.PAGED_DATA_FLOW
import androidx.paging.ActiveFlowTracker.FlowType.PAGE_EVENT_FLOW
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CachingTest {
    private val tracker = ActiveFlowTrackerImpl()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun noSharing() = testScope.runTest {
        val pageFlow = buildPageFlow()
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            )
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 1,
                generation = 0,
                start = 0,
                size = 9
            )
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(0)
    }

    @Test
    fun cached() = testScope.runTest {
        val pageFlow = buildPageFlow().cachedIn(backgroundScope, tracker)
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            )
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            )
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
    }

    @Test
    fun cached_afterMapping() = testScope.runTest {
        var mappingCnt = 0
        val pageFlow = buildPageFlow().map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = mappingIndex.toString())
            }
        }.cachedIn(backgroundScope, tracker)
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            ) {
                it.copy(metadata = "0")
            }
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            ) {
                it.copy(metadata = "0")
            }
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
    }

    @Test
    fun cached_beforeMapping() = testScope.runTest {
        var mappingCnt = 0
        val pageFlow = buildPageFlow().cachedIn(backgroundScope, tracker).map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = mappingIndex.toString())
            }
        }
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            ) {
                it.copy(metadata = "0")
            }
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            ) {
                it.copy(metadata = "1")
            }
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
    }

    @Test
    fun cached_afterMapping_withMoreMappingAfterwards() = testScope.runTest {
        var mappingCnt = 0
        val pageFlow = buildPageFlow().map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = mappingIndex.toString())
            }
        }.cachedIn(backgroundScope, tracker).map { pagingData ->
            val mappingIndex = mappingCnt++
            pagingData.map {
                it.copy(metadata = "${it.metadata}_$mappingIndex")
            }
        }
        val firstCollect = pageFlow.collectItemsUntilSize(6)
        val secondCollect = pageFlow.collectItemsUntilSize(9)
        assertThat(firstCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 6
            ) {
                it.copy(metadata = "0_1")
            }
        )

        assertThat(secondCollect).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 9
            ) {
                it.copy(metadata = "0_2")
            }
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
    }

    @Test
    fun pagesAreClosedProperty() {
        val job = SupervisorJob()
        val subScope = CoroutineScope(job + Dispatchers.Default)
        val pageFlow = buildPageFlow().cachedIn(subScope, tracker)
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(0)
        val items = runBlocking {
            pageFlow.collectItemsUntilSize(9)
        }
        val firstList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 9
        )
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
        val items2 = runBlocking {
            pageFlow.collectItemsUntilSize(21)
        }
        assertThat(items2).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 21
            )
        )
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(1)
        assertThat(items).isEqualTo(firstList)
        runBlocking {
            job.cancelAndJoin()
        }
        assertThat(tracker.pageEventFlowCount()).isEqualTo(0)
        assertThat(tracker.pageDataFlowCount()).isEqualTo(0)
    }

    @Test
    fun cachedWithPassiveCollector() = testScope.runTest {
        val flow = buildPageFlow().cachedIn(backgroundScope, tracker)
        val passive = ItemCollector(flow)
        passive.collectPassivelyIn(backgroundScope)
        testScope.runCurrent()
        // collecting on the paged source will trigger initial page
        assertThat(passive.items()).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 3
            )
        )
        val firstList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 9
        )
        // another collector is causing more items to be loaded, they should be reflected in the
        // passive one
        assertThat(flow.collectItemsUntilSize(9)).isEqualTo(firstList)
        assertThat(passive.items()).isEqualTo(firstList)
        val passive2 = ItemCollector(flow)
        passive2.collectPassivelyIn(backgroundScope)
        testScope.runCurrent()
        // a new passive one should receive all existing items immediately
        assertThat(passive2.items()).isEqualTo(firstList)

        // now we get another collector that'll fetch more pages, it should reflect in passives
        val secondList = buildItems(
            version = 0,
            generation = 0,
            start = 0,
            size = 12
        )
        // another collector is causing more items to be loaded, they should be reflected in the
        // passive one
        assertThat(flow.collectItemsUntilSize(12)).isEqualTo(secondList)
        assertThat(passive.items()).isEqualTo(secondList)
        assertThat(passive2.items()).isEqualTo(secondList)
    }

    /**
     * Test that, when cache is active but there is no active downstream collectors, intermediate
     * invalidations create new PagingData BUT a new collector only sees the latest one.
     */
    @Test
    public fun unusedPagingDataIsNeverCollectedByNewDownstream(): Unit = testScope.runTest {
        val factory = StringPagingSource.VersionedFactory()
        val flow = buildPageFlow(factory).cachedIn(backgroundScope, tracker)
        val collector = ItemCollector(flow)
        val job = SupervisorJob()
        val subScope = CoroutineScope(coroutineContext + job)
        collector.collectPassivelyIn(subScope)
        testScope.runCurrent()
        assertThat(collector.items()).isEqualTo(
            buildItems(
                version = 0,
                generation = 0,
                start = 0,
                size = 3
            )
        )
        // finish that collector
        job.cancelAndJoin()
        assertThat(factory.nextVersion).isEqualTo(1)
        repeat(10) {
            factory.invalidateLatest()
            testScope.runCurrent()
        }
        runCurrent()
        // next version is 11, the last paged data we've created has version 10
        assertThat(factory.nextVersion).isEqualTo(11)

        // create another collector from shared, should only receive 1 paging data and that
        // should be the latest because previous PagingData is invalidated
        val collector2 = ItemCollector(flow)
        collector2.collectPassivelyIn(backgroundScope)
        testScope.runCurrent()
        assertThat(collector2.items()).isEqualTo(
            buildItems(
                version = 10,
                generation = 0,
                start = 0,
                size = 3
            )
        )
        assertThat(collector2.receivedPagingDataCount).isEqualTo(1)
        testScope.runCurrent()
        assertThat(factory.nextVersion).isEqualTo(11)
        val activeCollection = flow.collectItemsUntilSize(9)
        assertThat(activeCollection).isEqualTo(
            buildItems(
                version = 10,
                generation = 0,
                start = 0,
                size = 9
            )
        )
        testScope.runCurrent()
        // make sure passive collector received those items as well
        assertThat(collector2.items()).isEqualTo(
            buildItems(
                version = 10,
                generation = 0,
                start = 0,
                size = 9
            )
        )
    }

    private fun buildPageFlow(
        factory: StringPagingSource.VersionedFactory = StringPagingSource.VersionedFactory()
    ): Flow<PagingData<Item>> {
        return Pager(
            pagingSourceFactory = factory::create,
            config = PagingConfig(
                pageSize = 3,
                prefetchDistance = 1,
                enablePlaceholders = false,
                initialLoadSize = 3,
                maxSize = 1000
            )
        ).flow
    }

    /**
     * Used for assertions internally to ensure we don't get some data with wrong generation
     * during collection. This shouldn't happen but happened during development so it is best to
     * add assertions for it.
     */
    private val PagingData<Item>.version
        get(): Int {
            return (
                (hintReceiver as PageFetcher<*, *>.PagerHintReceiver<*, *>)
                    .pageFetcherSnapshot.pagingSource as StringPagingSource
                ).version
        }

    private suspend fun Flow<PagingData<Item>>.collectItemsUntilSize(
        expectedSize: Int,
    ): List<Item> {
        return this
            .mapLatest { pagingData ->
                val expectedVersion = pagingData.version
                val items = mutableListOf<Item>()
                yield() // this yield helps w/ cancellation wrt mapLatest
                val receiver = pagingData.hintReceiver
                var loadedPageCount = 0
                pagingData.flow.filterIsInstance<PageEvent.Insert<Item>>()
                    .onEach {
                        items.addAll(
                            it.pages.flatMap {
                                assertThat(
                                    it.data.map { it.pagingSourceId }.toSet()
                                ).containsExactly(
                                    expectedVersion
                                )
                                it.data
                            }
                        )
                        loadedPageCount += it.pages.size
                        if (items.size < expectedSize) {
                            receiver.accessHint(
                                ViewportHint.Access(
                                    pageOffset = loadedPageCount - 1,
                                    indexInPage = it.pages.last().data.size - 1,
                                    presentedItemsBefore = it.pages.sumOf { it.data.size } - 1,
                                    presentedItemsAfter = 0,
                                    originalPageOffsetFirst =
                                        it.pages.first().originalPageOffsets.minOrNull()!!,
                                    originalPageOffsetLast =
                                        it.pages.last().originalPageOffsets.maxOrNull()!!
                                )
                            )
                        } else {
                            throw AbortCollectionException()
                        }
                    }.catch { ex ->
                        if (ex !is AbortCollectionException) {
                            throw ex
                        }
                    }
                    .toList()
                items
            }.first()
    }

    /**
     * Paged list collector that does not call any hints but always collects
     */
    private class ItemCollector(
        val source: Flow<PagingData<Item>>
    ) {
        private var items: List<Item> = emptyList()
        private var job: Job? = null
        var receivedPagingDataCount = 0
            private set

        /**
         * Collect w/o calling any UI hints so it more like observing the stream w/o affecting it.
         */
        fun collectPassivelyIn(scope: CoroutineScope) {
            check(job == null) {
                "don't call collect twice"
            }
            job = scope.launch {
                collectPassively()
            }
        }

        private suspend fun collectPassively() {
            source.collect {
                receivedPagingDataCount++
                // clear to latest
                val list = mutableListOf<Item>()
                items = list
                it.flow.filterIsInstance<PageEvent.Insert<Item>>().collect {
                    it.pages.forEach {
                        list.addAll(it.data)
                    }
                }
            }
        }

        fun items() = items.toList()
    }

    private class StringPagingSource(
        val version: Int
    ) : PagingSource<Int, Item>() {
        private var generation = -1

        override val keyReuseSupported: Boolean
            get() = true

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
            when (params) {
                is LoadParams.Refresh -> {
                    generation++
                    return doLoad(
                        position = params.key ?: 0,
                        size = params.loadSize
                    )
                }
                is LoadParams.Prepend -> {
                    val loadSize = minOf(params.key, params.loadSize)
                    return doLoad(
                        position = params.key - params.loadSize,
                        size = loadSize
                    )
                }
                is LoadParams.Append -> {
                    return doLoad(
                        position = params.key,
                        size = params.loadSize
                    )
                }
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Item>): Int? = null

        private fun doLoad(
            position: Int,
            size: Int
        ): LoadResult<Int, Item> {
            return LoadResult.Page(
                data = buildItems(
                    version = version,
                    generation = generation,
                    start = position,
                    size = size
                ),
                prevKey = if (position == 0) null else position,
                nextKey = position + size
            )
        }

        class VersionedFactory {
            var nextVersion = 0
                private set
            private var latestSource: StringPagingSource? = null
            fun create() = StringPagingSource(nextVersion++).also {
                latestSource = it
            }
            fun invalidateLatest() = latestSource?.invalidate()
        }
    }

    companion object {
        private fun buildItems(
            version: Int,
            generation: Int,
            start: Int,
            size: Int,
            modifier: ((Item) -> Item)? = null
        ): List<Item> {
            return (start until start + size).map { id ->
                Item(
                    pagingSourceId = version,
                    generation = generation,
                    value = id
                ).let {
                    modifier?.invoke(it) ?: it
                }
            }
        }
    }

    private data class Item(
        /**
         * which paged source generated this item
         */
        val pagingSourceId: Int,
        /**
         * # of refresh counts in the paged source
         */
        val generation: Int,
        /**
         * Item unique identifier
         */
        val value: Int,

        /**
         * Any additional data by transformations etc
         */
        val metadata: String? = null
    )

    private class ActiveFlowTrackerImpl : ActiveFlowTracker {
        private val counters = mapOf(
            PAGED_DATA_FLOW to AtomicInteger(0),
            PAGE_EVENT_FLOW to AtomicInteger(0)
        )

        override fun onNewCachedEventFlow(cachedPageEventFlow: CachedPageEventFlow<*>) {
        }

        override suspend fun onStart(flowType: FlowType) {
            (counters[flowType] ?: error("invalid type $flowType")).incrementAndGet()
        }

        override suspend fun onComplete(flowType: FlowType) {
            (counters[flowType] ?: error("invalid type $flowType")).decrementAndGet()
        }

        fun pageDataFlowCount() = (counters[PAGED_DATA_FLOW] ?: error("unexpected")).get()
        fun pageEventFlowCount() = (counters[PAGE_EVENT_FLOW] ?: error("unexpected")).get()
    }

    private class AbortCollectionException : Throwable()
}