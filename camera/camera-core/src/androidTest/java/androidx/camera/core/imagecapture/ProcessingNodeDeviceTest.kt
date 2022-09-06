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
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.ImageFormat
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.utils.ImageUtil
import androidx.camera.testing.TestImageUtil.createYuvFakeImageProxy
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [JpegBytes2Image].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ProcessingNodeDeviceTest {

    @Test
    fun processYuvInputInMemory_getsJpegOutput() = runBlocking {
        // Arrange: create a YUV input.
        val yuvIn = ProcessingNode.In.of(ImageFormat.YUV_420_888)
        ProcessingNode(mainThreadExecutor()).also { it.transform(yuvIn) }
        val takePictureCallback = FakeTakePictureCallback()
        val imageIn = createYuvFakeImageProxy(FakeImageInfo(), WIDTH, HEIGHT)
        val processingRequest = ProcessingRequest(
            { listOf() },
            /*outputFileOptions=*/ null,
            CROP_RECT,
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)

        // Act.
        yuvIn.edge.accept(input)
        val imageOut = takePictureCallback.getInMemoryResult()

        // Assert: image content is cropped correctly
        // TODO(b/245940015): verify the content of the restored image.
        val jpegOut = ImageUtil.jpegImageToJpegByteArray(imageOut)
        val bitmapOut = decodeByteArray(jpegOut, 0, jpegOut.size)
        assertThat(bitmapOut.width).isEqualTo(WIDTH)
        assertThat(bitmapOut.height).isEqualTo(HEIGHT / 2)
    }

    @Test
    fun processYuvInputOnDisk_getsJpegOutput() = runBlocking {
        // Arrange: create a YUV input.
        val yuvIn = ProcessingNode.In.of(ImageFormat.YUV_420_888)
        ProcessingNode(mainThreadExecutor()).also { it.transform(yuvIn) }
        val takePictureCallback = FakeTakePictureCallback()
        val imageIn = createYuvFakeImageProxy(FakeImageInfo(), WIDTH, HEIGHT)
        val processingRequest = ProcessingRequest(
            { listOf() },
            OUTPUT_FILE_OPTIONS,
            CROP_RECT,
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            takePictureCallback
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, imageIn)

        // Act.
        yuvIn.edge.accept(input)
        val filePath = takePictureCallback.getOnDiskResult().savedUri!!.path!!

        // Assert: image content is cropped correctly
        // TODO(b/245940015): verify the content of the restored image.
        val bitmap = BitmapFactory.decodeFile(filePath)
        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT / 2)
    }
}