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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.IBinder
import android.os.IInterface
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn

/**
 * A binder wrapper that handles binding to a SysUi service, sending update requests, and unbinding.
 *
 * @param ServiceType the binder interface type
 * @param UpdateData the data type used in an update request
 * @param action the intent action used to bind to the service
 * @param asInterface a function that converts a binder interface to the service interface
 * @param dispatcher the dispatcher used for background operations
 * @param sendRequest a function that sends the update request using the service interface
 */
internal class WidgetUpdateBinder<ServiceType : IInterface, UpdateData>(
    private val context: Context,
    private val action: String,
    private val asInterface: (IBinder?) -> ServiceType?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sendRequest: suspend (serviceType: ServiceType, updateData: UpdateData) -> Unit,
) {
    private val sharedServiceFlow: SharedFlow<ServiceType?> =
        callbackFlow<ServiceType?> {
                val connection =
                    object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            val localService = asInterface(service)
                            if (localService != null) {
                                trySendBlocking(localService)
                            } else {
                                close(IllegalStateException("Binder Interface is null"))
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            close(RuntimeException("Service disconnected"))
                        }

                        override fun onBindingDied(name: ComponentName?) {
                            close(RuntimeException("Binding died"))
                        }
                    }

                val intent = buildUpdateBindIntent(context, action)
                if (intent == null) {
                    close(IllegalStateException("Could not build bind intent for $action"))
                    return@callbackFlow
                }
                val bound =
                    context.applicationContext.bindService(
                        intent,
                        connection,
                        Context.BIND_AUTO_CREATE,
                    )
                if (!bound) {
                    close(RuntimeException("Binding failed for ${intent.`package`}"))
                    return@callbackFlow
                }

                awaitClose {
                    try {
                        context.applicationContext.unbindService(connection)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Service is already unbound.", e)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error while unbinding", e)
                    }
                }
            }
            .retryWhen { ex, _ ->
                when (ex) {
                    is IllegalStateException -> false

                    // Exception thrown in disconnection above. Should retry.
                    is RuntimeException -> {
                        emit(null)
                        delay(RETRY_DELAY_MS)
                        true
                    }
                    else -> false
                }
            }
            .shareIn(
                scope = CoroutineScope(SupervisorJob() + dispatcher),
                started =
                    SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = IDLE_TIMEOUT_MS,
                        replayExpirationMillis = 0L,
                    ),
                replay = 1,
            )

    suspend fun requestUpdate(updateData: UpdateData) {
        sharedServiceFlow
            .filterNotNull()
            .map { service -> sendRequest.invoke(service, updateData) }
            .first()
    }

    internal companion object {
        private const val TAG = "WidgetUpdateBinder"
        const val DEFAULT_TARGET_SYSUI = "com.google.android.wearable.app"
        const val SYSUI_SETTINGS_KEY = "clockwork_sysui_package"
        const val CATEGORY_HOME_MAIN = "${Intent.CATEGORY_HOME}_MAIN"
        internal const val RETRY_DELAY_MS = 1000L
        internal const val IDLE_TIMEOUT_MS = 1000L

        private fun buildUpdateBindIntent(context: Context, action: String): Intent? {
            val bindIntent = Intent(action).apply { `package` = getSysUiPackageName(context) }

            // Find the concrete ComponentName of the service that implements what we need.
            val services =
                context.packageManager.queryIntentServices(
                    bindIntent,
                    PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER,
                )

            if (services.isEmpty()) {
                Log.w(TAG, "Couldn't find any services filtering on $action")
                return null
            }

            services.first().serviceInfo.let { bindIntent.setClassName(it.packageName, it.name) }

            return bindIntent
        }

        private fun getSysUiPackageName(context: Context): String? {
            if (
                VERSION.SDK_INT == UPSIDE_DOWN_CAKE &&
                    (context.applicationInfo.targetSdkVersion > UPSIDE_DOWN_CAKE)
            ) {
                return getSysUiPackageNameOnU(context)
            }

            val sysUiPackageName =
                Settings.Global.getString(context.contentResolver, SYSUI_SETTINGS_KEY)
            return if (sysUiPackageName.isNullOrEmpty()) {
                DEFAULT_TARGET_SYSUI
            } else {
                sysUiPackageName
            }
        }

        private fun getSysUiPackageNameOnU(context: Context): String? {
            val queryIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(CATEGORY_HOME_MAIN) }
            val homeActivity =
                context.packageManager.resolveActivity(
                    queryIntent,
                    PackageManager.MATCH_DEFAULT_ONLY,
                )
            if (homeActivity == null) {
                Log.e(TAG, "Couldn't find SysUi packageName")
                return DEFAULT_TARGET_SYSUI
            }
            return homeActivity.activityInfo.packageName
        }
    }
}
