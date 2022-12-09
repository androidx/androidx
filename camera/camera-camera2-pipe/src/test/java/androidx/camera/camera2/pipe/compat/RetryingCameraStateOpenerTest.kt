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
import androidx.camera.camera2.pipe.CameraError.Companion.ERROR_CAMERA_IN_USE
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeTimeSource
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RetryingCameraStateOpenerTest {
    private val cameraId0 = CameraId("0")
    private val cameraMetadataProvider = object : CameraMetadataProvider {
        override suspend fun getMetadata(cameraId: CameraId): CameraMetadata =
            FakeCameraMetadata(cameraId = cameraId)

        override fun awaitMetadata(cameraId: CameraId): CameraMetadata =
            FakeCameraMetadata(cameraId = cameraId)
    }

    // TODO(lnishan): Consider mocking this object when Mockito works well with value classes.
    private val cameraOpener = object : CameraOpener {
        var toThrow: Throwable? = null
        var numberOfOpens = 0

        override fun openCamera(cameraId: CameraId, stateCallback: CameraDevice.StateCallback) {
            numberOfOpens++
            toThrow?.let {
                throw it
            }
        }
    }

    private val fakeTimeSource = FakeTimeSource()

    private val cameraStateOpener =
        CameraStateOpener(cameraOpener, cameraMetadataProvider, fakeTimeSource)

    private val cameraAvailabilityMonitor = object : CameraAvailabilityMonitor {
        override suspend fun awaitAvailableCamera(
            cameraId: CameraId,
            timeoutMillis: Long
        ): Boolean = true
    }

    private val retryingCameraStateOpener =
        RetryingCameraStateOpener(cameraStateOpener, cameraAvailabilityMonitor, fakeTimeSource)

    @Test
    fun testCameraRetryReturnsTrueWithinTimeout() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_IN_USE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetryReturnsFalseWhenTimeoutExpires() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(30_000_000_000L) // 30 seconds

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_IN_USE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isFalse()
    }

    @Test
    fun testCameraRetryShouldFailUndetermined() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_UNDETERMINED,
                1, firstAttemptTimestamp, fakeTimeSource
            )
        ).isFalse()
    }

    @Test
    fun testCameraRetryCameraInUse() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_IN_USE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_IN_USE,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isFalse()
    }

    @Test
    fun testCameraRetryCameraLimitExceeded() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetryCameraDisabled() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DISABLED,
                1, firstAttemptTimestamp, fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DISABLED,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isFalse()
    }

    @Test
    fun testCameraRetryCameraDevice() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DEVICE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DEVICE,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetryCameraService() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_SERVICE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_SERVICE,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetryCameraDisconnected() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DISCONNECTED,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_CAMERA_DISCONNECTED,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetryIllegalArgumentException() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()
    }

    @Test
    fun testCameraRetrySecurityException() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_SECURITY_EXCEPTION,
                1, firstAttemptTimestamp, fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            RetryingCameraStateOpener.shouldRetry(
                CameraError.ERROR_SECURITY_EXCEPTION,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isFalse()
    }

    @Test
    fun cameraStateOpenerReturnsCorrectError() = runTest {
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        val result =
            cameraStateOpener.tryOpenCamera(cameraId0, 1, Timestamps.now(fakeTimeSource))

        assertThat(result.errorCode).isEqualTo(ERROR_CAMERA_IN_USE)
    }

    @Test
    fun retryingCameraStateOpenerRetriesCorrectly() = runTest {
        cameraOpener.toThrow = CameraAccessException(CameraAccessException.CAMERA_IN_USE)
        val result = async {
            retryingCameraStateOpener.openCameraWithRetry(cameraId0)
        }
        // Advance the time to allow for retries.
        advanceTimeBy(200)
        advanceUntilIdle()

        // Now make the retry time limit expire to conclude openCameraWithRetry().
        fakeTimeSource.currentTimestamp = TimestampNs(30_000_000_000)

        assertThat(result.await()).isNull()
        assertThat(cameraOpener.numberOfOpens).isEqualTo(2)
    }
}