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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.TagBundle;
import androidx.core.util.Preconditions;

/**
 * An adapter that passes {@link CameraCaptureSession.CaptureCallback} to {@link
 * CameraCaptureCallback}.
 */
final class CaptureCallbackAdapter extends CameraCaptureSession.CaptureCallback {

    private final CameraCaptureCallback mCameraCaptureCallback;

    CaptureCallbackAdapter(CameraCaptureCallback cameraCaptureCallback) {
        if (cameraCaptureCallback == null) {
            throw new NullPointerException("cameraCaptureCallback is null");
        }
        mCameraCaptureCallback = cameraCaptureCallback;
    }

    @Override
    public void onCaptureStarted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            long timestamp,
            long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
        mCameraCaptureCallback.onCaptureStarted(getCaptureConfigId(request));
    }

    private int getCaptureConfigId(CaptureRequest captureRequest) {
        if (!(captureRequest.getTag() instanceof TagBundle)) {
            return CaptureConfig.DEFAULT_ID;
        }
        TagBundle tagbundle = (TagBundle) captureRequest.getTag();
        Integer captureConfigId =
                (Integer) tagbundle.getTag(CaptureConfig.CAPTURE_CONFIG_ID_TAG_KEY);
        if (captureConfigId == null) {
            return CaptureConfig.DEFAULT_ID;
        }
        return captureConfigId;
    }

    @Override
    public void onCaptureCompleted(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);

        Object captureRequestTag = request.getTag();
        TagBundle tagBundle;

        // Inside the CameraX, the CaptureResult's tag should be issued from CameraX, that means
        // it will be a TagBundle object.
        if (captureRequestTag != null) {
            Preconditions.checkArgument(captureRequestTag instanceof TagBundle, "The "
                    + "tagBundle object from the CaptureResult is not a TagBundle object.");

            tagBundle = (TagBundle) captureRequestTag;
        } else {
            tagBundle = TagBundle.emptyBundle();
        }
        mCameraCaptureCallback.onCaptureCompleted(getCaptureConfigId(request),
                new Camera2CameraCaptureResult(tagBundle, result));
    }

    @Override
    public void onCaptureFailed(
            @NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request,
            @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);

        CameraCaptureFailure cameraFailure =
                new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR);

        mCameraCaptureCallback.onCaptureFailed(getCaptureConfigId(request), cameraFailure);
    }
}
