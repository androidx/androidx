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

import android.content.Context
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequest
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.test.TestScope

/**
 * This class creates a [CameraPipe] and [CameraGraph] instance using a [FakeCameraBackend].
 *
 * The CameraGraphSimulator is primarily intended to be used within a Kotlin `runTest` block, and
 * must be created with a coroutine scope by invoking [CameraGraphSimulator.create] and passing the
 * coroutine scope. This ensures that the created objects, dispatchers, and scopes correctly inherit
 * from the parent [TestScope].
 */
public class CameraGraphSimulator
internal constructor(
    private val cameraMetadata: CameraMetadata,
    private val cameraController: CameraControllerSimulator,
    private val fakeImageReaders: FakeImageReaders,
    private val fakeImageSources: FakeImageSources,
    private val realCameraGraph: CameraGraph,
    public val config: CameraGraph.Config,
) : CameraGraph by realCameraGraph, AutoCloseable, CameraSimulator {

    @Deprecated("CameraGraphSimulator directly implements CameraGraph")
    public val cameraGraph: CameraGraph
        get() = this

    public companion object {
        /**
         * Create a [CameraGraphSimulator] using the current [TestScope] provided by a Kotlin
         * `runTest` block. This will create the [CameraPipe] and [CameraGraph] using the parent
         * test scope, which helps ensure all long running operations are wrapped up by the time the
         * test completes and allows the test to provide more fine grained control over the
         * interactions.
         */
        public fun create(
            testScope: TestScope,
            testContext: Context,
            cameraMetadata: CameraMetadata,
            graphConfig: CameraGraph.Config,
        ): CameraGraphSimulator {

            val cameraPipeSimulator =
                CameraPipeSimulator.create(testScope, testContext, listOf(cameraMetadata))
            return cameraPipeSimulator.createCameraGraphSimulator(graphConfig)
        }

        /**
         * Create a single [CameraGraphSimulator] instance using the provided [Context] and
         * [CameraPipe.ThreadConfig]. This creates a unique [CameraGraph] and [CameraPipe] instance
         * for each test, while allowing all executors/dispatchers to be controlled from outside the
         * simulator instances.
         */
        public fun create(
            testContext: Context,
            testThreads: CameraPipe.ThreadConfig,
            testCamera: CameraMetadata,
            graphConfig: CameraGraph.Config,
        ): CameraGraphSimulator {
            val cameraPipeSimulator =
                CameraPipeSimulator.create(
                    testContext = testContext,
                    testThreads = testThreads,
                    testCameras = listOf(testCamera),
                )
            return cameraPipeSimulator.createCameraGraphSimulator(graphConfig)
        }
    }

    init {
        check(config.camera == cameraMetadata.camera) {
            "CameraGraphSimulator must be creating with a camera id that matches the provided " +
                "cameraMetadata! Received ${config.camera}, but expected " +
                "${cameraMetadata.camera}"
        }
    }

    private val closed = atomic(false)

    private val frameClockNanos = atomic(0L)
    private val frameCounter = atomic(0L)
    private val pendingFrameQueue = mutableListOf<FrameSimulator>()
    private val fakeSurfaces = FakeSurfaces()

    override val isClosed: Boolean
        get() = closed.value

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            realCameraGraph.close()
            fakeSurfaces.close()
        }
    }

    override fun simulateCameraStarted() {
        check(!closed.value) { "Cannot call simulateCameraStarted on $this after close." }
        cameraController.simulateCameraStarted()
    }

    override fun simulateCameraStopped() {
        check(!closed.value) { "Cannot call simulateCameraStopped on $this after close." }
        cameraController.simulateCameraStopped()
    }

    override fun simulateCameraModified() {
        check(!closed.value) { "Cannot call simulateCameraModified on $this after close." }
        cameraController.simulateCameraModified()
    }

    override fun simulateCameraError(graphStateError: GraphStateError) {
        check(!closed.value) { "Cannot call simulateCameraError on $this after close." }
        cameraController.simulateCameraError(graphStateError)
    }

    override fun initializeSurfaces() {
        check(!closed.value) {
            "Cannot call simulateFakeSurfaceConfiguration on $this after close."
        }
        for (stream in streams.streams) {
            val imageSource = fakeImageSources[stream.id]
            if (imageSource != null) {
                println("Using FakeImageSource ${imageSource.surface} for ${stream.id}")
                continue
            }

            val imageReader = fakeImageReaders[stream.id]
            if (imageReader != null) {
                println("Using FakeImageReader ${imageReader.surface} for ${stream.id}")
                realCameraGraph.setSurface(stream.id, imageReader.surface)
                continue
            }

            // Pick the smallest output (This matches the behavior of MultiResolutionImageReader)
            val minOutput = stream.outputs.minBy { it.size.width * it.size.height }
            val surface = fakeSurfaces.createFakeSurface(minOutput.size)

            println("Using Fake $surface for ${stream.id}")
            realCameraGraph.setSurface(stream.id, surface)
        }
    }

    override fun simulateNextFrame(advanceClockByNanos: Long): FrameSimulator =
        generateNextFrame().also {
            val clockNanos = frameClockNanos.addAndGet(advanceClockByNanos)
            it.simulateStarted(clockNanos)
        }

    private fun generateNextFrame(): FrameSimulator {
        val captureSequenceProcessor = cameraController.currentCaptureSequenceProcessor
        check(captureSequenceProcessor != null) {
            "simulateCameraStarted() must be called before frames can be created!"
        }

        // This checks the pending frame queue and polls for the next request. If no request is
        // available it will suspend until the next interaction with the request processor.
        if (pendingFrameQueue.isEmpty()) {
            val captureSequence = captureSequenceProcessor.nextCaptureSequence()
            checkNotNull(captureSequence) {
                "Failed to simulate a CaptureSequence from $captureSequenceProcessor! Make sure " +
                    "Requests have been submitted or that the repeating Request has been set."
            }

            // Each sequence is processed as a group, and if a sequence contains multiple requests
            // the list of requests is processed in order before polling the next sequence.
            for (request in captureSequence.captureRequestList) {
                pendingFrameQueue.add(FrameSimulator(request, captureSequence))
            }
        }
        return pendingFrameQueue.removeAt(0)
    }

    override fun simulateImage(streamId: StreamId, imageTimestamp: Long, outputId: OutputId?) {
        check(simulateImageInternal(streamId, outputId, imageTimestamp)) {
            "Failed to simulate image for $streamId on $this!"
        }
    }

    override fun simulateImages(
        request: Request,
        imageTimestamp: Long,
        physicalCameraId: CameraId?,
    ) {
        var imageSimulated = false
        for (streamId in request.streams) {
            val outputId =
                if (physicalCameraId == null) {
                    streams.outputs.single().id
                } else {
                    streams[streamId]?.outputs?.find { it.camera == physicalCameraId }?.id
                }
            val success = simulateImageInternal(streamId, outputId, imageTimestamp)
            imageSimulated = imageSimulated || success
        }

        check(imageSimulated) {
            "Failed to simulate images for $request!" +
                "No matching FakeImageReaders or FakeImageSources were found."
        }
    }

    private fun simulateImageInternal(
        streamId: StreamId,
        outputId: OutputId?,
        imageTimestamp: Long,
    ): Boolean {
        val stream = streams[streamId]
        checkNotNull(stream) { "Cannot simulate an image for invalid $streamId on $this!" }
        // Prefer to simulate images directly on the imageReader if possible, and then
        // defer to the imageSource if an imageReader does not exist.
        val imageReader = fakeImageReaders[streamId]
        if (imageReader != null) {
            imageReader.simulateImage(imageTimestamp = imageTimestamp, outputId = outputId)
            return true
        } else {
            val fakeImageSource = fakeImageSources[streamId]
            if (fakeImageSource != null) {
                fakeImageSource.simulateImage(timestamp = imageTimestamp, outputId = outputId)
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "CameraGraphSimulator($realCameraGraph)"
    }

    /**
     * A [FrameSimulator] allows a test to synchronously invoke callbacks. A single request can
     * generate multiple captures (eg, if used as a repeating request). A [FrameSimulator] allows a
     * test to control exactly one of those captures. This means that a new simulator is created for
     * each frame, and allows tests to simulate unusual ordering or delays that may appear under
     * real conditions.
     */
    public inner class FrameSimulator
    internal constructor(
        public val request: Request,
        public val requestSequence: FakeCaptureSequence,
    ) {
        private val requestMetadata = requestSequence.requestMetadata[request]!!

        public val frameNumber: FrameNumber = FrameNumber(frameCounter.incrementAndGet())
        public var timestampNanos: Long? = null

        public fun simulateStarted(timestampNanos: Long) {
            this.timestampNanos = timestampNanos

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onStarted(requestMetadata, frameNumber, CameraTimestamp(timestampNanos))
            }
        }

        public fun simulatePartialCaptureResult(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
        ) {
            val metadata =
                createFakeMetadataFor(
                    resultMetadata = resultMetadata,
                    extraResultMetadata = extraResultMetadata,
                    extraMetadata = extraMetadata,
                )

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onPartialCaptureResult(requestMetadata, frameNumber, metadata)
            }
        }

        public fun simulateTotalCaptureResult(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
            physicalResultMetadata: Map<CameraId, Map<CaptureResult.Key<*>, Any?>> = emptyMap(),
        ) {
            val metadata =
                createFakeMetadataFor(
                    resultMetadata = resultMetadata,
                    extraResultMetadata = extraResultMetadata,
                    extraMetadata = extraMetadata,
                )
            val frameInfo =
                FakeFrameInfo(
                    metadata = metadata,
                    requestMetadata,
                    createFakePhysicalMetadata(physicalResultMetadata),
                )

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)
            }
        }

        public fun simulateComplete(
            resultMetadata: Map<CaptureResult.Key<*>, Any?>,
            extraResultMetadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
            extraMetadata: Map<*, Any?> = emptyMap<Any, Any>(),
            physicalResultMetadata: Map<CameraId, Map<CaptureResult.Key<*>, Any?>> = emptyMap(),
        ) {
            val metadata =
                createFakeMetadataFor(
                    resultMetadata = resultMetadata,
                    extraResultMetadata = extraResultMetadata,
                    extraMetadata = extraMetadata,
                )
            val frameInfo =
                FakeFrameInfo(
                    metadata = metadata,
                    requestMetadata,
                    createFakePhysicalMetadata(physicalResultMetadata),
                )

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onComplete(requestMetadata, frameNumber, frameInfo)
            }
        }

        public fun simulateFailure(requestFailure: RequestFailure) {
            requestSequence.invokeOnRequest(requestMetadata) {
                it.onFailed(requestMetadata, frameNumber, requestFailure)
            }
        }

        public fun simulateBufferLoss(streamId: StreamId) {
            requestSequence.invokeOnRequest(requestMetadata) {
                it.onBufferLost(requestMetadata, frameNumber, streamId)
            }
        }

        public fun simulateAbort() {
            requestSequence.invokeOnRequest(requestMetadata) { it.onAborted(request) }
        }

        public fun simulateImage(
            streamId: StreamId,
            imageTimestamp: Long? = null,
            outputId: OutputId? = null,
        ) {
            val timestamp = imageTimestamp ?: timestampNanos
            checkNotNull(timestamp) {
                "Cannot simulate an image without a timestamp! Provide an " +
                    "imageTimestamp or call simulateStarted before simulateImage."
            }
            this@CameraGraphSimulator.simulateImage(streamId, timestamp, outputId)
        }

        /**
         * Utility function to simulate the production of [FakeImage]s for all outputs on a Frame.
         * Use [simulateImage] to directly control simulation of each individual image.
         */
        public fun simulateImages(
            imageTimestamp: Long? = null,
            physicalCameraId: CameraId? = null,
        ) {
            val timestamp = imageTimestamp ?: timestampNanos
            checkNotNull(timestamp) {
                "Cannot simulate an image without a timestamp! Provide an " +
                    "imageTimestamp or call simulateStarted before simulateImage."
            }
            this@CameraGraphSimulator.simulateImages(request, timestamp, physicalCameraId)
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
        ): FakeFrameMetadata =
            FakeFrameMetadata(
                camera = cameraMetadata.camera,
                frameNumber = frameNumber,
                resultMetadata = resultMetadata.toMap(),
                extraResultMetadata = extraResultMetadata.toMap(),
                extraMetadata = extraMetadata.toMap(),
            )
    }
}
