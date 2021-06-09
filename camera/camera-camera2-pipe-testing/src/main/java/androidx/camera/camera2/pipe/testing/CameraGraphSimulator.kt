/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.withTimeout

/** Simulator for observing and responding to interactions with the a [CameraGraph]. */
class CameraGraphSimulator(
    private val config: CameraGraph.Config,
    cameraMetadata: CameraMetadata
) {
    init {
        check(config.camera == cameraMetadata.camera)
    }

    private val fakeRequestProcessor = FakeRequestProcessor()
    private val cameraPipe = CameraPipe.External()
    public val cameraGraph = cameraPipe.create(
        config,
        cameraMetadata,
        fakeRequestProcessor
    )

    private var frameClockNanos = atomic(0L)
    private var frameCounter = atomic(0L)
    private val pendingFrameQueue = mutableListOf<FrameSimulator>()

    suspend fun simulateNextFrame(
        advanceClockByNanos: Long = 33_366_666 // (2_000_000_000 / (60  / 1.001))
    ): FrameSimulator = generateNextFrame().also {
        val clockNanos = frameClockNanos.addAndGet(advanceClockByNanos)
        it.simulateStarted(clockNanos)
    }

    private suspend fun generateNextFrame(): FrameSimulator {
        // This checks the pending frame queue and polls for the next request. If no request is
        // available it will suspend until the next interaction with the request processor.
        if (pendingFrameQueue.isEmpty()) {
            val requestSequence =
                withTimeout(timeMillis = 250) { fakeRequestProcessor.nextRequestSequence() }

            // Each sequence is processed as a group, and if a sequence contains multiple requests
            // the list of requests is processed in order before polling the next sequence.
            for (request in requestSequence.requests) {
                pendingFrameQueue.add(FrameSimulator(request, requestSequence))
            }
        }
        return pendingFrameQueue.removeFirst()
    }

    /**
     * A [FrameSimulator] allows a test to synchronously invoke callbacks. A single request can
     * generate multiple captures (eg, if used as a repeating request). A [FrameSimulator] allows
     * a test to control exactly one of those captures. This means that a new simulator is
     * created for each frame, and allows tests to simulate unusual ordering or delays that may
     * appear under real conditions.
     */
    inner class FrameSimulator internal constructor(
        val request: Request,
        val requestSequence: FakeRequestProcessor.RequestSequence,
    ) {
        private val requestMetadata = requestSequence.requestMetadata[request]!!
        private val requestListeners = requestSequence.requestListeners[request]!!

        val frameNumber: FrameNumber = FrameNumber(frameCounter.incrementAndGet())
        var timestampNanos: Long? = null

        fun simulateStarted(timestampNanos: Long) {
            this.timestampNanos = timestampNanos

            for (listener in requestListeners) {
                listener.onStarted(requestMetadata, frameNumber, CameraTimestamp(timestampNanos))
            }
        }

        fun simulatePartialCaptureResult(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>()
        ) {
            val metadata = createFakeMetadataFor(
                resultMetadata = resultMetadata,
                extraResultMetadata = extraResultMetadata,
                extraMetadata = extraMetadata
            )

            for (listener in requestListeners) {
                listener.onPartialCaptureResult(requestMetadata, frameNumber, metadata)
            }
        }

        fun simulateTotalCaptureResult(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
            physicalResultMetadata: Map<CameraId, Map<CaptureResult.Key<*>, Any?>> = emptyMap()
        ) {
            val metadata = createFakeMetadataFor(
                resultMetadata = resultMetadata,
                extraResultMetadata = extraResultMetadata,
                extraMetadata = extraMetadata
            )
            val frameInfo = FakeFrameInfo(
                metadata = metadata, requestMetadata,
                createFakePhysicalMetadata(physicalResultMetadata)
            )

            for (listener in requestListeners) {
                listener.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)
            }
        }

        fun simulateComplete(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
            physicalResultMetadata: Map<CameraId, Map<CaptureResult.Key<*>, Any?>> = emptyMap()
        ) {
            val metadata = createFakeMetadataFor(
                resultMetadata = resultMetadata,
                extraResultMetadata = extraResultMetadata,
                extraMetadata = extraMetadata
            )
            val frameInfo = FakeFrameInfo(
                metadata = metadata, requestMetadata,
                createFakePhysicalMetadata(physicalResultMetadata)
            )

            for (listener in requestListeners) {
                listener.onComplete(requestMetadata, frameNumber, frameInfo)
            }
        }

        fun simulateFailure(captureFailure: CaptureFailure) {
            for (listener in requestListeners) {
                listener.onFailed(requestMetadata, frameNumber, captureFailure)
            }
        }

        fun simulateBufferLoss(streamId: StreamId) {
            for (listener in requestListeners) {
                listener.onBufferLost(requestMetadata, frameNumber, streamId)
            }
        }

        fun simulateAbort() {
            for (listener in requestListeners) {
                listener.onAborted(request)
            }
        }

        private fun createFakePhysicalMetadata(
            physicalResultMetadata: Map<CameraId, Map<CaptureResult.Key<*>, Any?>>
        ): Map<CameraId, FrameMetadata> {
            val resultMap = mutableMapOf<CameraId, FrameMetadata>()
            for ((k, v) in physicalResultMetadata) {
                resultMap[k] = createFakeMetadataFor(v)
            }
            return resultMap
        }

        private fun createFakeMetadataFor(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
        ): FakeFrameMetadata = FakeFrameMetadata(
            camera = config.camera,
            frameNumber = frameNumber,
            resultMetadata = resultMetadata.toMap(),
            extraResultMetadata = extraResultMetadata.toMap(),
            extraMetadata = extraMetadata.toMap()
        )
    }
}