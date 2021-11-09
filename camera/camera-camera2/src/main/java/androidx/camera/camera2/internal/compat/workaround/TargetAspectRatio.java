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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.AspectRatioLegacyApi21Quirk;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.Nexus4AndroidLTargetAspectRatioQuirk;
import androidx.camera.camera2.internal.compat.quirk.SamsungPreviewTargetAspectRatioQuirk;
import androidx.camera.core.impl.ImageOutputConfig;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Workaround to get corrected target aspect ratio.
 *
 * @see SamsungPreviewTargetAspectRatioQuirk
 * @see Nexus4AndroidLTargetAspectRatioQuirk
 * @see AspectRatioLegacyApi21Quirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class TargetAspectRatio {
    /** 4:3 standard aspect ratio. */
    public static final int RATIO_4_3 = 0;
    /** 16:9 standard aspect ratio. */
    public static final int RATIO_16_9 = 1;
    /** The same aspect ratio as the maximum JPEG resolution. */
    public static final int RATIO_MAX_JPEG = 2;
    /** No correction is needed. */
    public static final int RATIO_ORIGINAL = 3;

    /**
     * Gets corrected target aspect ratio based on device and camera quirks.
     */
    @TargetAspectRatio.Ratio
    public int get(@NonNull ImageOutputConfig imageOutputConfig, @NonNull String cameraId,
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final SamsungPreviewTargetAspectRatioQuirk samsungQuirk =
                DeviceQuirks.get(SamsungPreviewTargetAspectRatioQuirk.class);
        if (samsungQuirk != null && samsungQuirk.require16_9(imageOutputConfig)) {
            return TargetAspectRatio.RATIO_16_9;
        }
        final Nexus4AndroidLTargetAspectRatioQuirk nexus4AndroidLQuirk =
                DeviceQuirks.get(Nexus4AndroidLTargetAspectRatioQuirk.class);
        if (nexus4AndroidLQuirk != null) {
            return nexus4AndroidLQuirk.getCorrectedAspectRatio();
        }

        final AspectRatioLegacyApi21Quirk quirk = CameraQuirks.get(cameraId,
                cameraCharacteristicsCompat).get(AspectRatioLegacyApi21Quirk.class);
        if (quirk != null) {
            return quirk.getCorrectedAspectRatio();
        }

        return TargetAspectRatio.RATIO_ORIGINAL;
    }

    /**
     * @hide
     */
    @IntDef({RATIO_4_3, RATIO_16_9, RATIO_MAX_JPEG, RATIO_ORIGINAL})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface Ratio {
    }
}
