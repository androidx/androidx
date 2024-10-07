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

package androidx.camera.camera2.pipe.graph

import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.core.acquireToken
import androidx.camera.camera2.pipe.core.acquireTokenAndSuspend
import androidx.camera.camera2.pipe.core.tryAcquireToken
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.GraphLifecycleManager
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

@CameraGraphScope
internal class CameraGraphImpl
@Inject
constructor(
    graphConfig: CameraGraph.Config,
    metadata: CameraMetadata,
    private val graphLifecycleManager: GraphLifecycleManager,
    private val graphProcessor: GraphProcessor,
    private val graphListener: GraphListener,
    private val streamGraph: StreamGraphImpl,
    private val surfaceGraph: SurfaceGraph,
    private val cameraBackend: CameraBackend,
    private val cameraController: CameraController,
    private val graphState3A: GraphState3A,
    private val listener3A: Listener3A,
    private val frameDistributor: FrameDistributor,
    private val frameCaptureQueue: FrameCaptureQueue,
    private val audioRestrictionController: AudioRestrictionController,
    override val id: CameraGraphId,
    override val parameters: CameraGraph.Parameters
) : CameraGraph {
    private val sessionMutex = Mutex()
    private val controller3A = Controller3A(graphProcessor, metadata, graphState3A, listener3A)
    private val closed = atomic(false)

    // TODO(amycao): b/354899829 CameraGraph.Session#close() should build and update repeating
    // request

    init {
        // Log out the configuration of the camera graph when it is created.
        Log.info { Debug.formatCameraGraphProperties(metadata, graphConfig, this) }

        // Enforce preview and video stream use cases for high speed sessions
        if (graphConfig.sessionMode == CameraGraph.OperatingMode.HIGH_SPEED) {
            require(streamGraph.outputs.isNotEmpty()) {
                "Cannot create a HIGH_SPEED CameraGraph without outputs."
            }
            require(streamGraph.outputs.size <= 2) {
                "Cannot create a HIGH_SPEED CameraGraph with more than two outputs. " +
                    "Configured outputs are ${streamGraph.outputs}"
            }

            // Streams must be preview and/or video for high speed sessions
            val allStreamsValidForHighSpeedOperatingMode =
                this.streamGraph.outputs.all { it.isValidForHighSpeedOperatingMode() }

            require(allStreamsValidForHighSpeedOperatingMode) {
                "HIGH_SPEED CameraGraph must only contain Preview and/or Video " +
                    "streams. Configured outputs are ${streamGraph.outputs}"
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            require(graphConfig.input == null) { "Reprocessing not supported under Android M" }
        }
        if (graphConfig.input != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            require(graphConfig.input.isNotEmpty()) {
                "At least one InputConfiguration is required for reprocessing"
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                require(graphConfig.input.size <= 1) {
                    "Multi resolution reprocessing not supported under Android S"
                }
            }
        }
    }

    override val streams: StreamGraph
        get() = streamGraph

    override val graphState: StateFlow<GraphState>
        get() = graphProcessor.graphState

    override var isForeground: Boolean = true
        set(value) {
            field = value
            cameraController.isForeground = value
        }

    override fun start() {
        check(!closed.value) { "Cannot start $this after calling close()" }

        Debug.traceStart { "$this#start" }
        Log.info { "Starting $this" }
        graphListener.onGraphStarting()
        graphLifecycleManager.monitorAndStart(cameraBackend, cameraController)
        Debug.traceStop()
    }

    override fun stop() {
        check(!closed.value) { "Cannot stop $this after calling close()" }

        Debug.traceStart { "$this#stop" }
        Log.info { "Stopping $this" }
        graphListener.onGraphStopping()
        graphLifecycleManager.monitorAndStop(cameraBackend, cameraController)
        Debug.traceStop()
    }

    override suspend fun acquireSession(): CameraGraph.Session {
        // Step 1: Acquire a lock on the session mutex, which returns a releasable token. This may
        //         or may not suspend.
        val token = sessionMutex.acquireToken()

        // Step 2: Return a session that can be used to interact with the session. The session must
        //         be closed when it is no longer needed.
        return createSessionFromToken(token)
    }

    override fun acquireSessionOrNull(): CameraGraph.Session? {
        val token = sessionMutex.tryAcquireToken() ?: return null
        return createSessionFromToken(token)
    }

    override suspend fun <T> useSession(
        action: suspend CoroutineScope.(CameraGraph.Session) -> T
    ): T =
        acquireSession().use {
            // Wrap the block in a coroutineScope to ensure all operations are completed before
            // releasing the lock.
            coroutineScope { action(it) }
        }

    override fun <T> useSessionIn(
        scope: CoroutineScope,
        action: suspend CoroutineScope.(CameraGraph.Session) -> T
    ): Deferred<T> {
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1578
        // To handle `runBlocking` we need to use `job.complete()` in `result.invokeOnCompletion`.
        // However, if we do this directly on the scope that is provided it will cause
        // SupervisorScopes to block and never complete. To work around this, we create a childJob,
        // propagate the existing context, and use that as the context for scope.async.
        val childJob = Job(scope.coroutineContext[Job])
        val context = scope.coroutineContext + childJob
        val result =
            scope.async(context = context, start = CoroutineStart.UNDISPATCHED) {
                ensureActive() // Exit early if the parent scope has been canceled.

                // It is very important to acquire *and* suspend here. Invoking a coroutine using
                // UNDISPATCHED will execute on the current thread until the suspension point, and
                // this will force the execution to switch to the provided scope after ensuring the
                // lock is acquired or in the queue. This guarantees exclusion, ordering, and
                // execution within the correct scope.
                val token = sessionMutex.acquireTokenAndSuspend()

                // Create and use the session
                createSessionFromToken(token).use {
                    // Wrap the block in a coroutineScope to ensure all operations are completed
                    // before exiting and releasing the lock. The lock can be released early if the
                    // calling action decides to call session.close() early.
                    coroutineScope { action(it) }
                }
            }

        result.invokeOnCompletion { childJob.complete() }
        return result
    }

    private fun createSessionFromToken(token: Token) =
        CameraGraphSessionImpl(token, graphProcessor, controller3A, frameCaptureQueue)

    override fun setSurface(stream: StreamId, surface: Surface?) {
        Debug.traceStart { "$stream#setSurface" }
        if (surface != null && !surface.isValid) {
            Log.warn { "$this#setSurface: $surface is invalid" }
        }
        surfaceGraph[stream] = surface
        Debug.traceStop()
    }

    override fun updateAudioRestrictionMode(mode: AudioRestrictionMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            audioRestrictionController.updateCameraGraphAudioRestrictionMode(this, mode)
        }
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            Debug.traceStart { "$this#close" }
            Log.info { "Closing $this" }
            graphProcessor.close()
            graphLifecycleManager.monitorAndClose(cameraBackend, cameraController)
            frameDistributor.close()
            frameCaptureQueue.close()
            surfaceGraph.close()
            audioRestrictionController.removeCameraGraph(this)
            Debug.traceStop()
        }
    }

    override fun toString(): String = id.toString()
}
