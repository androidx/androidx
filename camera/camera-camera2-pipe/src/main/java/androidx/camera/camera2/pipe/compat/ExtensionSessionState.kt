/*
 * Copyright 2021 The Android Open Source Project
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
import android.hardware.camera2.CameraExtensionSession
import androidx.annotation.RequiresApi

/**
 * This class acts as a state callback wrapper for a Camera2 [CameraCaptureSession]. It is
 * responsible for managing the lifecycle of the session and delegates the callback methods to an
 * instance of [CaptureSessionState].
 *
 * The purpose of this class is to handle the configuration, finalization, and closure events of the
 * [CameraExtensionSession]. It receives callbacks from the [CameraExtensionSessionWrapper] and
 * delegates them to the corresponding methods in the provided [CaptureSessionState] instance.
 *
 * @param captureSessionState The [CaptureSessionState] instance to delegate the callback methods
 *   to.
 */
@RequiresApi(31)
internal class ExtensionSessionState(private val captureSessionState: CaptureSessionState) :
    CameraExtensionSessionWrapper.StateCallback {
    override fun onConfigured(session: CameraExtensionSessionWrapper) {
        captureSessionState.onConfigured(session as CameraCaptureSessionWrapper)
    }

    override fun onSessionFinalized() {
        captureSessionState.onSessionFinalized()
    }

    override fun onConfigureFailed(session: CameraExtensionSessionWrapper) {
        captureSessionState.onConfigureFailed(session as CameraCaptureSessionWrapper)
    }

    override fun onClosed(session: CameraExtensionSessionWrapper) {
        captureSessionState.onClosed(session as CameraCaptureSessionWrapper)
    }
}
