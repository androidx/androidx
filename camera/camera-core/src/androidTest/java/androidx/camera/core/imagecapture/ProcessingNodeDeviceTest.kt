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
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.TIMESTAMP
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.core.processing.InternalImageProcessor
import androidx.camera.testing.AndroidUtil
import androidx.camera.testing.ExifUtil
import androidx.camera.testing.TestImageUtil.createBitmap
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
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ProcessingNode].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ProcessingNodeDeviceTest {

    @Before
    fun setUp() {
        assumeFalse(AndroidUtil.isEmulatorAndAPI21())
    }

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

    @Test
    fun processImageEqualsCropSize_croppingNotInvoked() = runBlocking {
        cropRectEqualsImageRect_croppingNotInvoked(OUTPUT_FILE_OPTIONS)
    }

    @Test
    fun processInMemoryInputPacket_callbackInvoked() = runBlocking {
        inMemoryInputPacket_callbackInvoked(null)
    }

    @Test
    fun processSaveJpegOnDisk_verifyOutput() = runBlocking {
        saveJpegOnDisk_verifyOutput(OUTPUT_FILE_OPTIONS)
    }

    private suspend fun processYuvAndVerifyOutputSize(outputFileOptions: OutputFileOptions?) {
        // Arrange: create node with JPEG input and grayscale effect.
        val node = ProcessingNode(mainThreadExecutor())
        val nodeIn = ProcessingNode.In.of(ImageFormat.YUV_420_888, ImageFormat.JPEG)
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
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, ImageFormat.JPEG)
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
            takePictureCallback,
            Futures.immediateFuture(null)
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn, false)
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

    private suspend fun cropRectEqualsImageRect_croppingNotInvoked(
        outputFileOptions: OutputFileOptions?
    ) {
        // Arrange: create a request with no cropping
        val node = ProcessingNode(mainThreadExecutor())
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, ImageFormat.JPEG)
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest = ProcessingRequest(
            { listOf() },
            outputFileOptions,
            Rect(0, 0, WIDTH, HEIGHT),
            0,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback,
            Futures.immediateFuture(null)
        )
        val imageIn = createJpegFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
            createJpegBytes(WIDTH, HEIGHT)
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn, false)
        // Act and return.
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: restored image is not cropped.
        val restoredBitmap = BitmapFactory.decodeFile(filePath)

        // Assert: restored image is not cropped.
        assertThat(
            getAverageDiff(
                createBitmap(WIDTH, HEIGHT),
                restoredBitmap
            )
        ).isEqualTo(0)
    }

    private suspend fun inMemoryInputPacket_callbackInvoked(outputFileOptions: OutputFileOptions?) {
        // Arrange.
        val node = ProcessingNode(mainThreadExecutor())
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, ImageFormat.JPEG)
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest = ProcessingRequest(
            { listOf() },
            outputFileOptions,
            Rect(0, 0, WIDTH, HEIGHT),
            0,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback,
            Futures.immediateFuture(null)
        )
        val imageIn = createJpegFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
            createJpegBytes(WIDTH, HEIGHT)
        )
        // Act.
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn, false)
        // Act and return.
        nodeIn.edge.accept(input)
        // Assert: the output image is identical to the input.
        val imageOut = takePictureCallback.getInMemoryResult()
        val restoredJpeg = jpegImageToJpegByteArray(imageOut)

        assertThat(getAverageDiff(createJpegBytes(WIDTH, HEIGHT), restoredJpeg)).isEqualTo(0)
        assertThat(imageOut.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }

    private suspend fun saveJpegOnDisk_verifyOutput(outputFileOptions: OutputFileOptions?) {
        // Arrange: create a on-disk processing request.
        val node = ProcessingNode(mainThreadExecutor())
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, ImageFormat.JPEG)
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()
        val jpegBytes = ExifUtil.updateExif(createJpegBytes(640, 480)) {
            it.description = EXIF_DESCRIPTION
        }
        val imageIn = createJpegFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
            jpegBytes
        )
        val processingRequest = ProcessingRequest(
            { listOf() },
            outputFileOptions,
            CROP_RECT,
            0,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback,
            Futures.immediateFuture(null)
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn, false)

        // Act: send input to the edge and wait for the saved URI
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: image content is cropped correctly
        val bitmap = BitmapFactory.decodeFile(filePath)

        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), Color.BLUE)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), Color.YELLOW)).isEqualTo(0)
        // Assert: Exif info is saved correctly.
        val exif = Exif.createFromFileString(filePath)
        assertThat(exif.description).isEqualTo(EXIF_DESCRIPTION)
    }
}
