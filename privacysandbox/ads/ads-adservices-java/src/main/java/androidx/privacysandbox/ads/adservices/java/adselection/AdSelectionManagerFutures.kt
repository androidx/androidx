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
