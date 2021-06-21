/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.core.Debug

/**
 * This implementation provides access to [CameraCharacteristics] and lazy caching of properties
 * that are either expensive to create and access, or that only exist on newer versions of the
 * OS. This allows all fields to be accessed and return reasonable values on all OS versions.
 */
internal class Camera2CameraMetadata constructor(
    override val camera: CameraId,
    override val isRedacted: Boolean,
    private val characteristics: CameraCharacteristics,
    private val metadataProvider: CameraMetadataProvider,
    private val metadata: Map<Metadata.Key<*>, Any?>,
) : CameraMetadata {
    @GuardedBy("values")
    private val values = ArrayMap<CameraCharacteristics.Key<*>, Any?>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T =
        metadata[key] as T? ?: default

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? {
        // Cache the return value of calls to characteristics as the implementation performs a
        // blocking jni binder call which can be expensive when invoked frequently (see b/144028609
        // for more details).

        // Implementation notes:
        // 1. Null return values may eventually turn non-null on subsequent queries (due to
        //    permissions). Null return values are not cached and are always re-queried.
        //
        // 2. Non-null return values will never turn null, and will not change once returned. If
        //    permissions are revoked, this should result in the app process being restarted.
        //
        // 3. Duplicate non-null values are expected to be identical (even if the object instance
        //    is different), and so it does not matter which value is cached if two calls from
        //    different threads try to read the value simultaneously.
        @Suppress("UNCHECKED_CAST")
        var result = synchronized(values) { values[key] } as T?
        if (result == null) {
            result = characteristics.get(key)
            if (result != null) {
                synchronized(values) {
                    values[key] = result
                }
            }
        }
        return result
    }

    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    override fun unwrap(): CameraCharacteristics = characteristics

    override val keys: Set<CameraCharacteristics.Key<*>> get() = _keys.value
    override val requestKeys: Set<CaptureRequest.Key<*>> get() = _requestKeys.value
    override val resultKeys: Set<CaptureResult.Key<*>> get() = _resultKeys.value
    override val sessionKeys: Set<CaptureRequest.Key<*>> get() = _sessionKeys.value
    override val physicalCameraIds: Set<CameraId> get() = _physicalCameraIds.value
    override val physicalRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _physicalRequestKeys.value

    override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata {
        check(physicalCameraIds.contains(cameraId)) {
            "$cameraId is not a valid physical camera on $this"
        }
        return metadataProvider.getMetadata(cameraId)
    }

    override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata {
        check(physicalCameraIds.contains(cameraId)) {
            "$cameraId is not a valid physical camera on $this"
        }
        return metadataProvider.awaitMetadata(cameraId)
    }

    private val _keys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#keys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.keys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet()
            }
        }

    private val _requestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#availableCaptureRequestKeys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.availableCaptureRequestKeys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet()
            }
        }

    private val _resultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-${camera.value}#availableCaptureResultKeys") {
                    @Suppress("UselessCallOnNotNull")
                    characteristics.availableCaptureResultKeys.orEmpty().toSet()
                }
            } catch (ignored: AssertionError) {
                emptySet()
            }
        }

    private val _physicalCameraIds: Lazy<Set<CameraId>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#physicalCameraIds") {
                        @Suppress("UselessCallOnNotNull")
                        Api28Compat.getPhysicalCameraIds(characteristics)
                            .orEmpty()
                            .map { CameraId(it) }
                            .toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet()
                }
            }
        }

    private val _physicalRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#availablePhysicalCameraRequestKeys") {
                        Api28Compat.getAvailablePhysicalCameraRequestKeys(characteristics)
                            .orEmpty()
                            .toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet()
                }
            }
        }

    private val _sessionKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("Camera-${camera.value}#availableSessionKeys") {
                        Api28Compat.getAvailableSessionKeys(characteristics).orEmpty().toSet()
                    }
                } catch (ignored: AssertionError) {
                    emptySet()
                }
            }
        }
}
