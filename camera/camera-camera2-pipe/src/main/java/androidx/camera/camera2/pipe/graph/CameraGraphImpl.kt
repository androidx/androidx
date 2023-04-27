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

import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.TokenLockImpl
import androidx.camera.camera2.pipe.core.acquire
import androidx.camera.camera2.pipe.core.acquireOrNull
import androidx.camera.camera2.pipe.internal.GraphLifecycleManager
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.StateFlow

internal val cameraGraphIds = atomic(0)

@RequiresApi(21)
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
    private val listener3A: Listener3A
) : CameraGraph {
    private val debugId = cameraGraphIds.incrementAndGet()

    // Only one session can be active at a time.
    private val sessionLock = TokenLockImpl(1)

    private val controller3A = Controller3A(graphProcessor, metadata, graphState3A, listener3A)

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
            val containsPreviewStream =
                this.streamGraph.outputs.any {
                    it.streamUseCase == OutputStream.StreamUseCase.PREVIEW
                }
            val containsVideoStream =
                this.streamGraph.outputs.any {
                    it.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD
                }
            if (streamGraph.outputs.size == 2) {
                require(containsPreviewStream) {
                    "Cannot create a HIGH_SPEED CameraGraph without setting the Preview " +
                        "Video stream. Configured outputs are ${streamGraph.outputs}"
                }
            } else {
                require(containsPreviewStream || containsVideoStream) {
                    "Cannot create a HIGH_SPEED CameraGraph without having a Preview or Video " +
                        "stream. Configured outputs are ${streamGraph.outputs}"
                }
            }
        }
    }

    override val streams: StreamGraph
        get() = streamGraph

    override val graphState: StateFlow<GraphState>
        get() = graphProcessor.graphState

    override fun start() {
        Debug.traceStart { "$this#start" }
        Log.info { "Starting $this" }
        graphListener.onGraphStarting()
        graphLifecycleManager.monitorAndStart(cameraBackend, cameraController)
        Debug.traceStop()
    }

    override fun stop() {
        Debug.traceStart { "$this#stop" }
        Log.info { "Stopping $this" }
        graphListener.onGraphStopping()
        graphLifecycleManager.monitorAndStop(cameraBackend, cameraController)
        Debug.traceStop()
    }

    override suspend fun acquireSession(): CameraGraph.Session {
        Debug.traceStart { "$this#acquireSession" }
        val token = sessionLock.acquire(1)
        val session = CameraGraphSessionImpl(token, graphProcessor, controller3A)
        Debug.traceStop()
        return session
    }

    override fun acquireSessionOrNull(): CameraGraph.Session? {
        Debug.traceStart { "$this#acquireSessionOrNull" }
        val token = sessionLock.acquireOrNull(1) ?: return null
        val session = CameraGraphSessionImpl(token, graphProcessor, controller3A)
        Debug.traceStop()
        return session
    }

    override fun setSurface(stream: StreamId, surface: Surface?) {
        Debug.traceStart { "$stream#setSurface" }
        if (surface != null && !surface.isValid) {
            Log.warn { "$this#setSurface: $surface is invalid" }
        }
        surfaceGraph[stream] = surface
        Debug.traceStop()
    }

    override fun close() {
        Debug.traceStart { "$this#close" }
        Log.info { "Closing $this" }
        sessionLock.close()
        graphProcessor.close()
        graphLifecycleManager.monitorAndClose(cameraBackend, cameraController)
        surfaceGraph.close()
        Debug.traceStop()
    }

    override fun toString(): String = "CameraGraph-$debugId"
}
