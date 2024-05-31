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
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures

/**
 * This class represents the output of the [AdSelectionManager#selectAds] in the
 * [AdSelectionManager]. The fields are populated in the case of a successful
 * [AdSelectionManager#selectAds] call.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *   selection.
 * @param renderUri A render URL for the winning ad.
 */
@SuppressLint("ClassVerificationFailure")
class AdSelectionOutcome public constructor(val adSelectionId: Long, val renderUri: Uri) {

    /** Checks whether two [AdSelectionOutcome] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdSelectionOutcome) return false
        return this.adSelectionId == other.adSelectionId && this.renderUri == other.renderUri
    }

    /** Returns the hash of the [AdSelectionOutcome] object's data. */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + renderUri.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "AdSelectionOutcome: adSelectionId=$adSelectionId, renderUri=$renderUri"
    }

    @ExperimentalFeatures.Ext10OptIn
    fun hasOutcome(): Boolean {
        return this != NO_OUTCOME
    }

    @ExperimentalFeatures.Ext10OptIn
    companion object {
        /** Represents an AdSelectionOutcome with empty results. */
        @ExperimentalFeatures.Ext10OptIn
        @JvmField
        public val NO_OUTCOME = AdSelectionOutcome(0, Uri.EMPTY)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal constructor(
        response: android.adservices.adselection.AdSelectionOutcome
    ) : this(response.adSelectionId, response.renderUri)
}
