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

package androidx.camera.camera2;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureFailure;

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

    final Map<CaptureRequest, List<CameraCaptureCallback>> mCallbackMap;

    CameraBurstCaptureCallback() {
        mCallbackMap = new HashMap<>();
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        List<CameraCaptureCallback> cameraCaptureCallbacks = mCallbackMap.get(request);

        for (CameraCaptureCallback cb : cameraCaptureCallbacks) {
            cb.onCaptureCompleted(
                    new Camera2CameraCaptureResult(request.getTag(), result));
        }
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {

        List<CameraCaptureCallback> cameraCaptureCallbacks = mCallbackMap.get(request);

        CameraCaptureFailure cameraFailure =
                new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR);
        for (CameraCaptureCallback cb : cameraCaptureCallbacks) {
            cb.onCaptureFailed(cameraFailure);
        }
    }

    void addCallback(CaptureRequest captureRequest,
            List<CameraCaptureCallback> cameraCaptureCallbacks) {

        mCallbackMap.put(captureRequest, cameraCaptureCallbacks);

    }
}
