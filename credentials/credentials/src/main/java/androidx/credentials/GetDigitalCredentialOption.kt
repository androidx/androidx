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

import android.content.ComponentName
import android.os.Bundle
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.internal.RequestValidationHelper

/**
 * A request to retrieve the user's digital credential, normally used for verification or sign-in
 * purpose.
 *
 * Note that this option cannot be combined with other types of options in a single
 * [GetCredentialRequest].
 *
 * @property requestJson the request in the JSON format; the latest format is defined at
 *   https://wicg.github.io/digital-credentials/#the-digitalcredentialrequestoptions-dictionary
 */
@ExperimentalDigitalCredentialApi
class GetDigitalCredentialOption
internal constructor(
    val requestJson: String,
    requestData: Bundle,
    candidateQueryData: Bundle,
    isSystemProviderRequired: Boolean,
    isAutoSelectAllowed: Boolean,
    allowedProviders: Set<ComponentName>,
    typePriorityHint: @PriorityHints Int,
) :
    CredentialOption(
        type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
        requestData = requestData,
        candidateQueryData = candidateQueryData,
        isSystemProviderRequired = isSystemProviderRequired,
        isAutoSelectAllowed = isAutoSelectAllowed,
        allowedProviders = allowedProviders,
        typePriorityHint = typePriorityHint,
    ) {

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "credentialJson must not be empty, and must be a valid JSON"
        }
    }

    /**
     * Constructs a `GetDigitalCredentialOption`.
     *
     * Note that this option cannot be combined with other types of options in a single
     * [GetCredentialRequest].
     *
     * @param requestJson the request in the JSON format; the latest format is defined at
     *   https://wicg.github.io/digital-credentials/#the-digitalcredentialrequestoptions-dictionary
     * @throws IllegalArgumentException if the `credentialJson` is not a valid json
     */
    constructor(
        requestJson: String
    ) : this(
        requestJson = requestJson,
        requestData = toBundle(requestJson),
        candidateQueryData = toBundle(requestJson),
        isSystemProviderRequired = false,
        isAutoSelectAllowed = false,
        allowedProviders = emptySet(),
        typePriorityHint = PRIORITY_PASSKEY_OR_SIMILAR,
    )

    /** Companion constants / helpers for [GetDigitalCredentialOption]. */
    internal companion object {
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"

        @JvmStatic
        internal fun toBundle(requestJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            return bundle
        }

        @JvmStatic
        internal fun createFrom(
            requestData: Bundle,
            candidateQueryData: Bundle,
            requireSystemProvider: Boolean,
            allowedProviders: Set<ComponentName>,
        ): GetDigitalCredentialOption {
            try {
                val requestJson = requestData.getString(BUNDLE_KEY_REQUEST_JSON)!!
                return GetDigitalCredentialOption(
                    requestJson = requestJson,
                    requestData = requestData,
                    candidateQueryData = candidateQueryData,
                    isSystemProviderRequired = requireSystemProvider,
                    isAutoSelectAllowed =
                        requestData.getBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, false),
                    allowedProviders = allowedProviders,
                    typePriorityHint =
                        requestData.getInt(
                            BUNDLE_KEY_TYPE_PRIORITY_VALUE,
                            PRIORITY_PASSKEY_OR_SIMILAR
                        ),
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
