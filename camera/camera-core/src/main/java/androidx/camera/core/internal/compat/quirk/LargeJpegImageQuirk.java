/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 288828159, 299069235
 *     Description: Quirk required to check whether the captured JPEG image contains redundant
 *                  0's padding data. For example, Samsung A5 (2017) series devices have the
 *                  problem and result in the output JPEG image to be extremely large (about 32 MB).
 *     Device(s): Samsung Galaxy A5 (2017), A52, A70, A71, A72, M51, S7, S22, S22+ series devices
 *                and Vivo S16, X60, X60 Pro devices. This issue might also happen on some other
 *                Samsung devices. Therefore, a generic rule is added to force check the invalid
 *                JPEG data if the captured image size is larger than 10 MB.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class LargeJpegImageQuirk implements Quirk {

    private static final int INVALID_JPEG_DATA_CHECK_THRESHOLD = 10_000_000; // 10 MB

    private static final Set<String> SAMSUNG_DEVICE_MODELS = new HashSet<>(Arrays.asList(
            // Samsung Galaxy A5 series devices
            "SM-A520F",
            "SM-A520L",
            "SM-A520K",
            "SM-A520S",
            "SM-A520X",
            "SM-A520W",
            // Samsung Galaxy A52 series devices
            "SM-A525F",
            "SM-A525M",
            // Samsung Galaxy A70 series devices
            "SM-A705F",
            "SM-A705FN",
            "SM-A705GM",
            "SM-A705MN",
            "SM-A7050",
            "SM-A705W",
            "SM-A705YN",
            "SM-A705U",
            // Samsung Galaxy A71 series devices
            "SM-A715F",
            "SM-A715F/DS",
            "SM-A715F/DSM",
            "SM-A715F/DSN",
            "SM-A715W",
            "SM-A715X",
            // Samsung Galaxy A72 series devices
            "SM-A725F",
            "SM-A725M",
            // Samsung Galaxy M51 series devices
            "SM-M515F",
            "SM-M515F/DSN",
            // Samsung Galaxy S7 series devices
            "SM-G930T",
            "SM-G930V",
            // Samsung Galaxy S22 series devices
            "SM-S901B",
            "SM-S901B/DS",
            // Samsung Galaxy S22+ series device
            "SM-S906B"
    ));

    private static final Set<String> VIVO_DEVICE_MODELS = new HashSet<>(Arrays.asList(
            // Vivo S16
            "V2244A",
            // Vivo X60
            "V2045",
            // Vivo X60 Pro
            "V2046"
    ));

    static boolean load() {
        return isSamsungDevice() || isVivoProblematicDevice();
    }

    private static boolean isSamsungProblematicDevice() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && SAMSUNG_DEVICE_MODELS.contains(
                Build.MODEL.toUpperCase(Locale.US));
    }

    private static boolean isSamsungDevice() {
        return "Samsung".equalsIgnoreCase(Build.BRAND);
    }

    private static boolean isVivoProblematicDevice() {
        return "Vivo".equalsIgnoreCase(Build.BRAND) && VIVO_DEVICE_MODELS.contains(
                Build.MODEL.toUpperCase(Locale.US));
    }

    /**
     * Return {@code true} if there might be invalid JPEG data contained in the bytes array.
     */
    public boolean shouldCheckInvalidJpegData(@NonNull byte[] bytes) {
        // For the confirmed problematic devices, always check the invalid data
        if (isSamsungProblematicDevice() || isVivoProblematicDevice()) {
            return true;
        } else {
            // For other devices, check the invalid data if the image size is larger than 10 MB
            return bytes.length > INVALID_JPEG_DATA_CHECK_THRESHOLD;
        }
    }
}
