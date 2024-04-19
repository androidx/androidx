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
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.Frame.Companion.isFrameInfoAvailable
import androidx.camera.camera2.pipe.Frame.Companion.isImageAvailable
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.FrameReference.Companion.acquire
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.ImageSimulator
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [FrameDistributor] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FrameDistributorTest {

    private val stream1Config =
        CameraStream.Config.create(
            Size(1280, 720),
            StreamFormat.YUV_420_888
        )
    private val stream2Config =
        CameraStream.Config.create(
            Size(1920, 1080), StreamFormat.YUV_420_888
        )
    private val streamConfigs = listOf(stream1Config, stream2Config)

    private val imageSimulator = ImageSimulator(streamConfigs)
    private val stream1Id = imageSimulator.streamGraph[stream1Config]!!.id
    private val stream2Id = imageSimulator.streamGraph[stream2Config]!!.id
    private val streams = listOf(stream1Id, stream2Id)

    private val cameraId = imageSimulator.cameraMetadata.camera
    private val cameraTimestamp = CameraTimestamp(1234L)
    private val cameraFrameNumber = FrameNumber(420)

    private val request = Request(streams = streams)
    private val fakeRequestMetadata = FakeRequestMetadata.from(
        request,
        imageSimulator.streamToSurfaceMap,
        repeating = false
    )
    private val fakeFrameInfo = FakeFrameInfo(
        metadata = FakeFrameMetadata(
            camera = cameraId,
            frameNumber = cameraFrameNumber
        ),
        requestMetadata = fakeRequestMetadata
    )

    private val fakeFrameBuffer = FakeFrameBuffer()
    private val frameCaptureQueue = FrameCaptureQueue()
    private val frameDistributor =
        FrameDistributor(imageSimulator.imageSources, frameCaptureQueue, fakeFrameBuffer)

    @Test
    fun frameDistributorSetupVerification() {
        assertThat(imageSimulator.imageSources.keys).containsExactly(stream1Id, stream2Id)
        assertThat(imageSimulator.streamToSurfaceMap.keys).containsExactly(stream1Id, stream2Id)
    }

    @Test
    fun framesAreAddedToFrameBuffer() {
        frameDistributor.onStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            cameraTimestamp
        )

        assertThat(fakeFrameBuffer.frames.size).isEqualTo(1)

        val frame = fakeFrameBuffer.frames[0]
        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStreams).containsExactly(stream1Id, stream2Id)

        // Closing should cause all outputs to be closed, since this should be the only frame.
        frame.close()

        assertThat(frame.imageStreams).containsExactly(stream1Id, stream2Id)

        assertThat(frame.getFrameInfo()).isEqualTo(null)
        assertThat(frame.getImage(stream1Id)).isEqualTo(null)
        assertThat(frame.getImage(stream2Id)).isEqualTo(null)

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun outputsAreDistributedToFrame() {
        frameDistributor.onStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            cameraTimestamp
        )

        assertThat(fakeFrameBuffer.frames.size).isEqualTo(1)
        val frame = fakeFrameBuffer.frames[0]

        val image1 = imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        assertThat(frame.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame.isImageAvailable(stream2Id)).isFalse()
        assertThat(frame.isFrameInfoAvailable).isFalse()
        assertThat(image1.isClosed).isFalse()

        val image2 = imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)
        assertThat(frame.isImageAvailable(stream2Id)).isTrue()
        assertThat(frame.isFrameInfoAvailable).isFalse()
        assertThat(image2.isClosed).isFalse()

        frameDistributor.onComplete(
            fakeRequestMetadata,
            cameraFrameNumber,
            fakeFrameInfo
        )
        assertThat(frame.isFrameInfoAvailable).isTrue()

        // Now close the frame (without acquiring images)
        frame.close()

        // Assert that the images are closed
        assertThat(image1.isClosed).isTrue()
        assertThat(image2.isClosed).isTrue()
    }

    @Test
    fun onStartedCausesFrameCaptureToBeAvailable() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request) as FrameCapture
        assertThat(frameCapture.status).isEqualTo(OutputStatus.PENDING)

        frameDistributor.onStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            cameraTimestamp
        )

        assertThat(frameCapture.status).isEqualTo(OutputStatus.AVAILABLE)
        val frame = frameCapture.getFrame()
        assertThat(frame).isNotNull()
        frame?.close()
    }

    @Test
    fun abortedRequestsCauseFramesToBeAborted() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onAborted(
            fakeRequestMetadata.request
        )
        assertThat(frameCapture.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(frameCapture.getFrame()).isNull()
    }

    @Test
    fun onFailureCausesFrameInfoToBeLost() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            cameraTimestamp
        )
        val frame = frameCapture.getFrame()!!

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)

        frameDistributor.onFailed(
            fakeRequestMetadata,
            cameraFrameNumber,
            FakeRequestFailure(fakeRequestMetadata, cameraFrameNumber, wasImageCaptured = true)
        )

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)

        // Images are still delivered, even after onFailed
        imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.isImageAvailable(stream1Id)).isEqualTo(true)
        assertThat(frame.isImageAvailable(stream2Id)).isEqualTo(true)
    }

    @Test
    fun onFailureWithImageLossAllOutputsToFail() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            cameraTimestamp
        )
        val frame = frameCapture.getFrame()!!

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)

        frameDistributor.onFailed(
            fakeRequestMetadata,
            cameraFrameNumber,
            FakeRequestFailure(fakeRequestMetadata, cameraFrameNumber, wasImageCaptured = false)
        )

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)

        // Images are still delivered, even after onFailed
        val fakeImage1 = imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        val fakeImage2 = imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)

        assertThat(fakeImage1.isClosed).isTrue()
        assertThat(fakeImage2.isClosed).isTrue()
    }

    @After
    fun cleanup() {
        imageSimulator.close()
    }

    private class FakeFrameBuffer : FrameDistributor.FrameStartedListener,
        AutoCloseable {
        private val lock = Any()
        private var closed = false
        private val _frames = mutableListOf<Frame>()
        val frames: List<Frame>
            get() = synchronized(lock) { _frames.toList() }

        override fun onFrameStarted(frameReference: FrameReference) {
            synchronized(lock) {
                if (!closed) {
                    _frames.add(frameReference.acquire())
                }
            }
        }

        override fun close() {
            val shouldClose: Boolean
            synchronized(lock) {
                shouldClose = !closed
                closed = true
            }

            if (shouldClose) {
                for (outputFrame in _frames) {
                    outputFrame.close()
                }
                _frames.clear()
            }
        }
    }
}
