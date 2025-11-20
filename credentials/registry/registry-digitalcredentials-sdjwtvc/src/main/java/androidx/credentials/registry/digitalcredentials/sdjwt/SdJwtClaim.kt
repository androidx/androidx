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

import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialField
import androidx.credentials.registry.provider.digitalcredentials.FieldDisplayProperties

/**
 * An attribute of a [SdJwtEntry] conforming to the
 * [IETF SD-JWT-based Verifiable Credentials (SD-JWT VC)](https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/)
 * credential format.
 *
 * @property value the json-serializable claim value, used for matching when the incoming Digital
 *   Credentials request specifies a required value for this claim;
 * @property path a non-empty array of path strings used to select this given claim during matching
 * @property isSelectivelyDisclosable true (default) if the given claim is selectively disclosable,
 *   or false otherwise when the claim will always be disclosed
 * @property fieldDisplayPropertySet a set of field display metadata, each serving a different UI
 *   style variant
 * @constructor
 */
public class SdJwtClaim(
    public val path: List<String>,
    public val value: Any?,
    fieldDisplayPropertySet: Set<FieldDisplayProperties>,
    public val isSelectivelyDisclosable: Boolean = true,
) : DigitalCredentialField(fieldDisplayPropertySet = fieldDisplayPropertySet) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SdJwtClaim) return false
        return this.fieldDisplayPropertySet == other.fieldDisplayPropertySet &&
            this.path == other.path &&
            this.value == other.value &&
            this.isSelectivelyDisclosable == other.isSelectivelyDisclosable
    }

    override fun hashCode(): Int {
        var result = fieldDisplayPropertySet.hashCode()
        result = 31 * result + isSelectivelyDisclosable.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
