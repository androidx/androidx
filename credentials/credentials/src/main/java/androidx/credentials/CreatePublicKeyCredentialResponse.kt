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
 * A response of a public key credential (passkey) flow.
 *
 * @property registrationResponseJson the public key credential registration response in JSON format
 * @throws NullPointerException If [registrationResponseJson] is null
 * @throws IllegalArgumentException If [registrationResponseJson] is blank
 */
class CreatePublicKeyCredentialResponse(
    val registrationResponseJson: String
) : CreateCredentialResponse(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    toBundle(registrationResponseJson)
) {

    init {
        require(registrationResponseJson.isNotEmpty()) { "registrationResponseJson must not be " +
            "empty" }
    }

    internal companion object {
        internal const val BUNDLE_KEY_REGISTRATION_RESPONSE_JSON =
            "androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON"

        @JvmStatic
        internal fun toBundle(registrationResponseJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REGISTRATION_RESPONSE_JSON, registrationResponseJson)
            return bundle
        }

        @JvmStatic
        internal fun createFrom(data: Bundle): CreatePublicKeyCredentialResponse {
            try {
                val registrationResponseJson =
                    data.getString(BUNDLE_KEY_REGISTRATION_RESPONSE_JSON)
                return CreatePublicKeyCredentialResponse(registrationResponseJson!!)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}