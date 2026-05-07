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

package androidx.camera.camera2.pipe.compat

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.internal.CriticalCameraErrorListener
import androidx.camera.camera2.pipe.testing.FakeCamera2MetadataProvider
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M)
@OptIn(ExperimentalCoroutinesApi::class)
internal class PruningCamera2DeviceManagerImplTest {
    private val testScope = TestScope()

    private val fakeContext: Context = mock()
    private val fakePermissions = Permissions(fakeContext)
    private val fakeCamera2DeviceCloser: Camera2DeviceCloser = mock()
    private val fakeThreads =
        FakeThreads.fromDispatcher(StandardTestDispatcher(testScope.testScheduler))
    private val cameraId0 = CameraId("0")
    private val cameraId1 = CameraId("1")
    private val fakeCameraDevice0: CameraDevice = mock()
    private val fakeCameraDevice1: CameraDevice = mock()
    private val fakeTimeSource: TimeSource = mock()
    private val fakeCameraErrorListener: CameraErrorListener = mock()
    val fakeAudioRestrictionController: AudioRestrictionController = mock()

    private val fakeRetryingCameraStateOpener =
        object : RetryingCameraStateOpener {
            var androidCameraStates = mutableListOf<AndroidCameraState>()

            override suspend fun openCameraWithRetry(
                cameraId: CameraId,
                camera2DeviceCloser: Camera2DeviceCloser,
                isForegroundObserver: (Unit) -> Boolean,
            ): OpenCameraResult {
                val fakeCameraMetadata = FakeCameraMetadata(cameraId = cameraId)
                val fakeCamera2MetadataProvider =
                    FakeCamera2MetadataProvider(mapOf(cameraId to fakeCameraMetadata))
                val fakeCamera2Quirks =
                    Camera2Quirks(fakeCamera2MetadataProvider, StrictMode(false))
                val fakeAndroidCameraState =
                    AndroidCameraState(
                        cameraId,
                        fakeCameraMetadata,
                        1,
                        TimestampNs(0L),
                        fakeTimeSource,
                        fakeCameraErrorListener,
                        fakeCamera2DeviceCloser,
                        fakeCamera2Quirks,
                        fakeCamera2SystemState,
                        fakeThreads,
                        fakeAudioRestrictionController,
                    )
                androidCameraStates.add(fakeAndroidCameraState)
                return OpenCameraResult(fakeAndroidCameraState, null)
            }

            override fun openAndAwaitCameraWithRetry(
                cameraId: CameraId,
                camera2DeviceCloser: Camera2DeviceCloser,
            ): AwaitOpenCameraResult {
                TODO("Not yet implemented")
            }

            override fun cancelOpen() {
                TODO("Not yet implemented")
            }
        }
    private val fakeCriticalCameraErrorListener: CriticalCameraErrorListener = mock()
    private val criticalCameraErrorListenerProvider = Provider { fakeCriticalCameraErrorListener }
    private val fakeCamera2ErrorProcessor =
        Camera2ErrorProcessor(criticalCameraErrorListenerProvider)
    private val fakeCamera2SystemState =
        Camera2SystemState(CameraPipe.CameraInteropConfig(), fakeThreads)

    private val deviceManager =
        PruningCamera2DeviceManager(
            fakePermissions,
            fakeRetryingCameraStateOpener,
            fakeCamera2DeviceCloser,
            fakeCamera2ErrorProcessor,
            fakeCamera2SystemState,
            fakeThreads,
        )

    private val fakeGraphListener1: GraphListener = mock()
    private val fakeGraphListener2: GraphListener = mock()
    private val fakeGraphListener3: GraphListener = mock()
    private val fakeGraphListener4: GraphListener = mock()

    init {
        whenever(fakeContext.checkSelfPermission(any())).thenReturn(PERMISSION_GRANTED)
        whenever(fakeCameraDevice0.id).thenReturn(cameraId0.value)
        whenever(fakeCameraDevice1.id).thenReturn(cameraId1.value)
    }

    @Test
    fun canOpenAndCloseCamera() =
        testScope.runTest {
            deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()

            deviceManager.close(cameraId0)
            advanceUntilIdle()

            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun cameraIsClosedAfterDisconnect() =
        testScope.runTest {
            val virtualCamera =
                checkNotNull(
                    deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
                )
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()

            virtualCamera.disconnect()
            advanceUntilIdle()

            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun cameraIsReusedWhenTheSameCameraIsOpened() =
        testScope.runTest {
            val virtualCamera1 =
                checkNotNull(
                    deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
                )
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)

            // Simulate camera graph close by disconnecting the virtual camera.
            virtualCamera1.disconnect()

            // Simulate a small delay for capture session switching, but short enough to keep the
            // camera opened.
            advanceTimeBy(100)

            // Now open the same camera again.
            val virtualCamera2 =
                checkNotNull(
                    deviceManager.open(cameraId0, emptyList(), fakeGraphListener2, false) { true }
                )
            advanceUntilIdle()

            // The first virtual camera should be disconnected.
            val virtualCameraState1 = virtualCamera1.value
            assertThat(virtualCameraState1).isInstanceOf(CameraStateClosed::class.java)

            // There shouldn't be any new camera open calls, and thus we should be reusing the same
            // Android camera state.
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
        }

    @Test
    fun cameraIsNotReusedWhenTheSameCameraIsOpenedAfterLongDelay() =
        testScope.runTest {
            val virtualCamera1 =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera1).isNotNull()
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)

            // Simulate camera graph close by disconnecting the virtual camera.
            virtualCamera1!!.disconnect()

            // Simulate a long delay such that the camera should be closed.
            advanceTimeBy(3000)

            // Now open the same camera again.
            val virtualCamera2 =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener2, false) { true }
            assertThat(virtualCamera2).isNotNull()
            advanceUntilIdle()

            // The first virtual camera should be disconnected.
            val virtualCameraState = virtualCamera1.value
            assertThat(virtualCameraState).isInstanceOf(CameraStateClosed::class.java)

            // Given the long delay, we should be reopening the camera, and thus we would have 2
            // camera open calls with 2 Android camera states.
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
        }

    @Test
    fun cameraIsClosedOnlyOnceWhenMultipleRequestClose() =
        testScope.runTest {
            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)
            advanceUntilIdle()

            deviceManager.close(cameraId0)
            advanceUntilIdle()

            deviceManager.closeAll()
            advanceUntilIdle()

            deviceManager.close(cameraId0)
            advanceUntilIdle()

            verify(fakeCamera2DeviceCloser, times(1))
                .closeCamera(
                    any(),
                    eq(fakeCameraDevice0),
                    eq(androidCameraState),
                    any(),
                    any(),
                    any(),
                )
        }

    @Test
    fun closingUnopenedCameraIsIgnored() =
        testScope.runTest {
            val virtualCamera =
                checkNotNull(
                    deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
                )
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)
            advanceUntilIdle()

            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateOpen::class.java)

            // Close camera 1, which was never opened.
            deviceManager.close(cameraId1)
            advanceUntilIdle()

            // Camera 0 should remain open.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateOpen::class.java)
        }

    @Test
    fun openingDifferentCameraClosesThePreviousOne() =
        testScope.runTest {
            val virtualCamera1 =
                checkNotNull(
                    deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
                )
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState1 = fakeRetryingCameraStateOpener.androidCameraStates.first()
            assertThat(androidCameraState1.cameraId).isEqualTo(cameraId0)

            // Now open a different camera. To do so, the previous camera would have to be closed.
            val virtualCamera2 =
                checkNotNull(
                    deviceManager.open(cameraId1, emptyList(), fakeGraphListener2, false) { true }
                )
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            assertThat(androidCameraState2.cameraId).isEqualTo(cameraId1)

            // The previous camera should be closed.
            assertThat(androidCameraState1.state.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun canPrewarmCamera() =
        testScope.runTest {
            // Test to make sure the camera prewarmed is reused in a later regular open request.
            deviceManager.prewarm(cameraId0)

            // Advance time by a little bit to complete the prewarm processing request.
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            // Advance time by a little bit to allow camera open processing to finish.
            advanceTimeBy(100)

            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            advanceUntilIdle()

            // Verify we get an opened camera and we didn't open the camera twice.
            assertThat(virtualCamera!!.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
        }

    @Test
    fun prewarmDoesNotDisconnectPriorRequestOpen() =
        testScope.runTest {
            // Test to make sure that cameras are not disconnected by a later prewarm request.
            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)

            // The prewarm request should not disconnect the virtual camera.
            assertThat(virtualCamera!!.value).isNotInstanceOf(CameraStateClosed::class.java)

            // Now provide an opened camera.
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)
            advanceUntilIdle()

            // Verify the camera is opened successfully.
            assertThat(virtualCamera.value).isInstanceOf(CameraStateOpen::class.java)

            // Now disconnect the virtual camera. The camera should still close eventually.
            virtualCamera.disconnect()
            advanceUntilIdle()
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun prewarmedCameraShouldBeClosedEventually() =
        testScope.runTest {
            // Test to make sure we do close the camera eventually if a prewarmed camera went
            // unused after a period of time.
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            advanceTimeBy(100)
            // Verify the camera is still open due to prewarm.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)

            advanceUntilIdle()
            // Make sure the camera is closed eventually.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun shouldReopenCameraWhenOpenComesTooLateAfterPrewarm() =
        testScope.runTest {
            // Test to verify when we open a camera later than expected after
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            advanceTimeBy(100)
            // Verify the camera is still open due to prewarm.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)

            advanceTimeBy(5000)
            // Make sure the camera is closed.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)

            // Now open the same camera.
            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            advanceTimeBy(100)

            // Verify we do open the camera again.
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState2.onOpened(fakeCameraDevice0)

            advanceTimeBy(100)
            // Verify that we opened the camera successfully.
            assertThat(androidCameraState2.state.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(virtualCamera!!.value).isInstanceOf(CameraStateOpen::class.java)

            // Now disconnect the virtual camera. The camera should be closed eventually.
            virtualCamera.disconnect()
            advanceUntilIdle()
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun prewarmedAndOpenedCameraShouldBeClosedWhenDisconnect() =
        testScope.runTest {
            // Test to make sure a prewarmed and later opened camera should be closed when the
            // virtual camera disconnects.
            deviceManager.prewarm(cameraId0)

            // Advance time by a little bit to complete the prewarm processing request.
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            // Advance time by a little bit to allow camera open processing to finish.
            advanceTimeBy(100)
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)

            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            advanceUntilIdle()

            // Verify we get an opened camera.
            assertThat(virtualCamera!!.value).isInstanceOf(CameraStateOpen::class.java)

            // Now disconnect the virtual camera, which should release the token and close the
            // camera after a while.
            virtualCamera.disconnect()
            advanceUntilIdle()

            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun canPrewarmCameraMultipleTimes() =
        testScope.runTest {
            // Test to make sure the camera can be prewarmed multiple times before the camera is
            // actually opened.
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            // Advance time by a little bit to allow camera open processing to finish.
            advanceTimeBy(100)
            // The prewarm requests should result in an opened camera.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)

            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()
            advanceUntilIdle()

            // Verify we get an opened camera and we didn't open the camera twice.
            assertThat(virtualCamera!!.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)

            // Now disconnect the virtual camera.
            virtualCamera.disconnect()
            advanceUntilIdle()
            // Verify the camera is closed.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun prewarmDoesNotDisconnectCameraAfterPrewarmAndOpen() =
        testScope.runTest {
            // Test to make sure the camera can be prewarmed multiple times before the camera is
            // actually opened.
            deviceManager.prewarm(cameraId0)
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState.onOpened(fakeCameraDevice0)

            // Advance time by a little bit to allow camera open processing to finish.
            advanceTimeBy(100)
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateOpen::class.java)

            // Open the camera.
            val virtualCamera =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera).isNotNull()

            // Simulate a prewarm that is sent immediately after.
            deviceManager.prewarm(cameraId0)
            advanceUntilIdle()

            // Verify we get an opened camera and we didn't open the camera twice.
            assertThat(virtualCamera!!.value).isInstanceOf(CameraStateOpen::class.java)
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)

            // Now disconnect the virtual camera.
            virtualCamera.disconnect()
            advanceUntilIdle()
            // Verify the camera is closed.
            assertThat(androidCameraState.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(virtualCamera.value).isInstanceOf(CameraStateClosed::class.java)
        }

    @Test
    fun allCamerasShouldBeOpenedBeforeConnectingWhenOpeningConcurrentCameras() =
        testScope.runTest {
            val virtualCamera1 =
                deviceManager.open(cameraId0, listOf(cameraId1), fakeGraphListener1, false) { true }
            assertThat(virtualCamera1).isNotNull()
            // Advance time by just a bit to allow coroutines to finish.
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState1 = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState1.onOpened(fakeCameraDevice0)
            advanceTimeBy(100)

            // Since camera 1 is not yet opened, the virtual camera should not be connected yet.
            var virtualCameraState1 = virtualCamera1!!.value
            assertThat(virtualCameraState1).isNotInstanceOf(CameraStateOpen::class.java)

            val virtualCamera2 =
                deviceManager.open(cameraId1, listOf(cameraId0), fakeGraphListener2, false) { true }
            assertThat(virtualCamera2).isNotNull()
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState2.onOpened(fakeCameraDevice1)
            advanceUntilIdle()

            virtualCameraState1 = virtualCamera1.value
            assertThat(virtualCameraState1).isInstanceOf(CameraStateOpen::class.java)
            val virtualCameraState2 = virtualCamera2!!.value
            assertThat(virtualCameraState2).isInstanceOf(CameraStateOpen::class.java)
        }

    @Test
    fun pendingCamerasShouldBeHeldWhenOpeningConcurrentCameras() =
        testScope.runTest {
            val virtualCamera1 =
                deviceManager.open(cameraId0, listOf(cameraId1), fakeGraphListener1, false) { true }
            assertThat(virtualCamera1).isNotNull()
            // Advance until idle. This is the only but notable difference between this and the
            // prior test. Note that here because the request is still pending, the active camera
            // should not be closed.
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState1 = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState1.onOpened(fakeCameraDevice0)
            advanceUntilIdle()

            // Since camera 1 is not yet opened, the virtual camera should not be connected yet.
            var virtualCameraState1 = virtualCamera1!!.value
            assertThat(virtualCameraState1).isNotInstanceOf(CameraStateOpen::class.java)

            val virtualCamera2 =
                deviceManager.open(cameraId1, listOf(cameraId0), fakeGraphListener2, false) { true }
            assertThat(virtualCamera2).isNotNull()
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState2.onOpened(fakeCameraDevice1)
            advanceUntilIdle()

            virtualCameraState1 = virtualCamera1.value
            assertThat(virtualCameraState1).isInstanceOf(CameraStateOpen::class.java)
            val virtualCameraState2 = virtualCamera2!!.value
            assertThat(virtualCameraState2).isInstanceOf(CameraStateOpen::class.java)
        }

    @Test
    fun singleCameraShouldBeClosedWhenConcurrentCamerasAreRequested() =
        testScope.runTest {
            // First open camera 0 in regular (single) camera mode.
            val virtualCamera1 =
                deviceManager.open(cameraId0, emptyList(), fakeGraphListener1, false) { true }
            assertThat(virtualCamera1).isNotNull()
            advanceUntilIdle()

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState1 = fakeRetryingCameraStateOpener.androidCameraStates.first()
            assertThat(androidCameraState1.cameraId).isEqualTo(cameraId0)

            // Now open camera 0, with camera 1 as a shared camera.
            val virtualCamera2 =
                deviceManager.open(cameraId0, listOf(cameraId1), fakeGraphListener2, false) { true }
            assertThat(virtualCamera2).isNotNull()
            advanceTimeBy(100)

            // Even though we request the same camera, the single camera 0 cannot be reused, and
            // should thus be closed.
            assertThat(androidCameraState1.state.value).isInstanceOf(CameraStateClosed::class.java)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState2.onOpened(fakeCameraDevice0)
            advanceTimeBy(100)

            val virtualCamera3 =
                deviceManager.open(cameraId1, listOf(cameraId0), fakeGraphListener3, false) { true }
            assertThat(virtualCamera3).isNotNull()
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(3)
            val androidCameraState3 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState3.onOpened(fakeCameraDevice1)
            advanceUntilIdle()

            // We should still succeed in opening the concurrent cameras.
            val virtualCameraState2 = virtualCamera2!!.value
            assertThat(virtualCameraState2).isInstanceOf(CameraStateOpen::class.java)
            val virtualCameraState3 = virtualCamera3!!.value
            assertThat(virtualCameraState3).isInstanceOf(CameraStateOpen::class.java)
        }

    @Test
    fun onlyOneCameraIsClosedWhenGoingFromConcurrentCameraToSingleCamera() =
        testScope.runTest {
            // First, open concurrent cameras with camera 0 and camera1.
            val virtualCamera1 =
                deviceManager.open(cameraId0, listOf(cameraId1), fakeGraphListener1, false) { true }
            assertThat(virtualCamera1).isNotNull()
            // Advance time by just a bit to allow coroutines to finish.
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(1)
            val androidCameraState1 = fakeRetryingCameraStateOpener.androidCameraStates.first()
            androidCameraState1.onOpened(fakeCameraDevice0)
            advanceTimeBy(100)

            val virtualCamera2 =
                deviceManager.open(cameraId1, listOf(cameraId0), fakeGraphListener2, false) { true }
            assertThat(virtualCamera2).isNotNull()
            advanceTimeBy(100)

            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)
            val androidCameraState2 = fakeRetryingCameraStateOpener.androidCameraStates.last()
            androidCameraState2.onOpened(fakeCameraDevice1)
            advanceUntilIdle()

            // Then, open camera 1 in regular (single) camera mode.
            val virtualCamera3 =
                deviceManager.open(cameraId1, emptyList(), fakeGraphListener3, false) { true }
            assertThat(virtualCamera3).isNotNull()
            advanceUntilIdle()
            assertThat(virtualCamera3!!.value).isInstanceOf(CameraStateOpen::class.java)

            // No new cameras should be opened
            assertThat(fakeRetryingCameraStateOpener.androidCameraStates.size).isEqualTo(2)

            // Camera 0 should be closed, while camera 1 should remain open.
            assertThat(androidCameraState1.state.value).isInstanceOf(CameraStateClosed::class.java)
            assertThat(androidCameraState2.state.value).isInstanceOf(CameraStateOpen::class.java)
        }

    @Test
    fun prunePrioritizesRequestCloseAndOrdersAreRetained() =
        testScope.runTest {
            val requestOpen1 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1)
            val requestClose1 = createFakeRequestClose(cameraId0)
            var requestList =
                mutableListOf<CameraRequest>(RequestCloseById(cameraId0), requestClose1)
            deviceManager.prune(requestList)
            assertThat(requestList.first()).isEqualTo(requestClose1)

            val requestOpen2 = createFakeRequestOpen(cameraId1, emptyList(), fakeGraphListener2)
            val requestClose2 = createFakeRequestClose(cameraId1)
            requestList =
                mutableListOf<CameraRequest>(
                    requestOpen1,
                    RequestCloseById(cameraId0),
                    RequestCloseById(cameraId1),
                    requestOpen2,
                    requestClose1,
                    requestClose2,
                )
            deviceManager.prune(requestList)
            assertThat(requestList[0]).isEqualTo(requestClose1)
            assertThat(requestList[1]).isEqualTo(requestClose2)
        }

    @Test
    fun pruneRequestCloseAllSupersedesAllRequests() =
        testScope.runTest {
            val requestList =
                mutableListOf<CameraRequest>(
                    createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1),
                    RequestCloseById(cameraId0),
                    createFakeRequestOpen(cameraId1, emptyList(), fakeGraphListener2),
                    createFakeRequestClose(cameraId1),
                    RequestCloseAll(),
                    RequestCloseAll(),
                    createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1),
                )
            deviceManager.prune(requestList)
            assertThat(requestList[0]).isInstanceOf(RequestCloseAll::class.java)
            // The former RequestCloseAll should be superseded, and thus we should only have one.
            assertThat(requestList[1]).isNotInstanceOf(RequestCloseAll::class.java)
        }

    @Test
    fun pruneMultipleRequestOpensWithSameCameraId() =
        testScope.runTest {
            val requestOpen1 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1)
            val requestOpen2 =
                createFakeRequestOpen(cameraId0, listOf(cameraId1), fakeGraphListener2)
            val requestOpen3 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener3)
            var requestList =
                mutableListOf<CameraRequest>(
                    RequestCloseById(cameraId1),
                    requestOpen1,
                    createFakeRequestClose(cameraId1),
                    requestOpen2,
                    requestOpen3,
                )
            deviceManager.prune(requestList)
            val remainingRequestOpens = requestList.filterIsInstance<RequestOpen>()
            assertThat(remainingRequestOpens.size).isEqualTo(1)
            assertThat(remainingRequestOpens.first()).isEqualTo(requestOpen3)
        }

    @Test
    fun pruneMultipleRequestOpensWithDifferentCameraId() =
        testScope.runTest {
            val requestOpen1 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1)
            val requestOpen2 = createFakeRequestOpen(cameraId1, emptyList(), fakeGraphListener2)
            val requestOpen3 =
                createFakeRequestOpen(cameraId1, listOf(cameraId0), fakeGraphListener3)
            val requestOpen4 =
                createFakeRequestOpen(cameraId0, listOf(cameraId1), fakeGraphListener4)
            val requestList =
                mutableListOf<CameraRequest>(
                    RequestCloseById(cameraId1),
                    requestOpen1,
                    createFakeRequestClose(cameraId1),
                    requestOpen2,
                    requestOpen3,
                    requestOpen4,
                )
            deviceManager.prune(requestList)
            val remainingRequestOpens = requestList.filterIsInstance<RequestOpen>()
            // 1. requestOpen1 should be pruned by requestOpen2 since their camera IDs are
            //    different, and they don't share the set of concurrent cameras.
            // 2. requestOpen2 should be pruned by requestOpen3 since their camera IDs are the same.
            // 3. requestOpen3 and requestOpen4 should both be kept since they same the set of
            //    concurrent cameras.
            assertThat(remainingRequestOpens.size).isEqualTo(2)
            assertThat(remainingRequestOpens[0]).isEqualTo(requestOpen3)
            assertThat(remainingRequestOpens[1]).isEqualTo(requestOpen4)
        }

    @Test
    fun pruneRequestCloseById() =
        testScope.runTest {
            val requestList =
                mutableListOf<CameraRequest>(
                    createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1),
                    RequestCloseById(cameraId0),
                    createFakeRequestOpen(cameraId1, listOf(cameraId0), fakeGraphListener2),
                    createFakeRequestOpen(cameraId0, listOf(cameraId1), fakeGraphListener3),
                    RequestCloseById(cameraId1),
                )
            deviceManager.prune(requestList)
            // 1. The first RequestOpen should be pruned by RequestCloseById(cameraId0)
            // 2. The latter RequestOpen for concurrent cameras should be pruned altogether by
            //    RequestCloseById(cameraId1), because by closing cameraId1, neither would succeed.
            assertThat(requestList.size).isEqualTo(2)
            assertThat(requestList[0]).isInstanceOf(RequestCloseById::class.java)
            assertThat((requestList[0] as RequestCloseById).activeCameraId).isEqualTo(cameraId0)
            assertThat(requestList[1]).isInstanceOf(RequestCloseById::class.java)
            assertThat((requestList[1] as RequestCloseById).activeCameraId).isEqualTo(cameraId1)
        }

    @Test
    fun PrewarmShouldNotPruneRegularRequestOpens() =
        testScope.runTest {
            val fakeRequestOpen1 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1)
            val fakeRequestOpen2 =
                createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener2, isPrewarm = true)

            val requestList = mutableListOf<CameraRequest>(fakeRequestOpen1, fakeRequestOpen2)
            deviceManager.prune(requestList)
            // The latter RequestOpen is a prewarm, and should thus not prune the former RequestOpen
            assertThat(requestList.size).isEqualTo(2)
            assertThat(requestList[0]).isEqualTo(fakeRequestOpen1)
            assertThat(requestList[1]).isEqualTo(fakeRequestOpen2)
        }

    @Test
    fun PrewarmShouldBePrunedByAnyRequestOpen() =
        testScope.runTest {
            val fakeRequestOpen1 =
                createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1, isPrewarm = true)
            val fakeRequestOpen2 = createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener2)
            val fakeRequestOpen3 =
                createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener3, isPrewarm = true)

            var requestList = mutableListOf<CameraRequest>(fakeRequestOpen1, fakeRequestOpen2)
            deviceManager.prune(requestList)
            // If we have a prewarm request with the same camera that hasn't be processed, it should
            // be pruned.
            assertThat(requestList.size).isEqualTo(1)
            assertThat(requestList.first()).isEqualTo(fakeRequestOpen2)

            requestList = mutableListOf<CameraRequest>(fakeRequestOpen1, fakeRequestOpen3)
            deviceManager.prune(requestList)
            // If we have consecutive prewarm requests for the same camera, the former can be pruned
            assertThat(requestList.size).isEqualTo(1)
            assertThat(requestList.first()).isEqualTo(fakeRequestOpen3)
        }

    @Test
    fun deferredStillCompletesForPrunedRequests() =
        testScope.runTest {
            val requestCloseById1 = RequestCloseById(cameraId0)
            val requestCloseById2 = RequestCloseById(cameraId1)
            val requestCloseById3 = RequestCloseById(cameraId1)
            val requestCloseById4 = RequestCloseById(cameraId1)
            val requestCloseAll1 = RequestCloseAll()
            val requestCloseAll2 = RequestCloseAll()
            val requestCloseAll3 = RequestCloseAll()
            val requestList =
                mutableListOf<CameraRequest>(
                    createFakeRequestOpen(cameraId0, emptyList(), fakeGraphListener1),
                    requestCloseById1,
                    requestCloseAll1,
                    requestCloseAll2,
                    requestCloseAll3,
                    createFakeRequestOpen(cameraId1, emptyList(), fakeGraphListener1),
                    requestCloseById2,
                    requestCloseById3,
                    requestCloseById4,
                )
            deviceManager.prune(requestList)
            assertThat(requestList).isEqualTo(listOf(requestCloseAll3, requestCloseById4))

            advanceUntilIdle()
            assertThat(requestCloseById1.deferred.isCompleted).isFalse()
            assertThat(requestCloseById2.deferred.isCompleted).isFalse()
            assertThat(requestCloseById3.deferred.isCompleted).isFalse()
            assertThat(requestCloseById4.deferred.isCompleted).isFalse()
            assertThat(requestCloseAll1.deferred.isCompleted).isFalse()
            assertThat(requestCloseAll2.deferred.isCompleted).isFalse()
            assertThat(requestCloseAll3.deferred.isCompleted).isFalse()

            // Simulate completion of requestCloseAll3, which should also complete all requests it
            // has pruned previously.
            requestCloseAll3.deferred.complete(Unit)
            advanceUntilIdle()
            assertThat(requestCloseById1.deferred.isCompleted).isTrue()
            assertThat(requestCloseAll1.deferred.isCompleted).isTrue()
            assertThat(requestCloseAll2.deferred.isCompleted).isTrue()

            // Simulate completion of requestCloseById4, which should also complete all requests it
            // has pruned previously.
            requestCloseById4.deferred.complete(Unit)
            advanceUntilIdle()
            assertThat(requestCloseById2.deferred.isCompleted).isTrue()
            assertThat(requestCloseById3.deferred.isCompleted).isTrue()
        }

    private fun createFakeRequestOpen(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean = false,
    ): RequestOpen {
        val virtualCamera = VirtualCameraState(cameraId, graphListener, fakeThreads.cameraPipeScope)
        return RequestOpen(virtualCamera, sharedCameraIds, graphListener, isPrewarm, { true })
    }

    private fun createFakeRequestClose(
        cameraId: CameraId,
        allCameraIds: Set<CameraId> = setOf(cameraId),
    ): RequestClose {
        val fakeCameraMetadata = FakeCameraMetadata(cameraId = cameraId)
        val fakeCamera2MetadataProvider =
            FakeCamera2MetadataProvider(mapOf(cameraId to fakeCameraMetadata))
        val fakeCamera2Quirks = Camera2Quirks(fakeCamera2MetadataProvider, StrictMode(false))
        val fakeAndroidCameraState =
            AndroidCameraState(
                cameraId,
                fakeCameraMetadata,
                1,
                TimestampNs(0),
                fakeTimeSource,
                fakeCameraErrorListener,
                fakeCamera2DeviceCloser,
                fakeCamera2Quirks,
                fakeCamera2SystemState,
                fakeThreads,
                fakeAudioRestrictionController,
            )
        val fakeActiveCamera =
            ActiveCamera(fakeAndroidCameraState, allCameraIds, fakeThreads.cameraPipeScope) {}
        return RequestClose(fakeActiveCamera)
    }
}
