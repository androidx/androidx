/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.paging.Pager
import androidx.paging.PagingState
import androidx.paging.rxjava2.RxPagingSource
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.testutil.ItemStore
import androidx.room.integration.kotlintestapp.testutil.PagingDb
import androidx.room.integration.kotlintestapp.testutil.PagingEntity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.reactivex.Single
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class Rx2PagingSourceTest {
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var db: PagingDb
    private lateinit var itemStore: ItemStore

    private val mainThreadQueries = mutableListOf<Pair<String, String>>()
    private val pagingSources = mutableListOf<RxPagingSourceImpl>()

    @Before
    fun init() {
        coroutineScope = CoroutineScope(Dispatchers.Main)
        itemStore = ItemStore(coroutineScope)

        val mainThread: Thread = runBlocking(Dispatchers.Main) { Thread.currentThread() }
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    PagingDb::class.java
                )
                .setQueryCallback(
                    object : RoomDatabase.QueryCallback {
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            if (Thread.currentThread() === mainThread) {
                                mainThreadQueries.add(sqlQuery to Throwable().stackTraceToString())
                            }
                        }
                    }
                ) {
                    // instantly execute the log callback so that we can check the thread.
                    it.run()
                }
                .build()
    }

    @After
    fun tearDown() {
        // Check no mainThread queries happened.
        assertThat(mainThreadQueries).isEmpty()
        coroutineScope.cancel()
        pagingSources.clear()
    }

    @Test
    fun refresh_canceledCoroutine_disposesSingle() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        var isDisposed = false
        val pager =
            Pager(CONFIG) {
                val baseSource = db.getDao().loadItemsRx2()
                RxPagingSourceImpl(
                        baseSource = baseSource,
                        initialLoadSingle = { params ->
                            baseSource
                                .loadSingle(params)
                                // delay load for refresh so we have time to cancel load
                                .doOnSubscribe { Thread.sleep(500) }
                                .doOnSuccess { assertWithMessage("Should not succeed").fail() }
                                .doOnDispose { isDisposed = true }
                        },
                        nonInitialLoadSingle = { params -> baseSource.loadSingle(params) },
                    )
                    .also { pagingSources.add(it) }
            }

        runTest(pager) { collectionJob ->
            // ensure initial load has started
            assertFalse(collectionJob.start())
            // allow collection to start and return a single
            delay(200)

            // make sure it progresses enough to have created the first single
            assertThat(pagingSources.size).isEqualTo(1)
            val pagingSource = pagingSources.first()
            assertThat(pagingSource.singles.size).isEqualTo(1)

            assertFalse(isDisposed)

            collectionJob.cancelAndJoin() // this should dispose single

            assertTrue(isDisposed)
            assertFalse(pagingSource.invalid) // paging source should still be valid though
        }
    }

    @Test
    fun append_canceledCoroutine_disposesSingle() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        var isDisposed = false
        val pager =
            Pager(CONFIG) {
                val baseSource = db.getDao().loadItemsRx2()
                RxPagingSourceImpl(
                        baseSource = baseSource,
                        initialLoadSingle = { params -> baseSource.loadSingle(params) },
                        nonInitialLoadSingle = { params ->
                            baseSource
                                .loadSingle(params)
                                // delay load for append/prepend so we have time to cancel load
                                .doOnSubscribe { Thread.sleep(500) }
                                .doOnSuccess { assertWithMessage("Should not succeed").fail() }
                                .doOnDispose { isDisposed = true }
                        },
                    )
                    .also { pagingSources.add(it) }
            }

        runTest(pager) { collectionJob ->
            // do initial load first
            assertThat(itemStore.awaitInitialLoad(2))
                .containsExactlyElementsIn(
                    items.createExpected(fromIndex = 0, toIndex = CONFIG.initialLoadSize)
                )

            // trigger an append and give it time to create second single
            itemStore.get(30)
            delay(200)

            // make sure it progresses enough to have created second single
            assertThat(pagingSources.size).isEqualTo(1)
            val pagingSource = pagingSources.first()
            assertThat(pagingSource.singles.size).isEqualTo(2)

            assertFalse(isDisposed)

            collectionJob.cancelAndJoin() // this should now dispose second single

            assertTrue(isDisposed)
            assertFalse(pagingSource.invalid) // paging source should still be valid though
        }
    }

    @Test
    fun prepend_canceledCoroutine_disposesSingle() {
        val items = createItems(startId = 0, count = 90)
        db.getDao().insert(items)

        var isDisposed = false
        val pager =
            Pager(config = CONFIG, initialKey = 50) {
                val baseSource = db.getDao().loadItemsRx2()
                RxPagingSourceImpl(
                        baseSource = baseSource,
                        initialLoadSingle = { params -> baseSource.loadSingle(params) },
                        nonInitialLoadSingle = { params ->
                            baseSource
                                .loadSingle(params)
                                // delay load for append/prepend so we have time to cancel load
                                .doOnSubscribe { Thread.sleep(500) }
                                .doOnSuccess { assertWithMessage("Should not succeed").fail() }
                                .doOnDispose { isDisposed = true }
                        },
                    )
                    .also { pagingSources.add(it) }
            }

        runTest(pager) { collectionJob ->
            // do initial load first
            assertThat(itemStore.awaitInitialLoad(2))
                .containsExactlyElementsIn(
                    items.createExpected(fromIndex = 50, toIndex = 50 + CONFIG.initialLoadSize)
                )

            // trigger a prepend and give it time to create second single
            itemStore.get(30)
            delay(200)

            // make sure it progresses enough to have created second single
            assertThat(pagingSources.size).isEqualTo(1)
            val pagingSource = pagingSources.first()
            assertThat(pagingSource.singles.size).isEqualTo(2)

            assertFalse(isDisposed)

            collectionJob.cancelAndJoin() // this should now dispose second single

            assertTrue(isDisposed)
            assertFalse(pagingSource.invalid) // paging source should still be valid though
        }
    }

    private fun runTest(pager: Pager<Int, PagingEntity>, block: suspend (Job) -> Unit) {
        val collection =
            coroutineScope.launch(Dispatchers.Main) {
                pager.flow.collectLatest { itemStore.collectFrom(it) }
            }
        runBlocking { block(collection) }
    }

    private class RxPagingSourceImpl(
        private val baseSource: RxPagingSource<Int, PagingEntity>,
        private val initialLoadSingle: (LoadParams<Int>) -> Single<LoadResult<Int, PagingEntity>>,
        private val nonInitialLoadSingle:
            (LoadParams<Int>) -> Single<LoadResult<Int, PagingEntity>>,
    ) : RxPagingSource<Int, PagingEntity>() {

        val singles = mutableListOf<Single<LoadResult<Int, PagingEntity>>>()

        override fun getRefreshKey(state: PagingState<Int, PagingEntity>): Int? {
            return baseSource.getRefreshKey(state)
        }

        override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, PagingEntity>> {
            return if (singles.isEmpty()) {
                    initialLoadSingle(params)
                } else {
                    nonInitialLoadSingle(params)
                }
                .also { singles.add(it) }
        }
    }
}
