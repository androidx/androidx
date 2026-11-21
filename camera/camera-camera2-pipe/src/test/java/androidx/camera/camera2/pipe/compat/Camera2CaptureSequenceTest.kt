/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Request.Listener
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.SensorTimestamp
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class Camera2CaptureSequenceTest {
    private val cameraId: CameraId = CameraId("1")
    private val fakeMetadata =
        FakeCameraMetadata.fromTemplate(template = HighEndDeviceTemplate, cameraId = cameraId)
    private val captureSession: CameraCaptureSession = mock()
    private val captureRequest: CaptureRequest = mock()
    private val listener: FakeRequestListener = FakeRequestListener()
    private val listeners: List<Listener> = listOf(listener)
    private val sequenceListener: CaptureSequence.CaptureSequenceListener = mock()
    private val requestNumber: RequestNumber = RequestNumber(1)
    private val surface: Surface = mock()

    private val streamConfig = CameraStream.Config.create(Size(1280, 720), StreamFormat.PRIVATE)
    private val graphConfig = CameraGraph.Config(camera = cameraId, streams = listOf(streamConfig))
    private val streamGraph =
        StreamGraphImpl(fakeMetadata, graphConfig, mock(), mock(), MemoryEstimator.create())
    private val streamId = streamGraph.streams.single().id
    private val outputId = streamGraph.outputs.single().id

    private val frameNumber: Long = 4
    private val request: Request = Request(listOf(streamId), listeners = listeners)
    private val requestMetadata: FakeRequestMetadata =
        FakeRequestMetadata(request = request, requestNumber = requestNumber)
    private val camera2CaptureSequence =
        Camera2CaptureSequence(
            cameraId,
            false,
            listOf(captureRequest),
            listOf(requestMetadata),
            listeners,
            sequenceListener,
            mapOf(surface to streamId),
            mapOf(surface to outputId),
            streamGraph,
            StrictMode(true),
        )

    @Before
    fun setUp() {
        whenever(captureRequest.tag).thenReturn(requestNumber)
    }

    @Test
    fun onCaptureStartedTest() {
        val timestamp: Long = 123456789
        camera2CaptureSequence.onCaptureStarted(
            captureSession,
            captureRequest,
            timestamp,
            frameNumber,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber)
        assertThat(listener.lastTimeStamp?.value).isEqualTo(timestamp)
    }

    @Test
    fun onReadoutStartedTest() {
        val timestamp: Long = 123456789
        camera2CaptureSequence.onReadoutStarted(
            captureSession,
            captureRequest,
            timestamp,
            frameNumber,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber)
        assertThat(listener.lastSensorTimeStamp?.value).isEqualTo(timestamp)
    }

    @Test
    fun onCaptureCompletedTest() {
        val totalCaptureResult: TotalCaptureResult = mock()
        whenever(totalCaptureResult.frameNumber).thenReturn(frameNumber)
        camera2CaptureSequence.onCaptureCompleted(
            captureSession,
            captureRequest,
            totalCaptureResult,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber)
        assertThat(listener.lastFrameInfo?.requestMetadata).isEqualTo(requestMetadata)
    }

    @Test
    fun onCaptureFailedWithCaptureFailureTest() {
        val captureFailure: CaptureFailure = mock()
        camera2CaptureSequence.onCaptureFailed(captureSession, captureRequest, captureFailure)
        assertThat(listener.lastFrameNumber?.value).isNotEqualTo(frameNumber)
        assertThat(listener.lastRequestFailure?.frameNumber?.value).isNotEqualTo(frameNumber)
    }

    @Test
    fun onCaptureFailedWithFrameNumberTest() {
        camera2CaptureSequence.onCaptureFailed(captureRequest, CameraFrameNumber(frameNumber))
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber)
        assertThat(listener.lastRequestFailure?.frameNumber?.value).isEqualTo(frameNumber)
    }

    @Test
    fun onCaptureBufferLost_singleOutputStream() {
        camera2CaptureSequence.onCaptureBufferLost(
            captureSession,
            captureRequest,
            surface,
            frameNumber,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber)
        assertThat(listener.lastStreamId).isEqualTo(streamId)
        assertThat(listener.lastOutputId).isEqualTo(outputId)
    }

    @Test
    fun onCaptureBufferLost_multiOutputStream() {
        val outputConfig1 = OutputStream.Config.create(Size(1280, 720), StreamFormat.PRIVATE)
        val outputConfig2 = OutputStream.Config.create(Size(1920, 1080), StreamFormat.PRIVATE)
        val streamConfig = CameraStream.Config.create(listOf(outputConfig1, outputConfig2))
        val graphConfig = CameraGraph.Config(camera = cameraId, streams = listOf(streamConfig))
        val streamGraph =
            StreamGraphImpl(fakeMetadata, graphConfig, mock(), mock(), MemoryEstimator.create())
        val stream = checkNotNull(streamGraph[streamConfig])
        val output1 = stream.outputs[0]
        val output2 = stream.outputs[1]

        val request = Request(listOf(stream.id), listeners = listeners)
        val requestMetadata = FakeRequestMetadata(request = request, requestNumber = requestNumber)
        val surface1: Surface = mock()
        val surface2: Surface = mock()
        val camera2CaptureSequence =
            Camera2CaptureSequence(
                cameraId,
                false,
                listOf(captureRequest),
                listOf(requestMetadata),
                listeners,
                sequenceListener,
                mapOf(surface1 to stream.id),
                mapOf(surface1 to output1.id, surface2 to output2.id),
                streamGraph,
                StrictMode(true),
            )

        val frameNumber1: Long = 5
        val frameNumber2: Long = 7
        camera2CaptureSequence.onCaptureBufferLost(
            captureSession,
            captureRequest,
            surface1,
            frameNumber1,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber1)
        assertThat(listener.lastStreamId).isEqualTo(stream.id)
        assertThat(listener.lastOutputId).isEqualTo(output1.id)

        camera2CaptureSequence.onCaptureBufferLost(
            captureSession,
            captureRequest,
            surface2,
            frameNumber2,
        )
        assertThat(listener.lastFrameNumber?.value).isEqualTo(frameNumber2)
        assertThat(listener.lastStreamId).isEqualTo(stream.id)
        assertThat(listener.lastOutputId).isEqualTo(output2.id)
    }

    @Test
    fun onCaptureCompleted_singleRequest_invokesOnCaptureSequenceComplete() {
        camera2CaptureSequence.onCaptureCompleted(
            captureSession,
            captureRequest,
            mockTotalCaptureResult(frameNumber),
        )

        verify(sequenceListener).onCaptureSequenceComplete(camera2CaptureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureFailedWithCaptureFailure_singleRequest_invokesOnCaptureSequenceComplete() {
        val captureFailure: CaptureFailure = mock()
        camera2CaptureSequence.onCaptureFailed(captureSession, captureRequest, captureFailure)

        verify(sequenceListener).onCaptureSequenceComplete(camera2CaptureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureFailedWithFrameNumber_singleRequest_invokesOnCaptureSequenceComplete() {
        camera2CaptureSequence.onCaptureFailed(captureRequest, CameraFrameNumber(frameNumber))

        verify(sequenceListener).onCaptureSequenceComplete(camera2CaptureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureCompleted_multiRequestBurst_invokesOnCaptureSequenceCompleteOnlyAfterLastRequest() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val request3: CaptureRequest = mock()
        val captureSequence =
            createCaptureSequence(
                repeating = false,
                captureRequests = listOf(request1, request2, request3),
            )

        captureSequence.onCaptureCompleted(
            captureSession,
            request1,
            mockTotalCaptureResult(frameNumber),
        )
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureCompleted(
            captureSession,
            request2,
            mockTotalCaptureResult(frameNumber + 1),
        )
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureCompleted(
            captureSession,
            request3,
            mockTotalCaptureResult(frameNumber + 2),
        )
        verify(sequenceListener).onCaptureSequenceComplete(captureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureFailed_multiRequestBurst_invokesOnCaptureSequenceCompleteOnlyAfterLastRequest() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val request3: CaptureRequest = mock()
        val captureFailure: CaptureFailure = mock()
        val captureSequence =
            createCaptureSequence(
                repeating = false,
                captureRequests = listOf(request1, request2, request3),
            )

        captureSequence.onCaptureFailed(captureSession, request1, captureFailure)
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureFailed(request2, CameraFrameNumber(frameNumber + 1))
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureFailed(captureSession, request3, captureFailure)
        verify(sequenceListener).onCaptureSequenceComplete(captureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureCompletedAndFailed_multiRequestBurst_invokesOnCaptureSequenceCompleteOnFinalComplete() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val request3: CaptureRequest = mock()
        val captureFailure: CaptureFailure = mock()
        val captureSequence =
            createCaptureSequence(
                repeating = false,
                captureRequests = listOf(request1, request2, request3),
            )

        captureSequence.onCaptureCompleted(
            captureSession,
            request1,
            mockTotalCaptureResult(frameNumber),
        )
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureFailed(captureSession, request2, captureFailure)
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureCompleted(
            captureSession,
            request3,
            mockTotalCaptureResult(frameNumber + 2),
        )
        verify(sequenceListener).onCaptureSequenceComplete(captureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureCompletedAndFailed_multiRequestBurst_invokesOnCaptureSequenceCompleteOnFinalFailure() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val captureFailure: CaptureFailure = mock()
        val captureSequence =
            createCaptureSequence(repeating = false, captureRequests = listOf(request1, request2))

        captureSequence.onCaptureCompleted(
            captureSession,
            request1,
            mockTotalCaptureResult(frameNumber),
        )
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureFailed(captureSession, request2, captureFailure)
        verify(sequenceListener).onCaptureSequenceComplete(captureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun onCaptureSequenceAborted_multiRequestBurstAfterPartialCompletion_invokesOnCaptureSequenceComplete() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val sequenceId = 42
        val captureSequence =
            createCaptureSequence(repeating = false, captureRequests = listOf(request1, request2))
                .apply { sequenceNumber = sequenceId }

        captureSequence.onCaptureCompleted(
            captureSession,
            request1,
            mockTotalCaptureResult(frameNumber),
        )
        verifyNoInteractions(sequenceListener)

        captureSequence.onCaptureSequenceAborted(captureSession, sequenceId)
        verify(sequenceListener).onCaptureSequenceComplete(captureSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun repeatingSequence_invokesOnCaptureSequenceCompleteOnlyOnSequenceCompleted() {
        val sequenceId = 10
        val captureFailure: CaptureFailure = mock()
        val repeatingSequence =
            createCaptureSequence(repeating = true, captureRequests = listOf(captureRequest))
                .apply { sequenceNumber = sequenceId }

        repeatingSequence.onCaptureCompleted(
            captureSession,
            captureRequest,
            mockTotalCaptureResult(frameNumber),
        )
        repeatingSequence.onCaptureCompleted(
            captureSession,
            captureRequest,
            mockTotalCaptureResult(frameNumber + 1),
        )
        repeatingSequence.onCaptureFailed(captureSession, captureRequest, captureFailure)
        repeatingSequence.onCaptureFailed(captureRequest, CameraFrameNumber(frameNumber + 2))
        verifyNoInteractions(sequenceListener)

        repeatingSequence.onCaptureSequenceCompleted(captureSession, sequenceId, frameNumber + 2)
        verify(sequenceListener).onCaptureSequenceComplete(repeatingSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun repeatingSequence_invokesOnCaptureSequenceCompleteOnlyOnSequenceAborted() {
        val sequenceId = 11
        val captureFailure: CaptureFailure = mock()
        val repeatingSequence =
            createCaptureSequence(repeating = true, captureRequests = listOf(captureRequest))
                .apply { sequenceNumber = sequenceId }

        repeatingSequence.onCaptureCompleted(
            captureSession,
            captureRequest,
            mockTotalCaptureResult(frameNumber),
        )
        repeatingSequence.onCaptureFailed(captureSession, captureRequest, captureFailure)
        verifyNoInteractions(sequenceListener)

        repeatingSequence.onCaptureSequenceAborted(captureSession, sequenceId)
        verify(sequenceListener).onCaptureSequenceComplete(repeatingSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    @Test
    fun repeatingBurstSequence_invokesOnCaptureSequenceCompleteOnlyOnSequenceCompleted() {
        val request1: CaptureRequest = mock()
        val request2: CaptureRequest = mock()
        val sequenceId = 12
        val repeatingBurstSequence =
            createCaptureSequence(repeating = true, captureRequests = listOf(request1, request2))
                .apply { sequenceNumber = sequenceId }

        repeatingBurstSequence.onCaptureCompleted(
            captureSession,
            request1,
            mockTotalCaptureResult(frameNumber),
        )
        repeatingBurstSequence.onCaptureCompleted(
            captureSession,
            request2,
            mockTotalCaptureResult(frameNumber + 1),
        )
        verifyNoInteractions(sequenceListener)

        repeatingBurstSequence.onCaptureSequenceCompleted(
            captureSession,
            sequenceId,
            frameNumber + 1,
        )
        verify(sequenceListener).onCaptureSequenceComplete(repeatingBurstSequence)
        verifyNoMoreInteractions(sequenceListener)
    }

    private fun createCaptureSequence(
        repeating: Boolean = false,
        captureRequests: List<CaptureRequest> = listOf(captureRequest),
        requestMetadataList: List<RequestMetadata> = captureRequests.mapIndexed { index, _ ->
            FakeRequestMetadata(
                request = Request(listOf(streamId), listeners = listeners),
                requestNumber = RequestNumber(index + 1L),
                repeating = repeating,
            )
        },
    ): Camera2CaptureSequence =
        Camera2CaptureSequence(
            cameraId = cameraId,
            repeating = repeating,
            captureRequestList = captureRequests,
            captureMetadataList = requestMetadataList,
            listeners = listeners,
            sequenceListener = sequenceListener,
            surfaceToStreamMap = mapOf(surface to streamId),
            surfaceToOutputMap = mapOf(surface to outputId),
            streamGraph = streamGraph,
            strictMode = StrictMode(true),
        )

    private fun mockTotalCaptureResult(frameNumber: Long): TotalCaptureResult =
        mock<TotalCaptureResult>().also { whenever(it.frameNumber).thenReturn(frameNumber) }

    private class FakeRequestListener : Listener {

        var lastFrameNumber: CameraFrameNumber? = null
        var lastTimeStamp: CameraTimestamp? = null
        var lastFrameInfo: FrameInfo? = null
        var lastRequestFailure: RequestFailure? = null
        var lastSensorTimeStamp: SensorTimestamp? = null
        var lastStreamId: StreamId? = null
        var lastOutputId: OutputId? = null

        override fun onStarted(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            timestamp: CameraTimestamp,
        ) {
            lastFrameNumber = frameNumber
            lastTimeStamp = timestamp
        }

        override fun onComplete(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            result: FrameInfo,
        ) {
            lastFrameNumber = frameNumber
            lastFrameInfo = result
        }

        override fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            requestFailure: RequestFailure,
        ) {
            lastFrameNumber = frameNumber
            lastRequestFailure = requestFailure
        }

        override fun onReadoutStarted(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            timestamp: SensorTimestamp,
        ) {
            lastFrameNumber = frameNumber
            lastSensorTimeStamp = timestamp
        }

        override fun onBufferLost(
            requestMetadata: RequestMetadata,
            frameNumber: CameraFrameNumber,
            streamId: StreamId,
            outputId: OutputId,
        ) {
            lastFrameNumber = frameNumber
            lastStreamId = streamId
            lastOutputId = outputId
        }
    }
}
