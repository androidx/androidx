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

import android.os.Build;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: 252818931, 261744070, 319913852, 361328838
 *     Description: On certain devices, the captured image has color or zoom freezing issue for
 *                  reprocessing. We need to disable zero-shutter lag and return false for
 *                  {@link CameraInfo#isZslSupported()}.
 *     Device(s): Samsung Fold4, Samsung s22, Xiaomi Mi 8
 */
public class ZslDisablerQuirk implements Quirk {

    private static final List<String> AFFECTED_SAMSUNG_MODEL = Arrays.asList(
            "SM-F936",
            "SM-S901U",
            "SM-S908U",
            "SM-S908U1",
            "SM-F721U1",
            "SM-S928U1"
    );

    private static final List<String> AFFECTED_XIAOMI_MODEL = Arrays.asList(
            "MI 8"
    );

    static boolean load() {
        return isAffectedSamsungDevices() || isAffectedXiaoMiDevices();
    }

    private static boolean isAffectedSamsungDevices() {
        return "samsung".equalsIgnoreCase(Build.BRAND)
                && isAffectedModel(AFFECTED_SAMSUNG_MODEL);
    }
    private static boolean isAffectedXiaoMiDevices() {
        return "xiaomi".equalsIgnoreCase(Build.BRAND)
                && isAffectedModel(AFFECTED_XIAOMI_MODEL);
    }

    private static boolean isAffectedModel(List<String> modelList) {
        for (String model : modelList) {
            if (Build.MODEL.toUpperCase(Locale.US).startsWith(model)) {
                return true;
            }
        }
        return false;
    }
}
