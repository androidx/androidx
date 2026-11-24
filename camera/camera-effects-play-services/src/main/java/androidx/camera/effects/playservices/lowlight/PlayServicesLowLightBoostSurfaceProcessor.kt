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
import android.os.Looper
import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.core.os.HandlerCompat
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.cameralowlight.LowLightBoostClient
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [SurfaceProcessor] implementation for [LowLightBoostEffect] backed by Google Play Services
 * `LowLightBoostClient`.
 *
 * Each [LowLightBoostEffect] instance owns a [PlayServicesLowLightBoostSurfaceProcessor] to track
 * its per-instance state ([committedSnapshot]), listeners and enabled flag, while delegating the
 * process-wide `LowLightBoostSession` management and dual-bridge OpenGL rendering to
 * [LowLightBoostSessionManager] and [LowLightBoostRenderPipeline].
 *
 * **Threading:** [LowLightBoostEffect] delivers the [SurfaceProcessor] callbacks on [glExecutor],
 * the single GL thread that all session and OpenGL work runs on. States are posted on the GL thread
 * ([postState]) and committed on the main thread, which then notifies the listeners.
 *
 * **Resource Lifecycle:** Because [LowLightBoostEffect] instances may be reused across multiple
 * camera bindings, configuration changes, and lens switches, this processor does not permanently
 * shut down on a single unbind. Instead, when a camera or effect is unbound, CameraX closes the
 * upstream [SurfaceRequest] and downstream [SurfaceOutput]s, which triggers
 * [LowLightBoostSessionManager.scheduleSessionReleaseIfIdle] to automatically tear down the
 * session, OpenGL bridges, and active processor reference once the pipeline is idle.
 *
 * **API level:** The `LowLightBoostSession` requires Android 11 (API level 30). On lower API
 * levels, this processor never creates a [LowLightBoostClient] or session and treats the device as
 * unsupported (passthrough with [LowLightBoostEffect.STATUS_OFF] and a [LowLightBoostException]
 * with [LowLightBoostException.ERROR_NOT_SUPPORTED] via [LowLightBoostEffect.error] when bound
 * while [isLowLightBoostEnabled] is `true`), while [isLowLightBoostEnabled] behaves uniformly
 * across all API levels.
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

    /** The handler of the main thread, which commits the posted states and notifies listeners. */
    private val mainHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    /** The executor of the GL thread, which the [SurfaceProcessor] callbacks must run on. */
    internal val glExecutor: Executor
        get() = sessionManager.glExecutor

    /**
     * The latest state posted by [postState] on the GL thread. Unlike [committedSnapshot], it
     * includes a state that is posted but not yet committed on the main thread, so the session
     * manager can check the state synchronously on the GL thread.
     */
    @Volatile
    internal var lastPostedState: LowLightBoostStateSnapshot = LowLightBoostStateSnapshot.OFF
        private set

    /**
     * The state backing [LowLightBoostEffect.status] and [LowLightBoostEffect.error]. It is only
     * written on the main thread, as a single reference, before the listeners of the transition are
     * notified, so both values are always read consistently on the main thread.
     */
    @Volatile
    internal var committedSnapshot: LowLightBoostStateSnapshot = LowLightBoostStateSnapshot.OFF
        private set

    private val listenerLock = Any()

    @GuardedBy("listenerLock")
    private val listeners = LinkedHashMap<LowLightBoostEffect.Listener, ListenerRegistration>()

    @GuardedBy("listenerLock")
    private val transitionObservers = LinkedHashMap<Runnable, AtomicBoolean>()

    @Volatile private var boostEnabled = true

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
     * Posts [snapshot] to be committed on the main thread, unless it equals the latest posted
     * state, so listeners aren't notified again, e.g. when a new SurfaceRequest reuses the session.
     * Called on the GL thread.
     */
    internal fun postState(snapshot: LowLightBoostStateSnapshot) {
        if (snapshot == lastPostedState) {
            return
        }
        lastPostedState = snapshot
        // Every posted state is committed in order, so no transition is dropped.
        mainHandler.post { commitAndDispatchOnMain(snapshot) }
    }

    /**
     * Commits [snapshot] as the [committedSnapshot] and notifies the listeners and the transition
     * observers registered at this point, unless it equals the committed state.
     *
     * The state is committed before any callback, so the callbacks of this transition read the new
     * [LowLightBoostEffect.status] and [LowLightBoostEffect.error]. A listener removed by a
     * callback, including one of an earlier listener, isn't called for the rest of the transition.
     * A listener added by a callback is only notified of later transitions.
     */
    @MainThread
    private fun commitAndDispatchOnMain(snapshot: LowLightBoostStateSnapshot) {
        val previous = committedSnapshot
        if (snapshot == previous) {
            return
        }
        committedSnapshot = snapshot
        val statusChanged = snapshot.status != previous.status
        val errorChanged = snapshot.error != previous.error
        val (registrations, observers) =
            synchronized(listenerLock) {
                listeners.values.toList() to transitionObservers.entries.toList()
            }
        for (registration in registrations) {
            registration.dispatch(snapshot, statusChanged, errorChanged)
        }
        // Notified once per transition, even if both the status and the error changed.
        notifyTransitionObservers(observers)
    }

    @MainThread
    private fun dispatchEnabledChangedOnMain(
        enabled: Boolean,
        registrations: List<ListenerRegistration>,
        observers: List<Map.Entry<Runnable, AtomicBoolean>>,
    ) {
        for (registration in registrations) {
            registration.dispatchEnabled(enabled)
        }
        notifyTransitionObservers(observers)
    }

    @MainThread
    private fun notifyTransitionObservers(observers: List<Map.Entry<Runnable, AtomicBoolean>>) {
        for ((observer, isActive) in observers) {
            if (isActive.get()) {
                observer.run()
            }
        }
    }

    /**
     * Registers [listener] to be notified of future state transitions, on the main thread if
     * [executor] is null, or sequentially on [executor] otherwise. Does nothing if [listener] is
     * already registered. Can be called on any thread.
     */
    internal fun addListener(executor: Executor?, listener: LowLightBoostEffect.Listener) {
        synchronized(listenerLock) {
            if (listeners.containsKey(listener)) {
                return
            }
            // Calls of the same listener mustn't overlap or be reordered, even if [executor] runs
            // tasks concurrently, e.g. a thread pool.
            listeners[listener] =
                ListenerRegistration(
                    listener,
                    executor?.let(CameraXExecutors::newSequentialExecutor),
                )
        }
    }

    /**
     * Unregisters [listener], including its calls that are pending on its executor or remaining in
     * the current transition. Does nothing if [listener] isn't registered. Can be called on any
     * thread.
     */
    internal fun removeListener(listener: LowLightBoostEffect.Listener) {
        val removed = synchronized(listenerLock) { listeners.remove(listener) }
        removed?.isActive?.set(false)
    }

    /**
     * Registers [observer] to be run on the main thread once per future state transition, after the
     * transition is committed. Does nothing if [observer] is already registered.
     */
    internal fun addTransitionObserver(observer: Runnable) {
        synchronized(listenerLock) {
            transitionObservers.getOrPut(observer) { AtomicBoolean(true) }
        }
    }

    /**
     * Unregisters [observer], which then isn't run again, even for the rest of the current
     * transition.
     */
    internal fun removeTransitionObserver(observer: Runnable) {
        val isActive = synchronized(listenerLock) { transitionObservers.remove(observer) }
        isActive?.set(false)
    }

    /** The number of registered transition observers. */
    @VisibleForTesting
    internal val transitionObserverCount: Int
        get() = synchronized(listenerLock) { transitionObservers.size }

    /**
     * Enables or disables Low Light Boost enhancement dynamically, notifies listeners and
     * transition observers, and delegates session/renderer state updates to [sessionManager] on the
     * GL thread. Does nothing if the value matches the current setting. Can be called on any
     * thread.
     */
    internal var isLowLightBoostEnabled: Boolean
        get() = boostEnabled
        set(enabled) {
            synchronized(listenerLock) {
                if (boostEnabled == enabled) {
                    return
                }
                boostEnabled = enabled
                val registrations = listeners.values.toList()
                val observers = transitionObservers.entries.toList()
                // Enqueue under listenerLock so concurrent writers keep the same FIFO order on
                // main and GL.
                mainHandler.post {
                    dispatchEnabledChangedOnMain(enabled, registrations, observers)
                }
                glExecutor.execute { sessionManager.onBoostEnabledChanged(this, enabled) }
            }
        }

    /**
     * Posts [LowLightBoostEffect.STATUS_OFF] with [error] normalized for
     * [LowLightBoostEffect.error].
     */
    internal fun notifyError(error: Throwable) {
        postState(LowLightBoostStateSnapshot.error(LowLightBoostSupport.toPublicError(error)))
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
                if (isLowLightBoostEnabled) {
                    notifyError(e)
                }
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

    /**
     * A registered [LowLightBoostEffect.Listener]. [sequentialExecutor] is null for a listener
     * called on the main thread, while the transition is committed.
     */
    private class ListenerRegistration(
        val listener: LowLightBoostEffect.Listener,
        val sequentialExecutor: Executor?,
    ) {
        /** Cleared when the listener is removed, so its pending and remaining calls are skipped. */
        val isActive = AtomicBoolean(true)

        @MainThread
        fun dispatch(
            snapshot: LowLightBoostStateSnapshot,
            statusChanged: Boolean,
            errorChanged: Boolean,
        ) {
            dispatchAsync {
                if (statusChanged && isActive.get()) {
                    listener.onStatusChanged(snapshot.status)
                }
                if (errorChanged && isActive.get()) {
                    listener.onErrorChanged(snapshot.error)
                }
            }
        }

        @MainThread
        fun dispatchEnabled(enabled: Boolean) {
            dispatchAsync {
                if (isActive.get()) {
                    listener.onLowLightBoostEnabledChanged(enabled)
                }
            }
        }

        @MainThread
        private inline fun dispatchAsync(crossinline action: () -> Unit) {
            if (!isActive.get()) {
                return
            }
            if (sequentialExecutor == null) {
                action()
                return
            }
            try {
                sequentialExecutor.execute { action() }
            } catch (e: RejectedExecutionException) {
                // E.g. the app shut down the executor without removing the listener first.
                Logger.w(TAG, "Failed to notify Low Light Boost listener $listener", e)
            }
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
