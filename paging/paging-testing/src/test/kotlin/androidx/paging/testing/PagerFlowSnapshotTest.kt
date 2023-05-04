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

package androidx.paging.testing

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSourceFactory
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class PagerFlowSnapshotTest(
    private val loadDelay: Long
) {
    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun withLoadDelay(): Array<Long> {
            return arrayOf(0, 10000)
        }
    }

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun createFactory(dataFlow: Flow<List<Int>>) = WrappedPagingSourceFactory(
        dataFlow.asPagingSourceFactory(testScope.backgroundScope),
        loadDelay
    )

    @Before
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun initialRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun initialRefresh_emptyOperations() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this)
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun initialRefresh_withSeparators() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow).map { pagingData ->
            pagingData.insertSeparators { before: Int?, after: Int? ->
                if (before != null && after != null) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}
            // loads 8[initial 5 + prefetch 3] items total, including separators
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, "sep", 1, "sep", 2, "sep", 3, "sep", 4)
            )
        }
    }

    @Test
    fun initialRefresh_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )
        }
    }

    @Test
    fun initialRefresh_withInitialKey() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
            )
        }
    }

    @Test
    fun initialRefresh_withInitialKey_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14)
            )
        }
    }

    @Test
    fun emptyInitialRefresh() {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                emptyList<Int>()
            )
        }
    }

    @Test
    fun emptyInitialRefresh_emptyOperations() {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this)

            assertThat(snapshot).containsExactlyElementsIn(
                emptyList<Int>()
            )
        }
    }

    @Test
    fun manualRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                refresh()
            }
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4),
            )
        }
    }

    @Test
    fun manualEmptyRefresh() {
        val dataFlow = emptyFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                refresh()
            }
            assertThat(snapshot).containsExactlyElementsIn(
                emptyList<Int>()
            )
        }
    }

    @Test
    fun appendWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [0-4]
                // prefetched [5-7]
                // appended [8-16]
                // prefetched [17-19]
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun appendWhile_withDrops() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerWithDrops(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item ->
                    item < 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // dropped [0-10]
                listOf(11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun appendWhile_withSeparators() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 9 || before == 12) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item ->
                    item !is Int || item < 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [0-4]
                // prefetched [5-7]
                // appended [8-16]
                // prefetched [17-19]
                listOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, "sep", 10, 11, 12, "sep", 13, 14, 15,
                    16, 17, 18, 19
                )
            )
        }
    }

    @Test
    fun appendWhile_withoutPrefetch() {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPrefetch(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [0-4]
                // appended [5-16]
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendWhile_withoutPlaceholders() {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPlaceholders(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item != 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [0-4]
                // prefetched [5-7]
                // appended [8-16]
                // prefetched [17-19]
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun prependWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 14
                }
            }
            // initial load [20-24]
            // prefetched [17-19], [25-27]
            // prepended [14-16]
            // prefetched [11-13]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27)
            )
        }
    }

    @Test
    fun prependWhile_withDrops() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerWithDrops(dataFlow, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 14
                }
            }
            // dropped [20-27]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun prependWhile_withSeparators() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 20).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 14 || before == 18) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item ->
                    item !is Int || item > 14
                }
            }
            // initial load [20-24]
            // prefetched [17-19], no append prefetch because separator fulfilled prefetchDistance
            // prepended [14-16]
            // prefetched [11-13]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    11, 12, 13, 14, "sep", 15, 16, 17, 18, "sep", 19, 20, 21, 22, 23,
                    24, 25, 26, 27
                )
            )
        }
    }

    @Test
    fun prependWhile_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, 20)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 14
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [20-24]
                // prepended [14-19]
                listOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
            )
        }
    }

    @Test
    fun prependWhile_withoutPlaceholders() {
        val dataFlow = flowOf(List(50) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 30)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item != 22
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [30-34]
                // prefetched [27-29], [35-37]
                // prepended [21-26]
                // prefetched [18-20]
                listOf(
                    18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37
                )
            )
        }
    }

    @Test
    fun appendWhile_withInitialKey() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 18
                }
            }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // appended [18-20]
            // prefetched [21-23]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
            )
        }
    }

    @Test
    fun appendWhile_withInitialKey_withoutPlaceholders() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item != 19
                }
            }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // appended [18-20]
            // prefetched [21-23]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
            )
        }
    }

    @Test
    fun appendWhile_withInitialKey_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 18
                }
            }
            // initial load [10-14]
            // appended [15-20]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
            )
        }
    }

    @Test
    fun prependWhile_withoutInitialKey() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > -3
                }
            }
            // initial load [0-4]
            // prefetched [5-7]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun consecutiveAppendWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 7
                }
            }

            val snapshot2 = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 22
                }
            }

            // includes initial load, 1st page, 2nd page (from prefetch)
            assertThat(snapshot1).containsExactlyElementsIn(
                List(11) { it }
            )

            // includes extra page from prefetch
            assertThat(snapshot2).containsExactlyElementsIn(
                List(26) { it }
            )
        }
    }

    @Test
    fun consecutivePrependWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPagerNoPrefetch(dataFlow, 20).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 17
                }
            }
            assertThat(snapshot1).containsExactlyElementsIn(
                listOf(17, 18, 19, 20, 21, 22, 23, 24)
            )
            val snapshot2 = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 11
                }
            }
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)
            )
        }
    }

    @Test
    fun appendWhile_outOfBounds_returnsCurrentlyLoadedItems() {
        val dataFlow = flowOf(List(10) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    // condition scrolls till end of data since we only have 10 items
                    item < 18
                }
            }

            // returns the items loaded before index becomes out of bounds
            assertThat(snapshot).containsExactlyElementsIn(
                List(10) { it }
            )
        }
    }

    @Test
    fun prependWhile_outOfBounds_returnsCurrentlyLoadedItems() {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPager(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    // condition scrolls till index = 0
                    item > -3
                }
            }
            // returns the items loaded before index becomes out of bounds
            assertThat(snapshot).containsExactlyElementsIn(
                // initial load [10-14]
                // prefetched [7-9], [15-17]
                // prepended [0-6]
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
            )
        }
    }

    @Test
    fun refreshAndAppendWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                refresh() // triggers second gen
                appendScrollWhile { item: Int ->
                    item < 10
                }
            }
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
            )
        }
    }

    @Test
    fun refreshAndPrependWhile() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 20).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // this prependScrollWhile does not cause paging to load more items
                // but it helps this test register a non-null anchorPosition so the upcoming
                // refresh doesn't start at index 0
                prependScrollWhile { item -> item > 20 }
                // triggers second gen
                refresh()
                prependScrollWhile { item: Int ->
                    item > 12
                }
            }
            // second gen initial load, anchorPos = 20, refreshKey = 18, loaded
            // initial load [18-22]
            // prefetched [15-17], [23-25]
            // prepended [12-14]
            // prefetched [9-11]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25)
            )
        }
    }

    @Test
    fun appendWhileAndRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                appendScrollWhile { item: Int ->
                    item < 10
                }
                refresh()
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // second gen initial load, anchorPos = 10, refreshKey = 8
                // initial load [8-12]
                // prefetched [5-7], [13-15]
                listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
            )
        }
    }

    @Test
    fun prependWhileAndRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val pager = createPager(dataFlow, 15).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                prependScrollWhile { item: Int ->
                    item > 8
                }
                refresh()
            }
            assertThat(snapshot).containsExactlyElementsIn(
                // second gen initial load, anchorPos = 8, refreshKey = 6
                // initial load [6-10]
                // prefetched [3-5], [11-13]
                listOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
            )
        }
    }

    @Test
    fun consecutiveGenerations_fromFlow() {
        val loadDelay = 500 + loadDelay
        // wait for 500 + loadDelay between each emission
        val dataFlow = flow {
            emit(emptyList())
            delay(loadDelay)

            emit(List(30) { it })
            delay(loadDelay)

            emit(List(30) { it + 30 })
        }
        val pager = createPagerNoPrefetch(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this)
            assertThat(snapshot1).containsExactlyElementsIn(
                emptyList<Int>()
            )

            delay(500)

            val snapshot2 = pager.asSnapshot(this)
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )

            delay(500)

            val snapshot3 = pager.asSnapshot(this)
            assertThat(snapshot3).containsExactlyElementsIn(
                listOf(30, 31, 32, 33, 34)
            )
        }
    }

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitAfterRefresh() {
        val dataFlow = MutableSharedFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this)
            assertThat(snapshot1).containsExactlyElementsIn(
                emptyList<Int>()
            )

            val snapshot2 = pager.asSnapshot(this) {
                dataFlow.emit(List(30) { it })
            }
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )

            val snapshot3 = pager.asSnapshot(this) {
                dataFlow.emit(List(30) { it + 30 })
            }
            assertThat(snapshot3).containsExactlyElementsIn(
                listOf(30, 31, 32, 33, 34)
            )
        }
    }

    @Test
    fun consecutiveGenerations_fromSharedFlow_emitBeforeRefresh() {
        val dataFlow = MutableSharedFlow<List<Int>>()
        val pager = createPagerNoPrefetch(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            dataFlow.emit(emptyList())
            val snapshot1 = pager.asSnapshot(this)
            assertThat(snapshot1).containsExactlyElementsIn(
                emptyList<Int>()
            )

            dataFlow.emit(List(30) { it })
            val snapshot2 = pager.asSnapshot(this)
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )

            dataFlow.emit(List(30) { it + 30 })
            val snapshot3 = pager.asSnapshot(this)
            assertThat(snapshot3).containsExactlyElementsIn(
                listOf(30, 31, 32, 33, 34)
            )
        }
    }

    @Test
    fun consecutiveGenerations_nonNullRefreshKey() {
        val loadDelay = 500 + loadDelay
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            // wait for refresh + append
            delay(loadDelay * 2)
            // second gen
            emit(List(20) { it })
        }
        val pager = createPagerNoPrefetch(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) {
                // we scroll to register a non-null anchorPos
                appendScrollWhile { item: Int ->
                    item < 5
                }
            }
            assertThat(snapshot1).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )

            delay(1000)
            val snapshot2 = pager.asSnapshot(this)
            // anchorPos = 5, refreshKey = 3
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun consecutiveGenerations_withInitialKey_nullRefreshKey() {
        val loadDelay = 500 + loadDelay
        // wait for 500 + loadDelay between each emission
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            delay(loadDelay)
            // second gen
            emit(List(20) { it })
        }
        val pager = createPagerNoPrefetch(dataFlow, 10).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this)
            assertThat(snapshot1).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14)
            )

            delay(500)
            val snapshot2 = pager.asSnapshot(this)
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )
        }
    }

    @Test
    fun consecutiveGenerations_withInitialKey_nonNullRefreshKey() {
        val loadDelay = 500 + loadDelay
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            // wait for refresh + append
            delay(loadDelay * 2)
            // second gen
            emit(List(20) { it })
        }
        val pager = createPagerNoPrefetch(dataFlow, 10).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) {
                // we scroll to register a non-null anchorPos
                appendScrollWhile { item: Int ->
                    item < 15
                }
            }
            assertThat(snapshot1).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14, 15, 16, 17)
            )

            delay(1000)
            val snapshot2 = pager.asSnapshot(this)
            // anchorPos = 15, refreshKey = 13
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(13, 14, 15, 16, 17)
            )
        }
    }

    @Test
    fun prependScroll() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependScroll_withDrops() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
            }
            // dropped [47-57]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(38, 39, 40, 41, 42, 43, 44, 45, 46)
            )
        }
    }

    @Test
    fun prependScroll_withSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 42 || before == 49) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, "sep", 43, 44, 45, 46, 47, 48, 49, "sep", 50, 51, 52,
                    53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependScroll() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
                scrollTo(38)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependScroll_multiSnapshots() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
                )
            )

            val snapshot2 = pager.asSnapshot(this) {
                scrollTo(38)
            }
            // prefetched [35-37]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependScroll_indexOutOfBounds() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(-5)
            }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // scrollTo prepended [0-1]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            )
        }
    }

    @Test
    fun prependScroll_accessPageBoundary() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(47)
            }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [50-54]
            // prefetched [47-49], [55-57] - expect only one extra page to be prefetched after this
            // scrollTo prepended [44-46]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )
        }
    }

    @Test
    fun prependScroll_withoutPrefetch() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPrefetch(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(42)
            }
            // initial load [50-54]
            // prepended [41-49]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54)
            )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                scrollTo(0)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBounds() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(-5)
            }
            // ensure it honors negative indices starting with index[0] = item[47]
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // scrollTo prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_indexOutOfBoundsIsCapped() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(-5)
            }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // scrollTo prepended [0-1]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            )
        }
    }

    @Test
    fun consecutivePrependScroll_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
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
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependScroll_withoutPlaceholders_multiSnapshot() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                scrollTo(-1)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // scrollTo prepended [44-46]
            // prefetched [41-43]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )

            val snapshot2 = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                scrollTo(-5)
            }
            // scrollTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(
                    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependScroll_withoutPlaceholders_noPrefetchTriggered() {
        val dataFlow = flowOf(List(100) { it })
        val pager = Pager(
            config = PagingConfig(
                pageSize = 4,
                initialLoadSize = 8,
                enablePlaceholders = false,
                // a small prefetchDistance to prevent prefetch until we scroll to boundary
                prefetchDistance = 1
            ),
            initialKey = 50,
            pagingSourceFactory = createFactory(dataFlow),
        ).flow.cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                scrollTo(0)
            }
            // initial load [50-57]
            // no prefetch after initial load because it didn't hit prefetch distance
            // scrollTo prepended [46-49]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )
        }
    }

    @Test
    fun appendScroll() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendScroll_withDrops() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
            }
            // dropped [0-7]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendScroll_withSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 0 || before == 14) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, "sep", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, "sep", 15, 16)
            )
        }
    }

    @Test
    fun consecutiveAppendScroll() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
                scrollTo(18)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-19]
            // prefetched [20-22]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22)
            )
        }
    }

    @Test
    fun consecutiveAppendScroll_multiSnapshots() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )

            val snapshot2 = pager.asSnapshot(this) {
                scrollTo(18)
            }
            // appended [17-19]
            // prefetched [20-22]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22
                )
            )
        }
    }

    @Test
    fun appendScroll_indexOutOfBounds() {
        val dataFlow = flowOf(List(15) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // index out of bounds
                scrollTo(50)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
            )
        }
    }

    @Test
    fun appendScroll_accessPageBoundary() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // after initial Load and prefetch, max loaded index is 7
                scrollTo(7)
            }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [0-4]
            // prefetched [5-7] - expect only one extra page to be prefetched after this
            // scrollTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun appendScroll_withoutPrefetch() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPrefetch(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(10)
            }
            // initial load [0-4]
            // appended [5-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun appendScroll_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // scroll to max loaded index
                scrollTo(7)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun appendScroll_withoutPlaceholders_indexOutOfBounds() {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // 12 is larger than differ.size = 8 after initial refresh
                scrollTo(12)
            }
            // ensure it honors scrollTo indices >= differ.size
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendToScroll_withoutPlaceholders_indexOutOfBoundsIsCapped() {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(50)
            }
            // ensure index is still capped to max index available
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-19]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
                scrollTo(17)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // first scrollTo appended [8-13]
            // second scrollTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22)
            )
        }
    }

    @Test
    fun consecutiveAppendScroll_withoutPlaceholders_multiSnapshot() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // scrollTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )

            val snapshot2 = pager.asSnapshot(this) {
                scrollTo(17)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // first scrollTo appended [8-13]
            // second scrollTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22)
            )
        }
    }

    @Test
    fun scrollTo_indexAccountsForSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        val pagerWithSeparator = pager.map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 6) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(8)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-10]
            // prefetched [11-13]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
            )

            val snapshotWithSeparator = pagerWithSeparator.asSnapshot(this) {
                scrollTo(8)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-10]
            // no prefetch on [11-13] because separator fulfilled prefetchDistance
            assertThat(snapshotWithSeparator).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, "sep", 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun prependFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependFling_withDrops() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(42)
            }
            // dropped [47-57]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(38, 39, 40, 41, 42, 43, 44, 45, 46)
            )
        }
    }

    @Test
    fun prependFling_withSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 42 || before == 49) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, "sep", 43, 44, 45, 46, 47, 48, 49, "sep", 50, 51, 52,
                    53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(42)
                flingTo(38)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependFling_multiSnapshots() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(42)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [41-46]
            // prefetched [38-40]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57
                )
            )

            val snapshot2 = pager.asSnapshot(this) {
                flingTo(38)
            }
            // prefetched [35-37]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }
    @Test
    fun prependFling_jump() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(30)
                // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
            }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35)
            )
        }
    }

    @Test
    fun prependFling_scrollThenJump() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(43)
                flingTo(30)
                // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
            }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35)
            )
        }
    }

    @Test
    fun prependFling_jumpThenFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(30)
                // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                flingTo(22)
            }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            // flingTo prepended [22-24]
            // prefetched [19-21]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35)
            )
        }
    }

    @Test
    fun prependFling_indexOutOfBounds() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 10)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(-3)
            }
            // initial load [10-14]
            // prefetched [7-9], [15-17]
            // flingTo prepended [0-6]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
            )
        }
    }

    @Test
    fun prependFling_accessPageBoundary() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // page boundary
                flingTo(44)
            }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46] - expect only one extra page to be prefetched after this
            // prefetched [41-43]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                flingTo(0)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [44-46]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBounds() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(-8)
            }
            // ensure we honor negative indices if there is data to load
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // prepended [38-46]
            // prefetched [35-37]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53,
                    54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexOutOfBoundsIsCapped() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 5).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(-20)
            }
            // ensure index is capped when no more data to load
            // initial load [5-9]
            // prefetched [2-4], [10-12]
            // flingTo prepended [0-1]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            )
        }
    }

    @Test
    fun consecutivePrependFling_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
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
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(
                    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun consecutivePrependFling_withoutPlaceholders_multiSnapshot() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow, 50).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                flingTo(-1)
            }
            // initial load [50-54]
            // prefetched [47-49], [55-57]
            // flingTo prepended [44-46]
            // prefetched [41-43]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57)
            )

            val snapshot2 = pager.asSnapshot(this) {
                // Without placeholders, first loaded page always starts at index[0]
                flingTo(-5)
            }
            // flingTo prepended [35-40]
            // prefetched [32-34]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(
                    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50, 51, 52, 53, 54, 55, 56, 57
                )
            )
        }
    }

    @Test
    fun prependFling_withoutPlaceholders_indexPrecision() {
        val dataFlow = flowOf(List(100) { it })
        // load sizes and prefetch set to 1 to test precision of flingTo indexing
        val pager = Pager(
            config = PagingConfig(
                pageSize = 1,
                initialLoadSize = 1,
                enablePlaceholders = false,
                prefetchDistance = 1
            ),
            initialKey = 50,
            pagingSourceFactory = createFactory(dataFlow),
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {
                // after refresh, lastAccessedIndex == index[2] == item(9)
                flingTo(-1)
            }
            // initial load [50]
            // prefetched [49], [51]
            // prepended [48]
            // prefetched [47]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(47, 48, 49, 50, 51)
            )
        }
    }

    @Test
    fun appendFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendFling_withDrops() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithDrops(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
            }
            // dropped [0-7]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendFling_withSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow).map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 0 || before == 14) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, "sep", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, "sep", 15, 16)
            )
        }
    }

    @Test
    fun consecutiveAppendFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
                flingTo(18)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-19]
            // prefetched [20-22]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22)
            )
        }
    }

    @Test
    fun consecutiveAppendFling_multiSnapshots() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )

            val snapshot2 = pager.asSnapshot(this) {
                flingTo(18)
            }
            // appended [17-19]
            // prefetched [20-22]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22
                )
            )
        }
    }

    @Test
    fun appendFling_jump() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(30)
                // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
            }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35)
            )
        }
    }

    @Test
    fun appendFling_scrollThenJump() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                scrollTo(30)
                flingTo(43)
                // jump triggered when flingTo registered lastAccessedIndex[43], refreshKey[41]
            }
            // initial load [41-45]
            // prefetched [38-40], [46-48]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48)
            )
        }
    }

    @Test
    fun appendFling_jumpThenFling() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerWithJump(dataFlow)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(30)
                // jump triggered when flingTo registered lastAccessedIndex[30], refreshKey[28]
                flingTo(38)
            }
            // initial load [28-32]
            // prefetched [25-27], [33-35]
            // flingTo appended [36-38]
            // prefetched [39-41]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41)
            )
        }
    }

    @Test
    fun appendFling_indexOutOfBounds() {
        val dataFlow = flowOf(List(15) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // index out of bounds
                flingTo(50)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
            )
        }
    }

    @Test
    fun appendFling_accessPageBoundary() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // after initial Load and prefetch, max loaded index is 7
                flingTo(7)
            }
            // ensure that SnapshotLoader waited for last prefetch before returning
            // initial load [0-4]
            // prefetched [5-7] - expect only one extra page to be prefetched after this
            // flingTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // scroll to max loaded index
                flingTo(7)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-10]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBounds() {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                // 12 is larger than differ.size = 8 after initial refresh
                flingTo(12)
            }
            // ensure it honors scrollTo indices >= differ.size
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexOutOfBoundsIsCapped() {
        val dataFlow = flowOf(List(20) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(50)
            }
            // ensure index is still capped to max index available
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-19]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
            )
        }
    }

    @Test
    fun consecutiveAppendFling_withoutPlaceholders() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
                flingTo(17)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // first flingTo appended [8-13]
            // second flingTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22)
            )
        }
    }

    @Test
    fun consecutiveAppendFling_withoutPlaceholders_multiSnapshot() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPagerNoPlaceholders(dataFlow).cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(12)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // flingTo appended [8-13]
            // prefetched [14-16]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            )

            val snapshot2 = pager.asSnapshot(this) {
                flingTo(17)
            }
            // initial load [0-4]
            // prefetched [5-7]
            // first flingTo appended [8-13]
            // second flingTo appended [14-19]
            // prefetched [19-22]
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22)
            )
        }
    }

    @Test
    fun appendFling_withoutPlaceholders_indexPrecision() {
        val dataFlow = flowOf(List(100) { it })
        // load sizes and prefetch set to 1 to test precision of flingTo indexing
        val pager = Pager(
            config = PagingConfig(
                pageSize = 1,
                initialLoadSize = 1,
                enablePlaceholders = false,
                prefetchDistance = 1
            ),
            pagingSourceFactory = createFactory(dataFlow),
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {
                // after refresh, lastAccessedIndex == index[2] == item(9)
                flingTo(2)
            }
            // initial load [0]
            // prefetched [1]
            // appended [2]
            // prefetched [3]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3)
            )
        }
    }

    @Test
    fun flingTo_indexAccountsForSeparators() {
        val dataFlow = flowOf(List(100) { it })
        val pager = createPager(
            dataFlow,
            PagingConfig(
                pageSize = 1,
                initialLoadSize = 1,
                prefetchDistance = 1
            ),
            50
        )
        val pagerWithSeparator = pager.map { pagingData ->
            pagingData.insertSeparators { before: Int?, _ ->
                if (before == 49) "sep" else null
            }
        }
        testScope.runTest {
            val snapshot = pager.asSnapshot(this) {
                flingTo(51)
            }
            // initial load [50]
            // prefetched [49], [51]
            // flingTo [51] accessed item[51]prefetched [52]
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(49, 50, 51, 52)
            )

            val snapshotWithSeparator = pagerWithSeparator.asSnapshot(this) {
                flingTo(51)
            }
            // initial load [50]
            // prefetched [49], [51]
            // flingTo [51] accessed item[50], no prefetch triggered
            assertThat(snapshotWithSeparator).containsExactlyElementsIn(
                listOf(49, "sep", 50, 51)
            )
        }
    }

    @Test
    fun errorHandler_throw() {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager = Pager(
            config = PagingConfig(pageSize = 3, initialLoadSize = 5),
            pagingSourceFactory = {
                factory.invoke().also { pagingSources.add(it as TestPagingSource) }
            },
        ).flow
        testScope.runTest {
            val error = assertFailsWith(IllegalArgumentException::class) {
                pager.asSnapshot(
                    coroutineScope = this,
                    onError = { ErrorRecovery.THROW }
                ) {
                    val source = pagingSources.first()
                    source.errorOnNextLoad = true
                    scrollTo(12)
                }
            }
            assertThat(error.message).isEqualTo("PagingSource load error")
        }
    }

    @Test
    fun errorHandler_retry() {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager = Pager(
            config = PagingConfig(pageSize = 3, initialLoadSize = 5),
            pagingSourceFactory = {
                factory.invoke().also { pagingSources.add(it as TestPagingSource) }
            },
        ).flow
        testScope.runTest {
            val snapshot = pager.asSnapshot(
                coroutineScope = this,
                onError = { ErrorRecovery.RETRY }
            ) {
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
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            )
        }
    }

    @Test
    fun errorHandler_retryFails() {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager = Pager(
            config = PagingConfig(pageSize = 3, initialLoadSize = 5),
            pagingSourceFactory = {
                factory.invoke().also { pagingSources.add(it as TestPagingSource) }
            },
        ).flow
        var retryCount = 0
        testScope.runTest {
            val snapshot = pager.asSnapshot(
                coroutineScope = this,
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
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun errorHandler_returnSnapshot() {
        val dataFlow = flowOf(List(30) { it })
        val factory = createFactory(dataFlow)
        val pagingSources = mutableListOf<TestPagingSource>()
        val pager = Pager(
            config = PagingConfig(pageSize = 3, initialLoadSize = 5),
            pagingSourceFactory = {
                factory.invoke().also { pagingSources.add(it as TestPagingSource) }
            },
        ).flow
        testScope.runTest {
            val snapshot = pager.asSnapshot(
                coroutineScope = this,
                onError = { ErrorRecovery.RETURN_CURRENT_SNAPSHOT }
            ) {
                val source = pagingSources.first()
                source.errorOnNextLoad = true
                scrollTo(12)
            }
            // snapshot items before scrollTo
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    private fun createPager(dataFlow: Flow<List<Int>>, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5),
            initialKey
        )

    private fun createPagerNoPlaceholders(dataFlow: Flow<List<Int>>, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(
                pageSize = 3,
                initialLoadSize = 5,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            initialKey)

    private fun createPagerNoPrefetch(dataFlow: Flow<List<Int>>, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, prefetchDistance = 0),
            initialKey
        )

    private fun createPagerWithJump(dataFlow: Flow<List<Int>>, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, jumpThreshold = 5),
            initialKey
        )

    private fun createPagerWithDrops(dataFlow: Flow<List<Int>>, initialKey: Int = 0) =
        createPager(
            dataFlow,
            PagingConfig(pageSize = 3, initialLoadSize = 5, maxSize = 9),
            initialKey
        )

    private fun createPager(
        dataFlow: Flow<List<Int>>,
        config: PagingConfig,
        initialKey: Int = 0,
    ) = Pager(
            config = config,
            initialKey = initialKey,
            pagingSourceFactory = createFactory(dataFlow),
        ).flow
}

private class WrappedPagingSourceFactory(
    private val factory: PagingSourceFactory<Int, Int>,
    private val loadDelay: Long,
) : PagingSourceFactory<Int, Int> {
    override fun invoke(): PagingSource<Int, Int> = TestPagingSource(factory(), loadDelay)
}

private class TestPagingSource(
    private val originalSource: PagingSource<Int, Int>,
    private val loadDelay: Long,
) : PagingSource<Int, Int>() {

    var errorOnNextLoad = false
    var errorOnLoads = false
    private val _loads = mutableListOf<LoadParams<Int>>()
    val loads: List<LoadParams<Int>>
        get() = _loads.toList()

    init {
        originalSource.registerInvalidatedCallback {
            invalidate()
        }
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