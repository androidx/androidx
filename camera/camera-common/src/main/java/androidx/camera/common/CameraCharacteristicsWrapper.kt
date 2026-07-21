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

package androidx.camera.common

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult

/**
 * Wrapper interface providing compatibility-focused access to [CameraCharacteristics].
 *
 * Use this interface to query camera capabilities, physical properties, and supported
 * configurations. It abstracts OS version differences and caches expensive-to-retrieve properties
 * to ensure efficient access across all API levels.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCameraCharacteristics`).
 *
 * ### Example
 *
 * ```kotlin
 * val cameraMetadata: CameraCharacteristicsWrapper = ...
 * val lensFacing = cameraMetadata[CameraCharacteristics.LENS_FACING]
 * ```
 */
public interface CameraCharacteristicsWrapper : Metadata, UnsafeWrapper {
    /** The [CameraId] identifying this camera. */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key].
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key], or returns [default] if not
     * found.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * Set of all [CameraCharacteristics.Key]s supported by this camera device.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeys
     */
    public val keys: Set<CameraCharacteristics.Key<*>>

    /**
     * Set of all [CaptureRequest.Key]s supported by this camera device.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureRequestKeys
     */
    public val captureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * Set of all [CaptureResult.Key]s supported by this camera device.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureResultKeys
     */
    public val captureResultKeys: Set<CaptureResult.Key<*>>

    /**
     * Set of physical [CaptureRequest.Key]s that can be overridden for physical devices backing a
     * logical multi-camera.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailablePhysicalCameraRequestKeys
     */
    public val physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * Set of [CameraCharacteristics.Key]s whose values are capture session specific.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionCharacteristicsKeys
     */
    public val sessionKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * Set of [CaptureRequest.Key]s that the camera device can pass as part of the capture session
     * initialization.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionKeys
     */
    public val sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * Set of [CameraCharacteristics.Key]s that require camera clients to obtain the
     * `Manifest.permission.CAMERA` permission.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeysNeedingPermission
     */
    public val restrictedKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * Set of physical camera ids that this logical camera device is made up of.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getPhysicalCameraIds
     */
    public val physicalCameraIds: Set<CameraId>
}
