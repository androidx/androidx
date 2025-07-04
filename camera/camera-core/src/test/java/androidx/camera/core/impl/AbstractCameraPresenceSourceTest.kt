/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Build
import androidx.camera.core.CameraIdentifier
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AbstractCameraPresenceSourceTest {

    // A fake implementation of the abstract class for testing.
    internal class FakeCameraPresenceSource : AbstractCameraPresenceSource() {
        val isMonitoring = AtomicBoolean(false)
        var fetchDataCompleter: CallbackToFutureAdapter.Completer<Set<CameraIdentifier>>? = null

        override fun startMonitoring() {
            isMonitoring.set(true)
        }

        override fun stopMonitoring() {
            isMonitoring.set(false)
        }

        override fun fetchData(): ListenableFuture<Set<CameraIdentifier>> {
            return CallbackToFutureAdapter.getFuture { completer ->
                    fetchDataCompleter = completer
                    "FetchData for FakeCameraPresenceSource"
                }
                .apply {
                    // Fake implementation to call updateDate/updateError with the result.
                    Futures.addCallback(
                        this,
                        object : FutureCallback<Set<CameraIdentifier>> {
                            override fun onSuccess(result: Set<CameraIdentifier>) {
                                updateData(result)
                            }

                            override fun onFailure(t: Throwable) {
                                updateError(t)
                            }
                        },
                        MoreExecutors.directExecutor(),
                    )
                }
        }

        // Public methods to simulate data/error events from the source.
        fun pushData(newData: Set<CameraIdentifier>) {
            updateData(newData)
        }

        fun pushError(error: Throwable) {
            updateError(error)
        }
    }

    // A fake observer to capture results without using a mocking framework.
    internal class FakeObserver : Observable.Observer<Set<CameraIdentifier>> {
        private val updates: BlockingQueue<Result<Set<CameraIdentifier>>> = LinkedBlockingQueue()

        // Helper class to wrap results, differentiating data from errors.
        data class Result<T>(val data: T?, val error: Throwable?) {
            companion object {
                fun <T> fromData(data: T): Result<T> = Result(data, null)

                fun <T> fromError(error: Throwable): Result<T> = Result(null, error)
            }
        }

        override fun onNewData(value: Set<CameraIdentifier>?) {
            // Correctly handles the nullable parameter from the Observer interface.
            if (value != null) {
                updates.offer(Result.fromData(value))
            }
        }

        override fun onError(t: Throwable) {
            updates.offer(Result.fromError(t))
        }

        // Waits for the next update and returns it. Fails test if no update received.
        fun awaitNextResult(
            timeout: Long = 1,
            unit: TimeUnit = TimeUnit.SECONDS,
        ): Result<Set<CameraIdentifier>> {
            val result = updates.poll(timeout, unit)
            assertThat(result).isNotNull()
            return result!!
        }

        fun assertNoNewUpdate() {
            assertThat(updates).isEmpty()
        }
    }

    private lateinit var source: FakeCameraPresenceSource
    private val mainExecutor = Executors.newSingleThreadExecutor()

    private val id1 = CameraIdentifier.create("1")
    private val id2 = CameraIdentifier.create("2")
    private val initialSet = setOf(id1)
    private val updatedSet = setOf(id1, id2)

    @Before
    fun setUp() {
        source = FakeCameraPresenceSource()
    }

    @Test
    fun startMonitoring_isCalled_whenFirstObserverIsAdded() {
        // Arrange
        val observer = FakeObserver()
        assertThat(source.isMonitoring.get()).isFalse()

        // Act
        source.addObserver(mainExecutor, observer)

        // Assert
        assertThat(source.isMonitoring.get()).isTrue()
    }

    @Test
    fun stopMonitoring_isCalled_whenLastObserverIsRemoved() {
        // Arrange
        val observer1 = FakeObserver()
        val observer2 = FakeObserver()
        source.addObserver(mainExecutor, observer1)
        source.addObserver(mainExecutor, observer2)
        assertThat(source.isMonitoring.get()).isTrue()

        // Act
        source.removeObserver(observer1)
        // Assert: Still active after removing one observer
        assertThat(source.isMonitoring.get()).isTrue()

        // Act: Remove the last observer
        source.removeObserver(observer2)
        // Assert: No longer active
        assertThat(source.isMonitoring.get()).isFalse()
    }

    @Test
    fun newObserver_receivesInitialEmptyState() {
        // Arrange
        val observer = FakeObserver()

        // Act
        source.addObserver(mainExecutor, observer)

        // Assert: New observer gets the current (empty) data set immediately.
        val result = observer.awaitNextResult()
        assertThat(result.data).isEqualTo(emptySet<CameraIdentifier>())
        assertThat(result.error).isNull()
    }

    @Test
    fun newObserver_receivesLatestData() {
        // Arrange: Push data before observer is added
        source.pushData(initialSet)
        val observer = FakeObserver()

        // Act
        source.addObserver(mainExecutor, observer)

        // Assert: New observer gets the current data set immediately.
        val result = observer.awaitNextResult()
        assertThat(result.data).isEqualTo(initialSet)
    }

    @Test
    fun newObserver_receivesLatestError() {
        // Arrange: Push an error before observer is added
        val testError = RuntimeException("Test Error")
        source.pushError(testError)
        val observer = FakeObserver()

        // Act
        source.addObserver(mainExecutor, observer)

        // Assert: New observer gets the current error immediately.
        val result = observer.awaitNextResult()
        assertThat(result.error).isEqualTo(testError)
    }

    @Test
    fun updateData_notifiesExistingObservers() {
        // Arrange
        val observer = FakeObserver()
        source.addObserver(mainExecutor, observer)
        // Consume the initial notification
        observer.awaitNextResult()

        // Act
        source.pushData(initialSet)

        // Assert
        val result = observer.awaitNextResult()
        assertThat(result.data).isEqualTo(initialSet)
    }

    @Test
    fun updateData_doesNotNotify_ifDataIsUnchanged() {
        // Arrange
        val observer = FakeObserver()
        source.pushData(initialSet)
        source.addObserver(mainExecutor, observer)
        // Consume the initial notification
        observer.awaitNextResult()

        // Act: Push the exact same data again
        source.pushData(initialSet)

        // Assert: No new notifications should occur.
        observer.assertNoNewUpdate()
    }

    @Test
    fun updateError_notifiesExistingObservers() {
        // Arrange
        val observer = FakeObserver()
        source.addObserver(mainExecutor, observer)
        // Consume initial update
        observer.awaitNextResult()
        val testError = RuntimeException("Test Error")

        // Act
        source.pushError(testError)

        // Assert
        val result = observer.awaitNextResult()
        assertThat(result.error).isEqualTo(testError)
    }

    @Test
    fun fetchData_completesAndUpdatesState() {
        // Arrange
        val observer = FakeObserver()
        source.addObserver(mainExecutor, observer)
        // Consume initial update
        observer.awaitNextResult()

        // Act
        val future = source.fetchData()
        assertThat(source.fetchDataCompleter).isNotNull()
        source.fetchDataCompleter!!.set(updatedSet)

        // Assert
        // 1. The future completes with the data
        val futureResult = future.get(1, TimeUnit.SECONDS)
        assertThat(futureResult).isEqualTo(updatedSet)

        // 2. The observer is notified of the state change
        val observerResult = observer.awaitNextResult()
        assertThat(observerResult.data).isEqualTo(updatedSet)
    }

    @Test
    fun fetchData_failsAndUpdatesState() {
        // Arrange
        val observer = FakeObserver()
        source.addObserver(mainExecutor, observer)
        // Consume initial update
        observer.awaitNextResult()
        val testError = RuntimeException("Fetch Failed")

        // Act
        val future = source.fetchData()
        assertThat(source.fetchDataCompleter).isNotNull()
        source.fetchDataCompleter!!.setException(testError)

        // Assert
        // 1. The future fails with the exception
        assertThrows<ExecutionException> { future.get(1, TimeUnit.SECONDS) }
            .hasCauseThat()
            .isEqualTo(testError)

        // 2. The observer is notified of the error
        val observerResult = observer.awaitNextResult()
        assertThat(observerResult.error).isEqualTo(testError)
    }
}
