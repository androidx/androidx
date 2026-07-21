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
 * `CameraCharacteristicsWrapper` implements [Metadata], allowing it to support type-safe retrieval
 * of custom library-defined metadata keys via [Metadata.Key] in addition to native
 * [CameraCharacteristics.Key] keys.
 *
 * It also implements [UnsafeWrapper], which allows unwrapping to the underlying native
 * [CameraCharacteristics] using [UnsafeWrapper.unwrapAs] when platform-specific APIs are required.
 * Note that bypassing the wrapper by unwrapping avoids compatibility fixes and caching.
 *
 * **Note:** This interface is not stable for inheritance. Implementations should not be created
 * directly by clients. For testing, use the fakes in `androidx.camera.common.testing` package (such
 * as `FakeCameraCharacteristics`).
 *
 * ### Example
 *
 * Querying native and custom keys:
 * ```kotlin
 * val characteristics: CameraCharacteristicsWrapper = ...
 *
 * // Query a native CameraCharacteristics key:
 * val lensFacing: Int? = characteristics[CameraCharacteristics.LENS_FACING]
 *
 * // Query a custom Metadata key:
 * val customValue: String? = characteristics[MY_CUSTOM_METADATA_KEY]
 * ```
 *
 * Unwrapping the native object (unsafe):
 * ```kotlin
 * val nativeCharacteristics: CameraCharacteristics? = characteristics.unwrapAs()
 * ```
 */
public interface CameraCharacteristicsWrapper : Metadata, UnsafeWrapper {
    /**
     * The [CameraId] identifying this camera device.
     *
     * This corresponds to the camera ID used to open the camera with
     * [android.hardware.camera2.CameraManager.openCamera].
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    public val cameraId: CameraId

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key].
     *
     * Calls to this method may be cached by the implementation to avoid repeated expensive IPC/JNI
     * queries to the camera service.
     *
     * Additionally, on certain API levels, the wrapper may emulate keys that are not natively
     * supported by the platform, returning a compatible fallback value.
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    public operator fun <T> get(key: CameraCharacteristics.Key<T>): T?

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key], or returns [default] if the
     * value is `null` or the key is unsupported.
     *
     * @param key The key to query.
     * @param default The value to return if the key is not present or unsupported.
     * @return The value of the key, or [default] if null.
     */
    public fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return get(key) ?: default
    }

    /**
     * A [Set] of all [CameraCharacteristics.Key]s supported by this camera device.
     *
     * This property is equivalent to calling [CameraCharacteristics.getKeys] on the underlying
     * camera characteristics, but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeys
     */
    public val keys: Set<CameraCharacteristics.Key<*>>

    /**
     * A [Set] of all [CaptureRequest.Key]s supported by this camera device for [CaptureRequest]s.
     *
     * This property is equivalent to calling
     * [CameraCharacteristics.getAvailableCaptureRequestKeys], but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureRequestKeys
     */
    public val captureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of all [CaptureResult.Key]s supported by this camera device for [CaptureResult]s.
     *
     * This property is equivalent to calling [CameraCharacteristics.getAvailableCaptureResultKeys],
     * but may be cached for efficiency.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableCaptureResultKeys
     */
    public val captureResultKeys: Set<CaptureResult.Key<*>>

    /**
     * A [Set] of physical [CaptureRequest.Key]s that can be overridden for physical devices backing
     * a logical multi-camera.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailablePhysicalCameraRequestKeys
     */
    public val physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of [CameraCharacteristics.Key]s whose values are capture session specific.
     *
     * On API levels prior to 35 (Android 15), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionCharacteristicsKeys
     */
    public val sessionKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * A [Set] of [CaptureRequest.Key]s that the camera device can pass as part of the capture
     * session initialization.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getAvailableSessionKeys
     */
    public val sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>>

    /**
     * A [Set] of [CameraCharacteristics.Key]s that require camera clients to obtain the
     * `Manifest.permission.CAMERA` permission.
     *
     * On API levels prior to 29 (Android Q), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getKeysNeedingPermission
     */
    public val restrictedKeys: Set<CameraCharacteristics.Key<*>>

    /**
     * A [Set] of physical camera IDs that this logical camera device is made up of.
     *
     * On API levels prior to 28 (Android P), this property returns an empty set.
     *
     * @see android.hardware.camera2.CameraCharacteristics.getPhysicalCameraIds
     */
    public val physicalCameraIds: Set<CameraId>
}
