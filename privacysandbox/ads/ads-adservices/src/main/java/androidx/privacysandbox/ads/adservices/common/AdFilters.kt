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

package androidx.privacysandbox.ads.adservices.common

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/**
 * A container class for filters which are associated with an ad.
 *
 * If any of the filters in an [AdFilters] instance are not satisfied, the associated ad will not be
 * eligible for ad selection. Filters are optional ad parameters and are not required as part of
 * [AdData].
 *
 * @param frequencyCapFilters Gets the [FrequencyCapFilters] instance that represents all frequency
 *   cap filters for the ad.
 */
@ExperimentalFeatures.Ext8OptIn
class AdFilters public constructor(val frequencyCapFilters: FrequencyCapFilters?) {
    /** Checks whether two [AdFilters] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdFilters) return false
        return this.frequencyCapFilters == other.frequencyCapFilters
    }

    /** Returns the hash of the [AdFilters] object's data. */
    override fun hashCode(): Int {
        return frequencyCapFilters.hashCode()
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "AdFilters: frequencyCapFilters=$frequencyCapFilters"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.common.AdFilters {
        return android.adservices.common.AdFilters.Builder()
            .setFrequencyCapFilters(frequencyCapFilters?.convertToAdServices())
            .build()
    }
}
