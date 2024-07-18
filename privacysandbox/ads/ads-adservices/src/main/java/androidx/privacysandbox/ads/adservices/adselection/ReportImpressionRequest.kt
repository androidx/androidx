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

/**
 * Represent input parameters to the reportImpression API.
 *
 * @param adSelectionId An ID unique only to a device user that identifies a successful ad
 *     selection.
 * @param adSelectionConfig The same configuration used in the selectAds() call identified by the
 *      provided ad selection ID.
 */
class ReportImpressionRequest public constructor(
    val adSelectionId: Long,
    val adSelectionConfig: AdSelectionConfig
) {

    /** Checks whether two [ReportImpressionRequest] objects contain the same information.  */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportImpressionRequest) return false
        return this.adSelectionId == other.adSelectionId &&
            this.adSelectionConfig == other.adSelectionConfig
    }

    /** Returns the hash of the [ReportImpressionRequest] object's data.  */
    override fun hashCode(): Int {
        var hash = adSelectionId.hashCode()
        hash = 31 * hash + adSelectionConfig.hashCode()
        return hash
    }

    /** Overrides the toString method.  */
    override fun toString(): String {
        return "ReportImpressionRequest: adSelectionId=$adSelectionId, " +
            "adSelectionConfig=$adSelectionConfig"
    }
}
