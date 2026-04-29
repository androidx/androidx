/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FakeImageReaderTest {
    private val imageReader =
        FakeImageReader.create(
            StreamFormat.PRIVATE,
            StreamId(32),
            OutputId(42),
            Size(IMAGE_WIDTH, IMAGE_HEIGHT),
            10,
            null,
        )

    @After
    fun cleanup() {
        imageReader.close()
    }

    @Test
    fun imageReaderCanBeClosed() {
        assertThat(imageReader.isClosed).isFalse()

        imageReader.close()

        assertThat(imageReader.isClosed).isTrue()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    fun imageReaderCanSimulateImages() {
        val fakeImage = imageReader.simulateImage(100)

        assertThat(fakeImage.width).isEqualTo(IMAGE_WIDTH)
        assertThat(fakeImage.height).isEqualTo(IMAGE_HEIGHT)
        assertThat(fakeImage.format).isEqualTo(StreamFormat.PRIVATE.value)
        assertThat(fakeImage.timestamp).isEqualTo(100)
        assertThat(fakeImage.planes).hasSize(0)
        assertThat(fakeImage.isClosed).isFalse()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    fun imageReaderCanSimulateYuvImagesWith3Planes() {
        val yuvImageReader =
            FakeImageReader.create(
                StreamFormat.YUV_420_888,
                StreamId(32),
                OutputId(42),
                Size(IMAGE_WIDTH, IMAGE_HEIGHT),
                10,
                null,
            )
        val fakeImage = yuvImageReader.simulateImage(100)

        assertThat(fakeImage.format).isEqualTo(StreamFormat.YUV_420_888.value)
        assertThat(fakeImage.planes).hasSize(3)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    fun imageReaderCanSimulateImagesWithCustomHardwareBuffer() {
        val width = 10
        val height = 20
        val hardwareBuffer = HardwareBuffer.create(width, height, IMAGE_FORMAT, 1, 1)
        val fakeImage = imageReader.simulateImage(100, hardwareBuffer = hardwareBuffer)

        assertThat(fakeImage.width).isEqualTo(IMAGE_WIDTH)
        assertThat(fakeImage.height).isEqualTo(IMAGE_HEIGHT)
        assertThat(fakeImage.format).isEqualTo(StreamFormat.PRIVATE.value)
        assertThat(fakeImage.timestamp).isEqualTo(100)
        assertThat(fakeImage.hardwareBuffer).isNotNull()
        assertThat(fakeImage.hardwareBuffer?.width).isEqualTo(width)
        assertThat(fakeImage.hardwareBuffer?.height).isEqualTo(height)
        assertThat(fakeImage.hardwareBuffer?.format).isEqualTo(IMAGE_FORMAT)
        assertThat(fakeImage.isClosed).isFalse()

        hardwareBuffer.close()
    }

    @Test
    fun closingAnImageReaderDoesNotCloseImages() {
        val fakeImage = imageReader.simulateImage(100)
        imageReader.close()

        assertThat(fakeImage.isClosed).isFalse()
    }

    @Test
    fun imageReaderSimulatesANewImageEachTime() {
        val fakeImage1 = imageReader.simulateImage(100)
        val fakeImage2 = imageReader.simulateImage(100)

        assertThat(fakeImage1).isNotSameInstanceAs(fakeImage2)
        fakeImage2.close()

        assertThat(fakeImage1.isClosed).isFalse()
        assertThat(fakeImage2.isClosed).isTrue()
    }

    @Test
    fun simulatingImagesArePassedToListener() {
        val fakeListener = FakeOnImageListener()

        imageReader.simulateImage(100)
        imageReader.onImageListener = fakeListener
        val image2 = imageReader.simulateImage(200)
        val image3 = imageReader.simulateImage(300)

        assertThat(fakeListener.onImageEvents.size).isEqualTo(2)

        assertThat(fakeListener.onImageEvents[0].image).isNotNull()
        assertThat(fakeListener.onImageEvents[0].image).isSameInstanceAs(image2)
        assertThat(fakeListener.onImageEvents[0].streamId).isEqualTo(StreamId(32))
        assertThat(fakeListener.onImageEvents[0].outputId).isEqualTo(OutputId(42))

        assertThat(fakeListener.onImageEvents[1].image).isNotNull()
        assertThat(fakeListener.onImageEvents[1].image).isSameInstanceAs(image3)
        assertThat(fakeListener.onImageEvents[1].streamId).isEqualTo(StreamId(32))
        assertThat(fakeListener.onImageEvents[1].outputId).isEqualTo(OutputId(42))
    }

    companion object {
        private val IMAGE_HEIGHT: Int = 480
        private val IMAGE_WIDTH: Int = 640
        private val IMAGE_FORMAT: Int = 3
    }
}
