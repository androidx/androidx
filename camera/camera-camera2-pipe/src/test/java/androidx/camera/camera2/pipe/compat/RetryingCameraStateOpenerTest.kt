/*
 * Copyright 2022 The Android Open Source Project
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

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_DISABLED
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_DISCONNECTED
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_IN_USE
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_LIMIT_EXCEEDED
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_SERVICE
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_DO_NOT_DISTURB_ENABLED
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_ILLEGAL_ARGUMENT_EXCEPTION
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_SECURITY_EXCEPTION
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.FakeCamera2DeviceCloser
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeTimeSource
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RetryingCameraStateOpenerTest {
    private val cameraId0 = CameraId("0")
    private val camera2MetadataProvider =
        object : Camera2MetadataProvider {
            override suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata =
                FakeCameraMetadata(cameraId = cameraId)

            override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata =
                FakeCameraMetadata(cameraId = cameraId)
        }

    // TODO(lnishan): Consider mocking this object when Mockito works well with value classes.
    private val cameraOpener =
        object : CameraOpener {
            var toThrow: Throwable? = null
            var numberOfOpens = 0

            override fun openCamera(cameraId: CameraId, stateCallback: CameraDevice.StateCallback) {
                numberOfOpens++
                toThrow?.let { throw it }
            }
        }

    private val fakeTimeSource = FakeTimeSource()
    private val cameraDeviceCloser = FakeCamera2DeviceCloser()

    // TODO(lnishan): Consider mocking this object when Mockito works well with value classes.
    private val fakeCameraErrorListener =
        object : CameraErrorListener {
            var numberOfErrorCalls = 0

            override fun onCameraError(
                cameraId: CameraId,
                cameraError: CameraError,
                willAttemptRetry: Boolean
            ) {
                numberOfErrorCalls++
            }
        }

    private val cameraStateOpener =
        CameraStateOpener(
            cameraOpener,
            camera2MetadataProvider,
            fakeCameraErrorListener,
            cameraDeviceCloser,
            fakeTimeSource,
            cameraInteropConfig = null,
        )

    private val cameraAvailabilityMonitor =
        object : CameraAvailabilityMonitor {
            override suspend fun awaitAvailableCamera(
                cameraId: CameraId,
                timeoutMillis: Long
            ): Boolean {
                delay(timeoutMillis)
                fakeTimeSource.currentTimestamp += DurationNs.fromMs(timeoutMillis)
                return true
            }
        }

    private val fakeDevicePolicyManager: DevicePolicyManagerWrapper = mock()

    private val retryingCameraStateOpener =
        RetryingCameraStateOpener(
            cameraStateOpener,
            fakeCameraErrorListener,
            cameraAvailabilityMonitor,
            fakeTimeSource,
            fakeDevicePolicyManager,
        )

    @Test
    fun testShouldRetryReturnsTrueWithinTimeout() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_IN_USE,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryReturnsFalseWhenTimeoutExpires() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_IN_USE,
                1,
                DurationNs(30_000_000_000L), // 30 seconds
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isFalse()
    }

    @Test
    fun testShouldRetryShouldFailUndetermined() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_UNDETERMINED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isFalse()
    }

    @Test
    fun testShouldRetryCameraInUse() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_IN_USE,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // The second retry attempt should fail if SDK version < S, and succeed otherwise.
        val secondRetry =
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_IN_USE,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            assertThat(secondRetry).isFalse()
        } else {
            assertThat(secondRetry).isTrue()
        }
    }

    @Test
    fun testShouldRetryCameraLimitExceeded() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_LIMIT_EXCEEDED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_LIMIT_EXCEEDED,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryOnceCameraDisabledWhenDpcCameraDisabled() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISABLED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = true,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should fail if camera is disabled.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISABLED,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = true,
                isForeground = false,
            )
        ).isFalse()
    }

    @Test
    fun testShouldRetryRepeatedlyCameraDisabledWhenDpcCameraEnabled() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISABLED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should success if camera is not disabled.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISABLED,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryCameraDevice() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DEVICE,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DEVICE,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryCameraService() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_SERVICE,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_SERVICE,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryCameraDisconnected() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISCONNECTED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISCONNECTED,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryIllegalArgumentException() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_ILLEGAL_ARGUMENT_EXCEPTION,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_ILLEGAL_ARGUMENT_EXCEPTION,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetrySecurityException() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_SECURITY_EXCEPTION,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_SECURITY_EXCEPTION,
                2,
                DurationNs(1_000_000_001L),
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isFalse()
    }

    @Test
    fun testShouldNotRetryDoNotDisturbModeEnabled() {
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_DO_NOT_DISTURB_ENABLED,
                1,
                DurationNs(1_000_000_000L), // 1 second
                camerasDisabledByDevicePolicy = false,
                isForeground = false,
            )
        ).isFalse()
    }

    @Test
    fun testShouldRetryCameraInUseOnLongerTimeoutWhenActiveResume() {
        if (Build.VERSION.SDK_INT !in (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2)) {
            // We won't activate active resume mode when the API level is not in [Q, S_V2].
            return
        }
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_IN_USE,
                1,
                DurationNs(1_000_000_000L),
                camerasDisabledByDevicePolicy = false,
                isForeground = true,
            )
        ).isTrue()

        val secondRetry = RetryingCameraStateOpener.shouldRetry(
            ERROR_CAMERA_IN_USE,
            2,
            DurationNs(30_000_000_000L), // 30s
            camerasDisabledByDevicePolicy = false,
            isForeground = true,
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Multi-window available
            assertThat(secondRetry).isFalse()
        } else {
            assertThat(secondRetry).isTrue()
        }
    }

    @Test
    fun testShouldRetryCameraLimitExceededOnLongerTimeoutWhenActiveResume() {
        if (Build.VERSION.SDK_INT !in (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2)) {
            // We won't activate active resume mode when the API level is not in [Q, S_V2].
            return
        }
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_LIMIT_EXCEEDED,
                1,
                DurationNs(1_000_000_000L),
                camerasDisabledByDevicePolicy = false,
                isForeground = true,
            )
        ).isTrue()

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_LIMIT_EXCEEDED,
                2,
                DurationNs(30_000_000_000L), // 30s
                camerasDisabledByDevicePolicy = false,
                isForeground = true,
            )
        ).isTrue()
    }

    @Test
    fun testShouldRetryCameraDisconnectedOnLongerTimeoutWhenActiveResume() {
        if (Build.VERSION.SDK_INT !in (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2)) {
            // We won't activate active resume mode when the API level is not in [Q, S_V2].
            return
        }
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISCONNECTED,
                1,
                DurationNs(1_000_000_000L),
                camerasDisabledByDevicePolicy = false,
                isForeground = true,
            )
        ).isTrue()

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                ERROR_CAMERA_DISCONNECTED,
                2,
                DurationNs(30_000_000_000L), // 30s
                camerasDisabledByDevicePolicy = false,
                isForeground = true,
            )
        ).isTrue()
    }

    @Test
    fun testShouldActivateActiveResume() {
        assertThat(
            RetryingCameraStateOpener.shouldActivateActiveResume(
                isForeground = false,
                errorCode = ERROR_CAMERA_IN_USE,
            )
        ).isFalse()
        if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2)) {
            // Regardless of what error it is, we should only activate active resume mode when the
            // API level is [Q, S_V2], where multi-resume is supported and camera access priority
            // changes aren't properly notified.
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_SERVICE,
                )
            ).isFalse()
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_IN_USE,
                )
            ).isTrue()
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_LIMIT_EXCEEDED,
                )
            ).isTrue()
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_DISCONNECTED,
                )
            ).isTrue()
        } else {
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_SERVICE,
                )
            ).isFalse()
            assertThat(
                RetryingCameraStateOpener.shouldActivateActiveResume(
                    isForeground = true,
                    errorCode = ERROR_CAMERA_IN_USE,
                )
            ).isFalse()
        }
    }

    @Test
    fun cameraStateOpenerReturnsCorrectError() = runTest {
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        val result = cameraStateOpener.tryOpenCamera(
            cameraId0,
            1,
            Timestamps.now(fakeTimeSource),
        )

        assertThat(result.errorCode).isEqualTo(ERROR_CAMERA_IN_USE)
    }

    @Test
    fun cameraStateOpenerReturnsCorrectErrorWhenDoNotDisturbModeEnabledOnApi28() = runTest {
        val throwable = RuntimeException("Camera is being used after Camera.release() was called")
        throwable.stackTrace = arrayOf(
            StackTraceElement(
                "android.hardware.Camera",
                "_enableShutterSound",
                "Native Method",
                0
            ),
            StackTraceElement(
                "android.hardware.Camera",
                "updateAppOpsPlayAudio",
                "Camera.java",
                1770
            )
        )
        cameraOpener.toThrow = throwable

        try {
            val result = cameraStateOpener.tryOpenCamera(
                cameraId0,
                1,
                Timestamps.now(fakeTimeSource),
            )
            assertThat(result.errorCode).isEqualTo(ERROR_DO_NOT_DISTURB_ENABLED)
        } catch (throwable: Throwable) {
            // Only non-28 SDK levels should throw an exception.
            assertThat(Build.VERSION.SDK_INT).isNotEqualTo(28)
        }
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnCameraInUse() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock to move past the retry timeout.
        advanceTimeBy(30_000)
        advanceUntilIdle()

        val openResult = result.await()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_CAMERA_IN_USE)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            assertThat(cameraOpener.numberOfOpens).isEqualTo(2)
        } else {
            assertThat(cameraOpener.numberOfOpens).isGreaterThan(2)
        }
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly the number of camera opens minus 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(
            cameraOpener.numberOfOpens - 1
        )
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnCameraLimitExceeded() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.MAX_CAMERAS_IN_USE)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock to move past the retry timeout.
        advanceTimeBy(30_000)
        advanceUntilIdle()

        val openResult = result.await()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_CAMERA_LIMIT_EXCEEDED)
        assertThat(cameraOpener.numberOfOpens).isGreaterThan(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly the number of camera opens minus 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(
            cameraOpener.numberOfOpens - 1
        )
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnCameraDisabledWhenDpcCameraDisabled() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(true)
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_DISABLED)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock with just enough time for 1 camera retry (we wait 500ms before the
        // next retry).
        advanceTimeBy(750)
        advanceUntilIdle()

        val openResult = result.getCompleted()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_CAMERA_DISABLED)
        assertThat(cameraOpener.numberOfOpens).isEqualTo(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(1)
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnCameraDisabledWhenDpcCameraEnabled() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_DISABLED)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock to move past the retry timeout.
        advanceTimeBy(30_000)
        advanceUntilIdle()

        val openResult = result.await()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_CAMERA_DISABLED)
        assertThat(cameraOpener.numberOfOpens).isGreaterThan(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly the number of camera opens minus 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(
            cameraOpener.numberOfOpens - 1
        )
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnCameraDisconnected() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock to move past the retry timeout.
        advanceTimeBy(30_000)
        advanceUntilIdle()

        val openResult = result.await()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_CAMERA_DISCONNECTED)
        assertThat(cameraOpener.numberOfOpens).isGreaterThan(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly the number of camera opens minus 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(
            cameraOpener.numberOfOpens - 1
        )
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnIllegalArgumentException() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = IllegalArgumentException()
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock to move past the retry timeout.
        advanceTimeBy(30_000)
        advanceUntilIdle()

        val openResult = result.await()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_ILLEGAL_ARGUMENT_EXCEPTION)
        assertThat(cameraOpener.numberOfOpens).isGreaterThan(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly the number of camera opens minus 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(
            cameraOpener.numberOfOpens - 1
        )
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectlyOnSecurityException() = runTest {
        whenever(fakeDevicePolicyManager.camerasDisabled).thenReturn(false)
        cameraOpener.toThrow = SecurityException()
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }

        // Advance virtual clock with just enough time for 1 camera retry (we wait 500ms before the
        // next retry).
        advanceTimeBy(750)
        advanceUntilIdle()

        val openResult = result.getCompleted()
        assertThat(openResult.cameraState).isNull()
        assertThat(openResult.errorCode).isEqualTo(ERROR_SECURITY_EXCEPTION)
        assertThat(cameraOpener.numberOfOpens).isEqualTo(2)
        // The first retry should be hidden. Therefore the number of onGraphError() calls should be
        // exactly 1.
        assertThat(fakeCameraErrorListener.numberOfErrorCalls).isEqualTo(1)
    }
}
