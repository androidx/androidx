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

import android.hardware.camera2.CameraCaptureSession
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.Flags.FinalizeSessionOnCloseBehavior
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import java.util.Collections.synchronizedMap
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val captureSessionDebugIds = atomic(0)

/**
 * This class encapsulates the state and logic required to manage the lifecycle of a single Camera2
 * [CameraCaptureSession], and subsequently wrapping it into a [GraphRequestProcessor] and passing
 * it to the [GraphListener].
 *
 * After this object is created, it waits for:
 * - A valid CameraDevice via [cameraDevice]
 * - A valid map of Surfaces via [configureSurfaceMap] Once these objects are available, it will
 *   create the [CameraCaptureSession].
 *
 * If at any time this object is put into a COSING or CLOSED state the session will either never be
 * created, or if the session has already been created, it will be de-referenced and ignored. This
 * object goes into a CLOSING or CLOSED state if disconnect or shutdown is called, or if the created
 * [CameraCaptureSession] invokes onClosed or onConfigureFailed.
 *
 * This class is thread safe.
 */
@RequiresApi(21)
internal class CaptureSessionState(
    private val graphListener: GraphListener,
    private val captureSessionFactory: CaptureSessionFactory,
    private val captureSequenceProcessorFactory: Camera2CaptureSequenceProcessorFactory,
    private val cameraSurfaceManager: CameraSurfaceManager,
    private val timeSource: TimeSource,
    private val cameraGraphFlags: CameraGraph.Flags,
    private val scope: CoroutineScope
) : CameraCaptureSessionWrapper.StateCallback {
    private val debugId = captureSessionDebugIds.incrementAndGet()
    private val lock = Any()
    private val finalized = atomic<Boolean>(false)

    private val activeSurfaceMap = synchronizedMap(HashMap<StreamId, Surface>())
    private var sessionCreatingTimestamp: TimestampNs? = null

    @GuardedBy("lock")
    private var _cameraDevice: CameraDeviceWrapper? = null
    var cameraDevice: CameraDeviceWrapper?
        get() = synchronized(lock) { _cameraDevice }
        set(value) =
            synchronized(lock) {
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
    private var hasAttemptedCaptureSession = false

    @GuardedBy("lock")
    private var _surfaceMap: Map<StreamId, Surface>? = null

    @GuardedBy("lock")
    private val _surfaceTokenMap: MutableMap<Surface, AutoCloseable> = mutableMapOf()
    fun configureSurfaceMap(surfaces: Map<StreamId, Surface>) {
        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return@synchronized
            }

            updateTrackedSurfaces(_surfaceMap ?: emptyMap(), surfaces)
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
        disconnect()
        Debug.traceStop()
    }

    override fun onConfigureFailed(session: CameraCaptureSessionWrapper) {
        Log.warn { "$this Configuration Failed" }
        Debug.traceStart { "$this#onConfigureFailed" }
        disconnect()
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
        Log.debug { "$this CaptureQueueEmpty" }
    }

    override fun onSessionFinalized() {
        // Only invoke finalizeSession once regardless of the number of times it is invoked.
        if (finalized.compareAndSet(expect = false, update = true)) {
            Log.debug { "$this Finalizing Session" }
            Debug.traceStart { "$this#onSessionFinalized" }
            disconnect()
            finalizeSession(0L)
            Debug.traceStop()
        }
    }

    private fun configure(session: CameraCaptureSessionWrapper?) {
        val captureSession: ConfiguredCameraCaptureSession?
        var tryConfigureDeferred = false

        // This block is designed to do two things:
        // 1. Get or create a GraphRequestProcessor instance.
        // 2. Pass the GraphRequestProcessor to the graphProcessor after the session is fully
        //    created and the onConfigured callback has been invoked.
        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return
            }

            if (cameraCaptureSession == null && session != null) {
                captureSession =
                    ConfiguredCameraCaptureSession(
                        session,
                        GraphRequestProcessor.from(
                            captureSequenceProcessorFactory.create(session, activeSurfaceMap)
                        )
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
                    val duration = Timestamps.now(timeSource) - sessionCreatingTimestamp!!
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
        shutdown(abortAndStopRepeating = false)
    }

    /**
     * This is used to disconnect the cached [CameraCaptureSessionWrapper] and put this object into
     * a closed state. This may stop the repeating request and abort captures.
     */
    private fun shutdown(abortAndStopRepeating: Boolean) {
        var configuredCaptureSession: ConfiguredCameraCaptureSession? = null

        synchronized(lock) {
            if (state == State.CLOSING || state == State.CLOSED) {
                return@synchronized
            }
            state = State.CLOSING

            configuredCaptureSession = cameraCaptureSession
            cameraCaptureSession = null
        }

        val graphProcessor = configuredCaptureSession?.processor
        if (graphProcessor != null) {
            // WARNING:
            // This normally does NOT call close on the captureSession to avoid potentially slow
            // reconfiguration during mode switch and shutdown. This avoids unintentional restarts
            // by clearing the internal captureSession variable, clearing all repeating requests,
            // and by aborting any pending single requests.
            //
            // The reason we do not call close is that the android camera HAL doesn't shut down
            // cleanly unless the device is also closed. See b/135125484 for example.
            //
            // WARNING - DO NOT CALL session.close().
            Log.debug { "$this Shutdown" }

            Debug.traceStart { "$this#shutdown" }
            Debug.traceStart { "$graphListener#onGraphStopped" }
            graphListener.onGraphStopped(graphProcessor)
            Debug.traceStop()
            if (abortAndStopRepeating) {
                Debug.traceStart { "$this#stopRepeating" }
                graphProcessor.stopRepeating()
                Debug.traceStop()
                Debug.traceStart { "$this#abortCaptures" }
                graphProcessor.abortCaptures()
                Debug.traceStop()
            }

            // There are rare, extraordinary circumstances where we might need to close the capture
            // session. It is possible the app might explicitly wait for the captures to be
            // completely stopped through signals from CameraSurfaceManager, and in which case
            // closing the capture session would eventually release the Surfaces [1]. Additionally,
            // on certain devices, we need to close the capture session, or else the camera device
            // close call might stall indefinitely [2].
            //
            // [1] b/277310425
            // [2] b/277675483
            if (cameraGraphFlags.quirkCloseCaptureSessionOnDisconnect) {
                val captureSession = configuredCaptureSession?.session
                checkNotNull(captureSession)
                Debug.trace("$this CameraCaptureSessionWrapper#close") {
                    Log.debug { "Closing capture session for $this" }
                    captureSession.close()
                }
            }
            Debug.traceStop()
        }

        var shouldFinalizeSession = false
        var finalizeSessionDelayMs = 0L
        synchronized(lock) {
            // If the CameraDevice is never opened, the session will never be created. For cleanup
            // reasons, make sure the session is finalized after shutdown if the cameraDevice was
            // never set.
            if (state != State.CLOSED) {
                if (_cameraDevice == null || !hasAttemptedCaptureSession) {
                    shouldFinalizeSession = true
                } else {
                    when (cameraGraphFlags.quirkFinalizeSessionOnCloseBehavior) {
                        FinalizeSessionOnCloseBehavior.IMMEDIATE -> {
                            shouldFinalizeSession = true
                        }

                        FinalizeSessionOnCloseBehavior.TIMEOUT -> {
                            shouldFinalizeSession = true
                            finalizeSessionDelayMs = 2000L
                        }
                    }
                }
            }
            _cameraDevice = null
            state = State.CLOSED
        }

        if (shouldFinalizeSession) {
            finalizeSession(finalizeSessionDelayMs)
        }
    }

    private fun finalizeSession(delayMs: Long = 0L) {
        if (delayMs != 0L) {
            scope.launch {
                Log.debug { "Finalizing $this in $delayMs ms" }
                delay(delayMs)
                finalizeSession(0L)
            }
        } else {
            Log.debug { "Finalizing $this" }
            val tokenList =
                synchronized(lock) {
                    val tokens = _surfaceTokenMap.values.toList()
                    _surfaceTokenMap.clear()
                    tokens
                }
            tokenList.forEach { it.close() }
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
            val finalizedStartTime = Timestamps.now(timeSource)
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
                        val finalizationTime = Timestamps.now(timeSource) - finalizedStartTime
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
            hasAttemptedCaptureSession = true
            sessionCreatingTimestamp = Timestamps.now(timeSource)
        }

        // Create the capture session and return a Map of StreamId -> OutputConfiguration for any
        // outputs that were not initially available. These will be configured later.
        Log.info {
            "Creating CameraCaptureSession from ${device?.cameraId} using $this with $surfaces"
        }

        val deferred =
            Debug.trace("CameraDevice-${device?.cameraId?.value}#createCaptureSession") {
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

                val availableDeferredSurfaces = _surfaceMap?.filter { deferred.containsKey(it.key) }

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

    @GuardedBy("lock")
    private fun updateTrackedSurfaces(
        oldSurfaceMap: Map<StreamId, Surface>,
        newSurfaceMap: Map<StreamId, Surface>
    ) {
        val oldSurfaces = oldSurfaceMap.values.toSet()
        val newSurfaces = newSurfaceMap.values.toSet()

        // Close the Surfaces that were removed (unset).
        val removedSurfaces = oldSurfaces - newSurfaces
        for (surface in removedSurfaces) {
            val surfaceToken = _surfaceTokenMap.remove(surface)?.also { it.close() }
            checkNotNull(surfaceToken) { "Surface $surface doesn't have a matching surface token!" }
        }

        // Register new Surfaces.
        val addedSurfaces = newSurfaces - oldSurfaces
        for (surface in addedSurfaces) {
            val surfaceToken = cameraSurfaceManager.registerSurface(surface)
            _surfaceTokenMap[surface] = surfaceToken
        }
    }

    override fun toString(): String = "CaptureSessionState-$debugId"

    private data class ConfiguredCameraCaptureSession(
        val session: CameraCaptureSessionWrapper,
        val processor: GraphRequestProcessor
    )
}
