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

package androidx.camera.viewfinder.internal.quirk;

import android.os.Build;

/**
 * A quirk where SurfaceView is stretched.
 *
 * <p>QuirkSummary
 *     Bug Id: 129403806
 *     Description: On certain Samsung devices, transform APIs (e.g. View#setScaleX) do not work
 *                  as intended.
 *     Device(s): Samsung Fold2 F2Q, Samsung Fold3 Q2Q, Oppo Find N OP4E75L1, Lenovo P12 Pro
 */
public class SurfaceViewStretchedQuirk implements Quirk {

    // Samsung Galaxy Z Fold2 b/129403806
    private static final String SAMSUNG = "SAMSUNG";
    private static final String GALAXY_Z_FOLD_2 = "F2Q";
    private static final String GALAXY_Z_FOLD_3 = "Q2Q";
    private static final String OPPO = "OPPO";
    private static final String OPPO_FIND_N = "OP4E75L1";
    private static final String LENOVO = "LENOVO";
    private static final String LENOVO_TAB_P12_PRO = "Q706F";

    static boolean load() {
        // The surface view issue is fixed in Android T.
        return  Build.VERSION.SDK_INT < 33
                && (isSamsungFold2OrFold3() || isOppoFoldable() || isLenovoTablet());
    }

    private static boolean isSamsungFold2OrFold3() {
        return SAMSUNG.equalsIgnoreCase(Build.MANUFACTURER)
                && (GALAXY_Z_FOLD_2.equalsIgnoreCase(Build.DEVICE)
                || GALAXY_Z_FOLD_3.equalsIgnoreCase(Build.DEVICE));
    }

    private static boolean isOppoFoldable() {
        return OPPO.equalsIgnoreCase(Build.MANUFACTURER)
                && OPPO_FIND_N.equalsIgnoreCase(Build.DEVICE);
    }

    private static boolean isLenovoTablet() {
        return LENOVO.equalsIgnoreCase(Build.MANUFACTURER)
                && LENOVO_TAB_P12_PRO.equalsIgnoreCase(Build.DEVICE);
    }
}
