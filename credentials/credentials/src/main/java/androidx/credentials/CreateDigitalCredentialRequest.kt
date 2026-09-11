/*
 * Copyright 2026 The Android Open Source Project
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
 * @property bindingTokenOptions optional options for generating a binding token that links the
 *   request session between the caller and the user-selected credential provider. Note that a
 *   credential provider will never receive this information when they receive the request.
 */
@ExperimentalDigitalCredentialApi
class CreateDigitalCredentialRequest
private constructor(
    val requestJson: String,
    val bindingTokenOptions: BindingTokenOptions?,
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
     * Constructs a request to save a digital credential to a holder application of the user's
     * choice.
     *
     * @param requestJson The
     *   [JSON](https://w3c-fedid.github.io/digital-credentials/#extensions-to-credentialcreationoptions-dictionary)
     *   string representing the request
     * @param origin the origin of a different application if the request is being made on behalf of
     *   that application
     * @param bindingTokenOptions optional options for generating a binding token that links the
     *   request session between the caller and the user-selected credential provider. Note that a
     *   credential provider will never receive this information when they receive the request.
     */
    @JvmOverloads
    constructor(
        requestJson: String,
        origin: String? = null,
        bindingTokenOptions: BindingTokenOptions? = null,
    ) : this(
        requestJson = requestJson,
        bindingTokenOptions = bindingTokenOptions,
        origin = origin,
        credentialData = toBundle(requestJson, bindingTokenOptions),
        candidateQueryData = Bundle(),
    )

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }

    internal companion object {
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        internal const val BUNDLE_KEY_BINDING_TOKEN_PROOFING_TOKEN =
            "androidx.credentials.BUNDLE_KEY_BINDING_TOKEN_PROOFING_TOKEN"
        internal const val BUNDLE_KEY_BINDING_TOKEN_ALGORITHM =
            "androidx.credentials.BUNDLE_KEY_BINDING_TOKEN_ALGORITHM"

        // DisplayInfo not used in this request, user name is required to create a DisplayInfo
        internal const val UNUSED_USER_ID = "unused"

        @JvmStatic internal fun populateUnusedDisplayInfo() = DisplayInfo(userId = UNUSED_USER_ID)

        @JvmStatic
        internal fun toBundle(
            requestJson: String,
            bindingTokenOptions: BindingTokenOptions? = null,
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            if (bindingTokenOptions != null) {
                bundle.putByteArray(
                    BUNDLE_KEY_BINDING_TOKEN_PROOFING_TOKEN,
                    bindingTokenOptions.proofingToken,
                )
                bundle.putString(
                    BUNDLE_KEY_BINDING_TOKEN_ALGORITHM,
                    bindingTokenOptions.bindingAlgorithm,
                )
            }
            return bundle
        }

        @JvmStatic
        internal fun createFrom(
            data: Bundle,
            origin: String?,
            candidateQueryData: Bundle,
        ): CreateDigitalCredentialRequest {
            val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)
            if (requestJson == null) {
                throw FrameworkClassParsingException()
            }
            val proofingToken = data.getByteArray(BUNDLE_KEY_BINDING_TOKEN_PROOFING_TOKEN)
            val bindingAlgorithm = data.getString(BUNDLE_KEY_BINDING_TOKEN_ALGORITHM)
            val bindingTokenOptions =
                if (proofingToken != null) {
                    if (bindingAlgorithm != null) {
                        BindingTokenOptions(proofingToken, bindingAlgorithm)
                    } else {
                        BindingTokenOptions(proofingToken)
                    }
                } else {
                    null
                }
            return CreateDigitalCredentialRequest(
                requestJson = requestJson,
                bindingTokenOptions = bindingTokenOptions,
                origin = origin,
                credentialData = data,
                candidateQueryData = candidateQueryData,
            )
        }
    }
}
