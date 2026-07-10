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

package androidx.xr.projected

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.view.Display.DEFAULT_DISPLAY
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.xr.projected.ProjectedActivityCompat.Companion.create
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.binding.ProjectedServiceConnection.ProjectedIntentAction.Companion.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedInputEventListener
import androidx.xr.projected.platform.IProjectedPermissionRequestCallback
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedInputEvent
import androidx.xr.projected.platform.ProjectedPermissionRequestData
import androidx.xr.projected.platform.ProjectedPermissionRequestState
import java.lang.ref.WeakReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Class providing Projected device-specific features for Activity, like listening to Projected
 * input events.
 *
 * Use [create] to create an instance of this class. Use [close] to clear the instance.
 */
@ExperimentalProjectedApi
public class ProjectedActivityCompat
private constructor(
    private val connection: ProjectedServiceConnection,
    private val projectedService: IProjectedService,
) : AutoCloseable {

    /** Flow providing a stream of Projected input events. */
    public val projectedInputEvents: Flow<androidx.xr.projected.ProjectedInputEvent> =
        callbackFlow {
            val projectedActionsListener: IProjectedInputEventListener =
                object : IProjectedInputEventListener.Stub() {
                    override fun onProjectedInputEvent(inputEvent: ProjectedInputEvent) {
                        try {
                            val projectedInputAction =
                                androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
                                    .fromCode(inputEvent.action)
                            trySend(ProjectedInputEvent(projectedInputAction))
                        } catch (_: Exception) {}
                    }

                    override fun getInterfaceVersion(): Int = VERSION
                }

            val job = launch {
                connection.isServiceConnected.collect { isConnected ->
                    if (!isConnected) {
                        channel.close()
                    }
                }
            }

            projectedService.registerProjectedInputEventListener(projectedActionsListener)

            awaitClose {
                projectedService.unregisterProjectedInputEventListener(projectedActionsListener)
                job.cancel()
            }
        }

    override fun close() {
        connection.disconnect()
    }

    public companion object {

        private const val LAUNCH_HOST_ACTIVITY_FLAGS =
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP

        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedActivityCompat] when the connection is established.
         *
         * @param context The context to use for binding to the service.
         * @return A [ProjectedActivityCompat] instance.
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @deprecated Use [ProjectedActivityCompat.create(activity: Activity)] instead
         *
         * TODO(b/479214966): Remove this once all clients have been migrated.
         */
        @JvmStatic
        @Deprecated("Use create(activity: Activity) instead")
        public suspend fun create(context: Context): ProjectedActivityCompat {
            val serviceConnection = ProjectedServiceConnection(context, ACTION_BIND)
            return ProjectedActivityCompat(
                serviceConnection,
                projectedService = serviceConnection.connect(),
            )
        }

        /**
         * Connects to the service providing features for Projected devices and returns the
         * ProjectedActivityCompat when the connection is established.
         *
         * @param activity The [Activity] running on a Projected device
         * @return A [ProjectedActivityCompat] instance
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @throws IllegalArgumentException if provided Activity is not running on a Projected
         *   device
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        public suspend fun create(activity: Activity): ProjectedActivityCompat {
            require(
                ProjectedContext.isProjectedDeviceContext(activity),
                { "Provided Activity is not running on a Projected device." },
            )
            val serviceConnection = ProjectedServiceConnection(activity, ACTION_BIND)
            return ProjectedActivityCompat(
                serviceConnection,
                projectedService = serviceConnection.connect(),
            )
        }

        /**
         * Requests permissions to be granted to this application. These permissions must be
         * declared in your manifest, they should not be granted to your app, and they should have
         * protection level dangerous, regardless of whether they are declared by the platform or a
         * third-party app.
         *
         * When this method is called from a Projected [Activity], a new activity will be launched
         * on the Projected device and another activity will be launched on the host device (e.g.
         * phone). The Projected activity will request the user to go to the host activity to act on
         * the permission request. The host activity will request permissions on behalf of the
         * calling application and the system dialog for permission requests will appear for the
         * user to grant/deny the permission.
         *
         * After the user has acted on the permission request, both the Projected activity and host
         * activity will finish and the results will be delivered to the
         * [Activity.onRequestPermissionsResult] method overridden by the activity passed to this
         * method.
         *
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @throws IllegalArgumentException if provided Activity is not running on a Projected
         *   device
         */
        @WorkerThread
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        @JvmStatic
        public fun requestPermissions(
            activity: Activity,
            permissions: Array<String>,
            @IntRange(from = 0) requestCode: Int,
        ) {
            require(
                ProjectedContext.isProjectedDeviceContext(activity),
                { "Provided Activity is not running on a Projected device." },
            )
            val serviceConnection = ProjectedServiceConnection(activity, ACTION_BIND)
            runBlocking {
                val projectedService = serviceConnection.connect()
                val componentName =
                    ComponentName(activity, TrampolineRequestPermissionsOnHostActivity::class.java)
                projectedService.launchProjectedPermissionRequest(
                    ProjectedPermissionRequestData().apply { this.permissions = permissions },
                    createPermissionRequestCallback(
                        activity,
                        permissions,
                        componentName,
                        requestCode,
                        projectedService,
                        serviceConnection,
                    ),
                )
            }
        }

        private fun createPermissionRequestCallback(
            activity: Activity,
            permissions: Array<String>,
            componentName: ComponentName,
            requestCode: Int,
            projectedService: IProjectedService,
            serviceConnection: ProjectedServiceConnection,
        ): IProjectedPermissionRequestCallback.Stub {
            val activityWeakReference = WeakReference(activity)
            return object : IProjectedPermissionRequestCallback.Stub() {

                @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
                override fun onProjectedPermissionRequestStateChanged(
                    state: Int,
                    pendingIntent: PendingIntent?,
                ) {
                    val activityReference = activityWeakReference.get()
                    if (activityReference == null) {
                        serviceConnection.disconnect()
                        return
                    }

                    when (state) {
                        ProjectedPermissionRequestState.ALLOWED -> {
                            val pendingIntent =
                                checkNotNull(
                                    pendingIntent,
                                    {
                                        "Intent to launch a Projected activity has not been provided."
                                    },
                                )

                            activityReference.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                /* requestCode= */ 0,
                                /* fillInIntent= */ null,
                                /* flagsMask= */ 0,
                                /* flagsValues= */ 0,
                                /* extraFlags= */ 0,
                                /* options= */ null,
                            )

                            val resultReceiver =
                                createResultReceiver(
                                    projectedService,
                                    activityReference,
                                    requestCode,
                                    serviceConnection,
                                )

                            val intent =
                                Intent().apply {
                                    component = componentName
                                    putExtra(
                                        ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER,
                                        resultReceiver,
                                    )
                                    putExtra(
                                        ProjectedPermissionsConstants.EXTRA_PERMISSIONS,
                                        permissions,
                                    )
                                    addFlags(LAUNCH_HOST_ACTIVITY_FLAGS)
                                }

                            startActivityOnHost(activityReference, intent)
                        }
                        ProjectedPermissionRequestState.DENIED -> {
                            val grantResults =
                                IntArray(permissions.size) { PackageManager.PERMISSION_DENIED }
                            activityReference.onRequestPermissionsResult(
                                requestCode,
                                permissions,
                                grantResults,
                                activityReference.deviceId,
                            )
                            serviceConnection.disconnect()
                        }
                    }
                }

                override fun getInterfaceVersion(): Int = VERSION
            }
        }

        private fun createResultReceiver(
            projectedService: IProjectedService,
            activity: Activity,
            requestCode: Int,
            serviceConnection: ProjectedServiceConnection,
        ): ResultReceiver {
            val activityWeakReference = WeakReference(activity)
            return object : ResultReceiver(Handler(Looper.getMainLooper())) {
                @SuppressLint("PrimitiveInCollection")
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    projectedService.finishProjectedPermissionRequest()
                    val activityReference = activityWeakReference.get()
                    if (activityReference != null) {
                        val permissions =
                            resultData?.getStringArray(
                                ProjectedPermissionsConstants.RESULT_DATA_PERMISSIONS
                            ) ?: emptyArray()
                        val grantResults =
                            resultData?.getIntArray(
                                ProjectedPermissionsConstants.RESULT_DATA_GRANT_RESULTS
                            ) ?: intArrayOf()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            activityReference.onRequestPermissionsResult(
                                requestCode,
                                permissions,
                                grantResults,
                                activityReference.deviceId,
                            )
                        } else {
                            activityReference.onRequestPermissionsResult(
                                requestCode,
                                permissions,
                                grantResults,
                            )
                        }
                    }
                    serviceConnection.disconnect()
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun startActivityOnHost(context: Context, intent: Intent) {
            context.startActivity(
                intent,
                ActivityOptions.makeBasic().setLaunchDisplayId(DEFAULT_DISPLAY).toBundle(),
            )
        }
    }
}
