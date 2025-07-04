/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.camera.viewfinder.core.impl.quirk

import android.os.Build

/**
 * A quirk where SurfaceView is stretched.
 *
 * QuirkSummary Bug Id: 129403806 Description: On certain Samsung devices, transform APIs (e.g.
 * View#setScaleX) do not work as intended. Device(s): Samsung Fold2 F2Q, Samsung Fold3 Q2Q, Oppo
 * Find N OP4E75L1, Lenovo P12 Pro
 */
internal object SurfaceViewStretchedQuirk : Quirk {
    // Samsung Galaxy Z Fold2 b/129403806
    private const val SAMSUNG = "SAMSUNG"
    private const val GALAXY_Z_FOLD_2 = "F2Q"
    private const val GALAXY_Z_FOLD_3 = "Q2Q"
    private const val OPPO = "OPPO"
    private const val OPPO_FIND_N = "OP4E75L1"
    private const val LENOVO = "LENOVO"
    private const val LENOVO_TAB_P12_PRO = "Q706F"

    @JvmStatic
    fun load(): Boolean {
        // The surface view issue is fixed in Android T.
        return Build.VERSION.SDK_INT < 33 &&
            (isSamsungFold2OrFold3 || isOppoFoldable || isLenovoTablet)
    }

    private val isSamsungFold2OrFold3: Boolean
        get() =
            SAMSUNG.equals(Build.MANUFACTURER, ignoreCase = true) &&
                (GALAXY_Z_FOLD_2.equals(Build.DEVICE, ignoreCase = true) ||
                    GALAXY_Z_FOLD_3.equals(Build.DEVICE, ignoreCase = true))

    private val isOppoFoldable: Boolean
        get() =
            OPPO.equals(Build.MANUFACTURER, ignoreCase = true) &&
                OPPO_FIND_N.equals(Build.DEVICE, ignoreCase = true)

    private val isLenovoTablet: Boolean
        get() =
            LENOVO.equals(Build.MANUFACTURER, ignoreCase = true) &&
                LENOVO_TAB_P12_PRO.equals(Build.DEVICE, ignoreCase = true)
}
