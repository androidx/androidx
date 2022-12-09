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

package androidx.privacysandbox.ads.adservices.customaudience

import android.net.Uri
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import java.time.Instant

/**
 * Represents the information necessary for a custom audience to participate in ad selection.
 *
 * A custom audience is an abstract grouping of users with similar demonstrated interests. This
 * class is a collection of some data stored on a device that is necessary to serve advertisements
 * targeting a single custom audience.
 *
 * @param buyer A buyer is identified by a domain in the form "buyerexample.com".
 * @param name The custom audience's name is an arbitrary string provided by the owner and buyer on
 * creation of the [CustomAudience] object.
 * @param dailyUpdateUri a URI that points to a buyer-operated server that hosts updated bidding
 * data and ads metadata to be used in the on-device ad selection process. The URI must use HTTPS.
 * @param biddingLogicUri the target URI used to fetch bidding logic when a custom audience
 * participates in the ad selection process. The URI must use HTTPS.
 * @param ads the list of [AdData] objects is a full and complete list of the ads that will be
 * served by this [CustomAudience] during the ad selection process.
 * @param activationTime optional activation time may be set in the future, in order to serve a
 * delayed activation. If the field is not set, the object will be activated at the time of joining.
 * @param expirationTime optional expiration time. Once it has passed, a custom audience is no
 * longer eligible for daily ad/bidding data updates or participation in the ad selection process.
 * The custom audience will then be deleted from memory by the next daily update.
 * @param userBiddingSignals optional User bidding signals, provided by buyers to be consumed by
 * buyer-provided JavaScript during ad selection in an isolated execution environment.
 * @param trustedBiddingSignals optional trusted bidding data, consists of a URI pointing to a
 * trusted server for buyers' bidding data and a list of keys to query the server with.
 */
class CustomAudience public constructor(
    val buyer: AdTechIdentifier,
    val name: String,
    val dailyUpdateUri: Uri,
    val biddingLogicUri: Uri,
    val ads: List<AdData>,
    val activationTime: Instant? = null,
    val expirationTime: Instant? = null,
    val userBiddingSignals: AdSelectionSignals? = null,
    val trustedBiddingSignals: TrustedBiddingData? = null
) {

    /**
     * Checks whether two [CustomAudience] objects contain the same information.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomAudience) return false
        return this.buyer == other.buyer &&
            this.name == other.name &&
            this.activationTime == other.activationTime &&
            this.expirationTime == other.expirationTime &&
            this.dailyUpdateUri == other.dailyUpdateUri &&
            this.userBiddingSignals == other.userBiddingSignals &&
            this.trustedBiddingSignals == other.trustedBiddingSignals &&
            this.ads == other.ads
    }

    /**
     * Returns the hash of the [CustomAudience] object's data.
     */
    override fun hashCode(): Int {
        var hash = buyer.hashCode()
        hash = 31 * hash + name.hashCode()
        hash = 31 * hash + activationTime.hashCode()
        hash = 31 * hash + expirationTime.hashCode()
        hash = 31 * hash + dailyUpdateUri.hashCode()
        hash = 31 * hash + userBiddingSignals.hashCode()
        hash = 31 * hash + trustedBiddingSignals.hashCode()
        hash = 31 * hash + biddingLogicUri.hashCode()
        hash = 31 * hash + ads.hashCode()
        return hash
    }

    override fun toString(): String {
        return "CustomAudience: " +
            "buyer=$biddingLogicUri, activationTime=$activationTime, " +
            "expirationTime=$expirationTime, dailyUpdateUri=$dailyUpdateUri, " +
            "userBiddingSignals=$userBiddingSignals, " +
            "trustedBiddingSignals=$trustedBiddingSignals, " +
            "biddingLogicUri=$biddingLogicUri, ads=$ads"
    }

    /** Builder for [CustomAudience] objects. */
    @SuppressWarnings("OptionalBuilderConstructorArgument")
    public class Builder(
        private var buyer: AdTechIdentifier,
        private var name: String,
        private var dailyUpdateUri: Uri,
        private var biddingLogicUri: Uri,
        private var ads: List<AdData>
    ) {
        private var activationTime: Instant? = null
        private var expirationTime: Instant? = null
        private var userBiddingSignals: AdSelectionSignals? = null
        private var trustedBiddingData: TrustedBiddingData? = null

        /**
         * Sets the buyer [AdTechIdentifier].
         *
         * @param buyer A buyer is identified by a domain in the form "buyerexample.com".
         */
        fun setBuyer(buyer: AdTechIdentifier): Builder = apply {
            this.buyer = buyer
        }

        /**
         * Sets the [CustomAudience] object's name.
         *
         * @param name  The custom audience's name is an arbitrary string provided by the owner and
         * buyer on creation of the [CustomAudience] object.
         */
        fun setName(name: String): Builder = apply {
            this.name = name
        }

        /**
         * On creation of the [CustomAudience] object, an optional activation time may be set
         * in the future, in order to serve a delayed activation. If the field is not set, the
         * [CustomAudience] will be activated at the time of joining.
         *
         * For example, a custom audience for lapsed users may not activate until a threshold of
         * inactivity is reached, at which point the custom audience's ads will participate in the
         * ad selection process, potentially redirecting lapsed users to the original owner
         * application.
         *
         * The maximum delay in activation is 60 days from initial creation.
         *
         * If specified, the activation time must be an earlier instant than the expiration time.
         *
         * @param activationTime activation time, truncated to milliseconds, after which the
         * [CustomAudience] will serve ads.
         */
        fun setActivationTime(activationTime: Instant): Builder = apply {
            this.activationTime = activationTime
        }

        /**
         * Once the expiration time has passed, a custom audience is no longer eligible for daily
         * ad/bidding data updates or participation in the ad selection process. The custom audience
         * will then be deleted from memory by the next daily update.
         *
         * If no expiration time is provided on creation of the [CustomAudience], expiry will
         * default to 60 days from activation.
         *
         * The maximum expiry is 60 days from initial activation.
         *
         * @param expirationTime the timestamp [Instant], truncated to milliseconds, after
         * which the custom audience should be removed.
         */
        fun setExpirationTime(expirationTime: Instant): Builder = apply {
            this.expirationTime = expirationTime
        }

        /**
         * This URI points to a buyer-operated server that hosts updated bidding data and ads
         * metadata to be used in the on-device ad selection process. The URI must use HTTPS.
         *
         * @param dailyUpdateUri the custom audience's daily update URI
         */
        fun setDailyUpdateUri(dailyUpdateUri: Uri): Builder = apply {
            this.dailyUpdateUri = dailyUpdateUri
        }

        /**
         * User bidding signals are optionally provided by buyers to be consumed by buyer-provided
         * JavaScript during ad selection in an isolated execution environment.
         *
         * If the user bidding signals are not a valid JSON object that can be consumed by the
         * buyer's JS, the custom audience will not be eligible for ad selection.
         *
         * If not specified, the [CustomAudience] will not participate in ad selection
         * until user bidding signals are provided via the daily update for the custom audience.
         *
         * @param userBiddingSignals an [AdSelectionSignals] object representing the user
         * bidding signals for the custom audience
         */
        fun setUserBiddingSignals(userBiddingSignals: AdSelectionSignals): Builder = apply {
            this.userBiddingSignals = userBiddingSignals
        }

        /**
         * Trusted bidding data consists of a URI pointing to a trusted server for buyers' bidding data
         * and a list of keys to query the server with. Note that the keys are arbitrary identifiers
         * that will only be used to query the trusted server for a buyer's bidding logic during ad
         * selection.
         *
         * If not specified, the [CustomAudience] will not participate in ad selection
         * until trusted bidding data are provided via the daily update for the custom audience.
         *
         * @param trustedBiddingSignals a [TrustedBiddingData] object containing the custom
         * audience's trusted bidding data.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setTrustedBiddingData(trustedBiddingSignals: TrustedBiddingData): Builder = apply {
            this.trustedBiddingData = trustedBiddingSignals
        }

        /**
         * Returns the target URI used to fetch bidding logic when a custom audience participates in the
         * ad selection process. The URI must use HTTPS.
         *
         * @param biddingLogicUri the URI for fetching buyer bidding logic
         */
        fun setBiddingLogicUri(biddingLogicUri: Uri): Builder = apply {
            this.biddingLogicUri = biddingLogicUri
        }

        /**
         * This list of [AdData] objects is a full and complete list of the ads that will be
         * served by this [CustomAudience] during the ad selection process.
         *
         * If not specified, or if an empty list is provided, the [CustomAudience] will not
         * participate in ad selection until a valid list of ads are provided via the daily update
         * for the custom audience.
         *
         * @param ads a [List] of [AdData] objects representing ads currently served by
         * the custom audience.
         */
        fun setAds(ads: List<AdData>): Builder = apply {
            this.ads = ads
        }

        /**
         * Builds an instance of a [CustomAudience].
         */
        fun build(): CustomAudience {
            return CustomAudience(
                buyer,
                name,
                dailyUpdateUri,
                biddingLogicUri,
                ads,
                activationTime,
                expirationTime,
                userBiddingSignals,
                trustedBiddingData
            )
        }
    }
}
