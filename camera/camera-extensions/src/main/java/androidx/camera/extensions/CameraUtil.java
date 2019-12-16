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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.impl.LensFacingCameraIdFilter;

import java.util.Set;

/**
 * Utility functions for accessing camera related parameters
 */
class CameraUtil {
    private static final String TAG = "CameraUtil";

    @Nullable
    static String getCameraIdUnchecked(@NonNull CameraSelector cameraSelector) {
        try {
            return CameraX.getCameraWithCameraSelector(cameraSelector);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to get camera id for the camera selector.");
            // Returns null if there's no camera id can be found.
            return null;
        }
    }

    @NonNull
    static Set<String> getCameraIdSetWithLensFacing(@Nullable Integer lensFacing)
            throws CameraInfoUnavailableException {
        Set<String> availableCameraIds = CameraX.getCameraFactory().getAvailableCameraIds();
        LensFacingCameraIdFilter lensFacingCameraIdFilter =
                CameraX.getCameraFactory().getLensFacingCameraIdFilter(lensFacing);
        availableCameraIds = lensFacingCameraIdFilter.filter(availableCameraIds);

        return availableCameraIds;
    }

    static CameraCharacteristics getCameraCharacteristics(String cameraId) {
        Context context = CameraX.getContext();
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }

        return cameraCharacteristics;
    }

    private CameraUtil() {
    }
}
