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
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/**
 * Represent input parameters to the [AdSelectionManager#persistAdSelectionResult] API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param seller AdTechIdentifier of the seller, for example "www.example-ssp.com".
 * @param adSelectionResult The adSelectionResult that is collected from device.
 */
@ExperimentalFeatures.Ext10OptIn
class PersistAdSelectionResultRequest
@JvmOverloads
public constructor(
    val adSelectionId: Long,
    val seller: AdTechIdentifier? = null,
    val adSelectionResult: ByteArray? = null,
) {
    /** Checks whether two [PersistAdSelectionResultRequest] contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PersistAdSelectionResultRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.seller == other.seller &&
            this.adSelectionResult.contentEquals(other.adSelectionResult)
    }

    /** Returns the hash of the [PersistAdSelectionResultRequest] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + seller.hashCode()
        hash = 31 * hash + adSelectionResult.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "PersistAdSelectionResultRequest: adSelectionId=$adSelectionId, " +
            "seller=$seller, adSelectionResult=$adSelectionResult"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    internal fun convertToAdServices():
        android.adservices.adselection.PersistAdSelectionResultRequest {
        @Suppress("DEPRECATION")
        return android.adservices.adselection.PersistAdSelectionResultRequest.Builder()
            .setAdSelectionId(adSelectionId)
            .setSeller(seller?.convertToAdServices())
            .setAdSelectionResult(adSelectionResult)
            .build()
    }
}
