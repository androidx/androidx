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
    private val graphState: GraphState
) : CameraGraph {
    private val debugId = cameraGraphIds.incrementAndGet()

    // Only one session can be active at a time.
    private val sessionLock = TokenLockImpl(1)

    init {
        // Log out the configuration of the camera graph when it is created.
        Debug.logConfiguration(this.toString(), metadata, graphConfig, streamMap)
    }

    override val streams: Map<StreamConfig, Stream>
        get() = streamMap.streamConfigMap

    override fun start() {
        Log.info { "Starting $this" }
        graphState.start()
    }

    override fun stop() {
        Log.info { "Stopping $this" }
        graphState.stop()
    }

    override suspend fun acquireSession(): CameraGraph.Session {
        val token = sessionLock.acquire(1)
        return CameraGraphSessionImpl(token, graphProcessor)
    }

    override fun acquireSessionOrNull(): CameraGraph.Session? {
        val token = sessionLock.acquireOrNull(1) ?: return null
        return CameraGraphSessionImpl(token, graphProcessor)
    }

    override fun setSurface(stream: StreamId, surface: Surface?) {
        streamMap[stream] = surface
    }

    override fun close() {
        Log.info { "Closing $this" }
        sessionLock.close()
        graphProcessor.close()
        graphState.stop()
    }

    override fun toString(): String = "CameraGraph-$debugId"
}