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

package androidx.camera.common.testing

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata
import androidx.camera.common.getUnchecked
import java.lang.Class

/**
 * A fake implementation of [CameraCharacteristicsWrapper] for testing.
 *
 * Allows mock values to be configured for camera characteristics, custom metadata, supported keys,
 * and physical camera IDs. This class is designed to be used in unit tests to mock camera behavior
 * without needing a physical device or a real camera service.
 *
 * @param cameraId The [CameraId] identifying this camera device. Defaults to
 *   [FakeCameraIds.default].
 * @param cameraCharacteristics A map of [CameraCharacteristics.Key] to their mock values. Any key
 *   queried via [get] will return the value from this map.
 * @param cameraMetadata A map of custom [Metadata.Key] to their mock values. Any key queried via
 *   [get] will return the value from this map.
 * @param captureRequestKeys The set of [CaptureRequest.Key]s supported by this camera.
 * @param captureResultKeys The set of [CaptureResult.Key]s supported by this camera.
 * @param sessionKeys The set of [CameraCharacteristics.Key]s whose values are capture session
 *   specific. All keys in this set must also be present in `cameraCharacteristics`.
 * @param sessionCaptureRequestKeys The set of [CaptureRequest.Key]s that the camera device can pass
 *   as part of the capture session initialization.
 * @param physicalCameraIds The set of physical [CameraId]s that this logical camera device is made
 *   up of. Must not contain `cameraId`.
 * @param physicalCaptureRequestKeys The set of physical [CaptureRequest.Key]s.
 * @param isRestricted Indicates whether the wrapper queried camera characteristics without camera
 *   permission.
 * @param restrictedKeys The set of [CameraCharacteristics.Key]s that require camera permissions.
 *   All keys in this set must also be present in `cameraCharacteristics`.
 * @param dynamicKeys The set of [CameraCharacteristics.Key]s whose values can change depending on
 *   the device state.
 * @throws IllegalArgumentException If `physicalCameraIds` contains `cameraId`, or if any key in
 *   `restrictedKeys` or `sessionKeys` is not present in `cameraCharacteristics`.
 */
public class FakeCameraCharacteristics
@Suppress("ValueClassUsageFromConstructor")
constructor(
    cameraId: CameraId = FakeCameraIds.default,
    private val cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
    private val cameraMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val captureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val captureResultKeys: Set<CaptureResult.Key<*>> = emptySet(),
    override val sessionKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val physicalCameraIds: Set<CameraId> = emptySet(),
    override val physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    override val isRestricted: Boolean = false,
    override val restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    override val dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
) : CameraCharacteristicsWrapper {

    init {
        require(!physicalCameraIds.contains(cameraId)) {
            "physicalCameraIds ($physicalCameraIds) should not contain cameraId ($cameraId)"
        }
        for (key in restrictedKeys) {
            require(cameraCharacteristics.containsKey(key)) {
                "restrictedKey ($key) must be present in cameraCharacteristics keys"
            }
        }
        for (key in sessionKeys) {
            require(cameraCharacteristics.containsKey(key)) {
                "sessionKey ($key) must be present in cameraCharacteristics keys"
            }
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = cameraMetadata.keys

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = cameraCharacteristics.keys

    /**
     * Retrieves the mock value for the specified [Metadata.Key].
     *
     * @param key The custom metadata key to query.
     * @return The mock value configured in [cameraMetadata], or `null` if not present.
     */
    override fun <T : Any> get(key: Metadata.Key<T>): T? = cameraMetadata.getUnchecked(key)

    /**
     * Retrieves the mock value for the specified [CameraCharacteristics.Key].
     *
     * @param key The native characteristics key to query.
     * @return The mock value configured in [cameraCharacteristics], or `null` if not present.
     */
    override fun <T : Any> get(key: CameraCharacteristics.Key<T>): T? =
        cameraCharacteristics.getUnchecked(key)

    /**
     * Unwraps this instance to the requested type if compatible.
     *
     * Since this is a fake implementation, it can only be unwrapped to [FakeCameraCharacteristics]
     * itself or its supertypes (e.g., [CameraCharacteristicsWrapper]). It cannot be unwrapped to a
     * native [android.hardware.camera2.CameraCharacteristics].
     *
     * @param type The class of the type to unwrap to.
     * @return This instance cast to [T] if compatible, or `null` otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            else -> null
        }

    public companion object {
        /**
         * Creates a [FakeCameraCharacteristics] instance for Java compatibility.
         *
         * @param cameraId The camera ID string. Defaults to [FakeCameraIds.default]'s value.
         * @param cameraCharacteristics The map of characteristics keys to their mock values.
         * @param cameraMetadata The map of custom metadata keys to their mock values.
         * @param captureRequestKeys The set of capture request keys supported by this camera.
         * @param captureResultKeys The set of capture result keys supported by this camera.
         * @param sessionKeys The set of session characteristics keys. All keys in this set must
         *   also be present in `cameraCharacteristics`.
         * @param sessionCaptureRequestKeys The set of session keys.
         * @param physicalCameraIds The set of physical camera ID strings. Must not contain
         *   `cameraId`.
         * @param physicalCaptureRequestKeys The set of physical capture request keys.
         * @param isRestricted True if the camera is operating in a restricted mode.
         * @param restrictedKeys The set of keys that require camera permissions. All keys in this
         *   set must also be present in `cameraCharacteristics`.
         * @param dynamicKeys The set of keys that can change dynamically.
         * @return A configured [FakeCameraCharacteristics] instance.
         * @throws IllegalArgumentException If `physicalCameraIds` contains `cameraId`, or if any
         *   key in `restrictedKeys` or `sessionKeys` is not present in `cameraCharacteristics`.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            cameraId: String = FakeCameraIds.default.value,
            cameraCharacteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
            cameraMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            captureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
            captureResultKeys: Set<CaptureResult.Key<*>> = emptySet(),
            sessionKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
            physicalCameraIds: Set<String> = emptySet(),
            physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
            isRestricted: Boolean = false,
            restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
            dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
        ): FakeCameraCharacteristics {
            return FakeCameraCharacteristics(
                cameraId = CameraId(cameraId),
                cameraCharacteristics = cameraCharacteristics,
                cameraMetadata = cameraMetadata,
                captureRequestKeys = captureRequestKeys,
                captureResultKeys = captureResultKeys,
                sessionKeys = sessionKeys,
                sessionCaptureRequestKeys = sessionCaptureRequestKeys,
                physicalCameraIds = physicalCameraIds.map { CameraId(it) }.toSet(),
                physicalCaptureRequestKeys = physicalCaptureRequestKeys,
                isRestricted = isRestricted,
                restrictedKeys = restrictedKeys,
                dynamicKeys = dynamicKeys,
            )
        }
    }
}
