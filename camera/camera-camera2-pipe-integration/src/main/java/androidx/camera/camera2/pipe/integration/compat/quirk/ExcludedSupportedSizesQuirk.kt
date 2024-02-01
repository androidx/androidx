/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Quirk

/**
 * Quirk required to exclude certain supported surface sizes that are problematic.
 *
 * QuirkSummary
 * Bug Id: b/157448499, b/192129158, b/245495234, b/303151423
 * Description:  These sizes are dependent on the device, camera and image format.
 * An example is the resolution size 4000x3000 which is supported on OnePlus 6,
 * but causes a WYSIWYG issue between preview and image capture. Another example
 * is on Huawei P20 Lite, the Preview screen will become too bright when 400x400
 * or 720x720 Preview resolutions are used together with a large zoom in value.
 * The same symptom happens on ImageAnalysis. On Samsung J7 Prime (SM-G610M) or
 * J7 (SM-J710MN) API 27 devices, the Preview images will be stretched if
 * 1920x1080 resolution is used.
 * Device(s): OnePlus 6, OnePlus 6T, Huawei P20, Samsung J7 Prime (SM-G610M) API 27, Samsung
 * J7 (SM-J710MN) API 27, Redmi Note 9 Pro
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ExcludedSupportedSizesQuirk : Quirk {
    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current
     * device, for the given camera id and image format.
     */
    fun getExcludedSizes(cameraId: String, imageFormat: Int): List<Size> {
        if (isOnePlus6) {
            return getOnePlus6ExcludedSizes(cameraId, imageFormat)
        }
        if (isOnePlus6T) {
            return getOnePlus6TExcludedSizes(cameraId, imageFormat)
        }
        if (isHuaweiP20Lite) {
            return getHuaweiP20LiteExcludedSizes(cameraId, imageFormat, null)
        }
        if (isSamsungJ7PrimeApi27Above) {
            return getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, imageFormat, null)
        }
        if (isSamsungJ7Api27Above) {
            return getSamsungJ7Api27AboveExcludedSizes(cameraId, imageFormat, null)
        }
        if (isRedmiNote9Pro) {
            return getRedmiNote9ProExcludedSizes(cameraId, imageFormat)
        }
        Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.")
        return emptyList()
    }

    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current
     * device, for the given camera id and class type.
     */
    fun getExcludedSizes(cameraId: String, klass: Class<*>): List<Size> {
        if (isHuaweiP20Lite) {
            return getHuaweiP20LiteExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
        }
        if (isSamsungJ7PrimeApi27Above) {
            return getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
        }
        if (isSamsungJ7Api27Above) {
            return getSamsungJ7Api27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
        }
        Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.")
        return emptyList()
    }

    private fun getOnePlus6ExcludedSizes(cameraId: String, imageFormat: Int): List<Size> {
        val sizes: MutableList<Size> = ArrayList()
        if ((cameraId == "0") && imageFormat == ImageFormat.JPEG) {
            sizes.add(Size(4160, 3120))
            sizes.add(Size(4000, 3000))
        }
        return sizes
    }

    private fun getOnePlus6TExcludedSizes(cameraId: String, imageFormat: Int): List<Size> {
        val sizes: MutableList<Size> = ArrayList()
        if ((cameraId == "0") && imageFormat == ImageFormat.JPEG) {
            sizes.add(Size(4160, 3120))
            sizes.add(Size(4000, 3000))
        }
        return sizes
    }

    private fun getHuaweiP20LiteExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?
    ): List<Size> {
        val sizes: MutableList<Size> = ArrayList()
        // When klass is not null, the list for PRIVATE format should be returned.
        if ((cameraId == "0") &&
            ((imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) ||
                (imageFormat == ImageFormat.YUV_420_888) || (klass != null))
        ) {
            sizes.add(Size(720, 720))
            sizes.add(Size(400, 400))
        }
        return sizes
    }

    private fun getSamsungJ7PrimeApi27AboveExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?
    ): List<Size> {
        val sizes: MutableList<Size> = ArrayList()

        // When klass is not null, the list for PRIVATE format should be returned.
        if ((cameraId == "0")) {
            if ((imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    klass != null)
            ) {
                sizes.add(Size(4128, 3096))
                sizes.add(Size(4128, 2322))
                sizes.add(Size(3088, 3088))
                sizes.add(Size(3264, 2448))
                sizes.add(Size(3264, 1836))
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                sizes.add(Size(4128, 2322))
                sizes.add(Size(3088, 3088))
                sizes.add(Size(3264, 2448))
                sizes.add(Size(3264, 1836))
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            }
        } else if ((cameraId == "1")) {
            if ((imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) ||
                (imageFormat == ImageFormat.YUV_420_888) || (klass != null)
            ) {
                sizes.add(Size(3264, 2448))
                sizes.add(Size(3264, 1836))
                sizes.add(Size(2448, 2448))
                sizes.add(Size(1920, 1920))
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            }
        }
        return sizes
    }

    private fun getSamsungJ7Api27AboveExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?
    ): List<Size> {
        val sizes: MutableList<Size> = ArrayList()

        // When klass is not null, the list for PRIVATE format should be returned.
        if (cameraId == "0") {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    klass != null
            ) {
                sizes.add(Size(4128, 3096))
                sizes.add(Size(4128, 2322))
                sizes.add(Size(3088, 3088))
                sizes.add(Size(3264, 2448))
                sizes.add(Size(3264, 1836))
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            }
        } else if (cameraId == "1") {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                imageFormat == ImageFormat.YUV_420_888 || klass != null
            ) {
                sizes.add(Size(2576, 1932))
                sizes.add(Size(2560, 1440))
                sizes.add(Size(1920, 1920))
                sizes.add(Size(2048, 1536))
                sizes.add(Size(2048, 1152))
                sizes.add(Size(1920, 1080))
            }
        }
        return sizes
    }

    private fun getRedmiNote9ProExcludedSizes(cameraId: String, imageFormat: Int): List<Size> {
        val sizes: MutableList<Size> = ArrayList()
        if (cameraId == "0" && imageFormat == ImageFormat.JPEG) {
            sizes.add(Size(9280, 6944))
        }
        return sizes
    }

    companion object {
        private const val TAG: String = "ExcludedSupportedSizesQuirk"
        private const val UNKNOWN_IMAGE_FORMAT: Int = -1
        fun isEnabled(): Boolean {
            return (isOnePlus6 || isOnePlus6T || isHuaweiP20Lite || isSamsungJ7PrimeApi27Above ||
                isSamsungJ7Api27Above || isRedmiNote9Pro)
        }

        internal val isOnePlus6: Boolean
            get() = "OnePlus".equals(Build.BRAND, ignoreCase = true) && "OnePlus6".equals(
                Build.DEVICE, ignoreCase = true
            )
        internal val isOnePlus6T: Boolean
            get() = "OnePlus".equals(Build.BRAND, ignoreCase = true) && "OnePlus6T".equals(
                Build.DEVICE, ignoreCase = true
            )
        internal val isHuaweiP20Lite: Boolean
            get() {
                return "HUAWEI".equals(
                    Build.BRAND,
                    ignoreCase = true
                ) && "HWANE".equals(Build.DEVICE, ignoreCase = true)
            }
        internal val isSamsungJ7PrimeApi27Above: Boolean
            get() {
                return ("SAMSUNG".equals(Build.BRAND, ignoreCase = true) &&
                    "ON7XELTE".equals(Build.DEVICE, ignoreCase = true) &&
                    (Build.VERSION.SDK_INT >= 27))
            }
        internal val isSamsungJ7Api27Above: Boolean
            get() {
                return ("SAMSUNG".equals(Build.BRAND, ignoreCase = true) &&
                    "J7XELTE".equals(Build.DEVICE, ignoreCase = true) &&
                    (Build.VERSION.SDK_INT >= 27))
            }
        internal val isRedmiNote9Pro: Boolean
            get() {
                return ("REDMI".equals(Build.BRAND, ignoreCase = true) &&
                    "joyeuse".equals(Build.DEVICE, ignoreCase = true))
            }
    }
}
