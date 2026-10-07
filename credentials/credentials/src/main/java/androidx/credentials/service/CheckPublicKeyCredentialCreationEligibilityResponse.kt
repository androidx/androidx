/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.service

import androidx.annotation.RestrictTo

/**
 * The relying party's answer to
 * [RelyingPartyEventsService.onCheckPublicKeyCredentialCreationEligibility].
 *
 * @property isEligible whether this relying party has an account that is ready for passkey creation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CheckPublicKeyCredentialCreationEligibilityResponse(public val isEligible: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CheckPublicKeyCredentialCreationEligibilityResponse) return false
        return isEligible == other.isEligible
    }

    override fun hashCode(): Int = isEligible.hashCode()

    override fun toString(): String =
        "CheckPublicKeyCredentialCreationEligibilityResponse(isEligible=$isEligible)"
}
