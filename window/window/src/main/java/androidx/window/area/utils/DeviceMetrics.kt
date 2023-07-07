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

/**
 * Data class holding metrics about a specific device.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
internal class DeviceMetrics(
    val manufacturer: String,
    val model: String,
    val rearDisplayMetrics: DisplayMetrics
) {
    override fun equals(other: Any?): Boolean {
        return other is DeviceMetrics &&
            manufacturer == other.manufacturer &&
            model == other.model &&
            rearDisplayMetrics.equals(other.rearDisplayMetrics)
    }

    override fun hashCode(): Int {
        var result = manufacturer.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + rearDisplayMetrics.hashCode()
        return result
    }

    override fun toString(): String {
        return "DeviceMetrics{ Manufacturer: $manufacturer, model: $model, " +
            "Rear display metrics: $rearDisplayMetrics }"
    }
}
