/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.InputRequest
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeImage
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [LatestFrameNumberListener] and [LatestFrameInfoListener] */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class LatestFrameListenerTests {
    private val streamId = StreamId(1)

    private val fakeSurfaces = FakeSurfaces()
    private val stream1Surface = fakeSurfaces.createFakeSurface()

    private val fakeRequestMetadata =
        FakeRequestMetadata(streams = mapOf(streamId to stream1Surface))
    private val fakeFrameNumber = FrameNumber(420)
    private val fakeImageTimestampNs = 1234L
    private val fakeReprocessingImage =
        FakeImage(640, 480, StreamFormat.YUV_420_888.value, fakeImageTimestampNs)
    private val fakeReprocessingFrameMetadata = FakeFrameMetadata(frameNumber = fakeFrameNumber)
    private val fakeReprocessingFrameInfo =
        FakeFrameInfo(
            metadata = fakeReprocessingFrameMetadata,
            requestMetadata = fakeRequestMetadata,
        )
    private val fakeReprocessingRequestMetadata =
        FakeRequestMetadata(
            request =
                Request(
                    streams = listOf(streamId),
                    inputRequest = InputRequest(fakeReprocessingImage, fakeReprocessingFrameInfo),
                ),
            streams = mapOf(streamId to stream1Surface),
        )

    private var latestFrameInfo: FrameInfo? = null
    private var latestFrameNumber: FrameNumber? = null

    private val latestFrameInfoListener = LatestFrameInfoListener { latestFrameInfo = it }
    private val latestFrameNumberListener = LatestFrameNumberListener { latestFrameNumber = it }

    @Test
    fun latestFrameNumberListenerUpdatesLatestState() {
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(1),
            CameraTimestamp(42),
        )
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(2),
            CameraTimestamp(43),
        )
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(3),
            CameraTimestamp(44),
        )

        assertThat(latestFrameNumber?.value).isEqualTo(3)
    }

    @Test
    fun latestFrameInfoListenerUpdatesLatestState() {
        val frame1Metadata = FakeFrameMetadata(frameNumber = FrameNumber(1))
        val frame1Info =
            FakeFrameInfo(metadata = frame1Metadata, requestMetadata = fakeRequestMetadata)
        latestFrameInfoListener.onTotalCaptureResult(
            fakeRequestMetadata,
            frame1Info.frameNumber,
            frame1Info,
        )

        val frame2Metadata = FakeFrameMetadata(frameNumber = FrameNumber(2))
        val frame2Info =
            FakeFrameInfo(metadata = frame2Metadata, requestMetadata = fakeRequestMetadata)
        latestFrameInfoListener.onTotalCaptureResult(
            fakeRequestMetadata,
            frame2Info.frameNumber,
            frame2Info,
        )

        assertThat(latestFrameInfo).isEqualTo(frame2Info)
    }

    @Test
    fun latestFrameNumberListenerSkipsOutOfOrderRequests() {
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(1),
            CameraTimestamp(42),
        )
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(3),
            CameraTimestamp(44),
        )
        // Out of order
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(2),
            CameraTimestamp(43),
        )

        assertThat(latestFrameNumber?.value).isEqualTo(3)
    }

    fun latestFrameInfoListenerSkipsOutOfOrderRequests() = runTest {
        val frame2Metadata = FakeFrameMetadata(frameNumber = FrameNumber(2))
        val frame2Info =
            FakeFrameInfo(metadata = frame2Metadata, requestMetadata = fakeRequestMetadata)
        latestFrameInfoListener.onTotalCaptureResult(
            fakeRequestMetadata,
            frame2Info.frameNumber,
            frame2Info,
        )

        // Out of order
        val frame1Metadata = FakeFrameMetadata(frameNumber = FrameNumber(1))
        val frame1Info =
            FakeFrameInfo(metadata = frame1Metadata, requestMetadata = fakeRequestMetadata)
        latestFrameInfoListener.onTotalCaptureResult(
            fakeRequestMetadata,
            frame1Info.frameNumber,
            frame1Info,
        )

        assertThat(latestFrameInfo).isEqualTo(frame2Info)
    }

    @Test
    fun latestFrameNumberListenerSkipsReprocessingRequests() {
        latestFrameNumberListener.onStarted(
            fakeRequestMetadata,
            FrameNumber(1),
            CameraTimestamp(42),
        )
        latestFrameNumberListener.onStarted(
            fakeReprocessingRequestMetadata,
            FrameNumber(100),
            CameraTimestamp(42),
        )
        assertThat(latestFrameNumber?.value).isEqualTo(1)
    }

    @Test
    fun latestFrameInfoListenerSkipsReprocessingRequests() {

        val frame1Metadata = FakeFrameMetadata(frameNumber = FrameNumber(1))
        val frame1Info =
            FakeFrameInfo(metadata = frame1Metadata, requestMetadata = fakeRequestMetadata)
        latestFrameInfoListener.onTotalCaptureResult(
            fakeRequestMetadata,
            frame1Info.frameNumber,
            frame1Info,
        )

        // Reprocessing should be skipped
        val frame2Metadata = FakeFrameMetadata(frameNumber = FrameNumber(100))
        val frame2Info =
            FakeFrameInfo(
                metadata = frame2Metadata,
                requestMetadata = fakeReprocessingRequestMetadata,
            )
        latestFrameInfoListener.onTotalCaptureResult(
            fakeReprocessingRequestMetadata,
            frame2Info.frameNumber,
            frame2Info,
        )

        assertThat(latestFrameInfo).isEqualTo(frame1Info)
    }
}
