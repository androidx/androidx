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
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi

/**
 * A fake [CameraCaptureSession.CaptureCallback].s
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class FakeSessionCaptureCallback : CameraCaptureSession.CaptureCallback() {

    var onCaptureCompletedCalled = false
    var onCaptureProgressedCalled = false
    var onCaptureFailedCalled = false
    var onCaptureSequenceCompletedCalled = false
    var onCaptureSequenceAbortedCalled = false
    var onCaptureBufferLostCalled = false
    var onCaptureStartedCalled = false
    var onReadoutStartedCalled = false

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: android.hardware.camera2.TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        onCaptureCompletedCalled = true
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult
    ) {
        super.onCaptureProgressed(session, request, partialResult)
        onCaptureProgressedCalled = true
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        failure: CaptureFailure
    ) {
        super.onCaptureFailed(session, request, failure)
        onCaptureFailedCalled = true
    }

    override fun onCaptureSequenceCompleted(
        session: CameraCaptureSession,
        sequenceId: Int,
        frameNumber: Long
    ) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
        onCaptureSequenceCompletedCalled = true
    }

    override fun onCaptureSequenceAborted(
        session: CameraCaptureSession,
        sequenceId: Int
    ) {
        super.onCaptureSequenceAborted(session, sequenceId)
        onCaptureSequenceAbortedCalled = true
    }

    override fun onCaptureBufferLost(
        session: CameraCaptureSession,
        request: CaptureRequest,
        target: Surface,
        frameNumber: Long
    ) {
        super.onCaptureBufferLost(session, request, target, frameNumber)
        onCaptureBufferLostCalled = true
    }

    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long
    ) {
        super.onCaptureStarted(session, request, timestamp, frameNumber)
        onCaptureStartedCalled = true
    }

    override fun onReadoutStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long
    ) {
        super.onReadoutStarted(session, request, timestamp, frameNumber)
        onReadoutStartedCalled = true
    }
}
