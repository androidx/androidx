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
import androidx.annotation.RestrictTo

/**
 * Encapsulates the result of a user credential request.
 *
 * @property credential the user credential that can be used to authenticate to your app
 * @throws NullPointerException If [credential] is null
 */
class GetCredentialResponse(val credential: Credential) {
    internal companion object {
        private const val EXTRA_CREDENTIAL_TYPE =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_TYPE"
        private const val EXTRA_CREDENTIAL_DATA =
            "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA"

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun asBundle(response: GetCredentialResponse): Bundle =
            Bundle().apply {
                putString(EXTRA_CREDENTIAL_TYPE, response.credential.type)
                putBundle(EXTRA_CREDENTIAL_DATA, response.credential.data)
            }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromBundle(bundle: Bundle): GetCredentialResponse? {
            val type = bundle.getString(EXTRA_CREDENTIAL_TYPE) ?: return null
            val data = bundle.getBundle(EXTRA_CREDENTIAL_DATA) ?: return null
            return GetCredentialResponse(Credential.createFrom(type, data))
        }
    }
}
