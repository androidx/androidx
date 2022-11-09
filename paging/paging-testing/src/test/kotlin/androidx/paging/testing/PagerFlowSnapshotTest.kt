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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
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
    fun simpleInitialRefresh() {
        val dataFlow = flowOf(List(30) { it })
        val factory = dataFlow.asPagingSourceFactory(testScope)
        val pager = Pager(
            config = PagingConfig(
                pageSize = 3,
                initialLoadSize = 5,
                prefetchDistance = 0
            ),
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
    fun emptyInitialRefresh() {
        val dataFlow = emptyFlow<List<Int>>()
        val factory = dataFlow.asPagingSourceFactory(testScope)
        val pager = Pager(
            config = PagingConfig(
                pageSize = 3,
                initialLoadSize = 5,
                prefetchDistance = 0
            ),
            pagingSourceFactory = factory
        )
        testScope.runTest {
            val snapshot = pager.flow.asSnapshot(this) {}

            assertThat(snapshot).isEmpty()
        }
    }
}