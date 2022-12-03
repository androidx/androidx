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

import android.os.Build
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VirtualCameraManagerTest {

    private val fakeTimeSource = object : TimeSource {
        public var currentTimestamp = TimestampNs(0L)

        override fun now() = currentTimestamp
    }

    @Test
    fun testCameraRetryReturnsTrueWithinTimeout() {
        val firstAttemptTimestamp = TimestampNs(0L)
        fakeTimeSource.currentTimestamp = TimestampNs(1_000_000_000L) // 1 second

        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_IN_USE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_DISABLED,
                1, firstAttemptTimestamp, fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_DEVICE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_SERVICE,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_CAMERA_DISCONNECTED,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION,
                1,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isTrue()

        // Second attempt should succeed as well.
        assertThat(
            VirtualCameraManager.shouldRetry(
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
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_SECURITY_EXCEPTION,
                1, firstAttemptTimestamp, fakeTimeSource
            )
        ).isTrue()

        // Second attempt should fail.
        assertThat(
            VirtualCameraManager.shouldRetry(
                CameraError.ERROR_SECURITY_EXCEPTION,
                2,
                firstAttemptTimestamp,
                fakeTimeSource
            )
        ).isFalse()
    }
}