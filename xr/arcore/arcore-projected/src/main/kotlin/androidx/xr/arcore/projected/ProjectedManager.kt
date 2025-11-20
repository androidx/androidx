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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manages the lifecycle of a Projected session.
 *
 * @property activity The [Activity] instance.
 * @property perceptionManager The [ProjectedPerceptionManager] instance.
 * @property timeSource The [ProjectedTimeSource] instance.
 * @property coroutineContext The [CoroutineContext] for this manager.
 * @property testPerceptionService An optional [IProjectedPerceptionService] for testing
 */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ProjectedManager
internal constructor(
    private val activity: Activity,
    internal val perceptionManager: ProjectedPerceptionManager,
    internal val timeSource: ProjectedTimeSource,
    private val coroutineContext: CoroutineContext,
    private val testPerceptionService: IProjectedPerceptionService? = null,
) : LifecycleManager {
    override val config: Config
        get() = perceptionManager.xrResources.config

    // TODO(b/411154789): Remove once Session runtime invocations are forced to run sequentially.
    internal val running = AtomicBoolean(false)

    private lateinit var serviceConnection: ServiceConnection

    /**
     * This method implements the [LifecycleManager.create] method.
     *
     * This method must be called before any operations can be performed by the
     * [ProjectedPerceptionManager].
     */
    override fun create() {
        runBlocking {
            checkProjectedSupportedAndUpToDate(activity)
            if (testPerceptionService != null) {
                perceptionManager.xrResources.service = testPerceptionService
            } else {
                bindPerceptionService(activity)
            }
        }
    }

    private fun serviceRequired(config: Config): Boolean {
        // The service is required if tracking or geospatial are enabled.
        // I.E. if no features are needed from the service we don't require it.
        return config.deviceTracking == Config.DeviceTrackingMode.LAST_KNOWN ||
            config.geospatial == Config.GeospatialMode.VPS_AND_GPS
    }

    override fun configure(config: Config) {
        if (
            config.deviceTracking == Config.DeviceTrackingMode.DISABLED &&
                config.geospatial == Config.GeospatialMode.VPS_AND_GPS
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
    }

    override fun resume() {}

    internal fun updateTrackingStates(deviceTrackingState: Int, geospatialTrackingState: Int) {
        perceptionManager.xrResources.deviceTrackingState = toTrackingState(deviceTrackingState)
        perceptionManager.xrResources.geospatialTrackingState =
            toTrackingState(geospatialTrackingState)
    }

    private fun toTrackingState(value: Int): TrackingState {
        return when (value) {
            0 -> TrackingState.TRACKING
            1 -> TrackingState.PAUSED
            else -> TrackingState.STOPPED
        }
    }

    private fun toPose(projectedPose: ProjectedPose): Pose {
        return Pose(
            Vector3(projectedPose.vector.x, projectedPose.vector.y, projectedPose.vector.z),
            Quaternion(projectedPose.q.x, projectedPose.q.y, projectedPose.q.z, projectedPose.q.w),
        )
    }

    override suspend fun update(): ComparableTimeMark {
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

    override fun pause() {}

    override fun stop() {
        if (!running.get()) {
            return
        }
        stopServiceInternal()
    }

    private fun startServiceInternal(config: Config) {
        val service = perceptionManager.xrResources.service ?: return
        val serviceConfig = ProjectedConfig()
        // TODO: b/452091636 - Remove hardcoded config" so we remember to address this.
        // TODO: b/455872882 - Currently, Geo is not compatible with 3DoF tracking stack.
        if (config.geospatial == Config.GeospatialMode.VPS_AND_GPS) {
            serviceConfig.geospatialMode = ProjectedGeospatialMode.ENABLED
            serviceConfig.trackingMode = ProjectedTrackingMode.PROJECTED_TRACKING_6DOF
        } else {
            serviceConfig.geospatialMode = ProjectedGeospatialMode.DISABLED
            serviceConfig.trackingMode = ProjectedTrackingMode.PROJECTED_TRACKING_3DOF
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

    internal suspend fun bindPerceptionService(context: Context): IBinder {
        return suspendCancellableCoroutine { continuation ->
            serviceConnection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val service = IProjectedPerceptionService.Stub.asInterface(binder)
                        perceptionManager.xrResources.service = service
                        // TODO: b/445567556 - Pass the API key to the service.

                        // When the service connects, we resume the coroutine with the binder.
                        if (continuation.isActive) {
                            continuation.resume(binder!!)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        running.set(false)
                        // TODO: b/444521361 - Handle glassescore service disconnect
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

    // Verify that Projected is installed and using the current version.
    internal fun checkProjectedSupportedAndUpToDate(activity: Activity) {}

    /**
     * Binds to a perception projected service using provided [ServiceConnection].
     *
     * If service can't be found, the method throws [IllegalStateException]. It means that the
     * system doesn't include a service supporting Projected XR devices.
     *
     * @param context can be either a host [Context] or the Projected device [Context].
     * @return true if the system is in the process of bringing up a service that your client has
     *   permission to bind to; false if the system couldn't find the service or if your client
     *   doesn't have permission to bind to it. Regardless of the return value, you should later
     *   call unbindService to release the connection.
     */
    private fun bindPerception(context: Context, serviceConnection: ServiceConnection): Boolean {
        return context.bindService(
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

    private companion object {
        internal const val ACTION_PERCEPTION_BIND: String =
            "androidx.xr.projected.ACTION_PERCEPTION_BIND"
    }
}
