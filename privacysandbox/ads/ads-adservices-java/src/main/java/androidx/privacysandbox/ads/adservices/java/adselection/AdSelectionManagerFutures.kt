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

package androidx.privacysandbox.ads.adservices.java.adselection

import android.adservices.common.AdServicesPermissions
import android.content.Context
import android.os.LimitExceededException
import android.os.TransactionTooLargeException
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionFromOutcomesConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataOutcome
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataRequest
import androidx.privacysandbox.ads.adservices.adselection.PersistAdSelectionResultRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.java.internal.asListenableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * This class provides APIs to select ads and report impressions.
 * This class can be used by Java clients.
 */
@OptIn(ExperimentalFeatures.Ext8OptIn::class)
abstract class AdSelectionManagerFutures internal constructor() {

    /**
     * Runs the ad selection process on device to select a remarketing ad for the caller
     * application.
     *
     * @param adSelectionConfig the config The input {@code adSelectionConfig} is provided by the
     * Ads SDK and the [AdSelectionConfig] object is transferred via a Binder call. For this
     * reason, the total size of these objects is bound to the Android IPC limitations. Failures to
     * transfer the [AdSelectionConfig] will throws an [TransactionTooLargeException].
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome]
     * for a successful run, or an [Exception] includes the type of the exception thrown and
     * the corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun selectAdsAsync(
        adSelectionConfig: AdSelectionConfig
    ): ListenableFuture<AdSelectionOutcome>

    /**
     * Selects an ad from the results of previously ran ad selections.
     *
     * @param adSelectionFromOutcomesConfig is provided by the Ads SDK and the
     * [AdSelectionFromOutcomesConfig] object is transferred via a Binder call. For this reason, the
     * total size of these objects is bound to the Android IPC limitations. Failures to transfer the
     * [AdSelectionFromOutcomesConfig] will throw an [TransactionTooLargeException].
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome]
     * for a successful run, or an [Exception] includes the type of the exception thrown and
     * the corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun selectAdsAsync(
        adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig
    ): ListenableFuture<AdSelectionOutcome>

    /**
     * Report the given impression. The [ReportImpressionRequest] is provided by the Ads SDK.
     * The receiver either returns a {@code void} for a successful run, or an [Exception]
     * indicates the error.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to report the impression.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support [ReportImpressionRequest] with null
     * {@code AdSelectionConfig}
     *
     * @param reportImpressionRequest the request for reporting impression.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun reportImpressionAsync(
        reportImpressionRequest: ReportImpressionRequest
    ): ListenableFuture<Unit>

    /**
     * Notifies the service that there is a new ad event to report for the ad selected by the
     * ad-selection run identified by {@code adSelectionId}. An ad event is any occurrence that
     * happens to an ad associated with the given {@code adSelectionId}. There is no guarantee about
     * when the ad event will be reported. The event reporting could be delayed and reports could be
     * batched.
     *
     * Using [ReportEventRequest#getKey()], the service will fetch the {@code reportingUri}
     * that was registered in {@code registerAdBeacon}. See documentation of [reportImpressionAsync]
     * for more details regarding {@code registerAdBeacon}. Then, the service will attach
     * [ReportEventRequest#getData()] to the request body of a POST request and send the request.
     * The body of the POST request will have the {@code content-type} of {@code text/plain}, and
     * the data will be transmitted in {@code charset=UTF-8}.
     *
     * The output is passed by the receiver, which either returns an empty [Object] for a
     * successful run, or an [Exception] includes the type of the exception thrown and the
     * corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to report the ad event.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     *
     * Events will be reported at most once as a best-effort attempt.
     *
     * @param reportEventRequest the request for reporting event.
     */
    @ExperimentalFeatures.Ext8OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun reportEventAsync(
        reportEventRequest: ReportEventRequest
    ): ListenableFuture<Unit>

    /**
     * Updates the counter histograms for an ad which was previously selected by a call to
     * [selectAdsAsync].
     *
     * The counter histograms are used in ad selection to inform frequency cap filtering on
     * candidate ads, where ads whose frequency caps are met or exceeded are removed from the
     * bidding process during ad selection.
     *
     * Counter histograms can only be updated for ads specified by the given {@code
     * adSelectionId} returned by a recent call to Protected Audience API ad selection from the same
     * caller app.
     *
     * A [SecurityException] is returned if:
     *
     * <ol>
     *   <li>the app has not declared the correct permissions in its manifest, or
     *   <li>the app or entity identified by the {@code callerAdTechIdentifier} are not authorized
     *       to use the API.
     * </ol>
     *
     * An [IllegalStateException] is returned if the call does not come from an app with a
     * foreground activity.
     *
     * A [LimitExceededException] is returned if the call exceeds the calling app's API throttle.
     *
     * An [UnsupportedOperationException] is returned if the Android API level and AdServices module
     * versions don't support this API.
     *
     * In all other failure cases, it will return an empty [Object]. Note that to protect user
     * privacy, internal errors will not be sent back via an exception.
     *
     * @param updateAdCounterHistogramRequest the request for updating the ad counter histogram.
     */
    @ExperimentalFeatures.Ext8OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun
        updateAdCounterHistogramAsync(
        updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
    ): ListenableFuture<Unit>

    /**
     * Collects custom audience data from device. Returns a compressed and encrypted blob to send to
     * auction servers for ad selection.
     *
     * Custom audience ads must have a {@code ad_render_id} to be eligible for to be collected.
     *
     * See [AdSelectionManager#persistAdSelectionResult] for how to process the results of
     * the ad selection run on server-side with the blob generated by this API.
     *
     * The output is passed by the receiver, which either returns an [GetAdSelectionDataOutcome]
     * for a successful run, or an [Exception] includes the type of
     * the exception thrown and the corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     *
     * @param getAdSelectionDataRequest the request for get ad selection data.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun getAdSelectionDataAsync(
        getAdSelectionDataRequest: GetAdSelectionDataRequest
    ): ListenableFuture<GetAdSelectionDataOutcome>

    /**
     * Persists the ad selection results from the server-side.
     *
     * See [AdSelectionManager#getAdSelectionData] for how to generate an encrypted blob to
     * run an ad selection on the server side.
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome]
     * for a successful run, or an [Exception] includes the type of the exception thrown and
     * the corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument
     * the API received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered
     * during bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package
     * exceeds the allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized
     * or permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     *
     * @param persistAdSelectionResultRequest the request for persist ad selection result.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun persistAdSelectionResultAsync(
        persistAdSelectionResultRequest: PersistAdSelectionResultRequest
    ): ListenableFuture<AdSelectionOutcome>

    private class Api33Ext4JavaImpl(
        private val mAdSelectionManager: AdSelectionManager?
    ) : AdSelectionManagerFutures() {
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun selectAdsAsync(
            adSelectionConfig: AdSelectionConfig
        ): ListenableFuture<AdSelectionOutcome> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.selectAds(adSelectionConfig)
            }.asListenableFuture()
        }

        @OptIn(ExperimentalFeatures.Ext10OptIn::class)
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun selectAdsAsync(
            adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig
        ): ListenableFuture<AdSelectionOutcome> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.selectAds(adSelectionFromOutcomesConfig)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun reportImpressionAsync(
            reportImpressionRequest: ReportImpressionRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.reportImpression(reportImpressionRequest)
            }.asListenableFuture()
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun updateAdCounterHistogramAsync(
            updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.updateAdCounterHistogram(updateAdCounterHistogramRequest)
            }.asListenableFuture()
        }

        @OptIn(ExperimentalFeatures.Ext8OptIn::class)
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun reportEventAsync(
            reportEventRequest: ReportEventRequest
        ): ListenableFuture<Unit> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.reportEvent(reportEventRequest)
            }.asListenableFuture()
        }

        @OptIn(ExperimentalFeatures.Ext10OptIn::class)
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun getAdSelectionDataAsync(
            getAdSelectionDataRequest: GetAdSelectionDataRequest
        ): ListenableFuture<GetAdSelectionDataOutcome> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.getAdSelectionData(getAdSelectionDataRequest)
            }.asListenableFuture()
        }

        @OptIn(ExperimentalFeatures.Ext10OptIn::class)
        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override fun persistAdSelectionResultAsync(
            persistAdSelectionResultRequest: PersistAdSelectionResultRequest
        ): ListenableFuture<AdSelectionOutcome> {
            return CoroutineScope(Dispatchers.Default).async {
                mAdSelectionManager!!.persistAdSelectionResult(persistAdSelectionResultRequest)
            }.asListenableFuture()
        }
    }

    companion object {
        /**
         *  Creates [AdSelectionManagerFutures].
         *
         *  @return AdSelectionManagerFutures object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        fun from(context: Context): AdSelectionManagerFutures? {
            return obtain(context)?.let { Api33Ext4JavaImpl(it) }
        }
    }
}
