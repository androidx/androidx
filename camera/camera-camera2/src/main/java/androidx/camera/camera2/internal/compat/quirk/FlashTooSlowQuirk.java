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
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Quirks that denotes the device has a slow flash sequence that could result in blurred pictures.
 *
 * <p>QuirkSummary
 *     Bug Id: 211474332, 286190938, 280221967, 296814664, 296816175
 *     Description: When capturing still photos in auto flash mode, it needs more than 1 second to
 *     flash or capture actual photo after flash, and therefore it easily results in blurred or dark
 *     or overexposed pictures.
 *     Device(s): Pixel 3a / Pixel 3a XL, all models of Pixel 4 and 5, SM-A320, Moto G20, Itel A48,
 *     Realme C11 2021
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FlashTooSlowQuirk implements UseTorchAsFlashQuirk {
    private static final List<String> AFFECTED_MODEL_PREFIXES = Arrays.asList(
            "PIXEL 3A",
            "PIXEL 3A XL",
            "PIXEL 4", // includes Pixel 4 XL, 4A, and 4A (5g) too
            "PIXEL 5", // includes Pixel 5A too
            "SM-A320",
            "MOTO G(20)",
            "ITEL L6006", // Itel A48
            "RMX3231" // Realme C11 2021
    );

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return isAffectedModel()
                && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_BACK;
    }

    private static boolean isAffectedModel() {
        for (String modelPrefix : AFFECTED_MODEL_PREFIXES) {
            if (Build.MODEL.toUpperCase(Locale.US).startsWith(modelPrefix)) {
                return true;
            }
        }
        return false;
    }
}
