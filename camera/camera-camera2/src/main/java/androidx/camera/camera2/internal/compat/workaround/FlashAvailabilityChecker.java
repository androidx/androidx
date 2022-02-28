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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.FlashAvailabilityBufferUnderflowQuirk;
import androidx.camera.core.Logger;

import java.nio.BufferUnderflowException;

/**
 * A workaround for devices which may throw a {@link java.nio.BufferUnderflowException} when
 * checking flash availability.
 *
 * @see androidx.camera.camera2.internal.compat.quirk.FlashAvailabilityBufferUnderflowQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FlashAvailabilityChecker {
    private static final String TAG = "FlashAvailabilityChecker";

    /**
     * Checks whether the camera characteristics advertise that flash is available safely.
     *
     * @param characteristics the characteristics to check for
     *                        {@link CameraCharacteristics#FLASH_INFO_AVAILABLE}.
     * @return the value of {@link CameraCharacteristics#FLASH_INFO_AVAILABLE} if it is contained
     * in the characteristics, or {@code false} if it is not or a
     * {@link java.nio.BufferUnderflowException} is thrown while checking.
     */
    public static boolean isFlashAvailable(@NonNull CameraCharacteristicsCompat characteristics) {
        if (DeviceQuirks.get(FlashAvailabilityBufferUnderflowQuirk.class) != null) {
            Logger.d(TAG, "Device has quirk "
                    + FlashAvailabilityBufferUnderflowQuirk.class.getSimpleName()
                    + ". Checking for flash availability safely...");
            return checkFlashAvailabilityWithPossibleBufferUnderflow(characteristics);
        } else {
            return checkFlashAvailabilityNormally(characteristics);
        }

    }

    private static boolean checkFlashAvailabilityWithPossibleBufferUnderflow(
            @NonNull CameraCharacteristicsCompat characteristics) {
        try {
            return checkFlashAvailabilityNormally(characteristics);
        } catch (BufferUnderflowException e) {
            return false;
        }
    }

    private static boolean checkFlashAvailabilityNormally(
            @NonNull CameraCharacteristicsCompat characteristics) {
        Boolean flashAvailable =
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        if (flashAvailable == null) {
            Logger.w(TAG, "Characteristics did not contain key FLASH_INFO_AVAILABLE. Flash is not"
                    + " available.");
        }
        return flashAvailable != null ? flashAvailable : false;
    }

    // Class should not be instantiated
    private FlashAvailabilityChecker() {
    }
}
