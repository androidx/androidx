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

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.InputEvent
import androidx.annotation.IntDef
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * Represent input parameters to the reportImpression API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param eventKey An event key, the type of ad event to be reported.
 * @param eventData The ad event data
 * @param reportingDestinations The bitfield of reporting destinations to report to (buyer, seller,
 *   or both).
 * @param inputEvent The input event associated with the user interaction.
 */
@OptIn(ExperimentalFeatures.Ext10OptIn::class)
@ExperimentalFeatures.Ext8OptIn
class ReportEventRequest
@JvmOverloads
public constructor(
    val adSelectionId: Long,
    val eventKey: String,
    val eventData: String,
    @ReportingDestination val reportingDestinations: Int,
    @property:ExperimentalFeatures.Ext10OptIn val inputEvent: InputEvent? = null
) {
    init {
        require(
            0 < reportingDestinations &&
                reportingDestinations <=
                    (FLAG_REPORTING_DESTINATION_SELLER or FLAG_REPORTING_DESTINATION_BUYER)
        ) {
            "Invalid reporting destinations bitfield."
        }
    }

    /** Checks whether two [ReportImpressionRequest] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportEventRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.eventKey == other.eventKey &&
            this.eventData == other.eventData &&
            this.reportingDestinations == other.reportingDestinations &&
            this.inputEvent == other.inputEvent
    }

    /** Returns the hash of the [ReportImpressionRequest] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + eventKey.hashCode()
        hash = 31 * hash + eventData.hashCode()
        hash = 31 * hash + reportingDestinations.hashCode()
        hash = 31 * hash + inputEvent.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "ReportEventRequest: adSelectionId=$adSelectionId, eventKey=$eventKey, " +
            "eventData=$eventData, reportingDestinations=$reportingDestinations" +
            "inputEvent=$inputEvent"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true,
        value =
            [
                Companion.FLAG_REPORTING_DESTINATION_SELLER,
                Companion.FLAG_REPORTING_DESTINATION_BUYER
            ]
    )
    annotation class ReportingDestination

    companion object {
        const val FLAG_REPORTING_DESTINATION_SELLER: Int =
            android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER
        const val FLAG_REPORTING_DESTINATION_BUYER: Int =
            android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
    }

    @SuppressLint("NewApi")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.adselection.ReportEventRequest {
        if (
            AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersionS() >= 10
        ) {
            return Ext10Impl.convertReportEventRequest(this)
        }
        return Ext8Impl.convertReportEventRequest(this)
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            fun convertReportEventRequest(
                request: ReportEventRequest
            ): android.adservices.adselection.ReportEventRequest {
                return android.adservices.adselection.ReportEventRequest.Builder(
                        request.adSelectionId,
                        request.eventKey,
                        request.eventData,
                        request.reportingDestinations
                    )
                    .setInputEvent(request.inputEvent)
                    .build()
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private class Ext8Impl private constructor() {
        companion object {
            fun convertReportEventRequest(
                request: ReportEventRequest
            ): android.adservices.adselection.ReportEventRequest {
                request.inputEvent?.let {
                    Log.w(
                        "ReportEventRequest",
                        "inputEvent is ignored. Min version to use inputEvent is API 31 ext 10"
                    )
                }
                return android.adservices.adselection.ReportEventRequest.Builder(
                        request.adSelectionId,
                        request.eventKey,
                        request.eventData,
                        request.reportingDestinations
                    )
                    .build()
            }
        }
    }
}
