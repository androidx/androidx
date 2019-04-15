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
package androidx.camera.extensions.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;

import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.CaptureStage;
import androidx.camera.extensions.PreviewExtender;

/**
 * Implementation for bokeh view finder use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public final class BokehPreviewExtender extends PreviewExtender {
    private static final String TAG = "BokehPreviewExtender";
    private static final int DEFAULT_STAGE_ID = 0;

    public BokehPreviewExtender(PreviewConfig.Builder builder) {
        super(builder);
    }

    @Override
    public void enableExtension() {
        Log.d(TAG, "Adding effects to the view finder");
        // Sets necessary CaptureRequest parameters via CaptureStage
        CaptureStage captureStage = new CaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);
        setCaptureStage(captureStage);
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Implement the logic to check whether the extension function is supported or not.
        return true;
    }
}
