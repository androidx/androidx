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
import androidx.camera.camera2.pipe.core.lazyOrEmptySet
import androidx.camera.camera2.pipe.core.lazyOrFalse
import kotlin.reflect.KClass

/**
 * This implementation provides access to [CameraExtensionMetadata] and lazy caching of properties
 * that are either expensive to create and access, or that only exist on newer versions of the OS.
 * This allows all fields to be accessed and return reasonable values on all OS versions.
 */
@RequiresApi(Build.VERSION_CODES.S)
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

    @GuardedBy("supportedPostviewSizes")
    private val supportedPostviewSizes = mutableMapOf<Size, Lazy<Set<Size>>>()

    override fun <T> get(key: CameraCharacteristics.Key<T>): T? {
        return null // TODO: Add support for this when VIC can be targeted in AndroidX
    }

    @Suppress("UNCHECKED_CAST") override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

    override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return default // TODO: Add support for this when VIC can be targeted in AndroidX
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T =
        metadata[key] as T? ?: default

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraExtensionCharacteristics::class -> extensionCharacteristics as T
            else -> null
        }

    override val isPostviewSupported: Boolean
        get() = _isPostviewSupported.value

    override val isCaptureProgressSupported: Boolean
        get() = _isCaptureProgressSupported.value

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = emptySet() // TODO: Add support for this when VIC can be targeted in AndroidX

    override val requestKeys: Set<CaptureRequest.Key<*>>
        get() = _requestKeys.value

    override val resultKeys: Set<CaptureResult.Key<*>>
        get() = _resultKeys.value

    override fun getOutputSizes(imageFormat: Int): Set<Size> {
        val lazySizes =
            synchronized(supportedExtensionSizesByFormat) {
                supportedExtensionSizesByFormat.getOrPut(imageFormat) {
                    lazyOrEmptySet({ "$camera#getExtensionSupportedSizes(${imageFormat})" }) {
                        Api31Compat.getExtensionSupportedSizes(
                                extensionCharacteristics,
                                cameraExtension,
                                imageFormat
                            )
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    override fun getOutputSizes(klass: Class<*>): Set<Size> {
        val lazySizes =
            synchronized(supportedExtensionSizesByClass) {
                supportedExtensionSizesByClass.getOrPut(klass) {
                    lazyOrEmptySet("$camera#getExtensionSupportedSizes(${klass.name})") {
                        Api31Compat.getExtensionSupportedSizes(
                                extensionCharacteristics,
                                cameraExtension,
                                klass
                            )
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    override fun getPostviewSizes(captureSize: Size, format: Int): Set<Size> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return emptySet()
        }

        val lazySizes =
            synchronized(supportedPostviewSizes) {
                supportedPostviewSizes.getOrPut(captureSize) {
                    lazyOrEmptySet("$camera#getPostviewSupportedSizes($captureSize, $format)") {
                        Api34Compat.getPostviewSupportedSizes(
                                extensionCharacteristics,
                                cameraExtension,
                                captureSize,
                                format
                            )
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    private val _requestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazyOrEmptySet({ "$camera#availableCaptureRequestKeys" }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33Compat.getAvailableCaptureRequestKeys(
                        extensionCharacteristics,
                        cameraExtension
                    )
                    .toSet()
            } else {
                emptySet()
            }
        }

    private val _resultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazyOrEmptySet({ "$camera#availableCaptureResultKeys" }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33Compat.getAvailableCaptureResultKeys(extensionCharacteristics, cameraExtension)
                    .toSet()
            } else {
                emptySet()
            }
        }

    private val _isPostviewSupported: Lazy<Boolean> =
        lazyOrFalse({ "$camera#isPostviewSupported" }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34Compat.isPostviewAvailable(extensionCharacteristics, cameraExtension)
            } else {
                false
            }
        }

    private val _isCaptureProgressSupported: Lazy<Boolean> =
        lazyOrFalse({ "$camera#isCaptureProgressSupported" }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34Compat.isCaptureProcessProgressAvailable(
                    extensionCharacteristics,
                    cameraExtension
                )
            } else {
                false
            }
        }
}
