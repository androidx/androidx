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
import androidx.paging.cachedIn
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class PagerFlowSnapshotTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun initialRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)

        val pager = Pager(
            config = CONFIG,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}
            // first page + prefetched page
            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4, 5, 6, 7)
            )
        }
    }

    @Test
    fun initialRefresh_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope)
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )
        }
    }

    @Test
    fun initialRefresh_withInitialKey() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope)
        val pager = Pager(
            config = CONFIG,
            initialKey = 10,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
            )
        }
    }

    @Test
    fun initialRefresh_withInitialKey_withoutPrefetch() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope)
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            initialKey = 10,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14)
            )
        }
    }

    @Test
    fun emptyInitialRefresh() {
        val dataFlow = emptyFlow<List<Int>>()
        val factory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        val pager = Pager(
            config = CONFIG,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}

            assertThat(snapshot).containsExactlyElementsIn(
                emptyList<Int>()
            )
        }
    }

    @Test
    fun manualRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            pagingSourceFactory = factory
        ).flow.cachedIn(testScope.backgroundScope)

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
        val factory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {
                refresh()
            }
            assertThat(snapshot).containsExactlyElementsIn(
                emptyList<Int>()
            )
        }
    }

    @Test
    fun consecutiveGenerations_fromSharedFlow() {
        val dataFlow = MutableSharedFlow<List<Int>>()
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            pagingSourceFactory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        ).flow.cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) { }
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
    fun consecutiveGenerations_fromFlow() {
        val dataFlow = flow {
            // first gen
            emit(emptyList())
            delay(500)
            // second gen
            emit(List(30) { it })
            delay(500)
            // third gen
            emit(List(30) { it + 30 })
        }
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            pagingSourceFactory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        ).flow.cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) { }
            assertThat(snapshot1).containsExactlyElementsIn(
                emptyList<Int>()
            )

            val snapshot2 = pager.asSnapshot(this) {
                delay(500)
            }
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )

            val snapshot3 = pager.asSnapshot(this) {
                delay(500)
            }
            assertThat(snapshot3).containsExactlyElementsIn(
                listOf(30, 31, 32, 33, 34)
            )
        }
    }

    @Test
    fun consecutiveGenerations_withInitialKey_nullRefreshKey() {
        val dataFlow = flow {
            // first gen
            emit(List(20) { it })
            delay(500)
            // second gen
            emit(List(20) { it })
        }
        val pager = Pager(
            config = CONFIG_NO_PREFETCH,
            initialKey = 10,
            pagingSourceFactory = dataFlow.asPagingSourceFactory(testScope.backgroundScope)
        ).flow.cachedIn(testScope.backgroundScope)
        testScope.runTest {
            val snapshot1 = pager.asSnapshot(this) { }
            assertThat(snapshot1).containsExactlyElementsIn(
                listOf(10, 11, 12, 13, 14)
            )

            val snapshot2 = pager.asSnapshot(this) {
                // wait for second gen to complete
                delay(500)
            }
            assertThat(snapshot2).containsExactlyElementsIn(
                listOf(0, 1, 2, 3, 4)
            )
        }
    }

    val CONFIG = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
    )

    val CONFIG_NO_PREFETCH = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
        prefetchDistance = 0
    )
}