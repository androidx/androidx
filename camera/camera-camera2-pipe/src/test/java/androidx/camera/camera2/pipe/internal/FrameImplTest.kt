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

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame.Companion.isFrameInfoAvailable
import androidx.camera.camera2.pipe.Frame.Companion.isImageAvailable
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [FrameImpl] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FrameImplTest {
    private val stream1Id = StreamId(1)
    private val stream2Id = StreamId(2)

    private val output1Id = OutputId(10)
    private val output2Id = OutputId(12)
    private val output3Id = OutputId(13)

    private val fakeSurfaces = FakeSurfaces()
    private val stream1Surface = fakeSurfaces.createFakeSurface(Size(640, 480))
    private val stream2Surface = fakeSurfaces.createFakeSurface(Size(640, 480))
    private val streamToSurfaceMap = mapOf(stream1Id to stream1Surface, stream2Id to stream2Surface)

    private val frameNumber = FrameNumber(420)
    private val frameTimestampNs = 1234L
    private val frameTimestamp = CameraTimestamp(frameTimestampNs)

    private val imageStreams = setOf(stream1Id, stream2Id)
    private val request = Request(streams = listOf(stream1Id, stream2Id))
    private val fakeRequestMetadata =
        FakeRequestMetadata.from(request, streamToSurfaceMap, repeating = false)

    private val frameState =
        FrameState(
            requestMetadata = fakeRequestMetadata,
            frameNumber = frameNumber,
            frameTimestamp = frameTimestamp,
            imageStreams
        )

    private val frameInfoResult = frameState.frameInfoOutput
    private val streamResult1 = frameState.imageOutputs.first { it.streamId == stream1Id }
    private val streamResult2 = frameState.imageOutputs.first { it.streamId == stream2Id }

    private val stream1Image = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)
    private val stream2Image = FakeImage(640, 480, StreamFormat.YUV_420_888.value, frameTimestampNs)

    private val stream1OutputImage = OutputImage.from(stream1Id, output1Id, stream1Image)
    private val stream2OutputImage = OutputImage.from(stream2Id, output3Id, stream2Image)
    private val fakeFrameMetadata = FakeFrameMetadata(frameNumber = frameNumber)
    private val fakeFrameInfo = FakeFrameInfo(metadata = fakeFrameMetadata)

    private val sharedOutputFrame = FrameImpl(frameState)

    @Test
    fun sharedOutputFrameHasResults() {
        assertThat(sharedOutputFrame.frameNumber).isEqualTo(frameNumber)
        assertThat(sharedOutputFrame.frameTimestamp).isEqualTo(frameTimestamp)
        assertThat(sharedOutputFrame.imageStreams).containsExactly(stream1Id, stream2Id)
    }

    @Test
    fun closingSharedOutputFrameCompletesImageResults() {
        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.frameInfoStatus).isEqualTo(OutputStatus.UNAVAILABLE)

        assertThat(streamResult1.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(streamResult2.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frameInfoResult.status).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun completedOutputsAreAvailableFromFrame() {
        distributeAllOutputs()

        assertThat(sharedOutputFrame.isFrameInfoAvailable).isTrue()
        assertThat(sharedOutputFrame.getFrameInfo()).isSameInstanceAs(fakeFrameInfo)

        assertThat(sharedOutputFrame.isImageAvailable(stream1Id)).isTrue()
        val outputImage1 = sharedOutputFrame.getImage(stream1Id)!!
        assertThat(outputImage1.streamId).isEqualTo(stream1Id)
        assertThat(outputImage1.outputId).isEqualTo(output1Id)

        assertThat(sharedOutputFrame.isImageAvailable(stream2Id)).isTrue()
        val outputImage2 = sharedOutputFrame.getImage(stream2Id)!!
        assertThat(outputImage2.streamId).isEqualTo(stream2Id)
        assertThat(outputImage2.outputId).isEqualTo(output3Id)

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()

        sharedOutputFrame.close()

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()

        outputImage1.close()
        outputImage2.close()

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun outputsCompletedAfterSharedOutputFrameIsClosedAreAlsoClosed() {
        sharedOutputFrame.close()
        distributeAllOutputs()

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun completedOutputsAreClosedAfterFrameIsClosed() {
        distributeAllOutputs()
        sharedOutputFrame.close()

        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun outputsAcquiredBeforeClosedAreNotClosedImmediately() {
        distributeAllOutputs()

        val output11 = sharedOutputFrame.getImage(stream1Id)!!
        val output12 = sharedOutputFrame.getImage(stream1Id)!!
        val output13 = sharedOutputFrame.getImage(stream1Id)!!

        val output21 = sharedOutputFrame.getImage(stream2Id)!!

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
        val stream1OutputImage = frame2.getImage(stream1Id)!!
        assertThat(stream1OutputImage.streamId).isEqualTo(stream1Id)
        assertThat(stream1OutputImage.outputId).isEqualTo(output1Id)

        assertThat(frame2.isImageAvailable(stream2Id)).isTrue()
        val stream2OutputImage = frame2.getImage(stream2Id)!!
        assertThat(stream2OutputImage.streamId).isEqualTo(stream2Id)
        assertThat(stream2OutputImage.outputId).isEqualTo(output3Id)

        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()
    }

    @Test
    fun closingForkedFramesAndOutputsClosesAllOutputs() {
        val frame2 = sharedOutputFrame.tryAcquire()!!
        val frame3 = sharedOutputFrame.tryAcquire()!!
        distributeAllOutputs()

        // Acquire a few outputs from various frames.
        val f1Output = sharedOutputFrame.getImage(stream1Id)!!
        val f2Output = frame2.getImage(stream1Id)!!
        val f3Output = frame3.getImage(stream1Id)!!

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

        distributeAllOutputs()

        assertThat(frame2.imageStreams).containsExactly(stream1Id)
        assertThat(frame3.imageStreams).containsExactly(stream2Id)
        assertThat(frame4.imageStreams).isEmpty()

        assertThat(frame2.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame3.isImageAvailable(stream2Id)).isTrue()

        sharedOutputFrame.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isFalse()

        frame3.close()
        assertThat(stream1Image.isClosed).isFalse()
        assertThat(stream2Image.isClosed).isTrue()

        frame2.close()
        assertThat(stream1Image.isClosed).isTrue()
        assertThat(stream2Image.isClosed).isTrue()
    }

    @Test
    fun accessingFrameAfterCloseReturnsNull() {
        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()
        assertThat(sharedOutputFrame.getImage(stream2Id)).isNull()
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
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED)
        )

        assertThat(sharedOutputFrame.imageStatus(stream1Id))
            .isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()

        sharedOutputFrame.close()

        assertThat(sharedOutputFrame.imageStatus(stream1Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(sharedOutputFrame.getImage(stream1Id)).isNull()
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
            OutputResult.from(stream1OutputImage)
        )

        // Complete streamResult2 with stream2Output3Image
        streamResult2.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameTimestamp.value,
            OutputResult.from(stream2OutputImage)
        )

        // Complete frameInfoResult
        frameInfoResult.onOutputComplete(
            frameNumber,
            frameTimestamp,
            42,
            frameNumber.value,
            OutputResult.from(fakeFrameInfo)
        )
    }
}
