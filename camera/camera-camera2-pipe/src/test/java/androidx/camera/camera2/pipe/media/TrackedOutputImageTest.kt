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
    private val expectedBytes =
        StreamFormat.bytesPerImage(fakeImageFormat, fakeImageSize.width, fakeImageSize.height)

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
                imageReader = fakeImageReader,
                maxImages = 8,
                usageFlags = null,
                memoryEstimator = memoryEstimator,
            )
        return Pair(fakeImageReader, imageSource)
    }

    private fun createTrackedImage(
        imageSource: ImageReaderImageSource,
        fakeImage: FakeImage,
        memoryEstimator: MemoryEstimator,
        skipMemoryTracking: Boolean = false,
    ): TrackedOutputImage {
        return TrackedOutputImage(
            imageReaderImageSource = imageSource,
            image = fakeImage,
            streamId = expectedStreamId,
            outputId = expectedOutputId,
            memoryEstimator = memoryEstimator,
        )
    }

    @Test
    fun trackedImagePropertiesAreCorrect() {
        val memoryEstimator = MemoryEstimator.create()
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

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

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

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

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

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
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        // 1. Create the image
        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

        // At birth, external usage is 0, so it should be evictable.
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        // 2. Simulate the App acquiring it (External Use)
        trackedImage.incrementExternalUse()

        // It is no longer evictable!
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(expectedBytes)

        // 3. Simulate the App dropping it
        trackedImage.decrementExternalUse()

        // It becomes evictable again!
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(expectedBytes)

        // 4. Simulate the pipeline permanently destroying it
        trackedImage.close()

        // The image is fully closed, so it must be completely removed from all memory math.
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        fakeImageReader.close()
    }

    @Test
    fun trackedImageAddExternalUseUpdatesEvictableMemoryCorrectly() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        // Adding 0 should do nothing
        trackedImage.addExternalUse(0)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        // Adding > 0 moves it out of evictable
        trackedImage.addExternalUse(2)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        // Decrementing should slowly return it back towards evictable state
        trackedImage.decrementExternalUse()
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        trackedImage.decrementExternalUse()
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        fakeImageReader.close()
    }

    @Test
    fun closeIsIdempotentAndDoesNotDoubleSubtractMemory() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

        // Baseline: Memory is allocated and evictable
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(expectedBytes)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        // First close: Memory is freed and returned to capacity
        trackedImage.close()
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        // Second close: Should be completely ignored by the atomic `closed` guard
        trackedImage.close()

        // If it wasn't idempotent, capacity would incorrectly jump above initial capacity!
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        fakeImageReader.close()
    }

    @Test
    fun closingWhileExternalUseIsActiveClearsMemorySafely() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)

        // The App acquires the image. It is removed from the evictable pool.
        trackedImage.incrementExternalUse()
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(expectedBytes)

        // The image is somehow closed while the app still has it.
        trackedImage.close()

        // The memory estimator must instantly return the capacity to max, and evictable
        // usage must safely remain at 0.
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        // Later, the App finally drops the frame.
        trackedImage.decrementExternalUse()

        // Should detect the image is already closed and do nothing.
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        fakeImageReader.close()
    }

    @Test
    fun trackedImageUpdatesImageSourceEvictableFlow() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, fakeImageFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        // Initial state
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(0)

        // 1. Create the image -> Should become evictable
        val trackedImage = createTrackedImage(imageSource, fakeImage, memoryEstimator)
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(1)

        // 2. App acquires it -> Should no longer be evictable
        trackedImage.incrementExternalUse()
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(0)

        // 3. App drops it -> Should be evictable again
        trackedImage.decrementExternalUse()
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(1)

        // 4. Image is closed -> Should be removed from evictable count
        trackedImage.close()
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(0)

        fakeImageReader.close()
    }

    @Test
    fun zeroByteImageDoesNotAffectMemoryEstimatorButUpdatesEvictableFlow() {
        val memoryEstimator = MemoryEstimator.create(initialCapacity)

        // UNKNOWN format typically evaluates to 0 bytes
        val unknownFormat = StreamFormat.UNKNOWN
        val fakeImage =
            FakeImage(fakeImageSize.width, fakeImageSize.height, unknownFormat.value, 1234L)
        val (fakeImageReader, imageSource) = createTestImageSource(memoryEstimator)

        val trackedImage =
            TrackedOutputImage(
                imageReaderImageSource = imageSource,
                image = fakeImage,
                streamId = expectedStreamId,
                outputId = expectedOutputId,
                memoryEstimator = memoryEstimator,
            )

        // 1. Memory estimator must be completely untouched because bytes is 0.
        assertThat(memoryEstimator.memoryUsage.value).isEqualTo(0L)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        // 2. However, the ImageSource must still track it as an evictable physical slot.
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(1)

        trackedImage.incrementExternalUse()
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(0)

        trackedImage.close()
        assertThat(imageSource.evictableImageCountFlow.value).isEqualTo(0)

        fakeImageReader.close()
    }

    @Test
    fun addExternalUseUpdatesEvictableMemoryCorrectly() {
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

        // Baseline: Evictable
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        // Add multiple uses at once
        trackedImage.addExternalUse(3)
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        // Decrement one by one, it should remain non-evictable until the last one
        trackedImage.decrementExternalUse()
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        trackedImage.decrementExternalUse()
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(0L)

        trackedImage.decrementExternalUse()
        // Finally hits 0 uses, becomes evictable again
        assertThat(memoryEstimator.evictableMemory.value).isEqualTo(expectedBytes)

        fakeImageReader.close()
    }

    @Test
    fun trackedOutputImageStateBitwiseMathIsCorrect() {
        // 1. Initial State
        var state = TrackedOutputImageState(0)
        assertThat(state.isEvictable).isTrue()
        assertThat(state.isClosed).isFalse()
        assertThat(state.externalUseCount).isEqualTo(0)

        // 2. Incrementing shifts the use count but keeps closed = false
        state = state.withIncrementedUse(1)
        assertThat(state.isEvictable).isFalse()
        assertThat(state.isClosed).isFalse()
        assertThat(state.externalUseCount).isEqualTo(1)

        // 3. Adding multiple uses works
        state = state.withIncrementedUse(4)
        assertThat(state.externalUseCount).isEqualTo(5)
        assertThat(state.isClosed).isFalse()

        // 4. Closing the state sets the 0th bit, but preserves the use count
        state = state.withClosed()
        assertThat(state.isClosed).isTrue()
        assertThat(state.externalUseCount).isEqualTo(5)

        // 5. Decrementing preserves the closed bit
        state = state.withDecrementedUse()
        assertThat(state.externalUseCount).isEqualTo(4)
        assertThat(state.isClosed).isTrue() // Must still be true!
    }
}
