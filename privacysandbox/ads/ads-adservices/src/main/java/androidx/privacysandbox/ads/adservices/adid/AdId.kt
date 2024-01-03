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

package androidx.privacysandbox.ads.adservices.adid

/**
 * A unique, user-resettable, device-wide, per-profile ID for advertising as returned by the
 * [AdIdManager#getAdId()] API.
 *
 * Ad networks may use {@code AdId} to monetize for Interest Based Advertising (IBA), i.e.
 * targeting and remarketing ads. The user may limit availability of this identifier.
 *
 * @param adId The advertising ID.
 * @param isLimitAdTrackingEnabled the limit ad tracking enabled setting.
 */
class AdId internal constructor(
    val adId: String,
    val isLimitAdTrackingEnabled: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdId) return false
        return this.adId == other.adId &&
            this.isLimitAdTrackingEnabled == other.isLimitAdTrackingEnabled
    }

    override fun hashCode(): Int {
        var hash = adId.hashCode()
        hash = 31 * hash + isLimitAdTrackingEnabled.hashCode()
        return hash
    }

    override fun toString(): String {
        return "AdId: adId=$adId, isLimitAdTrackingEnabled=$isLimitAdTrackingEnabled"
    }
}
