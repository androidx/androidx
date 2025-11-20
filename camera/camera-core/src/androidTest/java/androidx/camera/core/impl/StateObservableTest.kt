/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.camera.core.impl.Observable.Observer
import androidx.camera.testing.impl.asFlow
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

private const val INITIAL_STATE = 0
private const val MAGIC_STATE = 42
private val TEST_ERROR = TestError("TEST")

@SmallTest
@RunWith(AndroidJUnit4::class)
public class StateObservableTest {

    @Test
    public fun canFetchInitialState(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        val fetched = observable.fetchData().await()
        assertThat(fetched).isEqualTo(INITIAL_STATE)
    }

    @Test
    public fun canFetchInitialError(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialError<Any?>(TEST_ERROR)

        assertThrows<TestError> { observable.fetchData().await() }
    }

    @Test
    public fun fetchIsImmediate() {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        assertThat(observable.fetchData().isDone).isTrue()
    }

    @Test
    public fun canObserveToFetchInitialState(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        val fetched = observable.asFlow().first()
        assertThat(fetched).isEqualTo(INITIAL_STATE)
    }

    @Test
    public fun canObserveToFetchInitialError(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialError<Any?>(TEST_ERROR)

        assertThrows<TestError> { observable.asFlow().first() }
    }

    @Test
    public fun canObserveToFetchPostedState(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)
        observable.setState(MAGIC_STATE)

        val fetched = observable.asFlow().dropWhile { it != MAGIC_STATE }.first()
        assertThat(fetched).isEqualTo(MAGIC_STATE)
    }

    @Test
    public fun canObserveToReceivePostedError(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)
        observable.setError(TEST_ERROR)

        assertThrows<TestError> { observable.asFlow().collect() }
    }

    @Test
    public fun canSetAndFetchState_fromSeparateThreads(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)
        launch(Dispatchers.Main) {
            delay(100) // Not strictly necessary, but wait before setting state
            observable.setState(MAGIC_STATE)
        }

        val fetched =
            async(Dispatchers.IO) { observable.asFlow().dropWhile { it != MAGIC_STATE }.first() }

        assertThat(fetched.await()).isEqualTo(MAGIC_STATE)
    }

    @Test
    fun canObserveToRetrieveState_whenSetAfterObserve(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        // Add the observer
        val deferred = CompletableDeferred<Int>()
        observable.addObserver(
            Dispatchers.IO.asExecutor(),
            object : Observable.Observer<Int> {
                override fun onNewData(state: Int?) {
                    if (state == MAGIC_STATE) {
                        deferred.complete(state)
                    }
                }

                override fun onError(t: Throwable) {
                    deferred.completeExceptionally(t)
                }
            },
        )

        // Post the state
        observable.setState(MAGIC_STATE)

        assertThat(deferred.await()).isEqualTo(MAGIC_STATE)
    }

    @Test
    public fun canObserveToRetrieveError_whenSetAfterObserve(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        // Add the observer
        val deferred = CompletableDeferred<Int>()
        observable.addObserver(
            Dispatchers.IO.asExecutor(),
            object : Observable.Observer<Int> {
                override fun onNewData(state: Int?) {
                    // Ignore states
                }

                override fun onError(t: Throwable) {
                    deferred.completeExceptionally(t)
                }
            },
        )

        // Post the error
        observable.setError(TEST_ERROR)

        assertThrows<TestError> { deferred.await() }
    }

    @MediumTest
    @Test
    public fun allObservers_receiveFinalState(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        // Create 20 observers to ensure they all complete
        val receiveJob =
            launch(Dispatchers.IO) {
                repeat(20) {
                    launch { observable.asFlow().dropWhile { it != MAGIC_STATE }.first() }
                }
            }

        // Create another coroutine to set states
        launch(Dispatchers.IO) {
            (25 downTo 0).forEach { i ->
                observable.setState(MAGIC_STATE - i)
                delay(5)
            }
        }

        // Ensure receiveJob completes
        receiveJob.join()
    }

    @Test
    fun doesNotObserveNewState_whenSetAfterRemove(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        // Add the observer to remove later
        val deferredCompletingObserver1 = observable.addObserver(MAGIC_STATE)

        // Add another observer to know if state was notified
        val deferredCompletingObserver2 = observable.addObserver(MAGIC_STATE)

        // Remove first observer
        observable.removeObserver(deferredCompletingObserver1.observer)

        // Post the state
        observable.setState(MAGIC_STATE)

        // Wait for second observer to ensure observers were notified
        assertThat(deferredCompletingObserver2.deferred.await()).isEqualTo(MAGIC_STATE)

        // Check removed observer is not notified after waiting a small time
        assertThat(withTimeoutOrNull(100) { deferredCompletingObserver1.deferred.await() }).isNull()
    }

    @Test
    fun doesNotObserveNewState_whenSetAfterRemovingAll(): Unit = runBlocking {
        val observable = MutableStateObservable.withInitialState(INITIAL_STATE)

        // Add the first observer to remove later
        val deferredCompletingObserver1 = observable.addObserver(MAGIC_STATE)

        // Remove first observer
        observable.removeObservers()

        // Add the observer to know if state was notified
        val deferredCompletingObserver2 = observable.addObserver(MAGIC_STATE)

        // Post the state
        observable.setState(MAGIC_STATE)

        // Wait for second observer to ensure observers were notified
        assertThat(deferredCompletingObserver2.deferred.await()).isEqualTo(MAGIC_STATE)

        // Check the removed observer is not notified after waiting a small time
        assertThat(withTimeoutOrNull(100) { deferredCompletingObserver1.deferred.await() }).isNull()
    }

    data class DeferredCompletingObserver<T>(
        val observer: Observer<T>,
        val deferred: CompletableDeferred<T>,
    )

    private fun <T> Observable<T>.addObserver(targetState: T): DeferredCompletingObserver<T> {
        val deferred = CompletableDeferred<T>()
        val observer =
            object : Observer<T> {
                override fun onNewData(state: T?) {
                    if (state == targetState) {
                        deferred.complete(targetState)
                    }
                }

                override fun onError(t: Throwable) {
                    deferred.completeExceptionally(t)
                }
            }

        addObserver(Dispatchers.IO.asExecutor(), observer)

        return DeferredCompletingObserver(observer, deferred)
    }
}
