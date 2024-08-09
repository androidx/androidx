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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.quirk.ControlZoomRatioRangeAssertionErrorQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.internal.ZoomMath.nearZero

/**
 * Gets a [CameraCharacteristics] value with additional error handling.
 *
 * @param T the type of the characteristic value
 * @param key the [CameraCharacteristics.Key] of the characteristic
 * @return the value of the characteristic
 */
public fun <T> CameraMetadata.getSafely(key: CameraCharacteristics.Key<T>): T? {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            key == CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE
    ) {
        @Suppress("UNCHECKED_CAST") // T is guaranteed to be Range<Float>
        return getControlZoomRatioRangeSafely() as T?
    }

    return get(key)
}

/**
 * Gets the value of [CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE] with additional error
 * handling.
 *
 * Some devices may throw [AssertionError] when getting the value of
 * [CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]. Here, the error is caught and logged before
 * returning null as workaround.
 *
 * Ref: b/231701345
 *
 * @return the CONTROL_ZOOM_RATIO_RANGE characteristic value, null in case of [AssertionError].
 */
@RequiresApi(Build.VERSION_CODES.R)
public fun CameraMetadata.getControlZoomRatioRangeSafely(): Range<Float>? =
    try {
        var range = get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        if (range == null) {
            Log.warn { "Failed to read CONTROL_ZOOM_RATIO_RANGE for $camera!" }
            Range(1.0f, 1.0f)
        } else {
            val lower =
                if (nearZero(range.lower) || range.lower < 0.0f) {
                    Log.warn { "Invalid lower zoom range detected: ${range.lower}" }
                    1.0f
                } else {
                    range.lower
                }
            val upper =
                if (nearZero(range.upper) || range.upper < 0.0f) {
                    Log.warn { "Invalid upper zoom range detected: ${range.upper}" }
                    1.0f
                } else {
                    range.upper
                }
            Range(lower, upper)
        }
    } catch (e: AssertionError) {
        if (DeviceQuirks[ControlZoomRatioRangeAssertionErrorQuirk::class.java] != null) {
            Log.debug {
                "Device is known to throw an exception while retrieving" +
                    " the value for CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE." +
                    " CONTROL_ZOOM_RATIO_RANGE is not supported." +
                    " [Manufacturer: ${Build.MANUFACTURER}, Model:" +
                    " ${Build.MODEL}, API Level: ${Build.VERSION.SDK_INT}]."
            }
        } else {
            Log.error(e) {
                "Exception thrown while retrieving the value for" +
                    " CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE on devices not known to " +
                    "throw exceptions during this operation. Please file an issue at " +
                    "https://issuetracker.google.com/issues/new?component=618491&template=1257717" +
                    " with this error message [Manufacturer: ${Build.MANUFACTURER}, Model:" +
                    " ${Build.MODEL}, API Level: ${Build.VERSION.SDK_INT}]." +
                    " CONTROL_ZOOM_RATIO_RANGE is not available."
            }
        }

        Log.warn(e) {
            "AssertionError: " + "failed to get CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE"
        }
        null
    }
