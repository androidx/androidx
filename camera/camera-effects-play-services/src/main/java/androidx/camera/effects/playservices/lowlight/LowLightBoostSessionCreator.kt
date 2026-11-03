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
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import com.google.android.gms.cameralowlight.LowLightBoostCallback
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.cameralowlight.LowLightBoostOptions
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.LowLightBoostStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.concurrent.Executor

/**
 * Creates Google Play Services [LowLightBoostSession]s: checks the support and installs the Play
 * Services module if needed, and creates the session with retries and backoff ([RETRY_DELAYS_MS]).
 *
 * Each creation has an ID from the session manager (LowLightBoostSessionManager). The creator keeps
 * only the last release time for the cooldown ([SESSION_CLEANUP_DELAY_MS]): it asks [host] whether
 * a creation is still current and reports its outcome to [host], which installs the created session
 * or records the failure.
 *
 * **Threading:** used on the GL thread of [glExecutor] and [glHandler] only, and only on API level
 * 30 and higher. Google Play Services callbacks are moved to the GL thread, except for the
 * thread-safe marking of a terminated session.
 */
@WorkerThread
internal class LowLightBoostSessionCreator(
    private val glExecutor: Executor,
    private val glHandler: Handler,
    private val renderPipeline: LowLightBoostRenderPipeline,
    private val blockingCalls: BlockingCallTracker,
    private val host: Host,
) {
    /**
     * The session state that session creation reads, and the outcomes it reports. Outcomes are only
     * reported for the current creation, except [onSessionTerminated], which is reported for any
     * created session.
     */
    internal interface Host {
        /**
         * The application context of the owner of the session, i.e. the effect's surface processor,
         * or null if there is no owner. Creation is aborted when it's null.
         */
        val ownerContext: Context?

        /** Whether the owner of the session has Low Light Boost enabled. */
        fun isBoostEnabled(): Boolean

        /** Returns true if the creation [creationId] is still the current one. */
        fun isCurrentCreation(creationId: Long): Boolean

        /** Ends the current creation without reporting a state, when there is no owner. */
        @RequiresApi(Build.VERSION_CODES.R) fun onCreationAborted()

        /** Called when the module installation starts. */
        @RequiresApi(Build.VERSION_CODES.R) fun onModuleInstallationStarted()

        /** Called when the module was installed. */
        @RequiresApi(Build.VERSION_CODES.R) fun onModuleInstalled()

        /**
         * Called when the creation failed, e.g. after all retries, and Low Light Boost isn't
         * retried for the current SurfaceRequest. [unrecoverable] errors also keep it from being
         * retried when the app enables Low Light Boost again.
         */
        @RequiresApi(Build.VERSION_CODES.R)
        fun onCreationFailed(error: Throwable, unrecoverable: Boolean)

        /**
         * Ends the creation whose session, already released, was terminated before its creation
         * completed.
         */
        @RequiresApi(Build.VERSION_CODES.R) fun onCreatedSessionTerminated()

        /** Called when [session] was created for [target]. */
        @RequiresApi(Build.VERSION_CODES.R)
        fun onSessionCreated(session: LowLightBoostSession, target: SessionTarget)

        /** Called on the GL thread when a created [session] was destroyed or disconnected. */
        @RequiresApi(Build.VERSION_CODES.R)
        fun onSessionTerminated(session: LowLightBoostSession, errorMessage: String)
    }

    private var lastSessionReleaseTimeMs = 0L

    /**
     * Records that a session was released or a creation was aborted, so the next creation waits for
     * [SESSION_CLEANUP_DELAY_MS].
     */
    internal fun onSessionReleased() {
        lastSessionReleaseTimeMs = SystemClock.elapsedRealtime()
    }

    /**
     * Releases [resources] of an aborted creation after [SESSION_CLEANUP_DELAY_MS], since the
     * service may still render into its target surface meanwhile.
     */
    internal fun releaseAfterCleanupDelay(resources: OpenGlBridge.DetachedResources) {
        glHandler.postDelayed({ resources.release() }, SESSION_CLEANUP_DELAY_MS)
    }

    /**
     * Checks whether the Low Light Boost Play Services module is installed via
     * [LowLightBoostClient.isModuleInstalled]. If not installed, requests the installation once the
     * device and the camera are known to be supported (see [checkSupportAndInstallModule]);
     * otherwise proceeds directly to [createSessionWithRetry].
     */
    @RequiresApi(Build.VERSION_CODES.R)
    internal fun checkModuleAndCreateSession(
        creationId: Long,
        client: LowLightBoostClient,
        target: SessionTarget,
    ) {
        try {
            client.isModuleInstalled().addOnCompleteListener(glExecutor) { task ->
                if (!host.isCurrentCreation(creationId)) {
                    return@addOnCompleteListener
                }
                if (task.isSuccessful && task.result == false) {
                    checkSupportAndInstallModule(creationId, client, target)
                } else {
                    createSessionWithRetry(creationId, client, target, retryCount = 0)
                }
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to check whether the module is installed", e)
            createSessionWithRetry(creationId, client, target, retryCount = 0)
        }
    }

    /**
     * Requests the module installation for [target] once Google Play Services reports the device
     * and the camera as supported. The module can't be installed on an unsupported device, so the
     * current SurfaceRequest then fails with [LowLightBoostEffectState.ERROR] right away, without
     * [LowLightBoostEffectState.PENDING_INSTALLATION]. If support can't be determined, e.g. because
     * the query timed out, the installation is requested anyway.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkSupportAndInstallModule(
        creationId: Long,
        client: LowLightBoostClient,
        target: SessionTarget,
    ) {
        val appContext =
            host.ownerContext
                ?: run {
                    host.onCreationAborted()
                    return
                }
        // Results are cached, so only the first creation on a device or camera waits for it.
        val supportFuture =
            PlayServicesLowLightBoostAvailability.isSupportedAsync(
                appContext,
                target.cameraId,
            )
        supportFuture.addListener(
            {
                if (!host.isCurrentCreation(creationId)) {
                    return@addListener
                }
                if (host.ownerContext == null) {
                    host.onCreationAborted()
                    return@addListener
                }
                val unsupportedReason =
                    LowLightBoostSupport.cachedUnsupportedReasonOrNull(target.cameraId)
                if (unsupportedReason != null) {
                    Logger.w(TAG, unsupportedReason.logMessage)
                    host.onCreationFailed(unsupportedReason.error, unrecoverable = true)
                } else {
                    installModule(creationId, client, target)
                }
            },
            glExecutor,
        )
    }

    /**
     * Reports [LowLightBoostEffectState.PENDING_INSTALLATION] while frames are passed through, and
     * requests the module installation via [LowLightBoostClient.installModule].
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun installModule(
        creationId: Long,
        client: LowLightBoostClient,
        target: SessionTarget,
    ) {
        Logger.i(
            TAG,
            "Low Light Boost module not installed. Transitioning to PENDING_INSTALLATION and " +
                "starting passthrough preview.",
        )
        host.onModuleInstallationStarted()
        try {
            val callback = ModuleInstallStatusCallback(creationId, client, target)
            client
                .installModule(callback)
                .addOnFailureListener(glExecutor) { e ->
                    Logger.w(TAG, "Module installation request failed", e)
                    callback.handleInstallTerminated(
                        IllegalStateException("Module install failed: ${e.message}", e)
                    )
                }
                .addOnCanceledListener(glExecutor) {
                    Logger.w(TAG, "Module installation request cancelled")
                    callback.handleInstallTerminated(
                        IllegalStateException("Module install cancelled")
                    )
                }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to start installModule", e)
            createSessionWithRetry(creationId, client, target, retryCount = 0)
        }
    }

    /**
     * Callback receiving asynchronous Play Services module download and installation events from
     * [LowLightBoostClient.installModule]. Events are handled on the GL thread.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private inner class ModuleInstallStatusCallback(
        private val creationId: Long,
        private val client: LowLightBoostClient,
        private val target: SessionTarget,
    ) : LowLightBoostClient.InstallStatusCallback {
        override fun onInstalled() {
            Logger.i(TAG, "Low Light Boost module installed successfully.")
            glExecutor.execute {
                if (host.isCurrentCreation(creationId)) {
                    // Leave PENDING_INSTALLATION now, since creating the session can take a while,
                    // e.g. when it has to be retried.
                    host.onModuleInstalled()
                    createSessionWithRetry(creationId, client, target, retryCount = 0)
                }
            }
        }

        override fun onError(errorMsg: String) {
            Logger.w(TAG, "Module installation failed: $errorMsg")
            handleInstallTerminated(IllegalStateException("Module install failed: $errorMsg"))
        }

        override fun onCancelled() {
            Logger.w(TAG, "Module installation cancelled")
            handleInstallTerminated(IllegalStateException("Module install cancelled"))
        }

        fun handleInstallTerminated(error: IllegalStateException) {
            glExecutor.execute {
                if (host.isCurrentCreation(creationId)) {
                    host.onCreationFailed(error, unrecoverable = false)
                }
            }
        }

        override fun onDownloadProgressUpdate(progress: Int) {
            Logger.d(TAG, "Module download progress: $progress%")
        }

        override fun onDownloadPending() {}

        override fun onDownloadStart() {}

        override fun onDownloadPaused() {}

        override fun onDownloadComplete() {}
    }

    /**
     * Creates a [LowLightBoostSession], enforcing a [SESSION_CLEANUP_DELAY_MS] cooldown after any
     * recent session release and retrying transient failures with backoff ([RETRY_DELAYS_MS]).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun createSessionWithRetry(
        creationId: Long,
        client: LowLightBoostClient,
        target: SessionTarget,
        retryCount: Int,
    ) {
        // Step 1: Verify the creation is still current and wait for a recent session release to
        // finish in the service.
        if (!host.isCurrentCreation(creationId)) {
            Logger.d(TAG, "Aborting stale session creation (creationId=$creationId)")
            return
        }
        if (host.ownerContext == null) {
            host.onCreationAborted()
            return
        }
        val elapsedSinceRelease = SystemClock.elapsedRealtime() - lastSessionReleaseTimeMs
        if (lastSessionReleaseTimeMs > 0L && elapsedSinceRelease < SESSION_CLEANUP_DELAY_MS) {
            val waitDelayMs = SESSION_CLEANUP_DELAY_MS - elapsedSinceRelease
            Logger.d(TAG, "Waiting ${waitDelayMs}ms for the previous session release")
            glHandler.postDelayed(
                { createSessionWithRetry(creationId, client, target, retryCount) },
                waitDelayMs,
            )
            return
        }

        // Step 2: Ensure the boosted output bridge has a SessionTargetSurface that hasn't been
        // used for a session yet.
        val isOutputBridgeUsable =
            renderPipeline.ensureBoostedGlInitialized(
                resolution = target.resolution,
                forceNewIntermediate =
                    renderPipeline.outputBridge.inputSurface?.isUnusedForSession != true,
            )
        val targetSurface = renderPipeline.outputBridge.inputSurface
        val targetSurfaceError =
            when {
                !isOutputBridgeUsable || targetSurface == null ->
                    "Failed to initialize the OpenGL pipeline for Low Light Boost"
                !targetSurface.markConsumedForCreateSession() ->
                    "The OpenGL target surface was already used for a session"
                else -> null
            }
        // targetSurface == null implies targetSurfaceError != null; checking it smart-casts it.
        if (targetSurface == null || targetSurfaceError != null) {
            // A failed creation round: background recovery stops until a new round is allowed.
            Logger.w(TAG, "$targetSurfaceError; no session is created.")
            host.onCreationFailed(IllegalStateException(targetSurfaceError), unrecoverable = false)
            return
        }

        // Step 3: Invoke LowLightBoostClient.createSession() and attach completion listeners.
        val options =
            LowLightBoostOptions(
                targetSurface,
                target.cameraId,
                target.resolution.width,
                target.resolution.height,
                host.isBoostEnabled(),
            )
        val callback = LowLightBoostCallbackImpl(targetSurface)
        val task =
            try {
                blockingCalls.call { client.createSession(options, callback) }
            } catch (e: Exception) {
                handleCreateSessionFailure(
                    creationId = creationId,
                    client = client,
                    target = target,
                    targetSurface = targetSurface,
                    exception = e,
                    retryCount = retryCount,
                )
                return
            }
        task
            .addOnSuccessListener(glExecutor) { createdSession ->
                handleCreateSessionSuccess(
                    creationId,
                    createdSession,
                    target,
                    targetSurface,
                    callback,
                )
            }
            .addOnFailureListener(glExecutor) { exception ->
                handleCreateSessionFailure(
                    creationId = creationId,
                    client = client,
                    target = target,
                    targetSurface = targetSurface,
                    exception = exception,
                    retryCount = retryCount,
                )
            }
    }

    /** Releases [session], which was never installed as the active session. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun releaseUninstalledSession(session: LowLightBoostSession) {
        onSessionReleased()
        try {
            blockingCalls.call { session.release() }
        } catch (e: Exception) {
            Logger.d(TAG, "Ignored exception releasing an uninstalled session", e)
        }
    }

    /**
     * Handles the successful creation of [createdSession]: releases it if the creation is stale or
     * the session was terminated meanwhile, or caches the device and the camera as supported and
     * reports the session to [host].
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleCreateSessionSuccess(
        creationId: Long,
        createdSession: LowLightBoostSession,
        target: SessionTarget,
        targetSurface: SessionTargetSurface,
        callback: LowLightBoostCallbackImpl,
    ) {
        val isCurrent = host.isCurrentCreation(creationId)
        if (!isCurrent || host.ownerContext == null) {
            if (isCurrent) {
                host.onCreationAborted()
            }
            targetSurface.markTerminated()
            releaseUninstalledSession(createdSession)
            return
        }
        // Set the bound session before checking for termination: if the session is terminated
        // concurrently, either the termination callback sees the bound session and handles its
        // death on the GL thread after this method, or this check sees the termination.
        callback.boundSession = createdSession
        if (targetSurface.isTerminated) {
            // The session was destroyed or disconnected before this success listener ran, so its
            // termination callback couldn't handle it. The session is released before the host
            // ends the creation, which may start a recovery that waits for the release cooldown.
            Logger.w(TAG, "LowLightBoostSession was terminated before its creation completed")
            releaseUninstalledSession(createdSession)
            host.onCreatedSessionTerminated()
            return
        }
        PlayServicesLowLightBoostAvailability.setDeviceSupported(true)
        PlayServicesLowLightBoostAvailability.setCameraSupported(target.cameraId, true)
        host.onSessionCreated(createdSession, target)
    }

    /**
     * Handles a failed `createSession` attempt: caches the camera as unsupported for
     * `CAMERA_NOT_SUPPORTED`, schedules a retry with backoff if the error is transient, or reports
     * the failure to [host] if retries are exhausted or the error is unrecoverable.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleCreateSessionFailure(
        creationId: Long,
        client: LowLightBoostClient,
        target: SessionTarget,
        targetSurface: SessionTargetSurface,
        exception: Exception,
        retryCount: Int,
    ) {
        targetSurface.markTerminated()
        if (!host.isCurrentCreation(creationId)) {
            return
        }
        val unrecoverable = isUnrecoverableError(exception)
        if (exception is ApiException) {
            Logger.w(
                TAG,
                "LowLightBoost createSession failed with ApiException " +
                    "statusCode=${exception.statusCode}: ${exception.message}",
            )
            if (exception.statusCode == LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED) {
                PlayServicesLowLightBoostAvailability.setCameraSupported(target.cameraId, false)
            } else if (exception.statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                Logger.e(
                    TAG,
                    "LowLightBoost createSession failed with DEVELOPER_ERROR; check session " +
                        "configuration or Google Play Services integration.",
                    exception,
                )
            }
        }
        if (!unrecoverable && retryCount < MAX_RETRIES) {
            val delayMs = RETRY_DELAYS_MS[retryCount]
            Logger.w(
                TAG,
                "LowLightBoost createSession failed (attempt ${retryCount + 1}/$MAX_RETRIES): " +
                    "${exception.message}. Retrying in ${delayMs}ms...",
            )
            glHandler.postDelayed(
                { createSessionWithRetry(creationId, client, target, retryCount + 1) },
                delayMs,
            )
            return
        }
        // A transient failure, e.g. API_NOT_CONNECTED while Google Play Services updates, is not
        // cached as unsupported, so later requests and support checks can still succeed.
        Logger.w(
            TAG,
            if (unrecoverable) {
                "Low Light Boost session creation failed with an unrecoverable error; continuing " +
                    "in passthrough mode"
            } else {
                "All $MAX_RETRIES Low Light Boost session retries exhausted; continuing in " +
                    "passthrough mode"
            },
            exception,
        )
        host.onCreationFailed(exception, unrecoverable)
    }

    /**
     * Callback receiving session destruction and disconnection notifications of a
     * [LowLightBoostSession], on a Google Play Services thread. Immediately marks [targetSurface]
     * as terminated so it is never sent to Google Play Services again (see [SessionTargetSurface]),
     * then reports the termination to [host] on the GL thread, while passthrough preview continues.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private inner class LowLightBoostCallbackImpl(private val targetSurface: SessionTargetSurface) :
        LowLightBoostCallback {
        // Set on the GL thread and read on the Google Play Services callback thread.
        @Volatile var boundSession: LowLightBoostSession? = null

        override fun onSessionDestroyed() {
            handleSessionTerminated(
                logReason = "destroyed",
                errorMessage = "LowLightBoostSession was destroyed unexpectedly",
            )
        }

        override fun onSessionDisconnected(status: Status) {
            handleSessionTerminated(
                logReason = "disconnected ($status)",
                errorMessage = "LowLightBoostSession disconnected: status=$status",
            )
        }

        private fun handleSessionTerminated(logReason: String, errorMessage: String) {
            targetSurface.markTerminated()
            val session = boundSession
            Logger.d(TAG, "onSession $logReason called for session $session")
            if (session == null) {
                // The creation hasn't completed yet. handleCreateSessionSuccess() checks
                // targetSurface.isTerminated and handles it.
                return
            }
            glExecutor.execute { host.onSessionTerminated(session, errorMessage) }
        }
    }

    internal companion object {
        private const val TAG = "LlbSessionCreator"

        // The timing values below are tuned for Google Play Services Low Light Boost behavior
        // (b/448524973) and can be revisited when the service changes.

        /**
         * Delays between `createSession` retries after a transient failure, e.g. while the service
         * is still releasing a previous session or is being updated. Recreating the service's
         * processing pipeline takes about 2 seconds, so a session can usually be created again
         * within about 4 seconds, before ERROR is reported.
         */
        private val RETRY_DELAYS_MS = longArrayOf(1000L, 1000L, 2000L)
        private val MAX_RETRIES = RETRY_DELAYS_MS.size

        /**
         * Cooldown between releasing a [LowLightBoostSession] (or aborting an in-flight
         * `createSession` call) and the next `createSession` call. `LowLightBoostSession.release()`
         * completes asynchronously in the service, so creating a session right after it can fail
         * with `MAX_SESSIONS_REACHED` or race with the release.
         */
        private const val SESSION_CLEANUP_DELAY_MS = 350L

        /**
         * Classifies whether a session creation [exception] represents a permanent, unrecoverable
         * error (which should immediately fall back to passthrough without retrying) vs. a
         * transient error (such as `MAX_SESSIONS_REACHED` or a temporary disconnect).
         */
        internal fun isUnrecoverableError(exception: Throwable): Boolean {
            if (
                exception is IllegalArgumentException ||
                    exception is UnsupportedOperationException ||
                    exception is SecurityException
            ) {
                return true
            }
            if (exception is ApiException) {
                return when (exception.statusCode) {
                    LowLightBoostStatusCodes.CAMERA_NOT_SUPPORTED,
                    LowLightBoostStatusCodes.UNSUPPORTED_DATASPACE,
                    LowLightBoostStatusCodes.MISSING_CAMERA_CHARACTERISTICS,
                    LowLightBoostStatusCodes.INVALID_FRAME_METADATA,
                    CommonStatusCodes.DEVELOPER_ERROR -> true
                    CommonStatusCodes.API_NOT_CONNECTED ->
                        PlayServicesLowLightBoostAvailability.getCachedDeviceSupported() == false
                    else -> false
                }
            }
            return false
        }
    }
}
