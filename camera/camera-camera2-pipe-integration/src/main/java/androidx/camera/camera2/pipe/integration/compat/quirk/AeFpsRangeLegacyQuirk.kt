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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.impl.Quirk

/**
 *
 * QuirkSummary
 * - Bug Id: b/167425305
 * - Description: Quirk required to maintain good exposure on legacy devices by specifying a
 *                proper [android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE].
 *                Legacy devices set the AE target FPS range to [30, 30]. This can potentially
 *                cause underexposure issues.
 *                [androidx.camera.camera2.internal.compat.workaround.AeFpsRange]
 *                contains a workaround that is used on legacy devices to set a AE FPS range
 *                whose upper bound is 30, which guarantees a smooth frame rate, and whose lower
 *                bound is as small as possible to properly expose frames in low light
 *                conditions. The default behavior on non legacy devices does not add the AE
 *                FPS range option.
 * - Device(s): All legacy devices
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class AeFpsRangeLegacyQuirk(cameraMetadata: CameraMetadata) : Quirk {
    /**
     * Returns the fps range whose upper is 30 and whose lower is the smallest, or null if no
     * range has an upper equal to 30.  The rationale is:
     * - Range upper is always 30 so that a smooth frame rate is guaranteed.
     * - Range lower contains the smallest supported value so that it can adapt as much as
     * possible to low light conditions.
     */
    val range: Range<Int>? by lazy {
        val availableFpsRanges: Array<out Range<Int>>? =
            cameraMetadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
        pickSuitableFpsRange(availableFpsRanges)
    }

    private fun pickSuitableFpsRange(
        availableFpsRanges: Array<out Range<Int>>?
    ): Range<Int>? {
        if (availableFpsRanges.isNullOrEmpty()) {
            return null
        }
        var pickedRange: Range<Int>? = null
        for (fpsRangeBeforeCorrection in availableFpsRanges) {
            val fpsRange = getCorrectedFpsRange(fpsRangeBeforeCorrection)
            if (fpsRange.upper != 30) {
                continue
            }
            if (pickedRange == null) {
                pickedRange = fpsRange
            } else {
                if (fpsRange.lower < pickedRange.lower) {
                    pickedRange = fpsRange
                }
            }
        }
        return pickedRange
    }

    /**
     * On android 5.0/5.1, [CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
     * returns wrong ranges whose values were multiplied by 1000. So we need to convert them to the
     * correct values.
     */
    private fun getCorrectedFpsRange(fpsRange: Range<Int>): Range<Int> {
        var newUpper = fpsRange.upper
        var newLower = fpsRange.lower
        if (fpsRange.upper >= 1000) {
            newUpper = fpsRange.upper / 1000
        }
        if (fpsRange.lower >= 1000) {
            newLower = fpsRange.lower / 1000
        }
        return Range(newLower, newUpper)
    }

    companion object {
        fun isEnabled(cameraMetadata: CameraMetadata): Boolean {
            val level = cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            return level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        }
    }
}