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
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import kotlin.reflect.KClass

/**
 * This implementation provides access to [CameraCharacteristics] and lazy caching of properties
 * that are either expensive to create and access, or that only exist on newer versions of the OS.
 * This allows all fields to be accessed and return reasonable values on all OS versions.
 */
internal class Camera2CameraMetadata(
    override val camera: CameraId,
    override val isRedacted: Boolean,
    private val characteristics: CameraCharacteristics,
    private val metadataProvider: Camera2MetadataProvider,
    private val metadata: Map<Metadata.Key<*>, Any?>,
    private val cacheBlocklist: Set<CameraCharacteristics.Key<*>>,
) : CameraMetadata {
    @GuardedBy("values") private val values = ArrayMap<CameraCharacteristics.Key<*>, Any?>()

    @GuardedBy("extensionCache")
    private val extensionCache = ArrayMap<Int, CameraExtensionMetadata>()

    // TODO: b/299356087 - this here may need a switch statement on the key
    @Suppress("UNCHECKED_CAST") override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T =
        metadata[key] as T? ?: default

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? {
        if (cacheBlocklist.contains(key)) {
            return characteristics.getOrThrow(key)
        }

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
        @Suppress("UNCHECKED_CAST") var result = synchronized(values) { values[key] } as T?
        if (result == null) {
            result = characteristics.getOrThrow(key)
            if (result != null) {
                synchronized(values) { values[key] = result }
            }
        }
        return result
    }

    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
        get(key) ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraCharacteristics::class -> characteristics as T
            else -> null
        }

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = _keys.value

    override val requestKeys: Set<CaptureRequest.Key<*>>
        get() = _requestKeys.value

    override val resultKeys: Set<CaptureResult.Key<*>>
        get() = _resultKeys.value

    override val sessionKeys: Set<CaptureRequest.Key<*>>
        get() = _sessionKeys.value

    override val physicalCameraIds: Set<CameraId>
        get() = _physicalCameraIds.value

    override val physicalRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _physicalRequestKeys.value

    override val supportedExtensions: Set<Int>
        get() = _supportedExtensions.value

    override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata {
        check(physicalCameraIds.contains(cameraId)) {
            "$cameraId is not a valid physical camera on $this"
        }
        return metadataProvider.getCameraMetadata(cameraId)
    }

    override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata {
        check(physicalCameraIds.contains(cameraId)) {
            "$cameraId is not a valid physical camera on $this"
        }
        return metadataProvider.awaitCameraMetadata(cameraId)
    }

    override suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata {
        val existing = synchronized(extensionCache) { extensionCache[extension] }
        return if (existing != null) {
            existing
        } else {
            val extensionMetadata = metadataProvider.getCameraExtensionMetadata(camera, extension)
            synchronized(extensionCache) { extensionCache[extension] = extensionMetadata }
            extensionMetadata
        }
    }

    override fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata {
        val existing = synchronized(extensionCache) { extensionCache[extension] }
        return if (existing != null) {
            existing
        } else {
            val extensionMetadata = metadataProvider.awaitCameraExtensionMetadata(camera, extension)
            synchronized(extensionCache) { extensionCache[extension] = extensionMetadata }
            extensionMetadata
        }
    }

    private val _supportedExtensions: Lazy<Set<Int>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-$camera#supportedExtensions") {
                    metadataProvider.getSupportedCameraExtensions(camera)
                }
            } catch (e: AssertionError) {
                Log.warn(e) { "Failed to getSupportedExtensions from Camera-$camera" }
                emptySet()
            }
        }

    private val _keys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("$camera#keys") {
                    @Suppress("UselessCallOnNotNull") // Untrusted API
                    characteristics.keys.orEmpty().toSet()
                }
            } catch (e: AssertionError) {
                Log.warn(e) { "Failed to getKeys from $camera}" }
                emptySet()
            }
        }

    private val _requestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("$camera#availableCaptureRequestKeys") {
                    @Suppress("UselessCallOnNotNull") // Untrusted API
                    characteristics.availableCaptureRequestKeys.orEmpty().toSet()
                }
            } catch (e: AssertionError) {
                Log.warn(e) { "Failed to getAvailableCaptureRequestKeys from $camera" }
                emptySet()
            }
        }

    private val _resultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("$camera#availableCaptureResultKeys") {
                    @Suppress("UselessCallOnNotNull") // Untrusted API
                    characteristics.availableCaptureResultKeys.orEmpty().toSet()
                }
            } catch (e: AssertionError) {
                Log.warn(e) { "Failed to getAvailableCaptureResultKeys from $camera" }
                emptySet()
            }
        }

    private val _physicalCameraIds: Lazy<Set<CameraId>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                emptySet()
            } else {
                try {
                    Debug.trace("$camera#physicalCameraIds") {
                        val ids = Api28Compat.getPhysicalCameraIds(characteristics)
                        Log.info { "Loaded physicalCameraIds from $camera: $ids" }

                        @Suppress("UselessCallOnNotNull") ids.orEmpty().map { CameraId(it) }.toSet()
                    }
                } catch (e: AssertionError) {
                    Log.warn(e) { "Failed to getPhysicalCameraIds from $camera" }
                    emptySet()
                } catch (e: NullPointerException) {
                    Log.warn(e) { "Failed to getPhysicalCameraIds from $camera" }
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
                } catch (e: AssertionError) {
                    Log.warn(e) {
                        "Failed to getAvailablePhysicalCameraRequestKeys from " +
                            "Camera-${camera.value}"
                    }
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
                } catch (e: AssertionError) {
                    Log.warn(e) { "Failed to getAvailableSessionKeys from Camera-${camera.value}" }
                    emptySet()
                }
            }
        }

    private fun <T> CameraCharacteristics.getOrThrow(key: CameraCharacteristics.Key<T>): T? {
        try {
            return this.get(key)
        } catch (exception: AssertionError) {
            throw IllegalStateException(
                "Failed to get characteristic for $key: Framework throw an AssertionError"
            )
        }
    }
}
