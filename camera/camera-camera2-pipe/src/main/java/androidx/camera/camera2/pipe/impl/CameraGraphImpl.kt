/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Stream
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic
import javax.inject.Inject

internal val cameraGraphIds = atomic(0)

@CameraGraphScope
class CameraGraphImpl @Inject constructor(
    graphConfig: CameraGraph.Config,
    metadata: CameraMetadata,
    private val graphProcessor: GraphProcessor,
    private val streamMap: StreamMap,
    private val graphState: GraphState,
    private val graphState3A: GraphState3A,
    private val listener3A: Listener3A
) : CameraGraph {
    private val debugId = cameraGraphIds.incrementAndGet()

    // Only one session can be active at a time.
    private val sessionLock = TokenLockImpl(1)

    private val controller3A = Controller3A(graphProcessor, graphState3A, listener3A)

    init {
        // Log out the configuration of the camera graph when it is created.
        Debug.logConfiguration(this.toString(), metadata, graphConfig, streamMap)
    }

    override val streams: Map<StreamConfig, Stream>
        get() = streamMap.streamConfigMap

    override fun start() {
        Debug.traceStart { "$this#start" }
        Log.info { "Starting $this" }
        graphState.start()
        Debug.traceStop()
    }

    override fun stop() {
        Debug.traceStart { "$this#stop" }
        Log.info { "Stopping $this" }
        graphState.stop()
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
        streamMap[stream] = surface
        Debug.traceStop()
    }

    override fun close() {
        Debug.traceStart { "$this#close" }
        Log.info { "Closing $this" }
        sessionLock.close()
        graphProcessor.close()
        graphState.stop()
        Debug.traceStop()
    }

    override fun toString(): String = "CameraGraph-$debugId"
}
