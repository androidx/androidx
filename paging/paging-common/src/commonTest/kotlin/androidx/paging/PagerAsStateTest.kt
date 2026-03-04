/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.paging.PagingSource.LoadResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PagerAsStateTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)

    private val pagingSourceFactory = { TestPagingSource() }

    private val config =
        PagingConfig(
            pageSize = 2,
            prefetchDistance = 1,
            enablePlaceholders = false,
            initialLoadSize = 5,
        )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialRefresh() {
        val pager = Pager(config, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            val list = result.loadedLists.firstOrNull()
            assertThat(list).isNotNull()
            assertThat(list!!.items).containsExactly(0, 1, 2, 3, 4).inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun append() {
        val pager = Pager(config, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.first().items).containsExactly(0, 1, 2, 3, 4).inOrder()

            // first append
            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(0, 1, 2, 3, 4, 5, 6)
                .inOrder()

            // second append
            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            assertThat(result.loadedLists.last().items)
                .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun prepend() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            // first prepend
            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54)
                .inOrder()

            // second prepend
            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            assertThat(result.loadedLists.last().items)
                .containsExactly(46, 47, 48, 49, 50, 51, 52, 53, 54)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun appendPrepend() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            pager.append()
            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun prependAppend() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)

            advanceUntilIdle()

            val list = result.loadedLists.firstOrNull()
            assertThat(list).isNotNull()
            assertThat(list!!.items).containsExactly(50, 51, 52, 53, 54).inOrder()

            pager.prepend()
            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun refreshAll_withoutPlaceholders() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            pager.refresh()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun refreshAll_withPlaceholders() {
        val pager =
            Pager(
                PagingConfig(
                    pageSize = 2,
                    prefetchDistance = 1,
                    enablePlaceholders = true,
                    initialLoadSize = 5,
                ),
                initialKey = 50,
                pagingSourceFactory = pagingSourceFactory,
            )

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            pager.refresh()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun refreshAllAfterLoad() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()

            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            pager.refresh()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            val list2 = result.loadedLists.lastOrNull()
            assertThat(list2!!.items).containsExactly(50, 51, 52, 53, 54, 55, 56).inOrder()

            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(4)
            // the loadKey used here is 49 due to the way TestPagingSource calculates prevKey
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            pager.refresh()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(5)
            // since the Prepend's loadKey was 49, that became the refresh key here
            assertThat(result.loadedLists.last().items)
                .containsExactly(49, 50, 51, 52, 53, 54, 55, 56, 57)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun refreshAroundItem() {
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()

            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            pager.refresh(55)
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(3)
            assertThat(result.loadedLists.last().items)
                .containsExactly(55, 56, 57, 58, 59)
                .inOrder()

            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(4)
            assertThat(result.loadedLists.last().items)
                .containsExactly(53, 54, 55, 56, 57, 58, 59)
                .inOrder()

            pager.refresh(54)
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(5)
            assertThat(result.loadedLists.last().items)
                .containsExactly(54, 55, 56, 57, 58)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun dropFromTheFront() {
        val pager =
            Pager(
                PagingConfig(
                    pageSize = 2,
                    prefetchDistance = 1,
                    enablePlaceholders = false,
                    initialLoadSize = 5,
                    maxSize = 7,
                ),
                initialKey = 50,
                pagingSourceFactory = pagingSourceFactory,
            )

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            // prepend some pages to be dropped later
            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54)
                .inOrder()

            // the appends would exceed the max size so we should see continuous drops as we keep
            // appending
            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items).containsExactly(55, 56, 57, 58).inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun dropFromTheBack() {
        val pager =
            Pager(
                PagingConfig(
                    pageSize = 2,
                    prefetchDistance = 1,
                    enablePlaceholders = false,
                    initialLoadSize = 5,
                    maxSize = 7,
                ),
                initialKey = 50,
                pagingSourceFactory = pagingSourceFactory,
            )

        testScope.runTest {
            val result = collectOnPager(pager.flow)
            advanceUntilIdle()
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            // append some pages to be dropped later
            pager.append()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54, 55, 56)
                .inOrder()

            // the prepends would exceed the max size so we should see continuous drops as we keep
            // prepending
            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54)
                .inOrder()

            pager.prepend()
            advanceUntilIdle()
            assertThat(result.loadedLists.last().items).containsExactly(46, 47, 48, 49).inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun stateIn() {
        testScope.runTest {
            val pager =
                Pager(config, pagingSourceFactory = pagingSourceFactory)
                    .flow
                    .asState()
                    .stateIn(backgroundScope)

            advanceUntilIdle()
            assertThat(pager.value.items).containsExactly(0, 1, 2, 3, 4).inOrder()
        }
    }

    @Test
    fun insertSeparator() {
        testScope.runTest {
            val pager =
                Pager(config, pagingSourceFactory = pagingSourceFactory).flow.map { pagingData ->
                    pagingData.insertSeparators { _, next ->
                        if (next != null && next % 2 == 0) "sep" else null
                    }
                }

            val result = collectOnPager(pager)
            advanceUntilIdle()

            val list = result.loadedLists.firstOrNull()
            assertThat(list).isNotNull()
            assertThat(list!!.items).containsExactly("sep", 0, 1, "sep", 2, 3, "sep", 4).inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun onLoadErrorIsCalled() {
        val pagingSources = mutableListOf<TestPagingSource>()
        val pagingSourceFactory = {
            TestPagingSource(loadDelay = 500).also { pagingSources.add(it) }
        }
        val pager = Pager(config, pagingSourceFactory = pagingSourceFactory)
        var loadError = false

        testScope.runTest {
            val result = collectOnPager(pager.flow) { loadError = true }
            advanceTimeBy(100)

            // make refresh return load error
            pagingSources.first().errorNextLoad = true
            advanceUntilIdle()
            assertThat(result.loadedLists).isEmpty()
            assertThat(loadError).isTrue()

            result.job.cancel()
        }
    }

    @Test
    fun onLoadError_retryRefresh() {
        val pagingSources = mutableListOf<TestPagingSource>()
        val pagingSourceFactory = {
            TestPagingSource(loadDelay = 500).also { pagingSources.add(it) }
        }
        val pager = Pager(config, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow) { pager.retry() }
            advanceTimeBy(100)

            // make refresh return load error
            pagingSources.first().errorNextLoad = true
            // let this refresh complete but not the retry refresh
            advanceTimeBy(600)
            assertThat(pagingSources.size).isEqualTo(1)
            assertThat(result.loadedLists).isEmpty()

            // now let retry complete
            advanceUntilIdle()
            assertThat(pagingSources.size).isEqualTo(1) // retry reuses same generation
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items).containsExactly(0, 1, 2, 3, 4).inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun onLoadError_retryAppend() {
        val pagingSources = mutableListOf<TestPagingSource>()
        val pagingSourceFactory = {
            TestPagingSource(loadDelay = 500).also { pagingSources.add(it) }
        }
        val pager = Pager(config, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow) { pager.retry() }
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items).containsExactly(0, 1, 2, 3, 4).inOrder()

            // make append return load error
            pagingSources.first().errorNextLoad = true
            pager.append()
            // let this append complete but not the retry append
            advanceTimeBy(600)
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items).containsExactly(0, 1, 2, 3, 4).inOrder()

            // now let retry complete
            advanceUntilIdle()
            assertThat(pagingSources.size).isEqualTo(1) // retry reuses same generation
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(0, 1, 2, 3, 4, 5, 6)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun onLoadError_retryPrepend() {
        val pagingSources = mutableListOf<TestPagingSource>()
        val pagingSourceFactory = {
            TestPagingSource(loadDelay = 500).also { pagingSources.add(it) }
        }
        val pager = Pager(config, initialKey = 50, pagingSourceFactory = pagingSourceFactory)

        testScope.runTest {
            val result = collectOnPager(pager.flow) { pager.retry() }
            advanceUntilIdle()
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.first().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            // make prepend return load error
            pagingSources.first().errorNextLoad = true
            pager.prepend()
            // let this prepend complete but not the retry prepend
            advanceTimeBy(600)
            assertThat(result.loadedLists.size).isEqualTo(1)
            assertThat(result.loadedLists.last().items)
                .containsExactly(50, 51, 52, 53, 54)
                .inOrder()

            // now let retry complete
            advanceUntilIdle()
            assertThat(pagingSources.size).isEqualTo(1) // retry reuses same generation
            assertThat(result.loadedLists.size).isEqualTo(2)
            assertThat(result.loadedLists.last().items)
                .containsExactly(48, 49, 50, 51, 52, 53, 54)
                .inOrder()

            result.job.cancel()
        }
    }

    @Test
    fun onLoadError_throwException() {
        val exception = Exception()
        val pager =
            Pager(config) {
                TestPagingSource(loadDelay = 500).also {
                    // make refresh return load error
                    it.nextLoadResult = LoadResult.Error(exception)
                }
            }
        var error: IllegalStateException? = null
        testScope.runTest {
            launch {
                pager.flow
                    .asState { combinedLoadStates ->
                        throw IllegalStateException("$combinedLoadStates")
                    }
                    .catch { error = it as? IllegalStateException }
                    .collect {}
            }

            advanceUntilIdle()
            assertThat(error).isNotNull()
            assertThat(error!!.message)
                .isEqualTo(localLoadStatesOf(refreshLocal = LoadState.Error(exception)).toString())
        }
    }
}

private fun <Value : Any> CoroutineScope.collectOnPager(
    flow: Flow<PagingData<Value>>,
    onLoadError: (CombinedLoadStates) -> Unit = {},
): PagerResult<Value> {
    val dataList = arrayListOf<ItemSnapshotList<Value>>()

    val job = launch { flow.asState(onLoadError).collect { dataList.add(it) } }

    return PagerResult(dataList, job)
}

private data class PagerResult<Value : Any>(
    val loadedLists: List<ItemSnapshotList<Value>>,
    val job: Job,
)
