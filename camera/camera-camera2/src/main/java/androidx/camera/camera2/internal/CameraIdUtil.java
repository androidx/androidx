/*
 * Copyright 2024 The Android Open Source Project
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

import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.InitializationException;

/**
 * Utility class to enumerate and filter camera ids.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CameraIdUtil {

    private CameraIdUtil() {
    }

    /**
     * Checks whether the camera has
     * {@link CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE} or not.
     *
     * @param cameraManagerCompat {@link CameraManagerCompat}
     * @param cameraId camera id
     * @return True if it is backward compatible, otherwise false.
     * @throws InitializationException
     *
     */
    public static boolean isBackwardCompatible(
            @NonNull CameraManagerCompat cameraManagerCompat,
            @NonNull String cameraId) throws
            InitializationException {
        // Always returns true to not break robolectric tests because the cameras setup in
        // robolectric don't have REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE capability
        // by default.
        if ("robolectric".equals(Build.FINGERPRINT)) {
            return true;
        }

        int[] availableCapabilities;

        try {
            availableCapabilities = cameraManagerCompat.getCameraCharacteristicsCompat(cameraId)
                    .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        } catch (CameraAccessExceptionCompat e) {
            throw new InitializationException(CameraUnavailableExceptionHelper.createFrom(e));
        }

        if (availableCapabilities != null) {
            for (int capability : availableCapabilities) {
                if (capability == REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                    return true;
                }
            }
        }

        return false;
    }
}
