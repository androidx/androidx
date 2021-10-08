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

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * Quirk that produces stretched preview on Nexus 4 devices running Android L(API levels 21 and 22).
 *
 * <p> There is a Camera1/HAL1 issue on the Nexus 4. The preview will be stretched when
 * configuring a JPEG that doesn't actually have the same aspect ratio as the maximum JPEG
 * resolution. See: b/19606058.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Nexus4AndroidLTargetAspectRatioQuirk implements Quirk {
    // List of devices with the issue.
    private static final List<String> DEVICE_MODELS = Arrays.asList(
            "NEXUS 4" // b/158749159
    );

    static boolean load() {
        return "GOOGLE".equals(Build.BRAND.toUpperCase()) && Build.VERSION.SDK_INT < 23
                && DEVICE_MODELS.contains(android.os.Build.MODEL.toUpperCase());
    }

    /**
     * Get the corrected aspect ratio.
     */
    @TargetAspectRatio.Ratio
    public int getCorrectedAspectRatio() {
        return TargetAspectRatio.RATIO_MAX_JPEG;
    }
}
