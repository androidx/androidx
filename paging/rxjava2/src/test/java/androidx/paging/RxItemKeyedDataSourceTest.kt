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

import androidx.paging.ListenableItemKeyedDataSource.InitialResult
import androidx.paging.ListenableItemKeyedDataSource.LoadInitialParams
import androidx.paging.ListenableItemKeyedDataSource.LoadParams
import androidx.paging.ListenableItemKeyedDataSource.Result
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
class RxItemKeyedDataSourceTest {
    private val executor = TestExecutor()
    private val trivialDataSource: RxItemKeyedDataSource<Int, Int>
    private val perpetuallyLoadingDataSource: RxItemKeyedDataSource<Int, Int>
    private val errorDataSource: RxItemKeyedDataSource<Int, Int>

    private lateinit var disposable: Disposable

    init {
        // Trivial example with key == value.
        trivialDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> {
                val initialKey = params.requestedInitialKey ?: 0
                val data = List(params.requestedLoadSize) { initialKey + it }
                return Single.just(InitialResult(data, initialKey, data.size))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> {
                return Single.just(Result(List(params.requestedLoadSize) { params.key + it }))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> {
                val start = params.key - params.requestedLoadSize
                return Single.just(Result(List(params.requestedLoadSize) { start + it }))
                    .doOnSubscribe { disposable = it }
            }
        }

        perpetuallyLoadingDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { InitialResult<Int>(Collections.emptyList(), 0, 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int>(Collections.emptyList()) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { Result<Int>(Collections.emptyList()) }
                    .doOnSubscribe { disposable = it }
        }

        errorDataSource = object : RxItemKeyedDataSource<Int, Int>() {
            override fun getKey(item: Int): Int = item

            override fun onLoadInitial(params: LoadInitialParams<Int>): Single<InitialResult<Int>> =
                Single.error<InitialResult<Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadBefore(params: LoadParams<Int>): Single<Result<Int>> =
                Single.error<Result<Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadAfter(params: LoadParams<Int>): Single<Result<Int>> =
                Single.error<Result<Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }
        }

        trivialDataSource.initExecutor(executor)
        perpetuallyLoadingDataSource.initExecutor(executor)
        errorDataSource.initExecutor(executor)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(InitialResult<Int>(listOf(), 0, 0), actual)
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams(0, 3, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(InitialResult<Int>((0..2).toList(), 0, 3), actual)
    }

    @Test
    fun loadInitial_cancel() {
        val testParams = LoadInitialParams(0, 1, false)
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

        assertEquals(Result<Int>(listOf()), actual)
    }

    @Test
    fun loadAfter_simple() {
        val testParams = LoadParams(0, 3)
        val future = trivialDataSource.loadAfter(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(Result<Int>((0..2).toList()), actual)
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

        assertEquals(Result<Int>(listOf()), actual)
    }

    @Test
    fun loadBefore_simple() {
        val testParams = LoadParams(0, 3)
        val future = trivialDataSource.loadBefore(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(Result<Int>((-3..-1).toList()), actual)
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
        val testParams = LoadInitialParams(0, 0, false)
        val future = errorDataSource.loadInitial(testParams)

        executor.executeAll()
        assertFailsWith(ExecutionException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }
    }
}