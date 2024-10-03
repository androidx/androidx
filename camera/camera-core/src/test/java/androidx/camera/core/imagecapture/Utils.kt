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

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.util.Pair
import android.util.Size
import androidx.camera.core.CaptureBundles
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.CaptureBundle
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.impl.fakes.FakeCaptureStage
import androidx.camera.testing.impl.fakes.FakeImageProxy
import java.io.File
import java.util.UUID
import org.mockito.Mockito.mock
import org.robolectric.util.ReflectionHelpers.setStaticField

/** Utility methods for testing image capture. */
object Utils {

    internal const val WIDTH = 640
    internal const val HEIGHT = 480
    internal const val EXIF_DESCRIPTION = "description"
    internal const val ROTATION_DEGREES = 180
    internal const val ALTITUDE = 0.1
    internal const val JPEG_QUALITY = 90
    internal const val TIMESTAMP = 9999L
    internal val SENSOR_TO_BUFFER = Matrix().also { it.setScale(-1F, 1F, 320F, 240F) }
    internal val SIZE = Size(WIDTH, HEIGHT)
    // The crop rect is the lower half of the image.
    internal val CROP_RECT = Rect(0, 240, WIDTH, HEIGHT)
    internal val FULL_RECT = Rect(0, 0, WIDTH, HEIGHT)
    internal val TEMP_FILE =
        File.createTempFile("unit_test_" + UUID.randomUUID().toString(), ".temp").also {
            it.deleteOnExit()
        }
    internal val OUTPUT_FILE_OPTIONS = ImageCapture.OutputFileOptions.Builder(TEMP_FILE).build()
    internal val SECONDARY_OUTPUT_FILE_OPTIONS =
        ImageCapture.OutputFileOptions.Builder(TEMP_FILE).build()
    internal val CAMERA_CAPTURE_RESULT = FakeCameraCaptureResult().also { it.timestamp = TIMESTAMP }

    internal fun createProcessingRequest(
        takePictureCallback: TakePictureCallback = FakeTakePictureCallback(),
        captureBundle: CaptureBundle = CaptureBundles.singleDefaultCaptureBundle()
    ): ProcessingRequest {
        return ProcessingRequest(
            captureBundle,
            createTakePictureRequest(
                OUTPUT_FILE_OPTIONS,
                null,
                CROP_RECT,
                SENSOR_TO_BUFFER,
                ROTATION_DEGREES,
                JPEG_QUALITY
            ),
            takePictureCallback,
            Futures.immediateFuture(null)
        )
    }

    /** Inject a ImageCaptureRotationOptionQuirk. */
    fun injectRotationOptionQuirk() {
        setStaticField(Build::class.java, "BRAND", "HUAWEI")
        setStaticField(Build::class.java, "MODEL", "SNE-LX1")
    }

    /** Creates an empty [ImageCaptureConfig] so [ImagePipeline] constructor won't crash. */
    fun createEmptyImageCaptureConfig(): ImageCaptureConfig {
        val builder = ImageCapture.Builder().setCaptureOptionUnpacker { _, _ -> }
        builder.mutableConfig.insertOption(ImageInputConfig.OPTION_INPUT_FORMAT, ImageFormat.JPEG)
        return builder.useCaseConfig
    }

    fun createCaptureBundle(stageIds: IntArray): CaptureBundle {
        return CaptureBundle { stageIds.map { stageId -> FakeCaptureStage(stageId, null) } }
    }

    fun createFakeImage(tagBundleKey: String, stageId: Int): ImageProxy {
        return FakeImageProxy(createCameraCaptureResultImageInfo(tagBundleKey, stageId))
    }

    fun createCameraCaptureResultImageInfo(tagBundleKey: String, stageId: Int): ImageInfo {
        return CameraCaptureResultImageInfo(
            FakeCameraCaptureResult().also {
                it.setTagBundle(TagBundle.create(Pair(tagBundleKey, stageId)))
            }
        )
    }

    fun createTakePictureRequest(
        outputFileOptions: ImageCapture.OutputFileOptions?,
        secondaryOutputFileOptions: ImageCapture.OutputFileOptions?,
        cropRect: Rect,
        sensorToBufferTransform: Matrix,
        rotationDegrees: Int,
        jpegQuality: Int,
        isSimultaneousCapture: Boolean = false
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
            secondaryOutputFileOptions,
            cropRect,
            sensorToBufferTransform,
            rotationDegrees,
            jpegQuality,
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            isSimultaneousCapture,
            listOf()
        )
    }
}
