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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureResult
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraPipe.CameraBackendConfig
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequest
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout

/**
 * This class creates a [CameraPipe] and [CameraGraph] instance using a [FakeCameraBackend].
 *
 * The CameraGraphSimulator is primarily intended to be used within a Kotlin `runTest` block, and
 * must be created with a coroutine scope by invoking [CameraGraphSimulator.create] and passing the
 * coroutine scope. This ensures that the created objects, dispatchers, and scopes correctly inherit
 * from the parent [TestScope].
 *
 * The simulator does not make (many) assumptions about how the simulator will be used, and for this
 * reason it does not automatically put the underlying graph into a "started" state. In most cases,
 * the test will need start the [CameraGraph], [simulateCameraStarted], and either configure
 * surfaces for the [CameraGraph] or call [simulateFakeSurfaceConfiguration] to put the graph into a
 * state where it is able to send and simulate interactions with the camera. This mirrors the normal
 * lifecycle of a [CameraGraph]. Tests using CameraGraphSimulators should also close them after
 * they've completed their use of the simulator.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraGraphSimulator private constructor(
    val context: Context,
    val cameraMetadata: CameraMetadata,
    val graphConfig: CameraGraph.Config,
    val cameraGraph: CameraGraph,
    private val cameraController: CameraControllerSimulator
) : AutoCloseable {
    companion object {
        /**
         * Create a CameraGraphSimulator using the current [TestScope] provided by a Kotlin
         * `runTest` block. This will create the [CameraPipe] and [CameraGraph] using the parent
         * test scope, which helps ensure all long running operations are wrapped up by the time
         * the test completes and allows the test to provide more fine grained control over the
         * interactions.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        fun create(
            scope: TestScope,
            context: Context,
            cameraMetadata: CameraMetadata,
            graphConfig: CameraGraph.Config
        ): CameraGraphSimulator {
            val fakeCameraBackend = FakeCameraBackend(
                fakeCameras = mapOf(cameraMetadata.camera to cameraMetadata)
            )
            val cameraPipe = CameraPipe(
                CameraPipe.Config(
                    context,
                    cameraBackendConfig = CameraBackendConfig(
                        internalBackend = fakeCameraBackend
                    ),
                    threadConfig = CameraPipe.ThreadConfig(
                        testOnlyDispatcher = StandardTestDispatcher(scope.testScheduler),
                        testOnlyScope = scope,
                    )
                )
            )
            val cameraGraph = cameraPipe.create(graphConfig)
            val cameraController = checkNotNull(fakeCameraBackend.cameraControllers.lastOrNull()) {
                "Expected cameraPipe.create to create a CameraController instance from " +
                    "$fakeCameraBackend as part of its initialization."
            }
            return CameraGraphSimulator(
                context,
                cameraMetadata,
                graphConfig,
                cameraGraph,
                cameraController
            )
        }
    }

    init {
        check(graphConfig.camera == cameraMetadata.camera) {
            "CameraGraphSimulator must be creating with a camera id that matches the provided " +
                "cameraMetadata! Received ${graphConfig.camera}, but expected " +
                "${cameraMetadata.camera}"
        }
    }

    private val closed = atomic(false)

    private val surfaceTextureNames = atomic(0)
    private val frameClockNanos = atomic(0L)
    private val frameCounter = atomic(0L)
    private val pendingFrameQueue = mutableListOf<FrameSimulator>()
    private val surfacesCreated = mutableSetOf<Surface>()

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            cameraGraph.close()
            for (surface in surfacesCreated) {
                surface.release()
            }
            surfacesCreated.clear()
        }
    }

    fun simulateCameraStarted() {
        check(!closed.value) { "Cannot call simulateCameraStarted on $this after close." }
        cameraController.simulateCameraStarted()
    }

    fun simulateCameraStopped() {
        check(!closed.value) { "Cannot call simulateCameraStopped on $this after close." }
        cameraController.simulateCameraStopped()
    }

    fun simulateCameraModified() {
        check(!closed.value) { "Cannot call simulateCameraModified on $this after close." }
        cameraController.simulateCameraModified()
    }

    fun simulateCameraError(graphStateError: GraphStateError) {
        check(!closed.value) { "Cannot call simulateCameraError on $this after close." }
        cameraController.simulateCameraError(graphStateError)
    }

    fun simulateFakeSurfaceConfiguration() {
        check(!closed.value) {
            "Cannot call simulateFakeSurfaceConfiguration on $this after close."
        }
        for (stream in cameraGraph.streams.streams) {
            // Pick an output -- most will only have one.
            val output = stream.outputs.first()
            val surface = Surface(
                SurfaceTexture(surfaceTextureNames.getAndIncrement()).also {
                    it.setDefaultBufferSize(output.size.width, output.size.height)
                }
            )
            surfacesCreated.add(surface)
            cameraGraph.setSurface(stream.id, surface)
        }
    }

    suspend fun simulateNextFrame(
        advanceClockByNanos: Long = 33_366_666 // (2_000_000_000 / (60  / 1.001))
    ): FrameSimulator = generateNextFrame().also {
        val clockNanos = frameClockNanos.addAndGet(advanceClockByNanos)
        it.simulateStarted(clockNanos)
    }

    private suspend fun generateNextFrame(): FrameSimulator {
        val captureSequenceProcessor = cameraController.currentCaptureSequenceProcessor
        check(captureSequenceProcessor != null) {
            "simulateCameraStarted() must be called before frames can be created!"
        }

        // This checks the pending frame queue and polls for the next request. If no request is
        // available it will suspend until the next interaction with the request processor.
        if (pendingFrameQueue.isEmpty()) {
            val requestSequence =
                withTimeout(timeMillis = 250) { captureSequenceProcessor.nextRequestSequence() }

            // Each sequence is processed as a group, and if a sequence contains multiple requests
            // the list of requests is processed in order before polling the next sequence.
            for (request in requestSequence.captureRequestList) {
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
        val requestSequence: FakeCaptureSequence,
    ) {
        private val requestMetadata = requestSequence.requestMetadata[request]!!

        val frameNumber: FrameNumber = FrameNumber(frameCounter.incrementAndGet())
        var timestampNanos: Long? = null

        fun simulateStarted(timestampNanos: Long) {
            this.timestampNanos = timestampNanos

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onStarted(requestMetadata, frameNumber, CameraTimestamp(timestampNanos))
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

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onPartialCaptureResult(requestMetadata, frameNumber, metadata)
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

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)
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

            requestSequence.invokeOnRequest(requestMetadata) {
                it.onComplete(requestMetadata, frameNumber, frameInfo)
            }
        }

        fun simulateFailure(requestFailure: RequestFailure) {
            requestSequence.invokeOnRequest(requestMetadata) {
                it.onFailed(requestMetadata, frameNumber, requestFailure)
            }
        }

        fun simulateBufferLoss(streamId: StreamId) {
            requestSequence.invokeOnRequest(requestMetadata) {
                it.onBufferLost(requestMetadata, frameNumber, streamId)
            }
        }

        fun simulateAbort() {
            requestSequence.invokeOnRequest(requestMetadata) {
                it.onAborted(request)
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
            camera = cameraMetadata.camera,
            frameNumber = frameNumber,
            resultMetadata = resultMetadata.toMap(),
            extraResultMetadata = extraResultMetadata.toMap(),
            extraMetadata = extraMetadata.toMap()
        )
    }
}
