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

import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Session
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.internal.CameraGraphParametersImpl
import androidx.camera.camera2.pipe.internal.CameraGraphRequestListenersImpl
import androidx.camera.camera2.pipe.internal.FrameCaptureQueue
import androidx.camera.camera2.pipe.internal.FrameDistributor
import androidx.camera.camera2.pipe.internal.GraphSessionLock
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

@CameraGraphScope
internal class CameraGraphImpl
@Inject
constructor(
    graphConfig: CameraGraph.Config,
    metadata: CameraMetadata,
    private val graphProcessor: GraphProcessor,
    private val graphListener: GraphListener,
    private val streamGraph: StreamGraphImpl,
    private val surfaceGraph: SurfaceGraph,
    private val cameraController: CameraController,
    private val frameDistributor: FrameDistributor,
    private val frameCaptureQueue: FrameCaptureQueue,
    private val audioRestrictionController: AudioRestrictionController,
    override val id: CameraGraphId,
    override val parameters: CameraGraphParametersImpl,
    override val listeners: CameraGraphRequestListenersImpl,
    private val sessionLock: GraphSessionLock,
    @ForCameraGraph private val graphScope: CoroutineScope,
    private val controller3A: Controller3A,
) : CameraGraph {
    private val closed = atomic(false)

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

        if (graphConfig.input != null) {
            require(graphConfig.input.isNotEmpty()) {
                "At least one InputConfiguration is required for reprocessing"
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                require(graphConfig.input.size <= 1) {
                    "Multi resolution reprocessing not supported under Android S"
                }
            }
        }

        // Ensure the CameraController is notified about the surfaces for internally created
        // ImageSource(s) during graph construction.
        if (streamGraph.imageSourceMap.isNotEmpty()) {
            surfaceGraph.maybeUpdateSurfaces()
        }
    }

    override val streams: StreamGraph
        get() = streamGraph

    override val graphState: StateFlow<GraphState>
        get() = graphProcessor.graphState

    override val latestFrameNumber: Flow<FrameNumber>
        get() = callbackFlow {
            val listener = LatestFrameNumberListener { trySend(it) }
            listeners.add(listener)
            awaitClose { listeners.remove(listener) }
        }

    override val latestFrameInfo: Flow<FrameInfo>
        get() = callbackFlow {
            val listener = LatestFrameInfoListener { trySend(it) }
            listeners.add(listener)
            awaitClose { listeners.remove(listener) }
        }

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
        cameraController.start()
        Debug.traceStop()
    }

    override fun stop() {
        check(!closed.value) { "Cannot stop $this after calling close()" }

        Debug.traceStart { "$this#stop" }
        Log.info { "Stopping $this" }
        graphListener.onGraphStopping()
        cameraController.stop()
        Debug.traceStop()
    }

    override suspend fun acquireSession(): Session {
        // Step 1: Acquire a lock on the session mutex, which returns a releasable token. This may
        //         or may not suspend.
        val token = sessionLock.acquireToken()

        // Step 2: Return a session that can be used to interact with the session. The session must
        //         be closed when it is no longer needed.
        return createSessionFromToken(token)
    }

    override fun acquireSessionOrNull(): Session? {
        val token = sessionLock.tryAcquireToken() ?: return null
        return createSessionFromToken(token)
    }

    override suspend fun <T> useSession(action: suspend CoroutineScope.(Session) -> T): T =
        acquireSession().use {
            // Wrap the block in a coroutineScope to ensure all operations are completed before
            // releasing the lock.
            coroutineScope { action(it) }
        }

    override fun <T> useSessionIn(
        scope: CoroutineScope,
        action: suspend CoroutineScope.(Session) -> T,
    ): Deferred<T> {
        return sessionLock.withTokenIn(scope) { token ->
            // Create and use the session
            createSessionFromToken(token).use { session ->
                // Wrap the block in a coroutineScope to ensure all operations are completed
                // before exiting and releasing the lock. The lock can be released early if the
                // calling action decides to call session.close() early.
                coroutineScope { action(session) }
            }
        }
    }

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

    override fun update3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> = withSessionLockAsync {
        controller3A.update3A(
            aeMode = aeMode,
            afMode = afMode,
            awbMode,
            aeRegions = aeRegions,
            afRegions = afRegions,
            awbRegions = awbRegions,
        )
    }

    override fun submit3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> = withSessionLockAsync {
        controller3A.submit3A(aeMode, afMode, awbMode, aeRegions, afRegions, awbRegions)
    }

    override fun setTorchOn(): Deferred<Result3A> = withSessionLockAsync {
        controller3A.setTorchOn()
    }

    override fun setTorchOff(aeMode: AeMode?): Deferred<Result3A> = withSessionLockAsync {
        controller3A.setTorchOff(aeMode)
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
    ): Deferred<Result3A> = withSessionLockAsync {
        controller3A.lock3A(
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
    }

    override fun unlock3A(
        ae: Boolean?,
        af: Boolean?,
        awb: Boolean?,
        unlockedCondition: ((FrameMetadata) -> Boolean)?,
        frameLimit: Int,
        timeLimitNs: Long,
    ): Deferred<Result3A> = withSessionLockAsync {
        controller3A.unlock3A(ae, af, awb, unlockedCondition, frameLimit, timeLimitNs)
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            Debug.traceStart { "$this#close" }
            Log.info { "Closing $this" }
            graphProcessor.close()
            cameraController.close()
            frameDistributor.close()
            frameCaptureQueue.close()
            surfaceGraph.close()
            streamGraph.close()
            audioRestrictionController.removeCameraGraph(this)
            graphScope.cancel()
            Debug.traceStop()
        }
    }

    override fun toString(): String = id.toString()

    private fun createSessionFromToken(token: Token) =
        CameraGraphSessionImpl(
            token,
            graphProcessor,
            controller3A,
            frameCaptureQueue,
            parameters,
            listeners,
        )

    /**
     * Acquires a [GraphSessionLock] token and executes the given code block. The code block(s) will
     * execute in the same order as they were invoked. This method uses [graphScope]. See
     * [useSessionIn] for further reference. This method additionally chains the Deferred<T> return
     * type of the code block, to its own return type. The other advantage of this method as
     * compared to [useSessionIn] is that it doesn't create a [Session] object, however it should be
     * noted that any camera state changes, like parameter updates, that the [Session]'s init or
     * close block handles, will be skipped, so invoke them separately if needed.
     */
    private fun <T> withSessionLockAsync(
        block: suspend CoroutineScope.() -> Deferred<T>
    ): Deferred<T> = sessionLock.withTokenInAsync(graphScope) { coroutineScope { block() } }
}
