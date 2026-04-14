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

package androidx.xr.arcore.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.text.get
import kotlin.text.set
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of the [PerceptionRuntime] interface using Projected.
 *
 * @property lifecycleManager that manages the lifecycle of the Projected session
 * @property perceptionManager that manages the perception capabilities of a runtime using Projected
 * @property timeSource the [ProjectedTimeSource] instance
 */
internal class ProjectedRuntime
internal constructor(
    private val context: Context,
    override val lifecycleManager: ProjectedManager,
    override val perceptionManager: ProjectedPerceptionManager,
    internal val timeSource: ProjectedTimeSource,
    private val testPerceptionService: IProjectedPerceptionService? = null,
) : PerceptionRuntime {
    public override val config: Config
        get() = perceptionManager.xrResources.config

    // TODO(b/411154789): Remove once Session runtime invocations are forced to run sequentially.
    internal val running = AtomicBoolean(false)

    private lateinit var serviceConnection: ServiceConnection
    private var serviceBinder: IBinder? = null
    private val serviceDeathRecipient = IBinder.DeathRecipient { disconnect() }

    override fun initialize() {
        runBlocking {
            checkProjectedSupportedAndUpToDate(context)
            if (testPerceptionService == null) {
                bindPerceptionService(context)
            } else {
                perceptionManager.xrResources.service = testPerceptionService
                serviceConnection =
                    object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {}

                        override fun onServiceDisconnected(name: ComponentName?) {}
                    }
            }
        }
    }

    override fun resume() {}

    override fun pause() {}

    override suspend fun update(): ComparableTimeMark? {
        delay(30.milliseconds)
        if (!running.get()) {
            return timeSource.markNow()
        }
        val result = perceptionManager.xrResources.service.update()
        updateTrackingStates(result.deviceTrackingState.toInt(), result.earthTrackingState.toInt())
        perceptionManager.xrResources.arDevice.update(
            toTrackingState(result.deviceTrackingState.toInt()),
            toPose(result.devicePose),
        )
        timeSource.update(result.currentTimeNanos)
        return timeSource.markNow()
    }

    override fun configure(config: Config) {
        if (
            config.deviceTracking == DeviceTrackingMode.DISABLED &&
                config.geospatial == GeospatialMode.VPS_AND_GPS
        ) {
            throw UnsupportedOperationException(
                "Geospatial mode is not supported when device tracking is disabled."
            )
        }
        if (serviceRequired(config)) {
            // Re-configure the running service.
            startServiceInternal(config)
        } else if (running.get()) {
            // Stop the service as it's no longer needed.
            stopServiceInternal()
        }
        perceptionManager.xrResources.config = config
        lifecycleManager.configure(config)
    }

    override fun isSupported(configMode: ConfigMode): Boolean {
        return SUPPORTED_CONFIG_MODES.contains(configMode)
    }

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        // TODO(b/448458070) : Implement this function for projected once we have access to the
        // relevant services.
        throw NotImplementedError(
            "getPreferredBlendMode is not implemented by the arcore-projected runtime."
        )
    }

    override fun destroy() {
        if (!running.get()) {
            return
        }
        stopServiceInternal()
        disconnect()
    }

    internal fun updateTrackingStates(deviceTrackingState: Int, geospatialTrackingState: Int) {
        perceptionManager.xrResources.trackingState = toTrackingState(deviceTrackingState)
        perceptionManager.xrResources.geospatialTrackingState =
            toTrackingState(geospatialTrackingState)
    }

    private suspend fun bindPerceptionService(context: Context): IBinder {
        return suspendCancellableCoroutine { continuation ->
            serviceConnection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = IProjectedPerceptionService.Stub.asInterface(binder)
                        perceptionManager.xrResources.service = service
                        serviceBinder = binder
                        serviceBinder?.linkToDeath(serviceDeathRecipient, /* flags= */ 0)

                        // TODO: b/445567556 - Pass the API key to the service.

                        // When the service connects, we resume the coroutine with the binder.
                        if (continuation.isActive) {
                            continuation.resume(binder!!)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        disconnect()
                    }

                    override fun onBindingDied(name: ComponentName?) {
                        running.set(false)
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Binding died for $name")
                            )
                        }
                    }
                }

            // When the coroutine is cancelled, we must unbind the service.
            continuation.invokeOnCancellation { context.unbindService(serviceConnection) }

            val isBindingPermitted = bindPerception(context, serviceConnection)
            check(isBindingPermitted) {
                "Projected perception service not found or binding was not permitted."
            }
        }
    }

    private fun disconnect() {
        running.set(false)
        try {
            if (::serviceConnection.isInitialized) {
                context.unbindService(serviceConnection)
                serviceBinder?.unlinkToDeath(serviceDeathRecipient, /* flags= */ 0)
            }
        } catch (e: IllegalArgumentException) {
            XrLog.warn(e) { "Tried to unbind service that was already unbound." }
        } catch (e: NoSuchElementException) {
            XrLog.warn(e) { "Tried to unbind service that was already unbound." }
        }
        serviceBinder = null
    }

    // Verify that Projected is installed and using the current version.
    internal fun checkProjectedSupportedAndUpToDate(context: Context) {}

    /**
     * Binds to a perception projected service using provided [ServiceConnection].
     *
     * If service can't be found, the method throws [IllegalStateException]. It means that the
     * system doesn't include a service supporting Projected XR devices.
     *
     * @param context can be either a host [Context] or the Projected device [Context]
     * @param serviceConnection the [ServiceConnection] to use
     * @return true if the system is in the process of bringing up a service that your client has
     *   permission to bind to; false if the system couldn't find the service or if your client
     *   doesn't have permission to bind to it
     */
    private fun bindPerception(context: Context, serviceConnection: ServiceConnection): Boolean {
        return testPerceptionService != null ||
            context.bindService(
                getIntent(context, ACTION_PERCEPTION_BIND),
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
    }

    // LINT.IfChange(get_intent)
    private fun getIntent(context: Context, intentAction: String): Intent {
        val intent = Intent(intentAction)
        val projectedSystemServiceResolveInfo = findProjectedSystemService(context, intent)
        val foundService =
            ComponentName(
                projectedSystemServiceResolveInfo.serviceInfo.packageName,
                projectedSystemServiceResolveInfo.serviceInfo.name,
            )

        return Intent().apply {
            component = foundService
            action = intentAction
        }
    }

    private fun findProjectedSystemService(context: Context, intent: Intent): ResolveInfo {
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentServices(intent, PackageManager.GET_RESOLVED_FILTER)

        val resolveInfoSystemApps =
            resolveInfoList.filter {
                val applicationInfo =
                    context.packageManager.getApplicationInfo(
                        it.serviceInfo.packageName,
                        /* flags= */ 0,
                    )
                (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }

        check(resolveInfoSystemApps.isNotEmpty()) {
            "System doesn't include a service supporting Projected XR devices."
        }
        check(resolveInfoSystemApps.size == 1) {
            "More than one system service found for action: $intent."
        }

        return resolveInfoSystemApps.first()
    }

    // LINT.ThenChange(/xr/projected/projected/src/main/kotlin/androidx/xr/projected/ProjectedServiceBinding.kt:get_intent)

    @OptIn(PreviewSpatialApi::class)
    private fun serviceRequired(config: Config): Boolean {
        // The service is required if tracking or geospatial are enabled.
        // I.E. if no features are needed from the service we don't require it.
        return config.deviceTracking == DeviceTrackingMode.SPATIAL_LAST_KNOWN ||
            config.deviceTracking == DeviceTrackingMode.INERTIAL_LAST_KNOWN ||
            config.geospatial == GeospatialMode.VPS_AND_GPS
    }

    @OptIn(PreviewSpatialApi::class)
    private fun startServiceInternal(config: Config) {
        val service = perceptionManager.xrResources.service ?: return
        val serviceConfig = ProjectedConfig()
        // TODO: b/452091636 - Remove hardcoded config" so we remember to address this.
        // TODO: b/455872882 - Currently, Geo is not compatible with 3DoF tracking stack.
        if (config.geospatial == GeospatialMode.VPS_AND_GPS) {
            serviceConfig.geospatialMode = ProjectedGeospatialMode.ENABLED
            serviceConfig.trackingMode = ProjectedTrackingMode.PROJECTED_TRACKING_6DOF
        } else {
            serviceConfig.geospatialMode = ProjectedGeospatialMode.DISABLED
            serviceConfig.trackingMode =
                if (config.deviceTracking == DeviceTrackingMode.INERTIAL_LAST_KNOWN) {
                    ProjectedTrackingMode.PROJECTED_TRACKING_3DOF
                } else {
                    ProjectedTrackingMode.PROJECTED_TRACKING_6DOF
                }
        }
        val status = service.startWithConfiguration(serviceConfig)
        if (status == ProjectedStatus.PROJECTED_ERROR_FINE_LOCATION_PERMISSION_NOT_GRANTED) {
            throw SecurityException(
                "Geospatial mode requested but app does not have ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION"
            )
        }
        running.set(true)
    }

    private fun stopServiceInternal() {
        perceptionManager.xrResources.service?.stop()
        running.set(false)
    }

    private fun toPose(projectedPose: ProjectedPose): Pose {
        return Pose(
            Vector3(projectedPose.vector.x, projectedPose.vector.y, projectedPose.vector.z),
            Quaternion(projectedPose.q.x, projectedPose.q.y, projectedPose.q.z, projectedPose.q.w),
        )
    }

    private fun toTrackingState(value: Int): TrackingState {
        return when (value) {
            0 -> TrackingState.TRACKING
            1 -> TrackingState.PAUSED
            else -> TrackingState.STOPPED
        }
    }

    internal companion object {
        internal const val ACTION_PERCEPTION_BIND: String =
            "androidx.xr.projected.ACTION_PERCEPTION_BIND"

        @VisibleForTesting
        internal val SUPPORTED_CONFIG_MODES: Set<ConfigMode> =
            setOf(
                PlaneTrackingMode.DISABLED,
                HandTrackingMode.DISABLED,
                DeviceTrackingMode.DISABLED,
                DepthEstimationMode.DISABLED,
                AnchorPersistenceMode.DISABLED,
                FaceTrackingMode.DISABLED,
                GeospatialMode.DISABLED,
                GeospatialMode.VPS_AND_GPS,
                EyeTrackingMode.DISABLED,
            )
    }
}
