/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeImageReader
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [OutputImage] and [SharedOutputImage] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImageSourceTest {
    private val streamId = StreamId(32)
    private val outputId = OutputId(42)
    private val fakeImageSize = Size(640, 480)
    private val fakeImageFormat = StreamFormat.YUV_420_888
    private val fakeImageReader =
        FakeImageReader.create(
            format = fakeImageFormat,
            streamId = streamId,
            outputId = outputId,
            size = fakeImageSize,
            capacity = 10
        )
    private val imageSource = ImageReaderImageSource(fakeImageReader, fakeImageReader.capacity - 2)

    @After
    fun cleanup() {
        fakeImageReader.close()
    }

    @Test
    fun testImageSourceForwardsImagesFromImageReader() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)
        fakeImageReader.simulateImage(12345)

        assertThat(testListener.onImageEvents.size).isEqualTo(1)
        assertThat(testListener.onImageEvents[0].outputId).isEqualTo(outputId)
        assertThat(testListener.onImageEvents[0].outputTimestamp).isEqualTo(12345)
        assertThat(testListener.onImageEvents[0].image).isNotNull()
    }

    @Test
    fun testImageSourceForwardsEmptyImagesAfterReachingCapacity() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)

        for (i in 0..99) {
            fakeImageReader.simulateImage(12345 + (i * 10000L))
        }

        assertThat(testListener.onImageEvents.size).isEqualTo(100)
        // Spot check 11th index
        assertThat(testListener.onImageEvents[10].outputId).isEqualTo(outputId)
        assertThat(testListener.onImageEvents[10].image).isNull()

        // Spot check last index
        assertThat(testListener.onImageEvents[99].outputId).isEqualTo(outputId)
        assertThat(testListener.onImageEvents[99].image).isNull()
    }

    @Test
    fun closingImagesAllowsAllImagesToBeProduced() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)

        for (i in 0..99) {
            fakeImageReader.simulateImage(12345 + (i * 10000L))
            testListener.onImageEvents.last().image!!.close()
        }

        assertThat(testListener.onImageEvents.size).isEqualTo(100)
        // Spot check 11th index
        assertThat(testListener.onImageEvents[10].outputId).isEqualTo(outputId)
        assertThat(testListener.onImageEvents[10].image).isNotNull()

        // Spot check last index
        assertThat(testListener.onImageEvents[99].outputId).isEqualTo(outputId)
        assertThat(testListener.onImageEvents[99].image).isNotNull()
    }

    @Test
    fun imagesWithoutAListenerAreClosed() {
        val image =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 12345)

        fakeImageReader.simulateImage(image, outputId)
        assertThat(image.isClosed).isTrue()
    }

    @Test
    fun closingImageSourceClosesImageReader() {
        imageSource.close()
        assertThat(fakeImageReader.isClosed)
    }

    @Test
    fun closingImageSourceAfterClosingImagesClosesImageReader() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)

        // Simulate 3 images.
        fakeImageReader.simulateImage(12345)
        fakeImageReader.simulateImage(12345)
        fakeImageReader.simulateImage(12345)

        // Close all the images
        testListener.onImageEvents.forEach { it.image!!.close() }

        // Close the image source
        imageSource.close()

        // Check that the image reader is closed.
        assertThat(fakeImageReader.isClosed).isTrue()
    }

    @Test
    fun closingImageSourceBeforeClosingImagesClosesImageReader() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)

        // Simulate 3 images.
        fakeImageReader.simulateImage(12345)
        fakeImageReader.simulateImage(12345)
        fakeImageReader.simulateImage(12345)

        // Close the image source before closing images.
        imageSource.close()

        // Check that the image reader is *NOT* closed.
        assertThat(fakeImageReader.isClosed).isFalse()

        // Close all the images
        testListener.onImageEvents.forEach { it.image!!.close() }

        // Check that the image reader is now closed.
        assertThat(fakeImageReader.isClosed).isTrue()
    }

    @Test
    fun imagesAfterCloseAreClosed() {
        val testListener = TestImageSourceListener()
        imageSource.setListener(testListener)

        // Simulate 3 images.
        fakeImageReader.simulateImage(12345)
        fakeImageReader.simulateImage(12346)
        fakeImageReader.simulateImage(12347)

        // Close the image source before closing images.
        imageSource.close()

        // Check that the image reader is *NOT* closed.
        assertThat(fakeImageReader.isClosed).isFalse()

        // Now simulate the imageReader producing images after the imageSource is closed
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 54321)
        fakeImageReader.simulateImage(fakeImage, outputId)
        // Image is immediately closed
        assertThat(fakeImage.isClosed)

        // Event is fired, but the image is *not* passed down
        assertThat(testListener.onImageEvents.size).isEqualTo(4)
        assertThat(testListener.onImageEvents.last().image).isNull()
        assertThat(testListener.onImageEvents.last().outputTimestamp).isEqualTo(fakeImage.timestamp)

        // Close all the images
        testListener.onImageEvents.forEach { it.image?.close() }

        // Make sure the image reader gets closed.
        assertThat(fakeImageReader.isClosed).isTrue()
    }

    private class TestImageSourceListener : ImageSourceListener {
        val onImageEvents = mutableListOf<OnImage>()

        data class OnImage(
            val streamId: StreamId,
            val outputId: OutputId,
            val outputTimestamp: Long,
            val image: ImageWrapper?,
        )

        override fun onImage(
            streamId: StreamId,
            outputId: OutputId,
            outputTimestamp: Long,
            image: ImageWrapper?
        ) {
            onImageEvents.add(OnImage(streamId, outputId, outputTimestamp, image))
        }
    }
}
