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
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

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
        fun create(): PagingSource<Int, String> {
            return MockPagingSource()
        }

        var throwable: Throwable? = null

        fun enqueueError() {
            throwable = EXCEPTION
        }

        private inner class MockPagingSource : PagingSource<Int, String>() {
            override suspend fun load(params: LoadParams<Int>) = when (params) {
                is LoadParams.Refresh -> loadInitial(params)
                else -> loadRange()
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

    companion object {
        val EXCEPTION = Exception("")
    }
}
