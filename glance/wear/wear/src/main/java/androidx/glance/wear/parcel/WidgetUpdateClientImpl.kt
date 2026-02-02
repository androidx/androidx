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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_HOME
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.IBinder
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.glance.wear.parcel.legacy.TileUpdateRequestData
import androidx.glance.wear.parcel.legacy.TileUpdateRequesterService

internal class WidgetUpdateClientImpl() : WidgetUpdateClient {
    private val lock = Any()

    @GuardedBy("lock") private val pendingUpdates = mutableSetOf<ComponentName>()
    @GuardedBy("lock") private var bindingInProgress = false

    override fun requestUpdate(context: Context, provider: ComponentName) {
        synchronized(lock) {
            pendingUpdates.add(provider)

            if (bindingInProgress) {
                // Something else kicked off the bind; let that carry on binding.
                return
            } else {
                bindingInProgress = true
            }
        }

        val bindIntent = buildUpdateBindIntent(context)
        if (bindIntent == null) {
            Log.e(TAG, "Could not build bind intent")
            synchronized(lock) { bindingInProgress = false }
            return
        }
        bindAndUpdate(context, bindIntent)
    }

    private fun bindAndUpdate(context: Context, intent: Intent) {
        context.bindService(
            intent,
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val pendingUpdatesCopy =
                        synchronized(lock) {
                            val copy = ArrayList(pendingUpdates)
                            pendingUpdates.clear()
                            bindingInProgress = false
                            copy
                        }

                    val updateRequesterService: TileUpdateRequesterService? =
                        TileUpdateRequesterService.Stub.asInterface(service)

                    for (index in 0 until pendingUpdatesCopy.size) {
                        updateRequesterService?.sendUpdateRequest(pendingUpdatesCopy[index])
                    }

                    context.unbindService(this)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            },
            Context.BIND_AUTO_CREATE,
        )
    }

    companion object {
        internal const val ACTION_BIND_UPDATE_REQUESTER =
            "androidx.wear.tiles.action.BIND_UPDATE_REQUESTER"
        internal const val CATEGORY_HOME_MAIN = "${CATEGORY_HOME}_MAIN"
        private const val DEFAULT_TARGET_SYSUI = "com.google.android.wearable.app"
        private const val SYSUI_SETTINGS_KEY = "clockwork_sysui_package"
        private const val TAG = "WidgetUpdateClient"

        private fun TileUpdateRequesterService.sendUpdateRequest(provider: ComponentName) {
            try {
                this.requestUpdate(provider, TileUpdateRequestData())
            } catch (ex: RemoteException) {
                Log.w(TAG, "while requesting widget update", ex)
            }
        }

        private fun buildUpdateBindIntent(context: Context): Intent? {
            val bindIntent =
                Intent(ACTION_BIND_UPDATE_REQUESTER).apply {
                    `package` = getSysUiPackageName(context)
                }

            // Find the concrete ComponentName of the service that implements what we need.
            val services =
                context.packageManager.queryIntentServices(
                    bindIntent,
                    PackageManager.GET_META_DATA or PackageManager.GET_RESOLVED_FILTER,
                )

            if (services.isEmpty()) {
                Log.w(TAG, "Couldn't find any services filtering on $ACTION_BIND_UPDATE_REQUESTER")
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
            val queryIntent = Intent(ACTION_MAIN).apply { addCategory(CATEGORY_HOME_MAIN) }
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
