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

package androidx.window.area.utils

import android.os.Build
import android.util.DisplayMetrics
import androidx.window.area.WindowAreaController
import androidx.window.extensions.area.WindowAreaComponent

/**
 * Object to provide util methods for non-standard behaviors around device metrics that are needed
 * or provided through the [WindowAreaController] API's that may exist on certain devices or certain
 * vendor API levels. This currently includes the rear display metrics for devices that support
 * WindowArea features, but do not currently support the [WindowAreaComponent.getRearDisplayMetrics]
 * method. This object can expand to include any differences that have to be taken into account that
 * vary from the standard behavior.
 */
internal object DeviceMetricsCompatUtils {

    private val deviceMetricsList =
        listOf(
            DeviceMetrics(
                "google",
                "pixel fold",
                DisplayMetrics().apply {
                    widthPixels = 1080
                    heightPixels = 2092
                    density = 2.625f
                    densityDpi = DisplayMetrics.DENSITY_420
                }
            )
        )

    fun hasDeviceMetrics(): Boolean {
        return getDeviceMetrics() != null
    }

    fun getDeviceMetrics(): DeviceMetrics? {
        return deviceMetricsList.firstOrNull { deviceMetrics ->
            deviceMetrics.brand.equals(Build.BRAND, ignoreCase = true) &&
                deviceMetrics.model.equals(Build.MODEL, ignoreCase = true)
        }
    }
}
