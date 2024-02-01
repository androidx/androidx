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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier

/**
 * Contains the configuration of the ad selection process.
 *
 * Instances of this class are created by SDKs to be provided as arguments to the
 * [AdSelectionManager#selectAds] and [AdSelectionManager#reportImpression] methods in
 * [AdSelectionManager].
 *
 * @param seller AdTechIdentifier of the seller, for example "www.example-ssp.com".
 * @param decisionLogicUri the URI used to retrieve the JS code containing the seller/SSP scoreAd
 *     function used during the ad selection and reporting processes.
 * @param customAudienceBuyers a list of custom audience buyers allowed by the SSP to participate
 *     in the ad selection process.
 * @param adSelectionSignals signals given to the participating buyers in the ad selection and
 *     reporting processes.
 * @param sellerSignals represents any information that the SSP used in the ad
 *     scoring process to tweak the results of the ad selection process (e.g. brand safety
 *     checks, excluded contextual ads).
 * @param perBuyerSignals any information that each buyer would provide during ad selection to
 *     participants (such as bid floor, ad selection type, etc.)
 * @param trustedScoringSignalsUri URI endpoint of sell-side trusted signal from which creative
 *     specific realtime information can be fetched from.
 */
@SuppressLint("ClassVerificationFailure")
class AdSelectionConfig public constructor(
    val seller: AdTechIdentifier,
    val decisionLogicUri: Uri,
    val customAudienceBuyers: List<AdTechIdentifier>,
    val adSelectionSignals: AdSelectionSignals,
    val sellerSignals: AdSelectionSignals,
    val perBuyerSignals: Map<AdTechIdentifier, AdSelectionSignals>,
    val trustedScoringSignalsUri: Uri
) {

    /** Checks whether two [AdSelectionConfig] objects contain the same information.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdSelectionConfig) return false
        return this.seller == other.seller &&
            this.decisionLogicUri == other.decisionLogicUri &&
            this.customAudienceBuyers == other.customAudienceBuyers &&
            this.adSelectionSignals == other.adSelectionSignals &&
            this.sellerSignals == other.sellerSignals &&
            this.perBuyerSignals == other.perBuyerSignals &&
            this.trustedScoringSignalsUri == other.trustedScoringSignalsUri
    }

    /** Returns the hash of the [AdSelectionConfig] object's data.  */
    override fun hashCode(): Int {
        var hash = seller.hashCode()
        hash = 31 * hash + decisionLogicUri.hashCode()
        hash = 31 * hash + customAudienceBuyers.hashCode()
        hash = 31 * hash + adSelectionSignals.hashCode()
        hash = 31 * hash + sellerSignals.hashCode()
        hash = 31 * hash + perBuyerSignals.hashCode()
        hash = 31 * hash + trustedScoringSignalsUri.hashCode()
        return hash
    }

    /** Overrides the toString method.  */
    override fun toString(): String {
        return "AdSelectionConfig: seller=$seller, decisionLogicUri='$decisionLogicUri', " +
            "customAudienceBuyers=$customAudienceBuyers, adSelectionSignals=$adSelectionSignals, " +
            "sellerSignals=$sellerSignals, perBuyerSignals=$perBuyerSignals, " +
            "trustedScoringSignalsUri=$trustedScoringSignalsUri"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.adselection.AdSelectionConfig {
        return android.adservices.adselection.AdSelectionConfig.Builder()
            .setAdSelectionSignals(adSelectionSignals.convertToAdServices())
            .setCustomAudienceBuyers(customAudienceBuyers.convertToAdServices())
            .setDecisionLogicUri(decisionLogicUri)
            .setSeller(seller.convertToAdServices())
            .setPerBuyerSignals(perBuyerSignals.convertToAdServices())
            .setSellerSignals(sellerSignals.convertToAdServices())
            .setTrustedScoringSignalsUri(trustedScoringSignalsUri)
            .build()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private fun List<AdTechIdentifier>.convertToAdServices():
        MutableList<android.adservices.common.AdTechIdentifier> {
            val ids = mutableListOf<android.adservices.common.AdTechIdentifier>()
            for (buyer in this) {
                ids.add(buyer.convertToAdServices())
            }
            return ids
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private fun Map<AdTechIdentifier, AdSelectionSignals>.convertToAdServices():
        MutableMap<android.adservices.common.AdTechIdentifier,
                android.adservices.common.AdSelectionSignals?> {
            val map = HashMap<android.adservices.common.AdTechIdentifier,
                android.adservices.common.AdSelectionSignals?>()
            for (key in this.keys) {
                val id = key.convertToAdServices()
                val value = this[key]?.convertToAdServices()
                map[id] = value
            }
            return map
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal companion object {
        val EMPTY = AdSelectionConfig(
            AdTechIdentifier(""),
            Uri.EMPTY,
            emptyList(),
            AdSelectionSignals(""),
            AdSelectionSignals(""),
            emptyMap(),
            Uri.EMPTY
        )
    }
}
