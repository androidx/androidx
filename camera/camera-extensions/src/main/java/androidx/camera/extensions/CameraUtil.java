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

package androidx.camera.extensions;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.core.util.Preconditions;

/**
 * Utility functions for accessing camera related parameters
 */
class CameraUtil {
    private static final String TAG = "CameraUtil";

    @Nullable
    static String getCameraIdUnchecked(@NonNull CameraSelector cameraSelector) {
        try {
            return CameraX.getCameraWithCameraSelector(
                    cameraSelector).getCameraInfoInternal().getCameraId();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to get camera id for the camera selector.");
            // Returns null if there's no camera id can be found.
            return null;
        }
    }

    @Nullable
    static String getCameraIdWithLensFacingUnchecked(@Nullable Integer lensFacing) {
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        return getCameraIdUnchecked(cameraSelector);
    }

    static CameraCharacteristics getCameraCharacteristics(String cameraId) {
        Preconditions.checkNotNull(cameraId, "Invalid camera id.");
        Context context = CameraX.getContext();
        CameraManagerCompat cameraManager = CameraManagerCompat.from(context);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessExceptionCompat e) {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }

        return cameraCharacteristics;
    }

    private CameraUtil() {
    }
}
