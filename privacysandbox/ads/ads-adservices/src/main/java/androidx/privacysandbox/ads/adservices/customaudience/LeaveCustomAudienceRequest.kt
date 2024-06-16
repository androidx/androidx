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

package androidx.privacysandbox.ads.adservices.customaudience

import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier

/**
 * The request object to leave a custom audience.
 *
 * @param buyer an [AdTechIdentifier] containing the custom audience's buyer's domain.
 * @param name the String name of the custom audience.
 */
class LeaveCustomAudienceRequest public constructor(val buyer: AdTechIdentifier, val name: String) {

    /** Checks whether two [LeaveCustomAudienceRequest] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LeaveCustomAudienceRequest) return false
        return this.buyer == other.buyer && this.name == other.name
    }

    /** Returns the hash of the [LeaveCustomAudienceRequest] object's data. */
    override fun hashCode(): Int {
        return (31 * buyer.hashCode()) + name.hashCode()
    }

    override fun toString(): String {
        return "LeaveCustomAudience: buyer=$buyer, name=$name"
    }
}
