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

import android.hardware.camera2.MultiResolutionImageReader
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class AndroidImageReaderTest {
    private val streamId = StreamId(32)
    private val outputId = OutputId(42)

    @Test
    fun onImageAvailable_succeedsAfterFix() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        val timestamp = 12345L
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)
        whenever(mockImage.timestamp).thenReturn(timestamp)

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        androidImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                var imageReceived: ImageWrapper? = null

                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    imageReceived = image
                }
            }
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)

        assertThat(testListener.imageReceived).isNotNull()
        verify(mockExpectedOutputsListener).onExpectedOutputs(timestamp, setOf(outputId))
    }

    @Test
    fun onImageAvailable_handlesClosedImage() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)

        // Simulate closed image by throwing IllegalStateException on timestamp access
        whenever(mockImage.timestamp).thenThrow(IllegalStateException("Image is already closed"))

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        androidImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                var imageReceived: ImageWrapper? = null

                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    imageReceived = image
                }
            }
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)

        assertThat(testListener.imageReceived).isNull()
        verifyNoInteractions(mockExpectedOutputsListener)
        verify(mockImage).close()
    }

    @Test
    fun onImageAvailable_handlesIllegalStateExceptionOnWrapAndClosesImageAndDoesNotFireExpectedOutputs() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        val timestamp = 12345L
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)
        whenever(mockImage.timestamp).thenReturn(timestamp)
        // Simulate closed image during wrapping (e.g. format access)
        whenever(mockImage.format).thenThrow(IllegalStateException("Image is already closed"))

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        androidImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        val testListener = mock<ImageReaderWrapper.OnImageListener>()
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)

        verify(mockImage).close()
        verifyNoInteractions(mockExpectedOutputsListener)
        verifyNoInteractions(testListener)
    }

    @Test
    fun onImageAvailable_closesImageWhenListenerIsNull() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        androidImageReader.onImageAvailable(mockImageReader)

        verify(mockImage).close()
    }

    @Test
    fun onImageAvailable_handlesIllegalStateExceptionOnAcquire() {
        val mockImageReader = mock<ImageReader>()
        val mockSurface = mock<Surface>()
        whenever(mockImageReader.surface).thenReturn(mockSurface)
        whenever(mockImageReader.acquireNextImage()).thenThrow(IllegalStateException("Closed"))

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val testListener = mock<ImageReaderWrapper.OnImageListener>()
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)

        verifyNoInteractions(testListener)
    }

    @Test(expected = RuntimeException::class)
    fun onImageAvailable_doesNotCatchListenerExceptions() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        val timestamp = 12345L
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)
        whenever(mockImage.timestamp).thenReturn(timestamp)

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    throw RuntimeException("Listener error")
                }
            }
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)
    }

    @Test
    @Config(minSdk = 31)
    fun multiResolutionOnImageAvailable_handlesClosedImage() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()

        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)

        val mockStreamInfo = mock<MultiResolutionStreamInfo>()
        whenever(mockMultiReader.getStreamInfoForImageReader(mockImageReader))
            .thenReturn(mockStreamInfo)

        val streamInfoMap = mapOf(mockStreamInfo to outputId)

        // Simulate closed image by throwing IllegalStateException on timestamp access
        whenever(mockImage.timestamp).thenThrow(IllegalStateException("Image is already closed"))

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = streamInfoMap,
                surfaceToOutputIdMap = emptyMap(),
                concurrentOutputsEnabled = false,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        multiImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                var imageReceived: ImageWrapper? = null

                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    imageReceived = image
                }
            }
        multiImageReader.onImageListener = testListener

        multiImageReader.onImageAvailable(mockImageReader)

        assertThat(testListener.imageReceived).isNull()
        verifyNoInteractions(mockExpectedOutputsListener)
        verify(mockImage).close()
    }

    @Test
    @Config(minSdk = 31)
    fun multiResolutionOnImageAvailable_firesExpectedOutputs_onSuccess() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val timestamp = 12345L

        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImage.timestamp).thenReturn(timestamp)

        val mockStreamInfo = mock<MultiResolutionStreamInfo>()
        whenever(mockMultiReader.getStreamInfoForImageReader(mockImageReader))
            .thenReturn(mockStreamInfo)

        val streamInfoMap = mapOf(mockStreamInfo to outputId)

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = streamInfoMap,
                surfaceToOutputIdMap = emptyMap(),
                concurrentOutputsEnabled = false,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        multiImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                var imageReceived: ImageWrapper? = null

                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    imageReceived = image
                }
            }
        multiImageReader.onImageListener = testListener

        multiImageReader.onImageAvailable(mockImageReader)

        assertThat(testListener.imageReceived).isNotNull()
        verify(mockExpectedOutputsListener).onExpectedOutputs(timestamp, setOf(outputId))
    }

    @Test(expected = IllegalStateException::class)
    @Config(minSdk = 31)
    fun multiResolutionOnImageAvailable_throws_ifMappingMissing() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val timestamp = 12345L

        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImage.timestamp).thenReturn(timestamp)

        val mockStreamInfo = mock<MultiResolutionStreamInfo>()
        whenever(mockMultiReader.getStreamInfoForImageReader(mockImageReader))
            .thenReturn(mockStreamInfo)

        val streamInfoMap = emptyMap<MultiResolutionStreamInfo, OutputId>()

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = streamInfoMap,
                surfaceToOutputIdMap = emptyMap(),
                concurrentOutputsEnabled = false,
            )

        val testListener = mock<ImageReaderWrapper.OnImageListener>()
        multiImageReader.onImageListener = testListener

        multiImageReader.onImageAvailable(mockImageReader)
    }
}
