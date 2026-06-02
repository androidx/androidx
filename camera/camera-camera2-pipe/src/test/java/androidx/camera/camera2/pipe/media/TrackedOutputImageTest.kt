/*
 * Copyright 2026 The Android Open Source Project
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

import android.media.Image
import android.util.Size
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeImageReader
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class TrackedOutputImageTest {

    private val initialCapacity = 100_000_000L
    private val expectedStreamId = StreamId(42)
    private val expectedOutputId = OutputId(10)
    private val fakeImageSize = Size(100, 100)
    private val fakeImageFormat = StreamFormat.YUV_420_888

    // Helper to create a real ImageSource backed by a FakeImageReader
    private fun createTestImageSource(
        memoryEstimator: MemoryEstimator
    ): Pair<FakeImageReader, ImageReaderImageSource> {
        val fakeImageReader =
            FakeImageReader.create(
                format = fakeImageFormat,
                streamId = expectedStreamId,
                outputId = expectedOutputId,
                size = fakeImageSize,
                capacity = 10,
                usageFlags = null,
            )
        val imageSource =
            ImageReaderImageSource(
                fakeImageReader,
                maxImages = 8,
                memoryEstimator = memoryEstimator,
            )
        return Pair(fakeImageReader, imageSource)
    }

    @Test
    fun trackedImagePropertiesAreCorrect() {
        val memoryEstimator = MemoryEstimator.create()
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        assertThat(trackedImage.streamId).isEqualTo(expectedStreamId)
        assertThat(trackedImage.outputId).isEqualTo(expectedOutputId)
        assertThat(trackedImage.timestamp).isEqualTo(1234L)
        assertThat(trackedImage.format).isEqualTo(fakeImageFormat.value)

        fakeImageReader.close()
    }

    @Test
    fun trackedImageUnwrapAsWorksCorrectly() {
        val memoryEstimator = MemoryEstimator.create()
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        // Should unwrap to itself for its own implemented types
        assertThat(trackedImage.unwrapAs(TrackedOutputImage::class.java))
            .isSameInstanceAs(trackedImage)
        assertThat(trackedImage.unwrapAs(OutputImage::class.java)).isSameInstanceAs(trackedImage)
        assertThat(trackedImage.unwrapAs(ImageWrapper::class.java)).isSameInstanceAs(trackedImage)

        // Should delegate to the underlying FakeImage for unknown types (e.g. android.media.Image)
        assertThat(trackedImage.unwrapAs(Image::class.java)).isNull()

        fakeImageReader.close()
    }

    @Test
    fun closeDelegatesToImageSource() {
        val memoryEstimator = MemoryEstimator.create()
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        assertThat(fakeImage.isClosed).isFalse()

        // Close the tracked wrapper
        trackedImage.close()

        // Ensure it properly cascaded the close down to the FakeImage
        assertThat(fakeImage.isClosed).isTrue()

        fakeImageReader.close()
    }

    @Test
    fun trackedImageUpdatesEvictableMemoryCorrectly() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val expectedBytes =
            StreamFormat.bytesPerImage(fakeImageFormat, fakeImageSize.width, fakeImageSize.height)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        // 1. Create the image
        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        // At birth, external usage is 0, so it should be evictable.
        assertThat(memoryEstimator.usage.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.evictable.value).isEqualTo(expectedBytes)

        // 2. Simulate the App acquiring it (External Use)
        trackedImage.incrementExternalUse()

        // It is no longer evictable!
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)
        assertThat(memoryEstimator.usage.value).isEqualTo(expectedBytes)

        // 3. Simulate the App dropping it
        trackedImage.decrementExternalUse()

        // It becomes evictable again!
        assertThat(memoryEstimator.evictable.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.usage.value).isEqualTo(expectedBytes)

        // 4. Simulate the pipeline permanently destroying it
        trackedImage.close()

        // The image is fully closed, so it must be completely removed from all memory math.
        assertThat(memoryEstimator.usage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)

        fakeImageReader.close()
    }

    @Test
    fun closeIsIdempotentAndDoesNotDoubleSubtractMemory() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val expectedBytes =
            StreamFormat.bytesPerImage(fakeImageFormat, fakeImageSize.width, fakeImageSize.height)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        // Baseline: Memory is allocated and evictable
        assertThat(memoryEstimator.usage.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.evictable.value).isEqualTo(expectedBytes)

        // First close: Memory is freed and returned to capacity
        trackedImage.close()
        assertThat(memoryEstimator.usage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)

        // Second close: Should be completely ignored by the atomic `closed` guard
        trackedImage.close()

        // If it wasn't idempotent, capacity would incorrectly jump above initial capacity!
        assertThat(memoryEstimator.usage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)

        fakeImageReader.close()
    }

    @Test
    fun closingWhileExternalUseIsActiveClearsMemorySafely() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val expectedBytes =
            StreamFormat.bytesPerImage(fakeImageFormat, fakeImageSize.width, fakeImageSize.height)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageSource,
                fakeImage,
                expectedStreamId,
                expectedOutputId,
                memoryEstimator,
            )

        // The App acquires the image. It is removed from the evictable pool.
        trackedImage.incrementExternalUse()
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)
        assertThat(memoryEstimator.usage.value).isEqualTo(expectedBytes)

        // The image is somehow closed while the app still has it.
        trackedImage.close()

        // The memory estimator must instantly return the capacity to max, and evictable
        // usage must safely remain at 0.
        assertThat(memoryEstimator.usage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)

        // Later, the App finally drops the frame.
        trackedImage.decrementExternalUse()

        // Should detect the image is already closed and do nothing.
        assertThat(memoryEstimator.usage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictable.value).isEqualTo(0L)

        fakeImageReader.close()
    }
}
