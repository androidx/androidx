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

import android.graphics.BitmapFactory
import android.graphics.Color.BLUE
import android.graphics.Color.YELLOW
import android.graphics.ImageFormat
import android.graphics.Rect
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.TIMESTAMP
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.imagecapture.Utils.createProcessingRequest
import androidx.camera.core.impl.utils.Exif.createFromFileString
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.testing.ExifUtil.updateExif
import androidx.camera.testing.TestImageUtil.createBitmap
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.TestImageUtil.getAverageDiff
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ProcessingNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ProcessingNodeTest {

    private lateinit var processingNodeIn: ProcessingNode.In

    private val node = ProcessingNode(mainThreadExecutor())

    @Before
    fun setUp() {
        processingNodeIn = ProcessingNode.In.of(ImageFormat.JPEG)
        node.transform(processingNodeIn)
    }

    @Test
    fun processAbortedRequest_noOps() {
        // Arrange: create a request with aborted callback.
        val callback = FakeTakePictureCallback()
        callback.aborted = true
        val request = ProcessingRequest(
            { listOf() },
            OUTPUT_FILE_OPTIONS,
            Rect(0, 0, WIDTH, HEIGHT),
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            callback
        )

        // Act: process the request.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is not saved.
        assertThat(callback.onDiskResult).isNull()
    }

    @Test
    fun cropRectEqualsImageRect_croppingNotInvoked() {
        // Arrange: create a request with no cropping
        val callback = FakeTakePictureCallback()
        val request = ProcessingRequest(
            { listOf() },
            OUTPUT_FILE_OPTIONS,
            Rect(0, 0, WIDTH, HEIGHT),
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            callback
        )
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        // Track if cropping is invoked.
        var croppingInvoked = false
        node.injectJpegBytes2CroppedBitmapForTesting {
            croppingInvoked = true
            JpegBytes2CroppedBitmap().apply(it)
        }

        // Act.
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()
        val filePath = callback.onDiskResult!!.savedUri!!.path!!

        // Assert: restored image is not cropped.
        val restoredBitmap = BitmapFactory.decodeFile(filePath)
        assertThat(getAverageDiff(createBitmap(WIDTH, HEIGHT), restoredBitmap)).isEqualTo(0)
        // Assert: cropping was not invoked.
        assertThat(croppingInvoked).isFalse()
    }

    @Test
    fun inMemoryInputPacket_callbackInvoked() {
        // Arrange.
        val callback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(createCaptureBundle(intArrayOf()), callback)
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(
            CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT), jpegBytes
        )
        // Act.
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()
        // Assert: the output image is identical to the input.
        val imageOut = callback.inMemoryResult!!
        val restoredJpeg = jpegImageToJpegByteArray(imageOut)
        assertThat(getAverageDiff(jpegBytes, restoredJpeg)).isEqualTo(0)
        assertThat(imageOut.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }

    @Test
    fun saveIncorrectImage_getsErrorCallback() {
        // Arrange: create an invalid ImageProxy.
        val takePictureCallback = FakeTakePictureCallback()
        val image = FakeImageProxy(FakeImageInfo())
        val processingRequest = createProcessingRequest(takePictureCallback)
        val input = ProcessingNode.InputPacket.of(processingRequest, image)

        // Act: send input to the edge and wait for callback
        processingNodeIn.edge.accept(input)
        shadowOf(getMainLooper()).idle()

        // Assert: receives a process failure.
        assertThat(takePictureCallback.processFailure)
            .isInstanceOf(ImageCaptureException::class.java)
    }

    @Test
    fun saveJpegOnDisk_verifyOutput() {
        // Arrange: create a on-disk processing request.
        val takePictureCallback = FakeTakePictureCallback()
        val jpegBytes = updateExif(createJpegBytes(640, 480)) {
            it.description = EXIF_DESCRIPTION
        }
        val image = createJpegFakeImageProxy(jpegBytes)
        val processingRequest = createProcessingRequest(takePictureCallback)
        val input = ProcessingNode.InputPacket.of(processingRequest, image)

        // Act: send input to the edge and wait for the saved URI
        processingNodeIn.edge.accept(input)
        shadowOf(getMainLooper()).idle()
        val filePath = takePictureCallback.onDiskResult!!.savedUri!!.path!!

        // Assert: image content is cropped correctly
        val bitmap = BitmapFactory.decodeFile(filePath)
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), BLUE)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, WIDTH, 240), YELLOW)).isEqualTo(0)
        // Assert: Exif info is saved correctly.
        val exif = createFromFileString(filePath)
        assertThat(exif.description).isEqualTo(EXIF_DESCRIPTION)
    }
}
