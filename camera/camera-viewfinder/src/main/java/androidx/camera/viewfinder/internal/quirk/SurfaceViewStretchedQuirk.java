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

import androidx.annotation.RequiresApi;

/**
 * A quirk where SurfaceView is stretched.
 *
 * <p>QuirkSummary
 *     Bug Id: 129403806
 *     Description: On certain Samsung devices, transform APIs (e.g. View#setScaleX) do not work
 *                  as intended.
 *     Device(s): SAMSUNG F2Q Q2Q
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceViewStretchedQuirk implements Quirk {

    // Samsung Galaxy Z Fold2 b/129403806
    private static final String SAMSUNG = "SAMSUNG";
    private static final String GALAXY_Z_FOLD_2 = "F2Q";
    private static final String GALAXY_Z_FOLD_3 = "Q2Q";

    static boolean load() {
        return SAMSUNG.equalsIgnoreCase(Build.MANUFACTURER) && isFold2OrFold3();
    }

    static boolean isFold2OrFold3() {
        return GALAXY_Z_FOLD_2.equalsIgnoreCase(Build.DEVICE)
                || GALAXY_Z_FOLD_3.equalsIgnoreCase(Build.DEVICE);
    }
}
