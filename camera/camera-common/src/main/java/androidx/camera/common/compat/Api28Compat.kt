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

package androidx.camera.common.compat

import android.hardware.HardwareBuffer
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.media.Image
import androidx.annotation.RequiresApi

/** Helper class to avoid class verification errors for APIs introduced in API level 28. */
@RequiresApi(28)
internal object Api28Compat {
    /**
     * Returns the [HardwareBuffer] associated with the given [Image], or `null` if the image does
     * not support hardware buffers.
     *
     * Wraps [Image.getHardwareBuffer].
     *
     * @param image The image to retrieve the hardware buffer from.
     * @return The hardware buffer, or `null`.
     */
    @JvmStatic
    fun getHardwareBuffer(image: Image): HardwareBuffer? {
        return image.hardwareBuffer
    }

    /**
     * Returns a list of keys that are supported for physical camera requests.
     *
     * Wraps [CameraCharacteristics.getAvailablePhysicalCameraRequestKeys].
     *
     * @param cameraCharacteristics The camera characteristics to retrieve the keys from.
     * @return The list of physical camera request keys, or `null` if not supported.
     */
    @JvmStatic
    fun getAvailablePhysicalCameraRequestKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availablePhysicalCameraRequestKeys
    }

    /**
     * Returns a list of keys that can be passed to [CaptureRequest] when configuring a session.
     *
     * Wraps [CameraCharacteristics.getAvailableSessionKeys].
     *
     * @param cameraCharacteristics The camera characteristics to retrieve the keys from.
     * @return The list of session keys, or `null` if not supported.
     */
    @JvmStatic
    fun getAvailableSessionKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availableSessionKeys
    }

    /**
     * Returns the set of physical camera IDs that this logical camera is composed of.
     *
     * Wraps [CameraCharacteristics.getPhysicalCameraIds].
     *
     * @param cameraCharacteristics The camera characteristics.
     * @return The set of physical camera IDs, or an empty set if it is not a logical camera.
     */
    @JvmStatic
    fun getPhysicalCameraIds(cameraCharacteristics: CameraCharacteristics): Set<String> {
        return cameraCharacteristics.physicalCameraIds
    }

    /**
     * Returns a list of keys contained in the [CaptureRequest].
     *
     * Wraps [CaptureRequest.getKeys].
     *
     * @param captureRequest The capture request to retrieve the keys from.
     * @return A list of keys for the request.
     */
    @JvmStatic
    fun getKeys(captureRequest: CaptureRequest): List<CaptureRequest.Key<*>> {
        return captureRequest.keys
    }

    /**
     * Returns a list of keys contained in the [CaptureResult].
     *
     * Wraps [CaptureResult.getKeys].
     *
     * @param captureResult The capture result to retrieve the keys from.
     * @return A list of keys for the result.
     */
    @JvmStatic
    fun getKeys(captureResult: CaptureResult): List<CaptureResult.Key<*>> {
        return captureResult.keys
    }
}
