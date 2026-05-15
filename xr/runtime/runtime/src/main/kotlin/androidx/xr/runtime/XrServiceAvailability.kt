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
import android.os.Build
import androidx.annotation.RestrictTo

/** Provides information about the XR API availability. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object XrServiceAvailability {
    /** Includes all supported states for the XR service availability. */
    public class XrServiceAvailabilityStatus private constructor(private val code: Int) {
        public companion object {
            /** XR service availability is unknown. */
            @JvmField
            public val UNKNOWN: XrServiceAvailabilityStatus = XrServiceAvailabilityStatus(0)
            /** XR service is available, and it's in the most recent version. */
            @JvmField
            public val AVAILABLE: XrServiceAvailabilityStatus = XrServiceAvailabilityStatus(1)
            /** XR service is unsupported on the device. */
            @JvmField
            public val UNSUPPORTED: XrServiceAvailabilityStatus = XrServiceAvailabilityStatus(2)
            /**
             * XR service available on the device is older than the library API used by the client
             * application.
             */
            @JvmField
            public val OUTDATED: XrServiceAvailabilityStatus = XrServiceAvailabilityStatus(3)
        }
    }

    /**
     * Returns the [XrServiceAvailabilityStatus] of Projected XR features support.
     *
     * @param context any [Context] object.
     */
    @JvmStatic
    public fun checkProjectedServiceAvailability(context: Context): XrServiceAvailabilityStatus {
        if (isProjectedServiceUnsupported(context)) {
            return XrServiceAvailabilityStatus.UNSUPPORTED
        }
        return try {
            if (isProjectedServiceOutdated(context)) {
                XrServiceAvailabilityStatus.OUTDATED
            } else {
                XrServiceAvailabilityStatus.AVAILABLE
            }
        } catch (_: IllegalStateException) {
            XrServiceAvailabilityStatus.UNSUPPORTED
        }
    }

    private fun isProjectedServiceUnsupported(context: Context) =
        !PackageManagerUtils.hasXrProjectedSystemFeature(context)

    private fun isProjectedServiceOutdated(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            throw IllegalStateException("Service unsupported on builds older than Android U")
        }
        val serviceApiVersion = PackageManagerUtils.getProjectedPlatformApiVersion(context)
        val clientApiVersion = context.getString(R.string.xr_projected_min_api_version).toInt()

        return serviceApiVersion < clientApiVersion
    }
}
