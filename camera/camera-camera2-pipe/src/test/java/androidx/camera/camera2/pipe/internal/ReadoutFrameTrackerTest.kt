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

package androidx.camera.camera2.pipe.internal

import android.util.Size
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.NEWEST_SDK])
class ReadoutFrameTrackerTest {
    private val cameraId = CameraId("0")

    // Stream 1: a single output that uses readout timestamps.
    private val readoutStreamId = StreamId(1)
    private val readoutOutputId = OutputId(1)
    private val readoutOutput =
        StreamGraphImpl.OutputStreamImpl(
            readoutOutputId,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
            useReadoutTimestamp = true,
        )
    private val readoutStream =
        CameraStream(readoutStreamId, listOf(readoutOutput)).apply { readoutOutput.stream = this }

    // Stream 2: a concurrent stream with two outputs that use readout timestamps.
    private val concurrentStreamId = StreamId(2)
    private val concurrentOutput1Id = OutputId(2)
    private val concurrentOutput2Id = OutputId(3)
    private val concurrentOutput1 =
        StreamGraphImpl.OutputStreamImpl(
            concurrentOutput1Id,
            Size(640, 480),
            StreamFormat.RAW10,
            cameraId,
            useReadoutTimestamp = true,
        )
    private val concurrentOutput2 =
        StreamGraphImpl.OutputStreamImpl(
            concurrentOutput2Id,
            Size(640, 480),
            StreamFormat.RAW10,
            cameraId,
            useReadoutTimestamp = true,
        )
    private val concurrentStream =
        CameraStream(concurrentStreamId, listOf(concurrentOutput1, concurrentOutput2)).apply {
            concurrentOutput1.stream = this
            concurrentOutput2.stream = this
        }

    // Stream 3: a single output that uses exposure timestamps.
    private val exposureStreamId = StreamId(3)
    private val exposureOutputId = OutputId(4)
    private val exposureOutput =
        StreamGraphImpl.OutputStreamImpl(
            exposureOutputId,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
        )
    private val exposureStream =
        CameraStream(exposureStreamId, listOf(exposureOutput)).apply {
            exposureOutput.stream = this
        }

    // Stream 4: a non-concurrent stream with two outputs that use readout timestamps.
    private val multiOutputStreamId = StreamId(4)
    private val multiOutput1Id = OutputId(5)
    private val multiOutput2Id = OutputId(6)
    private val multiOutput1 =
        StreamGraphImpl.OutputStreamImpl(
            multiOutput1Id,
            Size(640, 480),
            StreamFormat.YUV_420_888,
            cameraId,
            useReadoutTimestamp = true,
        )
    private val multiOutput2 =
        StreamGraphImpl.OutputStreamImpl(
            multiOutput2Id,
            Size(1280, 720),
            StreamFormat.YUV_420_888,
            cameraId,
            useReadoutTimestamp = true,
        )
    private val multiOutputStream =
        CameraStream(multiOutputStreamId, listOf(multiOutput1, multiOutput2)).apply {
            multiOutput1.stream = this
            multiOutput2.stream = this
        }

    private val imageStreams =
        setOf(readoutStream, concurrentStream, exposureStream, multiOutputStream)
    private val concurrentImageStreams = setOf(concurrentStreamId)

    private val fakeSurfaces = FakeSurfaces()
    private val readoutRequestMetadata =
        FakeRequestMetadata(streams = mapOf(readoutStreamId to fakeSurfaces.createFakeSurface()))
    private val concurrentRequestMetadata =
        FakeRequestMetadata(streams = mapOf(concurrentStreamId to fakeSurfaces.createFakeSurface()))
    private val exposureRequestMetadata =
        FakeRequestMetadata(streams = mapOf(exposureStreamId to fakeSurfaces.createFakeSurface()))
    private val multiOutputRequestMetadata =
        FakeRequestMetadata(
            streams = mapOf(multiOutputStreamId to fakeSurfaces.createFakeSurface())
        )

    private val tracker = ReadoutFrameTracker()

    @After
    fun tearDown() {
        fakeSurfaces.close()
    }

    @Test
    fun onStarted_thenOnReadoutStarted_returnsWaitingFrame() {
        val frameState = createFrameState(1, readoutRequestMetadata)

        tracker.onStarted(frameState)

        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isSameInstanceAs(frameState)
        // The frame is no longer tracked after onReadoutStarted.
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
        assertThat(tracker.clear()).isEmpty()
    }

    @Test
    fun onStarted_frameWithoutReadoutOutputs_isNotTracked() {
        val frameState = createFrameState(1, exposureRequestMetadata)
        assertThat(frameState.expectsReadoutTimestamp).isFalse()

        tracker.onStarted(frameState)

        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
        assertThat(tracker.clear()).isEmpty()
    }

    @Test
    fun onReadoutStarted_afterRemove_isDropped() {
        val frameState = createFrameState(1, readoutRequestMetadata)
        tracker.onStarted(frameState)

        assertThat(tracker.remove(CameraFrameNumber(1))).isSameInstanceAs(frameState)

        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
        assertThat(tracker.remove(CameraFrameNumber(1))).isNull()
        assertThat(tracker.clear()).isEmpty()
    }

    @Test
    fun onBufferLost_singleOutput_removesFrame() {
        val frameState = createFrameState(1, readoutRequestMetadata)
        tracker.onStarted(frameState)

        val result =
            tracker.onBufferLost(
                CameraFrameNumber(1),
                readoutStreamId,
                readoutOutputId,
                isConcurrentStream = false,
            )

        assertThat(result).isNotNull()
        assertThat(result!!.frameState).isSameInstanceAs(frameState)
        assertThat(result.lostOutputs).containsExactlyElementsIn(frameState.readoutImageOutputs)
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
    }

    @Test
    fun onBufferLost_nonConcurrentStreamWithMultipleOutputs_losesAllOutputsOnStream() {
        val frameState = createFrameState(1, multiOutputRequestMetadata)
        tracker.onStarted(frameState)
        assertThat(frameState.readoutImageOutputs).hasSize(2)

        // A non-concurrent stream only gets one onBufferLost, so every output on it is lost.
        val result =
            tracker.onBufferLost(
                CameraFrameNumber(1),
                multiOutputStreamId,
                multiOutput1Id,
                isConcurrentStream = false,
            )

        assertThat(result).isNotNull()
        assertThat(result!!.frameState).isSameInstanceAs(frameState)
        assertThat(result.lostOutputs).containsExactlyElementsIn(frameState.readoutImageOutputs)
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
    }

    @Test
    fun onBufferLost_concurrentStream_removesFrameOnlyWhenNoOutputsPending() {
        val frameState = createFrameState(1, concurrentRequestMetadata)
        tracker.onStarted(frameState)
        val output1 = frameState.readoutImageOutputs.single { it.outputId == concurrentOutput1Id }
        val output2 = frameState.readoutImageOutputs.single { it.outputId == concurrentOutput2Id }

        // Losing the first output keeps the frame waiting, since output 2 is still pending.
        val firstResult =
            tracker.onBufferLost(
                CameraFrameNumber(1),
                concurrentStreamId,
                concurrentOutput1Id,
                isConcurrentStream = true,
            )
        assertThat(firstResult).isNotNull()
        assertThat(firstResult!!.lostOutputs).containsExactly(output1)

        // The caller fails the lost output, as FrameDistributor does.
        output1.onOutputComplete(
            frameState.frameNumber,
            frameState.frameTimestamp,
            1L,
            1L,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_FAILED),
        )

        // Losing the second output removes the frame, since no output is pending anymore.
        val secondResult =
            tracker.onBufferLost(
                CameraFrameNumber(1),
                concurrentStreamId,
                concurrentOutput2Id,
                isConcurrentStream = true,
            )
        assertThat(secondResult).isNotNull()
        assertThat(secondResult!!.lostOutputs).containsExactly(output2)
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
    }

    @Test
    fun onBufferLost_unknownFrame_returnsNull() {
        tracker.onStarted(createFrameState(1, readoutRequestMetadata))

        val result =
            tracker.onBufferLost(
                CameraFrameNumber(2),
                readoutStreamId,
                readoutOutputId,
                isConcurrentStream = false,
            )

        assertThat(result).isNull()
        // The tracked frame is not affected.
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNotNull()
    }

    @Test
    fun clear_returnsWaitingFramesAndEmptiesState() {
        val frameState1 = createFrameState(1, readoutRequestMetadata)
        val frameState2 = createFrameState(2, readoutRequestMetadata)
        tracker.onStarted(frameState1)
        tracker.onStarted(frameState2)

        assertThat(tracker.clear()).containsExactly(frameState1, frameState2).inOrder()

        assertThat(tracker.clear()).isEmpty()
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(1))).isNull()
        assertThat(tracker.onReadoutStarted(CameraFrameNumber(2))).isNull()
    }

    private fun createFrameState(
        frameNumber: Long,
        requestMetadata: FakeRequestMetadata,
    ): FrameState =
        FrameState(
            requestMetadata,
            CameraFrameNumber(frameNumber),
            CameraTimestamp(frameNumber * 33_000_000L),
            imageStreams,
            concurrentImageStreams,
        )
}
