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

import android.hardware.camera2.CameraCaptureSession
import android.os.Build
import android.os.Handler
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class AndroidCaptureSessionStateCallbackTest {
    private val camera: CameraDeviceWrapper = mock()
    private val stateCallback: CameraCaptureSessionWrapper.StateCallback = mock()
    private val previousStateCallback: CameraCaptureSessionWrapper.StateCallback = mock()
    private val captureSession: CameraCaptureSession = mock()
    private val cameraErrorListener: CameraErrorListener = mock()
    private val callbackHandler: Handler = mock()
    private val androidStateCallback =
        AndroidCaptureSessionStateCallback(
            device = camera,
            stateCallback = stateCallback,
            lastStateCallback = previousStateCallback,
            cameraErrorListener = cameraErrorListener,
            callbackHandler = callbackHandler
        )

    @Test
    fun normalMethodsAreForwardedToStateCallback() {
        androidStateCallback.onConfigured(captureSession)
        androidStateCallback.onReady(captureSession)
        androidStateCallback.onActive(captureSession)

        verify(stateCallback, times(1)).onConfigured(any())
        verify(stateCallback, times(1)).onReady(any())
        verify(stateCallback, times(1)).onActive(any())
        verify(stateCallback, never()).onCaptureQueueEmpty(any())
        verify(stateCallback, never()).onConfigureFailed(any())
        verify(stateCallback, never()).onClosed(any())
        verify(stateCallback, never()).onSessionFinalized()
    }

    @Test
    fun onConfigureFailedFinalizesSession() {
        androidStateCallback.onConfigureFailed(captureSession)

        verify(stateCallback, never()).onConfigured(any())
        verify(stateCallback, never()).onReady(any())
        verify(stateCallback, never()).onActive(any())
        verify(stateCallback, never()).onCaptureQueueEmpty(any())
        verify(stateCallback, times(1)).onConfigureFailed(any())
        verify(stateCallback, never()).onClosed(any())
        verify(stateCallback, times(1)).onSessionFinalized()
    }

    @Test
    fun onCloseFinalizesSession() {
        androidStateCallback.onConfigured(captureSession)
        androidStateCallback.onClosed(captureSession)

        verify(stateCallback, times(1)).onConfigured(any())
        verify(stateCallback, never()).onReady(any())
        verify(stateCallback, never()).onActive(any())
        verify(stateCallback, never()).onCaptureQueueEmpty(any())
        verify(stateCallback, never()).onConfigureFailed(any())
        verify(stateCallback, times(1)).onClosed(any())
        verify(stateCallback, times(1)).onSessionFinalized()
    }

    @Test
    fun onConfiguredFinalizesPreviousStateCallback() {
        androidStateCallback.onConfigured(captureSession)

        verify(stateCallback, times(1)).onConfigured(any())
        verify(stateCallback, never()).onReady(any())
        verify(stateCallback, never()).onActive(any())
        verify(stateCallback, never()).onCaptureQueueEmpty(any())
        verify(stateCallback, never()).onConfigureFailed(any())
        verify(stateCallback, never()).onClosed(any())
        verify(stateCallback, never()).onSessionFinalized()

        verify(previousStateCallback, never()).onConfigured(any())
        verify(previousStateCallback, never()).onReady(any())
        verify(previousStateCallback, never()).onActive(any())
        verify(previousStateCallback, never()).onCaptureQueueEmpty(any())
        verify(previousStateCallback, never()).onConfigureFailed(any())
        verify(previousStateCallback, never()).onClosed(any())
        verify(previousStateCallback, times(1)).onSessionFinalized()
    }

    @Test
    fun onConfigureFailedCallsFinalize() {
        androidStateCallback.onConfigureFailed(captureSession)

        verify(stateCallback, never()).onConfigured(any())
        verify(stateCallback, never()).onReady(any())
        verify(stateCallback, never()).onActive(any())
        verify(stateCallback, never()).onCaptureQueueEmpty(any())
        verify(stateCallback, times(1)).onConfigureFailed(any())
        verify(stateCallback, never()).onClosed(any())
        verify(stateCallback, times(1)).onSessionFinalized()

        verify(previousStateCallback, never()).onConfigured(any())
        verify(previousStateCallback, never()).onReady(any())
        verify(previousStateCallback, never()).onActive(any())
        verify(previousStateCallback, never()).onCaptureQueueEmpty(any())
        verify(previousStateCallback, never()).onConfigureFailed(any())
        verify(previousStateCallback, never()).onClosed(any())
        verify(previousStateCallback, times(1)).onSessionFinalized()
    }

    @Test
    fun closingMultipleTimesOnlyFinalizesPreviousSessionOnce() {
        androidStateCallback.onClosed(captureSession)
        androidStateCallback.onClosed(captureSession)
        androidStateCallback.onClosed(captureSession)

        verify(stateCallback, times(3)).onClosed(any())
        verify(stateCallback, times(3)).onSessionFinalized()

        verify(previousStateCallback, never()).onClosed(any())
        verify(previousStateCallback, times(1)).onSessionFinalized()
    }
}
