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

package androidx.xr.scenecore.projected

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.IBinder
import androidx.xr.runtime.Log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal open class ProjectedSceneCoreServiceClient {
    /** The connected service interface, or null if not connected. */
    public var service: IProjectedSceneCoreService? = null
        private set

    private var mActiveConnection: ServiceConnection? = null
    private var mBoundContext: Context? = null

    /**
     * Binds to the Projected SceneCore service and suspends until the connection is established.
     *
     * @param context The context used to bind to the service.
     * @return The connected [IProjectedSceneCoreService].
     * @throws IllegalStateException If the service cannot be found or binding fails.
     */
    public open suspend fun bindService(context: Context): IProjectedSceneCoreService {
        // Return immediately if already connected
        service?.let {
            return it
        }

        return suspendCancellableCoroutine { continuation ->
            val serviceIntent =
                try {
                    getServiceIntent(context)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                    return@suspendCancellableCoroutine
                }

            val connection =
                object : ServiceConnection {
                    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                        Log.info { "Projected SceneCore Service Connected" }
                        val connectedService = IProjectedSceneCoreService.Stub.asInterface(binder)
                        service = connectedService

                        if (continuation.isActive) {
                            continuation.resume(connectedService)
                        }
                    }

                    override fun onServiceDisconnected(className: ComponentName) {
                        Log.info { "Projected SceneCore Service Disconnected" }
                        service = null
                        // We do not unbind here automatically; the system might attempt to
                        // reconnect. However, if the caller relies on a non-null service, they must
                        // handle this state.

                        // TODO: b/477998425 - Handle onServiceDisconnected for the client
                    }

                    override fun onBindingDied(name: ComponentName?) {
                        Log.warn { "Binding died for $name" }
                        unbindService()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Binding died for $name")
                            )
                        }
                    }

                    override fun onNullBinding(name: ComponentName?) {
                        Log.warn { "Null binding for $name" }
                        unbindService()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Service returned null binding")
                            )
                        }
                    }
                }

            // Ensure we clean up if the coroutine is canceled (e.g. Activity destroyed)
            continuation.invokeOnCancellation {
                if (service == null) {
                    // Only unbind if we haven't successfully connected yet,
                    // or if we want to enforce strict lifecycle scope.
                    context.unbindService(connection)
                    mActiveConnection = null
                    mBoundContext = null
                }
            }

            try {
                val didBind =
                    context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                if (didBind) {
                    Log.info { "bindService request accepted, waiting for connection..." }
                    mActiveConnection = connection
                    mBoundContext = context
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("bindService returned false")
                    )
                }
            } catch (e: SecurityException) {
                continuation.resumeWithException(e)
            }
        }
    }

    /** Unbinds from the service and releases resources. */
    public open fun unbindService() {
        val connection = mActiveConnection
        val context = mBoundContext

        if (connection != null && context != null) {
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // Service likely not registered or already unbound
                Log.warn { "Error unbinding service: ${e.message}" }
            }
        }

        mActiveConnection = null
        mBoundContext = null
        service = null
    }

    private fun getServiceIntent(context: Context): Intent {
        val intent = Intent(ACTION_SCENE_CORE_BIND)
        val projectedSystemServiceResolveInfo = findSystemService(context, intent)
        val serviceComponentName =
            ComponentName(
                projectedSystemServiceResolveInfo.serviceInfo.packageName,
                projectedSystemServiceResolveInfo.serviceInfo.name,
            )

        return Intent().apply {
            component = serviceComponentName
            action = ACTION_SCENE_CORE_BIND
        }
    }

    private fun findSystemService(context: Context, intent: Intent): ResolveInfo {
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentServices(intent, PackageManager.GET_RESOLVED_FILTER)

        val systemApps =
            resolveInfoList.filter {
                val appInfo =
                    context.packageManager.getApplicationInfo(
                        it.serviceInfo.packageName,
                        /* flags= */ 0,
                    )
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }
        check(systemApps.isNotEmpty()) {
            "System doesn't include a service supporting Projected XR devices."
        }
        check(systemApps.size == 1) { "More than one system service found for action: $intent." }
        return systemApps.first()
    }

    internal companion object {
        const val ACTION_SCENE_CORE_BIND = "androidx.xr.projected.ACTION_SCENE_CORE_BIND"
    }
}
