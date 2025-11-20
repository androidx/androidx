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

import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialEntry
import androidx.credentials.registry.provider.digitalcredentials.EntryDisplayProperties

/**
 * A digital credential entry conforming to the
 * [IETF SD-JWT-based Verifiable Credentials (SD-JWT VC)](https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/)
 * credential format.
 *
 * @property verifiableCredentialType the verifiable credential type (vct) as defined in
 *   [the SD-JWT VC spec](https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-10.html#section-3.2.2.1)
 * @property claims the individual attributes associated with this credential
 * @property entryDisplayPropertySet a set of entry display metadata, each serving a different UI
 *   style variant
 * @property id the unique identifier of this credential entry, which can be used to identify the
 *   exact credential that the user has chosen
 * @constructor
 * @throws IllegalArgumentException if [id] length is greater than 64 characters
 */
public class SdJwtEntry(
    public val verifiableCredentialType: String,
    public val claims: List<SdJwtClaim>,
    entryDisplayPropertySet: Set<EntryDisplayProperties>,
    id: String,
) : DigitalCredentialEntry(id = id, entryDisplayPropertySet = entryDisplayPropertySet) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SdJwtEntry) return false
        return this.id == other.id &&
            this.entryDisplayPropertySet == other.entryDisplayPropertySet &&
            this.verifiableCredentialType == other.verifiableCredentialType &&
            this.claims == other.claims &&
            this.entryDisplayPropertySet == other.entryDisplayPropertySet
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entryDisplayPropertySet.hashCode()
        result = 31 * result + verifiableCredentialType.hashCode()
        result = 31 * result + claims.hashCode()
        return result
    }
}
