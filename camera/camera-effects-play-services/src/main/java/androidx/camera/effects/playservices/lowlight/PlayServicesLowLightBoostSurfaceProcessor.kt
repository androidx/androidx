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
import android.os.HandlerThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.cameralowlight.LowLightBoostClient
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [SurfaceProcessor] implementation for [LowLightBoostEffect] backed by Google Play Services
 * `LowLightBoostClient`.
 *
 * Each [LowLightBoostEffect] instance owns a [PlayServicesLowLightBoostSurfaceProcessor] to track
 * its per-instance [stateLiveData] and enabled flag, while delegating the process-wide
 * `LowLightBoostSession` management and dual-bridge OpenGL rendering to
 * [LowLightBoostSessionManager] and [LowLightBoostRenderPipeline].
 *
 * **Threading:** [LowLightBoostEffect] delivers the [SurfaceProcessor] callbacks on [glExecutor],
 * the single GL thread that all session and OpenGL work runs on.
 *
 * **Resource Lifecycle:** Because [LowLightBoostEffect] instances may be reused across multiple
 * camera bindings, configuration changes, and lens switches, this processor does not permanently
 * shut down on a single unbind. Instead, when a camera or effect is unbound, CameraX closes the
 * upstream [SurfaceRequest] and downstream [SurfaceOutput]s, which triggers
 * [LowLightBoostSessionManager.scheduleSessionReleaseIfIdle] to automatically tear down the
 * session, OpenGL bridges, and active processor reference once the pipeline is idle.
 *
 * **API level:** The `LowLightBoostSession` requires Android 11 (API level 30). On lower API
 * levels, this processor never creates a [LowLightBoostClient] or session, treats the device as
 * unsupported (passthrough with [LowLightBoostEffectState.ERROR]), and ignores
 * [setLowLightBoostEnabled].
 */
internal class PlayServicesLowLightBoostSurfaceProcessor
@VisibleForTesting
internal constructor(
    internal val appContext: Context,
    private val clientFactory: (Context) -> LowLightBoostClient?,
    private val sessionManager: LowLightBoostSessionManager,
) : SurfaceProcessor {

    internal constructor(
        appContext: Context
    ) : this(
        appContext,
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) LowLightBoost.getClient(it)
            else null
        },
        defaultSessionManager,
    )

    private val mutableState =
        MutableLiveData<LowLightBoostEffectState>(LowLightBoostEffectState.createOff())

    /** LiveData exposing the current [LowLightBoostEffectState] to [LowLightBoostEffect.state]. */
    internal val stateLiveData: LiveData<LowLightBoostEffectState>
        get() = mutableState

    /** The executor of the GL thread, which the [SurfaceProcessor] callbacks must run on. */
    internal val glExecutor: Executor
        get() = sessionManager.glExecutor

    /**
     * The latest state posted to [stateLiveData]. Unlike `LiveData.getValue()`, it includes a state
     * that is posted but not yet delivered on the main thread.
     */
    @Volatile
    internal var lastPostedState: LowLightBoostEffectState? = LowLightBoostEffectState.createOff()
        private set

    private val boostEnabled = AtomicBoolean(true)

    // Written on the main or GL thread and read on the GL thread.
    @Volatile private var clientInternal: LowLightBoostClient? = null

    /** The error of the latest failed [LowLightBoostClient] initialization, if any. */
    @Volatile
    internal var clientInitializationError: Exception? = null
        private set

    internal val client: LowLightBoostClient?
        get() = ensureClientInitialized()

    init {
        ensureClientInitialized()
    }

    /** Lazily initializes and caches the [LowLightBoostClient] using [appContext]. */
    private fun ensureClientInitialized(): LowLightBoostClient? {
        clientInternal?.let {
            return it
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // LowLightBoostClient requires API level 30. The session manager reports the device as
            // unsupported when the input surface is provided.
            return null
        }
        try {
            val initializedClient = clientFactory(appContext)
            clientInternal = initializedClient
            clientInitializationError = null
            return initializedClient
        } catch (e: Exception) {
            // The session manager reports it once the effect is bound, so the initial state stays
            // OFF.
            Logger.w(TAG, "Failed to instantiate LowLightBoostClient", e)
            clientInitializationError = e
        }
        return null
    }

    /**
     * Posts [state] to [mutableState], unless it equals the latest posted state, so observers
     * aren't notified again, e.g. when a new SurfaceRequest reuses the session.
     */
    internal fun postState(state: LowLightBoostEffectState) {
        if (state == lastPostedState) {
            return
        }
        lastPostedState = state
        mutableState.postValue(state)
    }

    /**
     * Enables or disables Low Light Boost enhancement dynamically and delegates session/renderer
     * state updates to [sessionManager] on the GL thread. Does nothing if [enabled] doesn't change
     * the current setting. Can be called on any thread.
     */
    internal fun setLowLightBoostEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        if (boostEnabled.getAndSet(enabled) == enabled) {
            return
        }
        glExecutor.execute { sessionManager.onBoostEnabledChanged(this, enabled) }
    }

    /**
     * Returns true if Low Light Boost enhancement is currently enabled on this processor. Always
     * false below API level 30, where Low Light Boost is unsupported.
     */
    internal fun isLowLightBoostEnabled(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && boostEnabled.get()

    /** Transitions [stateLiveData] to [LowLightBoostEffectState.ERROR] with [error]. */
    internal fun notifyError(error: Throwable) {
        val exception = error as? Exception ?: RuntimeException(error.message, error)
        postState(LowLightBoostEffectState.createError(exception))
    }

    /**
     * Called by CameraX on the GL thread when a new upstream camera [SurfaceRequest] is ready.
     * Binds the request and camera capture callback, provides the OpenGL input surface, and
     * prepares the session.
     *
     * The processor doesn't apply rotation itself and doesn't listen to
     * [SurfaceRequest.TransformationInfo] updates. The output is rendered with the transform from
     * [SurfaceOutput.updateTransformMatrix], and CameraX recreates the output when a target
     * rotation change needs a new one.
     */
    @WorkerThread
    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        val resolution = surfaceRequest.resolution
        val cameraInfoInternal =
            try {
                surfaceRequest.camera.cameraInfoInternal
            } catch (e: RuntimeException) {
                Logger.e(TAG, "CameraInfoInternal unavailable; no Surface is provided", e)
                surfaceRequest.willNotProvideSurface()
                notifyError(e)
                return
            }
        val cameraId = realCameraId(cameraInfoInternal)
        sessionManager.bindInputSurfaceRequest(this, surfaceRequest, cameraInfoInternal, cameraId)
        sessionManager.provideInputSurfaceAndPrepareSession(
            processor = this,
            surfaceRequest = surfaceRequest,
            resolution = resolution,
            cameraId = cameraId,
        )
    }

    /**
     * Returns the ID of the camera that [cameraInfoInternal] belongs to. When the effect is applied
     * to a child of a shared stream, e.g. a Preview with a PREVIEW-only effect under StreamSharing,
     * the [SurfaceRequest] comes from a virtual camera whose ID Google Play Services doesn't know.
     */
    private fun realCameraId(cameraInfoInternal: CameraInfoInternal): String =
        cameraInfoInternal.implementation.cameraId

    /**
     * Called by CameraX on the GL thread when the downstream [SurfaceOutput] is ready to receive
     * processed frames. Registers it with the rendering pipeline. It replaces the previous output,
     * if any.
     */
    @WorkerThread
    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        sessionManager.cancelScheduledSessionRelease()
        val renderPipeline = sessionManager.renderPipeline
        try {
            val surface =
                surfaceOutput.getSurface(glExecutor) {
                    // Release the EGL surface before closing the output.
                    renderPipeline.unregisterOutputSurface(surfaceOutput)
                    surfaceOutput.close()
                    sessionManager.scheduleSessionReleaseIfIdle()
                }
            if (!renderPipeline.registerOutputSurface(surfaceOutput, surface)) {
                surfaceOutput.close()
                // The idle release cancelled above is still needed.
                sessionManager.scheduleSessionReleaseIfIdle()
                return
            }
            sessionManager.triggerAutoRecoveryIfNeeded(this)
            // The camera stream may already be idle, e.g. if its SurfaceRequest completed.
            sessionManager.scheduleSessionReleaseIfIdle()
        } catch (e: Exception) {
            Logger.e(TAG, "Error registering output surface", e)
            // Release the EGL surface, if any, before closing the output.
            renderPipeline.unregisterOutputSurface(surfaceOutput)
            surfaceOutput.close()
            sessionManager.scheduleSessionReleaseIfIdle()
        }
    }

    internal companion object {
        private const val TAG = "PlayServicesLLBProc"

        /**
         * The session manager shared by all processors. Its process-wide GL thread runs all EGL
         * rendering and session lifecycle work, because EGL contexts are thread-affine and Google
         * Play Services allows only one Low Light Boost session at a time.
         */
        private val defaultSessionManager by lazy {
            val glThread =
                HandlerThread("CameraX-LLB-GL").apply {
                    isDaemon = true
                    start()
                }
            val glHandler = Handler(glThread.looper)
            LowLightBoostSessionManager(CameraXExecutors.newHandlerExecutor(glHandler), glHandler)
        }
    }
}
