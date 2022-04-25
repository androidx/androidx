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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.RequestProcessor
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Collections.synchronizedMap

internal val virtualSessionDebugIds = atomic(0)

/**
 * This class encapsulates the state and logic required to create and start a CaptureSession.
 *
 * After being created, it will wait for a valid CameraDevice and Surfaces that it will use
 * to create and start the capture session. Calling shutdown or disconnect will release the current
 * session (if one has been configured), and prevent / close any session that was in the process of
 * being created when shutdown / disconnect was called.
 */
internal class VirtualSessionState(
    private val graphListener: GraphListener,
    private val captureSessionFactory: CaptureSessionFactory,
    private val requestProcessorFactory: Camera2RequestProcessorFactory,
    private val scope: CoroutineScope
) : CameraCaptureSessionWrapper.StateCallback, Camera2StreamGraph.SurfaceListener {
    private val debugId = virtualSessionDebugIds.incrementAndGet()
    private val lock = Any()

    private val activeSurfaceMap = synchronizedMap(HashMap<StreamId, Surface>())

    private var sessionCreatingTimestamp: TimestampNs? = null

    @GuardedBy("lock")
    private var _cameraDevice: CameraDeviceWrapper? = null
    var cameraDevice: CameraDeviceWrapper?
        get() = synchronized(lock) { _cameraDevice }
        set(value) = synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return
            }

            _cameraDevice = value
            if (value != null) {
                scope.launch { tryCreateCaptureSession() }
            }
        }

    @GuardedBy("lock")
    private var cameraCaptureSession: ConfiguredCameraCaptureSession? = null

    @GuardedBy("lock")
    private var pendingOutputMap: Map<StreamId, OutputConfigurationWrapper>? = null

    @GuardedBy("lock")
    private var pendingSurfaceMap: Map<StreamId, Surface>? = null

    @GuardedBy("lock")
    private var state = State.PENDING

    private enum class State {
        PENDING,
        CREATING,
        CREATED,
        CLOSING,
        CLOSED
    }

    @GuardedBy("lock")
    private var _surfaceMap: Map<StreamId, Surface>? = null
    override fun onSurfaceMapUpdated(surfaces: Map<StreamId, Surface>) {
        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return@synchronized
            }

            _surfaceMap = surfaces

            val pendingOutputs = pendingOutputMap
            if (pendingOutputs != null && pendingSurfaceMap == null) {

                // Filter the list of current surfaces down ones that are present in the set of
                // deferred outputs.
                val pendingSurfaces = surfaces.filter { pendingOutputs.containsKey(it.key) }

                // We can only invoke finishDeferredOutputs after we have a surface for ALL
                // of the deferred outputs.
                if (pendingSurfaces.size == pendingOutputs.size) {
                    pendingSurfaceMap = pendingSurfaces
                    scope.launch { finalizeOutputsIfAvailable() }
                }
            }
            scope.launch { tryCreateCaptureSession() }
        }
    }

    override fun onActive(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Active" }
    }

    override fun onClosed(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Closed" }
        Debug.traceStart { "$this#onClosed" }
        shutdown()
        Debug.traceStop()
    }

    override fun onConfigureFailed(session: CameraCaptureSessionWrapper) {
        Log.warn { "Failed to configure $this" }
        Debug.traceStart { "$this#onConfigureFailed" }
        shutdown()
        Debug.traceStop()
    }

    override fun onConfigured(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Configured" }
        Debug.traceStart { "$this#configure" }
        configure(session)
        Debug.traceStop()
    }

    override fun onReady(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Ready" }
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Active" }
    }

    private fun configure(session: CameraCaptureSessionWrapper?) {
        val captureSession: ConfiguredCameraCaptureSession?
        var tryConfigureDeferred = false

        // This block is designed to do two things:
        // 1. Get or create a RequestProcessor instance.
        // 2. Pass the requestProcessor to the graphProcessor after the session is fully created and
        //    the onConfigured callback has been invoked.
        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return
            }

            if (cameraCaptureSession == null && session != null) {
                captureSession = ConfiguredCameraCaptureSession(
                    session,
                    requestProcessorFactory.create(session, activeSurfaceMap)
                )
                cameraCaptureSession = captureSession
            } else {
                captureSession = cameraCaptureSession
            }

            if (state != State.CREATED || captureSession == null) {
                return
            }

            // Finalize deferredConfigs if finalizeOutputConfigurations was previously invoked.
            if (pendingOutputMap != null && pendingSurfaceMap != null) {
                tryConfigureDeferred = true
            }
        }

        if (tryConfigureDeferred) {
            finalizeOutputsIfAvailable(retryAllowed = false)
        }

        synchronized(lock) {
            captureSession?.let {
                Log.info {
                    val duration = Timestamps.now() - sessionCreatingTimestamp!!
                    "Configured $this in ${duration.formatMs()}"
                }

                graphListener.onGraphStarted(it.processor)
            }
        }
    }

    /**
     * This is used to disconnect the cached [CameraCaptureSessionWrapper] and put this object into
     * a closed state. This will not cancel repeating requests or abort captures.
     */
    fun disconnect() {
        val captureSession = synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return@synchronized null
            }

            cameraCaptureSession.also {
                cameraCaptureSession = null
                state = State.CLOSING
            }
        }

        if (captureSession != null) {
            graphListener.onGraphStopped(captureSession.processor)
        }

        synchronized(this) {
            _cameraDevice = null
            state = State.CLOSED
        }
    }

    /**
     * This is used to disconnect the cached [CameraCaptureSessionWrapper] and put this object into
     * a closed state. This may stop the repeating request and abort captures.
     */
    private fun shutdown() {
        val captureSession = synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return@synchronized null
            }

            cameraCaptureSession.also {
                cameraCaptureSession = null
                state = State.CLOSING
            }
        }

        if (captureSession != null) {
            Debug.traceStart { "$this#shutdown" }

            Debug.traceStart { "$graphListener#onGraphStopped" }
            graphListener.onGraphStopped(captureSession.processor)
            Debug.traceStop()

            Debug.traceStart { "$this#stopRepeating" }
            captureSession.processor.stopRepeating()
            Debug.traceStop()

            Debug.traceStart { "$this#stopRepeating" }
            captureSession.processor.abortCaptures()
            Debug.traceStop()

            Debug.traceStop()
        }

        synchronized(this) {
            _cameraDevice = null
            state = State.CLOSED
        }
    }

    private fun finalizeOutputsIfAvailable(retryAllowed: Boolean = true) {
        val captureSession: ConfiguredCameraCaptureSession?
        val pendingOutputs: Map<StreamId, OutputConfigurationWrapper>?
        val pendingSurfaces: Map<StreamId, Surface>?
        synchronized(lock) {
            captureSession = cameraCaptureSession
            pendingOutputs = pendingOutputMap
            pendingSurfaces = pendingSurfaceMap
        }

        if (captureSession != null && pendingOutputs != null && pendingSurfaces != null) {
            Debug.traceStart { "$this#finalizeOutputConfigurations" }
            val finalizedStartTime = Timestamps.now()
            for ((streamId, outputConfig) in pendingOutputs) {
                // TODO: Consider adding support for experimental libraries on older devices.

                val surface = checkNotNull(pendingSurfaces[streamId])
                outputConfig.addSurface(surface)
            }

            // It's possible that more than one stream maps to the same output configuration since
            // output configurations support multiple surfaces. If this happens, we may have more
            // deferred outputs than outputConfiguration objects.
            val distinctOutputs = pendingOutputs.mapTo(mutableSetOf()) { it.value }.toList()
            captureSession.session.finalizeOutputConfigurations(distinctOutputs)

            var tryResubmit = false
            synchronized(lock) {
                if (state == State.CREATED) {
                    activeSurfaceMap.putAll(pendingSurfaces)
                    Log.info {
                        val finalizationTime = Timestamps.now() - finalizedStartTime
                        "Finalized ${pendingOutputs.map { it.key }} for $this in " +
                            finalizationTime.formatMs()
                    }
                    tryResubmit = true
                }
            }

            if (tryResubmit && retryAllowed) {
                graphListener.onGraphModified(captureSession.processor)
            }
            Debug.traceStop()
        }
    }

    private fun tryCreateCaptureSession() {
        val surfaces: Map<StreamId, Surface>?
        val device: CameraDeviceWrapper?
        synchronized(lock) {
            if (state != State.PENDING) {
                return
            }

            surfaces = _surfaceMap
            device = _cameraDevice
            if (surfaces == null || device == null) {
                return
            }

            state = State.CREATING
            sessionCreatingTimestamp = Timestamps.now()
        }

        // Create the capture session and return a Map of StreamId -> OutputConfiguration for any
        // outputs that were not initially available. These will be configured later.
        Log.info {
            "Creating CameraCaptureSession from ${device?.cameraId} using $this with $surfaces"
        }

        val deferred = Debug.trace(
            "CameraDevice-${device?.cameraId?.value}#createCaptureSession"
        ) {
            captureSessionFactory.create(device!!, surfaces!!, this)
        }

        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                Log.info { "Warning: $this was $state while configuration was in progress." }
                return
            }
            check(state == State.CREATING) { "Unexpected state: $state" }
            state = State.CREATED

            activeSurfaceMap.putAll(surfaces!!)
            if (deferred.isNotEmpty()) {
                Log.info {
                    "Created $this with ${surfaces.keys.toList()}. " +
                        "Waiting to finalize ${deferred.keys.toList()}"
                }
                pendingOutputMap = deferred

                val availableDeferredSurfaces = _surfaceMap?.filter {
                    deferred.containsKey(it.key)
                }

                if (availableDeferredSurfaces != null &&
                    availableDeferredSurfaces.size == deferred.size
                ) {
                    pendingSurfaceMap = availableDeferredSurfaces
                }
            }
        }

        // There are rare cases where the onConfigured call may be invoked synchronously. If this
        // happens, we need to invoke configure here to make sure the session ends up in a valid
        // state.
        configure(session = null)
    }

    override fun toString(): String = "VirtualSessionState-$debugId"

    private data class ConfiguredCameraCaptureSession(
        val session: CameraCaptureSessionWrapper,
        val processor: RequestProcessor
    )
}