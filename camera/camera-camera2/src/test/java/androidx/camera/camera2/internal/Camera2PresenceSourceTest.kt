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

package androidx.camera.camera2.internal

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.MainThreadAsyncHandler
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2PresenceSourceTest {

    /** A fake implementation of [CameraManagerCompat] for direct control in tests. */
    private class FakeCameraManagerCompat : CameraManagerCompat.CameraManagerCompatImpl {

        private val mCameraManagerImpl =
            CameraManagerCompat.CameraManagerCompatImpl.from(
                ApplicationProvider.getApplicationContext(),
                MainThreadAsyncHandler.getInstance(),
            )

        var cameraIds: Array<String> = emptyArray()
        var availabilityCallback: CameraManager.AvailabilityCallback? = null
        val getCameraIdListCallCount = AtomicInteger(0)
        var shouldThrowException = false

        override fun getCameraIdList(): Array<String> {
            getCameraIdListCallCount.incrementAndGet()
            if (shouldThrowException) {
                throw CameraAccessExceptionCompat(CameraAccessExceptionCompat.CAMERA_ERROR)
            }
            return cameraIds
        }

        override fun registerAvailabilityCallback(
            executor: Executor,
            callback: CameraManager.AvailabilityCallback,
        ) {
            this.availabilityCallback = callback
        }

        override fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
            if (this.availabilityCallback == callback) {
                this.availabilityCallback = null
            }
        }

        // --- Unused methods for this test ---
        override fun getCameraManager(): CameraManager = mCameraManagerImpl.cameraManager

        override fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
            throw UnsupportedOperationException("Not implemented for this test.")
        }

        override fun openCamera(
            cameraId: String,
            executor: Executor,
            callback: CameraDevice.StateCallback,
        ) {
            throw UnsupportedOperationException("Not implemented for this test.")
        }

        override fun getConcurrentCameraIds(): MutableSet<MutableSet<String>> {
            throw UnsupportedOperationException("Not implemented for this test.")
        }
    }

    private lateinit var fakeCameraManager: FakeCameraManagerCompat
    private lateinit var camera2PresenceSource: Camera2PresenceSource
    private val directExecutor = MoreExecutors.directExecutor()

    private val id1 = CameraIdentifier.create("1")
    private val id2 = CameraIdentifier.create("2")

    @Before
    fun setUp() {
        fakeCameraManager = FakeCameraManagerCompat()
        camera2PresenceSource =
            Camera2PresenceSource(CameraManagerCompat.from(fakeCameraManager), directExecutor)
    }

    @Test
    fun startMonitoring_registersCallbackAndFetchesInitialData() {
        // Arrange
        fakeCameraManager.cameraIds = arrayOf("1")

        // Act
        camera2PresenceSource.startMonitoring()

        // Assert
        // Verify callback is registered
        assertThat(fakeCameraManager.availabilityCallback).isNotNull()
        // Verify initial fetch is triggered
        assertThat(fakeCameraManager.getCameraIdListCallCount.get()).isEqualTo(1)
    }

    @Test
    fun stopMonitoring_unregistersCallback() {
        // Arrange
        camera2PresenceSource.startMonitoring()
        assertThat(fakeCameraManager.availabilityCallback).isNotNull()

        // Act
        camera2PresenceSource.stopMonitoring()

        // Assert
        assertThat(fakeCameraManager.availabilityCallback).isNull()
    }

    @Test
    fun availabilityCallback_onCameraAvailable_triggersFetch() {
        // Arrange
        fakeCameraManager.cameraIds = arrayOf("1")
        camera2PresenceSource.startMonitoring()
        assertThat(fakeCameraManager.getCameraIdListCallCount.get()).isEqualTo(1)
        val callback = fakeCameraManager.availabilityCallback!!

        // Act
        fakeCameraManager.cameraIds = arrayOf("1", "2")
        callback.onCameraAvailable("2")

        // Assert: A new fetch should be triggered.
        assertThat(fakeCameraManager.getCameraIdListCallCount.get()).isEqualTo(2)
    }

    @Test
    fun fetchData_succeeds_andUpdatesData() {
        // Arrange
        fakeCameraManager.cameraIds = arrayOf("1", "2")
        val observer = FakeObserver()
        camera2PresenceSource.addObserver(directExecutor, observer)
        observer.awaitNextResult() // Consume initial empty set

        // Act
        val future = camera2PresenceSource.fetchData()

        // Assert
        val resultSet = future.get(1, TimeUnit.SECONDS)
        assertThat(resultSet).containsExactly(id1, id2)

        // The observer should also receive the updated data.
        val observerResult = observer.awaitNextResult()
        assertThat(observerResult.data).isEqualTo(resultSet)
    }

    @Test
    fun fetchData_fails_andUpdatesError() {
        // Arrange
        fakeCameraManager.shouldThrowException = true
        val observer = FakeObserver()
        camera2PresenceSource.addObserver(directExecutor, observer)
        observer.awaitNextResult() // Consume initial empty set

        // Act
        val future = camera2PresenceSource.fetchData()

        // Assert
        assertThrows(ExecutionException::class.java) { future.get(1, TimeUnit.SECONDS) }
            .hasCauseThat()
            .isInstanceOf(CameraUnavailableException::class.java)

        // The observer should also receive the error.
        val observerResult = observer.awaitNextResult()
        assertThat(observerResult.error).isInstanceOf(CameraUnavailableException::class.java)
    }

    // Helper fake observer for testing
    internal class FakeObserver : Observable.Observer<Set<CameraIdentifier>> {
        private val updates: BlockingQueue<Result> = LinkedBlockingQueue()

        data class Result(val data: Set<CameraIdentifier>?, val error: Throwable?)

        override fun onNewData(value: Set<CameraIdentifier>?) {
            // The Observable contract allows nullable T, but our source always provides non-null.
            // We check for safety, but expect non-null.
            if (value != null) {
                updates.offer(Result(value, null))
            }
        }

        override fun onError(t: Throwable) {
            updates.offer(Result(null, t))
        }

        fun awaitNextResult(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS): Result {
            val result = updates.poll(timeout, unit)
            assertThat(result).isNotNull()
            return result!!
        }
    }
}
