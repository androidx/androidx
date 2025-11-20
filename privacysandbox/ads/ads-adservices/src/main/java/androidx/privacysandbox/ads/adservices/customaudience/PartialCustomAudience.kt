/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import java.time.Instant

/**
 * Represents a partial custom audience that is passed along to DSP, when scheduling a delayed
 * update for Custom Audience. Any field set by the caller cannot be overridden by the custom
 * audience fetched from the `updateUri`.
 *
 * Given multiple Custom Audiences could be returned by DSP we will match the override restriction
 * based on the name of Custom Audience. Thus name would be a required field.
 *
 * For more information about each field refer to [CustomAudience].
 */
@ExperimentalFeatures.Ext14OptIn
public class PartialCustomAudience
@JvmOverloads
constructor(
    public val name: String,
    public val activationTime: Instant? = null,
    public val expirationTime: Instant? = null,
    public val userBiddingSignals: AdSelectionSignals? = null,
) {
    /** Checks whether two PartialCustomAudience objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PartialCustomAudience) return false
        return this.name == other.name &&
            this.activationTime == other.activationTime &&
            this.expirationTime == other.expirationTime &&
            this.userBiddingSignals == other.userBiddingSignals
    }

    /** Returns the hash of the PartialCustomAudience object's data. */
    override fun hashCode(): Int {
        var hash = name.hashCode()
        hash = 31 * hash + activationTime.hashCode()
        hash = 31 * hash + expirationTime.hashCode()
        hash = 31 * hash + userBiddingSignals.hashCode()
        return hash
    }

    override fun toString(): String {
        return "PartialCustomAudience: name=$name, " +
            "activationTime=$activationTime, expirationTime=$expirationTime, " +
            "userBiddingSignals=$userBiddingSignals"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    internal fun convertToAdServices(): android.adservices.customaudience.PartialCustomAudience {
        val builder = android.adservices.customaudience.PartialCustomAudience.Builder(name)
        builder.run {
            activationTime?.let { setActivationTime(it) }
            expirationTime?.let { setExpirationTime(it) }
            userBiddingSignals?.let { setUserBiddingSignals(it.convertToAdServices()) }
        }
        return builder.build()
    }
}
