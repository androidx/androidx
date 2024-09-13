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

package androidx.camera.core.imagecapture

import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import java.io.File
import java.util.UUID
import org.mockito.Mockito.mock

object Utils {
    const val WIDTH = 640
    const val HEIGHT = 480
    internal val EXIF_DESCRIPTION = "description"
    const val ROTATION_DEGREES = 90
    const val FOCAL_LENGTH = 10F
    internal const val TIMESTAMP = 9999L
    val SENSOR_TO_BUFFER = Matrix().also { it.setScale(-1F, 1F, 320F, 240F) }
    val SIZE = Size(WIDTH, HEIGHT)
    val CROP_RECT = Rect(0, 240, WIDTH, HEIGHT)
    internal val TEMP_FILE =
        File.createTempFile("unit_test_" + UUID.randomUUID().toString(), ".temp").also {
            it.deleteOnExit()
        }
    internal val OUTPUT_FILE_OPTIONS = ImageCapture.OutputFileOptions.Builder(TEMP_FILE).build()
    internal val CAMERA_CAPTURE_RESULT = FakeCameraCaptureResult().also { it.timestamp = TIMESTAMP }
    val EXIF_GAINMAP_PATTERNS =
        listOf(
            "xmlns:hdrgm=\"http://ns.adobe.com/hdr-gain-map/",
            "hdrgm:Version=",
            "Item:Semantic=\"GainMap\"",
        )

    fun createTakePictureRequest(
        outputFileOptions: List<ImageCapture.OutputFileOptions>?,
        cropRect: Rect,
        sensorToBufferTransform: Matrix,
        rotationDegrees: Int,
        jpegQuality: Int
    ): TakePictureRequest {
        var onDiskCallback: ImageCapture.OnImageSavedCallback? = null
        var onMemoryCallback: ImageCapture.OnImageCapturedCallback? = null
        if (outputFileOptions == null) {
            onMemoryCallback = mock(ImageCapture.OnImageCapturedCallback::class.java)
        } else {
            onDiskCallback = mock(ImageCapture.OnImageSavedCallback::class.java)
        }

        return TakePictureRequest.of(
            CameraXExecutors.mainThreadExecutor(),
            onMemoryCallback,
            onDiskCallback,
            outputFileOptions,
            cropRect,
            sensorToBufferTransform,
            rotationDegrees,
            jpegQuality,
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            listOf()
        )
    }
}
