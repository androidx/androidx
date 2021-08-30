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

import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.rxjava3.RxPagingSource
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
class RxPagingSourceTest {
    private fun loadInternal(params: PagingSource.LoadParams<Int>): Page<Int, Int> {
        val key = params.key!! // Intentionally fail on null keys
        return Page(
            List(params.loadSize) { it + key },
            prevKey = key - params.loadSize,
            nextKey = key + params.loadSize
        )
    }

    private val pagingSource = object : PagingSource<Int, Int>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
            return loadInternal(params)
        }

        override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
    }

    private val rxPagingSource = object : RxPagingSource<Int, Int>() {
        override fun loadSingle(params: LoadParams<Int>): Single<LoadResult<Int, Int>> {
            return Single.create { emitter ->
                emitter.onSuccess(loadInternal(params))
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
    }

    @Test
    fun basic() = runBlocking {
        val params = PagingSource.LoadParams.Refresh(0, 2, false)
        assertEquals(pagingSource.load(params), rxPagingSource.load(params))
    }

    @Test
    fun error() {
        runBlocking {
            val params = PagingSource.LoadParams.Refresh<Int>(null, 2, false)
            assertFailsWith<NullPointerException> { pagingSource.load(params) }
            assertFailsWith<NullPointerException> { rxPagingSource.load(params) }
        }
    }
}