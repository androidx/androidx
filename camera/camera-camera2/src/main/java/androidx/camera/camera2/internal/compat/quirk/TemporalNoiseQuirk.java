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

package androidx.camera.camera2.internal.compat.quirk;

import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.hardware.camera2.CameraDevice;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;

/**
 * <p>QuirkSummary
 *     Bug Id: b/316560705
 *     Description: Quirk indicates the recorded video contains obvious temporal noise. The issue
 *                  happens on Pixel 8 front camera and when the template type is
 *                  {@link CameraDevice#TEMPLATE_RECORD}.
 *     Device(s): Pixel 8.
 */
public class TemporalNoiseQuirk implements CaptureIntentPreviewQuirk {

    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        return isPixel8() && cameraCharacteristics.get(LENS_FACING) == LENS_FACING_FRONT;
    }

    private static boolean isPixel8() {
        return "Pixel 8".equalsIgnoreCase(Build.MODEL);
    }
}
