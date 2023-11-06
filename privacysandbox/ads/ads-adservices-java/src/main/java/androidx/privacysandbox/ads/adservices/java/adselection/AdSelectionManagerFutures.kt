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
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager.Companion.obtain
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
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
     * Report the given impression. The [ReportImpressionRequest] is provided by the Ads SDK.
     * The receiver either returns a {@code void} for a successful run, or an [Exception]
     * indicates the error.
     *
     * @param reportImpressionRequest the request for reporting impression.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract fun reportImpressionAsync(
        reportImpressionRequest: ReportImpressionRequest
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
