/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.runtime

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.JxrPlatformAdapterFactory
import androidx.xr.runtime.internal.PermissionNotGrantedException
import androidx.xr.runtime.internal.Runtime
import androidx.xr.runtime.internal.RuntimeFactory
import androidx.xr.runtime.internal.UnsupportedDeviceException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A session is the main entrypoint to features provided by ARCore for Jetpack XR. It manages the
 * system's state and its lifecycle, and contains the state of objects tracked by ARCore for Jetpack
 * XR.
 *
 * This class owns a significant amount of native heap memory. The [Session]'s lifecycle will be
 * scoped to the [Activity] that owns it.
 */
@Suppress("NotCloseable")
public class Session
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@JvmOverloads
public constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val activity: Activity,
    private val _runtime: Runtime?,
    private val _platformAdapter: JxrPlatformAdapter?,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val stateExtenders: List<StateExtender> =
        loadProviders(StateExtender::class.java, STATE_EXTENDER_PROVIDERS),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val sessionConnectors: List<SessionConnector> =
        loadProviders(SessionConnector::class.java, SESSION_CONNECTOR_PROVIDERS),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val coroutineScope: CoroutineScope = CoroutineScope(context = EmptyCoroutineContext),
) {
    init {
        check(!activitySessionMap.containsKey(activity)) {
            "Session already exists for activity: $activity"
        }
        activitySessionMap[activity] = this

        _runtime?.let {
            for (stateExtender in stateExtenders) {
                stateExtender.initialize(it)
            }
        }
        _platformAdapter?.let {
            for (sessionConnector in sessionConnectors) {
                sessionConnector.initialize(_runtime!!.lifecycleManager, it)
            }
        }
    }

    public companion object {
        private val activitySessionMap = ConcurrentHashMap<Activity, Session>()

        /**
         * Creates a new [Session].
         *
         * @param activity the [Activity] that owns the session.
         * @param coroutineContext the [CoroutineContext] that will be used to handle the session's
         *   coroutines.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or [SessionCreatePermissionsNotGranted] if the required
         *   permissions haven't been granted.
         */
        @JvmOverloads
        @JvmStatic
        @Suppress("deprecation")
        public fun create(
            activity: Activity,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): SessionCreateResult = create(activity, coroutineContext, false)

        /**
         * Creates a new [Session].
         *
         * @param activity the [Activity] that owns the session.
         * @param coroutineContext the [CoroutineContext] that will be used to handle the session's
         *   coroutines.
         * @param unscaledGravityAlignedActivitySpace whether to use the unscaled gravity aligned
         *   activity space for the session. When true, causes ActivitySpace for this Session to
         *   always be gravity aligned and to have a scale of [1 unit = 1 Meter]. Note that this
         *   might result in visual inconsistencies between HOME_SPACE and FULL_SPACE_MANAGED modes.
         *   Defaults to True.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or [SessionCreatePermissionsNotGranted] if the required
         *   permissions haven't been granted.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("Will be deleted in a future release.")
        public fun create(
            activity: Activity,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
            unscaledGravityAlignedActivitySpace: Boolean = true,
        ): SessionCreateResult {

            check(activity is LifecycleOwner) { "Unsupported Activity type: ${activity.javaClass}" }

            if (activitySessionMap.containsKey(activity)) {
                return SessionCreateSuccess(activitySessionMap[activity]!!)
            }

            val features = getDeviceFeatures(activity)
            println("Detected device features: $features")

            val runtimeFactory: RuntimeFactory? =
                selectProvider(
                    loadProviders(RuntimeFactory::class.java, RUNTIME_FACTORY_PROVIDERS),
                    features,
                )
            val runtime = runtimeFactory?.createRuntime(activity)
            try {
                runtime?.lifecycleManager?.create()
            } catch (e: PermissionNotGrantedException) {
                return SessionCreatePermissionsNotGranted(e.permissions)
            } catch (e: ApkNotInstalledException) {
                return SessionCreateApkRequired(e.requiredApk)
            } catch (e: UnsupportedDeviceException) {
                return SessionCreateUnsupportedDevice()
            } catch (e: ApkCheckAvailabilityInProgressException) {
                return SessionCreateApkRequired(e.requiredApk)
            } catch (e: ApkCheckAvailabilityErrorException) {
                return SessionCreateApkRequired(e.requiredApk)
            }

            val jxrPlatformAdapterFactory =
                selectProvider(
                    loadProviders(
                        JxrPlatformAdapterFactory::class.java,
                        JXR_PLATFORM_ADAPTER_FACTORY_PROVIDERS,
                    ),
                    features,
                )
            val jxrPlatformAdapter =
                jxrPlatformAdapterFactory?.createPlatformAdapter(
                    activity,
                    unscaledGravityAlignedActivitySpace,
                )

            check(runtime != null || jxrPlatformAdapter != null) {
                "Neither ARCore nor SceneCore are available. Did you forget to add a dependency?"
            }

            val stateExtenders = loadProviders(StateExtender::class.java, STATE_EXTENDER_PROVIDERS)
            val sessionConnectors =
                loadProviders(SessionConnector::class.java, SESSION_CONNECTOR_PROVIDERS)

            val session =
                Session(
                    activity,
                    runtime,
                    jxrPlatformAdapter,
                    stateExtenders,
                    sessionConnectors,
                    CoroutineScope(context = coroutineContext),
                )
            activity.lifecycle.addObserver(session.lifecycleObserver)

            return SessionCreateSuccess(session)
        }

        private val RUNTIME_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.runtime.openxr.OpenXrRuntimeFactory",
                "androidx.xr.runtime.testing.FakeRuntimeFactory",
            )
        private val JXR_PLATFORM_ADAPTER_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.scenecore.impl.JxrPlatformAdapterFactoryAxr",
                "androidx.xr.runtime.testing.FakeJxrPlatformAdapterFactory",
            )
        private val STATE_EXTENDER_PROVIDERS =
            listOf(
                "androidx.xr.arcore.PerceptionStateExtender",
                "androidx.xr.runtime.testing.FakeStateExtender",
            )
        private val SESSION_CONNECTOR_PROVIDERS =
            listOf(
                "androidx.xr.scenecore.Scene",
                "androidx.xr.runtime.testing.FakeSessionConnector",
            )
    }

    private val _state = MutableStateFlow<CoreState>(CoreState(TimeSource.Monotonic.markNow()))
    /** A [StateFlow] of the current state. */
    public val state: StateFlow<CoreState> = _state.asStateFlow()

    /**
     * The [Runtime] instance that is used to manage the session. Applications must NOT use this
     * property directly; they should use ARCore for XR APIs instead.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val runtime: Runtime =
        checkNotNull(_runtime) { "ARCore is not available. Did you forget to add a dependency?" }

    /**
     * The [JxrPlatformAdapter] instance that is used to manage the session. Applications must NOT
     * use this property directly; they should use SceneCore APIs instead.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val platformAdapter: JxrPlatformAdapter =
        checkNotNull(_platformAdapter) {
            "SceneCore is not available. Did you forget to add a dependency?"
        }

    private var updateJob: Job? = null

    /** The current state of the runtime configuration. */
    public val config: Config
        get() = runtime.lifecycleManager.config

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> {}
        }
    }

    private val Activity.lifecycle: Lifecycle
        get() = (this as LifecycleOwner).lifecycle

    /**
     * Sets or changes the configuration to use.
     *
     * @return the result of the operation. This will be a [SessionConfigureSuccess] if the
     *   configuration was successful, or another [SessionConfigureResult] if a certain
     *   configuration criteria was not met.
     * @throws IllegalStateException if the session has been destroyed.
     */
    public fun configure(config: Config): SessionConfigureResult {
        check(activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "Session has been destroyed."
        }
        try {
            runtime.lifecycleManager.configure(config)
        } catch (e: PermissionNotGrantedException) {
            return SessionConfigurePermissionsNotGranted(e.permissions)
        } catch (e: ConfigurationNotSupportedException) {
            return SessionConfigureConfigurationNotSupported()
        } catch (e: FaceTrackingNotCalibratedException) {
            return SessionConfigureCalibrationRequired(RequiredCalibrationType.FACE_TRACKING)
        }
        return SessionConfigureSuccess()
    }

    /**
     * Starts or resumes the session.
     *
     * @return the result of the operation. Can be [SessionResumeSuccess] if the session was
     *   successfully resumed, or [SessionResumePermissionsNotGranted] if the required permissions
     *   haven't been granted.
     */
    private fun resume(): SessionResumeResult {
        _runtime?.let {
            it.lifecycleManager.resume()
            // The update loop is only required when the runtime is present.
            updateJob = coroutineScope.launch { updateLoop() }
        }
        _platformAdapter?.startRenderer()

        return SessionResumeSuccess()
    }

    /**
     * Pauses execution of the session. Objects tracked by the session will not receive updates. The
     * system state will be retained in memory.
     *
     * Calling this method on an inactive session is a no-op.
     */
    private fun pause() {
        _runtime?.lifecycleManager?.pause()
        _platformAdapter?.stopRenderer()
        updateJob?.cancel()
        updateJob = null
    }

    /**
     * Destroys the session, releasing any resources acquired by the session. Objects tracked by the
     * system will not receive updates.
     *
     * Calling this method on a destroyed session is a no-op. Additionally, calling this method on
     * an active session will first call [pause].
     */
    private fun destroy() {
        activitySessionMap.remove(activity)
        // TODO: b/422830134 - Remove this check once there are multiple OpenXrManagers.
        if (activitySessionMap.isEmpty()) {
            _runtime?.lifecycleManager?.stop()
        }
        for (sessionConnector in sessionConnectors) {
            sessionConnector.close()
        }
        _platformAdapter?.dispose()
        coroutineScope.cancel()
    }

    private suspend fun updateLoop() {
        while (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            update()
        }
    }

    /** Produces the latest [CoreState] so it can be emitted downstream. */
    private suspend fun update() {
        val timeMark = runtime.lifecycleManager.update()
        val state = CoreState(timeMark)

        for (stateExtender in stateExtenders) {
            stateExtender.extend(state)
        }

        _state.emit(state)
    }
}
