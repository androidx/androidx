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

package androidx.credentials.providerevents.transfer

import androidx.annotation.RestrictTo
import androidx.credentials.providerevents.internal.RequestValidationHelper
import androidx.credentials.providerevents.internal.getCredentialTypes
import androidx.credentials.providerevents.internal.getKnownExtensions
import androidx.credentials.providerevents.internal.toRequestJson
import org.json.JSONException
import org.json.JSONObject

/**
 * A request for the state of the provider's credentials that can be exported
 *
 * @property credentialTypes the credential types that the requester supports. By default, this
 *   field will be used to filter which [ExportEntry] will be displayed. The values include, but not
 *   limited to, the constants in [CredentialTypes]. This cannot be empty. The exporter should
 *   return credentials that map to the requested types. Unknown values should be ignored.
 * @property knownExtensions the known extensions that the importer supports. The values include,
 *   but not limited to, the constants in [KnownExtensions]. This can be empty. The exporter should
 *   return extensions that map to the requested extensions. Unknown values should be ignored.
 */
public class CredentialTransferCapabilitiesRequest(
    public val credentialTypes: Set<String>,
    public val knownExtensions: Set<String>,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val requestJson: String = toRequestJson(credentialTypes, knownExtensions)

    init {
        require(credentialTypes.isNotEmpty()) { "credentialTypes must not be empty" }
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createFrom(requestJson: String): CredentialTransferCapabilitiesRequest? {
            if (!RequestValidationHelper.isValidJSON(requestJson)) {
                return null
            }
            try {
                val json = JSONObject(requestJson)
                val credentialTypes = getCredentialTypes(json)
                val knownExtensions = getKnownExtensions(json)
                return CredentialTransferCapabilitiesRequest(credentialTypes, knownExtensions)
            } catch (e: JSONException) {}
            return null
        }
    }
}
