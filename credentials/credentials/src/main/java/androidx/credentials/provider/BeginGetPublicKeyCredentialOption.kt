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
 * @property requestJson the privileged request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
 * @property allowHybrid defines whether hybrid credentials are allowed to fulfill this request,
 * true by default, with hybrid credentials defined
 * [here](https://w3c.github.io/webauthn/#dom-authenticatortransport-hybrid)
 * @throws NullPointerException If [requestJson] is null
 * @throws IllegalArgumentException If [requestJson] is empty
 *
 * @hide
 */
class BeginGetPublicKeyCredentialOption @JvmOverloads internal constructor(
    val requestJson: String,
    @get:JvmName("allowHybrid")
    val allowHybrid: Boolean = true,
) : BeginGetCredentialOption(
    type = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL
) {
    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
    }

    /** @hide */
    companion object {
        @Suppress("deprecation") // bundle.get() used for boolean value to prevent default
                                         // boolean value from being returned.
        @JvmStatic
        internal fun createFrom(data: Bundle): BeginGetPublicKeyCredentialOption {
            try {
                val requestJson = data.getString(
                    GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON)
                val allowHybrid = data.get(
                    GetPublicKeyCredentialOption.BUNDLE_KEY_ALLOW_HYBRID)
                return BeginGetPublicKeyCredentialOption(requestJson!!, (allowHybrid!!) as Boolean)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}