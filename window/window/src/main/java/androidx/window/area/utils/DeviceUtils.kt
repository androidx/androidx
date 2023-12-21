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
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Utility object to provide information about specific devices that may not be available
 * through the extensions API at a certain vendor API level
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
internal object DeviceUtils {

    private val deviceList = listOf(DeviceMetrics("google", "pixel fold",
        DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2092
            density = 2.625f
            densityDpi = 420 }
        ))

    internal fun hasDeviceMetrics(manufacturer: String, model: String): Boolean {
        return deviceList.any {
            it.manufacturer == manufacturer.lowercase(Locale.US) &&
                it.model == model.lowercase(Locale.US)
        }
    }

    internal fun getRearDisplayMetrics(manufacturer: String, model: String): DisplayMetrics? {
        return deviceList.firstOrNull {
            it.manufacturer == manufacturer.lowercase(Locale.US) &&
                it.model == model.lowercase(Locale.US)
        }?.rearDisplayMetrics
    }
}
