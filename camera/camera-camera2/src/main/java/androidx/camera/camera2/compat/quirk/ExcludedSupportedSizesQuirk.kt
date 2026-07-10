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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import androidx.camera.camera2.compat.quirk.Device.isHuaweiDevice
import androidx.camera.camera2.compat.quirk.Device.isNokiaDevice
import androidx.camera.camera2.compat.quirk.Device.isOnePlusDevice
import androidx.camera.camera2.compat.quirk.Device.isRedmiDevice
import androidx.camera.camera2.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.Logger
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Quirk

/**
 * Quirk required to exclude certain supported surface sizes that are problematic.
 *
 * QuirkSummary
 * - Bug Id: b/157448499, b/192129158, b/245495234, b/303151423, b/365877975, b/436524501,
 *   b/460322307
 * - Description: These sizes are dependent on the device, camera and image format. An example is
 *   the resolution size 4000x3000 which is supported on OnePlus 6, but causes a WYSIWYG issue
 *   between preview and image capture. Another example is on Huawei P20 Lite, the Preview screen
 *   will become too bright when 400x400 or 720x720 Preview resolutions are used together with a
 *   large zoom in value. The same symptom happens on ImageAnalysis. On Samsung J7 Prime (SM-G610M)
 *   or J7 (SM-J710MN) API 27 devices, the Preview images will be stretched if 1920x1080 resolution
 *   is used. On Samsung A05s (SM-A057G) device, black preview issue can happen when ImageAnalysis
 *   uses output sizes larger than 1920x1080. On Samsung Z Fold 4 device, the ImageAnalysis images
 *   have distortion issue.
 * - Device(s): OnePlus 6, OnePlus 6T, Huawei P20, Samsung J7 Prime (SM-G610M) API 27, Samsung J7
 *   (SM-J710MN) API 27, Redmi Note 9 Pro, Samsung A05s (SM-A057G), Nokia 7 plus, Samsung Z Fold 4
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ExcludedSupportedSizesQuirk : Quirk {
    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current device,
     * for the given camera id and image format.
     */
    public fun getExcludedSizes(cameraId: String, imageFormat: Int): List<Size> =
        when {
            isOnePlus6 -> getOnePlus6ExcludedSizes(cameraId, imageFormat)
            isOnePlus6T -> getOnePlus6TExcludedSizes(cameraId, imageFormat)
            isHuaweiP20Lite -> getHuaweiP20LiteExcludedSizes(cameraId, imageFormat, null)
            isSamsungJ7PrimeApi27Above ->
                getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, imageFormat, null)
            isSamsungJ7Api27Above ->
                getSamsungJ7Api27AboveExcludedSizes(cameraId, imageFormat, null)
            isRedmiNote9Pro -> getRedmiNote9ProExcludedSizes(cameraId, imageFormat)
            isSamsungA05s -> getSamsungA05sExcludedSizes(imageFormat)
            isNokia7Plus -> getNokia7PlusExcludedSizes(imageFormat)
            isSamsungZFold4 -> getSamsungZFold4ExcludedSizes(cameraId, imageFormat)
            else -> {
                Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.")
                emptyList()
            }
        }

    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current device,
     * for the given camera id and class type.
     */
    public fun getExcludedSizes(cameraId: String, klass: Class<*>): List<Size> =
        when {
            isHuaweiP20Lite -> getHuaweiP20LiteExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
            isSamsungJ7PrimeApi27Above ->
                getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
            isSamsungJ7Api27Above ->
                getSamsungJ7Api27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass)
            else -> {
                Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.")
                emptyList()
            }
        }

    private fun getOnePlus6ExcludedSizes(cameraId: String, imageFormat: Int): List<Size> =
        if (cameraId == "0" && imageFormat == ImageFormat.JPEG) {
            listOf(Size(4160, 3120), Size(4000, 3000))
        } else {
            emptyList()
        }

    private fun getOnePlus6TExcludedSizes(cameraId: String, imageFormat: Int): List<Size> =
        if (cameraId == "0" && imageFormat == ImageFormat.JPEG) {
            listOf(Size(4160, 3120), Size(4000, 3000))
        } else {
            emptyList()
        }

    private fun getHuaweiP20LiteExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?,
    ): List<Size> =
        if (
            cameraId == "0" &&
                (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    imageFormat == ImageFormat.YUV_420_888 ||
                    klass != null)
        ) {
            listOf(Size(720, 720), Size(400, 400))
        } else {
            emptyList()
        }

    private fun getSamsungJ7PrimeApi27AboveExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?,
    ): List<Size> {
        if (cameraId == "0") {
            if (
                imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    klass != null
            ) {
                return listOf(
                    Size(4128, 3096),
                    Size(4128, 2322),
                    Size(3088, 3088),
                    Size(3264, 2448),
                    Size(3264, 1836),
                    Size(2048, 1536),
                    Size(2048, 1152),
                    Size(1920, 1080),
                )
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                return listOf(
                    Size(4128, 2322),
                    Size(3088, 3088),
                    Size(3264, 2448),
                    Size(3264, 1836),
                    Size(2048, 1536),
                    Size(2048, 1152),
                    Size(1920, 1080),
                )
            }
        } else if (cameraId == "1") {
            if (
                imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    imageFormat == ImageFormat.YUV_420_888 ||
                    klass != null
            ) {
                return listOf(
                    Size(3264, 2448),
                    Size(3264, 1836),
                    Size(2448, 2448),
                    Size(1920, 1920),
                    Size(2048, 1536),
                    Size(2048, 1152),
                    Size(1920, 1080),
                )
            }
        }
        return emptyList()
    }

    private fun getSamsungJ7Api27AboveExcludedSizes(
        cameraId: String,
        imageFormat: Int,
        klass: Class<*>?,
    ): List<Size> {
        if (cameraId == "0") {
            if (
                imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    klass != null
            ) {
                return listOf(
                    Size(4128, 3096),
                    Size(4128, 2322),
                    Size(3088, 3088),
                    Size(3264, 2448),
                    Size(3264, 1836),
                    Size(2048, 1536),
                    Size(2048, 1152),
                    Size(1920, 1080),
                )
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                return listOf(Size(2048, 1536), Size(2048, 1152), Size(1920, 1080))
            }
        } else if (cameraId == "1") {
            if (
                imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE ||
                    imageFormat == ImageFormat.YUV_420_888 ||
                    klass != null
            ) {
                return listOf(
                    Size(2576, 1932),
                    Size(2560, 1440),
                    Size(1920, 1920),
                    Size(2048, 1536),
                    Size(2048, 1152),
                    Size(1920, 1080),
                )
            }
        }
        return emptyList()
    }

    private fun getRedmiNote9ProExcludedSizes(cameraId: String, imageFormat: Int): List<Size> =
        if (cameraId == "0" && imageFormat == ImageFormat.JPEG) {
            listOf(Size(9280, 6944))
        } else {
            emptyList()
        }

    private fun getSamsungA05sExcludedSizes(imageFormat: Int) =
        if (imageFormat == ImageFormat.YUV_420_888) {
            listOf(
                Size(3840, 2160),
                Size(3264, 2448),
                Size(3200, 2400),
                Size(2688, 1512),
                Size(2592, 1944),
                Size(2592, 1940),
                Size(1920, 1440),
            )
        } else {
            emptyList()
        }

    private fun getNokia7PlusExcludedSizes(imageFormat: Int) =
        if (imageFormat == ImageFormat.YUV_420_888) {
            listOf(
                Size(4032, 3024),
                Size(4000, 3000),
                Size(3264, 2448),
                Size(3200, 2400),
                Size(3024, 3024),
                Size(2976, 2976),
                Size(2448, 2448),
            )
        } else {
            emptyList()
        }

    private fun getSamsungZFold4ExcludedSizes(cameraId: String, imageFormat: Int): List<Size> =
        if (cameraId == "1" && imageFormat == ImageFormat.YUV_420_888) {
            listOf(
                Size(1280, 720),
                Size(1920, 1080),
                Size(2304, 1296),
                Size(640, 360),
                Size(177, 144),
                Size(2336, 1080),
                Size(2400, 1080),
                Size(1920, 824),
                Size(1088, 1088),
                Size(1728, 1728),
                Size(2736, 2736),
                Size(1824, 712),
            )
        } else {
            emptyList()
        }

    public companion object {
        private const val TAG: String = "ExcludedSupportedSizesQuirk"
        private const val UNKNOWN_IMAGE_FORMAT: Int = -1

        public fun isEnabled(): Boolean =
            isOnePlus6 ||
                isOnePlus6T ||
                isHuaweiP20Lite ||
                isSamsungJ7PrimeApi27Above ||
                isSamsungJ7Api27Above ||
                isRedmiNote9Pro ||
                isSamsungA05s ||
                isNokia7Plus ||
                isSamsungZFold4

        internal val isOnePlus6: Boolean
            get() = isOnePlusDevice() && "OnePlus6".equals(Build.DEVICE, ignoreCase = true)

        internal val isOnePlus6T: Boolean
            get() = isOnePlusDevice() && "OnePlus6T".equals(Build.DEVICE, ignoreCase = true)

        internal val isHuaweiP20Lite: Boolean
            get() = isHuaweiDevice() && "HWANE".equals(Build.DEVICE, ignoreCase = true)

        internal val isSamsungJ7PrimeApi27Above: Boolean
            get() =
                isSamsungDevice() &&
                    "ON7XELTE".equals(Build.DEVICE, ignoreCase = true) &&
                    Build.VERSION.SDK_INT >= 27

        internal val isSamsungJ7Api27Above: Boolean
            get() =
                isSamsungDevice() &&
                    "J7XELTE".equals(Build.DEVICE, ignoreCase = true) &&
                    Build.VERSION.SDK_INT >= 27

        internal val isRedmiNote9Pro: Boolean
            get() = isRedmiDevice() && "joyeuse".equals(Build.DEVICE, ignoreCase = true)

        internal val isSamsungA05s: Boolean
            get() =
                // "a05s" device name is not only used for Samsung A05s series devices but is also
                // used for the other F14 series devices that use different chipset. Therefore,
                // additionally checks the model name to not apply the quirk onto the F14 devices.
                isSamsungDevice() &&
                    "a05s".equals(Build.DEVICE, ignoreCase = true) &&
                    Build.MODEL.uppercase().contains("SM-A057")

        internal val isNokia7Plus: Boolean
            get() =
                isNokiaDevice() &&
                    ("B2N".equals(Build.DEVICE, ignoreCase = true) ||
                        "B2N_sprout".equals(Build.DEVICE, ignoreCase = true))

        internal val isSamsungZFold4: Boolean
            get() =
                isSamsungDevice() &&
                    ("q4q".equals(Build.DEVICE, ignoreCase = true) ||
                        "SCG16".equals(Build.DEVICE, ignoreCase = true) ||
                        "SC-55C".equals(Build.DEVICE, ignoreCase = true))
    }
}
