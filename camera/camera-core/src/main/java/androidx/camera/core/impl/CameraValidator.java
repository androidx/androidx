/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;

/**
 * Validation methods to verify the camera is initialized successfully, more info please reference
 * b/167201193.
 */
public final class CameraValidator {
    private CameraValidator() {
    }

    private static final String TAG = "CameraValidator";

    /**
     * Verifies the initialized camera instance in the CameraRepository
     *
     * <p>It should initialize the cameras that physically supported on the device. The
     * physically supported device lens facing information comes from the package manager and the
     * system feature flags are set by the vendor as part of the device build and CTS verified.
     *
     * @param context                  The application or activity context.
     * @param cameraRepository         The camera repository for verify.
     * @param availableCamerasSelector Indicate the camera that we need to check.
     * @throws CameraIdListIncorrectException if it fails to find all the camera instances that
     *                                        physically supported on the device.
     */
    public static void validateCameras(@NonNull Context context,
            @NonNull CameraRepository cameraRepository,
            @Nullable CameraSelector availableCamerasSelector)
            throws CameraIdListIncorrectException {

        Integer lensFacing = null;
        try {
            if (availableCamerasSelector != null
                    && (lensFacing = availableCamerasSelector.getLensFacing()) == null) {
                Logger.w(TAG, "No lens facing info in the availableCamerasSelector, don't "
                        + "verify the camera lens facing.");
                return;
            }
        } catch (IllegalStateException e) {
            Logger.e(TAG, "Cannot get lens facing from the availableCamerasSelector don't "
                    + "verify the camera lens facing.", e);
            return;
        }

        Logger.d(TAG,
                "Verifying camera lens facing on " + Build.DEVICE + ", lensFacingInteger: "
                        + lensFacing);

        PackageManager pm = context.getPackageManager();
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                if (availableCamerasSelector == null
                        || lensFacing.intValue() == CameraSelector.LENS_FACING_BACK) {
                    // Only verify the main camera if it is NOT specifying the available lens
                    // facing or it required the LENS_FACING_BACK camera.
                    CameraSelector.DEFAULT_BACK_CAMERA.select(cameraRepository.getCameras());
                }
            }
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                if (availableCamerasSelector == null
                        || lensFacing.intValue() == CameraSelector.LENS_FACING_FRONT) {
                    // Only verify the front camera if it is NOT specifying the available lens
                    // facing or it required the LENS_FACING_FRONT camera.
                    CameraSelector.DEFAULT_FRONT_CAMERA.select(cameraRepository.getCameras());
                }
            }
        } catch (IllegalArgumentException e) {
            Logger.e(TAG, "Camera LensFacing verification failed, existing cameras: "
                    + cameraRepository.getCameras());
            throw new CameraIdListIncorrectException("Expected camera missing from device.", e);
        }
    }

    /** The exception for the b/167201193: incorrect camera id list. */
    public static class CameraIdListIncorrectException extends Exception {
        public CameraIdListIncorrectException(@Nullable String message, @Nullable Throwable cause) {
            super(message, cause);
        }
    }
}
