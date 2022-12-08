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
import androidx.credentials.GetPublicKeyCredentialOptionPrivileged
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A privileged request to get passkeys from the user's public key credential provider. The caller
 * can modify the RP. Only callers with privileged permission (e.g. user's public browser or caBLE)
 * can use this. These permissions will be introduced in an upcoming release.
 * TODO("Add specific permission info/annotation")
 *
 * @property requestJson the privileged request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson).
 * @property allowHybrid defines whether hybrid credentials are allowed to fulfill this request,
 * true by default, with hybrid credentials defined
 * [here](https://w3c.github.io/webauthn/#dom-authenticatortransport-hybrid)
 * @property relyingParty the expected true RP ID which will override the one in the [requestJson],
 * where relyingParty is defined [here](https://w3c.github.io/webauthn/#rp-id) in more detail
 * @property clientDataHash a hash that is used to verify the [relyingParty] Identity
 * @throws NullPointerException If any of [requestJson], [relyingParty], or [clientDataHash]
 * is null
 * @throws IllegalArgumentException If any of [requestJson], [relyingParty], or [clientDataHash] is empty
 *
 * @hide
 */
class BeginGetPublicKeyCredentialOptionPrivileged @JvmOverloads internal constructor(
    val requestJson: String,
    val relyingParty: String,
    val clientDataHash: String,
    @get:JvmName("allowHybrid")
    val allowHybrid: Boolean = true
) : BeginGetCredentialOption(
    type = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL
) {

    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
        require(relyingParty.isNotEmpty()) { "rp must not be empty" }
        require(clientDataHash.isNotEmpty()) { "clientDataHash must not be empty" }
    }

    /** @hide */
    companion object {
        @Suppress("deprecation") // bundle.get() used for boolean value to prevent default
                                         // boolean value from being returned.
        @JvmStatic
        internal fun createFrom(data: Bundle): BeginGetPublicKeyCredentialOptionPrivileged {
            try {
                val requestJson = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_REQUEST_JSON)
                val rp = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RELYING_PARTY)
                val clientDataHash = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH)
                val allowHybrid = data.get(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_ALLOW_HYBRID)
                return BeginGetPublicKeyCredentialOptionPrivileged(
                    requestJson!!,
                    rp!!,
                    clientDataHash!!,
                    (allowHybrid!!) as Boolean,
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}