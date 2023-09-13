/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.measurement

import android.adservices.common.AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.InputEvent
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * This class provides APIs to manage ads attribution using Privacy Sandbox.
 */
abstract class MeasurementManager {
    /**
     * Delete previous registrations.
     *
     * @param deletionRequest The request for deleting data.
     */
    abstract suspend fun deleteRegistrations(deletionRequest: DeletionRequest)

    /**
     * Register an attribution source (click or view).
     *
     * @param attributionSource the platform issues a request to this URI in order to fetch metadata
     *     associated with the attribution source.
     * @param inputEvent either an [InputEvent] object (for a click event) or null (for a view
     *     event).
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    abstract suspend fun registerSource(attributionSource: Uri, inputEvent: InputEvent?)

    /**
     * Register a trigger (conversion).
     *
     * @param trigger the API issues a request to this URI to fetch metadata associated with the
     *     trigger.
     */
    // TODO(b/258551492): Improve docs.
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    abstract suspend fun registerTrigger(trigger: Uri)

    /**
     * Register an attribution source(click or view) from web context. This API will not process any
     * redirects, all registration URLs should be supplied with the request. At least one of
     * appDestination or webDestination parameters are required to be provided.
     *
     * @param request source registration request
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    abstract suspend fun registerWebSource(request: WebSourceRegistrationRequest)

    /**
     * Register an attribution trigger(click or view) from web context. This API will not process
     * any redirects, all registration URLs should be supplied with the request.
     *
     * @param request trigger registration request
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    abstract suspend fun registerWebTrigger(request: WebTriggerRegistrationRequest)

    /**
     * Register an attribution source(click or view) context. This API will not process any
     * redirects, all registration URLs should be supplied with the request.
     *
     * @param request source registration request
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    @ExperimentalFeatures.RegisterSourceOptIn
    abstract suspend fun registerSource(request: SourceRegistrationRequest)

    /**
     * Get Measurement API status.
     *
     * The call returns an integer value (see [MEASUREMENT_API_STATE_DISABLED] and
     * [MEASUREMENT_API_STATE_ENABLED] for possible values).
     */
    @RequiresPermission(ACCESS_ADSERVICES_ATTRIBUTION)
    abstract suspend fun getMeasurementApiStatus(): Int

    companion object {
        /**
         * This state indicates that Measurement APIs are unavailable. Invoking them will result
         * in an [UnsupportedOperationException].
         */
        public const val MEASUREMENT_API_STATE_DISABLED = 0
        /**
         * This state indicates that Measurement APIs are enabled.
         */
        public const val MEASUREMENT_API_STATE_ENABLED = 1

        /**
         *  Creates [MeasurementManager].
         *
         *  @return MeasurementManager object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): MeasurementManager? {
            Log.d("MeasurementManager",
                "AdServicesInfo.version=${AdServicesInfo.adServicesVersion()}")
            return if (AdServicesInfo.adServicesVersion() >= 5) {
                MeasurementManagerApi33Ext5Impl(context)
            } else if (AdServicesInfo.extServicesVersion() >= 9) {
                MeasurementManagerApi31Ext9Impl(context)
            } else {
                null
            }
        }
    }
}
