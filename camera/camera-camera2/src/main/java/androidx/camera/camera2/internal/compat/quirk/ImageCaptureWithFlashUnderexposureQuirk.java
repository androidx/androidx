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

package androidx.camera.camera2.internal.compat.quirk;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A quirk to denote even when the camera uses flash ON/AUTO mode, but the captured image is
 * still underexposed.
 *
 * <p>QuirkSummary
 *     Bug Id: 228800282
 *     Description: While the flash is in ON/AUTO mode and the camera fires the flash in a dark
 *                  environment, the captured photos are underexposed after continuously capturing 2
 *                  or more photos.
 *     Device(s): Samsung Galaxy A2 Core (sm-a260f), Samsung Galaxy J5 (sm-j530f), Samsung Galaxy
 *                J6 (sm-j600g), Samsung Galaxy J7 Neo (sm-j701f), Samsung Galaxy J7 Prime
 *                (sm-g610f), Samsung Galaxy J7 (sm-j710mn)
 */
public class ImageCaptureWithFlashUnderexposureQuirk implements UseTorchAsFlashQuirk {

    // List of devices with the issue. See b/228800282.
    public static final List<String> BUILD_MODELS = Arrays.asList(
            "sm-a260f",  // Samsung Galaxy A2 Core
            "sm-j530f",  // Samsung Galaxy J5
            "sm-j600g",  // Samsung Galaxy J6
            "sm-j701f",  // Samsung Galaxy J7 Neo
            "sm-g610f",  // Samsung Galaxy J7 Prime
            "sm-j710mn"  // Samsung Galaxy J7
    );

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return BUILD_MODELS.contains(Build.MODEL.toLowerCase(Locale.US))
                && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_BACK;
    }
}
