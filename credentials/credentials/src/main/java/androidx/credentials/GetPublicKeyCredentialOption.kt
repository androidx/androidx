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

package androidx.credentials

import android.os.Bundle
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A request to get passkeys from the user's public key credential provider.
 *
 * @property requestJson the request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson).
 * @property clientDataHash a hash that is used to verify the relying party identity, set only if
 * you have set the [GetCredentialRequest.origin]
 * @property preferImmediatelyAvailableCredentials true if you prefer the operation to return
 * immediately when there is no available credential instead of falling back to discovering remote
 * credentials, and false (default) otherwise
 * @throws NullPointerException If [requestJson] is null
 * @throws IllegalArgumentException If [requestJson] is empty
 */
class GetPublicKeyCredentialOption @JvmOverloads constructor(
    val requestJson: String,
    val clientDataHash: String? = null,
    @get:JvmName("preferImmediatelyAvailableCredentials")
    val preferImmediatelyAvailableCredentials: Boolean = false,
) : CredentialOption(
    type = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    requestData = toRequestDataBundle(requestJson, clientDataHash,
        preferImmediatelyAvailableCredentials),
    candidateQueryData = toRequestDataBundle(requestJson, clientDataHash,
        preferImmediatelyAvailableCredentials),
    isSystemProviderRequired = false,
    isAutoSelectAllowed = true,
) {
    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
    }

    internal companion object {
        internal const val BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS =
            "androidx.credentials.BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS"
        internal const val BUNDLE_KEY_CLIENT_DATA_HASH =
            "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH"
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        internal const val BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION =
            "androidx.credentials.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION"

        @JvmStatic
        internal fun toRequestDataBundle(
            requestJson: String,
            clientDataHash: String?,
            preferImmediatelyAvailableCredentials: Boolean
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION
            )
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putString(BUNDLE_KEY_CLIENT_DATA_HASH, clientDataHash)
            bundle.putBoolean(BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                preferImmediatelyAvailableCredentials)
            return bundle
        }

        @Suppress("deprecation") // bundle.get() used for boolean value to prevent default
                                         // boolean value from being returned.
        @JvmStatic
        internal fun createFrom(data: Bundle): GetPublicKeyCredentialOption {
            try {
                val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)
                val clientDataHash = data.getString(BUNDLE_KEY_CLIENT_DATA_HASH)
                val preferImmediatelyAvailableCredentials =
                    data.get(BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS)
                return GetPublicKeyCredentialOption(requestJson!!,
                    clientDataHash,
                    (preferImmediatelyAvailableCredentials!!) as Boolean)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
