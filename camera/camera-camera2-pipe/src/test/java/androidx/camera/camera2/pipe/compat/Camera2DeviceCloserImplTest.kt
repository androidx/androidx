/*
 * Copyright 2026 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.os.Build
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.EmulatorDeviceTemplate
import androidx.camera.camera2.pipe.testing.FakeAudioRestrictionController
import androidx.camera.camera2.pipe.testing.FakeCamera2MetadataProvider
import androidx.camera.camera2.pipe.testing.FakeCameraDeviceWrapper
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.common.CameraId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
@OptIn(ExperimentalCoroutinesApi::class)
internal class Camera2DeviceCloserImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val cameraId = RobolectricCameras.create {
        set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        )
    }
    private val testCamera = RobolectricCameras.open(cameraId)
    private val timeSource: TimeSource = SystemTimeSource()
    private val now = Timestamps.now(timeSource)
    private val threads = FakeThreads.fromDispatcher(testDispatcher)
    private val audioRestrictionController = FakeAudioRestrictionController()
    private val cameraErrorListener =
        object : CameraErrorListener {
            override fun onCameraError(
                cameraId: CameraId,
                cameraError: CameraError,
                willAttemptRetry: Boolean,
            ) {}
        }
    private val legacyMetadata =
        FakeCameraMetadata.fromTemplate(
            template = EmulatorDeviceTemplate,
            cameraId = cameraId,
            characteristicsOverrides =
                mapOf(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                        INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                ),
        )
    private val legacyCamera2Quirks =
        Camera2Quirks(
            FakeCamera2MetadataProvider(mapOf(cameraId to legacyMetadata)),
            StrictMode(false),
        )
    private val camera2SystemState = Camera2SystemState(CameraPipe.CameraInteropConfig(), threads)
    private val fakeRetryingCameraStateOpener = FakeRetryingCameraStateOpener()
    private val camera2DeviceCloser =
        Camera2DeviceCloserImpl(threads, fakeRetryingCameraStateOpener)

    @After
    fun teardown() {
        RobolectricCameras.clear()
    }

    @Test
    fun closeCamera_withAllQuirksDisabled_skipsReopenEmptySessionAndAwaitClosed() =
        testScope.runTest {
            val androidCameraState = createAndroidCameraState()
            val cameraDeviceWrapper =
                FakeCameraDeviceWrapper(testCamera, autoConfigureCaptureSession = true)

            camera2DeviceCloser.closeCamera(
                cameraDeviceWrapper = cameraDeviceWrapper,
                cameraDevice = testCamera.cameraDevice,
                androidCameraState = androidCameraState,
                audioRestrictionController = audioRestrictionController,
                shouldReopenCamera = false,
                shouldCreateEmptyCaptureSession = false,
                shouldWaitForCameraDeviceOnClosed = false,
            )

            assertThat(fakeRetryingCameraStateOpener.openAndAwaitCount).isEqualTo(0)
            assertThat(cameraDeviceWrapper.currentSession).isNull()
            assertThat(cameraDeviceWrapper.deviceClosingInvoked).isTrue()
            assertThat(cameraDeviceWrapper.deviceClosedInvoked).isTrue()
            assertThat(androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 0)).isFalse()
        }

    @Test
    fun closeCamera_withAllQuirksEnabled_reopensCreatesEmptySessionAndWaitsForClosed() =
        testScope.runTest {
            val reopenedCamera = RobolectricCameras.open(cameraId)
            val reopenedAndroidCameraState = createAndroidCameraState()
            reopenedAndroidCameraState.onClosed(reopenedCamera.cameraDevice)
            val reopenedCameraDeviceWrapper =
                FakeCameraDeviceWrapper(reopenedCamera, autoConfigureCaptureSession = true)
            fakeRetryingCameraStateOpener.openAndAwaitResult =
                AwaitOpenCameraResult(reopenedCameraDeviceWrapper, reopenedAndroidCameraState)

            val androidCameraState = createAndroidCameraState()
            // Signal initial camera device closed so the initial close during reopen does not wait
            // 2s.
            androidCameraState.onClosed(testCamera.cameraDevice)
            val cameraDeviceWrapper =
                FakeCameraDeviceWrapper(testCamera, autoConfigureCaptureSession = true)

            camera2DeviceCloser.closeCamera(
                cameraDeviceWrapper = cameraDeviceWrapper,
                cameraDevice = testCamera.cameraDevice,
                androidCameraState = androidCameraState,
                audioRestrictionController = audioRestrictionController,
                shouldReopenCamera = true,
                shouldCreateEmptyCaptureSession = true,
                shouldWaitForCameraDeviceOnClosed = true,
            )

            assertThat(fakeRetryingCameraStateOpener.openAndAwaitCount).isEqualTo(1)
            assertThat(cameraDeviceWrapper.currentSession).isNull()
            assertThat(reopenedCameraDeviceWrapper.currentSession).isNotNull()
            assertThat(reopenedCameraDeviceWrapper.currentSession!!.closed).isTrue()
            assertThat(cameraDeviceWrapper.deviceClosingInvoked).isTrue()
            assertThat(cameraDeviceWrapper.deviceClosedInvoked).isTrue()
            assertThat(androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 0)).isTrue()
            assertThat(reopenedAndroidCameraState.awaitCameraDeviceClosed(timeoutMillis = 0))
                .isTrue()
        }

    @Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P])
    @Test
    fun androidCameraStateCloseBeforeOpened_withRealCloser_doesNotReopenOrWaitForOnClosed() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId)
            val androidCameraState = createAndroidCameraState()

            androidCameraState.close()
            androidCameraState.onOpened(testCamera.cameraDevice)

            assertThat(fakeRetryingCameraStateOpener.openAndAwaitCount).isEqualTo(0)
            assertThat(androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 0)).isFalse()
            val closedState = androidCameraState.state.value as CameraStateClosed
            assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_CLOSED)
        }

    @Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.P])
    @Test
    fun androidCameraStateCloseDuringOnOpened_withRealCloser_doesNotReopenOrWaitForOnClosed() =
        testScope.runTest {
            camera2SystemState.onCameraOpening(cameraId)
            lateinit var androidCameraState: AndroidCameraState
            val interopStateCallback =
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        androidCameraState.close()
                    }

                    override fun onDisconnected(camera: CameraDevice) {}

                    override fun onError(camera: CameraDevice, error: Int) {}
                }
            androidCameraState =
                createAndroidCameraState(interopCameraDeviceStateCallback = interopStateCallback)

            androidCameraState.onOpened(testCamera.cameraDevice)

            assertThat(fakeRetryingCameraStateOpener.openAndAwaitCount).isEqualTo(0)
            assertThat(androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 0)).isFalse()
            val closedState = androidCameraState.state.value as CameraStateClosed
            assertThat(closedState.cameraClosedReason).isEqualTo(ClosedReason.APP_CLOSED)
        }

    private fun createAndroidCameraState(
        interopCameraDeviceStateCallback: CameraDevice.StateCallback? = null
    ): AndroidCameraState =
        AndroidCameraState(
            cameraId = cameraId,
            metadata = legacyMetadata,
            attemptNumber = 1,
            attemptTimestampNanos = now,
            timeSource = timeSource,
            cameraErrorListener = cameraErrorListener,
            camera2DeviceCloser = camera2DeviceCloser,
            camera2Quirks = legacyCamera2Quirks,
            camera2SystemState = camera2SystemState,
            threads = threads,
            audioRestrictionController = audioRestrictionController,
            interopCameraDeviceStateCallback = interopCameraDeviceStateCallback,
        )

    private class FakeRetryingCameraStateOpener : RetryingCameraStateOpener {
        /** Number of times [openAndAwaitCameraWithRetry] was invoked. */
        var openAndAwaitCount = 0
            private set

        /** Result returned by [openAndAwaitCameraWithRetry]. */
        var openAndAwaitResult: AwaitOpenCameraResult = AwaitOpenCameraResult()

        override suspend fun openCameraWithRetry(
            cameraId: CameraId,
            camera2DeviceCloser: Camera2DeviceCloser,
            isForegroundObserver: (Unit) -> Boolean,
            cameraOpenAborted: Deferred<Unit>,
        ): OpenCameraResult {
            throw UnsupportedOperationException("openCameraWithRetry is not used in this test")
        }

        override fun openAndAwaitCameraWithRetry(
            cameraId: CameraId,
            camera2DeviceCloser: Camera2DeviceCloser,
        ): AwaitOpenCameraResult {
            openAndAwaitCount++
            return openAndAwaitResult
        }

        override fun cancelOpen() {}
    }
}
