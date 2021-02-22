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

package androidx.camera.extensions.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * A utility class to check the availabilities of camera characteristics.
 */
final class CameraCharacteristicAvailability {
    private static final String TAG = "CharacteristicAbility";

    private CameraCharacteristicAvailability() {
    }

    /**
     * Check if the given effect id is available in the camera characteristics.
     *
     * @param cameraCharacteristics the camera characteristics.
     * @param effect the effect id.
     * @return {@code true} if the given effect id is available in the camera characteristics.
     * {@code false} otherwise.
     */
    static boolean isEffectAvailable(@NonNull CameraCharacteristics cameraCharacteristics,
            int effect) {
        int[] availableEffects = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
        if (availableEffects == null) {
            Log.d(TAG, "No CONTROL_AVAILABLE_EFFECTS info");
            return false;
        }

        for (int availableEffect : availableEffects) {
            if (availableEffect == effect) {
                return true;
            }
        }
        Log.d(TAG, "effect: " + effect + " is not in available list "
                + Arrays.toString(availableEffects));
        return false;
    }
}
