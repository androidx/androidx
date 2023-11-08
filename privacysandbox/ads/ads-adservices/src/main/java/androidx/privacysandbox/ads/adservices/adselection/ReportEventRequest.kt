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

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/**
 * Represent input parameters to the reportImpression API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 * selection.
 * @param eventKey An event key, the type of ad event to be reported.
 * @param eventData The ad event data
 * @param reportingDestinations The bitfield of reporting destinations to report to (buyer, seller,
 * or both).
 */
@ExperimentalFeatures.Ext8OptIn
class ReportEventRequest public constructor(
    val adSelectionId: Long,
    val eventKey: String,
    val eventData: String,
    val reportingDestinations: Int
) {
    init {
        require(0 < reportingDestinations &&
            reportingDestinations
                <= (FLAG_REPORTING_DESTINATION_SELLER or FLAG_REPORTING_DESTINATION_BUYER)) {
            "Invalid reporting destinations bitfield."
        }
    }

    /** Checks whether two [ReportImpressionRequest] objects contain the same information.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportEventRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.eventKey == other.eventKey &&
            this.eventData == other.eventData &&
            this.reportingDestinations == other.reportingDestinations
    }

    /** Returns the hash of the [ReportImpressionRequest] object's data.  */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + eventKey.hashCode()
        hash = 31 * hash + eventData.hashCode()
        hash = 31 * hash + reportingDestinations.hashCode()
        return hash
    }

    /** Overrides the toString method.  */
    override fun toString(): String {
        return "ReportEventRequest: adSelectionId=$adSelectionId, eventKey=$eventKey, " +
            "eventData=$eventData, reportingDestinations=$reportingDestinations"
    }

    companion object {
        const val FLAG_REPORTING_DESTINATION_SELLER: Int =
            android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER
        const val FLAG_REPORTING_DESTINATION_BUYER: Int =
            android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.adselection.ReportEventRequest {
        return android.adservices.adselection.ReportEventRequest.Builder(
            adSelectionId,
            eventKey,
            eventData,
            reportingDestinations)
            .build()
    }
}
