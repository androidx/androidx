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
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * Represent input parameters to the reportImpression API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param adSelectionConfig optional config used in the selectAds() call identified by the provided
 *   ad selection ID. If the {@code adSelectionId} is for a on-device auction run using
 *   [AdSelectionManager#selectAds], then the config must be included. If the {@code adSelectionId}
 *   is for a server auction run where device info collected
 *   by [AdSelectionManager#getAdSelectionData} then the impression reporting request should only
 *   include the ad selection id.
 */
@SuppressLint("ClassVerificationFailure")
class ReportImpressionRequest
public constructor(val adSelectionId: Long, val adSelectionConfig: AdSelectionConfig) {
    @ExperimentalFeatures.Ext8OptIn
    constructor(adSelectionId: Long) : this(adSelectionId, AdSelectionConfig.EMPTY)

    /** Checks whether two [ReportImpressionRequest] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportImpressionRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.adSelectionConfig == other.adSelectionConfig
    }

    /** Returns the hash of the [ReportImpressionRequest] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + adSelectionConfig.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "ReportImpressionRequest: adSelectionId=$adSelectionId, " +
            "adSelectionConfig=$adSelectionConfig"
    }

    @SuppressLint("NewApi")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun convertToAdServices(): android.adservices.adselection.ReportImpressionRequest {
        if (
            AdServicesInfo.adServicesVersion() >= 10 || AdServicesInfo.extServicesVersionS() >= 10
        ) {
            return Ext10Impl.convertReportImpressionRequest(this)
        }
        return Ext4Impl.convertReportImpressionRequest(this)
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            fun convertReportImpressionRequest(
                request: ReportImpressionRequest
            ): android.adservices.adselection.ReportImpressionRequest {
                return if (request.adSelectionConfig == AdSelectionConfig.EMPTY)
                    android.adservices.adselection.ReportImpressionRequest(request.adSelectionId)
                else
                    android.adservices.adselection.ReportImpressionRequest(
                        request.adSelectionId,
                        request.adSelectionConfig.convertToAdServices()
                    )
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    private class Ext4Impl private constructor() {
        companion object {
            fun convertReportImpressionRequest(
                request: ReportImpressionRequest
            ): android.adservices.adselection.ReportImpressionRequest {
                if (request.adSelectionConfig == AdSelectionConfig.EMPTY) {
                    throw UnsupportedOperationException(
                        "adSelectionConfig is mandatory for" + "API versions lower than ext 10"
                    )
                }
                return android.adservices.adselection.ReportImpressionRequest(
                    request.adSelectionId,
                    request.adSelectionConfig.convertToAdServices()
                )
            }
        }
    }
}
