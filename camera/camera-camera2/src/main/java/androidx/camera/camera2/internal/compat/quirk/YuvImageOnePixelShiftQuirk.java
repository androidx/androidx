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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.internal.compat.quirk.OnePixelShiftQuirk;

/**
 * <p>QuirkSummary
 *     Bug Id: 184229033
 *     Description: On certain devices, one pixel shifted when the HAL layer converts RGB data to
 *                  YUV data. It leads to the leftmost column degradation when converting YUV to
 *                  RGB in applications.
 *     Device(s): Motorola MotoG3, Samsung SM-G532F/SM-J700F/SM-J415F/SM-920F, Xiaomi Mi A1
 */
public final class YuvImageOnePixelShiftQuirk implements OnePixelShiftQuirk {

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        return isMotorolaMotoG3() || isSamsungSMG532F() || isSamsungSMJ700F()
                || isSamsungSMA920F() || isSamsungSMJ415F() || isXiaomiMiA1();
    }

    private static boolean isMotorolaMotoG3() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "MotoG3".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungSMG532F() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && "SM-G532F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungSMJ700F() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && "SM-J700F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungSMJ415F() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && "SM-J415F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungSMA920F() {
        return "samsung".equalsIgnoreCase(Build.BRAND) && "SM-A920F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isXiaomiMiA1() {
        return "xiaomi".equalsIgnoreCase(Build.BRAND) && "Mi A1".equalsIgnoreCase(Build.MODEL);
    }
}
