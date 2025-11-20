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

@file:Suppress("VisibleForTests")

package androidx.paging.testing

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@IgnoreWebTarget // b/395933428
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PagerFlowSnapshotTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun createFactory(
        dataFlow: Flow<List<Int>>,
        loadDelay: Long,
    ): () -> PagingSource<Int, Int> {
        val factory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        return { TestPagingSource(factory(), loadDelay) }
    }

    private fun createSingleGenFactory(
        data: List<Int>,
        loadDelay: Long,
    ): () -> PagingSource<Int, Int> {
        val factory = data.asPagingSourceFactory()
        return { TestPagingSource(factory(), loadDelay) }
    }

    @Test fun initialRefresh_loadDelay0() = initialRefresh(0)

    @Test fun initialRefresh_loadDelay10000() = initialRefresh(10000)

    private fun initialRefresh(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test fun initialRefreshSingleGen_loadDelay0() = initialRefreshSingleGen(0)

    @Test fun initialRefreshSingleGen_loadDelay10000() = initialRefreshSingleGen(10000)

    private fun initialRefreshSingleGen(loadDelay: Long) {
        val data = List(30) { it }
        val pager = createPager(data, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test fun initialRefresh_emptyOperations_loadDelay0() = initialRefresh_emptyOperations(0)

    @Test
    fun initialRefresh_emptyOperations_loadDelay10000() = initialRefresh_emptyOperations(10000)

    private fun initialRefresh_emptyOperations(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot {}
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test fun initialRefresh_withSeparators_loadDelay0() = initialRefresh_withSeparators(0)

    @Test fun initialRefresh_withSeparators_loadDelay10000() = initialRefresh_withSeparators(10000)

    private fun initialRefresh_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager =
            createPager(dataFlow, loadDelay).map { pagingData ->
                pagingData.insertSeparators { before: Int?, after: Int? ->
                    if (before != null && after != null) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot()
            // loads 8[initial 5 + prefetch 3] items total, including separators
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, "sep", 1, "sep", 2, "sep", 3, "sep", 4))
        }
    }

    @Test fun initialRefresh_withoutPrefetch_loadDelay0() = initialRefresh_withoutPrefetch(0)

    @Test
    fun initialRefresh_withoutPrefetch_loadDelay10000() = initialRefresh_withoutPrefetch(10000)

    private fun initialRefresh_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4))
        }
    }

    @Test fun initialRefresh_withInitialKey_loadDelay0() = initialRefresh_withInitialKey(0)

    @Test fun initialRefresh_withInitialKey_loadDelay10000() = initialRefresh_withInitialKey(10000)

    private fun initialRefresh_withInitialKey(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        }
    }

    @Test
    fun initialRefresh_withInitialKey_withoutPrefetch_loadDelay0() =
        initialRefresh_withInitialKey_withoutPrefetch(0)

    @Test
    fun initialRefresh_withInitialKey_withoutPrefetch_loadDelay10000() =
        initialRefresh_withInitialKey_withoutPrefetch(10000)

    private fun initialRefresh_withInitialKey_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot).containsExactlyElementsIn(listOf(10, 11, 12, 13, 14))
        }
    }

    @Test
    fun initialRefresh_PagingDataFrom_withoutLoadStates() {
        val data = List(10) { it }
        val pager = flowOf(PagingData.from(data))
        testScope.runTest {
            val snapshot = pager.asSnapshot()
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        }
    }

    @Test
    fun initialRefresh_PagingDataFrom_withLoadStates() {
        val data = List(10) { it }
        val pager =
            flowOf(
                PagingData.from(
                    data,
                    LoadStates(
                        refresh = LoadState.NotLoading(true),
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                    ),
                )
            )
        testScope.runTest {
            val snapshot = pager.asSnapshot()
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        }
    }

    @Test fun emptyInitialRefresh_loadDelay0() = emptyInitialRefresh(0)

    @Test fun emptyInitialRefresh_loadDelay10000() = emptyInitialRefresh(10000)

    private fun emptyInitialRefresh(loadDelay: Long) {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot).containsExactlyElementsIn(emptyList<Int>())
        }
    }

    @Test fun emptyInitialRefreshSingleGen_loadDelay0() = emptyInitialRefreshSingleGen(0)

    @Test fun emptyInitialRefreshSingleGen_loadDelay10000() = emptyInitialRefreshSingleGen(10000)

    private fun emptyInitialRefreshSingleGen(loadDelay: Long) {
        val data = emptyList<Int>()
        val pager = createPager(data, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot).containsExactlyElementsIn(emptyList<Int>())
        }
    }

    @Test
    fun emptyInitialRefresh_emptyOperations_loadDelay0() = emptyInitialRefresh_emptyOperations(0)

    @Test
    fun emptyInitialRefresh_emptyOperations_loadDelay10000() =
        emptyInitialRefresh_emptyOperations(10000)

    private fun emptyInitialRefresh_emptyOperations(loadDelay: Long) {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot()

            assertThat(snapshot).containsExactlyElementsIn(emptyList<Int>())
        }
    }

    @Test fun manualRefresh_loadDelay0() = manualRefresh(0)

    @Test fun manualRefresh_loadDelay10000() = manualRefresh(10000)

    private fun manualRefresh(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { refresh() }
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4))
        }
    }

    @Test fun manualRefreshSingleGen_loadDelay0() = manualRefreshSingleGen(0)

    @Test fun manualRefreshSingleGen_loadDelay10000() = manualRefreshSingleGen(10000)

    private fun manualRefreshSingleGen(loadDelay: Long) {
        val data = List(30) { it }
        val pager = createPager(data, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { refresh() }
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test
    fun manualRefreshSingleGen_pagingSourceInvalidated() {
        val data = List(30) { it }
        val sources = mutableListOf<PagingSource<Int, Int>>()
        val factory = data.asPagingSourceFactory()
        val pager =
            Pager(
                    config = PagingConfig(pageSize = 3, initialLoadSize = 5),
                    pagingSourceFactory = { factory().also { sources.add(it) } },
                )
                .flow
        testScope.runTest {
            pager.asSnapshot { refresh() }
            assertThat(sources.first().invalid).isTrue()
        }
    }

    @Test
    fun manualRefresh_PagingDataFrom_withoutLoadStates() {
        val data = List(10) { it }
        val pager = flowOf(PagingData.from(data))
        testScope.runTest {
            val snapshot = pager.asSnapshot { refresh() }
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        }
    }

    @Test
    fun manualRefresh_PagingDataFrom_withLoadStates() {
        val data = List(10) { it }
        val pager =
            flowOf(
                PagingData.from(
                    data,
                    LoadStates(
                        refresh = LoadState.NotLoading(true),
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                    ),
                )
            )
        testScope.runTest {
            val snapshot = pager.asSnapshot { refresh() }
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
        }
    }

    @Test fun manualEmptyRefresh_loadDelay0() = manualEmptyRefresh(0)

    @Test fun manualEmptyRefresh_loadDelay10000() = manualEmptyRefresh(10000)

    private fun manualEmptyRefresh(loadDelay: Long) {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { refresh() }
            assertThat(snapshot).containsExactlyElementsIn(emptyList<Int>())
        }
    }

    @Test fun appendWhile_loadDelay0() = appendWhile(0)

    @Test fun appendWhile_loadDelay10000() = appendWhile(10000)

    private fun appendWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item < 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [0-4]
                    // prefetched [5-7]
                    // appended [8-16]
                    // prefetched [17-19]
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
                )
        }
    }

    @Test fun appendWhile_withDrops_loadDelay0() = appendWhile_withDrops(0)

    @Test fun appendWhile_withDrops_loadDelay10000() = appendWhile_withDrops(10000)

    private fun appendWhile_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item -> item < 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // dropped [0-10]
                    listOf(11, 12, 13, 14, 15, 16, 17, 18, 19)
                )
        }
    }

    @Test fun appendWhile_withSeparators_loadDelay0() = appendWhile_withSeparators(0)

    @Test fun appendWhile_withSeparators_loadDelay10000() = appendWhile_withSeparators(10000)

    private fun appendWhile_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager =
            createPager(dataFlow, loadDelay).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 9 || before == 12) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot =
                pager.asSnapshot { appendScrollWhile { item -> item !is Int || item < 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [0-4]
                    // prefetched [5-7]
                    // appended [8-16]
                    // prefetched [17-19]
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        "sep",
                        10,
                        11,
                        12,
                        "sep",
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                    )
                )
        }
    }

    @Test fun appendWhile_withoutPrefetch_loadDelay0() = appendWhile_withoutPrefetch(0)

    @Test fun appendWhile_withoutPrefetch_loadDelay10000() = appendWhile_withoutPrefetch(10000)

    private fun appendWhile_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item < 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [0-4]
                    // appended [5-16]
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )
        }
    }

    @Test fun appendWhile_withoutPlaceholders_loadDelay0() = appendWhile_withoutPlaceholders(0)

    @Test
    fun appendWhile_withoutPlaceholders_loadDelay10000() = appendWhile_withoutPlaceholders(10000)

    private fun appendWhile_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPlaceholders(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item != 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [0-4]
                    // prefetched [5-7]
                    // appended [8-16]
                    // prefetched [17-19]
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
                )
        }
    }

    @Test fun prependWhile_loadDelay0() = prependWhile(0)

    @Test fun prependWhile_loadDelay10000() = prependWhile(10000)

    private fun prependWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot { prependScrollWhile { item: Int -> item > 14 } }
            // initial load [20-24]
            // prefetched [17-19], [25-27]
            // prepended [14-16]
            // prefetched [11-13]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27)
                )
        }
    }

    @Test fun prependWhile_withDrops_loadDelay0() = prependWhile_withDrops(0)

    @Test fun prependWhile_withDrops_loadDelay10000() = prependWhile_withDrops(10000)

    private fun prependWhile_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot { prependScrollWhile { item: Int -> item > 14 } }
            // dropped [20-27]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(11, 12, 13, 14, 15, 16, 17, 18, 19))
        }
    }

    @Test fun prependWhile_withSeparators_loadDelay0() = prependWhile_withSeparators(0)

    @Test fun prependWhile_withSeparators_loadDelay10000() = prependWhile_withSeparators(10000)

    private fun prependWhile_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager =
            createPager(dataFlow, loadDelay, 20).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 14 || before == 18) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot =
                pager.asSnapshot { prependScrollWhile { item -> item !is Int || item > 14 } }
            // initial load [20-24]
            // prefetched [17-19], no append prefetch because separator fulfilled prefetchDistance
            // prepended [14-16]
            // prefetched [11-13]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        11,
                        12,
                        13,
                        14,
                        "sep",
                        15,
                        16,
                        17,
                        18,
                        "sep",
                        19,
                        20,
                        21,
                        22,
                        23,
                        24,
                        25,
                        26,
                        27,
                    )
                )
        }
    }

    @Test fun prependWhile_withoutPrefetch_loadDelay0() = prependWhile_withoutPrefetch(0)

    @Test fun prependWhile_withoutPrefetch_loadDelay10000() = prependWhile_withoutPrefetch(10000)

    private fun prependWhile_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot { prependScrollWhile { item: Int -> item > 14 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [20-24]
                    // prepended [14-19]
                    listOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
                )
        }
    }

    @Test fun prependWhile_withoutPlaceholders_loadDelay0() = prependWhile_withoutPlaceholders(0)

    @Test
    fun prependWhile_withoutPlaceholders_loadDelay10000() = prependWhile_withoutPlaceholders(10000)

    private fun prependWhile_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPlaceholders(dataFlow, loadDelay, 30)
        testScope.runTest {
            val snapshot = pager.asSnapshot { prependScrollWhile { item: Int -> item != 22 } }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [30-34]
                    // prefetched [27-29], [35-37]
                    // prepended [21-26]
                    // prefetched [18-20]
                    listOf(
                        18,
                        19,
                        20,
                        21,
                        22,
                        23,
                        24,
                        25,
                        26,
                        27,
                        28,
                        29,
                        30,
                        31,
                        32,
                        33,
                        34,
                        35,
                        36,
                        37,
                    )
                )
        }
    }

    @Test fun appendWhile_withInitialKey_loadDelay0() = appendWhile_withInitialKey(0)

    @Test fun appendWhile_withInitialKey_loadDelay10000() = appendWhile_withInitialKey(10000)

    private fun appendWhile_withInitialKey(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item < 18 } }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // appended [18-20]
            // prefetched [21-23]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
                )
        }
    }

    @Test
    fun appendWhile_withInitialKey_withoutPlaceholders_loadDelay0() =
        appendWhile_withInitialKey_withoutPlaceholders(0)

    @Test
    fun appendWhile_withInitialKey_withoutPlaceholders_loadDelay10000() =
        appendWhile_withInitialKey_withoutPlaceholders(10000)

    private fun appendWhile_withInitialKey_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPlaceholders(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item != 19 } }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // appended [18-20]
            // prefetched [21-23]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
                )
        }
    }

    @Test
    fun appendWhile_withInitialKey_withoutPrefetch_loadDelay0() =
        appendWhile_withInitialKey_withoutPrefetch(0)

    @Test
    fun appendWhile_withInitialKey_withoutPrefetch_loadDelay10000() =
        appendWhile_withInitialKey_withoutPrefetch(10000)

    private fun appendWhile_withInitialKey_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot { appendScrollWhile { item: Int -> item < 18 } }
            // initial load [10-14]
            // appended [15-20]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
        }
    }

    @Test fun prependWhile_withoutInitialKey_loadDelay0() = prependWhile_withoutInitialKey(0)

    @Test
    fun prependWhile_withoutInitialKey_loadDelay10000() = prependWhile_withoutInitialKey(10000)

    private fun prependWhile_withoutInitialKey(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { prependScrollWhile { item: Int -> item > -3 } }
            // initial load [0-4]
            // prefetched [5-7]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test fun consecutiveAppendWhile_loadDelay0() = consecutiveAppendWhile(0)

    @Test fun consecutiveAppendWhile_loadDelay10000() = consecutiveAppendWhile(10000)

    private fun consecutiveAppendWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot { appendScrollWhile { item: Int -> item < 7 } }

            val snapshot2 = pager.asSnapshot { appendScrollWhile { item: Int -> item < 22 } }

            // includes initial load, 1st page, 2nd page (from prefetch)
            assertThat(snapshot1).containsExactlyElementsIn(List(11) { it })

            // includes extra page from prefetch
            assertThat(snapshot2).containsExactlyElementsIn(List(26) { it })
        }
    }

    @Test fun consecutivePrependWhile_loadDelay0() = consecutivePrependWhile(0)

    @Test fun consecutivePrependWhile_loadDelay10000() = consecutivePrependWhile(10000)

    private fun consecutivePrependWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager =
            createPagerNoPrefetch(dataFlow, loadDelay, 20).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot { prependScrollWhile { item: Int -> item > 17 } }
            assertThat(snapshot1).containsExactlyElementsIn(listOf(17, 18, 19, 20, 21, 22, 23, 24))
            val snapshot2 = pager.asSnapshot { prependScrollWhile { item: Int -> item > 11 } }
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
                )
        }
    }

    @Test
    fun appendWhile_outOfBounds_returnsCurrentlyLoadedItems_loadDelay0() =
        appendWhile_outOfBounds_returnsCurrentlyLoadedItems(0)

    @Test
    fun appendWhile_outOfBounds_returnsCurrentlyLoadedItems_loadDelay10000() =
        appendWhile_outOfBounds_returnsCurrentlyLoadedItems(10000)

    private fun appendWhile_outOfBounds_returnsCurrentlyLoadedItems(loadDelay: Long) {
        val dataFlow = flowOf(List(10) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    appendScrollWhile { item: Int ->
                        // condition scrolls till end of data since we only have 10 items
                        item < 18
                    }
                }

            // returns the items loaded before index becomes out of bounds
            assertThat(snapshot).containsExactlyElementsIn(List(10) { it })
        }
    }

    @Test
    fun prependWhile_outOfBounds_returnsCurrentlyLoadedItems_loadDelay0() =
        prependWhile_outOfBounds_returnsCurrentlyLoadedItems(0)

    @Test
    fun prependWhile_outOfBounds_returnsCurrentlyLoadedItems_loadDelay10000() =
        prependWhile_outOfBounds_returnsCurrentlyLoadedItems(10000)

    private fun prependWhile_outOfBounds_returnsCurrentlyLoadedItems(loadDelay: Long) {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPager(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    prependScrollWhile { item: Int ->
                        // condition scrolls till index = 0
                        item > -3
                    }
                }
            // returns the items loaded before index becomes out of bounds
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // initial load [10-14]
                    // prefetched [7-9], [15-17]
                    // prepended [0-6]
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                )
        }
    }

    @Test fun refreshAndAppendWhile_loadDelay0() = refreshAndAppendWhile(0)

    @Test fun refreshAndAppendWhile_loadDelay10000() = refreshAndAppendWhile(10000)

    private fun refreshAndAppendWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    refresh() // triggers second gen
                    appendScrollWhile { item: Int -> item < 10 }
                }
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
        }
    }

    @Test fun refreshAndPrependWhile_loadDelay0() = refreshAndPrependWhile(0)

    @Test fun refreshAndPrependWhile_loadDelay10000() = refreshAndPrependWhile(10000)

    private fun refreshAndPrependWhile(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay, 20).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // this prependScrollWhile does not cause paging to load more items
                    // but it helps this test register a non-null anchorPosition so the upcoming
                    // refresh doesn't start at index 0
                    prependScrollWhile { item -> item > 20 }
                    // triggers second gen
                    refresh()
                    prependScrollWhile { item: Int -> item > 12 }
                }
            // second gen initial load, anchorPos = 20, refreshKey = 18, loaded
            // initial load [18-22]
            // prefetched [15-17], [23-25]
            // prepended [12-14]
            // prefetched [9-11]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25)
                )
        }
    }

    @Test fun appendWhileAndRefresh_loadDelay0() = appendWhileAndRefresh(0)

    @Test fun appendWhileAndRefresh_loadDelay10000() = appendWhileAndRefresh(10000)

    private fun appendWhileAndRefresh(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    appendScrollWhile { item: Int -> item < 10 }
                    refresh()
                }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // second gen initial load, anchorPos = 10, refreshKey = 8
                    // initial load [8-12]
                    // prefetched [5-7], [13-15]
                    listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
                )
        }
    }

    @Test fun prependWhileAndRefresh_loadDelay0() = prependWhileAndRefresh(0)

    @Test fun prependWhileAndRefresh_loadDelay10000() = prependWhileAndRefresh(10000)

    private fun prependWhileAndRefresh(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, loadDelay, 15).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    prependScrollWhile { item: Int -> item > 8 }
                    refresh()
                }
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    // second gen initial load, anchorPos = 8, refreshKey = 6
                    // initial load [6-10]
                    // prefetched [3-5], [11-13]
                    listOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
                )
        }
    }

    @Test fun consecutiveGenerations_fromFlow_loadDelay0() = consecutiveGenerations_fromFlow(0)

    @Test
    fun consecutiveGenerations_fromFlow_loadDelay10000() = consecutiveGenerations_fromFlow(10000)

    private fun consecutiveGenerations_fromFlow(loadDelay: Long) {
        // wait for 500 + loadDelay between each emission
        val dataFlow = flow {
            emit(emptyList())
            delay(500 + loadDelay)

            emit(List(30) { it })
            delay(500 + loadDelay)

            emit(List(30) { it + 30 })
        }
        val pager = createPagerNoPrefetch(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot()
            assertThat(snapshot1).containsExactlyElementsIn(emptyList<Int>())

            delay(500)

            val snapshot2 = pager.asSnapshot()
            assertThat(snapshot2).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4))

            delay(500)

            val snapshot3 = pager.asSnapshot()
            assertThat(snapshot3).containsExactlyElementsIn(listOf(30, 31, 32, 33, 34))
        }
    }

    @Test
    fun consecutiveGenerations_PagingDataFrom_withoutLoadStates_loadDelay0() =
        consecutiveGenerations_PagingDataFrom_withoutLoadStates(0)

    @Test
    fun consecutiveGenerations_PagingDataFrom_withoutLoadStates_loadDelay10000() =
        consecutiveGenerations_PagingDataFrom_withoutLoadStates(10000)

    private fun consecutiveGenerations_PagingDataFrom_withoutLoadStates(loadDelay: Long) {
        // wait for 500 + loadDelay between each emission
        val pager = flow {
            emit(PagingData.empty())
            delay(500 + loadDelay)

            emit(PagingData.from(List(10) { it }))
            delay(500 + loadDelay)

            emit(PagingData.from(List(10) { it + 30 }))
        }
        testScope.runTest {
            val snapshot1 = pager.asSnapshot()
            assertWithMessage("Only the last generation should be loaded without LoadStates")
                .that(snapshot1)
                .containsExactlyElementsIn(listOf(30, 31, 32, 33, 34, 35, 36, 37, 38, 39))
        }
    }

    @Test
    fun consecutiveGenerations_PagingDataFrom_withLoadStates_loadDelay0() =
        consecutiveGenerations_PagingDataFrom_withLoadStates(0)

    @Test
    fun consecutiveGenerations_PagingDataFrom_withLoadStates_loadDelay10000() =
        consecutiveGenerations_PagingDataFrom_withLoadStates(10000)

    private fun consecutiveGenerations_PagingDataFrom_withLoadStates(loadDelay: Long) {
        // wait for 500 + loadDelay between each emission
        val pager =
            flow {
                    emit(
                        PagingData.empty(
                            LoadStates(
                                refresh = LoadState.NotLoading(true),
                                prepend = LoadState.NotLoading(true),
                                append = LoadState.NotLoading(true),
                            )
                        )
                    )
                    delay(500 + loadDelay)

                    emit(
                        PagingData.from(
                            List(10) { it },
                            LoadStates(
                                refresh = LoadState.NotLoading(true),
                                prepend = LoadState.NotLoading(true),
                                append = LoadState.NotLoading(true),
                            ),
                        )
                    )
                    delay(500 + loadDelay)

                    emit(
                        PagingData.from(
                            List(10) { it + 30 },
                            LoadStates(
                                refresh = LoadState.NotLoading(true),
                                prepend = LoadState.NotLoading(true),
                                append = LoadState.NotLoading(true),
                            ),
                        )
                    )
                }
                .cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot()
            assertThat(snapshot1).containsExactlyElementsIn(emptyList<Int>())

            delay(500 + loadDelay)

            val snapshot2 = pager.asSnapshot()
            assertThat(snapshot2).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

            delay(500 + loadDelay)

            val snapshot3 = pager.asSnapshot()
            assertThat(snapshot3)
                .containsExactlyElementsIn(listOf(30, 31, 32, 33, 34, 35, 36, 37, 38, 39))
        }
    }

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitAfterRefresh_loadDelay0() =
        consecutiveGenerations_fromSharedFlow_emitAfterRefresh(0)

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitAfterRefresh_loadDelay10000() =
        consecutiveGenerations_fromSharedFlow_emitAfterRefresh(10000)

    private fun consecutiveGenerations_fromSharedFlow_emitAfterRefresh(loadDelay: Long) {
        val dataFlow = MutableSharedFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot()
            assertThat(snapshot1).containsExactlyElementsIn(emptyList<Int>())

            val snapshot2 = pager.asSnapshot { dataFlow.emit(List(30) { it }) }
            assertThat(snapshot2).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4))

            val snapshot3 = pager.asSnapshot { dataFlow.emit(List(30) { it + 30 }) }
            assertThat(snapshot3).containsExactlyElementsIn(listOf(30, 31, 32, 33, 34))
        }
    }

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitBeforeRefresh_loadDelay0() =
        consecutiveGenerations_fromSharedFlow_emitBeforeRefresh(0)

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitBeforeRefresh_loadDelay10000() =
        consecutiveGenerations_fromSharedFlow_emitBeforeRefresh(10000)

    private fun consecutiveGenerations_fromSharedFlow_emitBeforeRefresh(loadDelay: Long) {
        val dataFlow = MutableSharedFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            dataFlow.emit(emptyList())
            val snapshot1 = pager.asSnapshot()
            assertThat(snapshot1).containsExactlyElementsIn(emptyList<Int>())

            dataFlow.emit(List(30) { it })
            val snapshot2 = pager.asSnapshot()
            assertThat(snapshot2).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4))

            dataFlow.emit(List(30) { it + 30 })
            val snapshot3 = pager.asSnapshot()
            assertThat(snapshot3).containsExactlyElementsIn(listOf(30, 31, 32, 33, 34))
        }
    }

    @Test fun consecutiveGenerations_nonNullRefreshKey_loadDelay0() = consecutiveGenerations(0)

    @Test fun consecutiveGenerations_loadDelay10000() = consecutiveGenerations(10000)

    private fun consecutiveGenerations(loadDelay: Long) {
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            // wait for refresh + append
            delay((500 + loadDelay) * 2)
            // second gen
            emit(List(20) { it })
        }
        val pager = createPagerNoPrefetch(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 =
                pager.asSnapshot {
                    // we scroll to register a non-null anchorPos
                    appendScrollWhile { item: Int -> item < 5 }
                }
            assertThat(snapshot1).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))

            delay(1000)
            val snapshot2 = pager.asSnapshot()
            // anchorPos = 5, refreshKey = 3
            assertThat(snapshot2).containsExactlyElementsIn(listOf(3, 4, 5, 6, 7))
        }
    }

    @Test
    fun consecutiveGenerations_withInitialKey_loadDelay0() =
        consecutiveGenerations_withInitialKey(0)

    @Test
    fun consecutiveGenerations_withInitialKey_loadDelay10000() =
        consecutiveGenerations_withInitialKey(10000)

    private fun consecutiveGenerations_withInitialKey(loadDelay: Long) {
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            // wait for refresh + append
            delay((500 + loadDelay) * 2)
            // second gen
            emit(List(20) { it })
        }
        val pager =
            createPagerNoPrefetch(dataFlow, loadDelay, 10).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 =
                pager.asSnapshot {
                    // we scroll to register a non-null anchorPos
                    appendScrollWhile { item: Int -> item < 15 }
                }
            assertThat(snapshot1).containsExactlyElementsIn(listOf(10, 11, 12, 13, 14, 15, 16, 17))

            delay(1000)
            val snapshot2 = pager.asSnapshot()
            // anchorPos = 15, refreshKey = 13
            assertThat(snapshot2).containsExactlyElementsIn(listOf(13, 14, 15, 16, 17))
        }
    }

    @Test fun prependScroll_loadDelay0() = prependScroll(0)

    @Test fun prependScroll_loadDelay10000() = prependScroll(10000)

    private fun prependScroll(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun prependScroll_withDrops_loadDelay0() = prependScroll_withDrops(0)

    @Test fun prependScroll_withDrops_loadDelay10000() = prependScroll_withDrops(10000)

    private fun prependScroll_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(42) }
            // dropped [47-57]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(38, 39, 40, 41, 42, 43, 44, 45, 46))
        }
    }

    @Test fun prependScroll_withSeparators_loadDelay0() = prependScroll_withSeparators(0)

    @Test fun prependScroll_withSeparators_loadDelay10000() = prependScroll_withSeparators(10000)

    private fun prependScroll_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPager(dataFlow, loadDelay, 50).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 42 || before == 49) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        "sep",
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        "sep",
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun consecutivePrependScroll_loadDelay0() = consecutivePrependScroll(0)

    @Test fun consecutivePrependScroll_loadDelay10000() = consecutivePrependScroll(10000)

    private fun consecutivePrependScroll(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    scrollTo(42)
                    scrollTo(38)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun consecutivePrependScroll_multiSnapshots_loadDelay0() =
        consecutivePrependScroll_multiSnapshots(0)

    @Test
    fun consecutivePrependScroll_multiSnapshots_loadDelay10000() =
        consecutivePrependScroll_multiSnapshots(10000)

    private fun consecutivePrependScroll_multiSnapshots(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )

            val snapshot2 = pager.asSnapshot { scrollTo(38) }
            // prefetched [35-37]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun prependScroll_indexOutOfBounds_loadDelay0() = prependScroll_indexOutOfBounds(0)

    @Test
    fun prependScroll_indexOutOfBounds_loadDelay10000() = prependScroll_indexOutOfBounds(10000)

    private fun prependScroll_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(-5) }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // scrollTo prepended [0-1]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        }
    }

    @Test fun prependScroll_accessPageBoundary_loadDelay0() = prependScroll_accessPageBoundary(0)

    @Test
    fun prependScroll_accessPageBoundary_loadDelay10000() = prependScroll_accessPageBoundary(10000)

    private fun prependScroll_accessPageBoundary(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(47) }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [50-54]
            // prefetched [47-49], [55-57] - expect only one extra page to be prefetched after this
            // scrollTo prepended [44-46]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )
        }
    }

    @Test fun prependScroll_withoutPrefetch_loadDelay0() = prependScroll_withoutPrefetch(0)

    @Test fun prependScroll_withoutPrefetch_loadDelay10000() = prependScroll_withoutPrefetch(10000)

    private fun prependScroll_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(42) }
            // initial load [50-54]
            // prepended [41-49]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54)
                )
        }
    }

    @Test fun prependScroll_withoutPlaceholders_loadDelay0() = prependScroll_withoutPlaceholders(0)

    @Test
    fun prependScroll_withoutPlaceholders_loadDelay10000() =
        prependScroll_withoutPlaceholders(10000)

    private fun prependScroll_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(0)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBounds_loadDelay0() =
        prependScroll_withoutPlaceholders_indexOutOfBounds(0)

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBounds_loadDelay10000() =
        prependScroll_withoutPlaceholders_indexOutOfBounds(10000)

    private fun prependScroll_withoutPlaceholders_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(-5) }
            // ensure it honors negative indices starting with index[0] = item[47]
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // scrollTo prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay0() =
        prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(0)

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay10000() =
        prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(10000)

    private fun prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(-5) }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // scrollTo prepended [0-1]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        }
    }

    @Test
    fun consecutivePrependScroll_withoutPlaceholders_loadDelay0() =
        consecutivePrependScroll_withoutPlaceholders(0)

    @Test
    fun consecutivePrependScroll_withoutPlaceholders_loadDelay10000() =
        consecutivePrependScroll_withoutPlaceholders(10000)

    private fun consecutivePrependScroll_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(-1)
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(-5)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // first scrollTo prepended [41-46]
            // index[0] is now anchored to [41]
            // second scrollTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        32,
                        33,
                        34,
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun consecutivePrependScroll_withoutPlaceholders_multiSnapshot_loadDelay0() =
        consecutivePrependScroll_withoutPlaceholders_multiSnapshot(0)

    @Test
    fun consecutivePrependScroll_withoutPlaceholders_multiSnapshot_loadDelay10000() =
        consecutivePrependScroll_withoutPlaceholders_multiSnapshot(10000)

    private fun consecutivePrependScroll_withoutPlaceholders_multiSnapshot(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(-1)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // scrollTo prepended [44-46]
            // prefetched [41-43]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )

            val snapshot2 =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(-5)
                }
            // scrollTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        32,
                        33,
                        34,
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_noPrefetchTriggered_loadDelay0() =
        prependScroll_withoutPlaceholders_noPrefetchTriggered(0)

    @Test
    fun prependScroll_withoutPlaceholders_noPrefetchTriggered_loadDelay10000() =
        prependScroll_withoutPlaceholders_noPrefetchTriggered(10000)

    private fun prependScroll_withoutPlaceholders_noPrefetchTriggered(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            Pager<Int, Int>(
                    config =
                        PagingConfig(
                            pageSize = 4,
                            initialLoadSize = 8,
                            enablePlaceholders = false,
                            // a small prefetchDistance to prevent prefetch until we scroll to
                            // boundary
                            prefetchDistance = 1,
                        ),
                    initialKey = 50,
                    pagingSourceFactory = createFactory(dataFlow, loadDelay),
                )
                .flow
                .cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    scrollTo(0)
                }
            // initial load [50-57]
            // no prefetch after initial load because it didn't hit prefetch distance
            // scrollTo prepended [46-49]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57))
        }
    }

    @Test fun appendScroll_loadDelay0() = appendScroll(0)

    @Test fun appendScroll_loadDelay10000() = appendScroll(10000)

    private fun appendScroll(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )
        }
    }

    @Test fun appendScroll_withDrops_loadDelay0() = appendScroll_withDrops(0)

    @Test fun appendScroll_withDrops_loadDelay10000() = appendScroll_withDrops(10000)

    private fun appendScroll_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(12) }
            // dropped [0-7]
            assertThat(snapshot).containsExactlyElementsIn(listOf(8, 9, 10, 11, 12, 13, 14, 15, 16))
        }
    }

    @Test fun appendScroll_withSeparators_loadDelay0() = appendScroll_withSeparators(0)

    @Test fun appendScroll_withSeparators_loadDelay10000() = appendScroll_withSeparators(10000)

    private fun appendScroll_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPager(dataFlow, loadDelay).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 0 || before == 14) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, "sep", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, "sep", 15, 16)
                )
        }
    }

    @Test fun consecutiveAppendScroll_loadDelay0() = consecutiveAppendScroll(0)

    @Test fun consecutiveAppendScroll_loadDelay10000() = consecutiveAppendScroll(10000)

    private fun consecutiveAppendScroll(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    scrollTo(12)
                    scrollTo(18)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-19]
            // prefetched [20-22]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun consecutiveAppendScroll_multiSnapshots_loadDelay0() =
        consecutiveAppendScroll_multiSnapshots(0)

    @Test
    fun consecutiveAppendScroll_multiSnapshots_loadDelay10000() =
        consecutiveAppendScroll_multiSnapshots(10000)

    private fun consecutiveAppendScroll_multiSnapshots(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )

            val snapshot2 = pager.asSnapshot { scrollTo(18) }
            // appended [17-19]
            // prefetched [20-22]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test fun appendScroll_indexOutOfBounds_loadDelay0() = appendScroll_indexOutOfBounds(0)

    @Test fun appendScroll_indexOutOfBounds_loadDelay10000() = appendScroll_indexOutOfBounds(10000)

    private fun appendScroll_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(15) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // index out of bounds
                    scrollTo(50)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-10]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        }
    }

    @Test fun appendScroll_accessPageBoundary_loadDelay0() = appendScroll_accessPageBoundary(0)

    @Test
    fun appendScroll_accessPageBoundary_loadDelay10000() = appendScroll_accessPageBoundary(10000)

    private fun appendScroll_accessPageBoundary(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // after initial Load and prefetch, max loaded index is 7
                    scrollTo(7)
                }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [0-4]
            // prefetched [5-7] - expect only one extra page to be prefetched after this
            // scrollTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test fun appendScroll_withoutPrefetch_loadDelay0() = appendScroll_withoutPrefetch(0)

    @Test fun appendScroll_withoutPrefetch_loadDelay10000() = appendScroll_withoutPrefetch(10000)

    private fun appendScroll_withoutPrefetch(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPrefetch(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(10) }
            // initial load [0-4]
            // appended [5-10]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test fun appendScroll_withoutPlaceholders_loadDelay0() = appendScroll_withoutPlaceholders(0)

    @Test
    fun appendScroll_withoutPlaceholders_loadDelay10000() = appendScroll_withoutPlaceholders(10000)

    private fun appendScroll_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // scroll to max loaded index
                    scrollTo(7)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test
    fun appendScroll_withoutPlaceholders_indexOutOfBounds_loadDelay0() =
        appendScroll_withoutPlaceholders_indexOutOfBounds(0)

    @Test
    fun appendScroll_withoutPlaceholders_indexOutOfBounds_loadDelay10000() =
        appendScroll_withoutPlaceholders_indexOutOfBounds(10000)

    private fun appendScroll_withoutPlaceholders_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(20) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // 12 is larger than differ.size = 8 after initial refresh
                    scrollTo(12)
                }
            // ensure it honors scrollTo indices >= differ.size
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )
        }
    }

    @Test
    fun appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay0() =
        appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(0)

    @Test
    fun appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay10000() =
        appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(10000)

    private fun appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped(loadDelay: Long) {
        val dataFlow = flowOf(List(20) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(50) }
            // ensure index is still capped to max index available
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-19]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
                )
        }
    }

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders_loadDelay0() =
        consecutiveAppendScroll_withoutPlaceholders(0)

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders_loadDelay10000() =
        consecutiveAppendScroll_withoutPlaceholders(10000)

    private fun consecutiveAppendScroll_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    scrollTo(12)
                    scrollTo(17)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // first scrollTo appended [8-13]
            // second scrollTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders_multiSnapshot_loadDelay0() =
        consecutiveAppendScroll_withoutPlaceholders_multiSnapshot(0)

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders_multiSnapshot_loadDelay10000() =
        consecutiveAppendScroll_withoutPlaceholders_multiSnapshot(10000)

    private fun consecutiveAppendScroll_withoutPlaceholders_multiSnapshot(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )

            val snapshot2 = pager.asSnapshot { scrollTo(17) }
            // initial load [0-4]
            // prefetched [5-7]
            // first scrollTo appended [8-13]
            // second scrollTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun scrollTo_indexAccountsForSeparators_loadDelay0() = scrollTo_indexAccountsForSeparators(0)

    @Test
    fun scrollTo_indexAccountsForSeparators_loadDelay10000() =
        scrollTo_indexAccountsForSeparators(10000)

    private fun scrollTo_indexAccountsForSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        val pagerWithSeparator =
            pager.map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ -> if (before == 6) "sep" else null }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { scrollTo(8) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-10]
            // prefetched [11-13]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))

            val snapshotWithSeparator = pagerWithSeparator.asSnapshot { scrollTo(8) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-10]
            // no prefetch on [11-13] because separator fulfilled prefetchDistance
            assertThat(snapshotWithSeparator)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, "sep", 7, 8, 9, 10))
        }
    }

    @Test fun prependFling_loadDelay0() = prependFling(0)

    @Test fun prependFling_loadDelay10000() = prependFling(10000)

    private fun prependFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun prependFling_withDrops_loadDelay0() = prependFling_withDrops(0)

    @Test fun prependFling_withDrops_loadDelay10000() = prependFling_withDrops(10000)

    private fun prependFling_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(42) }
            // dropped [47-57]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(38, 39, 40, 41, 42, 43, 44, 45, 46))
        }
    }

    @Test fun prependFling_withSeparators_loadDelay0() = prependFling_withSeparators(0)

    @Test fun prependFling_withSeparators_loadDelay10000() = prependFling_withSeparators(10000)

    private fun prependFling_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPager(dataFlow, loadDelay, 50).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 42 || before == 49) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        "sep",
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        "sep",
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun consecutivePrependFling_loadDelay0() = consecutivePrependFling(0)

    @Test fun consecutivePrependFling_loadDelay10000() = consecutivePrependFling(10000)

    private fun consecutivePrependFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(42)
                    flingTo(38)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun consecutivePrependFling_multiSnapshots_loadDelay0() =
        consecutivePrependFling_multiSnapshots(0)

    @Test
    fun consecutivePrependFling_multiSnapshots_loadDelay10000() =
        consecutivePrependFling_multiSnapshots(10000)

    private fun consecutivePrependFling_multiSnapshots(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(42) }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )

            val snapshot2 = pager.asSnapshot { flingTo(38) }
            // prefetched [35-37]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test fun prependFling_jump_loadDelay0() = prependFling_jump(0)

    @Test fun prependFling_jump_loadDelay10000() = prependFling_jump(10000)

    private fun prependFling_jump(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(30)
                    // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35))
        }
    }

    @Test fun prependFling_scrollThenJump_loadDelay0() = prependFling_scrollThenJump(0)

    @Test fun prependFling_scrollThenJump_loadDelay10000() = prependFling_scrollThenJump(10000)

    private fun prependFling_scrollThenJump(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    scrollTo(43)
                    flingTo(30)
                    // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35))
        }
    }

    @Test fun prependFling_jumpThenFling_loadDelay0() = prependFling_jumpThenFling(0)

    @Test fun prependFling_jumpThenFling_loadDelay10000() = prependFling_jumpThenFling(10000)

    private fun prependFling_jumpThenFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(30)
                    // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                    flingTo(22)
                }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            // flingTo prepended [22-24]
            // prefetched [19-21]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35)
                )
        }
    }

    @Test fun prependFling_indexOutOfBounds_loadDelay0() = prependFling_indexOutOfBounds(0)

    @Test fun prependFling_indexOutOfBounds_loadDelay10000() = prependFling_indexOutOfBounds(10000)

    private fun prependFling_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(-3) }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // flingTo prepended [0-6]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                )
        }
    }

    @Test fun prependFling_accessPageBoundary_loadDelay0() = prependFling_accessPageBoundary(0)

    @Test
    fun prependFling_accessPageBoundary_loadDelay10000() = prependFling_accessPageBoundary(10000)

    private fun prependFling_accessPageBoundary(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // page boundary
                    flingTo(44)
                }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46] - expect only one extra page to be prefetched after this
            // prefetched [41-43]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )
        }
    }

    @Test fun prependFling_withoutPlaceholders_loadDelay0() = prependFling_withoutPlaceholders(0)

    @Test
    fun prependFling_withoutPlaceholders_loadDelay10000() = prependFling_withoutPlaceholders(10000)

    private fun prependFling_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    flingTo(0)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBounds_loadDelay0() =
        prependFling_withoutPlaceholders_indexOutOfBounds(0)

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBounds_loadDelay10000() =
        prependFling_withoutPlaceholders_indexOutOfBounds(10000)

    private fun prependFling_withoutPlaceholders_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, loadDelay, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(-8) }
            // ensure we honor negative indices if there is data to load
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay0() =
        prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped(0)

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay10000() =
        prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped(10000)

    private fun prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(-20) }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // flingTo prepended [0-1]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        }
    }

    @Test
    fun consecutivePrependFling_withoutPlaceholders_loadDelay0() =
        consecutivePrependFling_withoutPlaceholders(0)

    @Test
    fun consecutivePrependFling_withoutPlaceholders_loadDelay10000() =
        consecutivePrependFling_withoutPlaceholders(10000)

    private fun consecutivePrependFling_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    flingTo(-1)
                    // Without placeholders, first loaded page always starts at index[0]
                    flingTo(-5)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // first flingTo prepended [41-46]
            // index[0] is now anchored to [41]
            // second flingTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        32,
                        33,
                        34,
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun consecutivePrependFling_withoutPlaceholders_multiSnapshot_loadDelay0() =
        consecutivePrependFling_withoutPlaceholders_multiSnapshot(0)

    @Test
    fun consecutivePrependFling_withoutPlaceholders_multiSnapshot_loadDelay10000() =
        consecutivePrependFling_withoutPlaceholders_multiSnapshot(10000)

    private fun consecutivePrependFling_withoutPlaceholders_multiSnapshot(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    flingTo(-1)
                }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // flingTo prepended [44-46]
            // prefetched [41-43]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
                )

            val snapshot2 =
                pager.asSnapshot {
                    // Without placeholders, first loaded page always starts at index[0]
                    flingTo(-5)
                }
            // flingTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        32,
                        33,
                        34,
                        35,
                        36,
                        37,
                        38,
                        39,
                        40,
                        41,
                        42,
                        43,
                        44,
                        45,
                        46,
                        47,
                        48,
                        49,
                        50,
                        51,
                        52,
                        53,
                        54,
                        55,
                        56,
                        57,
                    )
                )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexPrecision_loadDelay0() =
        prependFling_withoutPlaceholders_indexPrecision(0)

    @Test
    fun prependFling_withoutPlaceholders_indexPrecision_loadDelay10000() =
        prependFling_withoutPlaceholders_indexPrecision(10000)

    private fun prependFling_withoutPlaceholders_indexPrecision(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        // load sizes and prefetch set to 1 to test precision of flingTo indexing
        val pager =
            Pager<Int, Int>(
                config =
                    PagingConfig(
                        pageSize = 1,
                        initialLoadSize = 1,
                        enablePlaceholders = false,
                        prefetchDistance = 1,
                    ),
                initialKey = 50,
                pagingSourceFactory = createFactory(dataFlow, loadDelay),
            )
        testScope.runTest {
            val snapshot =
                pager.flow.asSnapshot {
                    // after refresh, lastAccessedIndex == index[2] == item(9)
                    flingTo(-1)
                }
            // initial load [50]
            // prefetched [49], [51]
            // prepended [48]
            // prefetched [47]
            assertThat(snapshot).containsExactlyElementsIn(listOf(47, 48, 49, 50, 51))
        }
    }

    @Test fun appendFling_loadDelay0() = appendFling(0)

    @Test fun appendFling_loadDelay10000() = appendFling(10000)

    private fun appendFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )
        }
    }

    @Test fun appendFling_withDrops_loadDelay0() = appendFling_withDrops(0)

    @Test fun appendFling_withDrops_loadDelay10000() = appendFling_withDrops(10000)

    private fun appendFling_withDrops(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(12) }
            // dropped [0-7]
            assertThat(snapshot).containsExactlyElementsIn(listOf(8, 9, 10, 11, 12, 13, 14, 15, 16))
        }
    }

    @Test fun appendFling_withSeparators_loadDelay0() = appendFling_withSeparators(0)

    @Test fun appendFling_withSeparators_loadDelay10000() = appendFling_withSeparators(10000)

    private fun appendFling_withSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPager(dataFlow, loadDelay).map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ ->
                    if (before == 0 || before == 14) "sep" else null
                }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, "sep", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, "sep", 15, 16)
                )
        }
    }

    @Test fun consecutiveAppendFling_loadDelay0() = consecutiveAppendFling(0)

    @Test fun consecutiveAppendFling_loadDelay10000() = consecutiveAppendFling(10000)

    private fun consecutiveAppendFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(12)
                    flingTo(18)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-19]
            // prefetched [20-22]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun consecutiveAppendFling_multiSnapshots_loadDelay0() =
        consecutiveAppendFling_multiSnapshots(0)

    @Test
    fun consecutiveAppendFling_multiSnapshots_loadDelay10000() =
        consecutiveAppendFling_multiSnapshots(10000)

    private fun consecutiveAppendFling_multiSnapshots(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )

            val snapshot2 = pager.asSnapshot { flingTo(18) }
            // appended [17-19]
            // prefetched [20-22]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test fun appendFling_jump_loadDelay0() = appendFling_jump(0)

    @Test fun appendFling_jump_loadDelay10000() = appendFling_jump(10000)

    private fun appendFling_jump(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(30)
                    // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35))
        }
    }

    @Test fun appendFling_scrollThenJump_loadDelay0() = appendFling_scrollThenJump(0)

    @Test fun appendFling_scrollThenJump_loadDelay10000() = appendFling_scrollThenJump(10000)

    private fun appendFling_scrollThenJump(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    scrollTo(30)
                    flingTo(43)
                    // jump triggered when flingTo registered lastAccessedIndex[43], refreshKey[41]
                }
            // initial load [41-45]
            // prefetched [38-40], [46-48]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48))
        }
    }

    @Test fun appendFling_jumpThenFling_loadDelay0() = appendFling_jumpThenFling(0)

    @Test fun appendFling_jumpThenFling_loadDelay10000() = appendFling_jumpThenFling(10000)

    private fun appendFling_jumpThenFling(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, loadDelay)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(30)
                    // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                    flingTo(38)
                }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            // flingTo appended [36-38]
            // prefetched [39-41]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
                )
        }
    }

    @Test fun appendFling_indexOutOfBounds_loadDelay0() = appendFling_indexOutOfBounds(0)

    @Test fun appendFling_indexOutOfBounds_loadDelay10000() = appendFling_indexOutOfBounds(10000)

    private fun appendFling_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(15) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // index out of bounds
                    flingTo(50)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-10]
            assertThat(snapshot)
                .containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        }
    }

    @Test fun appendFling_accessPageBoundary_loadDelay0() = appendFling_accessPageBoundary(0)

    @Test
    fun appendFling_accessPageBoundary_loadDelay10000() = appendFling_accessPageBoundary(10000)

    private fun appendFling_accessPageBoundary(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // after initial Load and prefetch, max loaded index is 7
                    flingTo(7)
                }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [0-4]
            // prefetched [5-7] - expect only one extra page to be prefetched after this
            // flingTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test fun appendFling_withoutPlaceholders_loadDelay0() = appendFling_withoutPlaceholders(0)

    @Test
    fun appendFling_withoutPlaceholders_loadDelay10000() = appendFling_withoutPlaceholders(10000)

    private fun appendFling_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // scroll to max loaded index
                    flingTo(7)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBounds_loadDelay0() =
        appendFling_withoutPlaceholders_indexOutOfBounds(0)

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBounds_loadDelay10000() =
        appendFling_withoutPlaceholders_indexOutOfBounds(10000)

    private fun appendFling_withoutPlaceholders_indexOutOfBounds(loadDelay: Long) {
        val dataFlow = flowOf(List(20) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    // 12 is larger than differ.size = 8 after initial refresh
                    flingTo(12)
                }
            // ensure it honors scrollTo indices >= differ.size
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay0() =
        appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped(0)

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped_loadDelay10000() =
        appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped(10000)

    private fun appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped(loadDelay: Long) {
        val dataFlow = flowOf(List(20) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(50) }
            // ensure index is still capped to max index available
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-19]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
                )
        }
    }

    @Test
    fun consecutiveAppendFling_withoutPlaceholders_loadDelay0() =
        consecutiveAppendFling_withoutPlaceholders(0)

    @Test
    fun consecutiveAppendFling_withoutPlaceholders_loadDelay10000() =
        consecutiveAppendFling_withoutPlaceholders(10000)

    private fun consecutiveAppendFling_withoutPlaceholders(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot =
                pager.asSnapshot {
                    flingTo(12)
                    flingTo(17)
                }
            // initial load [0-4]
            // prefetched [5-7]
            // first flingTo appended [8-13]
            // second flingTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun consecutiveAppendFling_withoutPlaceholders_multiSnapshot_loadDelay0() =
        consecutiveAppendFling_withoutPlaceholders_multiSnapshot(0)

    @Test
    fun consecutiveAppendFling_withoutPlaceholders_multiSnapshot_loadDelay10000() =
        consecutiveAppendFling_withoutPlaceholders_multiSnapshot(10000)

    private fun consecutiveAppendFling_withoutPlaceholders_multiSnapshot(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPagerNoPlaceholders(dataFlow, loadDelay).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(12) }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot)
                .containsExactlyElementsIn(
                    listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                )

            val snapshot2 = pager.asSnapshot { flingTo(17) }
            // initial load [0-4]
            // prefetched [5-7]
            // first flingTo appended [8-13]
            // second flingTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot2)
                .containsExactlyElementsIn(
                    listOf(
                        0,
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10,
                        11,
                        12,
                        13,
                        14,
                        15,
                        16,
                        17,
                        18,
                        19,
                        20,
                        21,
                        22,
                    )
                )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexPrecision_loadDelay0() =
        appendFling_withoutPlaceholders_indexPrecision(0)

    @Test
    fun appendFling_withoutPlaceholders_indexPrecision_loadDelay10000() =
        appendFling_withoutPlaceholders_indexPrecision(10000)

    private fun appendFling_withoutPlaceholders_indexPrecision(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        // load sizes and prefetch set to 1 to test precision of flingTo indexing
        val pager =
            Pager<Int, Int>(
                config =
                    PagingConfig(
                        pageSize = 1,
                        initialLoadSize = 1,
                        enablePlaceholders = false,
                        prefetchDistance = 1,
                    ),
                pagingSourceFactory = createFactory(dataFlow, loadDelay),
            )
        testScope.runTest {
            val snapshot =
                pager.flow.asSnapshot {
                    // after refresh, lastAccessedIndex == index[2] == item(9)
                    flingTo(2)
                }
            // initial load [0]
            // prefetched [1]
            // appended [2]
            // prefetched [3]
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3))
        }
    }

    @Test
    fun flingTo_indexAccountsForSeparators_loadDelay0() = flingTo_indexAccountsForSeparators(0)

    @Test
    fun flingTo_indexAccountsForSeparators_loadDelay10000() =
        flingTo_indexAccountsForSeparators(10000)

    private fun flingTo_indexAccountsForSeparators(loadDelay: Long) {
        val dataFlow = flowOf(List(100) { it })
        val pager =
            createPager(
                dataFlow,
                PagingConfig(pageSize = 1, initialLoadSize = 1, prefetchDistance = 1),
                loadDelay,
                50,
            )
        val pagerWithSeparator =
            pager.map { pagingData ->
                pagingData.insertSeparators { before: Int?, _ -> if (before == 49) "sep" else null }
            }
        testScope.runTest {
            val snapshot = pager.asSnapshot { flingTo(51) }
            // initial load [50]
            // prefetched [49], [51]
            // flingTo [51] accessed item[51]prefetched [52]
            assertThat(snapshot).containsExactlyElementsIn(listOf(49, 50, 51, 52))

            val snapshotWithSeparator = pagerWithSeparator.asSnapshot { flingTo(51) }
            // initial load [50]
            // prefetched [49], [51]
            // flingTo [51] accessed item[50], no prefetch triggered
            assertThat(snapshotWithSeparator).containsExactlyElementsIn(listOf(49, "sep", 50, 51))
        }
    }

    @Test fun errorHandler_throw_loadDelay0() = errorHandler_throw(0)

    @Test fun errorHandler_throw_loadDelay10000() = errorHandler_throw(10000)

    private fun errorHandler_throw(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow, loadDelay)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager =
            Pager(
                    config = PagingConfig(pageSize = 3, initialLoadSize = 5),
                    pagingSourceFactory = {
                        factory.invoke().also { pagingSources.add(it as TestPagingSource) }
                    },
                )
                .flow
        testScope.runTest {
            val error =
                assertFailsWith(IllegalArgumentException::class) {
                    pager.asSnapshot(onError = { ErrorRecovery.THROW }) {
                        val source = pagingSources.first()
                        source.errorOnNextLoad = true
                        scrollTo(12)
                    }
                }
            assertThat(error.message).isEqualTo("PagingSource load error")
        }
    }

    @Test fun errorHandler_retry_loadDelay0() = errorHandler_retry(0)

    @Test fun errorHandler_retry_loadDelay10000() = errorHandler_retry(10000)

    private fun errorHandler_retry(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow, loadDelay)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager =
            Pager(
                    config = PagingConfig(pageSize = 3, initialLoadSize = 5),
                    pagingSourceFactory = {
                        factory.invoke().also { pagingSources.add(it as TestPagingSource) }
                    },
                )
                .flow
        testScope.runTest {
            val snapshot =
                pager.asSnapshot(onError = { ErrorRecovery.RETRY }) {
                    val source = pagingSources.first()
                    // should have two loads to far - refresh and append(prefetch)
                    assertThat(source.loads.size).isEqualTo(2)

                    // throw error on next load, should trigger a retry
                    source.errorOnNextLoad = true
                    scrollTo(7)

                    // make sure it did retry
                    assertThat(source.loads.size).isEqualTo(4)
                    // failed load
                    val failedLoad = source.loads[2]
                    assertThat(failedLoad is LoadParams.Append).isTrue()
                    assertThat(failedLoad.key).isEqualTo(8)
                    // retry load
                    val retryLoad = source.loads[3]
                    assertThat(retryLoad is LoadParams.Append).isTrue()
                    assertThat(retryLoad.key).isEqualTo(8)
                }
            // retry success
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        }
    }

    @Test fun errorHandler_retryFails_loadDelay0() = errorHandler_retryFails(0)

    @Test fun errorHandler_retryFails_loadDelay10000() = errorHandler_retryFails(10000)

    private fun errorHandler_retryFails(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow, loadDelay)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager =
            Pager(
                    config = PagingConfig(pageSize = 3, initialLoadSize = 5),
                    pagingSourceFactory = {
                        factory.invoke().also { pagingSources.add(it as TestPagingSource) }
                    },
                )
                .flow
        var retryCount = 0
        testScope.runTest {
            val snapshot =
                pager.asSnapshot(
                    onError = {
                        // retry twice
                        if (retryCount < 2) {
                            retryCount++
                            ErrorRecovery.RETRY
                        } else {
                            ErrorRecovery.RETURN_CURRENT_SNAPSHOT
                        }
                    }
                ) {
                    val source = pagingSources.first()
                    // should have two loads to far - refresh and append(prefetch)
                    assertThat(source.loads.size).isEqualTo(2)

                    source.errorOnLoads = true
                    scrollTo(8)

                    // additional failed load + two retries
                    assertThat(source.loads.size).isEqualTo(5)
                }
            // retry failed, returned existing snapshot
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    @Test fun errorHandler_returnSnapshot_loadDelay0() = errorHandler_returnSnapshot(0)

    @Test fun errorHandler_returnSnapshot_loadDelay10000() = errorHandler_returnSnapshot(10000)

    private fun errorHandler_returnSnapshot(loadDelay: Long) {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow, loadDelay)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager =
            Pager(
                    config = PagingConfig(pageSize = 3, initialLoadSize = 5),
                    pagingSourceFactory = {
                        factory.invoke().also { pagingSources.add(it as TestPagingSource) }
                    },
                )
                .flow
        testScope.runTest {
            val snapshot =
                pager.asSnapshot(onError = { ErrorRecovery.RETURN_CURRENT_SNAPSHOT }) {
                    val source = pagingSources.first()
                    source.errorOnNextLoad = true
                    scrollTo(12)
                }
            // snapshot items before scrollTo
            assertThat(snapshot).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4, 5, 6, 7))
        }
    }

    private fun createPager(dataFlow: Flow<List<Int>>, loadDelay: Long, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5),
            loadDelay,
            initialKey,
        )

    private fun createPager(data: List<Int>, loadDelay: Long, initialKey: Int = 0) =
        Pager<Int, Int>(
                PagingConfig(pageSize = 3, initialLoadSize = 5),
                initialKey,
                createSingleGenFactory(data, loadDelay),
            )
            .flow

    private fun createPagerNoPlaceholders(
        dataFlow: Flow<List<Int>>,
        loadDelay: Long,
        initialKey: Int = 0,
    ) =
        createPager(
            dataFlow,
            PagingConfig(
                pageSize = 3,
                initialLoadSize = 5,
                enablePlaceholders = false,
                prefetchDistance = 3,
            ),
            loadDelay,
            initialKey,
        )

    private fun createPagerNoPrefetch(
        dataFlow: Flow<List<Int>>,
        loadDelay: Long,
        initialKey: Int = 0,
    ) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, prefetchDistance = 0),
            loadDelay,
            initialKey,
        )

    private fun createPagerWithJump(
        dataFlow: Flow<List<Int>>,
        loadDelay: Long,
        initialKey: Int = 0,
    ) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, jumpThreshold = 5),
            loadDelay,
            initialKey,
        )

    private fun createPagerWithDrops(
        dataFlow: Flow<List<Int>>,
        loadDelay: Long,
        initialKey: Int = 0,
    ) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, maxSize = 9),
            loadDelay,
            initialKey,
        )

    private fun createPager(
        dataFlow: Flow<List<Int>>,
        config: PagingConfig,
        loadDelay: Long,
        initialKey: Int = 0,
    ) =
        Pager<Int, Int>(
                config = config,
                initialKey = initialKey,
                pagingSourceFactory = createFactory(dataFlow, loadDelay),
            )
            .flow
}

private class TestPagingSource(
    private val originalSource: PagingSource<Int, Int>,
    private val loadDelay: Long,
) : PagingSource<Int, Int>() { // }

    var errorOnNextLoad = false
    var errorOnLoads = false
    private val _loads = mutableListOf<LoadParams<Int>>()
    val loads: List<LoadParams<Int>>
        get() = _loads.toList()

    init {
        originalSource.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
        delay(loadDelay)
        _loads.add(params)
        if (errorOnNextLoad) {
            errorOnNextLoad = false
            return LoadResult.Error(IllegalArgumentException("PagingSource load error"))
        }
        if (errorOnLoads) {
            return LoadResult.Error(IllegalArgumentException("PagingSource load error"))
        }
        return originalSource.load(params)
    }

    override fun getRefreshKey(state: PagingState<Int, Int>) = originalSource.getRefreshKey(state)

    override val jumpingSupported: Boolean = originalSource.jumpingSupported
}
