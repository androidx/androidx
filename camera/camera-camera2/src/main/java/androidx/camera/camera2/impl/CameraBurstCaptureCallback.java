/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A callback object for tracking the progress of a capture request submitted to the camera device.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class CameraBurstCaptureCallback extends CameraCaptureSession.CaptureCallback {

    final Map<CaptureRequest, List<CameraCaptureSession.CaptureCallback>> mCallbackMap;

    CameraBurstCaptureCallback() {
        mCallbackMap = new HashMap<>();
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCaptureBufferLost(
            CameraCaptureSession session, CaptureRequest request, Surface surface, long frame) {
        for (CameraCaptureSession.CaptureCallback callback : getCallbacks(request)) {
            callback.onCaptureBufferLost(session, request, surface, frame);
        }
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        for (CameraCaptureSession.CaptureCallback callback : getCallbacks(request)) {
            callback.onCaptureCompleted(session, request, result);
        }
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
        for (CameraCaptureSession.CaptureCallback callback : getCallbacks(request)) {
            callback.onCaptureFailed(session, request, failure);
        }
    }

    @Override
    public void onCaptureProgressed(
            @NonNull  CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull
            CaptureResult partialResult) {
        for (CameraCaptureSession.CaptureCallback callback : getCallbacks(request)) {
            callback.onCaptureProgressed(session, request, partialResult);
        }
    }

    @Override
    public void onCaptureStarted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            long timestamp,
            long frameNumber) {
        for (CameraCaptureSession.CaptureCallback callback : getCallbacks(request)) {
            callback.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    }

    @Override
    public void onCaptureSequenceAborted(
            @NonNull CameraCaptureSession session, int sequenceId) {
        // No-op.
    }

    @Override
    public void onCaptureSequenceCompleted(
            @NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        // No-op.
    }

    private List<CameraCaptureSession.CaptureCallback> getCallbacks(CaptureRequest request) {
        List<CameraCaptureSession.CaptureCallback> callbacks = mCallbackMap.get(request);
        return callbacks != null
                ? callbacks : Collections.<CameraCaptureSession.CaptureCallback>emptyList();
    }

    void addCamera2Callbacks(CaptureRequest captureRequest,
            List<CameraCaptureSession.CaptureCallback> captureCallbacks) {
        List<CameraCaptureSession.CaptureCallback> existingCallbacks =
                mCallbackMap.get(captureRequest);
        if (existingCallbacks != null) {
            List<CameraCaptureSession.CaptureCallback> totalCallbacks =
                    new ArrayList<>(captureCallbacks.size() + existingCallbacks.size());
            totalCallbacks.addAll(captureCallbacks);
            totalCallbacks.addAll(existingCallbacks);
            mCallbackMap.put(captureRequest, totalCallbacks);
        } else {
            mCallbackMap.put(captureRequest, captureCallbacks);
        }
    }

}
