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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata

/** Contains the CameraX-specific logic for [CameraMetadata]. */
public val CameraMetadata.availableAfModes: List<Int>
    get() =
        getOrDefault(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
                intArrayOf(CaptureRequest.CONTROL_AF_MODE_OFF)
            )
            .asList()

public val CameraMetadata.availableAeModes: List<Int>
    get() =
        getOrDefault(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
                intArrayOf(CaptureRequest.CONTROL_AE_MODE_OFF)
            )
            .asList()

public val CameraMetadata.availableAwbModes: List<Int>
    get() =
        getOrDefault(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES,
                intArrayOf(CaptureRequest.CONTROL_AWB_MODE_OFF)
            )
            .asList()

/** If preferredMode not available, priority is CONTINUOUS_PICTURE > AUTO > OFF */
public fun CameraMetadata.getSupportedAfMode(preferredMode: Int): Int =
    when {
        availableAfModes.contains(preferredMode) -> {
            preferredMode
        }
        availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) -> {
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        }
        availableAfModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) -> {
            CaptureRequest.CONTROL_AF_MODE_AUTO
        }
        else -> {
            CaptureRequest.CONTROL_AF_MODE_OFF
        }
    }

/** If preferredMode not available, priority is AE_ON > AE_OFF */
public fun CameraMetadata.getSupportedAeMode(preferredMode: Int): Int =
    when {
        availableAeModes.contains(preferredMode) -> {
            preferredMode
        }
        availableAeModes.contains(CaptureRequest.CONTROL_AE_MODE_ON) -> {
            CaptureRequest.CONTROL_AE_MODE_ON
        }
        else -> {
            CaptureRequest.CONTROL_AE_MODE_OFF
        }
    }

private fun CameraMetadata.isAeModeSupported(aeMode: Int) = getSupportedAeMode(aeMode) == aeMode

/** Returns whether [CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH] is supported. */
public fun CameraMetadata.isExternalFlashAeModeSupported(): Boolean =
    Build.VERSION.SDK_INT >= 28 &&
        isAeModeSupported(CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)

/** If preferredMode not available, priority is AWB_AUTO > AWB_OFF */
public fun CameraMetadata.getSupportedAwbMode(preferredMode: Int): Int =
    when {
        availableAwbModes.contains(preferredMode) -> {
            preferredMode
        }
        availableAwbModes.contains(CaptureRequest.CONTROL_AWB_MODE_AUTO) -> {
            CaptureRequest.CONTROL_AWB_MODE_AUTO
        }
        else -> {
            CaptureRequest.CONTROL_AWB_MODE_OFF
        }
    }

public fun <T> CameraMetadata?.getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
    this?.getOrDefault(key, default) ?: default
