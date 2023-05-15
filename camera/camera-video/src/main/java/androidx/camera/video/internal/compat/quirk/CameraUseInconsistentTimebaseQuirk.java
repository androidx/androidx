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

package androidx.camera.video.internal.compat.quirk;


import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.internal.workaround.VideoTimebaseConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 197805856, 280121263
 *     Description: Quirk that denotes some devices use a timebase for camera frames that is
 *                  different than what is reported by
 *                  {@link android.hardware.camera2.CameraCharacteristics
 *                  #SENSOR_INFO_TIMESTAMP_SOURCE}. This can cause A/V sync issues.
 *     Device(s): Some Samsung devices and devices running on certain Qualcomm SoCs
 *     @see VideoTimebaseConverter
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraUseInconsistentTimebaseQuirk implements Quirk {
    private static final Set<String> BUILD_HARDWARE_SET = new HashSet<>(Arrays.asList(
            "samsungexynos7570",
            "samsungexynos7870",
            "qcom"
    ));

    private static final Set<String> BUILD_SOC_MODEL_SET = new HashSet<>(Collections.singletonList(
            "sm6375"
    ));

    static boolean load() {
        return usesAffectedSoc() || isAffectedSamsungDevice();
    }

    private static boolean usesAffectedSoc() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && BUILD_SOC_MODEL_SET.contains(Build.SOC_MODEL.toLowerCase());
    }

    private static boolean isAffectedSamsungDevice() {
        return "SAMSUNG".equalsIgnoreCase(Build.BRAND)
                && BUILD_HARDWARE_SET.contains(Build.HARDWARE.toLowerCase());
    }
}
