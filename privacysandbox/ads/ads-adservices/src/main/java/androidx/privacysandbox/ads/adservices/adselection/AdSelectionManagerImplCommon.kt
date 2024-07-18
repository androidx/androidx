/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import kotlinx.coroutines.suspendCancellableCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi", "ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
open class AdSelectionManagerImplCommon(
    protected val mAdSelectionManager: android.adservices.adselection.AdSelectionManager
    ) : AdSelectionManager() {

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
        val ids = mutableListOf<android.adservices.common.AdTechIdentifier>()
        for (buyer in buyers) {
            ids.add(android.adservices.common.AdTechIdentifier.fromString(buyer.identifier))
        }
        return ids
    }

    private fun convertPerBuyerSignals(
        request: Map<AdTechIdentifier, AdSelectionSignals>
    ): Map<android.adservices.common.AdTechIdentifier,
        android.adservices.common.AdSelectionSignals?> {
        val map = HashMap<android.adservices.common.AdTechIdentifier,
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
