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

/** Utility functions for creating and verifying fake [HardwareBuffer] objects. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object FakeHardwareBuffers {

    /** Estimates the size of a raw flat binary buffer for compressed and unstructured formats. */
    internal fun estimateBlobBufferSize(width: Int, height: Int): Int =
        ImageFormats.bytesPerImage(android.graphics.ImageFormat.JPEG, width, height).toInt()

    /**
     * Creates a fake [HardwareBuffer] for the specified image properties.
     *
     * Returns `null` if the API level is less than 26 or if the format/dimensions are not
     * supported.
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
     * Checks if [HardwareBuffer] supports creating a buffer with the specified properties.
     *
     * On API level 29+, this queries the system directly using [HardwareBuffer.isSupported]. On API
     * levels 26-28, it falls back to a static verification check.
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
     * High-level image formats (e.g. from [android.graphics.ImageFormat]) are typically mapped
     * 1-to-1 to their [HardwareBuffer] counterparts (e.g. [HardwareBuffer.RGBA_8888] or
     * [HardwareBuffer.YCBCR_420_888]).
     *
     * Compressed or unstructured image formats (like [android.graphics.ImageFormat.JPEG],
     * [android.graphics.ImageFormat.DEPTH_POINT_CLOUD], etc.) do not have a standard 2D pixel grid
     * representation. These are represented in the hardware layer as flat raw binary buffers, which
     * map to [HardwareBuffer.BLOB].
     *
     * As new image formats are introduced to Android or supported by CameraX, additional mappings
     * may need to be added to this function.
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
