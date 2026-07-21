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

package androidx.camera.common.testing

import android.annotation.SuppressLint
import android.graphics.ImageFormat as GraphicsImageFormat
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.camera.common.ImageFormat
import androidx.camera.common.ImageFormats

/**
 * Utility functions for creating fake [HardwareBuffer] instances and checking system support for
 * specific hardware buffer configurations.
 *
 * This class is designed to help write unit tests that simulate interactions with Android hardware
 * buffers, providing fallback logic for API levels where native queries like
 * [HardwareBuffer.isSupported] are unavailable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object FakeHardwareBuffers {

    /**
     * Estimates the size of a raw flat binary buffer for compressed and unstructured formats.
     *
     * This utility helps determine the buffer size required to represent compressed image formats,
     * such as JPEG, in a hardware buffer layout (where they are represented as a
     * [HardwareBuffer.BLOB] of height 1).
     *
     * Internally, this delegates to [ImageFormats.bytesPerImage] using
     * [android.graphics.ImageFormat.JPEG] as the format to perform the estimation.
     *
     * @param width The image width in pixels.
     * @param height The image height in pixels.
     * @return The estimated buffer size in bytes.
     */
    internal fun estimateBlobBufferSize(width: Int, height: Int): Int =
        ImageFormats.bytesPerImage(android.graphics.ImageFormat.JPEG, width, height).toInt()

    /**
     * Creates a fake [HardwareBuffer] configured for the specified image properties.
     *
     * The dimensions and format of the created [HardwareBuffer] depend on the input [imageFormat]:
     * - Compressed/unstructured formats (e.g., JPEG, HEIC) are represented as raw binary buffers,
     *   which maps the buffer format to [HardwareBuffer.BLOB], width to an estimated size (via
     *   [estimateBlobBufferSize]), and height to `1`.
     * - Standard structured formats (e.g., YUV_420_888, RGBA_8888) maintain their original
     *   dimensions.
     *
     * @param imageFormat The high-level [ImageFormat] representing the image format.
     * @param imageWidth The width of the image in pixels.
     * @param imageHeight The height of the image in pixels.
     * @param hardwareBufferLayers The number of layers in the hardware buffer, defaults to 1.
     * @param hardwareBufferUsage The usage flags for the hardware buffer, defaults to
     *   [HardwareBuffer.USAGE_CPU_READ_OFTEN].
     * @return A newly created [HardwareBuffer], or `null` if the current API level is below 26, or
     *   if creating a buffer with the resolved configuration is not supported on this device.
     */
    fun createForImage(
        @ImageFormat imageFormat: Int,
        imageWidth: Int,
        imageHeight: Int,
        hardwareBufferLayers: Int = 1,
        hardwareBufferUsage: Long = HardwareBuffer.USAGE_CPU_READ_OFTEN,
    ): HardwareBuffer? {
        if (Build.VERSION.SDK_INT < 26) return null

        val hardwareBufferFormat = toHardwareBufferFormat(imageFormat)
        val hardwareBufferWidth =
            if (hardwareBufferFormat == HardwareBuffer.BLOB) {
                estimateBlobBufferSize(imageWidth, imageHeight)
            } else {
                imageWidth
            }
        val hardwareBufferHeight =
            if (hardwareBufferFormat == HardwareBuffer.BLOB) 1 else imageHeight

        if (
            !isHardwareBufferSupported(
                hardwareBufferWidth = hardwareBufferWidth,
                hardwareBufferHeight = hardwareBufferHeight,
                hardwareBufferFormat = hardwareBufferFormat,
                hardwareBufferLayers = hardwareBufferLayers,
                hardwareBufferUsage = hardwareBufferUsage,
            )
        ) {
            return null
        }

        return try {
            HardwareBuffer.create(
                hardwareBufferWidth,
                hardwareBufferHeight,
                hardwareBufferFormat,
                hardwareBufferLayers,
                hardwareBufferUsage,
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Checks if the system supports creating a [HardwareBuffer] with the specified properties.
     *
     * On Android 10 (API level 29) and above, this queries the system directly using
     * [HardwareBuffer.isSupported]. On API levels 26-28, it falls back to a static verification
     * check using a predefined list of supported formats.
     *
     * @param hardwareBufferWidth The width of the hardware buffer in pixels.
     * @param hardwareBufferHeight The height of the hardware buffer in pixels.
     * @param hardwareBufferFormat The format of the hardware buffer (e.g.,
     *   [HardwareBuffer.RGBA_8888]).
     * @param hardwareBufferLayers The number of layers in the hardware buffer.
     * @param hardwareBufferUsage The usage flags for the hardware buffer (e.g.,
     *   [HardwareBuffer.USAGE_CPU_READ_OFTEN]).
     * @return `true` if a [HardwareBuffer] with these parameters can be successfully created on the
     *   current device; `false` otherwise.
     */
    @SuppressLint("WrongConstant")
    fun isHardwareBufferSupported(
        hardwareBufferWidth: Int,
        hardwareBufferHeight: Int,
        hardwareBufferFormat: Int,
        hardwareBufferLayers: Int,
        hardwareBufferUsage: Long,
    ): Boolean {
        if (Build.VERSION.SDK_INT < 26) return false

        if (Build.VERSION.SDK_INT >= 29) {
            return try {
                HardwareBuffer.isSupported(
                    hardwareBufferWidth,
                    hardwareBufferHeight,
                    hardwareBufferFormat,
                    hardwareBufferLayers,
                    hardwareBufferUsage,
                )
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        // Fallback for API 26-28
        if (hardwareBufferFormat == HardwareBuffer.BLOB && hardwareBufferHeight != 1) return false
        return SUPPORTED_HW_FORMATS.contains(hardwareBufferFormat)
    }

    @get:SuppressLint("InlinedApi")
    private val SUPPORTED_HW_FORMATS: Set<Int> by lazy {
        val formats = mutableSetOf<Int>()
        val apiLevel = Build.VERSION.SDK_INT
        if (apiLevel >= 26) {
            formats.addAll(
                listOf(
                    HardwareBuffer.RGBA_8888,
                    HardwareBuffer.RGBA_FP16,
                    HardwareBuffer.RGBX_8888,
                    HardwareBuffer.RGB_565,
                    HardwareBuffer.RGB_888,
                    HardwareBuffer.BLOB,
                )
            )
        }
        if (apiLevel >= 27) {
            formats.add(HardwareBuffer.YCBCR_420_888)
        }
        if (apiLevel >= 28) {
            formats.addAll(
                listOf(
                    HardwareBuffer.RGBA_1010102,
                    HardwareBuffer.D_16,
                    HardwareBuffer.D_24,
                    HardwareBuffer.DS_24UI8,
                    HardwareBuffer.D_FP32,
                    HardwareBuffer.DS_FP32UI8,
                    HardwareBuffer.S_UI8,
                )
            )
        }
        formats
    }

    /**
     * Maps a high-level [ImageFormat] to its corresponding [HardwareBuffer] format.
     *
     * Standard 2D image formats (e.g., [android.graphics.ImageFormat.YUV_420_888]) are typically
     * mapped directly to their equivalent [HardwareBuffer] format.
     *
     * Compressed or unstructured image formats (like [android.graphics.ImageFormat.JPEG],
     * [android.graphics.ImageFormat.HEIC], or [android.graphics.ImageFormat.DEPTH_POINT_CLOUD]) do
     * not have a standard 2D pixel grid layout. These are mapped to [HardwareBuffer.BLOB] to
     * represent them as flat, raw binary buffers.
     *
     * @param imageFormat The high-level [ImageFormat] to map.
     * @return The corresponding [HardwareBuffer] format.
     */
    fun toHardwareBufferFormat(imageFormat: Int): Int =
        when (imageFormat) {
            GraphicsImageFormat.JPEG,
            GraphicsImageFormat.HEIC,
            GraphicsImageFormat.DEPTH_JPEG,
            GraphicsImageFormat.JPEG_R,
            GraphicsImageFormat.DEPTH_POINT_CLOUD -> HardwareBuffer.BLOB
            else -> imageFormat
        }
}
