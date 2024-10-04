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
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.internal.RequestValidationHelper

/**
 * A request to get the restore credential from the restore credential provider.
 *
 * @property requestJson the request in JSON format in the standard webauthn web json
 *   (https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson).
 *
 * Note that the userVerification field of the requestJson will always be overridden to
 * `discouraged` to support passive authentication during restore flow.
 *
 * [GetRestoreCredentialOption] cannot be requested with other credential options because of
 * conflicting user experience. When requesting restore credential, only a single
 * [GetRestoreCredentialOption] must be supplied.
 *
 * @throws IllegalArgumentException if the requestJson is an invalid Json that does not follow the
 *   standard webauthn web json format
 * @throws NoCredentialException if no viable restore credential is found
 * @throws IllegalArgumentException if the option is mixed with another [CredentialOption]
 */
class GetRestoreCredentialOption(val requestJson: String) :
    CredentialOption(
        type = RestoreCredential.TYPE_RESTORE_CREDENTIAL,
        requestData = toRequestDataBundle(requestJson),
        candidateQueryData = Bundle(),
        isSystemProviderRequired = false,
        isAutoSelectAllowed = false,
        allowedProviders = emptySet(),
        typePriorityHint = PRIORITY_DEFAULT,
    ) {

    init {
        require(RequestValidationHelper.isValidJSON(requestJson)) {
            "requestJson must not be empty, and must be a valid JSON"
        }
    }

    private companion object {
        private const val BUNDLE_KEY_GET_RESTORE_CREDENTIAL_REQUEST =
            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_REQUEST"

        private fun toRequestDataBundle(requestJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_GET_RESTORE_CREDENTIAL_REQUEST, requestJson)
            return bundle
        }
    }
}
