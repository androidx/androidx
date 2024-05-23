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

package androidx.camera.camera2.internal.compat.quirk;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.Camera2CameraControlImpl;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: 294870640
 *     Description: Quirk denoting the devices where {@link CaptureRequest#FLASH_MODE_TORCH} has
 *                  to be set for 3A states to be updated with good values (in some cases, AWB
 *                  scanning is not triggered at all). This results in problems like color tint
 *                  or bad exposure in captured image during captures where lighting condition
 *                  changes (e.g. screen flash capture). This maybe required even if a flash unit
 *                  is not available (e.g. with front camera) and
 *                  {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER} has been requested. If
 *                  {@link CaptureRequest#CONTROL_AE_MODE_ON_EXTERNAL_FLASH} is supported, it can
 *                  be used instead and thus setting {@code FLASH_MODE_TORCH} won't be required.
 *     Device(s): Pixel 6A, 6 PRO, 7, 7A, 7 PRO, 8, 8 PRO
 */
public class TorchFlashRequiredFor3aUpdateQuirk implements Quirk {
    private static final List<String> AFFECTED_PIXEL_MODELS = Arrays.asList(
            "PIXEL 6A",
            "PIXEL 6 PRO",
            "PIXEL 7",
            "PIXEL 7A",
            "PIXEL 7 PRO",
            "PIXEL 8",
            "PIXEL 8 PRO"
    );

    @NonNull
    private final CameraCharacteristicsCompat mCameraCharacteristics;

    public TorchFlashRequiredFor3aUpdateQuirk(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
    }

    /**
     * Checks if the quirk should be loaded based on device model info and camera lens facing.
     */
    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return isAffectedModel(cameraCharacteristics);
    }

    /**
     * Returns whether {@link CaptureRequest#FLASH_MODE_TORCH} is required to be set.
     *
     * <p> This will also check if the {@link CaptureRequest#CONTROL_AE_MODE_ON_EXTERNAL_FLASH} is
     * supported, which is more recommended than using a quirk like using {@code FLASH_MODE_TORCH}.
     */
    public boolean isFlashModeTorchRequired() {
        return !isExternalFlashAeModeSupported(mCameraCharacteristics);
    }

    private static boolean isAffectedModel(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return isAffectedPixelModel() && isFrontCamera(cameraCharacteristics);
    }

    private static boolean isAffectedPixelModel() {
        for (String model : AFFECTED_PIXEL_MODELS) {
            if (Build.MODEL.toUpperCase(Locale.US).equals(model)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFrontCamera(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_FRONT;
    }

    private static boolean isExternalFlashAeModeSupported(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics
    ) {
        if (Build.VERSION.SDK_INT < 28) {
            return false;
        }

        return Camera2CameraControlImpl.getSupportedAeMode(cameraCharacteristics,
                CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)
                == CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH;
    }
}
