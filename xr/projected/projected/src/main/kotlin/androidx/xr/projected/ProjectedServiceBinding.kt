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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.RestrictTo

/**
 * Helper object to bind to a Projected service.
 *
 * This helper uses service discovery to find service meeting the following requirements:
 * 1. Service must be a system service,
 * 2. Service must include the "androidx.xr.projected.ACTION_BIND" action in its
 *    [android.content.IntentFilter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ProjectedServiceBinding {

    internal const val ACTION_BIND: String = "androidx.xr.projected.ACTION_BIND"
    private val QUERY_INTENT = Intent(ACTION_BIND)

    /**
     * Binds to a service using provided [ServiceConnection].
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
    @JvmStatic
    public fun bind(context: Context, serviceConnection: ServiceConnection): Boolean =
        context.bindService(getIntent(context), serviceConnection, Context.BIND_AUTO_CREATE)

    private fun getIntent(context: Context): Intent {
        val projectedSystemServiceResolveInfo = findProjectedSystemService(context)
        val foundService =
            ComponentName(
                projectedSystemServiceResolveInfo.serviceInfo.packageName,
                projectedSystemServiceResolveInfo.serviceInfo.name,
            )

        return Intent().apply {
            component = foundService
            action = ACTION_BIND
        }
    }

    private fun findProjectedSystemService(context: Context): ResolveInfo {
        val resolveInfoList: List<ResolveInfo> =
            context.packageManager.queryIntentServices(
                QUERY_INTENT,
                PackageManager.GET_RESOLVED_FILTER,
            )

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
            "More than one system service found for action: $ACTION_BIND."
        }

        return resolveInfoSystemApps.first()
    }
}
