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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.runtime

import android.app.Activity
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.FaceTrackingNotCalibratedException
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import androidx.xr.runtime.internal.RenderingRuntimeFactory
import androidx.xr.runtime.internal.SceneRuntimeFactory
import androidx.xr.runtime.internal.UnsupportedDeviceException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val stateExtenders: List<StateExtender> =
        loadProviders(StateExtender::class.java, STATE_EXTENDER_PROVIDERS),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val sessionConnectors: List<SessionConnector> =
        loadProviders(SessionConnector::class.java, SESSION_CONNECTOR_PROVIDERS),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val runtimes: List<JxrRuntime> = emptyList(),
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val coroutineScope: CoroutineScope = CoroutineScope(context = EmptyCoroutineContext),
) {
    init {
        check(!activitySessionMap.containsKey(activity)) {
            "Session already exists for activity: $activity"
        }
        activitySessionMap[activity] = this

        for (stateExtender in stateExtenders) {
            stateExtender.initialize(runtimes)
        }
        for (sessionConnector in sessionConnectors) {
            sessionConnector.initialize(runtimes)
        }
    }

    public companion object {
        private val activitySessionMap = ConcurrentHashMap<Activity, Session>()

        /**
         * Creates a new [Session].
         *
         * @param activity the [Activity] that provides the context for the session's resources and
         *   controls the session's runtime state based on the [activity]'s lifecycle.
         * @param coroutineContext the [CoroutineContext] that will be used to handle the session's
         *   coroutines.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or another [SessionCreateResult] if a certain criteria was not
         *   met.
         * @throws [SecurityException] if the [Session] is backed by Google Play Services for AR and
         *   [android.Manifest.permission.CAMERA] has not been granted to the calling application.
         * @sample androidx.xr.arcore.samples.callSessionCreate
         */
        @JvmOverloads
        @JvmStatic
        @Suppress("deprecation")
        public fun create(
            activity: Activity,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): SessionCreateResult =
            create(activity, coroutineContext, unscaledGravityAlignedActivitySpace = false)

        /**
         * Creates a new [Session].
         *
         * @param activity the [Activity] that provides the context for the session's resources and
         *   controls the session's runtime state based on the [activity]'s lifecycle.
         * @param coroutineContext the [CoroutineContext] that will be used to handle the session's
         *   coroutines.
         * @param unscaledGravityAlignedActivitySpace whether to use the unscaled gravity aligned
         *   activity space for the session. When true, causes ActivitySpace for this session to
         *   always be gravity aligned and to have a scale of [1 unit = 1 Meter]. Note that this
         *   might result in visual inconsistencies between HOME_SPACE and FULL_SPACE_MANAGED modes.
         *   Defaults to false.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or another [SessionCreateResult] if a certain criteria was not
         *   met.
         * @throws [SecurityException] if the [Session] is backed by Google Play Services for AR and
         *   [android.Manifest.permission.CAMERA] has not been granted to the calling application.
         * @sample androidx.xr.arcore.samples.callSessionCreate
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        @Deprecated("Will be deleted in a future release.")
        public fun create(
            activity: Activity,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
            unscaledGravityAlignedActivitySpace: Boolean = false,
        ): SessionCreateResult {
            check(activity is LifecycleOwner) { "Unsupported Activity type: ${activity.javaClass}" }
            return create(
                activity,
                lifecycleOwner = activity,
                coroutineContext,
                unscaledGravityAlignedActivitySpace,
            )
        }

        /**
         * Creates a new [Session] with a provided [LifecycleOwner].
         *
         * Only use this version of the constructor if you desire to have finer control over the
         * session's lifecycle. The [lifecycleOwner]'s lifecycle must still be bounded within the
         * lifecycle of the provided [activity]. The session will be automatically destroyed if the
         * [activity]'s lifecycle becomes destroyed.
         *
         * @param activity the [Activity] that provides the context for the session's resources.
         * @param lifecycleOwner the [LifecycleOwner] whose lifecycle controls the runtime state of
         *   the session.
         * @param coroutineContext the [CoroutineContext] that will be used to handle the session's
         *   coroutines.
         * @return the result of the operation. Can be [SessionCreateSuccess], which contains the
         *   newly created session, or another [SessionCreateResult] if a certain criteria was not
         *   met.
         * @throws [SecurityException] if the [Session] is backed by Google Play Services for AR and
         *   [android.Manifest.permission.CAMERA] has not been granted to the calling application.
         * @sample androidx.xr.arcore.samples.callSessionCreate
         */
        @JvmOverloads
        @JvmStatic
        @Suppress("deprecation")
        public fun create(
            activity: Activity,
            lifecycleOwner: LifecycleOwner,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
        ): SessionCreateResult =
            create(
                activity,
                lifecycleOwner,
                coroutineContext,
                unscaledGravityAlignedActivitySpace = false,
            )

        private fun create(
            activity: Activity,
            lifecycleOwner: LifecycleOwner,
            coroutineContext: CoroutineContext = EmptyCoroutineContext,
            unscaledGravityAlignedActivitySpace: Boolean = false,
        ): SessionCreateResult {
            check(activity is LifecycleOwner) { "Unsupported Activity type: ${activity.javaClass}" }

            check(!activity.isDestroyed) { "Cannot create a new session on a destroyed activity." }

            if (activitySessionMap.containsKey(activity)) {
                return SessionCreateSuccess(activitySessionMap[activity]!!)
            }

            val features = getDeviceActivityFeatures(activity)

            val runtimes = mutableListOf<JxrRuntime>()

            val perceptionRuntimeFactory: PerceptionRuntimeFactory? =
                selectProvider(
                    loadProviders(PerceptionRuntimeFactory::class.java, RUNTIME_FACTORY_PROVIDERS),
                    features,
                )
            val perceptionRuntime =
                perceptionRuntimeFactory?.createRuntime(activity, coroutineContext)
            try {
                perceptionRuntime?.initialize()
            } catch (e: ApkNotInstalledException) {
                return SessionCreateApkRequired(e.requiredApk)
            } catch (e: UnsupportedDeviceException) {
                return SessionCreateUnsupportedDevice()
            } catch (e: ApkCheckAvailabilityInProgressException) {
                return SessionCreateApkRequired(e.requiredApk)
            } catch (e: ApkCheckAvailabilityErrorException) {
                return SessionCreateApkRequired(e.requiredApk)
            }
            perceptionRuntime?.let { runtimes.add(it) }

            val sceneRuntimeFactory =
                selectProvider(
                    loadProviders(SceneRuntimeFactory::class.java, SCENE_RUNTIME_FACTORY_PROVIDERS),
                    features,
                )
            val sceneRuntime =
                sceneRuntimeFactory?.create(activity, unscaledGravityAlignedActivitySpace)
            sceneRuntime?.let { runtimes.add(it) }

            val renderingRuntimeFactory =
                selectProvider(
                    loadProviders(
                        RenderingRuntimeFactory::class.java,
                        RENDERING_RUNTIME_FACTORY_PROVIDERS,
                    ),
                    features,
                )
            val renderingRuntime = renderingRuntimeFactory?.create(runtimes, activity)
            renderingRuntime?.let { runtimes.add(it) }

            check(runtimes.isNotEmpty()) {
                "Neither ARCore nor SceneCore are available. Did you forget to add a dependency?"
            }

            val stateExtenders = loadProviders(StateExtender::class.java, STATE_EXTENDER_PROVIDERS)
            val sessionConnectors =
                loadProviders(SessionConnector::class.java, SESSION_CONNECTOR_PROVIDERS)

            val session =
                Session(
                    activity,
                    stateExtenders,
                    sessionConnectors,
                    runtimes,
                    CoroutineScope(context = coroutineContext),
                )

            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.lifecycle.addObserver(session.lifecycleObserver)
                if (lifecycleOwner != activity) {
                    activity.lifecycle.addObserver(
                        observer =
                            LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_DESTROY -> session.destroy()
                                    else -> {}
                                }
                            }
                    )
                }
            }

            return SessionCreateSuccess(session)
        }

        private val RUNTIME_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.arcore.projected.ProjectedRuntimeFactory",
                "androidx.xr.arcore.playservices.ArCoreRuntimeFactory",
                "androidx.xr.arcore.openxr.OpenXrRuntimeFactory",
                "androidx.xr.arcore.testing.FakePerceptionRuntimeFactory",
            )

        private val SCENE_RUNTIME_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.scenecore.spatial.core.SpatialSceneRuntimeFactory",
                "androidx.xr.scenecore.testing.FakeSceneRuntimeFactory",
            )

        private val RENDERING_RUNTIME_FACTORY_PROVIDERS =
            listOf(
                "androidx.xr.scenecore.spatial.rendering.SpatialRenderingRuntimeFactory",
                "androidx.xr.scenecore.testing.FakeRenderingRuntimeFactory",
            )

        private val STATE_EXTENDER_PROVIDERS =
            listOf(
                "androidx.xr.arcore.PerceptionStateExtender",
                "androidx.xr.arcore.playservices.CameraStateExtender",
                "androidx.xr.arcore.testing.FakeStateExtender",
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

    private var updateJob: Job? = null

    private val lock = Mutex()

    /** The current state of the runtime configuration. */
    @GuardedBy("lock")
    public var config: Config =
        Config(
            Config.PlaneTrackingMode.DISABLED,
            augmentedObjectCategories = listOf(),
            Config.HandTrackingMode.DISABLED,
            Config.DeviceTrackingMode.DISABLED,
            Config.DepthEstimationMode.DISABLED,
            Config.AnchorPersistenceMode.DISABLED,
        )
        private set

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
     * Sets or changes the [Config] to use for the Session.
     *
     * The passed [config] will overwrite all [ConfigMode] values. Not all runtimes will support
     * every [ConfigMode], and the desired modes should first be queried for availability using
     * [ConfigMode.isSupported] before configuring.
     *
     * It is recommended to use and modify the [Config.copy] of the current [Session.config] to
     * maintain the current configuration state aside from the desired changes.
     *
     * Note that enabling most configurations will increase hardware resource consumption and should
     * only be enabled if needed.
     *
     * @param config the [Config] that will be enabled if successful.
     * @return the result of the operation. This will be a [SessionConfigureSuccess] if the
     *   configuration was successful, or another [SessionConfigureResult] if a certain
     *   configuration criteria was not met. In the case of the latter, the previous
     *   [Session.config] will remain active.
     * @throws [IllegalStateException] if the session has been destroyed.
     * @throws [UnsupportedOperationException] if the configuration is not supported.
     * @throws [SecurityException] if the necessary permissions have not been granted to the calling
     *   application for the provided configuration.
     */
    public fun configure(config: Config): SessionConfigureResult {
        check(activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "Session has been destroyed."
        }
        return runBlocking {
            lock.withLock {
                try {
                    for (runtime in runtimes) {
                        runtime.configure(config)
                    }
                } catch (e: FaceTrackingNotCalibratedException) {
                    return@withLock SessionConfigureCalibrationRequired(
                        RequiredCalibrationType.REQUIRED_CALIBRATION_TYPE_FACE_TRACKING
                    )
                }
                this@Session.config = config
                SessionConfigureSuccess()
            }
        }
    }

    /** Starts or resumes the session. */
    private fun resume() {
        for (runtime in runtimes) {
            runtime.resume()
        }
        updateJob = coroutineScope.launch { updateLoop() }
    }

    /**
     * Pauses execution of the session. Objects tracked by the session will not receive updates. The
     * system state will be retained in memory.
     *
     * Calling this method on an inactive session is a no-op.
     */
    private fun pause() {
        for (runtime in runtimes) {
            runtime.pause()
        }
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
        for (runtime in runtimes) {
            runtime.destroy()
        }
        for (sessionConnector in sessionConnectors) {
            sessionConnector.close()
        }
        coroutineScope.cancel()
    }

    private suspend fun updateLoop() {
        while (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            lock.withLock { update() }
        }
    }

    /** Produces the latest [CoreState] so it can be emitted downstream. */
    @GuardedBy("lock")
    private suspend fun update() {
        var timeMark: ComparableTimeMark? = null
        for (runtime in runtimes) {
            runtime.update()?.let { timeMark = it }
        }
        check(timeMark != null)
        val state = CoreState(timeMark)

        for (stateExtender in stateExtenders) {
            stateExtender.extend(state)
        }
        _state.emit(state)
    }
}
