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

import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import kotlin.reflect.KClass

/**
 * This implementation provides access to [CameraExtensionMetadata] and lazy caching of
 * properties that are either expensive to create and access, or that only exist on newer versions
 * of the OS. This allows all fields to be accessed and return reasonable values on all OS versions.
 */
@RequiresApi(Build.VERSION_CODES.S)
// TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2CameraExtensionMetadata(
    override val camera: CameraId,
    override val isRedacted: Boolean,
    override val cameraExtension: Int,
    private val extensionCharacteristics: CameraExtensionCharacteristics,
    private val metadata: Map<Metadata.Key<*>, Any?>
) : CameraExtensionMetadata {
    @GuardedBy("supportedExtensionSizesByFormat")
    private val supportedExtensionSizesByFormat = mutableMapOf<Int, Lazy<Set<Size>>>()

    @GuardedBy("supportedExtensionSizesByClass")
    private val supportedExtensionSizesByClass = mutableMapOf<Class<*>, Lazy<Set<Size>>>()

    // TODO: b/299356087 - this here may need a switch statement on the key
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T =
        metadata[key] as T? ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraExtensionCharacteristics::class -> extensionCharacteristics as T
            else -> null
        }

    override val requestKeys: Set<CaptureRequest.Key<*>>
        get() = _requestKeys.value
    override val resultKeys: Set<CaptureResult.Key<*>>
        get() = _resultKeys.value

    override fun getOutputSizes(imageFormat: Int): Set<Size> {
        val supportedExtensionSizes = synchronized(supportedExtensionSizesByFormat) {
            supportedExtensionSizesByFormat.getOrPut(imageFormat) {
                lazy(LazyThreadSafetyMode.PUBLICATION) {
                    Api31Compat.getExtensionSupportedSizes(
                        extensionCharacteristics,
                        cameraExtension,
                        imageFormat
                    ).toSet()
                }
            }
        }
        return supportedExtensionSizes.value
    }

    override fun getOutputSizes(klass: Class<*>): Set<Size> {
        val supportedExtensionSizes = synchronized(supportedExtensionSizesByClass) {
            supportedExtensionSizesByClass.getOrPut(klass) {
                lazy(LazyThreadSafetyMode.PUBLICATION) {
                    Api31Compat.getExtensionSupportedSizes(
                        extensionCharacteristics,
                        cameraExtension,
                        klass
                    ).toSet()
                }
            }
        }
        return supportedExtensionSizes.value
    }

    private val _requestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-$camera#availableCaptureRequestKeys") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Api33Compat.getAvailableCaptureRequestKeys(
                            extensionCharacteristics,
                            cameraExtension
                        ).toSet()
                    } else {
                        emptySet()
                    }
                }
            } catch (e: AssertionError) {
                Log.warn(e) {
                    "Failed to getAvailableCaptureRequestKeys from Camera-$camera"
                }
                emptySet()
            }
        }

    private val _resultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            try {
                Debug.trace("Camera-$camera#availableCaptureResultKeys") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Api33Compat.getAvailableCaptureResultKeys(
                            extensionCharacteristics,
                            cameraExtension
                        ).toSet()
                    } else {
                        emptySet()
                    }
                }
            } catch (e: AssertionError) {
                Log.warn(e) {
                    "Failed to getAvailableCaptureResultKeys from Camera-$camera"
                }
                emptySet()
            }
        }
}
