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

import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CameraCaptureFailure;
import androidx.camera.camera2.internal.Camera2CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;

/**
* An utility class to convert {@link CameraCaptureResult} to camera2 {@link CaptureResult}.
*/
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2CameraCaptureResultConverter {
    /**
     * Converts {@link CameraCaptureResult} to camera2 {@link CaptureResult}.
     *
     * @return The CaptureResult instance or {@code null} if there is no underlying CaptureResult.
     */
    @Nullable
    public static CaptureResult getCaptureResult(
            @Nullable CameraCaptureResult cameraCaptureResult) {
        if (cameraCaptureResult instanceof Camera2CameraCaptureResult) {
            return ((Camera2CameraCaptureResult) cameraCaptureResult).getCaptureResult();
        } else {
            return null;
        }
    }

    /**
     * Converts {@link CameraCaptureFailure} to camera2 {@link CaptureFailure}.
     *
     * @return The CaptureFailure instance or {@code null} if there is no underlying CaptureFailure.
     */
    @Nullable
    public static CaptureFailure getCaptureFailure(
            @NonNull CameraCaptureFailure cameraCaptureFailure) {
        if (cameraCaptureFailure instanceof Camera2CameraCaptureFailure) {
            return ((Camera2CameraCaptureFailure) cameraCaptureFailure).getCaptureFailure();
        } else {
            return null;
        }
    }

    private Camera2CameraCaptureResultConverter() {}
}
