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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.EXIF_GAINMAP_PATTERNS
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.TIMESTAMP
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.createTakePictureRequest
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.core.internal.compat.quirk.IncorrectJpegMetadataQuirk
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.core.processing.InternalImageProcessor
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil.COLOR_GRAY
import androidx.camera.testing.impl.TestImageUtil.COLOR_WHITE
import androidx.camera.testing.impl.TestImageUtil.createA24ProblematicJpegByteArray
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.createBitmapWithGainmap
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createYuvFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.camera.testing.impl.fakes.GrayscaleImageEffect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [ProcessingNode]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class ProcessingNodeDeviceTest {

    // The color before and after the encoding/decoding process on API 23 or below devices might
    // have some deviation. For example, the Color.BLUE color (-16776961) might become -16776965.
    // This will cause some testing failures. Therefore, use the tolerance value to check the
    // results for the API 21 ~ 23 devices.
    private val avgDiffTolerance =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            0
        } else {
            1
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

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun processImageEqualsCropSize_croppingNotInvoked_whenFormatIsJpegr() = runBlocking {
        cropRectEqualsImageRect_croppingNotInvoked_whenFormatIsJpegr(OUTPUT_FILE_OPTIONS)
    }

    @Test
    fun processInMemoryInputPacket_callbackInvoked() = runBlocking {
        inMemoryInputPacket_callbackInvoked(null)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun processInMemoryInputPacket_callbackInvoked_whenFormatIsJpegr() = runBlocking {
        inMemoryInputPacket_callbackInvoked_withJpegrFormat(null)
    }

    @Test
    fun processSaveJpegOnDisk_verifyOutput() = runBlocking {
        saveJpegOnDisk_verifyOutput(OUTPUT_FILE_OPTIONS)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun processSaveJpegOnDisk_verifyOutput_whenFormatIsJpegr() = runBlocking {
        saveJpegrOnDisk_verifyOutput(OUTPUT_FILE_OPTIONS)
    }

    private suspend fun processYuvAndVerifyOutputSize(outputFileOptions: OutputFileOptions?) {
        // Arrange: create node with JPEG input and grayscale effect.
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(ImageFormat.YUV_420_888, listOf(ImageFormat.JPEG))
        val imageIn =
            createYuvFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                WIDTH,
                HEIGHT,
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
        val node =
            ProcessingNode(
                mainThreadExecutor(),
                null,
                InternalImageProcessor(GrayscaleImageEffect()),
            )
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, listOf(ImageFormat.JPEG))
        val imageIn =
            createJpegFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createJpegBytes(WIDTH, HEIGHT),
            )
        // Act.
        val bitmap = processAndGetBitmap(node, nodeIn, imageIn, outputFileOptions)
        // Assert: the output is a cropped grayscale image.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), 0X555555))
            .isAtMost(avgDiffTolerance)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), 0XAAAAAA))
            .isAtMost(avgDiffTolerance)
    }

    private suspend fun processAndGetBitmap(
        node: ProcessingNode,
        nodeIn: ProcessingNode.In,
        imageIn: ImageProxy,
        outputFileOptions: OutputFileOptions?,
    ): Bitmap {
        // Arrange: create a YUV input.
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()
        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    CROP_RECT,
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
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

    private suspend fun cropRectEqualsImageRect_croppingNotInvoked(
        outputFileOptions: OutputFileOptions?
    ) {
        // Arrange: create a request with no cropping
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, listOf(ImageFormat.JPEG))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val imageIn =
            createJpegFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createJpegBytes(WIDTH, HEIGHT),
            )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        // Act and return.
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: restored image is not cropped.
        val restoredBitmap = BitmapFactory.decodeFile(filePath)

        // Assert: restored image is not cropped.
        assertThat(getAverageDiff(createBitmap(WIDTH, HEIGHT), restoredBitmap))
            .isAtMost(avgDiffTolerance)
    }

    @RequiresApi(api = 34)
    private suspend fun cropRectEqualsImageRect_croppingNotInvoked_whenFormatIsJpegr(
        outputFileOptions: OutputFileOptions?
    ) {
        // Arrange: create a request with no cropping
        val format = ImageFormat.JPEG_R
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(format, listOf(format))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val imageIn =
            createJpegrFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createJpegrBytes(WIDTH, HEIGHT),
            )

        // Act.
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: restored image is not cropped.
        val restoredBitmap = BitmapFactory.decodeFile(filePath)
        assertThat(getAverageDiff(createBitmap(WIDTH, HEIGHT), restoredBitmap))
            .isAtMost(avgDiffTolerance)

        // Assert: JPEG/R related info when format is JPEG/R.
        assertThat(restoredBitmap.hasGainmap()).isTrue()
        val gainmapContents = restoredBitmap.gainmap!!.gainmapContents
        assertThat(
                getAverageDiff(
                    createBitmapWithGainmap(WIDTH, HEIGHT).gainmap!!.gainmapContents,
                    gainmapContents,
                )
            )
            .isAtMost(avgDiffTolerance)
    }

    private suspend fun inMemoryInputPacket_callbackInvoked(outputFileOptions: OutputFileOptions?) {
        // Arrange.
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, listOf(ImageFormat.JPEG))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val imageIn =
            createJpegFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createJpegBytes(WIDTH, HEIGHT),
            )
        // Act.
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        // Act and return.
        nodeIn.edge.accept(input)
        // Assert: the output image is identical to the input.
        val imageOut = takePictureCallback.getInMemoryResult()
        val restoredJpeg = jpegImageToJpegByteArray(imageOut)

        assertThat(getAverageDiff(createJpegBytes(WIDTH, HEIGHT), restoredJpeg))
            .isAtMost(avgDiffTolerance)
        assertThat(imageOut.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }

    @RequiresApi(api = 34)
    private suspend fun inMemoryInputPacket_callbackInvoked_withJpegrFormat(
        outputFileOptions: OutputFileOptions?
    ) {
        // Arrange.
        val format = ImageFormat.JPEG_R
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(format, listOf(format))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val imageIn =
            createJpegrFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createJpegrBytes(WIDTH, HEIGHT),
            )

        // Act.
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        nodeIn.edge.accept(input)

        // Assert: the output image is identical to the input.
        val imageOut = takePictureCallback.getInMemoryResult()
        assertThat(imageOut.format).isEqualTo(ImageFormat.JPEG_R)
        val restoredJpegByteArray = jpegImageToJpegByteArray(imageOut)
        val expectedJpegByteArray = createJpegrBytes(WIDTH, HEIGHT)
        assertThat(getAverageDiff(expectedJpegByteArray, restoredJpegByteArray))
            .isAtMost(avgDiffTolerance)
        assertThat(expectedJpegByteArray.size).isEqualTo(restoredJpegByteArray.size)
        assertThat(imageOut.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }

    private suspend fun saveJpegOnDisk_verifyOutput(outputFileOptions: OutputFileOptions?) {
        // Arrange: create a on-disk processing request.
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, listOf(ImageFormat.JPEG))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()
        val jpegBytes =
            ExifUtil.updateExif(createJpegBytes(640, 480)) { it.description = EXIF_DESCRIPTION }
        val imageIn =
            createJpegFakeImageProxy(CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT), jpegBytes)
        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    CROP_RECT,
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)

        // Act: send input to the edge and wait for the saved URI
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: image content is cropped correctly
        val bitmap = BitmapFactory.decodeFile(filePath)
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), Color.BLUE))
            .isAtMost(avgDiffTolerance)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), Color.YELLOW))
            .isAtMost(avgDiffTolerance)
        // Assert: Exif info is saved correctly.
        val exif = Exif.createFromFileString(filePath)
        assertThat(exif.description).isEqualTo(EXIF_DESCRIPTION)
    }

    @RequiresApi(api = 34)
    private suspend fun saveJpegrOnDisk_verifyOutput(outputFileOptions: OutputFileOptions?) {
        // Arrange: create a on-disk processing request.
        val format = ImageFormat.JPEG_R
        val node = ProcessingNode(mainThreadExecutor(), null)
        val nodeIn = ProcessingNode.In.of(format, listOf(format))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()
        val jpegBytes =
            ExifUtil.updateExif(createJpegrBytes(640, 480)) { it.description = EXIF_DESCRIPTION }
        val imageIn =
            createJpegrFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                jpegBytes,
            )
        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    outputFileOptions,
                    null,
                    CROP_RECT,
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )

        // Act: send input to the edge and wait for the saved URI
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        nodeIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: image content is cropped correctly
        val bitmap = BitmapFactory.decodeFile(filePath)
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), Color.BLUE))
            .isAtMost(avgDiffTolerance)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), Color.YELLOW))
            .isAtMost(avgDiffTolerance)

        // Assert: JPEG/R related info when format is JPEG/R.
        assertThat(bitmap.hasGainmap()).isTrue()
        val gainmapContents = bitmap.gainmap!!.gainmapContents
        assertThat(getAverageDiff(gainmapContents, Rect(0, 0, WIDTH, 120), COLOR_GRAY))
            .isAtMost(avgDiffTolerance)
        assertThat(getAverageDiff(gainmapContents, Rect(0, 121, WIDTH, 240), COLOR_WHITE))
            .isAtMost(avgDiffTolerance)

        // Assert: Exif info is saved correctly.
        val exif = Exif.createFromFileString(filePath)
        assertThat(exif.description).isEqualTo(EXIF_DESCRIPTION)
        val exifMetadata = exif.metadata
        assertThat(exifMetadata).isNotNull()
        for (pattern in EXIF_GAINMAP_PATTERNS) {
            assertThat(exifMetadata).contains(pattern)
        }
    }

    @Test
    fun canFixIncorrectJpegMetadataForA24Device(): Unit = runBlocking {
        // Arrange.
        // Force inject the quirk for the A24 incorrect JPEG metadata problem
        val node =
            ProcessingNode(mainThreadExecutor(), Quirks(listOf(IncorrectJpegMetadataQuirk())), null)
        val nodeIn = ProcessingNode.In.of(ImageFormat.JPEG, listOf(ImageFormat.JPEG))
        node.transform(nodeIn)
        val takePictureCallback = FakeTakePictureCallback()

        val processingRequest =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    /*outputFileOptions=*/ null,
                    /*secondaryOutputFileOptions=*/ null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    /*rotationDegrees=*/ 0, // 0 because exif does not have rotation.
                    /*jpegQuality=*/ 100,
                ),
                takePictureCallback,
                Futures.immediateFuture(null),
            )
        val imageIn =
            createJpegFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                createA24ProblematicJpegByteArray(WIDTH, HEIGHT),
                WIDTH,
                HEIGHT,
            )
        // Act.
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)
        // Act and return.
        nodeIn.edge.accept(input)
        // Assert: the output image is identical to the input.
        val imageOut = takePictureCallback.getInMemoryResult()
        val restoredJpeg = jpegImageToJpegByteArray(imageOut)

        assertThat(getAverageDiff(createJpegBytes(WIDTH, HEIGHT), restoredJpeg))
            .isAtMost(avgDiffTolerance)
    }
}
