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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

/**
 * A class that contains utility methods for video stabilization.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoStabilizationUtil {

    private VideoStabilizationUtil() {
    }

    /**
     * Return true if the given camera characteristics support preview stabilization.
     */
    public static boolean isPreviewStabilizationSupported(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }
        int[] availableVideoStabilizationModes = characteristicsCompat.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        if (availableVideoStabilizationModes == null
                || availableVideoStabilizationModes.length == 0) {
            return false;
        }
        for (int mode : availableVideoStabilizationModes) {
            if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION) {
                return true;
            }
        }
        return false;
    }
}
