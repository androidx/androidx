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

package androidx.camera.camera2.internal

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi

/**
 * A `CaptureCallback` that forwards all callbacks to another `CaptureCallback` with a specific
 * `CaptureRequest`.
 */
public class RequestForwardingCaptureCallback(
    private val forwardedRequest: CaptureRequest,
    private val delegate: CameraCaptureSession.CaptureCallback,
) : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long,
    ) {
        delegate.onCaptureStarted(session, forwardedRequest, timestamp, frameNumber)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onReadoutStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long,
    ) {
        delegate.onReadoutStarted(session, forwardedRequest, timestamp, frameNumber)
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult,
    ) {
        delegate.onCaptureProgressed(session, forwardedRequest, partialResult)
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult,
    ) {
        delegate.onCaptureCompleted(session, forwardedRequest, result)
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        failure: CaptureFailure,
    ) {
        delegate.onCaptureFailed(session, forwardedRequest, failure)
    }

    override fun onCaptureSequenceCompleted(
        session: CameraCaptureSession,
        sequenceId: Int,
        frameNumber: Long,
    ) {
        delegate.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        delegate.onCaptureSequenceAborted(session, sequenceId)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCaptureBufferLost(
        session: CameraCaptureSession,
        request: CaptureRequest,
        target: Surface,
        frameNumber: Long,
    ) {
        delegate.onCaptureBufferLost(session, forwardedRequest, target, frameNumber)
    }
}
