/*
 * Copyright 2022 The Android Open Source Project
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

import static android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

/**
 * Utility class for Zero-Shutter Lag.
 */
final class ZslUtil {

    private ZslUtil() {
    }

    @RequiresApi(21)
    public static boolean isCapabilitySupported(
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat,
            int targetCapability) {
        int[] capabilities = cameraCharacteristicsCompat.get(REQUEST_AVAILABLE_CAPABILITIES);
        if (capabilities != null) {
            for (int capability : capabilities) {
                if (capability == targetCapability) {
                    return true;
                }
            }
        }
        return false;
    }
}
