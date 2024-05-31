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

package androidx.privacysandbox.ads.adservices.adselection

import android.adservices.common.AdServicesPermissions
import android.annotation.SuppressLint
import android.content.Context
import android.os.LimitExceededException
import android.os.TransactionTooLargeException
import androidx.annotation.RequiresPermission
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import androidx.privacysandbox.ads.adservices.internal.BackCompatManager
import java.util.concurrent.TimeoutException

/**
 * AdSelection Manager provides APIs for app and ad-SDKs to run ad selection processes as well as
 * report impressions.
 */
abstract class AdSelectionManager internal constructor() {
    /**
     * Runs the ad selection process on device to select a remarketing ad for the caller
     * application.
     *
     * @param adSelectionConfig the config The input {@code adSelectionConfig} is provided by the
     *   Ads SDK and the [AdSelectionConfig] object is transferred via a Binder call. For this
     *   reason, the total size of these objects is bound to the Android IPC limitations. Failures
     *   to transfer the [AdSelectionConfig] will throws an [TransactionTooLargeException].
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome] for a
     * successful run, or an [Exception] includes the type of the exception thrown and the
     * corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered during
     * bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun selectAds(adSelectionConfig: AdSelectionConfig): AdSelectionOutcome

    /**
     * Selects an ad from the results of previously ran ad selections.
     *
     * @param adSelectionFromOutcomesConfig is provided by the Ads SDK and the
     *   [AdSelectionFromOutcomesConfig] object is transferred via a Binder call. For this reason,
     *   the total size of these objects is bound to the Android IPC limitations. Failures to
     *   transfer the [AdSelectionFromOutcomesConfig] will throw an [TransactionTooLargeException].
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome] for a
     * successful run, or an [Exception] includes the type of the exception thrown and the
     * corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered during
     * bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized or
     * permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun selectAds(
        adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig
    ): AdSelectionOutcome

    /**
     * Report the given impression. The [ReportImpressionRequest] is provided by the Ads SDK. The
     * receiver either returns a {@code void} for a successful run, or an [Exception] indicating the
     * error.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to report the impression.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized or
     * permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support [ReportImpressionRequest] with null {@code
     * AdSelectionConfig}
     *
     * @param reportImpressionRequest the request for reporting impression.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun reportImpression(reportImpressionRequest: ReportImpressionRequest)

    /**
     * Notifies the service that there is a new ad event to report for the ad selected by the
     * ad-selection run identified by {@code adSelectionId}. An ad event is any occurrence that
     * happens to an ad associated with the given {@code adSelectionId}. There is no guarantee about
     * when the ad event will be reported. The event reporting could be delayed and reports could be
     * batched.
     *
     * Using [ReportEventRequest#getKey()], the service will fetch the {@code reportingUri} that was
     * registered in {@code registerAdBeacon}. See documentation of [reportImpression] for more
     * details regarding {@code registerAdBeacon}. Then, the service will attach
     * [ReportEventRequest#getData()] to the request body of a POST request and send the request.
     * The body of the POST request will have the {@code content-type} of {@code text/plain}, and
     * the data will be transmitted in {@code charset=UTF-8}.
     *
     * The output is passed by the receiver, which either returns an empty [Object] for a successful
     * run, or an [Exception] includes the type of the exception thrown and the corresponding error
     * message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to report the ad event.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized or
     * permission is not requested.
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
    abstract suspend fun reportEvent(reportEventRequest: ReportEventRequest)

    /**
     * Updates the counter histograms for an ad which was previously selected by a call to
     * [selectAds].
     *
     * The counter histograms are used in ad selection to inform frequency cap filtering on
     * candidate ads, where ads whose frequency caps are met or exceeded are removed from the
     * bidding process during ad selection.
     *
     * Counter histograms can only be updated for ads specified by the given {@code adSelectionId}
     * returned by a recent call to Protected Audience API ad selection from the same caller app.
     *
     * A [SecurityException] is returned if:
     * <ol>
     * <li>the app has not declared the correct permissions in its manifest, or
     * <li>the app or entity identified by the {@code callerAdTechIdentifier} are not authorized to
     *   use the API.
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
    abstract suspend fun updateAdCounterHistogram(
        updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
    )

    /**
     * Collects custom audience data from device. Returns a compressed and encrypted blob to send to
     * auction servers for ad selection.
     *
     * Custom audience ads must have a {@code ad_render_id} to be eligible for to be collected.
     *
     * See [AdSelectionManager#persistAdSelectionResult] for how to process the results of the ad
     * selection run on server-side with the blob generated by this API.
     *
     * The output is passed by the receiver, which either returns an [GetAdSelectionDataOutcome] for
     * a successful run, or an [Exception] includes the type of the exception thrown and the
     * corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered during
     * bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized or
     * permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     *
     * @param getAdSelectionDataRequest the request for get ad selection data.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun getAdSelectionData(
        getAdSelectionDataRequest: GetAdSelectionDataRequest
    ): GetAdSelectionDataOutcome

    /**
     * Persists the ad selection results from the server-side.
     *
     * See [AdSelectionManager#getAdSelectionData] for how to generate an encrypted blob to run an
     * ad selection on the server side.
     *
     * The output is passed by the receiver, which either returns an [AdSelectionOutcome] for a
     * successful run, or an [Exception] includes the type of the exception thrown and the
     * corresponding error message.
     *
     * If the [IllegalArgumentException] is thrown, it is caused by invalid input argument the API
     * received to run the ad selection.
     *
     * If the [IllegalStateException] is thrown with error message "Failure of AdSelection
     * services.", it is caused by an internal failure of the ad selection service.
     *
     * If the [TimeoutException] is thrown, it is caused when a timeout is encountered during
     * bidding, scoring, or overall selection process to find winning Ad.
     *
     * If the [LimitExceededException] is thrown, it is caused when the calling package exceeds the
     * allowed rate limits and is throttled.
     *
     * If the [SecurityException] is thrown, it is caused when the caller is not authorized or
     * permission is not requested.
     *
     * If the [UnsupportedOperationException] is thrown, it is caused when the Android API level and
     * AdServices module versions don't support this API.
     *
     * @param persistAdSelectionResultRequest the request for persist ad selection result.
     */
    @ExperimentalFeatures.Ext10OptIn
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun persistAdSelectionResult(
        persistAdSelectionResultRequest: PersistAdSelectionResultRequest
    ): AdSelectionOutcome

    companion object {
        /**
         * Creates [AdSelectionManager].
         *
         * @return AdSelectionManager object. If the device is running an incompatible build, the
         *   value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): AdSelectionManager? {
            return if (AdServicesInfo.adServicesVersion() >= 4) {
                AdSelectionManagerApi33Ext4Impl(context)
            } else if (AdServicesInfo.extServicesVersionS() >= 9) {
                BackCompatManager.getManager(context, "AdSelectionManager") {
                    AdSelectionManagerApi31Ext9Impl(context)
                }
            } else {
                null
            }
        }
    }
}
