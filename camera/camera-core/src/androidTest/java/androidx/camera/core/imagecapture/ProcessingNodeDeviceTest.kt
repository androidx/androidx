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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.core.processing.InternalImageProcessor
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.TestImageUtil.createYuvFakeImageProxy
import androidx.camera.testing.TestImageUtil.getAverageDiff
import androidx.camera.testing.fakes.GrayscaleImageEffect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ProcessingNode].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ProcessingNodeDeviceTest {
    @Test
    fun applyBitmapEffectInMemory_effectApplied() = runBlocking {
        processJpegAndVerifyEffectApplied(null)
    }

    @Test
    fun applyBitmapEffectOnDisk_effectApplied() = runBlocking {
        processJpegAndVerifyEffectApplied(OUTPUT_FILE_OPTIONS)
    }

    @Test
    fun processYuvInputInMemory_getsJpegOutput() = runBlocking {
        processYuvAndVerifyOutputSize(null)
    }

    @Test
    fun processYuvInputOnDisk_getsJpegOutput() = runBlocking {
        processYuvAndVerifyOutputSize(OUTPUT_FILE_OPTIONS)
    }

    private suspend fun processYuvAndVerifyOutputSize(outputFileOptions: OutputFileOptions?) {
        // Arrange: create node with JPEG input and grayscale effect.
        val node = ProcessingNode(mainThreadExecutor())
        val nodeIn = ProcessingNode.In.of(ImageFormat.YUV_420_888)
        val imageIn = createYuvFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
            WIDTH,
            HEIGHT
        )
        // Act.
        val bitmap = processAndGetBitmap(node, nodeIn, imageIn, outputFileOptions)
        // Assert: image content is cropped correctly
        // TODO(b/245940015): verify the content of the restored image.
        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT / 2)
    }

    private suspend fun processJpegAndVerifyEffectApplied(outputFileOptions: OutputFileOptions?) {
        // Arrange: create node with JPEG input and grayscale effect.
        val node = ProcessingNode(
            mainThreadExecutor(),
            InternalImageProcessor(GrayscaleImageEffect())
        )
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG)
        val imageIn = createJpegFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
            createJpegBytes(WIDTH, HEIGHT)
        )
        // Act.
        val bitmap = processAndGetBitmap(node, nodeIn, imageIn, outputFileOptions)
        // Assert: the output is a cropped grayscale image.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), 0X555555)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), 0XAAAAAA)).isEqualTo(0)
    }

    private suspend fun processAndGetBitmap(
        node: ProcessingNode,
        nodeIn: ProcessingNode.In,
        imageIn: ImageProxy,
        outputFileOptions: OutputFileOptions?
    ): Bitmap {
        // Arrange: create a YUV input.
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()
        val processingRequest = ProcessingRequest(
            { listOf() },
            outputFileOptions,
            CROP_RECT,
            /*rotationDegrees=*/0, // 0 because exif does not have rotation.
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        // Act and return.
        nodeIn.edge.accept(input)
        return if (outputFileOptions == null) {
            val imageOut = takePictureCallback.getInMemoryResult()
            val jpegOut = jpegImageToJpegByteArray(imageOut)
            decodeByteArray(jpegOut, 0, jpegOut.size)
        } else {
            val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!
            BitmapFactory.decodeFile(filePath)
        }
    }
}
