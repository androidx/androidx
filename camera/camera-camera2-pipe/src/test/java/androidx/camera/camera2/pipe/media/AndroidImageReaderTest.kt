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
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.Api37Compat
import androidx.camera.common.CameraId
import androidx.camera.common.ImageWrapper
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
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

    @Test
    @Config(minSdk = 31)
    fun multiResolutionOnImageAvailable_concurrentOutputsEnabled_doesNotFireExpectedOutputs() {
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
                concurrentOutputsEnabled = true,
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
        verifyNoInteractions(mockExpectedOutputsListener)
    }

    @Test
    @Config(minSdk = 31)
    fun multiResolutionOnActiveOutputSurfaces_singleAndMultipleSurfaces_firesExpectedOutputs() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockSurface1 = mock<Surface>()
        val mockSurface2 = mock<Surface>()
        val outputId2 = OutputId(43)

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = emptyMap(),
                surfaceToOutputIdMap = mapOf(mockSurface1 to outputId, mockSurface2 to outputId2),
                concurrentOutputsEnabled = true,
            )

        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        multiImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        multiImageReader.onActiveOutputSurfaces(listOf(mockSurface1), 1000L, 1L)
        verify(mockExpectedOutputsListener).onExpectedOutputs(1000L, setOf(outputId))

        multiImageReader.onActiveOutputSurfaces(listOf(mockSurface1, mockSurface2), 2000L, 2L)
        verify(mockExpectedOutputsListener).onExpectedOutputs(2000L, setOf(outputId, outputId2))
    }

    @Test
    @Config(minSdk = 31)
    fun multiResolutionOnActiveOutputSurfaces_unrecognizedSurface_throws() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockSurface = mock<Surface>()

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = emptyMap(),
                surfaceToOutputIdMap = emptyMap(),
                concurrentOutputsEnabled = true,
            )

        assertThrows<IllegalStateException> {
            multiImageReader.onActiveOutputSurfaces(listOf(mockSurface), 1000L, 1L)
        }
    }

    @Test
    @Config(minSdk = 31, maxSdk = 36)
    fun multiResolutionCreate_concurrentOutputsBelowApi37_throws() {
        val output1 =
            mock<OutputStream>().apply {
                whenever(id).thenReturn(outputId)
                whenever(size).thenReturn(Size(640, 480))
                whenever(camera).thenReturn(CameraId("0"))
            }
        val output2 =
            mock<OutputStream>().apply {
                whenever(id).thenReturn(OutputId(43))
                whenever(size).thenReturn(Size(1280, 720))
                whenever(camera).thenReturn(CameraId("1"))
            }

        assertThrows<IllegalArgumentException> {
            AndroidMultiResolutionImageReader.create(
                outputFormat = StreamFormat.YUV_420_888.value,
                streamId = streamId,
                outputs = listOf(output1, output2),
                capacity = 10,
                executor = { it.run() },
                usageFlags = null,
                enableConcurrentOutputs = true,
            )
        }
    }

    @Test
    @Config(minSdk = 37)
    fun setOnActiveOutputSurfacesListener_api37_delegatesAndForwardsActiveSurfaces() {
        val mockMultiReader = mock<MultiResolutionImageReader>()
        val mockSurface1 = mock<Surface>()
        val mockSurface2 = mock<Surface>()
        val outputId2 = OutputId(43)
        val executor = Executor { it.run() }

        val multiImageReader =
            AndroidMultiResolutionImageReader(
                multiResolutionImageReader = mockMultiReader,
                streamFormat = StreamFormat(1),
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputConfigurations = emptyList(),
                streamInfoToOutputIdMap = emptyMap(),
                surfaceToOutputIdMap = mapOf(mockSurface1 to outputId, mockSurface2 to outputId2),
                concurrentOutputsEnabled = true,
            )
        val mockExpectedOutputsListener = mock<ImageReaderWrapper.OnExpectedOutputsListener>()
        multiImageReader.onExpectedOutputsListener = mockExpectedOutputsListener

        Api37Compat.setOnActiveOutputSurfacesListener(
            mockMultiReader,
            executor,
            multiImageReader,
        )

        val listenerCaptor =
            argumentCaptor<MultiResolutionImageReader.OnActiveOutputSurfacesListener>()
        verify(mockMultiReader)
            .setOnActiveOutputSurfacesListener(eq(executor), listenerCaptor.capture())

        listenerCaptor.firstValue.onActiveOutputSurfaces(
            listOf(mockSurface1, mockSurface2),
            3000L,
            5L,
        )
        verify(mockExpectedOutputsListener).onExpectedOutputs(3000L, setOf(outputId, outputId2))
    }
}
