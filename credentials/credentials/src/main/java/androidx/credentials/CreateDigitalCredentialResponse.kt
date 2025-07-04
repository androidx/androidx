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

package androidx.credentials

import android.os.Bundle
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.internal.RequestValidationHelper

/**
 * A response of creating a digital credential.
 *
 * @property responseJson The
 *   [JSON](https://w3c-fedid.github.io/digital-credentials/#dom-digitalcredential) string
 *   representing the response.
 */
@ExperimentalDigitalCredentialApi
class CreateDigitalCredentialResponse(val responseJson: String) :
    CreateCredentialResponse(
        type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
        data = toBundle(responseJson),
    ) {
    init {
        require(RequestValidationHelper.isValidJSON(responseJson)) {
            "responseJson must not be empty, and must be a valid JSON"
        }
    }

    companion object {
        internal const val BUNDLE_KEY_RESPONSE_JSON =
            "androidx.credentials.BUNDLE_KEY_RESPONSE_JSON"

        @JvmStatic
        internal fun toBundle(responseJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_RESPONSE_JSON, responseJson)
            return bundle
        }

        @JvmStatic
        internal fun createFrom(data: Bundle): CreateDigitalCredentialResponse {
            try {
                val responseJson = data.getString(BUNDLE_KEY_RESPONSE_JSON)
                return CreateDigitalCredentialResponse(responseJson!!)
            } catch (_: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
