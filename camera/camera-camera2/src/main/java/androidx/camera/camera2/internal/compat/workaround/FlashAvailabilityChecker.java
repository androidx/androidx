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
import android.os.Build;

import androidx.annotation.NonNull;
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
public final class FlashAvailabilityChecker {
    private static final String TAG = "FlashAvailability";

    /**
     * Checks whether the camera characteristics advertise that flash is available safely.
     *
     * @param provider the characteristics provider to check for
     *                 {@link CameraCharacteristics#FLASH_INFO_AVAILABLE}.
     * @return the value of {@link CameraCharacteristics#FLASH_INFO_AVAILABLE} if it is contained
     * in the characteristics, or {@code false} if it is not or a
     * {@link java.nio.BufferUnderflowException} is thrown while checking.
     */
    public static boolean isFlashAvailable(@NonNull CameraCharacteristicsProvider provider) {
        return isFlashAvailable(/*allowRethrowOnError=*/false, provider);
    }

    /**
     * Checks whether the camera characteristics advertise that flash is available safely.
     *
     * @param allowRethrowOnError whether exceptions can be rethrown on devices that are not
     *                            known to be problematic. If {@code false}, these devices will be
     *                            logged as an error instead.
     * @param provider            the characteristics provider to check for
     *                            {@link CameraCharacteristics#FLASH_INFO_AVAILABLE}.
     * @return the value of {@link CameraCharacteristics#FLASH_INFO_AVAILABLE} if it is contained
     * in the characteristics, or {@code false} if it is not or a
     * {@link java.nio.BufferUnderflowException} is thrown while checking.
     */
    public static boolean isFlashAvailable(boolean allowRethrowOnError,
            @NonNull CameraCharacteristicsProvider provider) {
        Boolean flashAvailable;
        try {
            flashAvailable = provider.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        } catch (BufferUnderflowException e) {
            if (DeviceQuirks.get(FlashAvailabilityBufferUnderflowQuirk.class) != null) {
                Logger.d(TAG, String.format("Device is known to throw an exception while "
                        + "checking flash availability. Flash is not available. "
                        + "[Manufacturer: %s, Model: %s, API Level: %d].",
                        Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT));
            } else {
                Logger.e(TAG, String.format("Exception thrown while checking for flash "
                                + "availability on device not known to throw exceptions during "
                                + "this check. Please file an issue at "
                                + "https://issuetracker.google.com/issues/new?component=618491"
                                + "&template=1257717 with this error message "
                                + "[Manufacturer: %s, Model: %s, API Level: %d].\n"
                                + "Flash is not available.",
                        Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT), e);
            }

            if (allowRethrowOnError) {
                throw e;
            } else {
                flashAvailable = false;
            }
        }
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
