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
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * AdSelection Manager provides APIs for app and ad-SDKs to run ad selection processes as well
 * as report impressions.
 */
abstract class AdSelectionManager internal constructor() {
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
    abstract suspend fun selectAds(adSelectionConfig: AdSelectionConfig): AdSelectionOutcome

    /**
     * Report the given impression. The [ReportImpressionRequest] is provided by the Ads SDK.
     * The receiver either returns a {@code void} for a successful run, or an [Exception]
     * indicates the error.
     *
     * @param reportImpressionRequest the request for reporting impression.
     */
    @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
    abstract suspend fun reportImpression(reportImpressionRequest: ReportImpressionRequest)

    @SuppressLint("NewApi", "ClassVerificationFailure")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    private class Api33Ext4Impl(
        private val mAdSelectionManager: android.adservices.adselection.AdSelectionManager
    ) : AdSelectionManager() {
        constructor(context: Context) : this(
            context.getSystemService<android.adservices.adselection.AdSelectionManager>(
                android.adservices.adselection.AdSelectionManager::class.java
            )
        )

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override suspend fun selectAds(adSelectionConfig: AdSelectionConfig): AdSelectionOutcome {
            return convertResponse(selectAdsInternal(convertAdSelectionConfig(adSelectionConfig)))
        }

        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        private suspend fun selectAdsInternal(
            adSelectionConfig: android.adservices.adselection.AdSelectionConfig
        ): android.adservices.adselection.AdSelectionOutcome = suspendCancellableCoroutine { cont
            ->
            mAdSelectionManager.selectAds(
                adSelectionConfig,
                Runnable::run,
                cont.asOutcomeReceiver()
            )
        }

        private fun convertAdSelectionConfig(
            request: AdSelectionConfig
        ): android.adservices.adselection.AdSelectionConfig {
            return android.adservices.adselection.AdSelectionConfig.Builder()
                .setAdSelectionSignals(convertAdSelectionSignals(request.adSelectionSignals))
                .setCustomAudienceBuyers(convertBuyers(request.customAudienceBuyers))
                .setDecisionLogicUri(request.decisionLogicUri)
                .setSeller(android.adservices.common.AdTechIdentifier.fromString(
                    request.seller.identifier))
                .setPerBuyerSignals(convertPerBuyerSignals(request.perBuyerSignals))
                .setSellerSignals(convertAdSelectionSignals(request.sellerSignals))
                .setTrustedScoringSignalsUri(request.trustedScoringSignalsUri)
                .build()
        }

        private fun convertAdSelectionSignals(
            request: AdSelectionSignals
        ): android.adservices.common.AdSelectionSignals {
            return android.adservices.common.AdSelectionSignals.fromString(request.signals)
        }

        private fun convertBuyers(
            buyers: List<AdTechIdentifier>
        ): MutableList<android.adservices.common.AdTechIdentifier> {
            var ids = mutableListOf<android.adservices.common.AdTechIdentifier>()
            for (buyer in buyers) {
                ids.add(android.adservices.common.AdTechIdentifier.fromString(buyer.identifier))
            }
            return ids
        }

        private fun convertPerBuyerSignals(
            request: Map<AdTechIdentifier, AdSelectionSignals>
        ): Map<android.adservices.common.AdTechIdentifier,
            android.adservices.common.AdSelectionSignals?> {
            var map = HashMap<android.adservices.common.AdTechIdentifier,
                android.adservices.common.AdSelectionSignals?>()
            for (key in request.keys) {
                val id = android.adservices.common.AdTechIdentifier.fromString(key.identifier)
                val value = if (request[key] != null) convertAdSelectionSignals(request[key]!!)
                    else null
                map[id] = value
            }
            return map
        }

        private fun convertResponse(
            response: android.adservices.adselection.AdSelectionOutcome
        ): AdSelectionOutcome {
            return AdSelectionOutcome(response.adSelectionId, response.renderUri)
        }

        @DoNotInline
        @RequiresPermission(AdServicesPermissions.ACCESS_ADSERVICES_CUSTOM_AUDIENCE)
        override suspend fun reportImpression(reportImpressionRequest: ReportImpressionRequest) {
            suspendCancellableCoroutine<Any> { continuation ->
                mAdSelectionManager.reportImpression(
                    convertReportImpressionRequest(reportImpressionRequest),
                    Runnable::run,
                    continuation.asOutcomeReceiver()
                )
            }
        }

        private fun convertReportImpressionRequest(
            request: ReportImpressionRequest
        ): android.adservices.adselection.ReportImpressionRequest {
            return android.adservices.adselection.ReportImpressionRequest(
                request.adSelectionId,
                convertAdSelectionConfig(request.adSelectionConfig)
            )
        }
    }

    companion object {
        /**
         *  Creates [AdSelectionManager].
         *
         *  @return AdSelectionManager object. If the device is running an incompatible
         *  build, the value returned is null.
         */
        @JvmStatic
        @SuppressLint("NewApi", "ClassVerificationFailure")
        fun obtain(context: Context): AdSelectionManager? {
            return if (AdServicesInfo.version() >= 4) {
                Api33Ext4Impl(context)
            } else {
                null
            }
        }
    }
}
