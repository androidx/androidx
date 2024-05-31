/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters

/**
 * This class represents input to the [AdSelectionManager#updateAdCounterHistogram] in the
 * [AdSelectionManager].
 *
 * Note that the [FrequencyCapFilters.AD_EVENT_TYPE_WIN] event type cannot be updated manually using
 * the [AdSelectionManager#updateAdCounterHistogram] API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param adEventType A render URL for the winning ad.
 * @param callerAdTech The caller adtech entity's [AdTechIdentifier].
 */
@ExperimentalFeatures.Ext8OptIn
class UpdateAdCounterHistogramRequest
public constructor(
    val adSelectionId: Long,
    @FrequencyCapFilters.AdEventType val adEventType: Int,
    val callerAdTech: AdTechIdentifier
) {
    init {
        require(adEventType != FrequencyCapFilters.AD_EVENT_TYPE_WIN) {
            "Win event types cannot be manually updated."
        }
        require(
            adEventType == FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION ||
                adEventType == FrequencyCapFilters.AD_EVENT_TYPE_VIEW ||
                adEventType == FrequencyCapFilters.AD_EVENT_TYPE_CLICK
        ) {
            "Ad event type must be one of AD_EVENT_TYPE_IMPRESSION, AD_EVENT_TYPE_VIEW, or" +
                " AD_EVENT_TYPE_CLICK"
        }
    }

    /**
     * Checks whether two [UpdateAdCounterHistogramRequest] objects contain the same information.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateAdCounterHistogramRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.adEventType == other.adEventType &&
            this.callerAdTech == other.callerAdTech
    }

    /** Returns the hash of the [UpdateAdCounterHistogramRequest] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + adEventType.hashCode()
        hash = 31 * hash + callerAdTech.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        val adEventTypeStr =
            when (adEventType) {
                FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION -> "AD_EVENT_TYPE_IMPRESSION"
                FrequencyCapFilters.AD_EVENT_TYPE_VIEW -> "AD_EVENT_TYPE_VIEW"
                FrequencyCapFilters.AD_EVENT_TYPE_WIN -> "AD_EVENT_TYPE_WIN"
                FrequencyCapFilters.AD_EVENT_TYPE_CLICK -> "AD_EVENT_TYPE_CLICK"
                else -> "Invalid ad event type"
            }
        return "UpdateAdCounterHistogramRequest: adSelectionId=$adSelectionId, " +
            "adEventType=$adEventTypeStr, callerAdTech=$callerAdTech"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices():
        android.adservices.adselection.UpdateAdCounterHistogramRequest {
        return android.adservices.adselection.UpdateAdCounterHistogramRequest.Builder(
                adSelectionId,
                adEventType,
                callerAdTech.convertToAdServices()
            )
            .build()
    }
}
