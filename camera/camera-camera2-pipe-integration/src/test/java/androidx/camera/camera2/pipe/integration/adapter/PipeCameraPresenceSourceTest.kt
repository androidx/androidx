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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.impl.Observable
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@ExperimentalCoroutinesApi
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class PipeCameraPresenceSourceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var idFlow: MutableSharedFlow<List<CameraId>>
    private lateinit var pipeCameraPresenceSource: PipeCameraPresenceSource
    private lateinit var mockContext: Context
    private lateinit var mockCameraManager: CameraManager

    private val id1 = CameraIdentifier.create("1")
    private val id2 = CameraIdentifier.create("2")
    private val pipeId1 = CameraId.fromCamera2Id("1")
    private val pipeId2 = CameraId.fromCamera2Id("2")

    @Before
    fun setUp() {
        idFlow = MutableSharedFlow()

        mockContext = mock(Context::class.java)
        mockCameraManager = mock(CameraManager::class.java)
        `when`(mockContext.getSystemService(Context.CAMERA_SERVICE)).thenReturn(mockCameraManager)

        pipeCameraPresenceSource = PipeCameraPresenceSource(idFlow, testScope, mockContext)
    }

    @After
    fun tearDown() {
        pipeCameraPresenceSource.stopMonitoring()
    }

    @Test
    fun startMonitoring_collectsFlow_andUpdatesData() =
        testScope.runTest {
            val observer = FakeObserver()
            try {
                // Arrange
                pipeCameraPresenceSource.addObserver(testDispatcher.asExecutor(), observer)
                observer.awaitNextResult() // Consume initial empty set

                // Act
                idFlow.emit(listOf(pipeId1))

                // Assert
                val result = observer.awaitNextResult()
                assertThat(result.data).containsExactly(id1)

                // Act: Emit another update
                idFlow.emit(listOf(pipeId1, pipeId2))

                // Assert
                val result2 = observer.awaitNextResult()
                assertThat(result2.data).containsExactly(id1, id2)
            } finally {
                pipeCameraPresenceSource.stopMonitoring()
            }
        }

    @Test
    fun startMonitoring_handlesErrorInFlow() =
        testScope.runTest {
            // Arrange: Create a new source with a flow that immediately throws an error
            val testError = RuntimeException("Flow error")
            val errorFlow = flow<List<CameraId>> { throw testError }
            val errorSource = PipeCameraPresenceSource(errorFlow, testScope, mockContext)
            val observer = FakeObserver()

            try {
                // Act
                errorSource.addObserver(testDispatcher.asExecutor(), observer)

                // Assert
                val errorResult = observer.awaitNextResult()
                assertThat(errorResult.error).isEqualTo(testError)
            } finally {
                errorSource.stopMonitoring()
            }
        }

    @Test
    fun stopMonitoring_cancelsFlowCollection() =
        testScope.runTest {
            // Arrange
            val observer = FakeObserver()
            pipeCameraPresenceSource.addObserver(testDispatcher.asExecutor(), observer)
            observer.awaitNextResult()

            // Act
            pipeCameraPresenceSource.stopMonitoring()
            idFlow.emit(listOf(pipeId1))

            // Assert: No new data should be received after stopping.
            observer.assertNoNewUpdate()
        }

    @Test
    fun fetchData_succeeds() =
        testScope.runTest {
            // Arrange
            `when`(mockCameraManager.cameraIdList).thenReturn(arrayOf("1", "2"))
            val observer = FakeObserver()
            try {
                pipeCameraPresenceSource.addObserver(testDispatcher.asExecutor(), observer)
                observer.awaitNextResult() // Consume initial

                // Act
                val future = pipeCameraPresenceSource.fetchData()

                // Assert
                val resultSet = future.get(1, TimeUnit.SECONDS)
                assertThat(resultSet).containsExactly(id1, id2)

                val observerResult = observer.awaitNextResult()
                assertThat(observerResult.data).isEqualTo(resultSet)
            } finally {
                pipeCameraPresenceSource.stopMonitoring()
            }
        }

    // Helper fake observer for testing
    internal class FakeObserver : Observable.Observer<Set<CameraIdentifier>> {
        private val updates: BlockingQueue<Result> = LinkedBlockingQueue()

        data class Result(val data: Set<CameraIdentifier>?, val error: Throwable?)

        override fun onNewData(value: Set<CameraIdentifier>?) {
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

        fun assertNoNewUpdate() {
            // Give a small delay for any potential async updates to arrive
            val result = updates.poll(100, TimeUnit.MILLISECONDS)
            assertThat(result).isNull()
        }
    }
}
