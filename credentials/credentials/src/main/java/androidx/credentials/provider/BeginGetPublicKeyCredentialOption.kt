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

package androidx.credentials.provider

import android.os.Bundle
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A request to begin the flow of getting passkeys from the user's public key credential provider.
 *
 * @property requestJson the request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
 * @property clientDataHash a hash that is used to verify the relying party identity, set only if
 * [android.service.credentials.CallingAppInfo.getOrigin] is set
 * @param requestJson the request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
 * @param clientDataHash a hash that is used to verify the relying party identity, set only if
 * [android.service.credentials.CallingAppInfo.getOrigin] is set
 * @param id the id of this request option
 * @param candidateQueryData the request data in the [Bundle] format
 * @throws NullPointerException If [requestJson] is null
 * @throws IllegalArgumentException If [requestJson] is empty
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class BeginGetPublicKeyCredentialOption @JvmOverloads constructor(
    candidateQueryData: Bundle,
    id: String,
    val requestJson: String,
    val clientDataHash: ByteArray? = null,
) : BeginGetCredentialOption(
    id,
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    candidateQueryData
) {
    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
    }

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        /** @hide */
        @JvmStatic
        internal fun createFrom(data: Bundle, id: String): BeginGetPublicKeyCredentialOption {
            try {
                val requestJson = data.getString(GetPublicKeyCredentialOption
                    .BUNDLE_KEY_REQUEST_JSON)
                val clientDataHash = data.getByteArray(GetPublicKeyCredentialOption
                    .BUNDLE_KEY_CLIENT_DATA_HASH)
                return BeginGetPublicKeyCredentialOption(data, id, requestJson!!, clientDataHash)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        /** @hide */
        @JvmStatic
        internal fun createFromEntrySlice(data: Bundle, id: String):
            BeginGetPublicKeyCredentialOption {
            val requestJson = "dummy_request_json"
            return BeginGetPublicKeyCredentialOption(data, id, requestJson)
        }
    }
}
