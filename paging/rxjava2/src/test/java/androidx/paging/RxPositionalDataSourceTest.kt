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

import androidx.paging.ListenablePositionalDataSource.InitialResult
import androidx.paging.ListenablePositionalDataSource.LoadInitialParams
import androidx.paging.ListenablePositionalDataSource.LoadRangeParams
import androidx.paging.ListenablePositionalDataSource.RangeResult
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import org.junit.Assert.fail
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
class RxPositionalDataSourceTest {
    private val executor = TestExecutor()
    private val trivialDataSource: RxPositionalDataSource<Int>
    private val perpetuallyLoadingDataSource: RxPositionalDataSource<Int>
    private val errorDataSource: RxPositionalDataSource<Int>

    private lateinit var disposable: Disposable

    init {
        // Trivial example with position == value.
        trivialDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> {
                val start = params.requestedStartPosition
                val data = List(params.requestedLoadSize) { start + it }
                return Single.just(InitialResult(data, start, data.size))
                    .doOnSubscribe { disposable = it }
            }

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> {
                val start = params.startPosition
                return Single.just(RangeResult(List(params.loadSize) { start + it }))
                    .doOnSubscribe { disposable = it }
            }
        }

        perpetuallyLoadingDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { InitialResult<Int>(Collections.emptyList(), 0, 0) }
                    .doOnSubscribe { disposable = it }

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> =
                Single.timer(1, TimeUnit.DAYS)
                    .map { RangeResult<Int>(Collections.emptyList()) }
                    .doOnSubscribe { disposable = it }
        }

        errorDataSource = object : RxPositionalDataSource<Int>() {
            override fun onLoadInitial(params: LoadInitialParams): Single<InitialResult<Int>> =
                Single.error<InitialResult<Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }

            override fun onLoadRange(params: LoadRangeParams): Single<RangeResult<Int>> =
                Single.error<RangeResult<Int>>(NotImplementedError())
                    .doOnSubscribe { disposable = it }
        }

        trivialDataSource.initExecutor(executor)
        perpetuallyLoadingDataSource.initExecutor(executor)
        errorDataSource.initExecutor(executor)
    }

    @Test
    fun loadInitial_empty() {
        val testParams = LoadInitialParams(0, 0, 3, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(InitialResult<Int>(listOf(), 0, 0), actual)
    }

    @Test
    fun loadInitial_simple() {
        val testParams = LoadInitialParams(0, 4, 2, false)
        val future = trivialDataSource.loadInitial(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(InitialResult<Int>((0..3).toList(), 0, 4), actual)
    }

    @Test
    fun loadInitial_cancel() {
        val testParams = LoadInitialParams(0, 1, 1, false)
        val future = perpetuallyLoadingDataSource.loadInitial(testParams)
        future.cancel(true)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
            fail("Expected exception to be thrown but got here instead")
        } catch (expected: CancellationException) {
            // Don't re-throw since entering this block means the test passed!
        }

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun loadRange_empty() {
        val testParams = LoadRangeParams(5, 0)
        val future = trivialDataSource.loadRange(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(RangeResult<Int>(listOf()), actual)
    }

    @Test
    fun loadRange_simple() {
        val testParams = LoadRangeParams(0, 10)
        val future = trivialDataSource.loadRange(testParams)

        executor.executeAll()
        val actual = future.get(100, TimeUnit.MILLISECONDS)

        assertEquals(RangeResult<Int>((0..9).toList()), actual)
    }

    @Test
    fun loadRange_cancel() {
        val testParams = PositionalDataSource.LoadRangeParams(0, 10)
        val future = perpetuallyLoadingDataSource.loadRange(testParams)
        future.cancel(true)

        executor.executeAll()
        try {
            future.get(100, TimeUnit.MILLISECONDS)
            fail("Expected exception to be thrown but got here instead")
        } catch (expected: CancellationException) {
            // Don't re-throw since entering this block means the test passed!
        }

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun error() {
        val testParams = LoadRangeParams(0, 10)
        val future = errorDataSource.loadRange(testParams)

        executor.executeAll()
        assertFailsWith(ExecutionException::class) {
            future.get(100, TimeUnit.MILLISECONDS)
        }
    }
}