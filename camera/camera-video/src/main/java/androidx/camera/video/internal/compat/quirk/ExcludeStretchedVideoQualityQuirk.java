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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.video.Quality;

/**
 * <p>QuirkSummary
 *     Bug Id: 202792648, 245495234, 303054522
 *     Description: The captured video is stretched while selecting the quality is greater or
 *                  equality to FHD resolution
 *     Device(s): Samsung J2 (sm-j260f), Samsung J4 (sm-j400g), Samsung J5 (sm-j530f),
 *     Samsung J6 (sm-j600g), Samsung J7 Nxt (sm-j701f),
 *     Samsung J7 Prime (sm-g610m) API level 27 or above,
 *     Samsung J7 (sm-J710mn) API level 27 or above
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExcludeStretchedVideoQualityQuirk implements VideoQualityQuirk {
    static boolean load() {
        return isSamsungJ2() || isSamsungJ4() || isSamsungJ5() || isSamsungJ6() || isSamsungJ7Nxt()
                || isSamsungJ7PrimeApi27Above() || isSamsungJ7Api27Above();
    }

    private static boolean isSamsungJ2() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J260F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ4() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J400G".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ5() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J530F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ6() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "sm-j600g".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ7Nxt() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J701F".equalsIgnoreCase(Build.MODEL);
    }

    private static boolean isSamsungJ7PrimeApi27Above() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-G610M".equalsIgnoreCase(Build.MODEL)
                && Build.VERSION.SDK_INT >= 27;
    }

    private static boolean isSamsungJ7Api27Above() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && "SM-J710MN".equalsIgnoreCase(Build.MODEL)
                && Build.VERSION.SDK_INT >= 27;
    }

    /** Checks if the given Quality type is a problematic quality. */
    @Override
    public boolean isProblematicVideoQuality(@NonNull CameraInfoInternal cameraInfo,
            @NonNull Quality quality) {
        if (isSamsungJ4()) {
            return quality == Quality.FHD || quality == Quality.UHD;
        }
        if (isSamsungJ2() || isSamsungJ5() || isSamsungJ6() || isSamsungJ7Nxt()
                || isSamsungJ7PrimeApi27Above() || isSamsungJ7Api27Above()) {
            return quality == Quality.FHD;
        }
        return false;
    }
}
