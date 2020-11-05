/*
 * Copyright 2020 The Android Open Source Project
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

import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class ListenableFuturePagingDataTest {
    private val original = PagingData.from(listOf("a", "b", "c"))

    private val testDispatcher = TestCoroutineDispatcher()
    private val differ = TestPagingDataDiffer<String>(testDispatcher)

    @Test
    fun map() = testDispatcher.runBlockingTest {
        val transformed = original.mapAsync(
            AsyncFunction<String, String> {
                Futures.immediateFuture(it + it)
            },
            testDispatcher.asExecutor()
        )
        differ.collectFrom(transformed)
        assertEquals(listOf("aa", "bb", "cc"), differ.currentList)
    }

    @Test
    fun flatMap() = testDispatcher.runBlockingTest {
        val transformed = original.flatMapAsync(
            AsyncFunction<String, Iterable<String>> {
                Futures.immediateFuture(listOf(it!!, it))
            },
            testDispatcher.asExecutor()
        )
        differ.collectFrom(transformed)
        assertEquals(listOf("a", "a", "b", "b", "c", "c"), differ.currentList)
    }

    @Test
    fun filter() = testDispatcher.runBlockingTest {
        val filtered = original.filterAsync(
            AsyncFunction {
                Futures.immediateFuture(it != "b")
            },
            testDispatcher.asExecutor()
        )
        differ.collectFrom(filtered)
        assertEquals(listOf("a", "c"), differ.currentList)
    }

    @Test
    fun insertSeparators() = testDispatcher.runBlockingTest {
        val separated = original.insertSeparatorsAsync(
            AsyncFunction<AdjacentItems<String>, String?> {
                val (before, after) = it!!
                Futures.immediateFuture(if (before == null || after == null) null else "|")
            },
            testDispatcher.asExecutor()
        )
        differ.collectFrom(separated)
        assertEquals(listOf("a", "|", "b", "|", "c"), differ.currentList)
    }
}
