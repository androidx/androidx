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

package androidx.camera.integration.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.CameraEffect
import androidx.camera.core.ImageProcessor
import androidx.camera.core.ImageProcessor.Response
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.RgbaImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor

/**
 * A image effect that applies the same tone mapping as [ToneMappingSurfaceProcessor].
 */
class ToneMappingImageEffect : CameraEffect(
    IMAGE_CAPTURE, mainThreadExecutor(), ToneMappingImageProcessor()
) {

    fun isInvoked(): Boolean {
        return (imageProcessor as ToneMappingImageProcessor).processoed
    }

    private class ToneMappingImageProcessor : ImageProcessor {

        var processoed = false

        override fun process(request: ImageProcessor.Request): Response {
            processoed = true
            val inputImage = request.inputImage as RgbaImageProxy
            val bitmap = inputImage.createBitmap()
            applyToneMapping(bitmap)
            val outputImage = createOutputImage(bitmap, inputImage)
            inputImage.close()
            return Response { outputImage }
        }

        /**
         * Creates output image
         */
        private fun createOutputImage(newBitmap: Bitmap, imageIn: ImageProxy): ImageProxy {
            return RgbaImageProxy(
                newBitmap,
                imageIn.cropRect,
                imageIn.imageInfo.rotationDegrees,
                imageIn.imageInfo.sensorToBufferTransformMatrix,
                imageIn.imageInfo.timestamp
            )
        }

        /**
         * Applies the same color matrix as [ToneMappingSurfaceProcessor].
         */
        private fun applyToneMapping(bitmap: Bitmap) {
            val paint = Paint()
            paint.colorFilter = ColorMatrixColorFilter(
                floatArrayOf(
                    0.5F, 0.8F, 0.3F, 0F, 0F,
                    0.4F, 0.7F, 0.2F, 0F, 0F,
                    0.3F, 0.5F, 0.1F, 0F, 0F,
                    0F, 0F, 0F, 1F, 0F,
                )
            )
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(bitmap, 0F, 0F, paint)
        }
    }
}
