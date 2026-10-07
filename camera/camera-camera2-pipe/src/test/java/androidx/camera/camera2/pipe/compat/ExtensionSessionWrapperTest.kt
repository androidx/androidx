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

import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.common.CameraFrameNumber
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.S)
internal class ExtensionSessionWrapperTest {
    private val device: CameraDeviceWrapper = mock()
    private val cameraExtensionSession: CameraExtensionSession = mock()
    private val cameraErrorListener: CameraErrorListener = mock()
    private val captureCallback: Camera2CaptureCallback = mock()
    private val captureRequest: CaptureRequest = mock()
    private val totalCaptureResult: TotalCaptureResult = mock()

    private val androidExtensionSession =
        AndroidCameraExtensionSession(
            device = device,
            cameraExtensionSession = cameraExtensionSession,
            cameraErrorListener = cameraErrorListener,
            callbackExecutor = MoreExecutors.directExecutor(),
        )

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun onCaptureSequenceCompleted_withoutOnCaptureStarted_doesNotThrowNpe() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                captureCallback
            )

        extensionCaptureCallback.onCaptureSequenceCompleted(cameraExtensionSession, 1)

        verify(captureCallback).onCaptureSequenceCompleted(1, 1L)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun onCaptureSequenceCompleted_afterOnCaptureStarted_usesStartedFrameNumber() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                captureCallback
            )

        extensionCaptureCallback.onCaptureStarted(cameraExtensionSession, captureRequest, 100L)
        extensionCaptureCallback.onCaptureStarted(cameraExtensionSession, captureRequest, 200L)
        extensionCaptureCallback.onCaptureSequenceCompleted(cameraExtensionSession, 1)

        verify(captureCallback).onCaptureSequenceCompleted(1, 2L)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun onCaptureFailed_withoutOnCaptureStarted_doesNotThrowException() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                captureCallback
            )

        extensionCaptureCallback.onCaptureFailed(cameraExtensionSession, captureRequest)

        verify(captureCallback).onCaptureFailed(captureRequest, CameraFrameNumber(1L))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun onCaptureResultAvailable_withoutOnCaptureStarted_doesNotThrowException() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallback(
                captureCallback
            )

        extensionCaptureCallback.onCaptureResultAvailable(
            cameraExtensionSession,
            captureRequest,
            totalCaptureResult,
        )

        verify(captureCallback)
            .onCaptureCompleted(captureRequest, totalCaptureResult, CameraFrameNumber(1L))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun onCaptureSequenceCompletedAndroidS_withoutOnCaptureStarted_doesNotThrowNpe() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallbackAndroidS(
                captureCallback,
                mutableMapOf(),
            )

        extensionCaptureCallback.onCaptureSequenceCompleted(cameraExtensionSession, 1)

        verify(captureCallback).onCaptureSequenceCompleted(1, 1L)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun onCaptureFailedAndroidS_withoutOnCaptureStarted_doesNotThrowNpe() {
        val extensionCaptureCallback =
            androidExtensionSession.Camera2CaptureSessionCallbackToExtensionCaptureCallbackAndroidS(
                captureCallback,
                mutableMapOf(),
            )

        extensionCaptureCallback.onCaptureFailed(cameraExtensionSession, captureRequest)

        verify(captureCallback).onCaptureFailed(captureRequest, CameraFrameNumber(1L))
    }
}
