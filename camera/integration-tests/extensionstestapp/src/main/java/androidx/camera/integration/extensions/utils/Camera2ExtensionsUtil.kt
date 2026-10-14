/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.extensions.utils

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.integration.extensions.EXTENSION_MODE_NONE
import java.util.stream.Collectors

private const val TAG = "Camera2ExtensionsUtil"

/** Util functions for Camera2 Extensions implementation */
object Camera2ExtensionsUtil {

    private val lock = Any()

    /**
     * Caches the supported extension modes per camera id.
     *
     * Every call to [CameraExtensionCharacteristics.getSupportedExtensions] binds to and unbinds
     * from the extensions service. Querying it right after a previous extension session is closed
     * may race with the asynchronous service unbinding and throw "Service not registered" on some
     * devices (b/570911697), so the result is queried only once per camera id.
     */
    @GuardedBy("lock") private val cachedSupportedExtensions = mutableMapOf<String, List<Int>>()

    /** Camera2 extension modes */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    @JvmStatic
    val AVAILABLE_CAMERA2_EXTENSION_MODES =
        arrayOf(
            CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
            CameraExtensionCharacteristics.EXTENSION_BEAUTY,
            CameraExtensionCharacteristics.EXTENSION_BOKEH,
            CameraExtensionCharacteristics.EXTENSION_HDR,
            CameraExtensionCharacteristics.EXTENSION_NIGHT,
        )

    /** Converts extension mode from integer to string. */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    @JvmStatic
    fun getCamera2ExtensionModeStringFromId(extension: Int): String =
        when (extension) {
            EXTENSION_MODE_NONE -> "None"
            CameraExtensionCharacteristics.EXTENSION_HDR -> "HDR"
            CameraExtensionCharacteristics.EXTENSION_NIGHT -> "NIGHT"
            CameraExtensionCharacteristics.EXTENSION_BOKEH -> "BOKEH"
            CameraExtensionCharacteristics.EXTENSION_BEAUTY -> "FACE RETOUCH"
            CameraExtensionCharacteristics.EXTENSION_AUTOMATIC -> "AUTO"
            else -> throw IllegalArgumentException("Invalid extension mode id!")
        }

    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    @JvmStatic
    fun getCamera2ExtensionModeIdFromString(mode: String): Int =
        when (mode) {
            "None" -> EXTENSION_MODE_NONE
            "HDR" -> CameraExtensionCharacteristics.EXTENSION_HDR
            "NIGHT" -> CameraExtensionCharacteristics.EXTENSION_NIGHT
            "BOKEH" -> CameraExtensionCharacteristics.EXTENSION_BOKEH
            "FACE RETOUCH" -> CameraExtensionCharacteristics.EXTENSION_BEAUTY
            "AUTO" -> CameraExtensionCharacteristics.EXTENSION_AUTOMATIC
            else -> throw IllegalArgumentException("Invalid extension mode string!")
        }

    /** Gets the first camera id of the specified lens facing. */
    @JvmStatic
    fun getLensFacingCameraId(cameraManager: CameraManager, lensFacing: Int): String {
        cameraManager.cameraIdList.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (characteristics[CameraCharacteristics.LENS_FACING] == lensFacing) {
                characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]?.let {
                    if (
                        it.contains(
                            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                        )
                    ) {
                        return cameraId
                    }
                }
            }
        }

        throw IllegalArgumentException("Can't find camera of lens facing $lensFacing")
    }

    /**
     * Returns the supported Camera2 extension modes of the camera, excluding the modes that are
     * known to be broken on the device. The result is cached per camera id.
     */
    @RequiresApi(31)
    @JvmStatic
    fun getSupportedExtensions(cameraManager: CameraManager, cameraId: String): List<Int> =
        synchronized(lock) {
            cachedSupportedExtensions.getOrPut(cameraId) {
                cameraManager
                    .getCameraExtensionCharacteristics(cameraId)
                    .supportedExtensions
                    .filterNot { isExtensionDisabledByQuirk(cameraId, it) }
            }
        }

    /** Returns true if the device is a Samsung Galaxy A52s (SM-A528B). */
    @JvmStatic
    fun isSamsungGalaxyA52s(): Boolean =
        "Samsung".equals(Build.BRAND, ignoreCase = true) &&
            "SM-A528B".equals(Build.MODEL, ignoreCase = true)

    /**
     * Returns true if the extension mode is known to be broken on the device.
     *
     * This mirrors the Camera2 extension part of CameraX's `ExtensionDisabledQuirk` (which is
     * internal to camera-extensions and can't be reused here) and must be kept in sync with it.
     */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    private fun isExtensionDisabledByQuirk(cameraId: String, extensionMode: Int): Boolean {
        // Extension modes Bokeh and Face Retouch fail on Camera 0 for Samsung Galaxy A52s
        // (SM-A528B) due to HAL requesting 0 max buffers (b/532825627, b/570911697).
        if (isSamsungGalaxyA52s() && cameraId == "0") {
            return extensionMode == CameraExtensionCharacteristics.EXTENSION_BOKEH ||
                extensionMode == CameraExtensionCharacteristics.EXTENSION_BEAUTY
        }
        return false
    }

    @RequiresApi(31)
    @JvmStatic
    fun isCamera2ExtensionModeSupported(
        context: Context,
        cameraId: String,
        extensionMode: Int,
    ): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return getSupportedExtensions(cameraManager, cameraId).contains(extensionMode)
    }

    /**
     * Picks a preview resolution that is both close/same as the display size and supported by
     * camera and extensions.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun pickPreviewResolution(
        cameraManager: CameraManager,
        cameraId: String,
        displayMetrics: DisplayMetrics,
        extensionMode: Int,
    ): Size? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val textureSizes = map!!.getOutputSizes(SurfaceTexture::class.java)
        val displaySize = Point()
        displaySize.x = displayMetrics.widthPixels
        displaySize.y = displayMetrics.heightPixels
        if (displaySize.x < displaySize.y) {
            displaySize.x = displayMetrics.heightPixels
            displaySize.y = displayMetrics.widthPixels
        }
        val displayArRatio = displaySize.x.toFloat() / displaySize.y
        val previewSizes = ArrayList<Size>()
        var previewSize: Size? = null
        var currentDistance = Int.MAX_VALUE
        for (sz in textureSizes) {
            val arRatio = sz.width.toFloat() / sz.height
            if (Math.abs(arRatio - displayArRatio) <= .2f) {
                previewSizes.add(sz)
            }
            val distance = Math.abs(sz.width * sz.height - displaySize.x * displaySize.y)
            if (currentDistance > distance) {
                currentDistance = distance
                previewSize = sz
            }
        }

        if (previewSizes.isEmpty()) {
            previewSize?.let { previewSizes.add(it) }
                ?: throw IllegalStateException("No preview size was found")
        } else {
            previewSize = previewSizes[0]
        }

        if (extensionMode == EXTENSION_MODE_NONE) {
            for (sz in previewSizes) {
                val distance = Math.abs(sz.width * sz.height - displaySize.x * displaySize.y)
                if (currentDistance > distance) {
                    currentDistance = distance
                    previewSize = sz
                }
            }
            return previewSize
        }

        val extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        val extensionSizes =
            extensionCharacteristics.getExtensionSupportedSizes(
                extensionMode,
                SurfaceTexture::class.java,
            )
        if (extensionSizes.isEmpty()) {
            return null
        }

        previewSize = extensionSizes[0]
        val supportedPreviewSizes =
            previewSizes
                .stream()
                .distinct()
                .filter { o: Size -> extensionSizes.contains(o) }
                .collect(Collectors.toList())
        if (supportedPreviewSizes.isNotEmpty()) {
            currentDistance = Int.MAX_VALUE
            for (sz in supportedPreviewSizes) {
                val distance = Math.abs(sz.width * sz.height - displaySize.x * displaySize.y)
                if (currentDistance > distance) {
                    currentDistance = distance
                    previewSize = sz
                }
            }
        } else {
            Log.w(
                TAG,
                "No overlap between supported camera and extensions preview sizes using" +
                    " first available!",
            )
        }

        return previewSize
    }

    /** Picks a resolution for still image capture. */
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun pickStillImageResolution(
        cameraCharacteristics: CameraCharacteristics,
        extensionCharacteristics: CameraExtensionCharacteristics,
        extensionMode: Int,
    ): Pair<Size, Int> {
        val yuvColorEncodingSystemSizes =
            if (extensionMode == EXTENSION_MODE_NONE) {
                cameraCharacteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.YUV_420_888)
                    .toList()
            } else {
                extensionCharacteristics.getExtensionSupportedSizes(
                    extensionMode,
                    ImageFormat.YUV_420_888,
                )
            }
        val jpegSizes =
            if (extensionMode == EXTENSION_MODE_NONE) {
                cameraCharacteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG)
                    .toList()
            } else {
                extensionCharacteristics.getExtensionSupportedSizes(extensionMode, ImageFormat.JPEG)
            }
        val stillFormat = if (jpegSizes.isEmpty()) ImageFormat.YUV_420_888 else ImageFormat.JPEG
        val stillCaptureSize =
            if (jpegSizes.isEmpty()) yuvColorEncodingSystemSizes[0] else jpegSizes[0]

        return Pair(stillCaptureSize, stillFormat)
    }
}
