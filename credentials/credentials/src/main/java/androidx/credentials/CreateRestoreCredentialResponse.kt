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
import androidx.annotation.RestrictTo
import androidx.credentials.exceptions.CreateCredentialUnknownException

/**
 * A response of the [RestoreCredential] flow.
 *
 * @property responseJson the public key credential registration response in
 *   [JSON format](https://w3c.github.io/webauthn/#authenticatorattestationresponse).
 */
class CreateRestoreCredentialResponse
private constructor(
    val responseJson: String,
    data: Bundle,
) : CreateCredentialResponse(RestoreCredential.TYPE_RESTORE_CREDENTIAL, data) {

    /** Constructs a [CreateRestoreCredentialResponse]. */
    constructor(responseJson: String) : this(responseJson, toBundle(responseJson))

    companion object {
        const val BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_RESPONSE =
            "androidx.credentials.BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_RESPONSE"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun createFrom(data: Bundle): CreateRestoreCredentialResponse {
            val responseJson =
                data.getString(BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_RESPONSE)
                    ?: throw CreateCredentialUnknownException(
                        "The response bundle did not contain the response data. This should not happen."
                    )
            return CreateRestoreCredentialResponse(responseJson, data)
        }

        @JvmStatic
        internal fun toBundle(responseJson: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_RESPONSE, responseJson)
            return bundle
        }
    }
}
