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

import android.annotation.SuppressLint
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
import androidx.annotation.RequiresApi
import java.util.stream.Collectors

private const val TAG = "Camera2ExtensionsUtil"

/**
 * Util functions for Camera2 Extensions implementation
 */
object Camera2ExtensionsUtil {

    /**
     * Camera2 extension modes
     */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    @JvmStatic
    val AVAILABLE_CAMERA2_EXTENSION_MODES = arrayOf(
        CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
        CameraExtensionCharacteristics.EXTENSION_BEAUTY,
        CameraExtensionCharacteristics.EXTENSION_BOKEH,
        CameraExtensionCharacteristics.EXTENSION_HDR,
        CameraExtensionCharacteristics.EXTENSION_NIGHT,
    )

    /**
     * Converts extension mode from integer to string.
     */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @RequiresApi(31)
    @JvmStatic
    fun getCamera2ExtensionModeStringFromId(extension: Int): String = when (extension) {
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
    fun getCamera2ExtensionModeIdFromString(mode: String): Int = when (mode) {
        "HDR" -> CameraExtensionCharacteristics.EXTENSION_HDR
        "NIGHT" -> CameraExtensionCharacteristics.EXTENSION_NIGHT
        "BOKEH" -> CameraExtensionCharacteristics.EXTENSION_BOKEH
        "FACE RETOUCH" -> CameraExtensionCharacteristics.EXTENSION_BEAUTY
        "AUTO" -> CameraExtensionCharacteristics.EXTENSION_AUTOMATIC
        else -> throw IllegalArgumentException("Invalid extension mode string!")
    }

    /**
     * Gets the first camera id of the specified lens facing.
     */
    @JvmStatic
    fun getLensFacingCameraId(cameraManager: CameraManager, lensFacing: Int): String {
        cameraManager.cameraIdList.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (characteristics[CameraCharacteristics.LENS_FACING] == lensFacing) {
                characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]?.let {
                    if (it.contains(
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

    @SuppressLint("ClassVerificationFailure")
    @RequiresApi(31)
    @JvmStatic
    fun isCamera2ExtensionModeSupported(
        context: Context,
        cameraId: String,
        extensionMode: Int
    ): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        return extensionCharacteristics.supportedExtensions.contains(extensionMode)
    }

    /**
     * Picks a preview resolution that is both close/same as the display size and supported by camera
     * and extensions.
     */
    @SuppressLint("ClassVerificationFailure")
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun pickPreviewResolution(
        cameraManager: CameraManager,
        cameraId: String,
        displayMetrics: DisplayMetrics,
        extensionMode: Int
    ): Size? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )
        val textureSizes = map!!.getOutputSizes(
            SurfaceTexture::class.java
        )
        val displaySize = Point()
        displaySize.x = displayMetrics.widthPixels
        displaySize.y = displayMetrics.heightPixels
        if (displaySize.x < displaySize.y) {
            displaySize.x = displayMetrics.heightPixels
            displaySize.y = displayMetrics.widthPixels
        }
        val displayArRatio = displaySize.x.toFloat() / displaySize.y
        val previewSizes = ArrayList<Size>()
        for (sz in textureSizes) {
            val arRatio = sz.width.toFloat() / sz.height
            if (Math.abs(arRatio - displayArRatio) <= .2f) {
                previewSizes.add(sz)
            }
        }
        val extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        val extensionSizes = extensionCharacteristics.getExtensionSupportedSizes(
            extensionMode, SurfaceTexture::class.java
        )
        if (extensionSizes.isEmpty()) {
            return null
        }

        var previewSize = extensionSizes[0]
        val supportedPreviewSizes =
            previewSizes.stream().distinct().filter { o: Size -> extensionSizes.contains(o) }
                .collect(Collectors.toList())
        if (supportedPreviewSizes.isNotEmpty()) {
            var currentDistance = Int.MAX_VALUE
            for (sz in supportedPreviewSizes) {
                val distance = Math.abs(sz.width * sz.height - displaySize.x * displaySize.y)
                if (currentDistance > distance) {
                    currentDistance = distance
                    previewSize = sz
                }
            }
        } else {
            Log.w(
                TAG, "No overlap between supported camera and extensions preview sizes using" +
                    " first available!"
            )
        }

        return previewSize
    }

    /**
     * Picks a resolution for still image capture.
     */
    @SuppressLint("ClassVerificationFailure")
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun pickStillImageResolution(
        extensionCharacteristics: CameraExtensionCharacteristics,
        extensionMode: Int
    ): Pair<Size, Int> {
        val yuvColorEncodingSystemSizes = extensionCharacteristics.getExtensionSupportedSizes(
            extensionMode, ImageFormat.YUV_420_888
        )
        val jpegSizes = extensionCharacteristics.getExtensionSupportedSizes(
            extensionMode, ImageFormat.JPEG
        )
        val stillFormat = if (jpegSizes.isEmpty()) ImageFormat.YUV_420_888 else ImageFormat.JPEG
        val stillCaptureSize =
            if (jpegSizes.isEmpty()) yuvColorEncodingSystemSizes[0] else jpegSizes[0]

        return Pair(stillCaptureSize, stillFormat)
    }
}
