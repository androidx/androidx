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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk denoting Af Region is incorrectly flipped horizontally.
 *
 * <p>QuirkSummary
 *     Bug Id: 210548792
 *     Description: Regions set in {@link CaptureRequest#CONTROL_AF_REGIONS} are incorrectly flipped
 *                  horizontally when using front-facing cameras.
 *     Device(s): All Samsung devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AfRegionFlipHorizontallyQuirk implements Quirk {
    static boolean load(@NonNull final CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        return (Build.BRAND.equalsIgnoreCase("SAMSUNG")
                && cameraCharacteristicsCompat.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT);
    }
}
