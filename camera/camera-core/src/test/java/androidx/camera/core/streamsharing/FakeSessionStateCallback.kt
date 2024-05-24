/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.hardware.camera2.CameraCaptureSession
import android.view.Surface

/** A fake [CameraCaptureSession.StateCallback]. */
class FakeSessionStateCallback : CameraCaptureSession.StateCallback() {

    var onConfiguredCalled = false
    var onConfigureFailedCalled = false
    var onReadyCalled = false
    var onActiveCalled = false
    var onCaptureQueueEmptyCalled = false
    var onClosedCalled = false
    var onSurfacePreparedCalled = false

    override fun onConfigured(session: CameraCaptureSession) {
        onConfiguredCalled = true
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        onConfigureFailedCalled = true
    }

    override fun onReady(session: CameraCaptureSession) {
        super.onReady(session)
        onReadyCalled = true
    }

    override fun onActive(session: CameraCaptureSession) {
        super.onActive(session)
        onActiveCalled = true
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        super.onCaptureQueueEmpty(session)
        onCaptureQueueEmptyCalled = true
    }

    override fun onClosed(session: CameraCaptureSession) {
        super.onClosed(session)
        onClosedCalled = true
    }

    override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
        super.onSurfacePrepared(session, surface)
        onSurfacePreparedCalled = true
    }
}
