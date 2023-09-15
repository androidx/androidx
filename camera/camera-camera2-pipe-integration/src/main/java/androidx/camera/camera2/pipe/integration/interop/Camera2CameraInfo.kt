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

package androidx.camera.camera2.pipe.integration.interop

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.getSafely
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraInfoInternal

/**
 * An interface for retrieving Camera2-related camera information.
 */
@ExperimentalCamera2Interop
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class Camera2CameraInfo private constructor(
    internal val cameraProperties: CameraProperties,
) {

    /**
     * Gets a camera characteristic value.
     *
     * The characteristic value is the same as the value in the [CameraCharacteristics]
     * that would be obtained from
     * [android.hardware.camera2.CameraManager.getCameraCharacteristics].
     *
     * @param <T> The type of the characteristic value.
     * @param key The [CameraCharacteristics.Key] of the characteristic.
     * @return the value of the characteristic.
    </T> */
    fun <T> getCameraCharacteristic(
        key: CameraCharacteristics.Key<T>
    ): T? {
        return cameraProperties.metadata.getSafely(key)
    }

    /**
     * Gets the string camera ID.
     *
     *
     * The camera ID is the same as the camera ID that would be obtained from
     * [android.hardware.camera2.CameraManager.getCameraIdList]. The ID that is retrieved
     * is not static and can change depending on the current internal configuration of the
     * [androidx.camera.core.Camera] from which the CameraInfo was retrieved.
     *
     * The Camera is a logical camera which can be backed by multiple
     * [android.hardware.camera2.CameraDevice]. However, only one CameraDevice is active at
     * one time. When the CameraDevice changes then the camera id will change.
     *
     * @return the camera ID.
     * @throws IllegalStateException if the camera info does not contain the camera 2 camera ID
     * (e.g., if CameraX was not initialized with a
     * [androidx.camera.camera2.Camera2Config]).
     */

    fun getCameraId(): String = cameraProperties.cameraId.value

    /**
     * Returns a map consisting of the camera ids and the
     * [android.hardware.camera2.CameraCharacteristics]s.
     *
     * For every camera, the map contains at least the CameraCharacteristics for the camera id.
     * If the camera is logical camera, it will also contain associated physical camera ids and
     * their CameraCharacteristics.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getCameraCharacteristicsMap(): Map<String, CameraCharacteristics> {
        return buildMap {
            put(
                cameraProperties.cameraId.value,
                cameraProperties.metadata.unwrapAs(CameraCharacteristics::class)!!
            )
            for (cameraId in cameraProperties.metadata.physicalCameraIds) {
                put(
                    cameraId.value,
                    cameraProperties.metadata.awaitPhysicalMetadata(cameraId)
                        .unwrapAs(CameraCharacteristics::class)!!
                )
            }
        }
    }

    companion object {

        /**
         * Gets the [Camera2CameraInfo] from a [CameraInfo].
         *
         * @param cameraInfo The [CameraInfo] to get from.
         * @return The camera information with Camera2 implementation.
         * @throws IllegalArgumentException if the camera info does not contain the camera2 information
         * (e.g., if CameraX was not initialized with a
         * [androidx.camera.camera2.Camera2Config]).
         */
        @JvmStatic
        fun from(@Suppress("UNUSED_PARAMETER") cameraInfo: CameraInfo): Camera2CameraInfo {
            var cameraInfoImpl = (cameraInfo as CameraInfoInternal).implementation
            require(cameraInfoImpl is CameraInfoAdapter) {
                "CameraInfo doesn't contain Camera2 implementation."
            }
            return cameraInfoImpl.camera2CameraInfo
        }

        /**
         * This is the workaround to prevent constructor from being added to public API.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun create(cameraProperties: CameraProperties) = Camera2CameraInfo(cameraProperties)

        /**
         * Returns the [android.hardware.camera2.CameraCharacteristics] for this camera.
         *
         * The CameraCharacteristics will be the ones that would be obtained by
         * [android.hardware.camera2.CameraManager.getCameraCharacteristics]. The
         * CameraCharacteristics that are retrieved are not static and can change depending on the
         * current internal configuration of the camera.
         *
         * @param cameraInfo The [CameraInfo] to extract the CameraCharacteristics from.
         * @throws IllegalStateException if the camera info does not contain the camera 2
         *                               characteristics(e.g., if CameraX was not initialized with a
         *                               [androidx.camera.camera2.Camera2Config]).
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun extractCameraCharacteristics(cameraInfo: CameraInfo): CameraCharacteristics {
            var cameraInfoImpl = (cameraInfo as CameraInfoInternal).implementation
            require(cameraInfoImpl is CameraInfoAdapter) {
                "CameraInfo doesn't contain Camera2 implementation."
            }
            return cameraInfoImpl.camera2CameraInfo
                .cameraProperties.metadata.unwrapAs(CameraCharacteristics::class)!!
        }
    }
}
