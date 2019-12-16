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

package androidx.camera.core.impl

import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.concurrent.futures.await
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val MAGIC_VALUE = 42
private val TEST_ERROR = TestError("TEST")

@SmallTest
class LiveDataObservableTest {

    @Test
    fun uninitializedFetch_throwsISE() {
        val uninitializedObservable = LiveDataObservable<Int>()

        runBlocking {
            assertThrows<IllegalStateException> {
                uninitializedObservable.fetchData().await()
            }
        }
    }

    @Test
    fun canSetAndFetchValue() {
        val observable = LiveDataObservable<Int>().apply {
            postValue(MAGIC_VALUE)
        }

        runBlocking {
            val fetched = observable.fetchData().await()
            assertThat(fetched).isEqualTo(MAGIC_VALUE)
        }
    }

    @Test
    fun canSetAndFetchValue_onMainThread() {
        var fetched: Int? = null
        runBlocking(Dispatchers.Main) {
            val observable = LiveDataObservable<Int>().apply {
                postValue(MAGIC_VALUE)
            }

            fetched = observable.fetchData().await()
        }

        assertThat(fetched).isEqualTo(MAGIC_VALUE)
    }

    @Test
    fun canSetAndReceiveError() {
        val observable = LiveDataObservable<Int>().apply {
            postError(TEST_ERROR)
        }

        runBlocking {
            assertThrows<TestError> { observable.fetchData().await() }
        }
    }

    @Test
    fun canObserveToRetrieveValue() {
        val observable = LiveDataObservable<Int>()
        observable.postValue(MAGIC_VALUE)

        runBlocking {
            val fetched = observable.getValue()
            assertThat(fetched).isEqualTo(MAGIC_VALUE)
        }
    }

    @Test
    fun canObserveToRetrieveError() {
        val observable = LiveDataObservable<Int>()
        observable.postError(TEST_ERROR)

        runBlocking {
            assertThrows<TestError> { observable.getValue() }
        }
    }

    @Test
    fun canObserveToRetrieveValue_whenSetAfterObserve() {
        val observable = LiveDataObservable<Int>()

        var fetched: Int? = null
        runBlocking {
            async {
                fetched = observable.getValue()
            }

            observable.postValue(MAGIC_VALUE)
        }
        assertThat(fetched).isEqualTo(MAGIC_VALUE)
    }

    @Test
    fun canObserveToRetrieveError_whenSetAfterObserve() {
        val observable = LiveDataObservable<Int>()

        assertThrows<TestError> {
            runBlocking {
                async {
                    observable.getValue()
                }

                observable.postError(TEST_ERROR)
            }
        }
    }
}

private class TestError(message: String) : Exception(message)

// In general, it may not be safe to add an observer and remove it within its callback.
// Since we know that the LiveData will always trampoline off the main thread, this is OK here.
// However, this would probably be implemented better as a flow, but that is currently not
// available.
private suspend fun <T> Observable<T>.getValue(): T? =
    suspendCoroutine { continuation ->
        val observer = object : Observable.Observer<T> {
            override fun onNewData(value: T?) {
                removeObserver(this)
                continuation.resume(value)
            }

            override fun onError(t: Throwable) {
                removeObserver(this)
                continuation.resumeWithException(t)
            }
        }
        addObserver(CameraXExecutors.directExecutor(), observer)
    }