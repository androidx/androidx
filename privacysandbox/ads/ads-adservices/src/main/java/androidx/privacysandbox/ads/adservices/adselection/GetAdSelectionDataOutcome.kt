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
 * This class represents the output of the [AdSelectionManager#getAdSelectionData] in the
 * [AdSelectionManager]. The fields are populated in the case of a successful
 * [AdSelectionManager#getAdSelectionData] call.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param adSelectionData The adSelectionData that is collected from device.
 */
@ExperimentalFeatures.Ext10OptIn
class GetAdSelectionDataOutcome
@JvmOverloads
public constructor(val adSelectionId: Long, val adSelectionData: ByteArray? = null) {

    /** Checks whether two [GetAdSelectionDataOutcome] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetAdSelectionDataOutcome) return false
        return this.adSelectionId == other.adSelectionId &&
            this.adSelectionData.contentEquals(other.adSelectionData)
    }

    /** Returns the hash of the [GetAdSelectionDataOutcome] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + adSelectionData.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "GetAdSelectionDataOutcome: adSelectionId=$adSelectionId, " +
            "adSelectionData=$adSelectionData"
    }

    @Suppress("DEPRECATION")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    internal constructor(
        response: android.adservices.adselection.GetAdSelectionDataOutcome
    ) : this(response.adSelectionId, response.adSelectionData)
}
