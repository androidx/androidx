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

package androidx.xr.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.PackageManagerUtils.XR_PROJECTED_SYSTEM_FEATURE

/** Internal utils class for [android.content.pm.PackageManager]-related operations. */
internal object PackageManagerUtils {
    @VisibleForTesting
    internal const val XR_PROJECTED_SYSTEM_FEATURE = "com.google.android.feature.XR_PROJECTED"
    @VisibleForTesting internal const val ACTION_BIND: String = "androidx.xr.projected.ACTION_BIND"
    private const val PROJECTED_API_VERSION_PROPERTY: String =
        "androidx.xr.projected.projected_api_version"

    /** Returns true if the [XR_PROJECTED_SYSTEM_FEATURE] is present. False otherwise. */
    internal fun hasXrProjectedSystemFeature(context: Context): Boolean =
        context.packageManager.hasSystemFeature(XR_PROJECTED_SYSTEM_FEATURE)

    /**
     * Returns Projected platform API version if it's available.
     *
     * @throws IllegalStateException if the API version is unavailable.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    internal fun getProjectedPlatformApiVersion(context: Context): Int {
        val resolveInfo = findSystemServiceForIntent(context, Intent(ACTION_BIND))
        try {
            // TODO: b/492555594 - Add unit tests for property when Robolectric supports it
            val projectedApiVersion =
                context.packageManager.getProperty(
                    PROJECTED_API_VERSION_PROPERTY,
                    resolveInfo.serviceInfo.packageName,
                )
            return if (projectedApiVersion.isInteger) {
                projectedApiVersion.integer
            } else {
                throw IllegalStateException("Projected API version is not an integer")
            }
        } catch (_: PackageManager.NameNotFoundException) {
            throw IllegalStateException("Projected API version not found")
        }
    }

    internal fun hasXrProjectedSystemService(context: Context): Boolean =
        context.packageManager
            .queryIntentServices(Intent(ACTION_BIND), PackageManager.MATCH_SYSTEM_ONLY)
            .isNotEmpty()

    private fun findSystemServiceForIntent(context: Context, intent: Intent): ResolveInfo {
        val resolveInfoSystemApps: List<ResolveInfo> =
            context.packageManager.queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY)

        check(resolveInfoSystemApps.isNotEmpty()) {
            "System doesn't include a service for a provided intent."
        }
        check(resolveInfoSystemApps.size == 1) {
            "More than one system service found for a provided intent."
        }

        return resolveInfoSystemApps.first()
    }
}
