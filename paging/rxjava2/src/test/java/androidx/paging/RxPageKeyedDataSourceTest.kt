/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.paging.ListenablePageKeyedDataSource.LoadInitialParams
import androidx.paging.ListenablePageKeyedDataSource.LoadParams
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class RxPageKeyedDataSourceTest {
    private val executor = TestExecutor()
    private val trivialDataSource: RxPageKeyedDataSource<Int, Int>
    private val perpetuallyLoadingDataSource: RxPageKeyedDataSource<Int, Int>
    private val errorDataSource: RxPageKeyedDataSource<Int, Int>

    private lateinit var disposable: Disposable

    init {
        trivialDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> {
                val data = List(params.requestedLoadSize) { it }
                val prevPage = -params.requestedLoadSize
                val nextPage = params.requestedLoadSize
                return Single.just(InitialResult(data, 0, data.size, prevPage, nextPage))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> {
                val loadSize = params.requestedLoadSize
                val start = params.key - params.requestedLoadSize
                return Single.just(Result(List(loadSize) { start + it }, params.key + loadSize))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> {
                val start = params.key
                val end = params.requestedLoadSize - 1
                return Single.just(Result((start..end).toList(), params.requestedLoadSize))
                    .doOnSubscribe { disposable = it }
            }
        }

        perpetuallyLoadingDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { InitialResult<Int, Int>(Collections.emptyList(), 0, 0, 0, 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int, Int>(Collections.emptyList(), 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int, Int>(Collections.emptyList(), 0) }
                    .doOnSubscribe { disposable = it }
        }

        errorDataSource = object : RxPageKeyedDataSource<Int, Int>() {
            override fun onLoadInitial(
                params: LoadInitialParams<Int>
            ): Single<InitialResult<Int, Int>> =
                Single.error<InitialResult<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.error<Result<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int, Int>> =
                Single.error<Result<Int, Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }
        }

        trivialDataSource.initExecutor(executor)
        perpetuallyLoadingDataSource.initExecutor(executor)
        errorDataSource.initExecutor(executor)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams<Int>(0, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(
            actual,
            ListenablePageKeyedDataSource.InitialResult<Int, Int>(listOf(), 0, 0, 0, 0)
        )
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams<Int>(3, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(
            actual,
            ListenablePageKeyedDataSource.InitialResult<Int, Int>((0..2).toList(), 0, 3, -3, 3)
        )
    }

    @Test
    fun loadInitial_cancel() {
        val testParams = LoadInitialParams<Int>(1, false)
        val future = perpetuallyLoadingDataSource.loadInitial(testParams)
        future.cancel(true)

        executor.executeAll()
        assertFailsWith(CancellationException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun loadAfter_empty() {
        val testParams = LoadParams(0, 0)
        val future = trivialDataSource.loadAfter(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(ListenablePageKeyedDataSource.Result<Int, Int>(listOf(), 0), actual)
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 3)
        val future = trivialDataSource.loadAfter(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(ListenablePageKeyedDataSource.Result<Int, Int>((0..2).toList(), 3), actual)
    }

    @Test
    fun loadAfter_cancel() {
        val testParams = LoadParams(0, 1)
        val future = perpetuallyLoadingDataSource.loadAfter(testParams)
        future.cancel(true)

        executor.executeAll()
        assertFailsWith(CancellationException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun loadBefore_empty() {
        val testParams = LoadParams(0, 0)
        val future = trivialDataSource.loadBefore(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(ListenablePageKeyedDataSource.Result<Int, Int>(listOf(), 0), actual)
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 3)
        val future = trivialDataSource.loadBefore(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(ListenablePageKeyedDataSource.Result<Int, Int>((-3..-1).toList(), 3), actual)
    }

    @Test
    fun loadBefore_cancel() {
        val testParams = LoadParams(0, 1)
        val future = perpetuallyLoadingDataSource.loadBefore(testParams)
        future.cancel(true)

        executor.executeAll()
        assertFailsWith(CancellationException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun error() {
        val testParams = LoadParams(0, 1)
        val future = errorDataSource.loadBefore(testParams)

        executor.executeAll()
        assertFailsWith(ExecutionException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }
    }
}