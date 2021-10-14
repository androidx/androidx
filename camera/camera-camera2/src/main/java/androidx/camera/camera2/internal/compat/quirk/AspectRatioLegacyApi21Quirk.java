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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk that produces stretched use cases on all the legacy API 21 devices.
 *
 * If the device is LEGACY + Android 5.0, then return the same aspect ratio as maximum JPEG
 * resolution. The Camera2 LEGACY mode API always sends the HAL a configure call with the same
 * aspect ratio as the maximum JPEG resolution, and do the cropping/scaling before returning the
 * output. There is a bug because of a flipped scaling factor in the intermediate texture
 * transform matrix, and it was fixed in L MR1. See: b/128924712.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AspectRatioLegacyApi21Quirk implements Quirk {

    static boolean load(@NonNull final CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final Integer level = cameraCharacteristicsCompat.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                && Build.VERSION.SDK_INT == 21;
    }

    /**
     * Get the corrected aspect ratio.
     */
    @TargetAspectRatio.Ratio
    public int getCorrectedAspectRatio() {
        return TargetAspectRatio.RATIO_MAX_JPEG;
    }
}
