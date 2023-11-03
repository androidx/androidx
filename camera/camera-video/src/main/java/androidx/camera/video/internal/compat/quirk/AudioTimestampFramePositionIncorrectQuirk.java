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

package androidx.camera.video.internal.compat.quirk;

import android.media.AudioTimestamp;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: 245518008, 301067226
 *     Description: Quirk which denotes {@link android.media.AudioTimestamp#framePosition} queried
 *                  by {@link android.media.AudioRecord#getTimestamp(AudioTimestamp, int)} returns
 *                  incorrect info. On some devices, the frame position will become negative after
 *                  multiple recordings.
 *     Device(s): LG K10 (2017), Moto C, Realme C2, Redmi 6A, Vivo 1820 and some Oppo devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioTimestampFramePositionIncorrectQuirk implements Quirk {

    private static final List<String> AFFECTED_OPPO_MODELS = Arrays.asList(
            "cph1920", // Oppo AX5s
            "cph1923", // Oppo A1k
            "cph2015", // Oppo A31
            "cph2083" // Oppo A12
    );

    static boolean load() {
        return isAffectedOppoDevices() || isLgK10() || isMotoC() || isRealmeC2() || isRedmi6A()
                || isVivo1820();
    }

    private static boolean isAffectedOppoDevices() {
        return "oppo".equalsIgnoreCase(Build.BRAND)
                && AFFECTED_OPPO_MODELS.contains(Build.MODEL.toLowerCase(Locale.ROOT));
    }

    private static boolean isLgK10() {
        return "lge".equalsIgnoreCase(Build.BRAND) && "lg-m250".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isRealmeC2() {
        return "realme".equalsIgnoreCase(Build.BRAND) && "rmx1941".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isRedmi6A() {
        return "Xiaomi".equalsIgnoreCase(Build.BRAND) && "Redmi 6A".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isVivo1820() {
        return "vivo".equalsIgnoreCase(Build.BRAND) && "vivo 1820".equalsIgnoreCase(Build.MODEL);
    }
}
