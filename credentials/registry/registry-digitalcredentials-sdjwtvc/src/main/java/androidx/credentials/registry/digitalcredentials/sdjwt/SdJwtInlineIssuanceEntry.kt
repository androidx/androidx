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

package androidx.credentials.registry.digitalcredentials.sdjwt

import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry

/**
 * An entry offering the user an opportunity to add a digital credential to your holder / wallet
 * application during the presentation moment.
 *
 * This serves as a fallback or alternative to a real digital credential that can be presented. When
 * the verifier application requests for a digital credential presentation, it is possible that the
 * user has not officially add their corresponding digital credential(s) to their holder application
 * yet. The [SdJwtInlineIssuanceEntry] allows the holder application to add the credentials on the
 * fly and then immediately return them, allowing the request, as well as the future requests, to
 * still succeed.
 *
 * @property id the provider unique identifier of this credential entry, which can be used to
 *   identify the exact credential that the user has chosen; it is recommended that you generate
 *   this `id` with enough entropy that it cannot be guessed by a third party, e.g. through
 *   encrypting this `id` or randomizing it.
 * @property display the display properties associated with the given entry
 * @property supportedSdJwts supported SD-JWTs to offer the inline issuance flow; cannot be empty
 * @constructor
 * @throws IllegalArgumentException if [id] length is greater than 64 characters
 * @throws IllegalArgumentException if [supportedSdJwts] is empty
 */
public class SdJwtInlineIssuanceEntry(
    id: String,
    display: InlineIssuanceDisplayProperties,
    public val supportedSdJwts: Set<SupportedSdJwt>,
) : InlineIssuanceEntry(id = id, display = display) {
    init {
        require(supportedSdJwts.isNotEmpty()) { "`supportedSdJwts` must not be empty" }
    }

    /**
     * Configuration determining whether the [SdJwtInlineIssuanceEntry] should be offered for a
     * presentation request
     *
     * @property verifiableCredentialType the verifiable credential type (vct) as defined in
     *   [the SD-JWT VC spec](https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-10.html#section-3.2.2.1)
     * @constructor
     * @throws IllegalArgumentException if `verifiableCredentialType` is empty
     */
    public class SupportedSdJwt(public val verifiableCredentialType: String) {
        init {
            require(verifiableCredentialType.isNotEmpty()) { "`verifiableCredentialType` cannot" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SupportedSdJwt) return false
            return this.verifiableCredentialType == other.verifiableCredentialType
        }

        override fun hashCode(): Int {
            return verifiableCredentialType.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SdJwtInlineIssuanceEntry) return false
        return this.id == other.id &&
            this.display == other.display &&
            this.supportedSdJwts == other.supportedSdJwts
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + display.hashCode()
        result = 31 * result + supportedSdJwts.hashCode()
        return result
    }
}
