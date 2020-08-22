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

import androidx.concurrent.futures.CallbackToFutureAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListenableFuturePagingDataTest {
    private val original = PagingData.from(listOf("a", "b", "c"))

    @Test
    fun map() = runBlocking {
        val transformed = original.mapFuture {
            CallbackToFutureAdapter.getFuture<String> { completer -> completer.set(it + it) }
        }
        assertEquals(listOf("aa", "bb", "cc"), transformed.getFirstPageData())
    }

    @Test
    fun flatMap() = runBlocking {
        val transformed = original.flatMapFuture {
            CallbackToFutureAdapter.getFuture<Iterable<String>> { completer ->
                completer.set(listOf(it, it)) }
        }
        assertEquals(listOf("a", "a", "b", "b", "c", "c"), transformed.getFirstPageData())
    }

    @Test
    fun filter() = runBlocking {
        val filtered = original.filterFuture {
            CallbackToFutureAdapter.getFuture { completer -> completer.set(it != "b") }
        }
        assertEquals(listOf("a", "c"), filtered.getFirstPageData())
    }

    @Test
    fun insertSeparators() = runBlocking {
        val separated = original.insertSeparatorsFuture { left, right ->
            CallbackToFutureAdapter.getFuture<String?> { completer ->
                completer.set(if (left == null || right == null) null else "|")
            }
        }
        assertEquals(listOf("a", "|", "b", "|", "c"), separated.getFirstPageData())
    }
}
