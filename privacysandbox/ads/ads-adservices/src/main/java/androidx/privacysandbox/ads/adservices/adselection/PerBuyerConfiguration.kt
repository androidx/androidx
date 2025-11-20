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
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * Contains a per buyer configuration which will be used as part of a SellerConfiguration in a
 * GetAdSelectionDataRequest. This object will be created by the calling SDK as part of creating the
 * seller configuration.
 *
 * @param targetInputSizeBytes The service will make a best effort attempt to include this amount of
 *   bytes into the response of GetAdSelectionData for this buyer.
 * @param buyer associated with this per buyer configuration.
 */
@OptIn(ExperimentalFeatures.Ext14OptIn::class)
@ExperimentalFeatures.Ext14OptIn
public class PerBuyerConfiguration
public constructor(public val targetInputSizeBytes: Int, public val buyer: AdTechIdentifier) {
    /** Overrides the toString method. */
    override fun toString(): String {
        return "PerBuyerConfiguration: targetInputSizeBytes=$targetInputSizeBytes, " +
            "buyer=$buyer"
    }

    /** Checks whether two [PerBuyerConfiguration] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerBuyerConfiguration) return false
        return this.targetInputSizeBytes == other.targetInputSizeBytes &&
            this.buyer.equals(other.buyer)
    }

    /** Returns the hash of the [PerBuyerConfiguration] object's data. */
    override fun hashCode(): Int {
        var hash = targetInputSizeBytes.hashCode()
        hash = 31 * hash + buyer.hashCode()
        return hash
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    internal fun convertToAdServices(): android.adservices.adselection.PerBuyerConfiguration {
        if (
            AdServicesInfo.adServicesVersion() >= 14 && AdServicesInfo.extServicesVersionS() >= 14
        ) {
            return Ext14Impl.convertPerBuyerConfiguration(this)
        }
        throw UnsupportedOperationException("API is not available. Min version is API 31 ext 14")
    }

    private class Ext14Impl private constructor() {
        companion object {
            @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
            @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
            fun convertPerBuyerConfiguration(
                perBuyerConfiguration: PerBuyerConfiguration
            ): android.adservices.adselection.PerBuyerConfiguration {
                return android.adservices.adselection.PerBuyerConfiguration.Builder()
                    .setBuyer(perBuyerConfiguration.buyer.convertToAdServices())
                    .setTargetInputSizeBytes(perBuyerConfiguration.targetInputSizeBytes)
                    .build()
            }
        }
    }
}
