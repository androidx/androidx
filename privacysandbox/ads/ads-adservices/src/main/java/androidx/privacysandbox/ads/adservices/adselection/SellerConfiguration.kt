/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * SellerConfiguration will be part of the GetAdSelectionDataRequest and will be constructed and
 * used by the SDK to influence the size of the response of GetAdSelectionData API.
 *
 * @param maximumPayloadSizeBytes Returns the maximum size of the payload in bytes that the service
 *   will return.
 * @param perBuyerConfigurations Returns a set of per buyer configurations that the service will do
 *   a best effort to respect when constructing the response without exceeding
 *   maximumPayloadSizeBytes. If this is empty, the service will fill up the response with buyer
 *   data until maximumPayloadSizeBytes is reached. Otherwise, only data from buyers from the per
 *   buyer configuration will be included.
 */
@OptIn(ExperimentalFeatures.Ext14OptIn::class)
@ExperimentalFeatures.Ext14OptIn
public class SellerConfiguration
public constructor(
    public val maximumPayloadSizeBytes: Int,
    public val perBuyerConfigurations: Set<PerBuyerConfiguration>,
) {
    /** Overrides the toString method. */
    override fun toString(): String {
        return "SellerConfiguration: maximumPayloadSizeBytes=$maximumPayloadSizeBytes, " +
            "perBuyerConfigurations=$perBuyerConfigurations"
    }

    /** Checks whether two [SellerConfiguration] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SellerConfiguration) return false
        return this.maximumPayloadSizeBytes == other.maximumPayloadSizeBytes &&
            this.perBuyerConfigurations == other.perBuyerConfigurations
    }

    /** Returns the hash of the [SellerConfiguration] object's data. */
    override fun hashCode(): Int {
        var hash = maximumPayloadSizeBytes.hashCode()
        hash = 31 * hash + perBuyerConfigurations.hashCode()
        return hash
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    internal fun convertToAdServices(): android.adservices.adselection.SellerConfiguration {
        if (
            AdServicesInfo.adServicesVersion() >= 14 && AdServicesInfo.extServicesVersionS() >= 14
        ) {
            return Ext14Impl.convertSellerConfiguration(this)
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 14")
    }

    private class Ext14Impl private constructor() {
        companion object {
            @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
            @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
            fun convertSellerConfiguration(
                sellerConfiguration: SellerConfiguration
            ): android.adservices.adselection.SellerConfiguration {
                return android.adservices.adselection.SellerConfiguration.Builder()
                    .setMaximumPayloadSizeBytes(sellerConfiguration.maximumPayloadSizeBytes)
                    .setPerBuyerConfigurations(
                        sellerConfiguration.perBuyerConfigurations
                            .map { it.convertToAdServices() }
                            .toSet()
                    )
                    .build()
            }
        }
    }
}
