/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.REFRESH
import androidx.testutils.DirectDispatcher
import androidx.testutils.TestDispatcher
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.DelicateCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
@RunWith(JUnit4::class)
class RxPagedListBuilderTest {
    private data class LoadStateEvent(
        val type: LoadType,
        val state: LoadState
    )

    /**
     * Creates a data source that will sequentially supply the passed lists
     */
    private fun testDataSourceSequence(data: List<List<String>>): DataSource.Factory<Int, String> {
        return object : DataSource.Factory<Int, String>() {
            var localData = data
            override fun create(): DataSource<Int, String> {
                val currentList = localData.first()
                localData = localData.drop(1)
                return TestPositionalDataSource(currentList)
            }
        }
    }

    class MockDataSourceFactory {
        fun create(
            loadDispatcher: CoroutineDispatcher = DirectDispatcher
        ): PagingSource<Int, String> {
            return MockPagingSource(loadDispatcher)
        }

        var throwable: Throwable? = null

        fun enqueueError() {
            throwable = EXCEPTION
        }

        inner class MockPagingSource(
            // Allow explicit control of load calls outside of fetch / notify. Note: This is
            // different from simply setting fetchDispatcher because PagingObservableOnSubscribe
            // init happens on fetchDispatcher which makes it difficult to differentiate
            // InitialPagedList.
            val loadDispatcher: CoroutineDispatcher
        ) : PagingSource<Int, String>() {
            var invalidInitialLoad = false

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
                return withContext(loadDispatcher) {
                    if (invalidInitialLoad) {
                        invalidInitialLoad = false
                        LoadResult.Invalid()
                    } else when (params) {
                        is LoadParams.Refresh -> loadInitial(params)
                        else -> loadRange()
                    }
                }
            }

            override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

            private fun loadInitial(params: LoadParams<Int>): LoadResult<Int, String> {
                if (params is LoadParams.Refresh) {
                    assertEquals(6, params.loadSize)
                } else {
                    assertEquals(2, params.loadSize)
                }

                throwable?.let { error ->
                    throwable = null
                    return LoadResult.Error(error)
                }

                val data = listOf("a", "b")
                return LoadResult.Page(
                    data = data,
                    prevKey = null,
                    nextKey = data.size,
                    itemsBefore = 0,
                    itemsAfter = 4 - data.size
                )
            }

            private fun loadRange(): LoadResult<Int, String> {
                return LoadResult.Page(listOf("c", "d"), 0, 0, 0, 0)
            }
        }
    }

    @Test
    fun basic() {
        val factory = testDataSourceSequence(
            listOf(
                listOf("a", "b"),
                listOf("c", "d")
            )
        )
        val scheduler = TestScheduler()

        val observable = RxPagedListBuilder(factory, 10)
            .setFetchScheduler(scheduler)
            .setNotifyScheduler(scheduler)
            .buildObservable()

        val observer = TestObserver<PagedList<String>>()

        observable.subscribe(observer)

        // initial state
        observer.assertNotComplete()
        observer.assertValueCount(0)

        // load first item
        scheduler.triggerActions()
        observer.assertValueCount(2)
        assertEquals(listOf<String>(), observer.values().first())
        assertEquals(listOf("a", "b"), observer.values().last())

        // invalidate triggers second load
        observer.values().last().dataSource.invalidate()
        scheduler.triggerActions()
        observer.assertValueCount(3)
        assertTrue { observer.values()[1].pagingSource.invalid }
        assertEquals(listOf("c", "d"), observer.values().last())
    }

    @Test
    fun checkSchedulers() {
        val factory = testDataSourceSequence(listOf(listOf("a", "b"), listOf("c", "d")))
        val notifyScheduler = TestScheduler()
        val fetchScheduler = TestScheduler()

        val observable: Observable<PagedList<String>> = RxPagedListBuilder(factory, 10)
            .setFetchScheduler(fetchScheduler)
            .setNotifyScheduler(notifyScheduler)
            .buildObservable()

        val observer = TestObserver<PagedList<String>>()
        observable.subscribe(observer)

        // notify has nothing to do
        notifyScheduler.triggerActions()
        observer.assertValueCount(0)

        // fetch creates list, but observer doesn't see
        fetchScheduler.triggerActions()
        observer.assertValueCount(0)

        // now notify reveals item
        notifyScheduler.triggerActions()
        observer.assertValueCount(1)
    }

    @Test
    fun failedLoad() {
        val factory = MockDataSourceFactory()
        factory.enqueueError()

        // NOTE: we use two test schedulers here to inspect state during different stages of the
        // initial load - if we used one, we wouldn't be able to see the initial Loading state
        val notifyScheduler = TestScheduler()
        val fetchScheduler = TestScheduler()

        val observable = RxPagedListBuilder(factory::create, 2)
            .setFetchScheduler(fetchScheduler)
            .setNotifyScheduler(notifyScheduler)
            .buildObservable()

        val observer = TestObserver<PagedList<String>>()
        observable.subscribe(observer)

        factory.enqueueError()

        fetchScheduler.triggerActions()
        notifyScheduler.triggerActions()
        observer.assertValueCount(1)

        val initPagedList = observer.values()[0]!!
        assertTrue(initPagedList is InitialPagedList<*, *>)

        val loadStates = mutableListOf<LoadStateEvent>()

        // initial load failed, check that we're in error state
        val loadStateChangedCallback = { type: LoadType, state: LoadState ->
            if (type == REFRESH) {
                loadStates.add(LoadStateEvent(type, state))
            }
        }
        initPagedList.addWeakLoadStateListener(loadStateChangedCallback)
        assertEquals(
            listOf(
                LoadStateEvent(REFRESH, Loading)
            ),
            loadStates
        )

        fetchScheduler.triggerActions()
        notifyScheduler.triggerActions()
        observer.assertValueCount(1)

        assertEquals(
            listOf(
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION))
            ),
            loadStates
        )

        initPagedList.retry()
        fetchScheduler.triggerActions()
        notifyScheduler.triggerActions()

        assertEquals(
            listOf(
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION)),
                LoadStateEvent(REFRESH, Loading)
            ),
            loadStates
        )
        // flush loadInitial, should succeed now
        fetchScheduler.triggerActions()
        notifyScheduler.triggerActions()
        observer.assertValueCount(2)

        val newPagedList = observer.values().last()
        initPagedList.removeWeakLoadStateListener(loadStateChangedCallback)
        newPagedList.addWeakLoadStateListener(loadStateChangedCallback)

        assertEquals(listOf("a", "b", null, null), observer.values().last())

        assertEquals(
            listOf(
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(REFRESH, Error(EXCEPTION)),
                LoadStateEvent(REFRESH, Loading),
                LoadStateEvent(
                    REFRESH,
                    NotLoading(endOfPaginationReached = false)
                )
            ),
            loadStates
        )
    }

    @Test
    fun observablePagedList_invalidInitialResult() {
        // this TestDispatcher is used to queue up pagingSource.load(). This allows us to control
        // and assert against each load() attempt outside of fetch/notify dispatcher
        val loadDispatcher = TestDispatcher()
        val pagingSources = mutableListOf<MockDataSourceFactory.MockPagingSource>()
        val factory = {
            MockDataSourceFactory().create(loadDispatcher).also {
                val source = it as MockDataSourceFactory.MockPagingSource
                if (pagingSources.size == 0) source.invalidInitialLoad = true
                pagingSources.add(source)
            }
        }
        // this is essentially a direct scheduler so jobs are run immediately
        val scheduler = Schedulers.from(DirectDispatcher.asExecutor())
        val observable = RxPagedListBuilder(factory, 2)
            .setFetchScheduler(scheduler)
            .setNotifyScheduler(scheduler)
            .buildObservable()

        val observer = TestObserver<PagedList<String>>()
        // subscribe triggers the PagingObservableOnSubscribe's invalidate() to create first
        // pagingSource
        observable.subscribe(observer)

        // ensure the InitialPagedList with empty data is observed
        observer.assertValueCount(1)
        val initPagedList = observer.values()[0]!!
        assertThat(initPagedList).isInstanceOf(InitialPagedList::class.java)
        assertThat(initPagedList).isEmpty()
        // ensure first pagingSource is also created at this point
        assertThat(pagingSources.size).isEqualTo(1)

        val loadStates = mutableListOf<LoadStateEvent>()
        val loadStateChangedCallback = { type: LoadType, state: LoadState ->
            if (type == REFRESH) {
                loadStates.add(LoadStateEvent(type, state))
            }
        }

        initPagedList.addWeakLoadStateListener(loadStateChangedCallback)

        assertThat(loadStates).containsExactly(
            // before first load() is called, REFRESH is set to loading, represents load
            // attempt on first pagingSource
            LoadStateEvent(REFRESH, Loading)
        )

        // execute first load, represents load attempt on first paging source
        //
        // using poll().run() instead of executeAll(), because executeAll() + immediate schedulers
        // result in first load + subsequent loads executing immediately and we won't be able to
        // assert the pagedLists/loads incrementally
        loadDispatcher.queue.poll()?.run()

        // the load failed so there should still be only one PagedList, but the first
        // pagingSource should invalidated, and the second pagingSource is created
        observer.assertValueCount(1)
        assertTrue(pagingSources[0].invalid)
        assertThat(pagingSources.size).isEqualTo(2)

        assertThat(loadStates).containsExactly(
            // the first load attempt
            LoadStateEvent(REFRESH, Loading),
            // LoadResult.Invalid resets RERFRESH state
            LoadStateEvent(
                REFRESH,
                NotLoading(endOfPaginationReached = false)
            ),
            // before second load() is called, REFRESH is set to loading, represents load
            // attempt on second pagingSource
            LoadStateEvent(REFRESH, Loading),
        )

        // execute the load attempt on second pagingSource which succeeds
        loadDispatcher.queue.poll()?.run()

        // ensure second pagedList created with the correct data loaded
        observer.assertValueCount(2)
        val secondPagedList = observer.values()[1]
        assertThat(secondPagedList).containsExactly("a", "b", null, null)
        assertThat(secondPagedList).isNotInstanceOf(InitialPagedList::class.java)
        assertThat(secondPagedList).isInstanceOf(ContiguousPagedList::class.java)

        secondPagedList.addWeakLoadStateListener(loadStateChangedCallback)
        assertThat(loadStates).containsExactly(
            LoadStateEvent(REFRESH, Loading), // first load
            LoadStateEvent(
                REFRESH,
                NotLoading(endOfPaginationReached = false)
            ), // first load reset
            LoadStateEvent(REFRESH, Loading), // second load
            LoadStateEvent(
                REFRESH,
                NotLoading(endOfPaginationReached = false)
            ), // second load succeeds
        )
    }

    @Test
    fun instantiatesPagingSourceOnFetchDispatcher() {
        var pagingSourcesCreated = 0
        val pagingSourceFactory = {
            pagingSourcesCreated++
            TestPagingSource()
        }
        val notifyScheduler = TestScheduler()
        val fetchScheduler = TestScheduler()
        val rxPagedList = RxPagedListBuilder(
            pagingSourceFactory = pagingSourceFactory,
            pageSize = 10,
        ).apply {
            setNotifyScheduler(notifyScheduler)
            setFetchScheduler(fetchScheduler)
        }.buildObservable()

        fetchScheduler.triggerActions()
        assertEquals(0, pagingSourcesCreated)

        rxPagedList.subscribe { }

        assertEquals(0, pagingSourcesCreated)

        fetchScheduler.triggerActions()
        assertEquals(1, pagingSourcesCreated)
    }

    @Test
    fun initialValueAllowsGetDataSource() {
        val rxPagedList = RxPagedListBuilder(
            pagingSourceFactory = { TestPagingSource(loadDelay = 0) },
            pageSize = 10,
        ).apply {
            setNotifyScheduler(Schedulers.from { it.run() })
            setFetchScheduler(Schedulers.from { it.run() })
        }.buildObservable()

        // Calling .dataSource should never throw from the initial paged list.
        rxPagedList.firstOrError().blockingGet().dataSource
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun invalidPagingSourceOnInitialLoadTriggersInvalidation() {
        var pagingSourcesCreated = 0
        val pagingSourceFactory = {
            when (pagingSourcesCreated++) {
                0 -> TestPagingSource().apply {
                    invalidate()
                }
                else -> TestPagingSource()
            }
        }

        val testScheduler = TestScheduler()
        val rxPagedList = RxPagedListBuilder(
            pageSize = 10,
            pagingSourceFactory = pagingSourceFactory,
        ).apply {
            setNotifyScheduler(testScheduler)
            setFetchScheduler(testScheduler)
        }.buildObservable()

        rxPagedList.subscribe()
        testScheduler.triggerActions()
        assertEquals(2, pagingSourcesCreated)
    }

    companion object {
        val EXCEPTION = Exception("")
    }
}
