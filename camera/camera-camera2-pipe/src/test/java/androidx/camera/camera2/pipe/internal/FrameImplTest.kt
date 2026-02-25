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

package androidx.camera.camera2.pipe.internal

import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame.Companion.isFrameInfoAvailable
import androidx.camera.camera2.pipe.Frame.Companion.isImageAvailable
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [FrameImpl] */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameImplTest {
    private val stream1Id = StreamId(1)
    private val stream2Id = StreamId(2)
    private val stream3Id = StreamId(3)
    private val stream4Id = StreamId(4)

    private val output1Id = OutputId(10)
    private val output2Id = OutputId(12)
    private val output3Id = OutputId(13)
    private val output4Id = OutputId(14)
    private val output5Id = OutputId(15)
    private val output6Id = OutputId(16)
    private val output7Id = OutputId(17)
    private val output8Id = OutputId(18)
    private val output9Id = OutputId(19)

    private val cameraId = CameraId("0")

    private val outputStream1 =
        StreamGraphImpl.OutputStreamImpl(
            output1Id,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
        )
    private val outputStream3 =
        StreamGraphImpl.OutputStreamImpl(
            output3Id,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
        )
    private val outputStream4 =
        StreamGraphImpl.OutputStreamImpl(
            output4Id,
            Size(640, 480),
            StreamFormat.RAW_SENSOR,
            cameraId,
        )
    private val outputStream5 =
        StreamGraphImpl.OutputStreamImpl(
            output5Id,
            Size(720, 480),
            StreamFormat.RAW_SENSOR,
            cameraId,
        )
    private val outputStream6 =
        StreamGraphImpl.OutputStreamImpl(
            output6Id,
            Size(1600, 900),
            StreamFormat.RAW_SENSOR,
            cameraId,
        )
    private val outputStream7 =
        StreamGraphImpl.OutputStreamImpl(output7Id, Size(1280, 720), StreamFormat.RAW12, cameraId)
    private val outputStream8 =
        StreamGraphImpl.OutputStreamImpl(output8Id, Size(1280, 800), StreamFormat.RAW12, cameraId)
    private val outputStream9 =
        StreamGraphImpl.OutputStreamImpl(output9Id, Size(1024, 768), StreamFormat.RAW12, cameraId)
    private val stream1 =
        CameraStream(stream1Id, listOf(outputStream1)).apply { outputStream1.stream = this }
    private val stream2 =
        CameraStream(stream2Id, listOf(outputStream3)).apply { outputStream3.stream = this }
    private val stream3 =
        CameraStream(stream3Id, listOf(outputStream4, outputStream5, outputStream6)).apply {
            outputStream4.stream = this
            outputStream5.stream = this
            outputStream6.stream = this
        }
    private val stream4 =
        CameraStream(stream4Id, listOf(outputStream7, outputStream8, outputStream9)).apply {
            outputStream7.stream = this
            outputStream8.stream = this
            outputStream9.stream = this
        }

    private val fakeSurfaces = FakeSurfaces()
    private val stream1Surface = fakeSurfaces.createFakeSurface(Size(640, 480))
    private val stream2Surface = fakeSurfaces.createFakeSurface(Size(640, 480))
    private val stream3Surface = fakeSurfaces.createFakeSurface(Size(640, 480))
    private val stream4Surface = fakeSurfaces.createFakeSurface(Size(1280, 720))
    private val streamToSurfaceMap =
        mapOf(
            stream1Id to stream1Surface,
            stream2Id to stream2Surface,
            stream3Id to stream3Surface,
            stream4Id to stream4Surface,
        )

    private val frameNumber = FrameNumber(420)
    private val frameTimestampNs = 1234L
    private val frameTimestamp = CameraTimestamp(frameTimestampNs)

    private val imageStreams = setOf(stream1, stream2, stream3, stream4)
    private val request = Request(streams = listOf(stream1Id, stream2Id, stream3Id, stream4Id))
    private val fakeRequestMetadata =
        FakeRequestMetadata.from(request, streamToSurfaceMap, repeating = false)

    private val frameState =
        FrameState(
            requestMetadata = fakeRequestMetadata,
            frameNumber = frameNumber,
            frameTimestamp = frameTimestamp,
            imageStreams = imageStreams,
            concurrentImageStreams = setOf(stream4Id),
        )

    private val frameInfoResult = frameState.frameInfoOutput
    private val streamResult1 = frameState.imageOutputs.first { it.streamId == stream1Id }
    private val streamResult2 = frameState.imageOutputs.first { it.streamId == stream2Id }
    private val streamResult3 =
        frameState.imageOutputs.filter { it.streamId == stream3Id }.sortedBy { it.outputId.value }
    private val streamResult4 =
        frameState.imageOutputs.filter { it.streamId == stream4Id }.sortedBy { it.outputId.value }

    private val stream1Image = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)
    private val stream2Image = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)
    private val stream3Image1 = FakeImage(640, 480, StreamFormat.RAW_SENSOR.value, frameTimestampNs)
    private val stream4Image1 = FakeImage(1280, 720, StreamFormat.RAW12.value, frameTimestampNs)
    private val stream4Image2 = FakeImage(1280, 800, StreamFormat.RAW12.value, frameTimestampNs)

    private val stream1OutputImage = OutputImage.from(stream1Id, output1Id, stream1Image)
    private val stream2OutputImage = OutputImage.from(stream2Id, output3Id, stream2Image)
    private val stream3OutputImage1 = OutputImage.from(stream3Id, output4Id, stream3Image1)
    private val stream4OutputImage1 = OutputImage.from(stream4Id, output7Id, stream4Image1)
    private val stream4OutputImage2 = OutputImage.from(stream4Id, output8Id, stream4Image2)

    private val fakeFrameMetadata = FakeFrameMetadata(frameNumber = frameNumber)
    private val fakeFrameInfo = FakeFrameInfo(metadata = fakeFrameMetadata)

    private val sharedOutputFrame = FrameImpl(frameState)

    @After
    fun tearDown() {
        fakeSurfaces.close()
        sharedOutputFrame.close()
    }

    @Test
    fun sharedOutputFrameHasResults() {
        assertThat(sharedOutputFrame.frameNumber).isEqualTo(frameNumber)
        assertThat(sharedOutputFrame.frameTimestamp).isEqualTo(frameTimestamp)
        assertThat(sharedOutputFrame.imageStreams)
            .containsExactly(stream1Id, stream2Id, stream3Id, stream4Id)
    }

    @Test
    fun closingSharedOutputFrameCompletesImageResults() {
        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.frameInfoStatus).isEqualTo(OutputStatus.UNAVAILABLE)

        assertThat(streamResult1.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(streamResult2.status).isEqualTo(OutputStatus.UNAVAILABLE)
        streamResult3.forEach { assertThat(it.status).isEqualTo(OutputStatus.UNAVAILABLE) }
        streamResult4.forEach { assertThat(it.status).isEqualTo(OutputStatus.UNAVAILABLE) }
        assertThat(frameInfoResult.status).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun completedOutputsAreAvailableFromFrame() {
        distributeAllOutputs()

        assertThat(sharedOutputFrame.isFrameInfoAvailable).isTrue()
        assertThat(sharedOutputFrame.getFrameInfo()).isSameInstanceAs(fakeFrameInfo)

        assertThat(sharedOutputFrame.isImageAvailable(stream1Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output1Id)).isTrue()
        val outputImage1 = sharedOutputFrame.getImage(stream1Id)!!
        assertThat(outputImage1.streamId).isEqualTo(stream1Id)
        assertThat(outputImage1.outputId).isEqualTo(output1Id)
        val outputImages1 = sharedOutputFrame.getImages(stream1Id)
        assertThat(outputImages1.size).isEqualTo(1)
        assertThat(outputImages1[0].streamId).isEqualTo(stream1Id)
        assertThat(outputImages1[0].outputId).isEqualTo(output1Id)

        assertThat(sharedOutputFrame.isImageAvailable(stream2Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output3Id)).isTrue()
        val outputImage2 = sharedOutputFrame.getImage(stream2Id)!!
        assertThat(outputImage2.streamId).isEqualTo(stream2Id)
        assertThat(outputImage2.outputId).isEqualTo(output3Id)
        val outputImages2 = sharedOutputFrame.getImages(stream2Id)
        assertThat(outputImages2.size).isEqualTo(1)
        assertThat(outputImages2[0].streamId).isEqualTo(stream2Id)
        assertThat(outputImages2[0].outputId).isEqualTo(output3Id)

        assertThat(sharedOutputFrame.isImageAvailable(stream3Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output4Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output5Id)).isFalse()
        assertThat(sharedOutputFrame.isImageAvailable(output6Id)).isFalse()
        val outputImage3 = sharedOutputFrame.getImage(stream3Id)!!
        assertThat(outputImage3.streamId).isEqualTo(stream3Id)
        assertThat(outputImage3.outputId).isEqualTo(output4Id)
        val outputImages3 = sharedOutputFrame.getImages(stream3Id)
        assertThat(outputImages3.size).isEqualTo(1)
        assertThat(outputImages3[0].streamId).isEqualTo(stream3Id)
        assertThat(outputImages3[0].outputId).isEqualTo(output4Id)

        assertThat(sharedOutputFrame.isImageAvailable(stream4Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output7Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output8Id)).isTrue()
        assertThat(sharedOutputFrame.isImageAvailable(output9Id)).isFalse()
        val outputImages4 = sharedOutputFrame.getImages(stream4Id).sortedBy { it.outputId.value }
        assertThat(outputImages4.size).isEqualTo(2)
        assertThat(outputImages4[0].streamId).isEqualTo(stream4Id)
        assertThat(outputImages4[0].outputId).isEqualTo(output7Id)
        assertThat(outputImages4[1].streamId).isEqualTo(stream4Id)
        assertThat(outputImages4[1].outputId).isEqualTo(output8Id)

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()
        assertThat(stream3Image1.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        sharedOutputFrame.close()

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()
        assertThat(stream3Image1.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        outputImage1.close()
        outputImages1.forEach { it.close() }
        outputImage2.close()
        outputImages2.forEach { it.close() }
        outputImage3.close()
        outputImages3.forEach { it.close() }
        outputImages4.forEach { it.close() }

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isTrue()
        assertThat(stream4Image1.isClosed).isTrue()
        assertThat(stream4Image2.isClosed).isTrue()
    }

    @Test
    fun outputsCompletedAfterSharedOutputFrameIsClosedAreAlsoClosed() {
        sharedOutputFrame.close()
        distributeAllOutputs()

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isTrue()
        assertThat(stream4Image1.isClosed).isTrue()
        assertThat(stream4Image2.isClosed).isTrue()
    }

    @Test
    fun completedOutputsAreClosedAfterFrameIsClosed() {
        distributeAllOutputs()
        sharedOutputFrame.close()

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isTrue()
        assertThat(stream4Image1.isClosed).isTrue()
        assertThat(stream4Image2.isClosed).isTrue()
    }

    @Test
    fun getImageFromConcurrentStreamsShouldThrow() {
        distributeAllOutputs()

        assertThrows<IllegalStateException> { sharedOutputFrame.getImage(stream4Id) }
    }

    @Test
    fun outputsAcquiredBeforeClosedAreNotClosedImmediately() {
        distributeAllOutputs()

        val output11 = sharedOutputFrame.getImages(stream1Id).single()
        val output12 = sharedOutputFrame.getImages(stream1Id).single()
        val output13 = sharedOutputFrame.getImages(stream1Id).single()

        val output21 = sharedOutputFrame.getImages(stream2Id).single()

        sharedOutputFrame.close()

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()

        // Once we close the specific output, the underlying image should be closed:
        output21.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isTrue()

        // Close some, but not all images:
        output11.close()
        output12.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isTrue()

        // Close final image
        output13.close()
        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun sharedOutputFramesCanBeForkedBeforeCompleted() {
        val frame2 = sharedOutputFrame.tryAcquire()!!
        sharedOutputFrame.close()

        // Distribute outputs after the initial frame is closed.
        distributeAllOutputs()

        // Assert outputs are not closed, and that outputs are still available.
        assertThat(frame2.isFrameInfoAvailable).isTrue()
        assertThat(frame2.getFrameInfo()).isSameInstanceAs(fakeFrameInfo)

        assertThat(frame2.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame2.isImageAvailable(output1Id)).isTrue()
        val stream1OutputImage = frame2.getImage(stream1Id)!!
        assertThat(stream1OutputImage.streamId).isEqualTo(stream1Id)
        assertThat(stream1OutputImage.outputId).isEqualTo(output1Id)
        val stream1OutputImages = frame2.getImages(stream1Id)
        assertThat(stream1OutputImages.size).isEqualTo(1)
        assertThat(stream1OutputImages[0].streamId).isEqualTo(stream1Id)
        assertThat(stream1OutputImages[0].outputId).isEqualTo(output1Id)

        assertThat(frame2.isImageAvailable(stream2Id)).isTrue()
        assertThat(frame2.isImageAvailable(output3Id)).isTrue()
        val stream2OutputImage = frame2.getImage(stream2Id)!!
        assertThat(stream2OutputImage.streamId).isEqualTo(stream2Id)
        assertThat(stream2OutputImage.outputId).isEqualTo(output3Id)
        val stream2OutputImages = frame2.getImages(stream2Id)
        assertThat(stream2OutputImages.size).isEqualTo(1)
        assertThat(stream2OutputImages[0].streamId).isEqualTo(stream2Id)
        assertThat(stream2OutputImages[0].outputId).isEqualTo(output3Id)

        assertThat(frame2.isImageAvailable(stream3Id)).isTrue()
        assertThat(frame2.isImageAvailable(output4Id)).isTrue()
        assertThat(frame2.isImageAvailable(output5Id)).isFalse()
        assertThat(frame2.isImageAvailable(output6Id)).isFalse()
        val stream3OutputImage = frame2.getImage(stream3Id)!!
        assertThat(stream3OutputImage.streamId).isEqualTo(stream3Id)
        assertThat(stream3OutputImage.outputId).isEqualTo(output4Id)
        val stream3OutputImages = frame2.getImages(stream3Id)
        assertThat(stream3OutputImages.size).isEqualTo(1)
        assertThat(stream3OutputImages[0].streamId).isEqualTo(stream3Id)
        assertThat(stream3OutputImages[0].outputId).isEqualTo(output4Id)

        assertThat(frame2.isImageAvailable(stream4Id)).isTrue()
        assertThat(frame2.isImageAvailable(output7Id)).isTrue()
        assertThat(frame2.isImageAvailable(output8Id)).isTrue()
        assertThat(frame2.isImageAvailable(output9Id)).isFalse()
        val stream4OutputImages = frame2.getImages(stream4Id).sortedBy { it.outputId.value }
        assertThat(stream4OutputImages.size).isEqualTo(2)
        assertThat(stream4OutputImages[0].streamId).isEqualTo(stream4Id)
        assertThat(stream4OutputImages[0].outputId).isEqualTo(output7Id)
        assertThat(stream4OutputImages[1].streamId).isEqualTo(stream4Id)
        assertThat(stream4OutputImages[1].outputId).isEqualTo(output8Id)

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()
    }

    @Test
    fun closingForkedFramesAndOutputsClosesAllOutputs() {
        val frame2 = sharedOutputFrame.tryAcquire()!!
        val frame3 = sharedOutputFrame.tryAcquire()!!
        distributeAllOutputs()

        // Acquire a few outputs from various frames.
        val f1Output = sharedOutputFrame.getImages(stream1Id).single()
        val f2Output = frame2.getImages(stream1Id).single()
        val f3Output = frame3.getImages(stream1Id).single()

        // Close all the frames
        sharedOutputFrame.close()
        frame2.close()
        frame3.close()

        // Stream1 image is not closed (3 outstanding image references)
        assertThat(stream1Image.isClosed).isFalse()
        // Stream2 image is now closed (0 outstanding image references, 0 frame references)
        assertThat(stream2Image.isClosed).isTrue()

        // Close 2/3 of the outputs
        f1Output.close()
        f3Output.close()
        assertThat(stream1Image.isClosed).isFalse()

        // Close last output
        f2Output.close()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun sharedFramesCanBeForkedWithASubsetOfOutputs() {
        val frame2 = sharedOutputFrame.tryAcquire(setOf(stream1Id))!!
        val frame3 = sharedOutputFrame.tryAcquire(setOf(stream2Id))!!
        val frame4 = sharedOutputFrame.tryAcquire(setOf(StreamId(42)))!! // Unsupported Stream
        val frame5 = sharedOutputFrame.tryAcquire(setOf(stream3Id))!!
        val frame6 = sharedOutputFrame.tryAcquire(setOf(stream4Id))!!

        distributeAllOutputs()

        assertThat(frame2.imageStreams).containsExactly(stream1Id)
        assertThat(frame3.imageStreams).containsExactly(stream2Id)
        assertThat(frame4.imageStreams).isEmpty()
        assertThat(frame5.imageStreams).containsExactly(stream3Id)
        assertThat(frame6.imageStreams).containsExactly(stream4Id)

        assertThat(frame2.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame2.isImageAvailable(output1Id)).isTrue()

        assertThat(frame3.isImageAvailable(stream2Id)).isTrue()
        assertThat(frame2.isImageAvailable(output3Id)).isTrue()

        assertThat(frame5.isImageAvailable(stream3Id)).isTrue()
        assertThat(frame2.isImageAvailable(output4Id)).isTrue()
        assertThat(frame2.isImageAvailable(output5Id)).isFalse()
        assertThat(frame2.isImageAvailable(output6Id)).isFalse()

        assertThat(frame6.isImageAvailable(stream4Id)).isTrue()
        assertThat(frame2.isImageAvailable(output7Id)).isTrue()
        assertThat(frame2.isImageAvailable(output8Id)).isTrue()
        assertThat(frame2.isImageAvailable(output9Id)).isFalse()

        sharedOutputFrame.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()
        assertThat(stream3Image1.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        frame3.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        frame2.close()
        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isFalse()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        frame5.close()
        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isTrue()
        assertThat(stream4Image1.isClosed).isFalse()
        assertThat(stream4Image2.isClosed).isFalse()

        frame6.close()
        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
        assertThat(stream3Image1.isClosed).isTrue()
        assertThat(stream4Image1.isClosed).isTrue()
        assertThat(stream4Image2.isClosed).isTrue()
    }

    @Test
    fun accessingFrameAfterCloseReturnsNoImages() {
        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream1Id)).isEmpty()
        assertThat(sharedOutputFrame.getImage(stream2Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream2Id)).isEmpty()
        assertThat(sharedOutputFrame.getImage(stream3Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream3Id)).isEmpty()
        assertThat(sharedOutputFrame.getImage(stream4Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream4Id)).isEmpty()
        assertThat(sharedOutputFrame.getFrameInfo()).isNull()
    }

    @Test
    fun forkingAClosedFrameReturnsNull() {
        sharedOutputFrame.close()

        val frame = sharedOutputFrame.tryAcquire()
        assertThat(frame).isNull()
    }

    @Test
    fun forkCanOnlyAccessKnownStreams() {
        val stream1Frame = sharedOutputFrame.tryAcquire(setOf(stream1Id))!!
        val stream2Frame = stream1Frame.tryAcquire(setOf(stream2Id))!!
        val allStreamFrame = stream1Frame.tryAcquire(setOf(stream1Id, stream2Id))!!

        assertThat(stream2Frame.imageStreams).isEmpty()
        assertThat(allStreamFrame.imageStreams).containsExactly(stream1Id)
    }

    @Test
    fun frameReportsFailureReason() {
        streamResult1.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED),
        )

        assertThat(sharedOutputFrame.imageStatus(stream1Id))
            .isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream1Id)).isEmpty()

        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.imageStatus(stream1Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()
        assertThat(sharedOutputFrame.getImages(stream1Id)).isEmpty()
    }

    @Test
    fun imageStatusIsAvailableWhenAllOutputsAreAvailable() {
        val stream4Image3 = FakeImage(1024, 768, StreamFormat.RAW12.value, frameTimestampNs)
        val stream4OutputImage3 = OutputImage.from(stream4Id, output9Id, stream4Image3)

        streamResult4[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream4OutputImage1),
        )
        streamResult4[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream4OutputImage2),
        )
        streamResult4[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream4OutputImage3),
        )

        assertThat(sharedOutputFrame.imageStatus(stream4Id)).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun imageStatusAggregatesMultiOutputStatus() {
        // Initially everything is PENDING
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete one output (output4Id)
        streamResult3[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream3OutputImage1),
        )
        // Still pending because others are pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete second output (output5Id) with failure
        streamResult3[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED),
        )
        // Still pending because output6Id is pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete third output (output6Id) with failure
        streamResult3[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.UNAVAILABLE),
        )

        // Now all are done.
        // Statuses: AVAILABLE, ERROR_OUTPUT_DROPPED, UNAVAILABLE.
        // any == AVAILABLE is true.
        // Result should be AVAILABLE.
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun imageStatusAggregatesMultiOutputStatusAsUnavailableWhenAllFail() {
        // Initially everything is PENDING
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete one output (output4Id)
        streamResult3[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_MISSING),
        )
        // Still pending because others are pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete second output (output5Id) with failure
        streamResult3[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED),
        )
        // Still pending because output6Id is pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete third output (output6Id) with failure
        streamResult3[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.UNAVAILABLE),
        )

        // Now all are done.
        // Statuses: ERROR_OUTPUT_MISSING, ERROR_OUTPUT_DROPPED, UNAVAILABLE.
        // any == AVAILABLE is false.
        // Result should be UNAVAILABLE.
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun imageStatusAggregatesMultiOutputStatusWhenAllFailWithSameStatus() {
        // Initially everything is PENDING
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete one output (output4Id)
        streamResult3[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_FAILED),
        )
        // Still pending because others are pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete second output (output5Id) with failure
        streamResult3[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_FAILED),
        )
        // Still pending because output6Id is pending
        assertThat(sharedOutputFrame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)

        // Complete third output (output6Id) with failure
        streamResult3[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_FAILED),
        )

        // Now all are done.
        // Statuses: ERROR_OUTPUT_FAILED, ERROR_OUTPUT_FAILED, ERROR_OUTPUT_FAILED.
        // any == AVAILABLE is false.
        // all == statuses.first().
        // Result should be statuses.first() (ERROR_OUTPUT_FAILED).
        assertThat(sharedOutputFrame.imageStatus(stream3Id))
            .isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @After
    fun cleanup() {
        fakeSurfaces.close()
    }

    private fun distributeAllOutputs() {
        // Complete streamResult1 with stream1Output1Image
        streamResult1.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream1OutputImage),
        )

        // Complete streamResult2 with stream2Output3Image
        streamResult2.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream2OutputImage),
        )

        // Complete streamResult3 with stream3OutputImage1, the other ones unavailable. This is
        // to simulate the case where only one single output image is expected for the stream.
        streamResult3[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream3OutputImage1),
        )
        streamResult3[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.UNAVAILABLE),
        )
        streamResult3[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.UNAVAILABLE),
        )

        // Complete streamResult4 with 2 images available, 1 unavailable.
        streamResult4[0].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream4OutputImage1),
        )
        streamResult4[1].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream4OutputImage2),
        )
        streamResult4[2].onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.failure(OutputStatus.UNAVAILABLE),
        )

        // Complete frameInfoResult
        frameInfoResult.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameNumber.value,
            OutputResult.from(fakeFrameInfo),
        )
    }
}
