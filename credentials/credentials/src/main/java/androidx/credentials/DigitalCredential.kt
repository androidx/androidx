/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.credentials.internal.RequestValidationHelper

/**
 * Represents the user's digital credential, generally used for verification or sign-in purposes.
 *
 * Notice, this class does not necessarily always represents a successful digital credential.
 * Depending on the protocol in your request, the [credentialJson] may contain a failure response
 * generated from the provider. Therefore, please follow the protocol specification to parse the
 * [credentialJson] and determine the failure / success response. For example,
 * [here](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-error-response)
 * is the error response definition for the OpenID for Verifiable Presentations protocol.
 *
 * @property credentialJson the digital credential in the JSON format; the latest format is defined
 *   at https://wicg.github.io/digital-credentials/#the-digitalcredential-interface
 */
@ExperimentalDigitalCredentialApi
class DigitalCredential private constructor(val credentialJson: String, data: Bundle) :
    Credential(TYPE_DIGITAL_CREDENTIAL, data) {

    init {
        require(RequestValidationHelper.isValidJSON(credentialJson)) {
            "credentialJson must not be empty, and must be a valid JSON"
        }
    }

    /**
     * Constructs a `DigitalCredential`.
     *
     * @param credentialJson the digital credential in the JSON format; the latest format is defined
     *   at https://wicg.github.io/digital-credentials/#the-digitalcredential-interface
     * @throws IllegalArgumentException if the `credentialJson` is not a valid json
     */
    constructor(credentialJson: String) : this(credentialJson, toBundle(credentialJson))

    /** Companion constants / helpers for [DigitalCredential]. */
    companion object {
        /** The type value for public key credential related operations. */
        const val TYPE_DIGITAL_CREDENTIAL: String = "androidx.credentials.TYPE_DIGITAL_CREDENTIAL"

        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"

        // If the string length exceeds the threshold, the string will be converted to a byte array
        // during serialization to optimize space usage.
        private const val STRING_LEN_THRESHOLD = 250000

        @JvmStatic
        @Suppress("DEPRECATION")
        internal fun createFrom(data: Bundle): DigitalCredential {
            try {
                val credentialJson = data.get(BUNDLE_KEY_REQUEST_JSON)!!
                return when (credentialJson) {
                    is ByteArray -> DigitalCredential(String(credentialJson, Charsets.UTF_8), data)
                    else -> DigitalCredential(credentialJson as String, data)
                }
            } catch (_: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        @JvmStatic
        internal fun toBundle(responseJson: String): Bundle {
            val bundle = Bundle()
            if (responseJson.length >= STRING_LEN_THRESHOLD) {
                val jsonBytes = responseJson.toByteArray(Charsets.UTF_8)
                bundle.putByteArray(BUNDLE_KEY_REQUEST_JSON, jsonBytes)
            } else {
                bundle.putString(BUNDLE_KEY_REQUEST_JSON, responseJson)
            }
            return bundle
        }
    }
}
