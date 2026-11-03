/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import android.util.Size

/**
 * Checks whether Low Light Boost can be used for a camera stream: the resolution limits of the
 * Google Play Services pipeline, and the device and camera support cached by
 * [PlayServicesLowLightBoostAvailability]. The checks don't report anything; callers report the
 * returned [UnsupportedReason].
 */
internal object LowLightBoostSupport {
    /** Why Low Light Boost can't be used: the [error] to report and a [logMessage]. */
    internal class UnsupportedReason(val error: Throwable, val logMessage: String)

    /** Maximum width or height (in pixels) supported by Google Play Services Low Light Boost. */
    private const val MAX_SUPPORTED_DIMENSION = 2560

    /**
     * Maximum pixel area (2048 * 2048 = 4 * 1024 * 1024 = 4,194,304 pixels, covering standard
     * camera resolutions up to 2560x1440 16:9 and 2048x2048 1:1) supported by the Low Light Boost
     * pipeline.
     */
    private const val MAX_SUPPORTED_AREA_PIXELS = 4L * 1024 * 1024

    /**
     * Returns true if [resolution] satisfies Google Play Services Low Light Boost hardware and
     * model constraints (`width <= 2560`, `height <= 2560`, and `width * height <= 4,194,304`).
     */
    internal fun isResolutionSupported(resolution: Size): Boolean {
        return resolution.width <= MAX_SUPPORTED_DIMENSION &&
            resolution.height <= MAX_SUPPORTED_DIMENSION &&
            (resolution.width.toLong() * resolution.height.toLong() <= MAX_SUPPORTED_AREA_PIXELS)
    }

    /**
     * Returns why Low Light Boost can't be used for [resolution] and [cameraId]: the resolution
     * exceeds the Low Light Boost bounds, or the device or the camera has been cached as
     * unsupported. Returns null otherwise.
     */
    internal fun unsupportedReasonOrNull(resolution: Size, cameraId: String): UnsupportedReason? {
        if (!isResolutionSupported(resolution)) {
            return UnsupportedReason(
                error =
                    IllegalArgumentException(
                        "Unsupported resolution for Low Light Boost: $resolution. " +
                            "Low Light Boost requires width <= $MAX_SUPPORTED_DIMENSION, " +
                            "height <= $MAX_SUPPORTED_DIMENSION, and area <= " +
                            "$MAX_SUPPORTED_AREA_PIXELS pixels (4MP)."
                    ),
                logMessage =
                    "Resolution $resolution exceeds maximum supported resolution for Low Light " +
                        "Boost. Falling back to passthrough.",
            )
        }
        return cachedUnsupportedReasonOrNull(cameraId)
    }

    /**
     * Returns why Low Light Boost can't be used if the device or [cameraId] has been cached as
     * unsupported, or null otherwise.
     */
    internal fun cachedUnsupportedReasonOrNull(cameraId: String): UnsupportedReason? {
        if (PlayServicesLowLightBoostAvailability.getCachedDeviceSupported() == false) {
            return unsupportedDeviceReason()
        }
        if (PlayServicesLowLightBoostAvailability.getCachedCameraSupported(cameraId) == false) {
            return UnsupportedReason(
                error =
                    UnsupportedOperationException(
                        "Low Light Boost is not supported on camera $cameraId."
                    ),
                logMessage =
                    "Camera $cameraId does not support Low Light Boost. Falling back to " +
                        "passthrough.",
            )
        }
        return null
    }

    /** Returns the reason for a device that doesn't support Low Light Boost. */
    internal fun unsupportedDeviceReason(): UnsupportedReason =
        UnsupportedReason(
            error =
                UnsupportedOperationException("Low Light Boost is not supported on this device."),
            logMessage = "Device does not support Low Light Boost. Falling back to passthrough.",
        )
}
