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

package androidx.privacysandbox.ads.adservices.customaudience

import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import java.time.Instant

/**
 * The request object to fetch and join a custom audience.
 *
 * @param fetchUri The [Uri] from which the custom audience is to be fetched.
 * @param name The name of the custom audience to join.
 * @param activationTime The [Instant] by which joining the custom audience will be delayed.
 * @param expirationTime The [Instant] by when the membership to the custom audience will expire.
 * @param userBiddingSignals The [AdSelectionSignals] object representing the user bidding signals
 *   for the custom audience.
 */
@ExperimentalFeatures.Ext10OptIn
class FetchAndJoinCustomAudienceRequest
@JvmOverloads
public constructor(
    val fetchUri: Uri,
    val name: String? = null,
    val activationTime: Instant? = null,
    val expirationTime: Instant? = null,
    val userBiddingSignals: AdSelectionSignals? = null
) {
    /**
     * Checks whether two [FetchAndJoinCustomAudienceRequest] objects contain the same information.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FetchAndJoinCustomAudienceRequest) return false
        return this.fetchUri == other.fetchUri &&
            this.name == other.name &&
            this.activationTime == other.activationTime &&
            this.expirationTime == other.expirationTime &&
            this.userBiddingSignals == other.userBiddingSignals
    }

    /** Returns the hash of the [FetchAndJoinCustomAudienceRequest] object's data. */
    override fun hashCode(): Int {
        var hash = fetchUri.hashCode()
        hash = 31 * hash + name.hashCode()
        hash = 31 * hash + activationTime.hashCode()
        hash = 31 * hash + expirationTime.hashCode()
        hash = 31 * hash + userBiddingSignals.hashCode()
        return hash
    }

    override fun toString(): String {
        return "FetchAndJoinCustomAudienceRequest: fetchUri=$fetchUri, " +
            "name=$name, activationTime=$activationTime, " +
            "expirationTime=$expirationTime, userBiddingSignals=$userBiddingSignals"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    internal fun convertToAdServices():
        android.adservices.customaudience.FetchAndJoinCustomAudienceRequest {
        return android.adservices.customaudience.FetchAndJoinCustomAudienceRequest.Builder(fetchUri)
            .setName(name)
            .setActivationTime(activationTime)
            .setExpirationTime(expirationTime)
            .setUserBiddingSignals(userBiddingSignals?.convertToAdServices())
            .build()
    }
}
