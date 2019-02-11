/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testapp.camera2interopburst;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicReference;

/** A capture session capture callback which updates a reference to the capture request. */
final class RequestUpdatingSessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
    private final AtomicReference<CaptureRequest> request = new AtomicReference<>();

    @Override
    public void onCaptureBufferLost(
            CameraCaptureSession session, CaptureRequest request, Surface surface, long frame) {
    }

    @Override
    public void onCaptureCompleted(
            CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
    }

    @Override
    public void onCaptureFailed(
            CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
    }

    @Override
    public void onCaptureProgressed(
            CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
    }

    @Override
    public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
    }

    @Override
    public void onCaptureSequenceCompleted(
            CameraCaptureSession session, int sequenceId, long frame) {
    }

    @Override
    public void onCaptureStarted(
            CameraCaptureSession session, CaptureRequest request, long timestamp, long frame) {
        this.request.set(request);
    }

    CaptureRequest getRequest() {
        return request.get();
    }
}
