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

package androidx.privacysandbox.ads.adservices.java.measurement

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.InputEvent
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.os.BuildCompat
import androidx.privacysandbox.ads.adservices.java.internal.asListenableFuture
import androidx.privacysandbox.ads.adservices.measurement.DeletionRequest
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.measurement.MeasurementManager.MeasurementApiState
import androidx.privacysandbox.ads.adservices.measurement.WebSourceRegistrationRequest
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerRegistrationRequest
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * This provides APIs for App and Ad-Sdks to access Privacy Sandbox Measurement APIs in a privacy
 * preserving way. This class can be used by Java clients.
 */
abstract class MeasurementManagerFutures internal constructor() {
    /**
     * Delete previous registrations.
     *
     * @param deletionRequest The request for deleting data.
     * @return ListenableFuture. If the deletion is successful, result is null.
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    abstract fun deleteRegistrationsAsync(
        deletionRequest: DeletionRequest
    ): ListenableFuture<Unit>

    /**
     * Register an attribution source (click or view).
     *
     * @param attributionSource the platform issues a request to this URI in order to fetch metadata
     *     associated with the attribution source.
     * @param inputEvent either an {@link InputEvent} object (for a click event) or null (for a view
     *     event).
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
    abstract fun registerSourceAsync(
        attributionSource: Uri,
        inputEvent: InputEvent?
    ): ListenableFuture<Unit>

    /**
     * Register a trigger (conversion).
     *
     * @param trigger the API issues a request to this URI to fetch metadata associated with the
     *     trigger.
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
    abstract fun registerTriggerAsync(trigger: Uri): ListenableFuture<Unit>

    /**
     * Register an attribution source(click or view) from web context. This API will not process any
     * redirects, all registration URLs should be supplied with the request. At least one of
     * appDestination or webDestination parameters are required to be provided.
     *
     * @param request source registration request
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
    abstract fun registerWebSourceAsync(
        request: WebSourceRegistrationRequest
    ): ListenableFuture<Unit>

    /**
     * Register an attribution trigger(click or view) from web context. This API will not process
     * any redirects, all registration URLs should be supplied with the request.
     * OutcomeReceiver#onError}.
     *
     * @param request trigger registration request
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
    abstract fun registerWebTriggerAsync(
        request: WebTriggerRegistrationRequest,
    ): ListenableFuture<Unit>

    /**
     * Get Measurement API status.
     *
     * <p>The callback's {@code Integer} value is one of {@code MeasurementApiState}.
     */
    @DoNotInline
    @SuppressWarnings("MissingNullability")
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
    abstract fun getMeasurementApiStatusAsync(): ListenableFuture<MeasurementApiState>

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class Api33Ext4JavaImpl(
        private val mMeasurementManager: MeasurementManager
    ) : MeasurementManagerFutures() {
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun deleteRegistrationsAsync(
            deletionRequest: DeletionRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.deleteRegistrations(deletionRequest)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun registerSourceAsync(
            attributionSource: Uri,
            inputEvent: InputEvent?
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.registerSource(attributionSource, inputEvent)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun registerTriggerAsync(trigger: Uri): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.registerTrigger(trigger)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun registerWebSourceAsync(
            request: WebSourceRegistrationRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.registerWebSource(request)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun registerWebTriggerAsync(
            request: WebTriggerRegistrationRequest,
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.registerWebTrigger(request)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_ATTRIBUTION)
        override fun getMeasurementApiStatusAsync(): ListenableFuture<MeasurementApiState> {
            return CoroutineScope(Dispatchers.Main).async {
                mMeasurementManager.getMeasurementApiStatus()
            }.asListenableFuture()
        }
    }

    companion object {
        /**
         *  Creates [MeasurementManagerFutures].
         *
         *  @return MeasurementManagerFutures object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun from(context: Context): MeasurementManagerFutures? {
            // TODO: Add check SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 4
            return obtain(context)?.let { Api33Ext4JavaImpl(it) }
        }
    }
}