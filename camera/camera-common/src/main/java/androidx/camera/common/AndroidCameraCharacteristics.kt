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
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.common.compat.Api28Compat
import androidx.camera.common.compat.Api29Compat
import androidx.camera.common.compat.Api35Compat
import java.lang.Class

/**
 * An implementation of [CameraCharacteristicsWrapper] that wraps a system [CameraCharacteristics].
 *
 * This implementation caches the values retrieved from the underlying [CameraCharacteristics] to
 * avoid repeated expensive binder calls to the camera service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AndroidCameraCharacteristics
@Suppress("ValueClassUsageFromConstructor")
public constructor(
    cameraId: CameraId,
    private val characteristics: CameraCharacteristics,
    override val isRestricted: Boolean = false,
    override val dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    private val metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
) : CameraCharacteristicsWrapper {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    @GuardedBy("values") private val values = ArrayMap<CameraCharacteristics.Key<*>, Any?>()

    /**
     * Retrieves the value of the specified [CameraCharacteristics.Key].
     *
     * The results are cached internally to prevent repeated blocking JNI/binder calls to the camera
     * service.
     *
     * @param key The key to query.
     * @return The value of the key, or `null` if the key is not present or unsupported.
     */
    override fun <T : Any> get(key: CameraCharacteristics.Key<T>): T? {
        if (dynamicKeys.contains(key)) {
            return characteristics.get(key)
        }
        // Cache the return value of calls to characteristics as the implementation performs a
        // blocking jni binder call which can be expensive when invoked frequently.
        @Suppress("UNCHECKED_CAST") var result = synchronized(values) { values[key] } as T?
        if (result == null) {
            result = characteristics.get(key)
            if (result != null) {
                synchronized(values) { values[key] = result }
            }
        }
        return result
    }

    override fun <T : Any> get(key: Metadata.Key<T>): T? = metadata.getUnchecked(key)

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = metadata.keys

    /**
     * Unwraps this object to access the underlying implementation type.
     *
     * Supported types:
     * - [AndroidCameraCharacteristics] (returns `this`)
     * - [CameraCharacteristics] (returns the wrapped [CameraCharacteristics] instance)
     *
     * @param type The class type to unwrap to.
     * @return The unwrapped instance of type [T], or `null` if the type is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == CameraCharacteristics::class.java -> characteristics as T
            else -> null
        }

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = _keys.value

    override val captureRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _captureRequestKeys.value

    override val captureResultKeys: Set<CaptureResult.Key<*>>
        get() = _captureResultKeys.value

    override val physicalCaptureRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _physicalCaptureRequestKeys.value

    override val sessionKeys: Set<CameraCharacteristics.Key<*>>
        get() = _sessionKeys.value

    override val sessionCaptureRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _sessionCaptureRequestKeys.value

    override val restrictedKeys: Set<CameraCharacteristics.Key<*>>
        get() = _restrictedKeys.value

    override val physicalCameraIds: Set<CameraId>
        get() = _physicalCameraIds.value

    private val _keys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) { characteristics.keys.orEmpty().toSet() }

    private val _captureRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            characteristics.availableCaptureRequestKeys.orEmpty().toSet()
        }

    private val _captureResultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            characteristics.availableCaptureResultKeys.orEmpty().toSet()
        }

    private val _physicalCaptureRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                Api28Compat.getAvailablePhysicalCameraRequestKeys(characteristics).orEmpty().toSet()
            }
        }

    private val _sessionKeys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                emptySet()
            } else {
                Api35Compat.getAvailableSessionCharacteristicsKeys(characteristics)
                    .orEmpty()
                    .toSet()
            }
        }

    private val _sessionCaptureRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                Api28Compat.getAvailableSessionKeys(characteristics).orEmpty().toSet()
            }
        }

    private val _restrictedKeys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                emptySet()
            } else {
                Api29Compat.getKeysNeedingPermission(characteristics).orEmpty().toSet()
            }
        }

    private val _physicalCameraIds: Lazy<Set<CameraId>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                Api28Compat.getPhysicalCameraIds(characteristics)
                    .orEmpty()
                    .map { CameraId(it) }
                    .toSet()
            }
        }
}
