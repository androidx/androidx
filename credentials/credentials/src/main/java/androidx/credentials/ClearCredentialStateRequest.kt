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

/**
 * Request class for clearing a user's credential state from the credential providers.
 *
 * If the request type is [TYPE_CLEAR_CREDENTIAL_STATE], then the request will be sent to the
 * credential providers to clear the user's credential state.
 *
 * If the request type is [TYPE_CLEAR_RESTORE_CREDENTIAL], then the request will be sent to the
 * restore credential provider to delete any stored [RestoreCredential].
 *
 * @constructor creates a new ClearCredentialStateRequest
 * @property requestType the type of this request
 * @throws IllegalArgumentException if the [requestType] is unsupported type.
 */
class ClearCredentialStateRequest
@JvmOverloads
constructor(val requestType: @ClearCredentialRequestTypes String = TYPE_CLEAR_CREDENTIAL_STATE) {
    val requestBundle: Bundle = Bundle()

    init {
        if (
            requestType != TYPE_CLEAR_CREDENTIAL_STATE &&
                requestType != TYPE_CLEAR_RESTORE_CREDENTIAL
        ) {
            throw IllegalArgumentException("The request type $requestType is not supported.")
        }
        if (requestType == TYPE_CLEAR_RESTORE_CREDENTIAL) {
            requestBundle.putBoolean(BUNDLE_KEY_CLEAR_RESTORE_CREDENTIAL_REQUEST, true)
        }
    }

    companion object {
        /**
         * Clears credential state from all credential providers that have cached a user sign-in
         * states.
         */
        const val TYPE_CLEAR_CREDENTIAL_STATE = "androidx.credentials.TYPE_CLEAR_CREDENTIAL_STATE"

        /** Clears restore credential from the device and the backup */
        const val TYPE_CLEAR_RESTORE_CREDENTIAL =
            "androidx.credentials.TYPE_CLEAR_RESTORE_CREDENTIAL"

        private const val BUNDLE_KEY_CLEAR_RESTORE_CREDENTIAL_REQUEST =
            "androidx.credentials.BUNDLE_KEY_CLEAR_RESTORE_CREDENTIAL_REQUEST"
    }
}
