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

package androidx.xr.projected.binding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.RestrictTo

/**
 * Helper object to bind to a Projected service.
 *
 * This helper uses service discovery to find service meeting the following requirements:
 * 1. Service must be a system service,
 * 2. Service must include a provided action in its [android.content.IntentFilter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ProjectedServiceBinding {

    /**
     * Binds to a service using provided [ServiceConnection].
     *
     * @param context can be either a host [Context] or the Projected device [Context].
     * @param intentAction The action of the intent to match for the service.
     * @param serviceConnection The [ServiceConnection] to be used for binding.
     * @return true if the system is in the process of bringing up a service that your client has
     *   permission to bind to; false if the system couldn't find the service or if your client
     *   doesn't have permission to bind to it. Regardless of the return value, you should later
     *   call unbindService to release the connection.
     * @throws IllegalStateException if the service can't be found, meaning the system doesn't
     *   include a service supporting Projected XR devices.
     * @throws SecurityException if the caller does not have permission to access the service or the
     *   service cannot be found. Call unbindService(ServiceConnection) to release the connection
     *   when this exception is thrown.
     */
    internal fun bind(
        context: Context,
        intentAction: String,
        serviceConnection: ServiceConnection,
    ): Boolean =
        context.bindService(
            getIntent(context, intentAction),
            serviceConnection,
            Context.BIND_AUTO_CREATE,
        )

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
        val resolveInfoSystemApps: List<ResolveInfo> =
            context.packageManager.queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY)

        check(resolveInfoSystemApps.isNotEmpty()) {
            "System doesn't include a service supporting Projected XR devices."
        }
        check(resolveInfoSystemApps.size == 1) {
            "More than one system service found for action: $intent."
        }

        return resolveInfoSystemApps.first()
    }
}
