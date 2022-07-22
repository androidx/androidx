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
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraExtensionSession.ExtensionCaptureCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.stream.Collectors

private const val TAG = "Camera2ExtensionsUtil"

/**
 * Util functions for Camera2 Extensions implementation
 */
object Camera2ExtensionsUtil {

    /**
     * Converts extension mode from integer to string.
     */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @JvmStatic
    fun getExtensionModeStringFromId(extension: Int): String {
        return when (extension) {
            CameraExtensionCharacteristics.EXTENSION_HDR -> "HDR"
            CameraExtensionCharacteristics.EXTENSION_NIGHT -> "NIGHT"
            CameraExtensionCharacteristics.EXTENSION_BOKEH -> "BOKEH"
            CameraExtensionCharacteristics.EXTENSION_BEAUTY -> "FACE RETOUCH"
            else -> "AUTO"
        }
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

    /**
     * Creates a default extension capture callback implementation.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun createExtensionCaptureCallback(): ExtensionCaptureCallback {
        return object : ExtensionCaptureCallback() {
            override fun onCaptureStarted(
                session: CameraExtensionSession,
                request: CaptureRequest,
                timestamp: Long
            ) {
            }

            override fun onCaptureProcessStarted(
                session: CameraExtensionSession,
                request: CaptureRequest
            ) {
            }

            override fun onCaptureFailed(
                session: CameraExtensionSession,
                request: CaptureRequest
            ) {
                Log.v(TAG, "onCaptureProcessFailed")
            }

            override fun onCaptureSequenceCompleted(
                session: CameraExtensionSession,
                sequenceId: Int
            ) {
                Log.v(TAG, "onCaptureProcessSequenceCompleted: $sequenceId")
            }

            override fun onCaptureSequenceAborted(
                session: CameraExtensionSession,
                sequenceId: Int
            ) {
                Log.v(TAG, "onCaptureProcessSequenceAborted: $sequenceId")
            }
        }
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

    /**
     * Transforms the texture view to display the content of resolution in correct direction and
     * aspect ratio.
     */
    @JvmStatic
    fun transformPreview(textureView: TextureView, resolution: Size, displayRotation: Int) {
        if (resolution.width == 0 || resolution.height == 0) {
            return
        }
        if (textureView.width == 0 || textureView.height == 0) {
            return
        }
        val matrix = Matrix()
        val left: Int = textureView.left
        val right: Int = textureView.right
        val top: Int = textureView.top
        val bottom: Int = textureView.bottom

        // Compute the preview ui size based on the available width, height, and ui orientation.
        val viewWidth = right - left
        val viewHeight = bottom - top
        val displayRotationDegrees: Int = getDisplayRotationDegrees(displayRotation)
        val scaled: Size = calculatePreviewViewDimens(
            resolution, viewWidth, viewHeight, displayRotation
        )

        // Compute the center of the view.
        val centerX = (viewWidth / 2).toFloat()
        val centerY = (viewHeight / 2).toFloat()

        // Do corresponding rotation to correct the preview direction
        matrix.postRotate((-displayRotationDegrees).toFloat(), centerX, centerY)

        // Compute the scale value for center crop mode
        var xScale = scaled.width / viewWidth.toFloat()
        var yScale = scaled.height / viewHeight.toFloat()
        if (displayRotationDegrees % 180 == 90) {
            xScale = scaled.width / viewHeight.toFloat()
            yScale = scaled.height / viewWidth.toFloat()
        }

        // Only two digits after the decimal point are valid for postScale. Need to get ceiling of
        // two digits floating value to do the scale operation. Otherwise, the result may be scaled
        // not large enough and will have some blank lines on the screen.
        xScale = BigDecimal(xScale.toDouble()).setScale(2, RoundingMode.CEILING).toFloat()
        yScale = BigDecimal(yScale.toDouble()).setScale(2, RoundingMode.CEILING).toFloat()

        // Do corresponding scale to resolve the deformation problem
        matrix.postScale(xScale, yScale, centerX, centerY)
        textureView.setTransform(matrix)
    }

    /**
     * Converts the display rotation to degrees value.
     *
     * @return One of 0, 90, 180, 270.
     */
    @JvmStatic
    fun getDisplayRotationDegrees(displayRotation: Int): Int = when (displayRotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> throw UnsupportedOperationException(
            "Unsupported display rotation: $displayRotation"
        )
    }

    /**
     * Calculates the delta between a source rotation and destination rotation.
     *
     * <p>A typical use of this method would be calculating the angular difference between the
     * display orientation (destRotationDegrees) and camera sensor orientation
     * (sourceRotationDegrees).
     *
     * @param destRotationDegrees   The destination rotation relative to the device's natural
     *                              rotation.
     * @param sourceRotationDegrees The source rotation relative to the device's natural rotation.
     * @param isOppositeFacing      Whether the source and destination planes are facing opposite
     *                              directions.
     */
    @JvmStatic
    fun calculateRelativeImageRotationDegrees(
        destRotationDegrees: Int,
        sourceRotationDegrees: Int,
        isOppositeFacing: Boolean
    ): Int {
        val result: Int = if (isOppositeFacing) {
            (sourceRotationDegrees - destRotationDegrees + 360) % 360
        } else {
            (sourceRotationDegrees + destRotationDegrees) % 360
        }

        return result
    }

    /**
     * Calculates the preview size which can display the source image in correct aspect ratio.
     */
    @JvmStatic
    private fun calculatePreviewViewDimens(
        srcSize: Size,
        parentWidth: Int,
        parentHeight: Int,
        displayRotation: Int
    ): Size {
        var inWidth = srcSize.width
        var inHeight = srcSize.height
        if (displayRotation == 0 || displayRotation == 180) {
            // Need to reverse the width and height since we're in landscape orientation.
            inWidth = srcSize.height
            inHeight = srcSize.width
        }
        var outWidth = parentWidth
        var outHeight = parentHeight
        if (inWidth != 0 && inHeight != 0) {
            val vfRatio = inWidth / inHeight.toFloat()
            val parentRatio = parentWidth / parentHeight.toFloat()

            // Match shortest sides together.
            if (vfRatio < parentRatio) {
                outWidth = parentWidth
                outHeight = Math.round(parentWidth / vfRatio)
            } else {
                outWidth = Math.round(parentHeight * vfRatio)
                outHeight = parentHeight
            }
        }
        return Size(outWidth, outHeight)
    }
}