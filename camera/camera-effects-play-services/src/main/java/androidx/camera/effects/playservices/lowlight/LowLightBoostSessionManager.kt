/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.processing.OpenGlRenderer
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.SceneDetectorCallback
import java.util.concurrent.Executor

/**
 * Manages the process-wide Google Play Services [LowLightBoostSession] and the camera frames routed
 * through it.
 *
 * The manager owns the shared state: the processor that owns the session (the most recently bound
 * one), the current [SurfaceRequest] and its errors, and the session state (idle, being created, or
 * active). It applies the owner's enabled setting, reports [LowLightBoostEffectState] to it, reuses
 * the session for the same camera stream, triggers the recovery of a session that died, and
 * releases the session after the camera stream has been idle for a while. It delegates to these
 * collaborators, which keep their own state and report events back to it:
 * - [InputSurfaceProvider]: provides a new camera input [Surface] to each [SurfaceRequest].
 * - [LowLightBoostSessionCreator]: checks the support, installs the Play Services module, and
 *   creates sessions with retries.
 * - [LowLightBoostFrameRouter]: routes camera, passthrough and boosted frames, and detects a
 *   session that stopped producing boosted frames.
 * - [SessionRecoveryPolicy]: throttles and caps background recovery.
 * - [BlockingCallTracker]: runs synchronous Google Play Services calls and tracks how long they
 *   blocked the GL thread.
 * - [LowLightBoostSupport]: checks the resolution and the cached device and camera support.
 *
 * The manager owns [renderPipeline], which is shared with the collaborators:
 * - [InputSurfaceProvider] replaces the input bridge resources for each [SurfaceRequest], and binds
 *   the camera surface of a reusable session to the new input renderer.
 * - [LowLightBoostSessionCreator] prepares the output bridge for each session it creates.
 * - [LowLightBoostFrameRouter] renders frames, and binds the session camera surface to the input
 *   bridge if it isn't bound.
 * - The manager binds the camera surface of a created or reused session, detaches the output bridge
 *   when it releases a session, and releases both bridges on teardown.
 *
 * Google Play Services allows only one Low Light Boost session at a time, so the session is shared
 * by all [PlayServicesLowLightBoostSurfaceProcessor] instances. Binding Low Light Boost to more
 * than one camera at the same time isn't supported.
 *
 * **Threading:** all instance methods must be called on a single GL thread. [glExecutor] and
 * [glHandler] both run on it, and the processor's [androidx.camera.core.SurfaceProcessor] callbacks
 * are delivered on it. Callbacks from Google Play Services are moved to it, except for the
 * thread-safe marking of a terminated session. So the state needs no locks.
 *
 * The [LowLightBoostSession] requires Android 11 (API level 30). Below that, no session is ever
 * created: the device is reported as unsupported and frames are rendered in passthrough mode.
 */
internal class LowLightBoostSessionManager(
    internal val glExecutor: Executor,
    private val glHandler: Handler,
    rendererFactory: () -> OpenGlRenderer = { ContextAwareOpenGlRenderer() },
) {
    internal val renderPipeline =
        LowLightBoostRenderPipeline(
            glHandler = glHandler,
            rendererFactory = rendererFactory,
            onInputFrameAvailable = ::onInputBridgeFrameAvailable,
            onOutputFrameAvailable = ::onOutputBridgeFrameAvailable,
        )

    private val blockingCalls = BlockingCallTracker()

    private val recoveryPolicy = SessionRecoveryPolicy()

    private val inputSurfaceProvider = InputSurfaceProvider(renderPipeline, glExecutor)

    private val sessionCreator =
        LowLightBoostSessionCreator(
            glExecutor,
            glHandler,
            renderPipeline,
            blockingCalls,
            SessionCreatorHost(),
        )

    private val frameRouter =
        LowLightBoostFrameRouter(renderPipeline, blockingCalls, FrameRouterHost())

    /** The state of the Low Light Boost session. */
    private sealed interface SessionState {
        /** No session exists or is being created. */
        data object Idle : SessionState

        /** A session is being created. Results of other creations are stale. */
        class Creating(val id: Long, val target: SessionTarget) : SessionState

        /** A session exists for [target]. */
        class Active(
            val session: LowLightBoostSession,
            val target: SessionTarget,
            val startTimeMs: Long,
        ) : SessionState
    }

    private var sessionState: SessionState = SessionState.Idle

    private var lastCreationId = 0L

    private var currentSurfaceRequest: SurfaceRequest? = null

    // The camera and resolution of currentSurfaceRequest, which background recovery creates a
    // session for. A creation for the current SurfaceRequest always has this target: a request
    // for another stream either starts its own creation or fails, which stops recovery.
    private var currentRequestStream: SessionTarget? = null

    private var currentProcessor: PlayServicesLowLightBoostSurfaceProcessor? = null

    // The last state of the previous owner when the latest SurfaceRequest moved the ownership to
    // another processor, or null. A creation in flight for the same target continues for the new
    // owner, which then reports this state.
    private var previousOwnerState: LowLightBoostEffectState? = null

    private var registeredCameraInfoInternal: CameraInfoInternal? = null

    private var lastBoostStrength: Float? = null

    // The error that makes Low Light Boost unusable for the current camera stream, if any. It is
    // cleared by a SurfaceRequest that starts over (see bindInputSurfaceRequest()), a created
    // session or the full idle release, so toggling Low Light Boost doesn't retry it.
    private var unrecoverableErrorForCurrentStream: Throwable? = null

    // Whether the last session creation for the current camera stream failed after all retries,
    // the module installation failed, the support check found the device or camera unsupported,
    // or the boosted OpenGL pipeline couldn't be initialized.
    // Background recovery then stops, so the app doesn't see ERROR change to INACTIVE much later.
    // It is cleared by a SurfaceRequest that starts over, a created session, the app enabling Low
    // Light Boost, the camera stream resuming after a pause, or the full idle release.
    private var creationFailedForCurrentStream = false

    private val idleReleaseRunnable = Runnable { releaseSessionIfStillIdle() }

    private var isIdleReleaseScheduled = false

    private val activeSession: LowLightBoostSession?
        get() = (sessionState as? SessionState.Active)?.session

    /**
     * Forwards camera [TotalCaptureResult] metadata (such as exposure time and ISO sensitivity)
     * from CameraX's capture session to the active [LowLightBoostSession] for scene brightness
     * detection.
     */
    private val sessionCaptureCallback =
        object : CameraCaptureCallback() {
            override fun onCaptureCompleted(
                captureConfigId: Int,
                cameraCaptureResult: CameraCaptureResult,
            ) {
                val totalCaptureResult =
                    cameraCaptureResult.captureResult as? TotalCaptureResult ?: return
                processCaptureResult(totalCaptureResult)
            }
        }

    /**
     * Runs [block] on [session] and returns true if it succeeds. If it throws, e.g. because the
     * session died in Google Play Services, the session is treated as dead via [handleSessionDeath]
     * and false is returned.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private inline fun runSessionCall(
        session: LowLightBoostSession,
        operation: String,
        block: (LowLightBoostSession) -> Unit,
    ): Boolean {
        return try {
            blockingCalls.call { block(session) }
            true
        } catch (e: Exception) {
            Logger.w(TAG, "LowLightBoostSession $operation failed; treating the session as dead", e)
            handleSessionDeath(session, "LowLightBoostSession $operation failed: ${e.message}")
            false
        }
    }

    // Ownership and state: applies the owner's enabled setting and reports its state.

    /** Returns the active [LowLightBoostSession], or null if none is active. */
    @VisibleForTesting internal fun getCurrentSession(): LowLightBoostSession? = activeSession

    /**
     * Called when [processor] toggles Low Light Boost via
     * [PlayServicesLowLightBoostSurfaceProcessor.setLowLightBoostEnabled]. When enabled, resets the
     * recovery throttling, the recovery cap and the failed-creation flag, so a session can be
     * created again, and reports the unrecoverable error of the current [SurfaceRequest] again, if
     * any. Updates the processor's [LowLightBoostEffectState] and the session. The output switches
     * between boosted and passthrough frames with the next frames.
     *
     * If another processor owns the session, or no processor does, [processor] reports
     * [LowLightBoostEffectState.OFF], since it isn't processing a camera stream. So does an enabled
     * owner without a [SurfaceRequest], unless an unrecoverable error is still reported or the
     * module is still being installed.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    internal fun onBoostEnabledChanged(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        enabled: Boolean,
    ) {
        val owner = currentProcessor
        if (owner != null && owner !== processor) {
            // The session, its recovery and the request state belong to the owner.
            processor.postState(LowLightBoostEffectState.createOff())
            return
        }
        if (!enabled) {
            processor.postState(LowLightBoostEffectState.createOff())
        } else {
            // Enabling is an explicit request from the app, so allow an immediate recovery attempt
            // and reset the recovery cap. Otherwise, after the cap was reached, the state would be
            // reported as INACTIVE while no session would ever be created again.
            allowNewCreationRound()
            val unrecoverableError = unrecoverableErrorForCurrentStream
            val isOwner = processor === currentProcessor
            if (unrecoverableError != null && isOwner) {
                // Low Light Boost still can't be used for the current SurfaceRequest.
                processor.notifyError(unrecoverableError)
            } else if (currentSurfaceRequest == null || !isOwner) {
                // No camera stream is processed, unless the module is still being installed for
                // a creation that continues once a SurfaceRequest is bound again.
                val isInstalling =
                    sessionState is SessionState.Creating &&
                        processor.lastPostedState?.type ==
                            LowLightBoostEffectState.PENDING_INSTALLATION
                if (!isInstalling) {
                    processor.postState(LowLightBoostEffectState.createOff())
                }
            } else if (
                processor.lastPostedState?.type != LowLightBoostEffectState.PENDING_INSTALLATION
            ) {
                processor.postState(stateForBoostStrength(lastBoostStrength))
            }
        }
        val session = activeSession
        if (
            session != null &&
                !runSessionCall(session, "enableLowLightBoost") { it.enableLowLightBoost(enabled) }
        ) {
            return
        }
        if (enabled) {
            triggerAutoRecoveryIfNeeded(processor)
        }
    }

    /**
     * Allows a new session creation round for the current camera stream after a failed one, and
     * resets the recovery throttling and cap.
     */
    private fun allowNewCreationRound() {
        creationFailedForCurrentStream = false
        recoveryPolicy.reset()
    }

    /** Returns the ACTIVE or INACTIVE state for the scene's [boostStrength]. */
    private fun stateForBoostStrength(boostStrength: Float?): LowLightBoostEffectState =
        if (boostStrength != null && boostStrength >= ACTIVE_BOOST_STRENGTH_THRESHOLD) {
            LowLightBoostEffectState.createActive()
        } else {
            LowLightBoostEffectState.createInactive()
        }

    // Camera input: binds each SurfaceRequest and prepares a session for it.

    /**
     * Binds a new upstream [SurfaceRequest] of the camera [cameraId] to [processor] and registers
     * [sessionCaptureCallback] on the camera's [cameraInfoInternal].
     */
    @WorkerThread
    internal fun bindInputSurfaceRequest(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        surfaceRequest: SurfaceRequest,
        cameraInfoInternal: CameraInfoInternal,
        cameraId: String,
    ) {
        cancelScheduledSessionRelease()
        val stream = SessionTarget(surfaceRequest.resolution, cameraId)
        // CameraX sends a new SurfaceRequest for the same camera stream when it recreates the
        // pipeline, e.g. after a Preview target rotation change. If it arrives before the current
        // one completes, it keeps the current errors and recovery count, so a failed attempt isn't
        // repeated or reported again. Otherwise, e.g. after the camera stream stopped, it starts
        // over.
        val isSameStream =
            currentSurfaceRequest != null &&
                currentProcessor === processor &&
                currentRequestStream == stream
        if (!isSameStream) {
            unrecoverableErrorForCurrentStream = null
            allowNewCreationRound()
        }
        currentSurfaceRequest = surfaceRequest
        currentRequestStream = stream
        // The camera timestamps of a new stream may not follow the previous ones.
        frameRouter.resetCameraStreamTimestamps()
        previousOwnerState = null
        val previousProcessor = currentProcessor
        if (previousProcessor != null && previousProcessor !== processor) {
            // The previous processor no longer owns the session.
            previousOwnerState = previousProcessor.lastPostedState
            previousProcessor.postState(LowLightBoostEffectState.createOff())
        }
        currentProcessor = processor
        updateRegisteredCameraInfo(cameraInfoInternal)
    }

    /**
     * Moves [sessionCaptureCallback] from the previous [CameraInfoInternal], if any, to
     * [newCameraInfoInternal].
     */
    private fun updateRegisteredCameraInfo(newCameraInfoInternal: CameraInfoInternal?) {
        val oldCameraInfoInternal = registeredCameraInfoInternal
        if (oldCameraInfoInternal === newCameraInfoInternal) {
            return
        }
        if (oldCameraInfoInternal != null) {
            try {
                oldCameraInfoInternal.removeSessionCaptureCallback(sessionCaptureCallback)
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Failed to remove session capture callback", e)
            }
            registeredCameraInfoInternal = null
        }
        if (newCameraInfoInternal != null) {
            try {
                newCameraInfoInternal.addSessionCaptureCallback(glExecutor, sessionCaptureCallback)
                registeredCameraInfoInternal = newCameraInfoInternal
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Failed to add session capture callback", e)
            }
        }
    }

    /** Forwards [result] to the active [LowLightBoostSession] if one exists. */
    @WorkerThread
    private fun processCaptureResult(result: TotalCaptureResult) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val session = activeSession ?: return
        runSessionCall(session, "processCaptureResult") { it.processCaptureResult(result) }
    }

    /**
     * Provides a new input [Surface] to [surfaceRequest] (see [InputSurfaceProvider]), and either
     * reuses a warm [LowLightBoostSession] or initiates a new session for [resolution] and
     * [cameraId].
     */
    @WorkerThread
    internal fun provideInputSurfaceAndPrepareSession(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        surfaceRequest: SurfaceRequest,
        resolution: Size,
        cameraId: String,
    ) {
        val previousOwnerState = this.previousOwnerState
        this.previousOwnerState = null

        // Step 1: Provide a new input Surface to CameraX. Only a session that can be reused for
        // this request has its camera surface bound to the new input renderer. Another session is
        // destroyed below.
        val reusableSession =
            (sessionState as? SessionState.Active)
                ?.takeIf { it.target == SessionTarget(resolution, cameraId) }
                ?.session
        val activeCamSurface =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                reusableSession?.let { blockingCalls.cameraSurfaceOf(it) }
            } else {
                null
            }
        val isSurfaceProvided =
            inputSurfaceProvider.provideNewInputSurface(
                surfaceRequest,
                resolution,
                activeCamSurface,
            ) {
                onInputSurfaceClosed(surfaceRequest)
            }
        if (!isSurfaceProvided) {
            failCurrentRequest(
                processor = processor,
                error =
                    IllegalStateException(
                        "Failed to initialize the OpenGL pipeline for Low Light Boost"
                    ),
                logMessage = "Failed to initialize the OpenGL pipeline; no Surface is provided.",
            )
            // The SurfaceRequest won't complete, so release its state here.
            onInputSurfaceClosed(surfaceRequest)
            return
        }

        // LowLightBoostSession requires API level 30, so treat the device as unsupported and keep
        // rendering in passthrough mode below it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val reason = LowLightBoostSupport.unsupportedDeviceReason()
            failCurrentRequest(processor, reason.error, reason.logMessage)
            return
        }

        // A new SurfaceRequest for the same camera stream that arrives before the current one
        // completes keeps the result of a failed attempt, whose state is already reported. See
        // bindInputSurfaceRequest().
        if (unrecoverableErrorForCurrentStream != null || creationFailedForCurrentStream) {
            Logger.d(TAG, "Low Light Boost already failed for this camera stream; passing through")
            return
        }

        // Step 2: Reuse the active session if it is for the same camera and resolution.
        val target = SessionTarget(resolution, cameraId)
        if (tryReuseExistingSession(processor, target)) {
            return
        }

        // Step 3: Keep a creation in flight for the same target, or cancel one for another target.
        val creating = sessionState as? SessionState.Creating
        if (creating != null) {
            if (creating.target == target) {
                Logger.d(
                    TAG,
                    "LowLightBoostSession creation already in flight for $resolution, camera " +
                        "$cameraId; linking new SurfaceRequest",
                )
                // The creation, e.g. while the module is installed, continues for a new owner, so
                // it reports the same state as the previous owner.
                if (
                    previousOwnerState != null &&
                        previousOwnerState.type != LowLightBoostEffectState.OFF &&
                        processor.isLowLightBoostEnabled()
                ) {
                    processor.postState(previousOwnerState)
                }
                return
            }
            releaseDetachedSessionState(detachSessionState())
        }
        val creationId = beginSessionCreation(target)

        // Step 4: Validate the resolution and the cached device/camera support before making any
        // Google Play Services IPC calls. The creator prepares the boosted output OpenGL bridge.
        val unsupportedReason = LowLightBoostSupport.unsupportedReasonOrNull(resolution, cameraId)
        if (unsupportedReason != null) {
            failCurrentRequest(processor, unsupportedReason.error, unsupportedReason.logMessage)
            return
        }

        // Step 5: Initiate module check and session creation.
        val client = processor.client
        if (client == null) {
            failCurrentRequest(
                processor = processor,
                error =
                    processor.clientInitializationError
                        ?: IllegalStateException("LowLightBoostClient is null"),
                logMessage = "LowLightBoostClient is null; falling back to passthrough mode.",
            )
            return
        }
        sessionCreator.checkModuleAndCreateSession(creationId, client, target)
    }

    /**
     * Called when the Surface of [surfaceRequest] is no longer used. Unregisters
     * [sessionCaptureCallback] if [surfaceRequest] is still the active request and schedules an
     * idle session release check.
     */
    private fun onInputSurfaceClosed(surfaceRequest: SurfaceRequest) {
        if (currentSurfaceRequest === surfaceRequest) {
            currentSurfaceRequest = null
            currentRequestStream = null
            updateRegisteredCameraInfo(null)
        }
        scheduleSessionReleaseIfIdle()
    }

    // Session state: creation, reuse, failures and the created session.

    /** Returns true if the session creation [creationId] is still the current one. */
    private fun isCurrentCreation(creationId: Long): Boolean =
        (sessionState as? SessionState.Creating)?.id == creationId

    /** Starts creating a session for [target] and returns the creation ID. */
    private fun beginSessionCreation(target: SessionTarget): Long {
        val creationId = ++lastCreationId
        sessionState = SessionState.Creating(creationId, target)
        return creationId
    }

    /**
     * Reuses the active session if it is for [target] and its camera/GL surfaces are still valid.
     * Destroys the active session if it can't be reused.
     *
     * @return true if the active session was reused, false if a new session needs to be created.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private fun tryReuseExistingSession(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        target: SessionTarget,
    ): Boolean {
        val active = sessionState as? SessionState.Active ?: return false
        if (active.target != target) {
            releaseSessionKeepingInput()
            return false
        }
        val session = active.session
        val camSurface = blockingCalls.cameraSurfaceOf(session)
        val isEnabled = processor.isLowLightBoostEnabled()
        if (camSurface == null || !camSurface.isValid || !renderPipeline.outputBridge.isValid()) {
            Logger.w(TAG, "Existing session camera surface or GL intermediate is invalid")
            releaseSessionKeepingInput()
            return false
        }
        try {
            blockingCalls.call { session.enableLowLightBoost(isEnabled) }
        } catch (e: Exception) {
            Logger.w(TAG, "Existing LowLightBoostSession is dead; will recreate", e)
            releaseSessionKeepingInput()
            return false
        }
        Logger.d(
            TAG,
            "Reusing existing LowLightBoostSession for ${target.resolution}, camera " +
                target.cameraId,
        )
        renderPipeline.bindSessionCameraSurfaceToInputBridge(camSurface)
        processor.postState(
            if (isEnabled) {
                stateForBoostStrength(lastBoostStrength)
            } else {
                LowLightBoostEffectState.createOff()
            }
        )
        return true
    }

    /**
     * Records [error] as unrecoverable for the current [SurfaceRequest], so no further session is
     * created for it, ends a session creation, if any, and falls back to passthrough mode with
     * [LowLightBoostEffectState.ERROR].
     */
    private fun failCurrentRequest(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        error: Throwable,
        logMessage: String,
    ) {
        unrecoverableErrorForCurrentStream = error
        Logger.w(TAG, logMessage)
        if (sessionState is SessionState.Creating) {
            sessionState = SessionState.Idle
        }
        processor.notifyError(error)
    }

    /**
     * Installs [createdSession] as the active session: enables it, reports the state, registers the
     * scene detector callback and binds its camera surface to
     * [LowLightBoostRenderPipeline.inputBridge]. The output switches to boosted frames when the
     * first one arrives.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private fun installCreatedSession(
        processor: PlayServicesLowLightBoostSurfaceProcessor,
        createdSession: LowLightBoostSession,
        target: SessionTarget,
    ) {
        sessionState = SessionState.Active(createdSession, target, SystemClock.elapsedRealtime())
        unrecoverableErrorForCurrentStream = null
        creationFailedForCurrentStream = false
        val isEnabled = processor.isLowLightBoostEnabled()
        if (
            !runSessionCall(createdSession, "enableLowLightBoost") {
                it.enableLowLightBoost(isEnabled)
            }
        ) {
            return
        }
        processor.postState(
            if (isEnabled) {
                LowLightBoostEffectState.createInactive()
            } else {
                LowLightBoostEffectState.createOff()
            }
        )
        val sceneCallback = SceneDetectorCallback { _, boostStrength ->
            // Ignore late callbacks from a session that is no longer active.
            if (activeSession !== createdSession) {
                return@SceneDetectorCallback
            }
            lastBoostStrength = boostStrength
            val proc = currentProcessor
            if (proc != null && proc.isLowLightBoostEnabled()) {
                proc.postState(stateForBoostStrength(boostStrength))
            }
        }
        try {
            blockingCalls.call {
                createdSession.setSceneDetectorCallback(sceneCallback, glExecutor)
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to setSceneDetectorCallback", e)
        }

        val cameraSurface = blockingCalls.cameraSurfaceOf(createdSession)
        if (cameraSurface != null && cameraSurface.isValid) {
            renderPipeline.bindSessionCameraSurfaceToInputBridge(cameraSurface)
        } else {
            Logger.w(TAG, "Created session has invalid cameraSurface")
            handleSessionDeath(createdSession, "LowLightBoostSession camera surface is invalid")
        }
    }

    /** Receives the outcomes of [sessionCreator]. */
    private inner class SessionCreatorHost : LowLightBoostSessionCreator.Host {
        override val ownerContext: Context?
            get() = currentProcessor?.appContext

        override fun isBoostEnabled(): Boolean = currentProcessor?.isLowLightBoostEnabled() ?: false

        override fun isCurrentCreation(creationId: Long): Boolean =
            this@LowLightBoostSessionManager.isCurrentCreation(creationId)

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onCreationAborted() {
            sessionState = SessionState.Idle
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onModuleInstallationStarted() {
            currentProcessor?.postState(LowLightBoostEffectState.createPendingInstallation())
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onModuleInstalled() {
            currentProcessor?.let { proc ->
                if (proc.lastPostedState?.type == LowLightBoostEffectState.PENDING_INSTALLATION) {
                    proc.postState(
                        if (proc.isLowLightBoostEnabled()) {
                            LowLightBoostEffectState.createInactive()
                        } else {
                            LowLightBoostEffectState.createOff()
                        }
                    )
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onCreationFailed(error: Throwable, unrecoverable: Boolean) {
            if (unrecoverable) {
                unrecoverableErrorForCurrentStream = error
            }
            creationFailedForCurrentStream = true
            sessionState = SessionState.Idle
            currentProcessor?.notifyError(error)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onCreatedSessionTerminated() {
            sessionState = SessionState.Idle
            val processor = currentProcessor ?: return
            processor.notifyError(
                IllegalStateException("LowLightBoostSession was terminated during creation")
            )
            triggerAutoRecoveryIfNeeded(processor)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onSessionCreated(session: LowLightBoostSession, target: SessionTarget) {
            // The creator checked that a processor owns the creation right before, on this thread.
            val processor = checkNotNull(currentProcessor)
            installCreatedSession(processor, session, target)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onSessionTerminated(session: LowLightBoostSession, errorMessage: String) {
            handleSessionDeath(session, errorMessage)
        }
    }

    // Frame routing: wires the frame callbacks of renderPipeline to frameRouter.

    /** Called when a camera frame arrives. See [LowLightBoostFrameRouter.onInputFrameAvailable]. */
    @WorkerThread
    @VisibleForTesting
    internal fun onInputBridgeFrameAvailable(timestampNs: Long, textureTransform: FloatArray) {
        frameRouter.onInputFrameAvailable(timestampNs, textureTransform)
    }

    /**
     * Called when a boosted frame arrives. See [LowLightBoostFrameRouter.onOutputFrameAvailable].
     */
    @WorkerThread
    @VisibleForTesting
    internal fun onOutputBridgeFrameAvailable(timestampNs: Long, textureTransform: FloatArray) {
        frameRouter.onOutputFrameAvailable(timestampNs, textureTransform)
    }

    /** Provides the session state to [frameRouter] and receives its events. */
    private inner class FrameRouterHost : LowLightBoostFrameRouter.Host {
        override val activeSession: LowLightBoostSession?
            get() = this@LowLightBoostSessionManager.activeSession

        override fun isBoostEnabled(): Boolean = currentProcessor?.isLowLightBoostEnabled() ?: false

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onCameraStreamResumed() {
            // Allow a new creation round and reset the recovery cap if the last one failed, since
            // the cause, e.g. another app holding the session, may be gone now.
            allowNewCreationRound()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onSessionUnusable(session: LowLightBoostSession, errorMessage: String) {
            handleSessionDeath(session, errorMessage)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onSessionNeeded() {
            currentProcessor?.let { triggerAutoRecoveryIfNeeded(it) }
        }
    }

    // Recovery: session death and background recovery.

    /**
     * Handles the death of [session] if it is still the active session: releases it, reports
     * [LowLightBoostEffectState.ERROR] while frames are rendered in passthrough mode, and triggers
     * a capped background recovery.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleSessionDeath(session: LowLightBoostSession, errorMessage: String) {
        val active = sessionState as? SessionState.Active
        if (active?.session !== session) {
            Logger.d(TAG, "Ignoring death of non-current session $session")
            return
        }
        recoveryPolicy.onSessionDied(active.startTimeMs, SystemClock.elapsedRealtime())
        releaseSessionKeepingInput()
        val processor = currentProcessor
        if (processor == null || !processor.isLowLightBoostEnabled()) {
            // Keep the OFF state while Low Light Boost is disabled. A session is created again
            // when it is enabled.
            return
        }
        processor.notifyError(IllegalStateException(errorMessage))
        triggerAutoRecoveryIfNeeded(processor)
    }

    /**
     * Starts a background session recovery for [processor] if it owns the session, there is a
     * current [SurfaceRequest] and a registered output, no session is active or being created, no
     * unrecoverable error occurred for the current [SurfaceRequest], the last session creation
     * didn't fail after all retries, and [SessionRecoveryPolicy.canAttemptRecovery] allows it.
     *
     * Each recovery is one session creation with its own retries.
     */
    @WorkerThread
    internal fun triggerAutoRecoveryIfNeeded(processor: PlayServicesLowLightBoostSurfaceProcessor) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                processor !== currentProcessor ||
                !processor.isLowLightBoostEnabled()
        ) {
            return
        }
        if (
            sessionState !is SessionState.Idle ||
                unrecoverableErrorForCurrentStream != null ||
                creationFailedForCurrentStream
        ) {
            return
        }
        // Without a SurfaceRequest or an output, nothing would show the boosted frames, e.g. after
        // the session was released because the output was removed.
        if (currentSurfaceRequest == null || renderPipeline.isOutputSurfacesEmpty()) {
            return
        }
        val target = currentRequestStream ?: return
        val now = SystemClock.elapsedRealtime()
        if (!recoveryPolicy.canAttemptRecovery(now)) {
            return
        }
        val client = processor.client ?: return
        recoveryPolicy.onRecoveryAttempted(now)
        val creationId = beginSessionCreation(target)
        Logger.i(
            TAG,
            "Triggering background Low Light Boost session recovery for " +
                "${target.resolution}, camera ${target.cameraId}",
        )
        sessionCreator.checkModuleAndCreateSession(creationId, client, target)
    }

    // Idle release and teardown.

    /** Cancels the pending idle session release, if any. */
    @WorkerThread
    internal fun cancelScheduledSessionRelease() {
        if (isIdleReleaseScheduled) {
            glHandler.removeCallbacks(idleReleaseRunnable)
            isIdleReleaseScheduled = false
            Logger.d(TAG, "Cancelled pending session release; keeping session warm")
        }
    }

    /**
     * Schedules a delayed ([SESSION_RELEASE_DELAY_MS]) session teardown if either the downstream
     * output has been removed or the upstream [SurfaceRequest] has closed. Keeping the session warm
     * avoids expensive session re-creation across quick rebinds or lifecycle restarts.
     */
    @WorkerThread
    internal fun scheduleSessionReleaseIfIdle() {
        val isIdle = renderPipeline.isOutputSurfacesEmpty() || currentSurfaceRequest == null
        // Also release the state of an owner without OpenGL resources, e.g. after an OpenGL
        // initialization failure, so its state returns to OFF.
        val hasSession =
            activeSession != null ||
                renderPipeline.inputBridge.inputSurface != null ||
                currentProcessor != null ||
                sessionState !is SessionState.Idle
        if (isIdle && hasSession) {
            cancelScheduledSessionRelease()
            Logger.d(
                TAG,
                "Camera stream idle. Scheduling session release in ${SESSION_RELEASE_DELAY_MS}ms",
            )
            isIdleReleaseScheduled = true
            glHandler.postDelayed(idleReleaseRunnable, SESSION_RELEASE_DELAY_MS)
        }
    }

    /**
     * If the camera stream is still idle, releases the session, and also the OpenGL resources if
     * both the input and the output are gone.
     */
    @WorkerThread
    private fun releaseSessionIfStillIdle() {
        isIdleReleaseScheduled = false
        val processor = currentProcessor
        val outputsEmpty = renderPipeline.isOutputSurfacesEmpty()
        val noRequest = currentSurfaceRequest == null
        if (outputsEmpty && noRequest) {
            Logger.i(
                TAG,
                "Idle timeout reached with no active input or output surfaces; releasing " +
                    "session and all GL resources",
            )
            currentProcessor = null
            destroySessionInternal()
            // Also replaces a state reported without an active session, e.g.
            // PENDING_INSTALLATION or ERROR, since no camera stream is processed anymore.
            processor?.postState(LowLightBoostEffectState.createOff())
        } else if (outputsEmpty || noRequest) {
            Logger.i(
                TAG,
                "Idle timeout reached; releasing LowLightBoostSession while keeping input " +
                    "bridge warm",
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val detached = detachSessionState()
                val releasedSession = releaseDetachedSessionState(detached)
                // An aborted module installation would otherwise keep reporting
                // PENDING_INSTALLATION. It is reported again if a creation finds the module still
                // missing, e.g. when the output returns.
                val abortedInstallation =
                    detached.wasCreating &&
                        processor?.lastPostedState?.type ==
                            LowLightBoostEffectState.PENDING_INSTALLATION
                if (releasedSession || abortedInstallation) {
                    processor?.postState(LowLightBoostEffectState.createOff())
                }
            }
        }
    }

    private class DetachedSessionState(
        val sessionToRelease: LowLightBoostSession?,
        val wasCreating: Boolean,
    )

    /** Ends the active session or the session creation, if any, without releasing anything. */
    private fun detachSessionState(): DetachedSessionState {
        val state = sessionState
        val detached =
            DetachedSessionState(
                sessionToRelease = (state as? SessionState.Active)?.session,
                wasCreating = state is SessionState.Creating,
            )
        if (state !is SessionState.Idle) {
            sessionCreator.onSessionReleased()
        }
        sessionState = SessionState.Idle
        lastBoostStrength = null
        return detached
    }

    /**
     * Releases the session and the boosted OpenGL resources detached by [detachSessionState].
     *
     * @return true if a session was released.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private fun releaseDetachedSessionState(state: DetachedSessionState): Boolean {
        frameRouter.onSessionReleased()
        val detachedOutputResources =
            renderPipeline.detachBoostedSessionResources(
                state.sessionToRelease?.let { blockingCalls.cameraSurfaceOf(it) }
            )
        if (state.sessionToRelease != null) {
            try {
                Logger.d(TAG, "Releasing LowLightBoostSession only (input bridge kept alive)")
                blockingCalls.call { state.sessionToRelease.release() }
            } catch (e: Exception) {
                Logger.w(TAG, "Error releasing LowLightBoostSession", e)
            }
        }
        if (state.wasCreating) {
            sessionCreator.releaseAfterCleanupDelay(detachedOutputResources)
        } else {
            detachedOutputResources.release()
        }
        return state.sessionToRelease != null
    }

    /**
     * Destroys only the active [LowLightBoostSession] and detaches
     * [LowLightBoostRenderPipeline.outputBridge] while keeping
     * [LowLightBoostRenderPipeline.inputBridge] alive for uninterrupted passthrough preview.
     *
     * @return true if an active [LowLightBoostSession] was released.
     */
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.R)
    private fun releaseSessionKeepingInput(): Boolean =
        releaseDetachedSessionState(detachSessionState())

    /**
     * Destroys the active [LowLightBoostSession], unregisters [sessionCaptureCallback], and
     * releases both OpenGL bridges.
     *
     * @return true if an active [LowLightBoostSession] was released.
     */
    @WorkerThread
    internal fun destroySessionInternal(): Boolean {
        cancelScheduledSessionRelease()
        val hadLlbSession =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && releaseSessionKeepingInput()
        // No SurfaceRequest is bound anymore, so its errors don't apply to the next one.
        currentRequestStream = null
        unrecoverableErrorForCurrentStream = null
        creationFailedForCurrentStream = false
        updateRegisteredCameraInfo(null)
        frameRouter.resetCameraStreamTimestamps()
        // This runs when no SurfaceRequest is bound anymore, after an idle delay, or when tearing
        // down, so the input Surfaces are released even if an older request hasn't completed.
        renderPipeline.releaseBridges()
        inputSurfaceProvider.releaseDetachedResources()
        return hadLlbSession
    }

    private companion object {
        private const val TAG = "LlbSessionManager"

        /**
         * How long the session is kept after the camera stream becomes idle, so a quick rebind,
         * e.g. after a configuration change, reuses it instead of creating a new one. Tuned for
         * Google Play Services Low Light Boost behavior (b/448524973).
         */
        private const val SESSION_RELEASE_DELAY_MS = 2000L

        /** The scene brightness boost strength from which the state is reported as ACTIVE. */
        private const val ACTIVE_BOOST_STRENGTH_THRESHOLD = 0.5f
    }
}
