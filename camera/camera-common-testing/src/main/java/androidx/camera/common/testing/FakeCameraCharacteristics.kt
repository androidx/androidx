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
 * and physical camera IDs via its constructor or companion [create] method.
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
    override val restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
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

    override fun <T> get(key: Metadata.Key<T>): T? = cameraMetadata.getUnchecked(key)

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? =
        cameraCharacteristics.getUnchecked(key)

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
         * @param cameraId The camera ID string.
         * @param cameraCharacteristics The map of characteristics keys to their mock values.
         * @param cameraMetadata The map of custom metadata keys to their mock values.
         * @param captureRequestKeys The set of capture request keys supported by this camera.
         * @param captureResultKeys The set of capture result keys supported by this camera.
         * @param sessionKeys The set of session characteristics keys.
         * @param sessionCaptureRequestKeys The set of session keys.
         * @param physicalCameraIds The set of physical camera ID strings.
         * @param physicalCaptureRequestKeys The set of physical capture request keys.
         * @param restrictedKeys The set of keys that require camera permissions.
         * @return A configured [FakeCameraCharacteristics] instance.
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
            restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
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
                restrictedKeys = restrictedKeys,
            )
        }
    }
}
