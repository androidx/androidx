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

package androidx.camera.camera2.pipe.framegraph

import android.view.Surface
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.FrameBuffer
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.Parameters
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.FrameGraphCoroutineScope
import androidx.camera.camera2.pipe.config.FrameGraphScope
import androidx.camera.camera2.pipe.internal.FrameDistributor
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

@FrameGraphScope
internal class FrameGraphImpl
@Inject
constructor(
    private val cameraGraph: CameraGraph,
    private val frameDistributor: FrameDistributor,
    private val frameGraphBuffers: FrameGraphBuffers,
    @FrameGraphCoroutineScope private val frameGraphCoroutineScope: CoroutineScope,
) : FrameGraph {
    override val streams = cameraGraph.streams

    override val graphState: StateFlow<GraphState> = cameraGraph.graphState

    override var isForeground: Boolean = cameraGraph.isForeground
    override val parameters: Parameters
        get() = cameraGraph.parameters

    override val id: CameraGraphId
        get() = cameraGraph.id

    init {
        // Wire up the frameStartedListener.
        frameDistributor.frameStartedListener = frameGraphBuffers
    }

    override fun start() {
        cameraGraph.start()
    }

    override fun stop() {
        cameraGraph.stop()
    }

    override fun setSurface(stream: StreamId, surface: Surface?) {
        cameraGraph.setSurface(stream, surface)
    }

    override fun captureWith(
        streamIds: Set<StreamId>,
        parameters: Map<Any, Any?>,
        capacity: Int,
    ): FrameBuffer {
        return frameGraphBuffers.attach(streamIds, parameters, capacity)
    }

    override fun updateAudioRestrictionMode(mode: AudioRestrictionMode) {
        cameraGraph.updateAudioRestrictionMode(mode)
    }

    override fun close() {
        cameraGraph.close()
    }

    override suspend fun acquireSession(): FrameGraph.Session {
        return FrameGraphSessionImpl(cameraGraph.acquireSession(), frameGraphBuffers)
    }

    override fun acquireSessionOrNull(): FrameGraph.Session? {
        return cameraGraph.acquireSessionOrNull()?.let {
            FrameGraphSessionImpl(it, frameGraphBuffers)
        }
    }

    override suspend fun <T> useSession(
        action: suspend CoroutineScope.(FrameGraph.Session) -> T
    ): T {
        return cameraGraph.useSession { cameraGraphSession ->
            FrameGraphSessionImpl(cameraGraphSession, frameGraphBuffers).use { action(it) }
        }
    }

    override fun <T> useSessionIn(
        scope: CoroutineScope,
        action: suspend CoroutineScope.(FrameGraph.Session) -> T,
    ): Deferred<T> {
        return cameraGraph.useSessionIn(scope) { cameraGraphSession ->
            FrameGraphSessionImpl(cameraGraphSession, frameGraphBuffers).use { action(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraGraph::class -> cameraGraph as T?
            else -> null
        }
}
