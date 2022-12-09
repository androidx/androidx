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

import android.net.Uri

/**
 * This class represents  input to the [AdSelectionManager#selectAds] in the
 * [AdSelectionManager]. This field is populated in the case of a successful
 * [AdSelectionManager#selectAds] call.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *     selection.
 * @param renderUri A render URL for the winning ad.
 */
class AdSelectionOutcome public constructor(
    val adSelectionId: Long,
    val renderUri: Uri
) {

    /** Checks whether two [AdSelectionOutcome] objects contain the same information.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdSelectionOutcome) return false
        return this.adSelectionId == other.adSelectionId &&
            this.renderUri == other.renderUri
    }

    /** Returns the hash of the [AdSelectionOutcome] object's data.  */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + renderUri.hashCode()
        return hash
    }

    /** Overrides the toString method.  */
    override fun toString(): String {
        return "AdSelectionOutcome: adSelectionId=$adSelectionId, renderUri=$renderUri"
    }
}
