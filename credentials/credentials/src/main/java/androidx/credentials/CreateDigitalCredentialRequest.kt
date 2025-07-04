/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * A request to save a digital credential to a holder application of the user's choice.
 *
 * @property requestJson The
 *   [JSON](https://w3c-fedid.github.io/digital-credentials/#extensions-to-credentialcreationoptions-dictionary)
 *   string representing the request
 */
@ExperimentalDigitalCredentialApi
class CreateDigitalCredentialRequest
private constructor(
    val requestJson: String,
    origin: String?,
    credentialData: Bundle,
    candidateQueryData: Bundle,
) :
    CreateCredentialRequest(
        type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
        credentialData = credentialData,
        displayInfo = populateUnusedDisplayInfo(),
        candidateQueryData = candidateQueryData,
        isSystemProviderRequired = false,
        isAutoSelectAllowed = false,
        origin = origin,
        preferImmediatelyAvailableCredentials = false,
    ) {

    /**
     * @param requestJson The
     *   [JSON](https://w3c-fedid.github.io/digital-credentials/#extensions-to-credentialcreationoptions-dictionary)
     *   string representing the request
     * @param origin the origin of a different application if the request is being made on behalf of
     *   that application
     */
    constructor(
        requestJson: String,
        origin: String?,
    ) : this(
        requestJson = requestJson,
        origin = origin,
        credentialData = toBundle(requestJson),
        candidateQueryData = Bundle(),
    )

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }

    internal companion object {
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"

        // DisplayInfo not used in this request, user name is required to create a DisplayInfo
        internal const val UNUSED_USER_ID = "unused"

        @JvmStatic fun populateUnusedDisplayInfo() = DisplayInfo(userId = UNUSED_USER_ID)

        @JvmStatic
        internal fun toBundle(requestJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            return bundle
        }

        @JvmStatic
        fun createFrom(
            data: Bundle,
            origin: String?,
            candidateQueryData: Bundle,
        ): CreateDigitalCredentialRequest {
            val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)
            if (requestJson == null) {
                throw FrameworkClassParsingException()
            }
            return CreateDigitalCredentialRequest(
                requestJson = requestJson,
                origin = origin,
                credentialData = data,
                candidateQueryData = candidateQueryData,
            )
        }
    }
}
