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

import android.hardware.HardwareBuffer
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.Frame.Companion.isFrameInfoAvailable
import androidx.camera.camera2.pipe.Frame.Companion.isImageAvailable
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.FrameReference.Companion.acquire
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.SensorTimestamp
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.ImageSimulator
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [FrameDistributor] */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.NEWEST_SDK])
class FrameDistributorTest {

    private val stream1Config =
        CameraStream.Config.create(
            Size(1280, 720),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(5),
        )
    private val stream2Config =
        CameraStream.Config.create(
            Size(1920, 1080),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(5),
        )
    private val stream3OutputConfigs =
        listOf(
            OutputStream.Config.create(Size(1280, 720), StreamFormat.RAW10),
            OutputStream.Config.create(Size(1920, 1080), StreamFormat.RAW10),
            OutputStream.Config.create(Size(1920, 1200), StreamFormat.RAW10),
        )
    private val stream3Config =
        CameraStream.Config.create(stream3OutputConfigs, ImageSourceConfig(5))
    private val stream4OutputConfigs =
        listOf(
            OutputStream.Config.create(Size(1920, 1080), StreamFormat.RAW_SENSOR),
            OutputStream.Config.create(Size(1280, 720), StreamFormat.RAW_SENSOR),
            OutputStream.Config.create(Size(1920, 1200), StreamFormat.RAW_SENSOR),
        )
    private val stream4Config =
        CameraStream.Config.create(
            stream4OutputConfigs,
            ImageSourceConfig(5).apply { enableConcurrentOutputs = true },
        )

    private val streamConfigs = listOf(stream1Config, stream2Config, stream3Config, stream4Config)

    private val imageSimulator = ImageSimulator(streamConfigs)
    private val stream1Id = imageSimulator.streamGraph[stream1Config]!!.id
    private val stream1OutputId = imageSimulator.streamGraph[stream1Config]!!.outputs.first().id
    private val stream2Id = imageSimulator.streamGraph[stream2Config]!!.id
    private val stream2OutputId = imageSimulator.streamGraph[stream2Config]!!.outputs.first().id
    private val stream3Id = imageSimulator.streamGraph[stream3Config]!!.id
    private val stream3OutputIds = imageSimulator.streamGraph[stream3Config]!!.outputs.map { it.id }
    private val stream4Id = imageSimulator.streamGraph[stream4Config]!!.id
    private val stream4OutputIds = imageSimulator.streamGraph[stream4Config]!!.outputs.map { it.id }

    private val streams = listOf(stream1Id, stream2Id, stream3Id, stream4Id)

    private val cameraId = imageSimulator.cameraMetadata.camera
    private val cameraTimestamp = CameraTimestamp(1234L)
    private val cameraFrameNumber = FrameNumber(420)

    private val request = Request(streams = streams)
    private val fakeRequestMetadata =
        FakeRequestMetadata.from(request, imageSimulator.streamToSurfaceMap, repeating = false)
    private val fakeFrameInfo =
        FakeFrameInfo(
            metadata = FakeFrameMetadata(camera = cameraId, frameNumber = cameraFrameNumber),
            requestMetadata = fakeRequestMetadata,
        )

    private val fakeFrameBuffer = FakeFrameBuffer()
    private val frameCaptureQueue = FrameCaptureQueue()
    private val frameDistributor =
        FrameDistributor(imageSimulator.streamGraph, frameCaptureQueue, true, 0L).also {
            it.frameStartedListener = fakeFrameBuffer
        }

    private val readoutStreamConfig by lazy {
        CameraStream.Config.create(
            Size(1920, 1080),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(capacity = 10),
            useReadoutTimestamp = true,
        )
    }
    private lateinit var readoutImageSimulator: ImageSimulator
    private lateinit var readoutStreamIds: List<StreamId>
    private val readoutStreamId: StreamId
        get() = readoutStreamIds.first()

    private val readoutOutputId: OutputId
        get() = readoutImageSimulator.streamGraph[readoutStreamId]!!.outputs.first().id

    private lateinit var readoutFakeRequestMetadata: FakeRequestMetadata
    private lateinit var readoutFakeFrameBuffer: FakeFrameBuffer
    private lateinit var readoutFrameCaptureQueue: FrameCaptureQueue
    private lateinit var readoutFrameDistributor: FrameDistributor

    @Test
    fun frameDistributorSetupVerification() {
        assertThat(imageSimulator.streamGraph.streamIds)
            .containsExactly(stream1Id, stream2Id, stream3Id, stream4Id)
        assertThat(imageSimulator.streamToSurfaceMap.keys)
            .containsExactly(stream1Id, stream2Id, stream3Id, stream4Id)
    }

    @Test
    fun framesAreAddedToFrameBuffer() {
        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)

        assertThat(fakeFrameBuffer.frames.size).isEqualTo(1)

        val frame = fakeFrameBuffer.frames[0]
        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.PENDING)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }
        assertThat(frame.imageStreams).containsExactly(stream1Id, stream2Id, stream3Id, stream4Id)

        // Closing should cause all outputs to be closed, since this should be the only frame.
        frame.close()

        assertThat(frame.imageStreams).containsExactly(stream1Id, stream2Id, stream3Id, stream4Id)

        assertThat(frame.getFrameInfo()).isEqualTo(null)
        assertThat(frame.getImage(stream1Id)).isEqualTo(null)
        assertThat(frame.getImage(stream2Id)).isEqualTo(null)
        assertThat(frame.getImage(stream3Id)).isEqualTo(null)
        assertThat(frame.getImage(stream4Id)).isEqualTo(null)

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.UNAVAILABLE)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.UNAVAILABLE)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.UNAVAILABLE)
        }
    }

    @Test
    fun outputsAreDistributedToFrame() {
        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)

        assertThat(fakeFrameBuffer.frames.size).isEqualTo(1)
        val frame = fakeFrameBuffer.frames[0]

        val image1 = imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        assertThat(frame.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame.isImageAvailable(stream1OutputId)).isTrue()
        assertThat(frame.isImageAvailable(stream2Id)).isFalse()
        assertThat(frame.isImageAvailable(stream2OutputId)).isFalse()
        assertThat(frame.isImageAvailable(stream3Id)).isFalse()
        stream3OutputIds.forEach { assertThat(frame.isImageAvailable(it)).isFalse() }
        assertThat(frame.isImageAvailable(stream4Id)).isFalse()
        stream4OutputIds.forEach { assertThat(frame.isImageAvailable(it)).isFalse() }
        assertThat(frame.isFrameInfoAvailable).isFalse()
        assertThat(image1.isClosed).isFalse()

        val image2 = imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)
        assertThat(frame.isImageAvailable(stream2Id)).isTrue()
        assertThat(frame.isImageAvailable(stream2OutputId)).isTrue()
        assertThat(frame.isFrameInfoAvailable).isFalse()
        assertThat(image2.isClosed).isFalse()

        frameDistributor.onComplete(fakeRequestMetadata, cameraFrameNumber, fakeFrameInfo)
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

        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)

        assertThat(frameCapture.status).isEqualTo(OutputStatus.AVAILABLE)
        val frame = frameCapture.getFrame()
        assertThat(frame).isNotNull()
        frame?.close()
    }

    @Test
    fun abortedRequestsCauseFramesToBeAborted() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onAborted(fakeRequestMetadata.request)
        assertThat(frameCapture.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(frameCapture.getFrame()).isNull()
    }

    @Test
    fun onFailureCausesFrameInfoToBeLost() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)
        val frame = frameCapture.getFrame()!!

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.PENDING)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }

        frameDistributor.onFailed(
            fakeRequestMetadata,
            cameraFrameNumber,
            FakeRequestFailure(fakeRequestMetadata, cameraFrameNumber, wasImageCaptured = true),
        )

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.PENDING)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }

        // Images are still delivered, even after onFailed
        imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)
        imageSimulator.simulateExpectedOutputs(
            stream3Id,
            cameraTimestamp.value,
            setOf(stream3OutputIds[0]),
        )
        imageSimulator.simulateImage(stream3Id, cameraTimestamp.value, stream3OutputIds[0])
        imageSimulator.simulateExpectedOutputs(
            stream4Id,
            cameraTimestamp.value,
            setOf(stream4OutputIds[0], stream4OutputIds[2]),
        )
        imageSimulator.simulateImage(stream4Id, cameraTimestamp.value, stream4OutputIds[0])
        imageSimulator.simulateImage(stream4Id, cameraTimestamp.value, stream4OutputIds[2])

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.isImageAvailable(stream1Id)).isTrue()
        assertThat(frame.isImageAvailable(stream1OutputId)).isTrue()
        assertThat(frame.isImageAvailable(stream2Id)).isTrue()
        assertThat(frame.isImageAvailable(stream2OutputId)).isTrue()
        assertThat(frame.isImageAvailable(stream3Id)).isTrue()
        assertThat(frame.isImageAvailable(stream3OutputIds[0])).isTrue()
        assertThat(frame.isImageAvailable(stream3OutputIds[1])).isFalse()
        assertThat(frame.isImageAvailable(stream3OutputIds[2])).isFalse()
        assertThat(frame.isImageAvailable(stream4Id)).isTrue()
        assertThat(frame.isImageAvailable(stream4OutputIds[0])).isTrue()
        assertThat(frame.isImageAvailable(stream4OutputIds[1])).isFalse()
        assertThat(frame.isImageAvailable(stream4OutputIds[2])).isTrue()
    }

    @Test
    fun onFailureWithImageLossAllOutputsToFail() {
        val frameCapture = frameCaptureQueue.enqueue(fakeRequestMetadata.request)
        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)
        val frame = frameCapture.getFrame()!!

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.PENDING)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.PENDING)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.PENDING)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.PENDING)
        }

        frameDistributor.onFailed(
            fakeRequestMetadata,
            cameraFrameNumber,
            FakeRequestFailure(fakeRequestMetadata, cameraFrameNumber, wasImageCaptured = false),
        )

        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream1Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream1OutputId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream2Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream2OutputId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(stream3Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        stream3OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        }
        assertThat(frame.imageStatus(stream4Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        stream4OutputIds.forEach {
            assertThat(frame.imageStatus(it)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        }

        // Images are still delivered, even after onFailed
        val fakeImage1 = imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        val fakeImage2 = imageSimulator.simulateImage(stream2Id, cameraTimestamp.value)
        imageSimulator.simulateExpectedOutputs(
            stream3Id,
            cameraTimestamp.value,
            setOf(stream3OutputIds[0]),
        )
        val fakeImage3 =
            imageSimulator.simulateImage(stream3Id, cameraTimestamp.value, stream3OutputIds[0])
        imageSimulator.simulateExpectedOutputs(
            stream4Id,
            cameraTimestamp.value,
            setOf(stream4OutputIds[1], stream4OutputIds[2]),
        )
        val fakeImage4 =
            imageSimulator.simulateImage(stream4Id, cameraTimestamp.value, stream4OutputIds[1])
        val fakeImage5 =
            imageSimulator.simulateImage(stream4Id, cameraTimestamp.value, stream4OutputIds[2])

        assertThat(fakeImage1.isClosed).isTrue()
        assertThat(fakeImage2.isClosed).isTrue()
        assertThat(fakeImage3.isClosed).isTrue()
        assertThat(fakeImage4.isClosed).isTrue()
        assertThat(fakeImage5.isClosed).isTrue()
    }

    @Test
    fun selectTimestampMatcher_cameraRealtimeOutputMonotonic_fuzzyEqual() {
        // output monotonic time
        val imageSourceConfig =
            ImageSourceConfig(capacity = 5, usageFlags = HardwareBuffer.USAGE_VIDEO_ENCODE)
        val cameraStreamConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = imageSourceConfig,
            )
        val sleepTimeNs = -100_000_000L
        val realTimeNs = 500_000_000L
        val monoTimeNs = 400_000_000L

        val matcher =
            FrameDistributor.selectTimestampMatcher(
                cameraStreamId = StreamId(1),
                cameraStreamConfig = cameraStreamConfig,
                imageSourceConfig = imageSourceConfig,
                isCameraTimebaseRealtime = true, // camera real time
                realtimeToMonotonicOffsetNs = sleepTimeNs,
            )

        assertThat(matcher.fuzzyEqual(realTimeNs, monoTimeNs)).isTrue()
    }

    @Test
    fun selectTimestampMatcher_cameraMonotonicOutputRealtime_fuzzyEqual() {
        val imageSourceConfig = ImageSourceConfig(capacity = 5)
        // output real time
        val outputStreamConfig =
            OutputStream.Config.create(
                size = Size(1920, 1080),
                format = StreamFormat.YUV_420_888,
                timestampBase = OutputStream.TimestampBase.TIMESTAMP_BASE_REALTIME,
            )
        val cameraStreamConfig =
            CameraStream.Config.create(
                outputs = listOf(outputStreamConfig),
                imageSourceConfig = imageSourceConfig,
            )
        val sleepTimeNs = -100_000_000L
        val realTimeNs = 500_000_000L
        val monoTimeNs = 400_000_000L

        val matcher =
            FrameDistributor.selectTimestampMatcher(
                cameraStreamId = StreamId(1),
                cameraStreamConfig = cameraStreamConfig,
                imageSourceConfig = imageSourceConfig,
                isCameraTimebaseRealtime = false, // camera monotonic time
                realtimeToMonotonicOffsetNs = sleepTimeNs,
            )

        assertThat(matcher.fuzzyEqual(monoTimeNs, realTimeNs)).isTrue()
    }

    @Test
    fun frameDistributor_cameraRealtimeOutputMonotonic_positiveJitter_noFrameDrop() {
        val imageSourceConfig =
            ImageSourceConfig(capacity = 5, usageFlags = HardwareBuffer.USAGE_VIDEO_ENCODE)
        val cameraStreamConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = imageSourceConfig,
            )
        val localImageSimulator = ImageSimulator(listOf(cameraStreamConfig))
        val streamId = localImageSimulator.streamGraph[cameraStreamConfig]!!.id
        val outputId = localImageSimulator.streamGraph[cameraStreamConfig]!!.outputs.first().id
        val fakeRequestMetadata =
            FakeRequestMetadata.from(
                Request(streams = listOf(streamId)),
                localImageSimulator.streamToSurfaceMap,
                repeating = false,
            )
        val fakeFrameBuffer = FakeFrameBuffer()
        val localFrameDistributor =
            FrameDistributor(
                    localImageSimulator.streamGraph,
                    FrameCaptureQueue(),
                    isCameraTimebaseRealtime = true,
                    realtimeToMonotonicOffsetNs = -100_000_000L,
                )
                .also { it.frameStartedListener = fakeFrameBuffer }

        localFrameDistributor.onStarted(
            fakeRequestMetadata,
            FrameNumber(420),
            CameraTimestamp(500_000_000L),
        )
        val frame = fakeFrameBuffer.frames[0]
        localImageSimulator.simulateImage(streamId, 405_000_000L)

        assertThat(frame.isImageAvailable(streamId)).isTrue()
        assertThat(frame.isImageAvailable(outputId)).isTrue()

        frame.close()
        localImageSimulator.close()
    }

    @Test
    fun frameDistributor_cameraRealtimeOutputMonotonic_negativeJitter_noFrameDrop() {
        val imageSourceConfig =
            ImageSourceConfig(capacity = 5, usageFlags = HardwareBuffer.USAGE_VIDEO_ENCODE)
        val cameraStreamConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = imageSourceConfig,
            )
        val localImageSimulator = ImageSimulator(listOf(cameraStreamConfig))
        val streamId = localImageSimulator.streamGraph[cameraStreamConfig]!!.id
        val outputId = localImageSimulator.streamGraph[cameraStreamConfig]!!.outputs.first().id
        val fakeRequestMetadata =
            FakeRequestMetadata.from(
                Request(streams = listOf(streamId)),
                localImageSimulator.streamToSurfaceMap,
                repeating = false,
            )
        val fakeFrameBuffer = FakeFrameBuffer()
        val localFrameDistributor =
            FrameDistributor(
                    localImageSimulator.streamGraph,
                    FrameCaptureQueue(),
                    isCameraTimebaseRealtime = true,
                    realtimeToMonotonicOffsetNs = -100_000_000L,
                )
                .also { it.frameStartedListener = fakeFrameBuffer }

        localFrameDistributor.onStarted(
            fakeRequestMetadata,
            FrameNumber(420),
            CameraTimestamp(500_000_000L),
        )
        val frame = fakeFrameBuffer.frames[0]
        localImageSimulator.simulateImage(streamId, 395_000_000L)

        assertThat(frame.isImageAvailable(streamId)).isTrue()
        assertThat(frame.isImageAvailable(outputId)).isTrue()

        frame.close()
        localImageSimulator.close()
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_distributesImageOnReadoutStarted() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(100)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )

        readoutImageSimulator.simulateImage(readoutStreamId, readoutTimestamp.value)

        assertThat(frame.isImageAvailable(readoutStreamId)).isTrue()
        assertThat(frame.isImageAvailable(readoutOutputId)).isTrue()
        val image = checkNotNull(frame.getImage(readoutStreamId))
        assertThat(image.timestamp).isEqualTo(readoutTimestamp.value)
        image.close()
    }

    @Test
    fun frameDistributor_mixedStreams_startedAndReadoutStarted() {
        val streamExposureConfig =
            CameraStream.Config.create(
                Size(1280, 720),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = false,
            )
        val streamReadoutConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = true,
            )
        initReadoutFrameDistributor(listOf(streamExposureConfig, streamReadoutConfig))
        val streamExposureId = readoutStreamIds[0]
        val streamReadoutId = readoutStreamIds[1]
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(102)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutImageSimulator.simulateImage(streamExposureId, exposureTimestamp.value)
        assertThat(frame.isImageAvailable(streamExposureId)).isTrue()
        assertThat(frame.isImageAvailable(streamReadoutId)).isFalse()

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        readoutImageSimulator.simulateImage(streamReadoutId, readoutTimestamp.value)
        assertThat(frame.isImageAvailable(streamReadoutId)).isTrue()

        val exposureImage = checkNotNull(frame.getImage(streamExposureId))
        val readoutImage = checkNotNull(frame.getImage(streamReadoutId))
        assertThat(exposureImage.timestamp).isEqualTo(exposureTimestamp.value)
        assertThat(readoutImage.timestamp).isEqualTo(readoutTimestamp.value)
        exposureImage.close()
        readoutImage.close()
    }

    @Test
    fun frameDistributor_mixedStreams_onBufferLostForNonReadoutStream_readoutStreamStillCompletes() {
        val streamExposureConfig =
            CameraStream.Config.create(
                Size(1280, 720),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = false,
            )
        val streamReadoutConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = true,
            )
        initReadoutFrameDistributor(listOf(streamExposureConfig, streamReadoutConfig))
        val streamExposureId = readoutStreamIds[0]
        val streamExposureOutputId =
            readoutImageSimulator.streamGraph[streamExposureConfig]!!.outputs.first().id
        val streamReadoutId = readoutStreamIds[1]
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(103)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onBufferLost(
            readoutFakeRequestMetadata,
            frameNum,
            streamExposureId,
            streamExposureOutputId,
        )
        assertThat(frame.imageStatus(streamExposureId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(streamReadoutId)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        readoutImageSimulator.simulateImage(streamReadoutId, readoutTimestamp.value)
        assertThat(frame.isImageAvailable(streamReadoutId)).isTrue()

        val readoutImage = checkNotNull(frame.getImage(streamReadoutId))
        assertThat(readoutImage.timestamp).isEqualTo(readoutTimestamp.value)
        readoutImage.close()
    }

    @Test
    fun frameDistributor_mixedStreams_onBufferLostForReadoutStream_nonReadoutStreamStillCompletes() {
        val streamExposureConfig =
            CameraStream.Config.create(
                Size(1280, 720),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = false,
            )
        val streamReadoutConfig =
            CameraStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
                useReadoutTimestamp = true,
            )
        initReadoutFrameDistributor(listOf(streamExposureConfig, streamReadoutConfig))
        val streamExposureId = readoutStreamIds[0]
        val streamReadoutId = readoutStreamIds[1]
        val streamReadoutOutputId =
            readoutImageSimulator.streamGraph[streamReadoutConfig]!!.outputs.first().id
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(104)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onBufferLost(
            readoutFakeRequestMetadata,
            frameNum,
            streamReadoutId,
            streamReadoutOutputId,
        )
        assertThat(frame.imageStatus(streamReadoutId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        assertThat(frame.imageStatus(streamReadoutId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)

        readoutImageSimulator.simulateImage(streamExposureId, exposureTimestamp.value)
        assertThat(frame.isImageAvailable(streamExposureId)).isTrue()

        val exposureImage = checkNotNull(frame.getImage(streamExposureId))
        assertThat(exposureImage.timestamp).isEqualTo(exposureTimestamp.value)
        exposureImage.close()
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_streamNotInRequest_failsGracefully() {
        initReadoutFrameDistributor(requestStreamsMap = emptyMap())
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(103)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )

        val frame = readoutFakeFrameBuffer.frames[0]
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.UNAVAILABLE)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_onFailedWithImageLoss_failsOutputs() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val frameNum = FrameNumber(104)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onFailed(
            readoutFakeRequestMetadata,
            frameNum,
            FakeRequestFailure(readoutFakeRequestMetadata, frameNum, wasImageCaptured = false),
        )

        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_onFailedWithImageCaptured_deliversImage() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(104)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onFailed(
            readoutFakeRequestMetadata,
            frameNum,
            FakeRequestFailure(readoutFakeRequestMetadata, frameNum, wasImageCaptured = true),
        )

        // Frame metadata failed, but image output is still pending because wasImageCaptured is
        // true
        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        readoutImageSimulator.simulateImage(readoutStreamId, readoutTimestamp.value)

        assertThat(frame.isImageAvailable(readoutStreamId)).isTrue()
        val image = checkNotNull(frame.getImage(readoutStreamId))
        assertThat(image.timestamp).isEqualTo(readoutTimestamp.value)
        image.close()
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_closedBeforeReadoutStarted_abortsOutputs() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val frameNum = FrameNumber(105)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.close()

        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_onBufferLostBeforeReadoutStarted_failsOutput() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(106)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onBufferLost(
            readoutFakeRequestMetadata,
            frameNum,
            readoutStreamId,
            readoutOutputId,
        )
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_onBufferLostAfterReadoutStarted_failsOutput() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val readoutTimestamp = SensorTimestamp(120_000_000L)
        val frameNum = FrameNumber(107)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readoutTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.onBufferLost(
            readoutFakeRequestMetadata,
            frameNum,
            readoutStreamId,
            readoutOutputId,
        )
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_multipleFramesInFlight() {
        initReadoutFrameDistributor()
        val frame1Num = FrameNumber(108)
        val frame2Num = FrameNumber(109)
        val exp1 = CameraTimestamp(100_000_000L)
        val exp2 = CameraTimestamp(133_000_000L)
        val read1 = SensorTimestamp(120_000_000L)
        val read2 = SensorTimestamp(153_000_000L)

        readoutFrameDistributor.onStarted(readoutFakeRequestMetadata, frame1Num, exp1)
        readoutFrameDistributor.onStarted(readoutFakeRequestMetadata, frame2Num, exp2)

        val frame1 = readoutFakeFrameBuffer.frames[0]
        val frame2 = readoutFakeFrameBuffer.frames[1]

        // Frame 2 readout arrives before frame 1 readout
        readoutFrameDistributor.onReadoutStarted(readoutFakeRequestMetadata, frame2Num, read2)
        readoutImageSimulator.simulateImage(readoutStreamId, read2.value)

        assertThat(frame2.isImageAvailable(readoutStreamId)).isTrue()
        assertThat(frame1.isImageAvailable(readoutStreamId)).isFalse()

        readoutFrameDistributor.onReadoutStarted(readoutFakeRequestMetadata, frame1Num, read1)
        readoutImageSimulator.simulateImage(readoutStreamId, read1.value)

        assertThat(frame1.isImageAvailable(readoutStreamId)).isTrue()

        val img1 = checkNotNull(frame1.getImage(readoutStreamId))
        val img2 = checkNotNull(frame2.getImage(readoutStreamId))
        assertThat(img1.timestamp).isEqualTo(read1.value)
        assertThat(img2.timestamp).isEqualTo(read2.value)
        img1.close()
        img2.close()
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_onCompleteBeforeReadoutStarted_failsUnstartedOutputs() {
        initReadoutFrameDistributor()
        val exposureTimestamp = CameraTimestamp(100_000_000L)
        val frameNum = FrameNumber(110)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            exposureTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.PENDING)

        // HAL completes capture without sending onReadoutStarted
        readoutFrameDistributor.onComplete(
            readoutFakeRequestMetadata,
            frameNum,
            FakeFrameInfo(
                metadata = FakeFrameMetadata(camera = cameraId, frameNumber = frameNum),
                requestMetadata = readoutFakeRequestMetadata,
            ),
        )

        // The unstarted readout output should be marked as failed rather than hanging PENDING
        assertThat(frame.imageStatus(readoutStreamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.frameInfoStatus).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun frameDistributor_withUseReadoutTimestamp_concurrentOutputs_bufferLostForOneOutput_failsOnlyThatOutput() {
        val output1Config =
            OutputStream.Config.create(
                Size(1920, 1080),
                StreamFormat.YUV_420_888,
                useReadoutTimestamp = true,
            )
        val output2Config =
            OutputStream.Config.create(
                Size(1280, 720),
                StreamFormat.YUV_420_888,
                useReadoutTimestamp = true,
            )
        val streamConfig =
            CameraStream.Config.create(
                listOf(output1Config, output2Config),
                ImageSourceConfig(5).apply { enableConcurrentOutputs = true },
            )
        initReadoutFrameDistributor(listOf(streamConfig))
        val stream = readoutImageSimulator.streamGraph[streamConfig]!!
        val output1Id = stream.outputs[0].id
        val output2Id = stream.outputs[1].id

        val frameNum = FrameNumber(601)
        val expTimestamp = CameraTimestamp(100_000_000L)
        val readTimestamp = SensorTimestamp(120_000_000L)

        readoutFrameDistributor.onStarted(
            readoutFakeRequestMetadata,
            frameNum,
            expTimestamp,
        )
        val frame = readoutFakeFrameBuffer.frames[0]

        readoutFrameDistributor.onBufferLost(
            readoutFakeRequestMetadata,
            frameNum,
            readoutStreamId,
            output1Id,
        )
        assertThat(frame.imageStatus(output1Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.imageStatus(output2Id)).isEqualTo(OutputStatus.PENDING)

        readoutFrameDistributor.onReadoutStarted(
            readoutFakeRequestMetadata,
            frameNum,
            readTimestamp,
        )

        readoutImageSimulator.simulateImage(readoutStreamId, readTimestamp.value, output2Id)

        assertThat(frame.imageStatus(output1Id)).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(frame.isImageAvailable(output2Id)).isTrue()
        val img2 = checkNotNull(frame.getImage(output2Id))
        assertThat(img2.timestamp).isEqualTo(readTimestamp.value)
        img2.close()
    }

    @Test
    fun frameDistributor_withoutUseReadoutTimestamp_onReadoutStartedSafelyIgnored() {
        // On API 34+, Android Camera2 / HAL may invoke onReadoutStarted even when no streams on the
        // graph are configured to use readout timestamps. FrameDistributor should ignore it safely
        // without throwing an exception or interfering with normal frame distribution.

        frameDistributor.onReadoutStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            SensorTimestamp(cameraTimestamp.value + 500L),
        )

        frameDistributor.onStarted(fakeRequestMetadata, cameraFrameNumber, cameraTimestamp)

        frameDistributor.onReadoutStarted(
            fakeRequestMetadata,
            cameraFrameNumber,
            SensorTimestamp(cameraTimestamp.value + 1000L),
        )

        imageSimulator.simulateImage(stream1Id, cameraTimestamp.value)
        val frame = fakeFrameBuffer.frames[0]
        assertThat(frame.isImageAvailable(stream1OutputId)).isTrue()

        frame.close()
    }

    @Test
    @Config(sdk = [33])
    fun frameDistributor_belowApi34_useReadoutTimestampTrue_throws() {
        // On API < 34, creating a stream config with useReadoutTimestamp = true is not supported.
        assertThrows<IllegalStateException> { initReadoutFrameDistributor() }
            .hasMessageThat()
            .isEqualTo("onReadoutStarted is not supported with API < 34")
    }

    @After
    fun cleanup() {
        imageSimulator.close()
        if (::readoutFrameDistributor.isInitialized) {
            readoutFakeFrameBuffer.close()
            readoutFrameDistributor.close()
            readoutFrameCaptureQueue.close()
            readoutImageSimulator.checkImagesClosed()
            readoutImageSimulator.close()
        }
    }

    private fun initReadoutFrameDistributor(
        streamConfigs: List<CameraStream.Config> = listOf(readoutStreamConfig),
        requestStreamsMap: Map<StreamId, Surface>? = null,
    ) {
        readoutImageSimulator = ImageSimulator(streamConfigs)
        readoutStreamIds = streamConfigs.map { readoutImageSimulator.streamGraph[it]!!.id }
        readoutFakeRequestMetadata =
            if (requestStreamsMap != null) {
                FakeRequestMetadata(
                    request = Request(streams = readoutStreamIds),
                    streams = requestStreamsMap,
                )
            } else {
                FakeRequestMetadata.from(
                    Request(streams = readoutStreamIds),
                    readoutImageSimulator.streamToSurfaceMap,
                    repeating = false,
                )
            }
        readoutFakeFrameBuffer = FakeFrameBuffer()
        readoutFrameCaptureQueue = FrameCaptureQueue()
        readoutFrameDistributor =
            FrameDistributor(
                    readoutImageSimulator.streamGraph,
                    readoutFrameCaptureQueue,
                    isCameraTimebaseRealtime = false,
                    realtimeToMonotonicOffsetNs = 0L,
                )
                .also { it.frameStartedListener = readoutFakeFrameBuffer }
    }

    private class FakeFrameBuffer : FrameDistributor.FrameStartedListener, AutoCloseable {
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
