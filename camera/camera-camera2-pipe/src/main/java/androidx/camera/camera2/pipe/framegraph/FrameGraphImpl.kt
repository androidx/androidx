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

import android.hardware.camera2.params.MeteringRectangle
import android.view.Surface
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraControls3A
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.FrameBuffer
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Parameters
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestListeners
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.FrameGraphCoroutineScope
import androidx.camera.camera2.pipe.config.FrameGraphScope
import androidx.camera.camera2.pipe.graph.Controller3A
import androidx.camera.camera2.pipe.internal.FrameDistributor
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@FrameGraphScope
internal class FrameGraphImpl
@Inject
constructor(
    private val cameraGraph: CameraGraph,
    private val frameDistributor: FrameDistributor,
    private val frameGraphBuffers: FrameGraphBuffers,
    @FrameGraphCoroutineScope private val frameGraphCoroutineScope: CoroutineScope,
    private val controller3A: Controller3A,
    private val frameGraphFrameCaptureQueue: FrameGraphFrameCaptureQueue,
) : FrameGraph, CameraControls3A by cameraGraph {
    init {
        // Wire up the frameStartedListener.
        frameDistributor.frameStartedListener = frameGraphBuffers
    }

    override val streams = cameraGraph.streams

    override val graphState: StateFlow<GraphState> = cameraGraph.graphState
    override val latestFrameNumber: Flow<FrameNumber> = cameraGraph.latestFrameNumber
    override val latestFrameInfo: Flow<FrameInfo> = cameraGraph.latestFrameInfo

    override var isForeground: Boolean = cameraGraph.isForeground

    override fun setSurface(stream: StreamId, surface: Surface?) {
        cameraGraph.setSurface(stream, surface)
    }

    override fun updateAudioRestrictionMode(mode: AudioRestrictionMode) {
        cameraGraph.updateAudioRestrictionMode(mode)
    }

    override fun lock3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode?,
        convergedCondition: ((FrameMetadata) -> Boolean)?,
        lockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        convergedTimeLimitNs: Long,
        lockedTimeLimitNs: Long,
    ): Deferred<Result3A> =
        cameraGraph.lock3A(
            aeMode,
            afMode,
            awbMode,
            aeRegions,
            afRegions,
            awbRegions,
            aeLockBehavior,
            afLockBehavior,
            awbLockBehavior,
            afTriggerStartAeMode,
            convergedCondition,
            lockedCondition,
            frameLimit,
            convergedTimeLimitNs,
            lockedTimeLimitNs,
        )

    override fun unlock3A(
        ae: Boolean?,
        af: Boolean?,
        awb: Boolean?,
        unlockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long,
    ): Deferred<Result3A> =
        cameraGraph.unlock3A(ae, af, awb, unlockedCondition, frameLimit, timeLimitNs)

    override val parameters: Parameters
        get() = cameraGraph.parameters

    override val listeners: RequestListeners
        get() = cameraGraph.listeners

    override val id: CameraGraphId
        get() = cameraGraph.id

    override fun start() {
        cameraGraph.start()
    }

    override fun stop() {
        cameraGraph.stop()
    }

    override fun capture(request: Request): FrameCapture {
        return frameGraphFrameCaptureQueue.enqueue(request)
    }

    override fun capture(requests: List<Request>): List<FrameCapture> {
        return frameGraphFrameCaptureQueue.enqueue(requests)
    }

    override fun captureWith(
        streamIds: Set<StreamId>,
        parameters: Map<Any, Any?>,
        capacity: Int,
    ): FrameBuffer {
        return frameGraphBuffers.attach(streamIds, parameters, capacity)
    }

    override suspend fun acquireSession(): FrameGraph.Session {
        return createSession(cameraGraph.acquireSession())
    }

    override fun acquireSessionOrNull(): FrameGraph.Session? {
        return cameraGraph.acquireSessionOrNull()?.let { createSession(it) }
    }

    override suspend fun <T> useSession(
        action: suspend CoroutineScope.(FrameGraph.Session) -> T
    ): T {
        return cameraGraph.useSession { cameraGraphSession ->
            createSession(cameraGraphSession).use { action(it) }
        }
    }

    override fun <T> useSessionIn(
        scope: CoroutineScope,
        action: suspend CoroutineScope.(FrameGraph.Session) -> T,
    ): Deferred<T> {
        return cameraGraph.useSessionIn(scope) { cameraGraphSession ->
            createSession(cameraGraphSession).use { action(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CameraGraph::class -> cameraGraph as T?
            else -> null
        }

    override fun close() {
        frameGraphFrameCaptureQueue.close()
        cameraGraph.close()
        frameGraphCoroutineScope.cancel()
    }

    private fun createSession(cameraGraphSession: CameraGraph.Session): FrameGraph.Session {
        return FrameGraphSessionImpl(cameraGraphSession, frameGraphBuffers, controller3A)
    }
}
