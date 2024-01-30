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

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.annotation.IntRange
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Image util functions
 */
object ImageUtil {

    /**
     * Converts JPEG [Image] to [ByteArray]
     */
    @JvmStatic
    fun jpegImageToJpegByteArray(image: Image): ByteArray {
        require(image.format == ImageFormat.JPEG) {
            "Incorrect image format of the input image proxy: ${image.format}"
        }
        val planes = image.planes
        val buffer = planes[0].buffer
        val data = ByteArray(buffer.capacity())
        buffer.rewind()
        buffer[data]
        return data
    }

    /**
     * Converts YUV_420_888 [ImageProxy] to JPEG byte array. The input YUV_420_888 image
     * will be cropped if a non-null crop rectangle is specified. The output JPEG byte array will
     * be compressed by the specified quality value.
     */
    @JvmStatic
    fun yuvImageToJpegByteArray(
        image: Image,
        @IntRange(from = 1, to = 100) jpegQuality: Int
    ): ByteArray {
        require(image.format == ImageFormat.YUV_420_888) {
            "Incorrect image format of the input image proxy: ${image.format}"
        }
        return nv21ToJpeg(
            yuv_420_888toNv21(image),
            image.width,
            image.height,
            jpegQuality
        )
    }

    /**
     * Converts nv21 byte array to JPEG format.
     */
    @JvmStatic
    private fun nv21ToJpeg(
        nv21: ByteArray,
        width: Int,
        height: Int,
        @IntRange(from = 1, to = 100) jpegQuality: Int
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val success = yuv.compressToJpeg(Rect(0, 0, width, height), jpegQuality, out)

        if (!success) {
            throw RuntimeException("YuvImage failed to encode jpeg.")
        }
        return out.toByteArray()
    }

    /**
     * Converts a YUV [Image] to NV21 byte array.
     */
    @JvmStatic
    private fun yuv_420_888toNv21(image: Image): ByteArray {
        require(image.format == ImageFormat.YUV_420_888) {
            "Incorrect image format of the input image proxy: ${image.format}"
        }

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()
        val ySize = yBuffer.remaining()
        var position = 0
        // TODO(b/115743986): Pull these bytes from a pool instead of allocating for every image.
        val nv21 = ByteArray(ySize + image.width * image.height / 2)

        // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
        for (row in 0 until image.height) {
            yBuffer[nv21, position, image.width]
            position += image.width
            yBuffer.position(
                Math.min(ySize, yBuffer.position() - image.width + yPlane.rowStride)
            )
        }
        val chromaHeight = image.height / 2
        val chromaWidth = image.width / 2
        val vRowStride = vPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vPixelStride = vPlane.pixelStride
        val uPixelStride = uPlane.pixelStride

        // Interleave the u and v frames, filling up the rest of the buffer. Use two line buffers to
        // perform faster bulk gets from the byte buffers.
        val vLineBuffer = ByteArray(vRowStride)
        val uLineBuffer = ByteArray(uRowStride)
        for (row in 0 until chromaHeight) {
            vBuffer[vLineBuffer, 0, Math.min(vRowStride, vBuffer.remaining())]
            uBuffer[uLineBuffer, 0, Math.min(uRowStride, uBuffer.remaining())]
            var vLineBufferPosition = 0
            var uLineBufferPosition = 0
            for (col in 0 until chromaWidth) {
                nv21[position++] = vLineBuffer[vLineBufferPosition]
                nv21[position++] = uLineBuffer[uLineBufferPosition]
                vLineBufferPosition += vPixelStride
                uLineBufferPosition += uPixelStride
            }
        }
        return nv21
    }
}
