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
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.ArrayMap
import android.util.Range
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.common.compat.Api33Compat
import androidx.camera.common.compat.Api34Compat
import androidx.camera.common.compat.Api35Compat

@RequiresApi(31)
internal class AndroidCameraExtensionCharacteristics(
    cameraId: CameraId,
    @CameraExtension cameraExtension: Int,
    private val extensionCharacteristics: CameraExtensionCharacteristics,
    isRestricted: Boolean = false,
    restrictedKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
    dynamicKeys: Set<CameraCharacteristics.Key<*>> = emptySet(),
) : CameraExtensionCharacteristicsWrapper {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getCameraId")
    @get:Suppress("ValueClassUsageWithoutJvmName")
    override val cameraId: CameraId = cameraId

    @get:CameraExtension override val cameraExtension: Int = cameraExtension

    override val isRestricted: Boolean = isRestricted

    @GuardedBy("values") private val values = ArrayMap<CameraCharacteristics.Key<*>, Any?>()

    @GuardedBy("supportedExtensionSizesByFormat")
    private val supportedExtensionSizesByFormat = mutableMapOf<Int, Lazy<Set<Size>>>()

    @GuardedBy("supportedExtensionSizesByClass")
    private val supportedExtensionSizesByClass = mutableMapOf<Class<*>, Lazy<Set<Size>>>()

    @GuardedBy("supportedPostviewSizes")
    private val supportedPostviewSizes = mutableMapOf<Pair<Size, Int>, Lazy<Set<Size>>>()

    @GuardedBy("estimatedCaptureLatencyRangeMillis")
    private val estimatedCaptureLatencyRangeMillis =
        mutableMapOf<Pair<Size, Int>, Lazy<Range<Long>?>>()

    override fun <T : Any> get(key: CameraCharacteristics.Key<T>): T? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (dynamicKeys.contains(key)) {
                return Api35Compat.getExtensionCharacteristic(
                    extensionCharacteristics,
                    cameraExtension,
                    key,
                )
            }
            @Suppress("UNCHECKED_CAST") var result = synchronized(values) { values[key] } as T?
            if (result == null) {
                result =
                    Api35Compat.getExtensionCharacteristic(
                        extensionCharacteristics,
                        cameraExtension,
                        key,
                    )
                if (result != null) {
                    synchronized(values) { values[key] = result }
                }
            }
            return result
        }
        return null
    }

    override fun <T : Any> get(key: Metadata.Key<T>): T? = null

    override val metadataKeys: Set<Metadata.Key<*>>
        get() = emptySet()

    override fun <T : Any> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T {
        return get(key) ?: default
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == CameraExtensionCharacteristics::class.java -> extensionCharacteristics as T
            else -> null
        }

    override val isPostviewSupported: Boolean
        get() = _isPostviewSupported.value

    override val isCaptureProgressSupported: Boolean
        get() = _isCaptureProgressSupported.value

    override val keys: Set<CameraCharacteristics.Key<*>>
        get() = _keys.value

    override val captureRequestKeys: Set<CaptureRequest.Key<*>>
        get() = _captureRequestKeys.value

    override val captureResultKeys: Set<CaptureResult.Key<*>>
        get() = _captureResultKeys.value

    override val restrictedKeys: Set<CameraCharacteristics.Key<*>>
        get() = _restrictedKeys.value

    override val dynamicKeys: Set<CameraCharacteristics.Key<*>>
        get() = _dynamicKeys.value

    @Suppress("WrongConstant")
    override fun getOutputSizes(@ImageFormat imageFormat: Int): Set<Size> {
        val lazySizes =
            synchronized(supportedExtensionSizesByFormat) {
                supportedExtensionSizesByFormat.getOrPut(imageFormat) {
                    lazy(LazyThreadSafetyMode.PUBLICATION) {
                        extensionCharacteristics
                            .getExtensionSupportedSizes(cameraExtension, imageFormat)
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    @Suppress("WrongConstant")
    override fun getOutputSizes(klass: Class<*>): Set<Size> {
        val lazySizes =
            synchronized(supportedExtensionSizesByClass) {
                supportedExtensionSizesByClass.getOrPut(klass) {
                    lazy(LazyThreadSafetyMode.PUBLICATION) {
                        extensionCharacteristics
                            .getExtensionSupportedSizes(cameraExtension, klass)
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    override fun getPostviewSizes(captureSize: Size, @ImageFormat format: Int): Set<Size> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return emptySet()
        }
        val lazySizes =
            synchronized(supportedPostviewSizes) {
                supportedPostviewSizes.getOrPut(captureSize to format) {
                    lazy(LazyThreadSafetyMode.PUBLICATION) {
                        Api34Compat.getPostviewSupportedSizes(
                                extensionCharacteristics,
                                cameraExtension,
                                captureSize,
                                format,
                            )
                            .toSet()
                    }
                }
            }
        return lazySizes.value
    }

    @Suppress("WrongConstant")
    override fun getEstimatedCaptureLatencyRangeMillis(
        captureSize: Size,
        @ImageFormat imageFormat: Int,
    ): Range<Long>? {
        val lazyRange =
            synchronized(estimatedCaptureLatencyRangeMillis) {
                estimatedCaptureLatencyRangeMillis.getOrPut(captureSize to imageFormat) {
                    lazy(LazyThreadSafetyMode.PUBLICATION) {
                        extensionCharacteristics.getEstimatedCaptureLatencyRangeMillis(
                            cameraExtension,
                            captureSize,
                            imageFormat,
                        )
                    }
                }
            }
        return lazyRange.value
    }

    private val _keys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Api35Compat.getExtensionKeys(extensionCharacteristics, cameraExtension).toSet()
            } else {
                emptySet()
            }
        }

    private val _captureRequestKeys: Lazy<Set<CaptureRequest.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33Compat.getAvailableCaptureRequestKeys(
                        extensionCharacteristics,
                        cameraExtension,
                    )
                    .toSet()
            } else {
                emptySet()
            }
        }

    private val _captureResultKeys: Lazy<Set<CaptureResult.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33Compat.getAvailableCaptureResultKeys(extensionCharacteristics, cameraExtension)
                    .toSet()
            } else {
                emptySet()
            }
        }

    private val _isPostviewSupported: Lazy<Boolean> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34Compat.isPostviewAvailable(extensionCharacteristics, cameraExtension)
            } else {
                false
            }
        }

    private val _isCaptureProgressSupported: Lazy<Boolean> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34Compat.isCaptureProcessProgressAvailable(
                    extensionCharacteristics,
                    cameraExtension,
                )
            } else {
                false
            }
        }

    private val _restrictedKeys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) { restrictedKeys }

    private val _dynamicKeys: Lazy<Set<CameraCharacteristics.Key<*>>> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            if (dynamicKeys.isNotEmpty()) {
                dynamicKeys
            } else {
                keys
            }
        }
}
