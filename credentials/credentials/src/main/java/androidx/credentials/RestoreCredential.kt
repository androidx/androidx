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
 * Represents the user's restore credential for the app sign in. The restore credential is used to
 * restore the user's credential from the previous device to a new Android device.
 *
 * By creating a [RestoreCredential] for the user, the credential will be automatically transferred
 * over to the user's new device if the user selects the app to be transferred from the old device
 * during the setup stage.
 *
 * The [RestoreCredential] can only be used for apps that the users have selected to be restored.
 * The [RestoreCredential] for all other apps will not be transferred over from the old device. This
 * Credential can be used to create a seamless authentication user experience by providing a 0-tap
 * sign-in experience. If the [RestoreCredential] is available for an app, the user can be signed in
 * programmatically without the user's input.
 *
 * @param authenticationResponseJson the request the public key credential authentication response
 *   in JSON format that follows the standard webauthn json format shown at
 *   (https://w3c.github.io/webauthn/#dictdef-authenticationresponsejson)
 * @throws IllegalArgumentException If [authenticationResponseJson] is empty, or if it is not a
 *   valid JSON
 * @see CreateRestoreCredentialRequest on how to create a [RestoreCredential] instance.
 */
class RestoreCredential private constructor(val authenticationResponseJson: String, data: Bundle) :
    Credential(TYPE_RESTORE_CREDENTIAL, data) {

    init {
        require(RequestValidationHelper.isValidJSON(authenticationResponseJson)) {
            "authenticationResponseJson must not be empty, and must be a valid JSON"
        }
    }

    companion object {
        @JvmStatic
        internal fun createFrom(data: Bundle): RestoreCredential {
            val responseJson =
                data.getString(BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE)
                    ?: throw NoCredentialException(
                        "The device does not contain a restore credential."
                    )
            return RestoreCredential(responseJson, data)
        }

        /** The type value for restore credential related operations. */
        const val TYPE_RESTORE_CREDENTIAL: String = "androidx.credentials.TYPE_RESTORE_CREDENTIAL"
        private const val BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE =
            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE"
    }
}
