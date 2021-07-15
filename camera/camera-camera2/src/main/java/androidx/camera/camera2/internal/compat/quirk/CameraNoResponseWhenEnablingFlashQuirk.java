/*
 * Copyright 2021 The Android Open Source Project
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

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

import java.util.Locale;

/**
 * Camera gets stuck when taking pictures with flash ON or AUTO in dark environment.
 *
 * <p>See b/193336562 for details.
 */
public class CameraNoResponseWhenEnablingFlashQuirk implements UseTorchAsFlashQuirk {
    @VisibleForTesting
    public static final String BUILD_BRAND = "SAMSUNG";

    @VisibleForTesting
    public static final String BUILD_MODEL = "SM-N920";

    static boolean load(@NonNull CameraCharacteristicsCompat characteristics) {
        return BUILD_BRAND.equals(Build.BRAND.toUpperCase(Locale.US))
                // Enables on all Samsung Galaxy Note 5 devices.
                && Build.MODEL.toUpperCase(Locale.US).startsWith(BUILD_MODEL)
                && characteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_BACK;
    }
}
